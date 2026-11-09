package jp.jtak.core.weapons;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import jp.jtak.core.Lang;

/**
 * 依存のない小さな JSON の読み手（RFC 8259 の文法で厳しく読む。04 の 2.2）。
 * 値は {@link Map}（オブジェクト。キーの順を保つ）・{@link List}・{@link String}・{@link JsonNumber}・
 * {@link Boolean}・{@link #NULL} になる。
 * <p>
 * Small JSON reader without dependencies (strict RFC 8259 grammar; 04 §2.2). Values become {@link Map} (objects;
 * preserving key order), {@link List}, {@link String}, {@link JsonNumber}, {@link Boolean}, or {@link #NULL}.
 */
final class WeaponJson {
    static final int MAX_BYTES = 262144;
    static final int MAX_DEPTH = 10;
    /**
     * JSON の {@code null}。
     * <p>
     * JSON {@code null}.
     */
    static final Object NULL = new Object();

    private WeaponJson() {}

    /**
     * JSON の数。書き方（整数の書き方か）を見るために、元の文字も持つ。
     * <p>
     * JSON number. Also retains its original text to inspect its notation (whether written as an integer).
     */
    static final class JsonNumber {
        private final String raw;
        private final BigDecimal value;

        JsonNumber(String raw) {
            this.raw = raw;
            this.value = new BigDecimal(raw);
        }

        BigDecimal value() {
            return value;
        }

        /**
         * 小数点も指数もない書き方か。
         * <p>
         * Whether the notation contains neither a decimal point nor an exponent.
         */
        boolean isInteger() {
            return raw.indexOf('.') < 0 && raw.indexOf('e') < 0 && raw.indexOf('E') < 0;
        }
    }

    /**
     * 文法のエラー。メッセージは「{行}行{列}列: {理由}」。
     * <p>
     * Syntax error. Message format: “{line}行{column}列: {reason}”.
     */
    static final class JsonException extends Exception {
        private static final long serialVersionUID = 1L;

        JsonException(String message) {
            super(message);
        }
    }

    /**
     * UTF-8 のバイト列を読む。先頭の BOM は取り除く。
     * <p>
     * Reads UTF-8 bytes. Removes a leading BOM.
     */
    static Object parse(byte[] utf8, Lang lang) throws JsonException {
        Objects.requireNonNull(lang, "lang");
        if (utf8.length > MAX_BYTES) {
            throw new JsonException(lang == Lang.JA ? "1行1列: 入力が 256 KiB（262144 バイト）を超えています"
                    : "line 1, column 1: input exceeds 256 KiB (262144 bytes)");
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(utf8);
        CharBuffer out = CharBuffer.allocate(utf8.length + 1);
        CoderResult result = decoder.decode(in, out, true);
        if (result.isUnderflow()) {
            result = decoder.flush(out);
        }
        if (result.isError()) {
            String prefix = new String(utf8, 0, in.position(), StandardCharsets.UTF_8);
            throw new JsonException(position(prefix, prefix.length(), lang) + (lang == Lang.JA ? ": 正しくない UTF-8 のバイト列です" : ": invalid UTF-8 byte sequence"));
        }
        out.flip();
        String text = out.toString();
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        return new Reader(text, lang).readDocument();
    }

    /**
     * {@code text} の {@code index} 文字目の場所を「{行}行{列}列」にする（列は符号位置で数える）。
     * <p>
     * Converts character position {@code index} in {@code text} to “{line}行{column}列” (columns counted in code
     * points).
     */
    private static String position(String text, int index, Lang lang) {
        int line = 1;
        int lineStart = 0;
        int end = Math.min(index, text.length());
        for (int i = 0; i < end; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                lineStart = i + 1;
            }
        }
        int column = text.codePointCount(lineStart, end) + 1;
        return lang == Lang.JA ? line + "行" + column + "列" : "line " + line + ", column " + column;
    }

    private static final class Reader {
        private final String text;
        private final Lang lang;
        private int pos;

        Reader(String text, Lang lang) {
            this.text = text;
            this.lang = lang;
        }

        private JsonException error(int at, String reason) {
            return new JsonException(position(text, at, lang) + ": " + reason);
        }

