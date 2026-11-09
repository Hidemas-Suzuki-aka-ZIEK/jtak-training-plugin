package jp.jtak.core.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import jp.jtak.core.TrainingNotice;
import org.junit.Test;

/**
 * docs/detailed-design/03-cas-other-products.md 2.4 の D15〜D21（Restrictions と 3 つの引数の format）。
 * <p>
 * docs/detailed-design/03-cas-other-products.md §2.4: D15–D21 (Restrictions and the three-argument format).
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class CasRestrictionsTest {
    private static final String T = TrainingNotice.text();

    private static NineLine full() {
        return new NineLine.Builder()
                .line1("Hawk").line2("306").line3("29.7 NM")
                .line4("1250 feet").line5("Two trucks").line6("54H VE 12345 67890")
                .line7("Laser 1688").line8("South 300").line9("Egress North to Moon").build();
    }

    private static CasRemarks remarks() {
        return new CasRemarks.Builder().ltlPtl("LTL 270").build();
    }

    private static String brief(CasRestrictions r) {
        return CasBriefFormatter.format(full(), remarks(), r).text();
    }

    @Test
    public void d15_allItemsWithTimeTotAreInOrderAfterRemarks() {
        CasRestrictions r = new CasRestrictions.Builder()
                .dangerClose(true).dangerCloseInitials("AB")
                .acas("ACA Alpha").fahs("270-290").separation("500 m lateral")
                .rwOutOfBp("approved").pla("PLA point Gold")
                .tot(TotType.TIME).totTime("1430").build();
        CasBriefResult res = CasBriefFormatter.format(full(), remarks(), r);
        String tail = String.join("\n",
                "[Remarks]",
                "LTL/PTL: LTL 270",
                "[Restrictions]",
                "Danger close, initials AB (readback)",
                "ACAs: ACA Alpha (readback)",
                "FAHs: 270-290 (readback)",
                "Separation: 500 m lateral (readback)",
                "RW approval out of BP: approved (readback)",
                "PLA: PLA point Gold (readback)",
                "TOT 1430 (readback)",
                T);
        assertTrue(res.text(), res.text().endsWith(tail));
        assertTrue(res.text().startsWith(T + "\n"));
        assertTrue(res.missingRestrictions().isEmpty());
        assertTrue(res.missingLines().isEmpty());
    }

    @Test
    public void d15b_restrictionsAfterOmittedRemarks() {
        CasRestrictions r = new CasRestrictions.Builder().acas("ACA Alpha").build();
        String text = CasBriefFormatter.format(full(), new CasRemarks.Builder().build(), r).text();
        assertFalse(text, text.contains("[Remarks]"));
        assertTrue(text, text.endsWith("9 Egress: Egress North to Moon\n[Restrictions]\n"
                + "ACAs: ACA Alpha (readback)\n" + T));
    }

    @Test
    public void d15c_onlyEnteredItemsAreShown() {
        CasRestrictions r = new CasRestrictions.Builder().fahs("270-290").pla("　").build();
        String text = brief(r);
        assertTrue(text, text.contains("[Restrictions]\nFAHs: 270-290 (readback)\n" + T));
        assertFalse(text, text.contains("PLA"));
        assertFalse(text, text.contains("ACAs"));
    }

    @Test
    public void d16_totKinds() {
        assertTrue(totLine(TotType.STANDBY).contains("\nStandby TOT (readback)\n"));
        assertTrue(totLine(TotType.AFTER_CORRELATION)
                .contains("\nTOT will be issued after correlation (readback)\n"));
        assertTrue(totLine(TotType.PUSH_WHEN_READY).contains("\nPush when ready (readback)\n"));
    }

    private static String totLine(TotType t) {
        return brief(new CasRestrictions.Builder().tot(t).build());
    }

    @Test
    public void d17_timeWithoutTotTime() {
        CasRestrictions r = new CasRestrictions.Builder().tot(TotType.TIME).totTime("　").build();
        CasBriefResult res = CasBriefFormatter.format(full(), remarks(), r);
        assertTrue(res.text(), res.text().contains("\nTOT （未入力） (readback)\n"));
        assertEquals(Collections.singletonList("TOT"), res.missingRestrictions());
    }

    @Test
    public void d17b_totTimeWithOtherKindIsRejected() {
        try {
            new CasRestrictions.Builder().tot(TotType.STANDBY).totTime("1430").build();
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void d17c_totTimeWithoutKindIsRejected() {
        try {
            new CasRestrictions.Builder().totTime("1430").build();
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void d18_dangerCloseWithoutInitials() {
        CasRestrictions r = new CasRestrictions.Builder().dangerClose(true).build();
        CasBriefResult res = CasBriefFormatter.format(full(), remarks(), r);
        assertTrue(res.text(), res.text().contains("\nDanger close, initials （未入力） (readback)\n"));
        assertEquals(Collections.singletonList("Danger close initials"), res.missingRestrictions());
    }

    @Test
    public void d18b_initialsWithoutDangerCloseIsRejected() {
        try {
            new CasRestrictions.Builder().dangerCloseInitials("AB").build();
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void d18c_blankInitialsWithoutDangerCloseIsAccepted() {
        CasRestrictions r = new CasRestrictions.Builder().dangerCloseInitials("　 ").build();
        assertFalse(brief(r).contains("[Restrictions]"));
    }

    @Test
    public void d18d_missingRestrictionsInOrderAndUnmodifiable() {
        CasRestrictions r = new CasRestrictions.Builder().dangerClose(true).tot(TotType.TIME).build();
        CasBriefResult res = CasBriefFormatter.format(full(), remarks(), r);
        assertEquals(java.util.Arrays.asList("Danger close initials", "TOT"), res.missingRestrictions());
        try {
            res.missingRestrictions().add("x");
            fail("変えられないリストのはず");
        } catch (UnsupportedOperationException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void d19_emptyRestrictionsEqualTwoArgumentFormat() {
        CasBriefResult two = CasBriefFormatter.format(full(), remarks());
        CasBriefResult three = CasBriefFormatter.format(full(), remarks(), new CasRestrictions.Builder().build());
        assertEquals(two.text(), three.text());
        assertFalse(three.text().contains("[Restrictions]"));
        assertTrue(three.missingRestrictions().isEmpty());
        assertTrue(two.missingRestrictions().isEmpty());
        assertEquals(two.missingLines(), three.missingLines());
    }

    @Test
    public void d20_nullArguments() {
        CasRestrictions empty = new CasRestrictions.Builder().build();
        try {
            CasBriefFormatter.format(null, remarks(), empty);
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
        try {
            CasBriefFormatter.format(full(), null, empty);
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
        try {
            CasBriefFormatter.format(full(), remarks(), null);
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void d21_valuesAreCleaned() {
        CasRestrictions r = new CasRestrictions.Builder()
                .dangerClose(true).dangerCloseInitials("　A\nB ")
                .acas("ACA\n Alpha　").tot(TotType.TIME).totTime(" 14\n30 ").build();
        String text = brief(r);
        assertTrue(text, text.contains("\nDanger close, initials A B (readback)\n"));
        assertTrue(text, text.contains("\nACAs: ACA Alpha (readback)\n"));
        assertTrue(text, text.contains("\nTOT 14 30 (readback)\n"));
    }
}
