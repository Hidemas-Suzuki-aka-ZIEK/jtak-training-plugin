package jp.jtak.core.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.CharacterCodingException;
import java.util.List;
import jp.jtak.core.TrainingNotice;
import org.junit.Test;

/**
 * D16: ScenarioLines の単体の試験（設計 06 の 2.3）。
 * <p>
 * D16: ScenarioLines unit tests (design 06 §2.3).
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class ScenarioLinesTest {
    /**
     * 読むときに飛ばす訓練用の行。書き出しが出す文と同じものを使う（設計 06 の 2.3 の 3）。
     * <p>
     * Training-notice lines skipped when reading. Uses the same text emitted by export (design 06 §2.3, item 3).
     */
    private static final String U1 = TrainingNotice.text();

    @Test
    public void d16_BOMを除く() throws Exception {
        byte[] b = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b', 'c'};
        assertEquals("abc", ScenarioLines.decode(b));
    }

    @Test
    public void d16_BOMがなくても同じ() throws Exception {
        assertEquals("abc", ScenarioLines.decode(TestSupport.bytes("abc")));
    }

    @Test(expected = CharacterCodingException.class)
    public void d16_UTF8でないバイト列は例外() throws Exception {
        ScenarioLines.decode(new byte[] {'a', (byte) 0xC3, 0x28});
    }

    @Test
    public void d16_LFとCRLFで行に分ける() {
        List<ScenarioLines.Line> lines = ScenarioLines.split("a\r\nb\nc");
        assertEquals(3, lines.size());
        assertEquals("a", lines.get(0).text());
        assertEquals(1, lines.get(0).number());
        assertEquals("b", lines.get(1).text());
        assertEquals(2, lines.get(1).number());
        assertEquals("c", lines.get(2).text());
        assertEquals(3, lines.get(2).number());
    }

    @Test
    public void d16_CRだけは改行とみなさず値の中のCRは取り除く() {
        List<ScenarioLines.Line> lines = ScenarioLines.split("a\rb\nc");
        assertEquals(2, lines.size());
        assertEquals("ab", lines.get(0).text());
        assertEquals("c", lines.get(1).text());
    }

    @Test
    public void d16_全角の空白も前後から除く() {
        List<ScenarioLines.Line> lines = ScenarioLines.split("　 abc 　");
        assertEquals(1, lines.size());
        assertEquals("abc", lines.get(0).text());
    }

    @Test
    public void d16_空行とU1の行を飛ばし行の番号は残す() {
        List<ScenarioLines.Line> lines = ScenarioLines.split(U1 + "\n\n  \n" + U1 + "\nx\n" + U1);
        assertEquals(1, lines.size());
        assertEquals("x", lines.get(0).text());
        assertEquals(5, lines.get(0).number());
    }

    @Test
    public void d16_U1と少しでも違う行は飛ばさない() {
        List<ScenarioLines.Line> lines = ScenarioLines.split("TRAINING USE ONLY\ntraining use only / 訓練用");
        assertEquals(2, lines.size());
    }

    @Test
    public void d16_値を整える() {
        assertEquals("a", ScenarioLines.clean("  a "));
        assertEquals("a", ScenarioLines.clean("　a　"));
        assertNull(ScenarioLines.clean(""));
        assertNull(ScenarioLines.clean(" 　 "));
        assertNull(ScenarioLines.clean(null));
        assertTrue(ScenarioLines.split("").isEmpty());
    }

    @Test
    public void d16_U1の定数は書き出しの文と同じ() {
        assertEquals(TrainingNotice.text(), TestSupport.U1);
    }

    @Test
    public void d16_コロンで終わるラベルはコロンの直後が空白か行末のときだけ合う() {
        assertEquals("OWL 31", ScenarioLines.after("Other AOS: OWL 31", "Other AOS:"));
        assertEquals("OWL 31", ScenarioLines.after("Other AOS:　OWL 31", "Other AOS:"));
        assertNull(ScenarioLines.after("Other AOS:X", "Other AOS:"));
        assertNull(ScenarioLines.after("Other AOSX", "Other AOS:"));
        assertNull(ScenarioLines.clean(ScenarioLines.after("Other AOS:", "Other AOS:")));
    }
}
