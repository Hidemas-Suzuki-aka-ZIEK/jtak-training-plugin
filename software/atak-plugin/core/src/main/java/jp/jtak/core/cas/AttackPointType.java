package jp.jtak.core.cas;

/**
 * 9-line の 1 行目の点の種類。3 行目の距離の単位が変わる（IP は NM、BP は m）。
 * <p>
 * Point type for line 1 of the 9-line. Determines the distance unit in line 3 (NM for IP, m for BP).
 */
public enum AttackPointType {
    IP,
    BP
}
