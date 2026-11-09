package jp.jtak.core.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jp.jtak.core.lrf.GeoPosition;
import jp.jtak.core.lrf.Geodesy;
import org.junit.Test;

/**
 * docs/detailed-design/02-cas-brief.md 2.6 の C4〜C10・C19・C20。
 * <p>
 * docs/detailed-design/02-cas-brief.md §2.6: C4–C10, C19, and C20.
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class NineLineCalculatorTest {
    private static double dms(double sign, double d, double m, double s) {
        return sign * (d + m / 60.0 + s / 3600.0);
    }

    private static final GeoPosition FLINDERS =
            new GeoPosition(dms(-1, 37, 57, 3.72030), dms(1, 144, 25, 29.52440), Double.NaN);
    private static final GeoPosition BUNINYONG =
            new GeoPosition(dms(-1, 37, 39, 10.15610), dms(1, 143, 55, 35.38390), Double.NaN);

    @Test
    public void c4_headingFromBearing() {
        assertEquals("360", NineLineCalculator.headingFromBearing(0.2, 0));
        assertEquals("360", NineLineCalculator.headingFromBearing(359.6, 0));
        assertEquals("012", NineLineCalculator.headingFromBearing(5, -7));
        assertNull(NineLineCalculator.headingFromBearing(90, Double.NaN));
    }

    @Test
    public void c4b_headingFromBearingEdges() {
        assertEquals("005", NineLineCalculator.headingFromBearing(5, 0));
        assertEquals("355", NineLineCalculator.headingFromBearing(5, 10));
        assertEquals("090", NineLineCalculator.headingFromBearing(100, 10));
        assertEquals("360", NineLineCalculator.headingFromBearing(10, 10));
        assertNull(NineLineCalculator.headingFromBearing(Double.NaN, 0));
        assertNull(NineLineCalculator.headingFromBearing(Double.POSITIVE_INFINITY, 0));
        assertNull(NineLineCalculator.headingFromBearing(90, Double.POSITIVE_INFINITY));
    }

    @Test
    public void c5_headingSamePointIsNull() {
        assertNull(NineLineCalculator.heading(FLINDERS, FLINDERS, 0));
    }

    @Test
    public void c5b_headingUsesInverseBearing() {
        // 真方位 306.868° から偏差 +10° を引くと磁方位 297°
        // Subtracting declination +10° from true bearing 306.868° gives magnetic bearing 297°.
        assertEquals("297", NineLineCalculator.heading(FLINDERS, BUNINYONG, 10));
        assertEquals("307", NineLineCalculator.heading(FLINDERS, BUNINYONG, 0));
    }

    @Test
    public void c5c_invalidPositionsAreNull() {
        assertNull(NineLineCalculator.heading(null, BUNINYONG, 0));
        assertNull(NineLineCalculator.heading(FLINDERS, null, 0));
        assertNull(NineLineCalculator.heading(new GeoPosition(91, 0, 0), BUNINYONG, 0));
        assertNull(NineLineCalculator.heading(FLINDERS, new GeoPosition(0, 181, 0), 0));
        assertNull(NineLineCalculator.heading(FLINDERS, new GeoPosition(Double.NaN, 0, 0), 0));
        assertNull(NineLineCalculator.distance(null, BUNINYONG, AttackPointType.IP));
        assertNull(NineLineCalculator.distance(FLINDERS, new GeoPosition(0, Double.NaN, 0), AttackPointType.IP));
    }

    @Test
    public void c6_distanceText() {
        assertEquals("29.7 NM", NineLineCalculator.distanceText(54972.271, AttackPointType.IP));
        assertEquals("1200 meters", NineLineCalculator.distanceText(1249, AttackPointType.BP));
        assertEquals("1300 meters", NineLineCalculator.distanceText(1250, AttackPointType.BP));
        assertNull(NineLineCalculator.distanceText(Double.NaN, AttackPointType.IP));
    }

    @Test
    public void c6b_distanceTextEdges() {
        assertEquals("0.0 NM", NineLineCalculator.distanceText(0, AttackPointType.IP));
        assertEquals("0 meters", NineLineCalculator.distanceText(0, AttackPointType.BP));
        assertEquals("1.0 NM", NineLineCalculator.distanceText(1852, AttackPointType.IP));
        assertEquals("10.0 NM", NineLineCalculator.distanceText(18520, AttackPointType.IP));
        assertNull(NineLineCalculator.distanceText(Double.POSITIVE_INFINITY, AttackPointType.BP));
        assertNull(NineLineCalculator.distanceText(-1, AttackPointType.BP));
        assertNull(NineLineCalculator.distanceText(100, null));
    }

    @Test
    public void c7_distanceFromPositions() {
        assertEquals("29.7 NM", NineLineCalculator.distance(FLINDERS, BUNINYONG, AttackPointType.IP));
        assertEquals("55000 meters", NineLineCalculator.distance(FLINDERS, BUNINYONG, AttackPointType.BP));
    }

    @Test
    public void c8_elevationFeet() {
        assertEquals("1250 feet", NineLineCalculator.elevationFeet(381.0));
        assertEquals("0 feet", NineLineCalculator.elevationFeet(0));
        assertNull(NineLineCalculator.elevationFeet(Double.NaN));
        assertNull(NineLineCalculator.elevationFeet(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void c9_friendlies() {
        GeoPosition target = new GeoPosition(0, 0, Double.NaN);
        double[] south = Geodesy.direct(0, 0, 180, 300);
        double[] east = Geodesy.direct(0, 0, 90, 500);
        List<GeoPosition> list = Arrays.asList(
                new GeoPosition(east[0], east[1], Double.NaN),
                new GeoPosition(south[0], south[1], Double.NaN));
        assertEquals("South 300", NineLineCalculator.friendlies(target, list));
        assertNull(NineLineCalculator.friendlies(target, Collections.<GeoPosition>emptyList()));
    }

    @Test
    public void c9b_friendliesSkipsInvalidAndNull() {
        GeoPosition target = new GeoPosition(0, 0, Double.NaN);
        double[] west = Geodesy.direct(0, 0, 270, 120);
        List<GeoPosition> list = new ArrayList<>();
        list.add(null);
        list.add(new GeoPosition(95, 0, 0));
        list.add(new GeoPosition(west[0], west[1], Double.NaN));
        assertEquals("West 120", NineLineCalculator.friendlies(target, list));
        assertNull(NineLineCalculator.friendlies(target, null));
        assertNull(NineLineCalculator.friendlies(null, list));
        assertNull(NineLineCalculator.friendlies(target, Arrays.asList(new GeoPosition(95, 0, 0))));
    }

    @Test
    public void c9c_friendlyAtTargetHasNoDirection() {
        GeoPosition target = new GeoPosition(0, 0, Double.NaN);
        assertNull(NineLineCalculator.friendlies(target, Arrays.asList(new GeoPosition(0, 0, Double.NaN))));
    }

    @Test
    public void c19_antipodalPointsGiveNull() {
        GeoPosition a = new GeoPosition(0, 0, Double.NaN);
        GeoPosition b = new GeoPosition(0, 180, Double.NaN);
        assertNull(NineLineCalculator.heading(a, b, 0));
        assertNull(NineLineCalculator.distance(a, b, AttackPointType.IP));
    }

    @Test
    public void c20_friendlyAtTargetMakesResultNull() {
        GeoPosition target = new GeoPosition(0, 0, Double.NaN);
        double[] south = Geodesy.direct(0, 0, 180, 300);
        List<GeoPosition> list = Arrays.asList(
                new GeoPosition(0, 0, Double.NaN),
                new GeoPosition(south[0], south[1], Double.NaN));
        assertNull(NineLineCalculator.friendlies(target, list));
        // 順番を変えても同じ
        // Same result after reordering.
        assertNull(NineLineCalculator.friendlies(target, Arrays.asList(list.get(1), list.get(0))));
    }

    @Test
    public void c10_cardinalFromTrueBearing() {
        assertEquals(Cardinal.NORTH, Cardinal.fromTrueBearing(0));
        assertEquals(Cardinal.NORTH, Cardinal.fromTrueBearing(22.4));
        assertEquals(Cardinal.NORTHEAST, Cardinal.fromTrueBearing(22.5));
        assertEquals(Cardinal.EAST, Cardinal.fromTrueBearing(67.5));
        assertEquals(Cardinal.SOUTH, Cardinal.fromTrueBearing(180));
        assertEquals(Cardinal.NORTH, Cardinal.fromTrueBearing(337.5));
        assertEquals(Cardinal.NORTH, Cardinal.fromTrueBearing(360));
        assertEquals(Cardinal.NORTHWEST, Cardinal.fromTrueBearing(-45));
    }

    @Test
    public void c10b_cardinalRemaining() {
        assertEquals(Cardinal.SOUTHEAST, Cardinal.fromTrueBearing(135));
        assertEquals(Cardinal.SOUTHWEST, Cardinal.fromTrueBearing(225));
        assertEquals(Cardinal.WEST, Cardinal.fromTrueBearing(270));
        assertNull(Cardinal.fromTrueBearing(Double.NaN));
        assertNull(Cardinal.fromTrueBearing(Double.POSITIVE_INFINITY));
        assertEquals("Northeast", Cardinal.NORTHEAST.word());
        assertEquals("South", Cardinal.SOUTH.word());
    }
}
