package jp.jtak.core.lrf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jp.jtak.core.Lang;
import jp.jtak.core.TrainingNotice;
import org.junit.Test;

public class LrfCoreTest {
    @Test
    public void inputLabelRejectsNullLang() {
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> InputSource.MANUAL.label(null)).getMessage());
    }

    @Test
    public void statusMessageRejectsNullLang() {
        RangeReading r = reading(100, 0, AzimuthReference.TRUE, 0);
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> LrfMessages.forStatus(TargetStatus.OK, r, null)).getMessage());
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> LrfMessages.forStatus(null, null, null)).getMessage());
    }

    @Test
    public void remarksRejectNullLang() {
        RangeReading r = reading(100, 0, AzimuthReference.TRUE, 0);
        TargetSolution s = TargetCalculator.solve(new GeoPosition(35, 139, 0), r, 0);
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> LrfRemarks.format(r, s, null)).getMessage());
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> LrfRemarks.format(null, null, null)).getMessage());
    }

    private static final long T0 = 1762569000000L; // 2025-11-08T02:30:00Z 相当（値の検証は別） / Equivalent to 2025-11-08T02:30:00Z (value validation is separate).

    private static RangeReading reading(double d, double az, AzimuthReference ref, double el) {
        return new RangeReading(d, az, ref, el, T0, InputSource.MANUAL, null);
    }

    private static double dms(double sign, double d, double m, double s) {
        return sign * (d + m / 60.0 + s / 3600.0);
    }

    @Test
    public void t1_equatorCardinalDirections() {
        double[][] cases = {
            {0, 0.0090436947696644, 0}, {90, 0, 0.008983152841195215},
            {180, -0.0090436947696644, 0}, {270, 0, -0.008983152841195215}};
        for (double[] c : cases) {
            double[] p = Geodesy.direct(0, 0, c[0], 1000);
            assertEquals("lat az=" + c[0], c[1], p[0], 1e-9);
            assertEquals("lon az=" + c[0], c[2], p[1], 1e-9);
        }
    }

    @Test
    public void t2_vincentyPublishedExample() {
        double lat = dms(-1, 37, 57, 3.72030);
        double lon = dms(1, 144, 25, 29.52440);
        double az = dms(1, 306, 52, 5.37);
        double[] p = Geodesy.direct(lat, lon, az, 54972.271);
        assertEquals(dms(-1, 37, 39, 10.15610), p[0], 1e-6);
        assertEquals(dms(1, 143, 55, 35.38390), p[1], 1e-6);
    }

    @Test
    public void t3_zeroDistance() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 100.0);
        TargetSolution s = TargetCalculator.solve(obs, reading(0, 45, AzimuthReference.TRUE, 10), Double.NaN);
        assertEquals(TargetStatus.OK, s.status());
        assertEquals(35.0, s.target().latitudeDeg(), 1e-12);
        assertEquals(139.0, s.target().longitudeDeg(), 1e-12);
        assertEquals(100.0, s.target().haeMeters(), 1e-12);
    }

    @Test
    public void t4_elevationSplit() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 100.0);
        TargetSolution up = TargetCalculator.solve(obs, reading(1000, 0, AzimuthReference.TRUE, 30), 0);
        TargetSolution down = TargetCalculator.solve(obs, reading(1000, 0, AzimuthReference.TRUE, -30), 0);
        assertEquals(866.0254, up.horizontalDistanceMeters(), 1e-4);
        assertEquals(866.0254, down.horizontalDistanceMeters(), 1e-4);
        assertEquals(600.0, up.target().haeMeters(), 1e-6);
        assertEquals(-400.0, down.target().haeMeters(), 1e-6);
    }

    @Test
    public void t5_declination() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 0);
        assertEquals(5.0, TargetCalculator.solve(obs,
                reading(100, 350, AzimuthReference.MAGNETIC, 0), 15).trueAzimuthDeg(), 1e-9);
        assertEquals(355.0, TargetCalculator.solve(obs,
                reading(100, 10, AzimuthReference.MAGNETIC, 0), -15).trueAzimuthDeg(), 1e-9);
    }

    @Test
    public void t6_trueAzimuthIgnoresDeclination() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 0);
        TargetSolution s = TargetCalculator.solve(obs,
                reading(100, 90, AzimuthReference.TRUE, 0), Double.NaN);
        assertEquals(TargetStatus.OK, s.status());
    }

    @Test
    public void t7_azimuthNaN() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 0);
        TargetSolution s = TargetCalculator.solve(obs,
                reading(100, Double.NaN, AzimuthReference.MAGNETIC, 0), 0);
        assertEquals(TargetStatus.AZIMUTH_UNAVAILABLE, s.status());
        assertNull(s.target());
    }

    @Test
    public void t8_invalidInputs() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 0);
        assertEquals(TargetStatus.NO_OBSERVER, TargetCalculator.solve(null,
                reading(100, 0, AzimuthReference.TRUE, 0), 0).status());
        assertEquals(TargetStatus.INVALID_OBSERVER, TargetCalculator.solve(new GeoPosition(91, 0, 0),
                reading(100, 0, AzimuthReference.TRUE, 0), 0).status());
        assertEquals(TargetStatus.INVALID_DISTANCE, TargetCalculator.solve(obs,
                reading(-1, 0, AzimuthReference.TRUE, 0), 0).status());
        assertEquals(TargetStatus.INVALID_DISTANCE, TargetCalculator.solve(obs,
                reading(Double.NaN, 0, AzimuthReference.TRUE, 0), 0).status());
        assertEquals(TargetStatus.INVALID_ELEVATION, TargetCalculator.solve(obs,
                reading(100, 0, AzimuthReference.TRUE, 91), 0).status());
        assertEquals(TargetStatus.INVALID_DECLINATION, TargetCalculator.solve(obs,
                reading(100, 0, AzimuthReference.MAGNETIC, 0), Double.NaN).status());
        assertEquals(TargetStatus.INVALID_OBSERVER, TargetCalculator.solve(new GeoPosition(35, 181, 0),
                reading(100, 0, AzimuthReference.TRUE, 0), 0).status());
        assertEquals(TargetStatus.INVALID_OBSERVER, TargetCalculator.solve(new GeoPosition(Double.NaN, 139, 0),
                reading(100, 0, AzimuthReference.TRUE, 0), 0).status());
        assertEquals(TargetStatus.INVALID_ELEVATION, TargetCalculator.solve(obs,
                reading(100, 0, AzimuthReference.TRUE, Double.POSITIVE_INFINITY), 0).status());
    }

    @Test
    public void t8b_azimuthNormalized() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 0);
        TargetSolution s = TargetCalculator.solve(obs, reading(100, 370, AzimuthReference.TRUE, 0), 0);
        assertEquals(10.0, s.trueAzimuthDeg(), 1e-9);
        RangeReading r = reading(100, 370, AzimuthReference.MAGNETIC, 0);
        String text = LrfRemarks.format(r, TargetCalculator.solve(obs, r, 0), Lang.JA);
        assertTrue(text.contains("方位: 磁 010.0°"));
        assertEquals(TargetStatus.AZIMUTH_UNAVAILABLE, TargetCalculator.solve(obs,
                reading(100, Double.POSITIVE_INFINITY, AzimuthReference.TRUE, 0), 0).status());
    }

    @Test(expected = IllegalArgumentException.class)
    public void t13_nullAzimuthReferenceRejected() {
        new RangeReading(100, 0, null, 0, T0, InputSource.MANUAL, null);
    }

    @Test
    public void t15_distanceUpperLimit() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 0);
        assertEquals(20000.0, TargetCalculator.MAX_DISTANCE_METERS, 0.0);
        TargetSolution ok = TargetCalculator.solve(obs, reading(20000.0, 0, AzimuthReference.TRUE, 0), 0);
        assertEquals(TargetStatus.OK, ok.status());
        assertNotNull(ok.target());
        TargetSolution over = TargetCalculator.solve(obs, reading(20000.001, 0, AzimuthReference.TRUE, 0), 0);
        assertEquals(TargetStatus.DISTANCE_OUT_OF_RANGE, over.status());
        assertNull(over.target());
        assertEquals(TargetStatus.DISTANCE_OUT_OF_RANGE, TargetCalculator.solve(obs,
                reading(25000.0, 0, AzimuthReference.TRUE, 0), 0).status());
        // 距離が正しくない値は上限より先に INVALID_DISTANCE になる
        // Invalid distance values yield INVALID_DISTANCE before checking the upper limit.
        assertEquals(TargetStatus.INVALID_DISTANCE, TargetCalculator.solve(obs,
                reading(Double.POSITIVE_INFINITY, 0, AzimuthReference.TRUE, 0), 0).status());
    }

    @Test
    public void t16_deviceNameNeverShown() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 0);
        String name = "SecretDevice-X1\nOTHER";
        RangeReading r = new RangeReading(100, 0, AzimuthReference.TRUE, 0, T0, InputSource.ATAK_LRF, name);
        String remarks = LrfRemarks.format(r, TargetCalculator.solve(obs, r, 0), Lang.JA);
        String[] lines = remarks.split("\n");
        assertEquals(10, lines.length);
        assertEquals("入力: ATAK の LRF 連携", lines[2]);
        assertTrue(!remarks.contains("SecretDevice") && !remarks.contains("OTHER"));
        for (TargetStatus st : TargetStatus.values()) {
            String msg = LrfMessages.forStatus(st, r, Lang.JA);
            assertTrue(st.name(), !msg.contains("SecretDevice") && !msg.contains("OTHER"));
        }
    }

    @Test
    public void t14_nullSourceAndNullReadingRejected() {
        try {
            new RangeReading(100, 0, AzimuthReference.TRUE, 0, T0, null, null);
            fail("source が null の RangeReading は作れない");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
        try {
            TargetCalculator.solve(new GeoPosition(35.0, 139.0, 0), null, 0);
            fail("reading が null の solve は IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void t17_azimuthRoundedBeforeWrapping() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 0);
        RangeReading mag = reading(100, 359.96, AzimuthReference.MAGNETIC, 0);
        String magText = LrfRemarks.format(mag, TargetCalculator.solve(obs, mag, 0), Lang.JA);
        assertEquals("方位: 磁 000.0° / 偏差 +0.0° / 真 000.0°", magText.split("\n")[4]);
        assertTrue(!magText.contains("360.0"));
        RangeReading tru = reading(100, 359.96, AzimuthReference.TRUE, 0);
        String trueText = LrfRemarks.format(tru, TargetCalculator.solve(obs, tru, 0), Lang.JA);
        assertEquals("方位: 真 000.0°", trueText.split("\n")[4]);
        assertTrue(!trueText.contains("360.0"));
    }

    @Test
    public void t9_unknownObserverHeight() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, Double.NaN);
        TargetSolution s = TargetCalculator.solve(obs, reading(100, 0, AzimuthReference.TRUE, 10), 0);
        assertEquals(TargetStatus.OK, s.status());
        assertTrue(Double.isNaN(s.target().haeMeters()));
    }

    @Test
    public void t10_straightUpAndDown() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 0);
        for (double el : new double[] {90, -90}) {
            TargetSolution s = TargetCalculator.solve(obs, reading(1000, 45, AzimuthReference.TRUE, el), 0);
            assertEquals(0.0, s.horizontalDistanceMeters(), 1e-9);
            assertEquals(35.0, s.target().latitudeDeg(), 1e-9);
            assertEquals(139.0, s.target().longitudeDeg(), 1e-9);
        }
    }

    @Test
    public void t11_remarks() {
        GeoPosition obs = new GeoPosition(35.0, 139.0, 40.0);
        RangeReading r = new RangeReading(523.0, 90.0, AzimuthReference.MAGNETIC, 5.0,
                1794018600000L, InputSource.ATAK_LRF, "PLRF");
        TargetSolution s = TargetCalculator.solve(obs, r, 7.5);
        String[] lines = LrfRemarks.format(r, s, Lang.JA).split("\n");
        assertEquals(TrainingNotice.text(), lines[0]);
        assertEquals(TrainingNotice.text(), lines[lines.length - 1]);
        assertEquals(10, lines.length);
        assertEquals("JTAK LRF 測距点", lines[1]);
        assertEquals("入力: ATAK の LRF 連携", lines[2]);
        assertEquals("距離: 523.0 m", lines[3]);
        assertEquals("方位: 磁 090.0° / 偏差 +7.5° / 真 097.5°", lines[4]);
        assertEquals("俯仰: +5.0°", lines[5]);
        assertEquals("水平距離: 521.0 m / 高低差: +45.6 m", lines[6]);
        assertEquals("目標: 34.999387, 139.005658 / 標高(HAE): 85.6 m", lines[7]);
        assertEquals("時刻: " + java.time.Instant.ofEpochMilli(1794018600000L), lines[8]);

        RangeReading noAlt = new RangeReading(100, 97.5, AzimuthReference.TRUE, 0, T0,
                InputSource.MANUAL, null);
        TargetSolution s2 = TargetCalculator.solve(new GeoPosition(35.0, 139.0, Double.NaN), noAlt, Double.NaN);
        String text = LrfRemarks.format(noAlt, s2, Lang.JA);
        assertTrue(text.contains("標高(HAE): 不明"));
        assertTrue(text.contains("方位: 真 097.5°"));
        assertTrue(text.contains("入力: 手入力\n"));
        assertNotNull(s2.target());
    }

    @Test
    public void t12_messages() {
        RangeReading r = reading(523.0, 0, AzimuthReference.TRUE, 0);
        for (TargetStatus st : TargetStatus.values()) {
            assertTrue(st.name(), LrfMessages.forStatus(st, r, Lang.JA).startsWith(TrainingNotice.text()));
        }
        assertEquals(TrainingNotice.text() + " — 方位が取れなかったため点を置きません（距離 523.0 m）",
                LrfMessages.forStatus(TargetStatus.AZIMUTH_UNAVAILABLE, r, Lang.JA));
        RangeReading far = reading(25000.0, 0, AzimuthReference.TRUE, 0);
        assertEquals(TrainingNotice.text() + " — 距離が上限 20 km を超えたため点を置きません（距離 25000.0 m）",
                LrfMessages.forStatus(TargetStatus.DISTANCE_OUT_OF_RANGE, far, Lang.JA));
    }

    @Test
    public void englishStatusMessages() {
        RangeReading r = reading(523, 0, AzimuthReference.TRUE, 0);
        String[] bodies = {
            "Point placed", "Point not placed: own position unavailable",
            "Point not placed: own position invalid", "Point not placed: distance invalid",
            "Point not placed: distance exceeds the 20 km limit",
            "Point not placed: elevation angle invalid", "Point not placed: azimuth unavailable",
            "Point not placed: magnetic declination unavailable"
        };
        TargetStatus[] states = {TargetStatus.OK, TargetStatus.NO_OBSERVER, TargetStatus.INVALID_OBSERVER,
            TargetStatus.INVALID_DISTANCE, TargetStatus.DISTANCE_OUT_OF_RANGE, TargetStatus.INVALID_ELEVATION,
            TargetStatus.AZIMUTH_UNAVAILABLE, TargetStatus.INVALID_DECLINATION};
        for (int i = 0; i < states.length; i++) {
            assertEquals(TrainingNotice.text() + " — " + bodies[i] + " (distance 523.0 m)",
                    LrfMessages.forStatus(states[i], r, Lang.EN));
        }
    }

    @Test
    public void englishRemarksAndInputLabels() {
        assertEquals("ATAK LRF link", InputSource.ATAK_LRF.label(Lang.EN));
        assertEquals("BLE direct", InputSource.BLE.label(Lang.EN));
        assertEquals("Manual entry", InputSource.MANUAL.label(Lang.EN));
        GeoPosition obs = new GeoPosition(35, 139, 40);
        RangeReading r = new RangeReading(523, 90, AzimuthReference.MAGNETIC, 5,
                T0, InputSource.ATAK_LRF, "SecretDevice");
        String[] lines = LrfRemarks.format(r, TargetCalculator.solve(obs, r, 7.5), Lang.EN).split("\n");
        assertEquals(10, lines.length);
        assertEquals(TrainingNotice.text(), lines[0]);
        assertEquals(TrainingNotice.text(), lines[9]);
        assertEquals("JTAK LRF range point", lines[1]);
        assertEquals("Input: ATAK LRF link", lines[2]);
        assertEquals("Distance: 523.0 m", lines[3]);
        assertEquals("Azimuth: magnetic 090.0° / declination +7.5° / true 097.5°", lines[4]);
        assertEquals("Elevation angle: +5.0°", lines[5]);
        assertEquals("Horizontal distance: 521.0 m / Height difference: +45.6 m", lines[6]);
        assertEquals("Target: 34.999387, 139.005658 / Altitude (HAE): 85.6 m", lines[7]);
        assertEquals("Time: " + java.time.Instant.ofEpochMilli(T0), lines[8]);
        RangeReading tru = reading(100, 97.5, AzimuthReference.TRUE, 0);
        String text = LrfRemarks.format(tru,
                TargetCalculator.solve(new GeoPosition(35, 139, Double.NaN), tru, Double.NaN), Lang.EN);
        assertTrue(text.startsWith(TrainingNotice.text() + "\n"));
        assertTrue(text.endsWith("\n" + TrainingNotice.text()));
        assertTrue(text.contains("Azimuth: true 097.5°\n"));
        assertTrue(text.contains("Altitude (HAE): unknown\n"));
        assertTrue(text.contains("Input: Manual entry\n"));
    }

}
