package jp.jtak.core.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * docs/detailed-design/02-cas-brief.md 2.3.1・2.6 の C24〜C28。5 行目（Target description）の下書き。
 * <p>
 * docs/detailed-design/02-cas-brief.md §§2.3.1, 2.6: C24–C28. Draft for line 5 (Target description).
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class TargetDescriptionDraftTest {
    /**
     * C24: 2.3.1 の表の 27 行。
     * <p>
     * C24: The 27 rows of the table in §2.3.1.
     */
    @Test
    public void c24_everyRowOfTheTableGivesItsWord() {
        String[][] table = {
            // 陸上の部隊（E Change 1 の TABLE A-XXIII）
            // Ground units (E Change 1, TABLE A-XXIII)
            {"a-h-G-U-C", "Combat"},
            {"a-h-G-U-C-I", "Infantry"},
            {"a-h-G-U-C-A", "Armor/Mechanized"},
            {"a-h-G-U-C-A-A", "Antitank/Antiarmor"},
            {"a-h-G-U-C-R", "Reconnaissance/Cavalry/Scout"},
            {"a-h-G-U-C-D", "Air Defense"},
            {"a-h-G-U-C-F", "Field Artillery"},
            {"a-h-G-U-C-F-M", "Mortar"},
            {"a-h-G-U-C-E", "Engineer"},
            // 陸上の装備（E Change 1 の TABLE A-XXIX）
            // Ground equipment (E Change 1, TABLE A-XXIX)
            {"a-h-G-E-W", "Weapon/Weapon System"},
            {"a-h-G-E-W-R-R", "Rifle"},
            {"a-h-G-E-W-R-L", "Machine Gun"},
            {"a-h-G-E-W-R-H", "Machine Gun"},
            {"a-h-G-E-W-A", "Air Defense Gun"},
            {"a-h-G-E-W-G", "Antitank Gun"},
            {"a-h-G-E-W-H", "Howitzer"},
            {"a-h-G-E-W-M", "Missile Launcher"},
            {"a-h-G-E-W-M-T", "Antitank Missile Launcher"},
            {"a-h-G-E-W-O", "Mortar"},
            {"a-h-G-E-W-X", "Multiple Rocket Launcher"},
            {"a-h-G-E-V", "Vehicle"},
            {"a-h-G-E-V-A", "Armored"},
            {"a-h-G-E-V-A-A", "Armored Personnel Carrier"},
            {"a-h-G-E-V-A-T", "Tank"},
            {"a-h-G-E-V-E", "Engineer Vehicle and Equipment"},
            {"a-h-G-E-V-U", "Utility Vehicle"},
            {"a-h-G-E-V-C", "Civilian Vehicle"},
        };
        assertEquals(27, table.length);
        assertEquals(27, TargetDescriptionDraft.rowCount());
        for (String[] row : table) {
            assertEquals(row[0], row[1], TargetDescriptionDraft.fromCotType(row[0]));
        }
    }

    /**
     * C25: 長く合う行を選ぶ・前後の空白を取る。
     * <p>
     * C25: Select the longest matching row and trim leading/trailing whitespace.
     */
    @Test
    public void c25_longestRowWinsAndSurroundingWhitespaceIsIgnored() {
        assertEquals("Infantry", TargetDescriptionDraft.fromCotType("a-h-G-U-C-I-d"));
        assertEquals("Tank", TargetDescriptionDraft.fromCotType("a-h-G-E-V-A-T-x"));
        assertEquals("Infantry", TargetDescriptionDraft.fromCotType(" a-h-G-U-C-I "));
        // 全角空白で囲んだ入力（Character.isWhitespace の範囲。MgrsText.clean と同じ）。
        // Input surrounded by full-width spaces (within Character.isWhitespace; same as MgrsText.clean).
        assertEquals("Infantry", TargetDescriptionDraft.fromCotType("\u3000a-h-G-U-C-I\u3000"));
        // 末尾が - の type は段が 1 つ増えるだけで、Infantry に当たる。
        // A type ending in - simply adds one level and still matches Infantry.
        assertEquals("Infantry", TargetDescriptionDraft.fromCotType("a-h-G-U-C-I-"));
        // 空の段も 1 つの段として比べる。I の段が空なので Combat に戻る。
        // Compare an empty level as a level too. The I level is empty, so fall back to Combat.
        assertEquals("Combat", TargetDescriptionDraft.fromCotType("a-h-G-U-C--I"));
    }

    /**
     * C26: 字の途中では当てない・表にない字は短い行へ戻る。
     * <p>
     * C26: Do not match part of a character sequence; fall back to a shorter row for unlisted characters.
     */
    @Test
    public void c26_doesNotMatchInTheMiddleOfASegment() {
        assertEquals("Combat", TargetDescriptionDraft.fromCotType("a-h-G-U-C-IX"));
        assertEquals("Weapon/Weapon System", TargetDescriptionDraft.fromCotType("a-h-G-E-W-R"));
    }

    /**
     * C27: 敵（a-h-）の陸上（G）で表に当たるものだけ。
     * <p>
     * C27: Only hostile (a-h-) ground (G) types matching the table.
     */
    @Test
    public void c27_nonHostileNonGroundOrUnlistedGivesNull() {
        String[] types = {
            "a-f-G-U-C-I", // 味方 / Friendly
            "a-n-G-U-C-I", // 中立 / Neutral
            "a-u-G-U-C-I", // 不明 / Unknown
            "a-h-A-M-F", // 航空 / Air
            "a-h-G",
            "a-h-G-U",
            "a-h-G-I", // 表にない / Unlisted
        };
        for (String type : types) {
            assertNull(type, TargetDescriptionDraft.fromCotType(type));
        }
    }

    /**
     * C28: null・空・空白だけ・大文字小文字の違いは当てない。
     * <p>
     * C28: Do not match null, empty, whitespace-only, or differently cased input.
     */
    @Test
    public void c28_nullEmptyBlankAndWrongCaseGiveNull() {
        assertNull(TargetDescriptionDraft.fromCotType(null));
        assertNull(TargetDescriptionDraft.fromCotType(""));
        assertNull(TargetDescriptionDraft.fromCotType("   "));
        assertNull(TargetDescriptionDraft.fromCotType("A-H-G-U-C-I"));
    }
}
