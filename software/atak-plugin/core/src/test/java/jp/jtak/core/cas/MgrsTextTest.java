package jp.jtak.core.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * docs/detailed-design/02-cas-brief.md 5.1。MGRS の文字から見えない文字（U+200E・U+200F）を取り除く。
 * <p>
 * docs/detailed-design/02-cas-brief.md §5.1. Removes invisible characters (U+200E and U+200F) from MGRS text.
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class MgrsTextTest {
    @Test
    public void removesLeftToRightMark() {
        // U+200E が 1 つだけ、区域と帯の後ろに入った形。
        // Form with a single U+200E after the zone and band.
        assertEquals("54S UD 11111 22222", MgrsText.clean("54S\u200E UD 11111 22222"));
    }

    @Test
    public void removesMarksInTheFormAtakActuallyReturns() {
        // 実機（ATAK-CIV 5.8.0.9）の返す形: 区域と帯・100 km の区画・東の値の後ろに、
        // U+200E と空白が入る（北の値の後ろには入らない）。
        // Form returned by the device (ATAK-CIV 5.8.0.9): U+200E and a space follow the zone and band, the 100 km
        // square, and the easting (but not the northing).
        assertEquals("54S UD 11111 22222",
                MgrsText.clean("54S\u200E UD\u200E 11111\u200E 22222"));
    }

    @Test
    public void removesRightToLeftMark() {
        assertEquals("54S UD 11111 22222", MgrsText.clean("54S\u200F UD 11111 22222"));
    }

    @Test
    public void removesEveryMarkIncludingRepeatedOnes() {
        assertEquals("54S UD 11111 22222",
                MgrsText.clean("\u200E54S\u200E\u200F UD\u200F 11111\u200E 22222\u200F"));
    }

    @Test
    public void stripsSurroundingWhitespaceIncludingIdeographicSpace() {
        assertEquals("54S UD 11111 22222", MgrsText.clean("  54S UD 11111 22222\n"));
        assertEquals("54S UD 11111 22222", MgrsText.clean("　\u200E54S UD 11111 22222　"));
    }

    @Test
    public void nullStaysNull() {
        assertNull(MgrsText.clean(null));
    }

    @Test
    public void emptyOrWhitespaceOnlyBecomesNull() {
        assertNull(MgrsText.clean(""));
        assertNull(MgrsText.clean("   "));
    }

    @Test
    public void onlyInvisibleCharactersBecomesNull() {
        assertNull(MgrsText.clean("\u200E"));
        assertNull(MgrsText.clean(" \u200E\u200F　"));
    }

    @Test
    public void stringWithoutInvisibleCharactersIsUnchanged() {
        assertEquals("54S UD 11111 22222", MgrsText.clean("54S UD 11111 22222"));
    }
}
