package jp.jtak.core.importer;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import jp.jtak.core.TrainingNotice;

/**
 * バイト列を行に分け、値を整える部品（設計 06 の 2.3 の 1〜3・6）。
 * <p>
 * Components for splitting a byte sequence into lines and normalizing values (design 06 §2.3, items 1–3 and 6).
 */
final class ScenarioLines {
    /**
     * 項目に値が書かれていないことを表す文字（formatter が出す）。
     * <p>
     * Text indicating an item has no value (emitted by the formatter).
     */
    static final String NOT_ENTERED = "（未入力）";

    private ScenarioLines() {}

    /**
     * 1 行。{@code number} はファイルの中の行の番号（1 から。飛ばした行も数える）。
     * <p>
     * One line. {@code number} is the line number in the file (starting at 1, counting skipped lines).
     */
    static final class Line {
        private final int number;
        private final String text;

        Line(int number, String text) {
            this.number = number;
            this.text = text;
        }

        int number() {
            return number;
        }

        String text() {
            return text;
        }
    }

    /**
     * UTF-8 で読む。壊れた並びは例外にする。先頭の BOM は除く。
     * <p>
     * Reads UTF-8. Throws on malformed sequences. Removes a leading BOM.
     */
    static String decode(byte[] data) throws CharacterCodingException {
        String s = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(data))
                .toString();
        return s.startsWith("﻿") ? s.substring(1) : s;
    }

    /**
     * LF・CRLF で行に分け、前後の空白を除く。空行と訓練用の行は飛ばす（行の番号は残す）。
     * <p>
     * Splits on LF or CRLF and trims leading/trailing whitespace. Skips empty and training-notice lines (retaining
     * line numbers).
     */
    static List<Line> split(String text) {
        List<Line> out = new ArrayList<>();
        String[] raw = text.split("\n", -1);
        for (int i = 0; i < raw.length; i++) {
            String t = trim(raw[i].replace("\r", ""));
            if (t.isEmpty() || TrainingNotice.text().equals(t)) {
                continue;
            }
            out.add(new Line(i + 1, t));
        }
        return out;
    }

    /**
     * 値を整える。前後の空白を除き、空なら null。
     * <p>
     * Normalizes a value by trimming leading/trailing whitespace. Returns null if empty.
     */
    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String t = trim(value);
        return t.isEmpty() ? null : t;
    }

    /**
     * {@code text} が {@code prefix} で始まれば、残りを整えて返す（空なら ""）。始まらなければ null。
     * {@code prefix} が ':' で終わるときは、そのあとが空白か行末のときだけ合うとする（値が空の行は前後の空白を除いてあるため）。
     * <p>
     * If {@code text} starts with {@code prefix}, normalizes and returns the remainder ("" if empty). Otherwise
     * returns null. If {@code prefix} ends with ':', matches only if followed by whitespace or the end of the line
     * (because lines with empty values have already been trimmed).
     */
    static String after(String text, String prefix) {
        if (!text.startsWith(prefix)) {
            return null;
        }
        if (prefix.endsWith(":") && text.length() > prefix.length() && !isSpace(text.charAt(prefix.length()))) {
            return null;
        }
        return trim(text.substring(prefix.length()));
    }

    /**
     * 末尾の {@code (readback)} を 1 つ除く（なくてもよい）。
     * <p>
     * Removes one trailing {@code (readback)}, if present.
     */
    static String withoutReadback(String value) {
        String suffix = " (readback)";
        String v = trim(value);
        if (v.endsWith(suffix)) {
            return trim(v.substring(0, v.length() - suffix.length()));
        }
        return v;
    }

    private static String trim(String s) {
        int a = 0;
        int b = s.length();
        while (a < b && isSpace(s.charAt(a))) {
            a++;
        }
        while (b > a && isSpace(s.charAt(b - 1))) {
            b--;
        }
        return s.substring(a, b);
    }

    private static boolean isSpace(char c) {
        return Character.isWhitespace(c) || c == '　';
    }
}
