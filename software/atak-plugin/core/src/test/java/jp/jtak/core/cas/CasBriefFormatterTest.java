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
 * docs/detailed-design/02-cas-brief.md 2.6 の C11〜C18・C21。
 * <p>
 * docs/detailed-design/02-cas-brief.md §2.6: C11–C18 and C21.
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class CasBriefFormatterTest {
    private static final String T = TrainingNotice.text();

    private static NineLine.Builder full() {
        return new NineLine.Builder()
                .line1("Hawk").line2("306").line3("29.7 NM")
                .line4("1250 feet").line5("Two trucks").line6("54H VE 12345 67890")
                .line7("Laser 1688").line8("South 300").line9("Egress North to Moon");
    }

    private static CasRemarks emptyRemarks() {
        return new CasRemarks.Builder().build();
    }

    @Test
    public void c11_fullBrief() {
        CasRemarks remarks = new CasRemarks.Builder().ltlPtl("LTL 270").build();
        CasBriefResult r = CasBriefFormatter.format(full().build(), remarks);
        String expected = String.join("\n",
                T,
                "CAS 9-LINE (JP 3-09.3 2014 Fig. V-9) / Datum: WGS 84",
                "[1-3]",
                "1 IP/BP: Hawk",
                "2 Heading: 306",
                "3 Distance: 29.7 NM",
                "[4-6]",
                "4 Target elevation: 1250 feet (readback)",
                "5 Target description: Two trucks",
                "6 Target location: 54H VE 12345 67890 (readback)",
                "[7-9]",
                "7 Type mark / Terminal guidance: Laser 1688",
                "8 Location of friendlies: South 300",
                "9 Egress: Egress North to Moon",
                "[Remarks]",
                "LTL/PTL: LTL 270",
                T);
        assertEquals(expected, r.text());
        assertTrue(r.missingLines().isEmpty());
    }

    @Test
    public void c11b_allRemarksInOrder() {
        CasRemarks remarks = new CasRemarks.Builder()
                .additionalCalls("Abort code").additionalRemarks("Weather clear")
                .threat("SAM north").ordnance("2x BOMB-A").ltlPtl("LTL 270").build();
        String text = CasBriefFormatter.format(full().build(), remarks).text();
        String tail = String.join("\n",
                "[Remarks]",
                "LTL/PTL: LTL 270",
                "Ordnance / Effects: 2x BOMB-A",
                "Threat / SEAD: SAM north",
                "Additional remarks: Weather clear",
                "Additional calls: Abort code",
                T);
        assertTrue(text, text.endsWith(tail));
        assertTrue(text.startsWith(T + "\n"));
    }

    @Test
    public void c12_missingLines() {
        NineLine lines = full().line5("").line8(null).build();
        CasBriefResult r = CasBriefFormatter.format(lines, emptyRemarks());
        assertEquals(Arrays.asList(5, 8), r.missingLines());
        assertEquals(2, count(r.text(), "（未入力）"));
        assertTrue(r.text().contains("5 Target description: （未入力）"));
        assertTrue(r.text().contains("8 Location of friendlies: （未入力）"));
    }

    @Test
    public void c12b_missingLinesAreUnmodifiable() {
        CasBriefResult r = CasBriefFormatter.format(full().line5("").build(), emptyRemarks());
        try {
            r.missingLines().add(1);
            fail("変えられないリストのはず");
        } catch (UnsupportedOperationException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void c13_lines1to3Omitted() {
        NineLine lines = full().line1(null).line2("").line3("  ").build();
        CasBriefResult r = CasBriefFormatter.format(lines, emptyRemarks());
        assertFalse(r.text().contains("[1-3]"));
        assertFalse(r.text().contains("1 IP/BP"));
        assertFalse(r.text().contains("2 Heading"));
        assertFalse(r.text().contains("3 Distance"));
        assertTrue(r.text().contains("4 Target elevation: Elevation 1250 feet (readback)"));
        assertTrue(r.missingLines().isEmpty());
        assertTrue(r.text().startsWith(T + "\nCAS 9-LINE (JP 3-09.3 2014 Fig. V-9) / Datum: WGS 84\n[4-6]\n"));
    }

    @Test
    public void c13b_lines1to3PartiallyFilled() {
        NineLine lines = full().line1("").line3("").build();
        CasBriefResult r = CasBriefFormatter.format(lines, emptyRemarks());
        assertTrue(r.text().contains("[1-3]"));
        assertEquals(Arrays.asList(1, 3), r.missingLines());
        assertTrue(r.text().contains("4 Target elevation: 1250 feet (readback)"));
        assertFalse(r.text().contains("Elevation 1250"));
    }

    @Test
    public void c13c_omittedHeaderWithMissingLine4() {
        NineLine lines = full().line1(null).line2(null).line3(null).line4(null).build();
        CasBriefResult r = CasBriefFormatter.format(lines, emptyRemarks());
        assertEquals(Collections.singletonList(4), r.missingLines());
        assertTrue(r.text().contains("4 Target elevation: （未入力） (readback)"));
    }

    @Test
    public void c14_egressPrefix() {
        String a = CasBriefFormatter.format(full().line9("North to Moon").build(), emptyRemarks()).text();
        assertTrue(a, a.contains("9 Egress: Egress North to Moon\n"));
        String b = CasBriefFormatter.format(full().line9("egress North").build(), emptyRemarks()).text();
        assertTrue(b, b.contains("9 Egress: egress North\n"));
        assertFalse(b, b.contains("Egress egress"));
    }

    @Test
    public void c15_offset() {
        NineLine withOffset = full().line2("306").offset("left").build();
        String a = CasBriefFormatter.format(withOffset, emptyRemarks()).text();
        assertTrue(a, a.contains("2 Heading: 306, Offset left\n"));

        NineLine noHeading = full().line2(null).offset("left").build();
        CasBriefResult r = CasBriefFormatter.format(noHeading, emptyRemarks());
        assertTrue(r.text(), r.text().contains("2 Heading: （未入力）\n"));
        assertFalse(r.text(), r.text().contains("Offset"));
        assertEquals(Collections.singletonList(2), r.missingLines());
    }

    @Test
    public void c16_noRemarks() {
        String text = CasBriefFormatter.format(full().build(), emptyRemarks()).text();
        assertFalse(text.contains("[Remarks]"));
        assertTrue(text.endsWith("9 Egress: Egress North to Moon\n" + T));
    }

    @Test
    public void c17_whitespaceAndNewlines() {
        NineLine lines = full().line5("  Two\ntrucks\r\n  near  road \n").build();
        CasRemarks remarks = new CasRemarks.Builder().threat("  SAM-1\nnorth ").build();
        String text = CasBriefFormatter.format(lines, remarks).text();
        assertTrue(text, text.contains("5 Target description: Two trucks near  road\n"));
        assertTrue(text, text.contains("Threat / SEAD: SAM-1 north\n"));
    }

    @Test
    public void c21_fullWidthSpaceIsWhitespace() {
        String a = CasBriefFormatter.format(full().line9("　Egress North").build(), emptyRemarks()).text();
        assertTrue(a, a.contains("9 Egress: Egress North\n"));
        assertFalse(a, a.contains("Egress 　"));

        CasBriefResult r = CasBriefFormatter.format(full().line5("　　").build(), emptyRemarks());
        assertEquals(Collections.singletonList(5), r.missingLines());
        assertTrue(r.text(), r.text().contains("5 Target description: （未入力）"));

        // 改行のまわりの全角空白も 1 つの空白にまとまる
        // Full-width spaces around a newline also collapse to a single space.
        String c = CasBriefFormatter.format(full().line5("Two　\n　trucks").build(), emptyRemarks()).text();
        assertTrue(c, c.contains("5 Target description: Two trucks\n"));
    }

    @Test
    public void c18_nullArguments() {
        try {
            CasBriefFormatter.format(null, emptyRemarks());
            fail("null の lines は IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
        try {
            CasBriefFormatter.format(full().build(), null);
            fail("null の remarks は IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void trainingNoticeAlwaysFirstAndLast() {
        CasBriefResult r = CasBriefFormatter.format(new NineLine.Builder().build(), emptyRemarks());
        assertTrue(r.text().startsWith(T + "\n"));
        assertTrue(r.text().endsWith("\n" + T));
        assertEquals(Arrays.asList(4, 5, 6, 7, 8, 9), r.missingLines());
    }

    private static int count(String text, String part) {
        int n = 0;
        int from = 0;
        while ((from = text.indexOf(part, from)) >= 0) {
            n++;
            from += part.length();
        }
        return n;
    }
}
