package jp.jtak.core.weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 味方の点の判定の結果。
 * <p>
 * Result of assessing friendly points.
 */
public final class ProximityResult {
    private final List<ProximityHit> hits;
    private final int unknownCount;

    ProximityResult(List<ProximityHit> hits, int unknownCount) {
        this.hits = Collections.unmodifiableList(new ArrayList<>(hits));
        this.unknownCount = unknownCount;
    }

    /**
     * 範囲の中にある点。距離の近い順。
     * <p>
     * Points within range, sorted by increasing distance.
     */
    public List<ProximityHit> hits() {
        return hits;
    }

    /**
     * 距離を計算できなかった点の数（中にあるとはみなさない）。
     * <p>
     * Number of points whose distances could not be calculated (not considered inside).
     */
    public int unknownCount() {
        return unknownCount;
    }
}
