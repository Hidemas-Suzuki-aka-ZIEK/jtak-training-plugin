package jp.jtak.core.cas;

/**
 * 9-line の 9 行の値（文字列。空か null は未入力）と、2 行目に付ける offset。不変。
 * 値の前後の空白や改行の整理は {@link CasBriefFormatter} が行う。
 * <p>
 * Values for the nine lines of a 9-line (strings; empty or null means missing), plus offset for line 2. Immutable.
 * {@link CasBriefFormatter} handles leading/trailing whitespace and newlines.
 */
public final class NineLine {
    private final String[] lines;
    private final String offset;

    private NineLine(String[] lines, String offset) {
        this.lines = lines;
        this.offset = offset;
    }

    /**
     * n 行目（1〜9）の値。未入力は null か空。範囲外は {@link IllegalArgumentException}。
     * <p>
     * Value of line n (1–9). Null or empty if missing. Throws {@link IllegalArgumentException} if out of range.
     */
    public String line(int n) {
        if (n < 1 || n > 9) {
            throw new IllegalArgumentException("行の番号は 1〜9: " + n);
        }
        return lines[n - 1];
    }

    /**
     * 2 行目に付ける offset（例 {@code left}）。空か null なら付けない。
     * <p>
     * Offset appended to line 2 (for example, {@code left}). Omitted if empty or null.
     */
    public String offset() {
        return offset;
    }

    public static final class Builder {
        private final String[] lines = new String[9];
        private String offset;

        public Builder line1(String v) {
            lines[0] = v;
            return this;
        }

        public Builder line2(String v) {
            lines[1] = v;
            return this;
        }

        public Builder line3(String v) {
            lines[2] = v;
            return this;
        }

        public Builder line4(String v) {
            lines[3] = v;
            return this;
        }

        public Builder line5(String v) {
            lines[4] = v;
            return this;
        }

        public Builder line6(String v) {
            lines[5] = v;
            return this;
        }

        public Builder line7(String v) {
            lines[6] = v;
            return this;
        }

        public Builder line8(String v) {
            lines[7] = v;
            return this;
        }

        public Builder line9(String v) {
            lines[8] = v;
            return this;
        }

        public Builder offset(String v) {
            offset = v;
            return this;
        }

        public NineLine build() {
            return new NineLine(lines.clone(), offset);
        }
    }
}
