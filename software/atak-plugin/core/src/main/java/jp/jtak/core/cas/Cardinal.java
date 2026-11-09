package jp.jtak.core.cas;

/**
 * 8 方位。9-line の 8 行目（味方の方角）で使う。
 * <p>
 * Eight compass directions. Used in line 8 of the 9-line (direction of friendly forces).
 */
public enum Cardinal {
    NORTH("North"),
    NORTHEAST("Northeast"),
    EAST("East"),
    SOUTHEAST("Southeast"),
    SOUTH("South"),
    SOUTHWEST("Southwest"),
    WEST("West"),
    NORTHWEST("Northwest");

    private final String word;

    Cardinal(String word) {
        this.word = word;
    }

    /**
     * 読み上げ用の英語の語（例: {@code South}）。
     * <p>
     * English term for reading aloud (for example, {@code South}).
     */
    public String word() {
        return word;
    }

    /**
     * 真北から時計回りの角度（度）を 8 方位にする。北は [337.5, 360) と [0, 22.5)、以下 45° ごと。
     * 入力は [0, 360) に直してから使う。NaN・無限大は null。
     * <p>
     * Converts a clockwise angle from true north (degrees) to one of eight compass directions. North covers
     * [337.5, 360) and [0, 22.5), with subsequent sectors every 45°. Normalizes input to [0, 360) before use.
     * Returns null for NaN or infinity.
     */
    public static Cardinal fromTrueBearing(double deg) {
        if (Double.isNaN(deg) || Double.isInfinite(deg)) {
            return null;
        }
        double d = ((deg % 360.0) + 360.0) % 360.0;
        int index = (int) Math.floor((d + 22.5) / 45.0) % 8;
        return values()[index];
    }
}