        Object readDocument() throws JsonException {
            skipSpace();
            Object value = readValue(0);
            skipSpace();
            if (pos < text.length()) {
                throw error(pos, lang == Lang.JA ? "値の後に余分な文字があります" : "extra characters after the value");
            }
            return value;
        }

        private void skipSpace() {
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private Object readValue(int depth) throws JsonException {
            if (pos >= text.length()) {
                throw error(pos, lang == Lang.JA ? "値がありません（入力が途中で終わっています）" : "missing value (the input ends early)");
            }
            char c = text.charAt(pos);
            switch (c) {
                case '{':
                    return readObject(depth + 1);
                case '[':
                    return readArray(depth + 1);
                case '"':
                    return readString();
                case 't':
                    return readLiteral("true", Boolean.TRUE);
                case 'f':
                    return readLiteral("false", Boolean.FALSE);
                case 'n':
                    return readLiteral("null", NULL);
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return readNumber();
                    }
                    throw error(pos, lang == Lang.JA ? "値として読めない文字です（" + c + "）" : "unexpected character (" + c + ")");
            }
        }

        private Object readLiteral(String word, Object value) throws JsonException {
            if (text.startsWith(word, pos)) {
                pos += word.length();
                return value;
            }
            throw error(pos, lang == Lang.JA ? "値として読めない文字です" : "unexpected character");
        }

        private Map<String, Object> readObject(int depth) throws JsonException {
            if (depth > MAX_DEPTH) {
                throw error(pos, lang == Lang.JA ? "入れ子が深すぎます（" + MAX_DEPTH + " 段まで）" : "nesting too deep (up to " + MAX_DEPTH + " levels)");
            }
            pos++; // {
            Map<String, Object> map = new LinkedHashMap<>();
            skipSpace();
            if (pos < text.length() && text.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipSpace();
                if (pos >= text.length() || text.charAt(pos) != '"') {
                    throw error(pos, lang == Lang.JA ? "キーは \" で囲んだ文字列にしてください" : "a key must be a string in double quotes");
                }
                int keyAt = pos;
                String key = readString();
                if (map.containsKey(key)) {
                    throw error(keyAt, lang == Lang.JA ? "同じキーがあります（" + key + "）" : "duplicate key (" + key + ")");
                }
                skipSpace();
                if (pos >= text.length() || text.charAt(pos) != ':') {
                    throw error(pos, lang == Lang.JA ? "キーの後に : が必要です" : "':' expected after the key");
                }
                pos++;
                skipSpace();
                map.put(key, readValue(depth));
                skipSpace();
                if (pos >= text.length()) {
                    throw error(pos, lang == Lang.JA ? "オブジェクトが閉じていません" : "object not closed");
                }
                char c = text.charAt(pos);
                if (c == ',') {
                    pos++;
                } else if (c == '}') {
                    pos++;
                    return map;
                } else {
                    throw error(pos, lang == Lang.JA ? ", か } が必要です" : "',' or '}' expected");
                }
            }
        }

        private List<Object> readArray(int depth) throws JsonException {
            if (depth > MAX_DEPTH) {
                throw error(pos, lang == Lang.JA ? "入れ子が深すぎます（" + MAX_DEPTH + " 段まで）" : "nesting too deep (up to " + MAX_DEPTH + " levels)");
            }
            pos++; // [
            List<Object> list = new ArrayList<>();
            skipSpace();
            if (pos < text.length() && text.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipSpace();
                list.add(readValue(depth));
                skipSpace();
                if (pos >= text.length()) {
                    throw error(pos, lang == Lang.JA ? "配列が閉じていません" : "array not closed");
                }
                char c = text.charAt(pos);
                if (c == ',') {
                    pos++;
                } else if (c == ']') {
                    pos++;
                    return list;
                } else {
                    throw error(pos, lang == Lang.JA ? ", か ] が必要です" : "',' or ']' expected");
                }
            }
        }

