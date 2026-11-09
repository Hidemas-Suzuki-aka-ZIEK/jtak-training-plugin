package jp.jtak.core.cas;

/**
 * MGRS の文字列を整える（02 5.1）。ATAK-CIV 5.8.0.9 の MGRS の文字列には、空白の前に
 * 見えない文字 U+200E（LEFT-TO-RIGHT MARK）が入る。コピーや書き出しに混じらないよう、
 * U+200E と U+200F（RIGHT-TO-LEFT MARK）を取り除く。
 * <p>
 * Normalizes MGRS strings (02 §5.1). ATAK-CIV 5.8.0.9 MGRS strings contain the invisible character U+200E
 * (LEFT-TO-RIGHT MARK) before spaces. Removes U+200E and U+200F (RIGHT-TO-LEFT MARK) so they do not appear in
 * copied or exported text.
 */
public final class MgrsText {
    private static final char LEFT_TO_RIGHT_MARK = '\u200E';
    private static final char RIGHT_TO_LEFT_MARK = '\u200F';

    private MgrsText() {}

    /**
     * U+200E と U+200F をすべて取り除き、前後の空白（全角空白を含む）を取る。
     * null は null、空になったら null を返す。
     * <p>
     * Removes all U+200E and U+200F characters and trims leading/trailing whitespace (including full-width
     * spaces). Returns null for null input or an empty result.
     */
    public static String clean(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != LEFT_TO_RIGHT_MARK && c != RIGHT_TO_LEFT_MARK) {
                sb.append(c);
            }
        }
        // String.strip() は Android の古い版にないため、Character.isWhitespace（全角空白 U+3000 を含む）で自分で取る。
        // String.strip() is unavailable on older Android versions, so trim using Character.isWhitespace (including full-width space U+3000).
        int start = 0;
        int end = sb.length();
        while (start < end && Character.isWhitespace(sb.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(sb.charAt(end - 1))) {
            end--;
        }
        return start == end ? null : sb.substring(start, end);
    }
}
