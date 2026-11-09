package jp.jtak.core.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import jp.jtak.core.TrainingNotice;
import org.junit.Test;

/**
 * docs/detailed-design/03-cas-other-products.md 2.4 の D7〜D11・D20・D21（チェックイン）。
 * <p>
 * docs/detailed-design/03-cas-other-products.md §2.4: D7–D11, D20, and D21 (check-in).
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class CheckInFormatterTest {
    private static final String T = TrainingNotice.text();
    private static final String TITLE = "CHECK-IN (JP 3-09.3 2014 Fig. V-4)";

    private static CheckIn.Builder full() {
        return new CheckIn.Builder()
                .jtacCallsign("Hawg 11").aircraftCallsign("Razor 53")
                .missionNumber("1234").aircraft("2 by F/A-18C")
                .positionAltitude("Alpha, angels 15").ordnance("4x BOMB-A")
                .playtime("30 minutes").capabilities("Sensor A, Link-16")
                .abortCode("Bravo");
    }

    @Test
    public void d7_fullWithAllItems() {
        CasProductResult res = CheckInFormatter.format(full().build());
        String expected = String.join("\n",
                T,
                TITLE,
                "Hawg 11, this is Razor 53",
                "1 Mission number: 1234",
                "2 Number and type of aircraft: 2 by F/A-18C",
                "3 Position and altitude: Alpha, angels 15",
                "4 Ordnance: 4x BOMB-A",
                "5 Playtime: 30 minutes",
                "6 Capabilities: Sensor A, Link-16",
                "7 Abort code: Bravo (readback)",
                T);
        assertEquals(expected, res.text());
        assertTrue(res.missingItems().isEmpty());
    }

    @Test
    public void d8_fullWithMissingItems() {
        CheckIn c = full().positionAltitude("").playtime(null).build();
        CasProductResult res = CheckInFormatter.format(c);
        assertTrue(res.text(), res.text().contains("\n3 Position and altitude: （未入力）\n"));
        assertTrue(res.text(), res.text().contains("\n5 Playtime: （未入力）\n"));
        assertEquals(Arrays.asList("3 Position and altitude", "5 Playtime"), res.missingItems());
    }

    @Test
    public void d8b_fullWithMissingAbortCodeAndDefaultMode() {
        CheckIn c = new CheckIn.Builder().jtacCallsign("Hawg 11").aircraftCallsign("Razor 53").build();
        assertEquals(CheckInMode.FULL, c.mode());
        CasProductResult res = CheckInFormatter.format(c);
        assertEquals(Arrays.asList("1 Mission number", "2 Number and type of aircraft",
                "3 Position and altitude", "4 Ordnance", "5 Playtime", "6 Capabilities",
                "7 Abort code"), res.missingItems());
        assertTrue(res.text(), res.text().contains("\n7 Abort code: （未入力）\n"));
    }

    @Test
    public void d9_asFraggedShowsOnlyEnteredItems() {
        CheckIn c = new CheckIn.Builder().jtacCallsign("Hawg 11").aircraftCallsign("Razor 53")
                .mode(CheckInMode.AS_FRAGGED).ordnance("4x BOMB-A").build();
        CasProductResult res = CheckInFormatter.format(c);
        assertEquals(String.join("\n", T, TITLE, "Hawg 11, this is Razor 53", "As fragged.",
                "4 Ordnance: 4x BOMB-A", T), res.text());
        assertTrue(res.missingItems().isEmpty());
    }

    @Test
    public void d9b_withExceptionShowsOnlyEnteredItems() {
        CheckIn c = new CheckIn.Builder().jtacCallsign("Hawg 11").aircraftCallsign("Razor 53")
                .mode(CheckInMode.WITH_EXCEPTION).ordnance("4x BOMB-A").build();
        CasProductResult res = CheckInFormatter.format(c);
        assertEquals(String.join("\n", T, TITLE, "Hawg 11, this is Razor 53",
                "As fragged, with exception:", "4 Ordnance: 4x BOMB-A", T), res.text());
        assertTrue(res.missingItems().isEmpty());
    }

    @Test
    public void d9c_asFraggedStillRequiresCallsigns() {
        CheckIn c = new CheckIn.Builder().mode(CheckInMode.AS_FRAGGED).build();
        CasProductResult res = CheckInFormatter.format(c);
        assertEquals(Arrays.asList("JTAC callsign", "Aircraft callsign"), res.missingItems());
        assertTrue(res.text(), res.text().contains("\n（未入力）, this is （未入力）\n"));
    }

    @Test
    public void d10_abortCodeNone() {
        CheckIn c = full().abortCode(null).abortCodeNone(true).build();
        CasProductResult res = CheckInFormatter.format(c);
        assertTrue(res.text(), res.text()
                .contains("\n7 Abort code: none (standard ABORT, ABORT, ABORT)\n"));
        assertFalse(res.text(), res.text().contains("none (standard ABORT, ABORT, ABORT) (readback)"));
        assertTrue(res.missingItems().isEmpty());
    }

    @Test
    public void d10b_abortCodeAndNoneTogetherIsRejected() {
        try {
            full().abortCode("Bravo").abortCodeNone(true).build();
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void d10c_abortCodeNoneInAsFraggedIsShown() {
        CheckIn c = new CheckIn.Builder().jtacCallsign("Hawg 11").aircraftCallsign("Razor 53")
                .mode(CheckInMode.AS_FRAGGED).abortCodeNone(true).build();
        assertTrue(CheckInFormatter.format(c).text()
                .contains("\n7 Abort code: none (standard ABORT, ABORT, ABORT)\n"));
    }

    @Test
    public void d11_missingAircraftCallsign() {
        CasProductResult res = CheckInFormatter.format(full().aircraftCallsign("　").build());
        assertTrue(res.text(), res.text().contains("\nHawg 11, this is （未入力）\n"));
        assertEquals(Collections.singletonList("Aircraft callsign"), res.missingItems());
    }

    @Test
    public void d11b_missingJtacCallsign() {
        CasProductResult res = CheckInFormatter.format(full().jtacCallsign(null).build());
        assertTrue(res.text(), res.text().contains("\n（未入力）, this is Razor 53\n"));
        assertEquals(Collections.singletonList("JTAC callsign"), res.missingItems());
    }

    @Test
    public void d20_nullArgument() {
        try {
            CheckInFormatter.format(null);
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void d21_valuesAreCleaned() {
        CheckIn c = full().missionNumber("　12\n34 ").aircraft("2 by\nF/A-18C　").build();
        String text = CheckInFormatter.format(c).text();
        assertTrue(text, text.contains("\n1 Mission number: 12 34\n"));
        assertTrue(text, text.contains("\n2 Number and type of aircraft: 2 by F/A-18C\n"));
    }
}
