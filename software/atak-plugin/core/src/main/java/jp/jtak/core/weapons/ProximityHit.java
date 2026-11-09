package jp.jtak.core.weapons;

/**
 * 範囲の中にあった味方の点 1 つ。
 * <p>
 * One friendly point found within a range.
 */
public final class ProximityHit {
    private final String uid;
    private final String name;
    private final int ringIndex;
    private final String ringName;
    private final double distanceM;

    ProximityHit(String uid, String name, int ringIndex, String ringName, double distanceM) {
        this.uid = uid;
        this.name = name;
        this.ringIndex = ringIndex;
        this.ringName = ringName;
        this.distanceM = distanceM;
    }

    public String uid() {
        return uid;
    }

    public String name() {
        return name;
    }

    /**
     * 点が入るいちばん内側の輪の番号（0 から）。
     * <p>
     * Index of the innermost ring containing the point (starting at 0).
     */
    public int ringIndex() {
        return ringIndex;
    }

    public String ringName() {
        return ringName;
    }

    /**
     * 目標からの距離（m）。
     * <p>
     * Distance from target (m).
     */
    public double distanceM() {
        return distanceM;
    }
}
