package jp.jtak.core.weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.jtak.core.lrf.Geodesy;

/**
 * 目標の周りの範囲の中に味方の点があるかを調べる（04 の 2.7）。
 * <p>
 * Checks for friendly points within the ranges around a target (04 §2.7).
 */
public final class FriendlyProximity {
    private FriendlyProximity() {}

    /**
     * 2 点の距離（m）を返す。計算できないときは {@link IllegalStateException} を投げてよい。
     * <p>
     * Returns the distance between two points (m). May throw {@link IllegalStateException} if it cannot be
     * calculated.
     */
    public interface DistanceFunction {
        double distanceM(PointRef from, PointRef to);
    }

    private static final DistanceFunction GEODESY = new DistanceFunction() {
        @Override
        public double distanceM(PointRef from, PointRef to) {
            return Geodesy.inverse(from.lat(), from.lon(), to.lat(), to.lon())[0];
        }
    };

    public static ProximityResult check(WeaponEntry entry, PointRef target, List<PointRef> points) {
        return check(entry, target, points, GEODESY);
    }

    public static ProximityResult check(
            WeaponEntry entry, PointRef target, List<PointRef> points, DistanceFunction distance) {
        List<WeaponRing> rings = entry.rings();
        int outer = rings.get(rings.size() - 1).radiusM();
        List<ProximityHit> hits = new ArrayList<>();
        int unknown = 0;
        for (PointRef p : points) {
            if (!isFriendly(p) || p.uid().equals(target.uid())) {
                continue;
            }
            double d;
            try {
                d = distance.distanceM(target, p);
            } catch (IllegalStateException e) {
                unknown++;
                continue;
            }
            if (Double.isNaN(d)) {
                unknown++;
                continue;
            }
            if (d > outer) {
                continue; // 境目（距離 = 半径）は中に入れる / Include the boundary (distance = radius).
            }
            for (int i = 0; i < rings.size(); i++) {
                if (d <= rings.get(i).radiusM()) {
                    hits.add(new ProximityHit(p.uid(), p.name(), i, rings.get(i).name(), d));
                    break;
                }
            }
        }
        Collections.sort(hits, new Comparator<ProximityHit>() {
            @Override
            public int compare(ProximityHit a, ProximityHit b) {
                return Double.compare(a.distanceM(), b.distanceM());
            }
        });
        return new ProximityResult(hits, unknown);
    }

    private static boolean isFriendly(PointRef p) {
        return p.self() || p.cotType().startsWith("a-f-");
    }
}
