package jp.jtak.core.lrf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * docs/detailed-design/02-cas-brief.md 2.6 の C1〜C3・C19。
 * <p>
 * docs/detailed-design/02-cas-brief.md §2.6: C1–C3 and C19.
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class GeodesyInverseTest {
    private static double dms(double sign, double d, double m, double s) {
        return sign * (d + m / 60.0 + s / 3600.0);
    }

    @Test
    public void c1_vincentyPublishedExample() {
        double lat1 = dms(-1, 37, 57, 3.72030);
        double lon1 = dms(1, 144, 25, 29.52440);
        double lat2 = dms(-1, 37, 39, 10.15610);
        double lon2 = dms(1, 143, 55, 35.38390);
        double[] r = Geodesy.inverse(lat1, lon1, lat2, lon2);
        assertEquals(54972.271, r[0], 1e-3);
        assertEquals(dms(1, 306, 52, 5.37), r[1], 1e-5);
    }

    @Test
    public void c2_equatorCardinalDirections() {
        double[] north = Geodesy.inverse(0, 0, 0.0090436947696644, 0);
        assertEquals(1000.0, north[0], 1e-6);
        assertEquals(0.0, north[1], 1e-6);
        double[] east = Geodesy.inverse(0, 0, 0, 0.008983152841195215);
        assertEquals(1000.0, east[0], 1e-6);
        assertEquals(90.0, east[1], 1e-6);
    }

    @Test
    public void c2b_southAndWestStayInRange() {
        double[] south = Geodesy.inverse(0, 0, -0.0090436947696644, 0);
        assertEquals(180.0, south[1], 1e-6);
        double[] west = Geodesy.inverse(0, 0, 0, -0.008983152841195215);
        assertEquals(270.0, west[1], 1e-6);
        assertTrue(west[1] >= 0.0 && west[1] < 360.0);
    }

    @Test
    public void c3_samePoint() {
        double[] r = Geodesy.inverse(35.0, 139.0, 35.0, 139.0);
        assertEquals(0.0, r[0], 0.0);
        assertTrue(Double.isNaN(r[1]));
    }

    @Test
    public void c19_antipodalDoesNotConverge() {
        try {
            Geodesy.inverse(0, 0, 0, 180);
            fail("対蹠点は収束せず IllegalStateException のはず");
        } catch (IllegalStateException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void inverseIsConsistentWithDirect() {
        double[] p = Geodesy.direct(35.0, 139.0, 123.0, 20000.0);
        double[] r = Geodesy.inverse(35.0, 139.0, p[0], p[1]);
        assertEquals(20000.0, r[0], 1e-6);
        assertEquals(123.0, r[1], 1e-6);
    }

    @Test
    public void acrossAntimeridian() {
        double[] p = Geodesy.direct(10.0, 179.99, 90.0, 5000.0);
        double[] r = Geodesy.inverse(10.0, 179.99, p[0], p[1]);
        assertEquals(5000.0, r[0], 1e-6);
        assertEquals(90.0, r[1], 1e-6);
    }
}
