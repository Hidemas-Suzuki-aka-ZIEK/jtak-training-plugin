package jp.jtak.core.cas;

/**
 * 書式の値の整え方（docs/detailed-design/02-cas-brief.md 2.5・03-cas-other-products.md 2.1）。
 * <p>
 * Normalizes format values (docs/detailed-design/02-cas-brief.md §2.5; 03-cas-other-products.md §2.1).
 */
final class CasText {
    static final String MISSING = "（未入力）";

    private CasText() {}

    /**
     * 改行を空白 1 つにし、前後の空白（全角空白 U+3000 を含む）を除く。空なら null。
     * <p>
     * Replaces newlines with a single space and trims leading/trailing whitespace (including full-width space
     * U+3000). Returns null if empty.
     */
    static String clean(String s) {
        if (s == null) {
            return null;
        }
        String t = s.replaceAll("[\\s\\u3000]*[\\r\\n]+[\\s\\u3000]*", " ").strip();
        return t.isEmpty() ? null : t;
    }

    static String orMissing(String v) {
        return v == null ? MISSING : v;
    }

    /**
     * 呼び出し符号（整え済み。null なら付けない）を {@code "Razor 53, "} の形で前に付ける。
     * <p>
     * Prepends a normalized callsign (omitted if null) in the form {@code "Razor 53, "}.
     */
    static String withCallsign(String callsign, String sentence) {
        return callsign == null ? sentence : callsign + ", " + sentence;
    }

    /**
     * 文の頭の 1 文字を小文字にする。
     * <p>
     * Lowercases the first character of the text.
     */
    static String lowerFirst(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * 文の頭の 1 文字を大文字にする。
     * <p>
     * Uppercases the first character of the text.
     */
    static String upperFirst(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
