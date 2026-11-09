package jp.jtak.core.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import jp.jtak.core.TrainingNotice;
import org.junit.Test;

/**
 * docs/detailed-design/03-cas-other-products.md 2.4 の D1〜D6・D20・D21（ルーティング）。
 * <p>
 * docs/detailed-design/03-cas-other-products.md §2.4: D1–D6, D20, and D21 (routing).
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class RoutingFormatterTest {
    private static final String T = TrainingNotice.text();
    private static final String TITLE = "ROUTING (JP 3-09.3 2014 Fig. V-3)";

    @Test
    public void d1_maintainWithCallsignAndSend() {
        Routing r = new Routing.Builder()
                .callsign("Razor 53")
                .instruction(RoutingInstruction.MAINTAIN)
                .place("Chevy-Dodge 14-15")
                .closing(RoutingClosing.SEND)
                .build();
        CasProductResult res = RoutingFormatter.format(r);
        String expected = String.join("\n",
                T,
                TITLE,
                "Razor 53, maintain Chevy-Dodge 14-15.",
                "Send your check-in.",
                T);
        assertEquals(expected, res.text());
        assertTrue(res.missingItems().isEmpty());
    }

    @Test
    public void d1b_allFieldsInOrder() {
        Routing r = new Routing.Builder()
                .callsign("Razor 53")
                .instruction(RoutingInstruction.REPORT_PASSING)
                .place("IP Alpha")
                .otherInstructions("Hold east of the road")
                .onlyAircraftOnStation(true)
                .threats("SAM north")
                .weatherTerrain("Ridge 3000 feet")
                .statusCalls("Report at IP")
                .closing(RoutingClosing.SEND)
                .build();
        String expected = String.join("\n",
                T,
                TITLE,
                "Razor 53, report passing IP Alpha.",
                "Other instructions: Hold east of the road",
                "You are the only aircraft on station.",
                "Immediate threats: SAM north",
                "Significant weather/terrain: Ridge 3000 feet",
                "Status calls: Report at IP",
                "Send your check-in.",
                T);
        assertEquals(expected, RoutingFormatter.format(r).text());
    }

    @Test
    public void d1c_otherAosLine() {
        Routing r = new Routing.Builder()
                .instruction(RoutingInstruction.MAINTAIN).place("Alpha")
                .otherAos("Hawg 21 at 15000, 251.0").build();
        String text = RoutingFormatter.format(r).text();
        assertTrue(text, text.contains("\nOther AOS: Hawg 21 at 15000, 251.0\n"));
        assertFalse(text, text.contains("only aircraft"));
    }

    @Test
    public void d2_noCallsignProceedStartsWithCapital() {
        Routing r = new Routing.Builder()
                .instruction(RoutingInstruction.PROCEED_REPORT_ESTABLISHED)
                .place("Chevy-Dodge 14-15")
                .build();
        String text = RoutingFormatter.format(r).text();
        assertTrue(text, text.contains("\nProceed Chevy-Dodge 14-15 and report established.\n"));
        assertEquals(String.join("\n", T, TITLE,
                "Proceed Chevy-Dodge 14-15 and report established.", T), text);
    }

    @Test
    public void d2b_proceedWithCallsignStartsWithLowercase() {
        Routing r = new Routing.Builder().callsign("Razor 53")
                .instruction(RoutingInstruction.PROCEED_REPORT_ESTABLISHED)
                .place("Alpha").build();
        assertTrue(RoutingFormatter.format(r).text()
                .contains("\nRazor 53, proceed Alpha and report established.\n"));
    }

    @Test
    public void d3_missingInstruction() {
        Routing r = new Routing.Builder().callsign("Razor 53").place("Alpha").build();
        CasProductResult res = RoutingFormatter.format(r);
        assertTrue(res.text(), res.text().contains("\nRazor 53, （未入力）\n"));
        assertEquals(Collections.singletonList("Routing/Holding"), res.missingItems());
    }

    @Test
    public void d3b_missingPlaceNoCallsign() {
        Routing r = new Routing.Builder().instruction(RoutingInstruction.MAINTAIN).place("　").build();
        CasProductResult res = RoutingFormatter.format(r);
        assertEquals(String.join("\n", T, TITLE, "（未入力）", T), res.text());
        assertEquals(Collections.singletonList("Routing/Holding"), res.missingItems());
    }

    @Test
    public void d4_onlyAircraftAndOtherAosTogetherIsRejected() {
        try {
            new Routing.Builder().onlyAircraftOnStation(true).otherAos("Hawg 21").build();
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void d4b_onlyAircraftWithBlankOtherAosIsAccepted() {
        Routing r = new Routing.Builder().onlyAircraftOnStation(true).otherAos("　 ").build();
        assertTrue(RoutingFormatter.format(r).text().contains("You are the only aircraft on station."));
    }

    @Test
    public void d5_closings() {
        Routing base = new Routing.Builder().instruction(RoutingInstruction.MAINTAIN).place("A").build();
        assertTrue(text(base, RoutingClosing.HOLD, "attack in progress")
                .contains("\nHold your check-in, attack in progress.\n"));
        String holdNoReason = text(base, RoutingClosing.HOLD, null);
        assertTrue(holdNoReason, holdNoReason.contains("\nHold your check-in.\n"));
        String none = text(base, RoutingClosing.NONE, null);
        assertFalse(none, none.contains("check-in"));
        String nullClosing = text(base, null, null);
        assertFalse(nullClosing, nullClosing.contains("check-in"));
        String send = text(base, RoutingClosing.SEND, "attack in progress");
        assertTrue(send, send.contains("\nSend your check-in.\n"));
        assertFalse(send, send.contains("attack in progress"));
        String noneWithReason = text(base, RoutingClosing.NONE, "attack in progress");
        assertFalse(noneWithReason, noneWithReason.contains("attack in progress"));
    }

    private static String text(Routing base, RoutingClosing closing, String reason) {
        Routing r = new Routing.Builder()
                .instruction(base.instruction()).place(base.place())
                .closing(closing).holdReason(reason).build();
        return RoutingFormatter.format(r).text();
    }

    @Test
    public void d6_blankOptionalLinesAreOmitted() {
        Routing r = new Routing.Builder()
                .instruction(RoutingInstruction.MAINTAIN).place("A")
                .otherInstructions("　").threats("  ").weatherTerrain("\n").statusCalls("　 \n 　")
                .otherAos(null).build();
        String text = RoutingFormatter.format(r).text();
        assertEquals(String.join("\n", T, TITLE, "Maintain A.", T), text);
    }

    @Test
    public void d20_nullArgument() {
        try {
            RoutingFormatter.format(null);
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void d21_valuesAreCleaned() {
        Routing r = new Routing.Builder()
                .callsign("　Razor 53 ")
                .instruction(RoutingInstruction.MAINTAIN)
                .place("  Chevy\nDodge　")
                .threats("SAM\n  north ")
                .build();
        String text = RoutingFormatter.format(r).text();
        assertTrue(text, text.contains("\nRazor 53, maintain Chevy Dodge.\n"));
        assertTrue(text, text.contains("\nImmediate threats: SAM north\n"));
    }

    @Test
    public void startsAndEndsWithTrainingNotice() {
        String text = RoutingFormatter.format(new Routing.Builder().build()).text();
        assertTrue(text.startsWith(T + "\n"));
        assertTrue(text.endsWith("\n" + T));
    }
}
