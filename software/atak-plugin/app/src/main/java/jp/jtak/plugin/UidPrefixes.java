package jp.jtak.plugin;

/**
 * JTAK が地図に置くものの UID の頭。複数の場所で同じ値を使うので、ここに 1 つだけ置く。
 * <p>
 * UID prefixes for items JTAK places on the map. Defined only here because multiple locations use the same values.
 */
public final class UidPrefixes {
    /**
     * 兵器の範囲の円（04 3.4）の UID の頭。
     * 円を作る {@code WeaponCircles} と、円の部品を目標の候補から外す {@code MapPointLister} が使う。
     * <p>
     * UID prefix for weapon effect circles (04 §3.4). Used by {@code WeaponCircles}, which creates circles, and
     * {@code MapPointLister}, which excludes circle components from target candidates.
     */
    public static final String WEAPON_CIRCLE = "jtak-wpn-";

    private UidPrefixes() {}
}