        private String readString() throws JsonException {
            pos++; // "
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= text.length()) {
                    throw error(pos, lang == Lang.JA ? "文字列が閉じていません" : "string not closed");
                }
                char c = text.charAt(pos);
                if (c == '"') {
                    pos++;
                    return sb.toString();
                }
                if (c < 0x20) {
                    throw error(pos, lang == Lang.JA ? "文字列の中に、エスケープしていない制御文字があります" : "unescaped control character in a string");
                }
                if (c != '\\') {
                    sb.append(c);
                    pos++;
                    continue;
                }
                int escapeAt = pos;
                pos++;
                if (pos >= text.length()) {
                    throw error(escapeAt, lang == Lang.JA ? "文字列が閉じていません" : "string not closed");
                }
                char e = text.charAt(pos++);
                switch (e) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        readUnicodeEscape(escapeAt, sb);
                        break;
                    default:
                        throw error(escapeAt, lang == Lang.JA ? "使えないエスケープです（\\" + e + "）" : "invalid escape (\\" + e + ")");
                }
            }
        }

        private int readHex4(int escapeAt) throws JsonException {
            if (pos + 4 > text.length()) {
                throw error(escapeAt, lang == Lang.JA ? "\\u の後に 16 進数 4 桁が必要です" : "4 hex digits expected after \\u");
            }
            int v = 0;
            for (int i = 0; i < 4; i++) {
                int d = Character.digit(text.charAt(pos + i), 16);
                char ch = text.charAt(pos + i);
                boolean hex = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
                if (!hex || d < 0) {
                    throw error(escapeAt, lang == Lang.JA ? "\\u の後に 16 進数 4 桁が必要です" : "4 hex digits expected after \\u");
                }
                v = v * 16 + d;
            }
            pos += 4;
            return v;
        }

        private void readUnicodeEscape(int escapeAt, StringBuilder sb) throws JsonException {
            int u = readHex4(escapeAt);
            if (Character.isHighSurrogate((char) u)) {
                if (text.startsWith("\\u", pos)) {
                    int saved = pos;
                    pos += 2;
                    int low = readHex4(saved);
                    if (Character.isLowSurrogate((char) low)) {
                        sb.append((char) u).append((char) low);
                        return;
                    }
                }
                throw error(escapeAt, lang == Lang.JA ? "組になっていないサロゲートです" : "unpaired surrogate");
            }
            if (Character.isLowSurrogate((char) u)) {
                throw error(escapeAt, lang == Lang.JA ? "組になっていないサロゲートです" : "unpaired surrogate");
            }
            sb.append((char) u);
        }

        private JsonNumber readNumber() throws JsonException {
            int start = pos;
            if (text.charAt(pos) == '-') {
                pos++;
            }
            if (pos >= text.length() || !isDigit(text.charAt(pos))) {
                throw error(start, lang == Lang.JA ? "数の書き方が正しくありません" : "invalid number format");
            }
            if (text.charAt(pos) == '0') {
                pos++;
            } else {
                while (pos < text.length() && isDigit(text.charAt(pos))) {
                    pos++;
                }
            }
            if (pos < text.length() && text.charAt(pos) == '.') {
                pos++;
                if (pos >= text.length() || !isDigit(text.charAt(pos))) {
                    throw error(start, lang == Lang.JA ? "小数点の後に数字が必要です" : "digit expected after the decimal point");
                }
                while (pos < text.length() && isDigit(text.charAt(pos))) {
                    pos++;
                }
            }
            if (pos < text.length() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
                pos++;
                if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
                    pos++;
                }
                if (pos >= text.length() || !isDigit(text.charAt(pos))) {
                    throw error(start, lang == Lang.JA ? "指数の後に数字が必要です" : "digit expected after the exponent");
                }
                while (pos < text.length() && isDigit(text.charAt(pos))) {
                    pos++;
                }
            }
            String raw = text.substring(start, pos);
            try {
                return new JsonNumber(raw);
            } catch (NumberFormatException e) {
                throw error(start, lang == Lang.JA ? "数として読めません" : "cannot read as a number");
            }
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }
    }

    /**
     * 文字列を JSON の文字列（前後の " 付き）にする。{@code "}・{@code \}・制御文字だけをエスケープする。
     * <p>
     * Converts a string to a JSON string (with surrounding quotes). Escapes only {@code "}, {@code \}, and control
     * characters.
     */
    static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
