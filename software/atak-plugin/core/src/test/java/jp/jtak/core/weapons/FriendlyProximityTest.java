package jp.jtak.core.weapons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * 04 の 2.9 の W11。距離は注入して、境目を正確に試す。
 * <p>
 * 04 §2.9, W11. Inject distances to test boundaries exactly.
 */
public class FriendlyProximityTest {
    private static final WeaponEntry ENTRY = new WeaponEntry("t", "試験",
            Arrays.asList(new WeaponRing("内側", 100), new WeaponRing("外側", 300)), "");
    private static final PointRef TARGET = new PointRef("target", "目標", "a-h-G", 35.0, 139.0, false);

    /**
     * 名前が "d=数値" の点は、その数値を距離として返す。
     * <p>
     * For points named "d=number", returns that number as the distance.
     */
    private static final FriendlyProximity.DistanceFunction BY_NAME = new FriendlyProximity.DistanceFunction() {
        @Override
        public double distanceM(PointRef from, PointRef to) {
            String n = to.name();
            if (n.equals("throw")) {
                throw new IllegalStateException("収束しない");
            }
            if (n.equals("nan")) {
                return Double.NaN;
            }
            return Double.parseDouble(n);
        }
    };

    private static PointRef friend(String uid, String name) {
        return new PointRef(uid, name, "a-f-G", 35.0, 139.0, false);
    }

    private static ProximityResult run(PointRef... points) {
        return FriendlyProximity.check(ENTRY, TARGET, Arrays.asList(points), BY_NAME);
    }

    @Test
    public void w11_friendInInnerRing() {
        ProximityResult r = run(friend("f1", "50"));
        assertEquals(1, r.hits().size());
        ProximityHit h = r.hits().get(0);
        assertEquals("f1", h.uid());
        assertEquals(0, h.ringIndex());
        assertEquals("内側", h.ringName());
        assertEquals(50.0, h.distanceM(), 0.0);
        assertEquals(0, r.unknownCount());
    }

    @Test
    public void w11_friendInOuterRing() {
        ProximityResult r = run(friend("f1", "200"));
        assertEquals(1, r.hits().size());
        assertEquals(1, r.hits().get(0).ringIndex());
        assertEquals("外側", r.hits().get(0).ringName());
    }

    @Test
    public void w11_friendOutsideIsNotListed() {
        ProximityResult r = run(friend("f1", "300.5"), friend("f2", "5000"));
        assertTrue(r.hits().isEmpty());
        assertEquals(0, r.unknownCount());
    }

    @Test
    public void w11_boundaryCountsAsInside() {
        ProximityResult r = run(friend("in", "100"), friend("out", "300"));
        assertEquals(2, r.hits().size());
        assertEquals("in", r.hits().get(0).uid());
        assertEquals(0, r.hits().get(0).ringIndex());
        assertEquals("out", r.hits().get(1).uid());
        assertEquals(1, r.hits().get(1).ringIndex());
    }

    @Test
    public void w11_justOverBoundaryIsOutside() {
        assertTrue(run(friend("f", "300.0001")).hits().isEmpty());
        assertEquals(1, run(friend("f", "100.0001")).hits().get(0).ringIndex());
    }

    @Test
    public void w11_sameUidAsTargetIsSkipped() {
        PointRef same = new PointRef("target", "0", "a-f-G", 35.0, 139.0, false);
        assertTrue(run(same).hits().isEmpty());
    }

    @Test
    public void w11_enemyAndOtherTypesAreNotFriendly() {
        ProximityResult r = run(
                new PointRef("e1", "10", "a-h-G", 35.0, 139.0, false),
                new PointRef("n1", "10", "a-n-G", 35.0, 139.0, false),
                new PointRef("u1", "10", "a-u-G", 35.0, 139.0, false),
                new PointRef("b1", "10", "b-m-p-w", 35.0, 139.0, false));
        assertTrue(r.hits().isEmpty());
    }

    @Test
    public void w11_selfWithEmptyTypeCountsAsFriendly() {
        PointRef self = new PointRef("me", "20", "", 35.0, 139.0, true);
        ProximityResult r = run(self);
        assertEquals(1, r.hits().size());
        assertEquals("me", r.hits().get(0).uid());
    }

    @Test
    public void w11_nullFieldsAreSafe() {
        PointRef self = new PointRef(null, null, null, 35.0, 139.0, true);
        // uid が空でも落ちない（距離は名前から取るので、名前が空だと数にならない点は使わない）
        // Does not fail for an empty uid (distance comes from the name, so points whose empty names cannot be parsed as numbers are not used).
        ProximityResult r = FriendlyProximity.check(ENTRY, TARGET, Arrays.asList(self),
                new FriendlyProximity.DistanceFunction() {
                    @Override
                    public double distanceM(PointRef from, PointRef to) {
                        return 10.0;
                    }
                });
        assertEquals(1, r.hits().size());
        assertEquals("", r.hits().get(0).uid());
    }

    @Test
    public void w11_unknownDistanceIsCountedNotListed() {
        ProximityResult r = run(friend("f1", "throw"), friend("f2", "nan"), friend("f3", "10"));
        assertEquals(1, r.hits().size());
        assertEquals("f3", r.hits().get(0).uid());
        assertEquals(2, r.unknownCount());
    }

    @Test
    public void w11_enemyWithUnknownDistanceIsNotCounted() {
        ProximityResult r = run(new PointRef("e1", "throw", "a-h-G", 35.0, 139.0, false));
        assertEquals(0, r.unknownCount());
    }

    @Test
    public void w11_hitsAreSortedByDistance() {
        ProximityResult r = run(friend("far", "250"), friend("near", "10"), friend("mid", "100"));
        assertEquals(3, r.hits().size());
        assertEquals("near", r.hits().get(0).uid());
        assertEquals("mid", r.hits().get(1).uid());
        assertEquals("far", r.hits().get(2).uid());
    }

    @Test
    public void w11_noPointsGivesEmptyResult() {
        ProximityResult r = FriendlyProximity.check(ENTRY, TARGET, new ArrayList<PointRef>(), BY_NAME);
        assertTrue(r.hits().isEmpty());
        assertEquals(0, r.unknownCount());
    }

    @Test
    public void w11_singleRingEntryUsesThatRing() {
        WeaponEntry one = new WeaponEntry("o", "一", Arrays.asList(new WeaponRing("唯一", 50)), "");
        ProximityResult r = FriendlyProximity.check(one, TARGET,
                Arrays.asList(friend("a", "50"), friend("b", "51")), BY_NAME);
        assertEquals(1, r.hits().size());
        assertEquals("a", r.hits().get(0).uid());
    }

    @Test
    public void w11_realGeodesyDistance() {
        // 緯度 35 度、経度を 0.001 度ずらすと約 91 m（内側の輪の中）。0.01 度なら約 910 m（輪の外）
        // At latitude 35°, a longitude offset of 0.001° is about 91 m (inside the inner ring); 0.01° is about 910 m (outside the ring).
        PointRef near = new PointRef("n", "近", "a-f-G", 35.0, 139.001, false);
        PointRef far = new PointRef("f", "遠", "a-f-G", 35.0, 139.01, false);
        ProximityResult r = FriendlyProximity.check(ENTRY, TARGET, Arrays.asList(near, far));
        assertEquals(1, r.hits().size());
        assertEquals("n", r.hits().get(0).uid());
        assertEquals(0, r.hits().get(0).ringIndex());
        assertEquals(91.0, r.hits().get(0).distanceM(), 2.0);
    }
}
