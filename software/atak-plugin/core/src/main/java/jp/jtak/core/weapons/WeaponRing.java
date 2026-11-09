package jp.jtak.core.weapons;

/**
 * 兵器の表の輪 1 つ（名前と半径）。訓練用サンプル値で、実際の危害半径ではない。
 * <p>
 * One weapon table ring (name and radius). Uses training sample values, not actual hazard radii.
 */
public final class WeaponRing {
    private final String name;
    private final int radiusM;

    public WeaponRing(String name, int radiusM) {
        this.name = name;
        this.radiusM = radiusM;
    }

    public String name() {
        return name;
    }

    /**
     * 半径（m）。
     * <p>
     * Radius (m).
     */
    public int radiusM() {
        return radiusM;
    }
}
