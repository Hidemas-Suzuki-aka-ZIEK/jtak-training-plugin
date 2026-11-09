package jp.jtak.core.weapons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.jtak.core.Lang;
import org.junit.Test;

/**
 * 04 の 2.9 の W9・W10。
 * <p>
 * 04 §2.9: W9 and W10.
 */
public class WeaponEntryFormTest {
    @Test
    public void entryFormRejectsNullLang() {
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> WeaponEntryForm.toEntry("row-1", "Sample", a("Inner"), a("100"), "", null))
                .getMessage());
    }

    private static String[] a(String... v) {
        return v;
    }

    private static WeaponEntryForm.FormResult make(String label, String[] names, String[] radii, String note) {
        return WeaponEntryForm.toEntry("row-1", label, names, radii, note, Lang.JA);
    }

    private static void assertHasError(WeaponEntryForm.FormResult r, String part) {
        assertFalse(r.ok());
        assertNull(r.entry());
        for (String e : r.errors()) {
            if (e.contains(part)) {
                return;
            }
        }
        throw new AssertionError("「" + part + "」を含む理由がない: " + r.errors());
    }

    // ---- W9 ----

    @Test
    public void w9_fullThreeRingsPass() {
        WeaponEntryForm.FormResult r = make("甲", a("赤", "橙", "黄"), a("10", "20", "30"), "メモ");
        assertTrue(r.errors().toString(), r.ok());
        WeaponEntry e = r.entry();
        assertEquals("row-1", e.id());
        assertEquals("甲", e.label());
        assertEquals("メモ", e.note());
        assertEquals(3, e.rings().size());
        assertEquals("橙", e.rings().get(1).name());
        assertEquals(20, e.rings().get(1).radiusM());
    }

    @Test
    public void w9_emptySlotsAreDropped() {
        WeaponEntryForm.FormResult r = make("甲", a("内", "", ""), a("100", "", ""), "");
        assertTrue(r.errors().toString(), r.ok());
        assertEquals(1, r.entry().rings().size());
        // 真ん中の枠が空でもよい
        // The middle slot may be empty.
        WeaponEntryForm.FormResult mid = make("甲", a("内", "", "外"), a("100", "", "200"), "");
        assertTrue(mid.errors().toString(), mid.ok());
        assertEquals(2, mid.entry().rings().size());
        assertEquals("外", mid.entry().rings().get(1).name());
    }

    @Test
    public void w9_whitespaceOnlySlotCountsAsEmpty() {
        WeaponEntryForm.FormResult r = make("甲", a("内", "  ", null), a("100", "   ", null), "");
        assertTrue(r.errors().toString(), r.ok());
        assertEquals(1, r.entry().rings().size());
    }

    @Test
    public void w9_valuesAreTrimmed() {
        WeaponEntryForm.FormResult r = make("  甲  ", a("  内  "), a("  100  "), "  メモ  ");
        assertTrue(r.errors().toString(), r.ok());
        assertEquals("甲", r.entry().label());
        assertEquals("内", r.entry().rings().get(0).name());
        assertEquals(100, r.entry().rings().get(0).radiusM());
        assertEquals("メモ", r.entry().note());
    }

    @Test
    public void w9_nullNoteBecomesEmpty() {
        WeaponEntryForm.FormResult r = make("甲", a("内"), a("100"), null);
        assertTrue(r.ok());
        assertEquals("", r.entry().note());
    }

    @Test
    public void w9_halfFilledSlotIsError() {
        assertHasError(make("甲", a("内", "外"), a("100", ""), ""), "輪 2: 名前と距離の両方を入れてください");
        assertHasError(make("甲", a("内", ""), a("100", "200"), ""), "輪 2: 名前と距離の両方を入れてください");
    }

    @Test
    public void w9_radiusMustBeAsciiDigits() {
        String msg = "距離は 1 から 50000 までの整数（m）にしてください";
        assertHasError(make("甲", a("内"), a("abc"), ""), "輪 1: " + msg);
        assertHasError(make("甲", a("内"), a("12.5"), ""), "輪 1: " + msg);
        assertHasError(make("甲", a("内"), a("-5"), ""), "輪 1: " + msg);
        assertHasError(make("甲", a("内"), a("+5"), ""), "輪 1: " + msg);
        assertHasError(make("甲", a("内"), a("1e2"), ""), "輪 1: " + msg);
        assertHasError(make("甲", a("内"), a("１００"), ""), "輪 1: " + msg); // 全角 / Full-width
        assertHasError(make("甲", a("内"), a("1 00"), ""), "輪 1: " + msg);
    }

    @Test
    public void w9_radiusRange() {
        String msg = "距離は 1 から 50000 までの整数（m）にしてください";
        assertHasError(make("甲", a("内"), a("0"), ""), msg);
        assertHasError(make("甲", a("内"), a("50001"), ""), msg);
        assertHasError(make("甲", a("内"), a("99999999999"), ""), msg);
        assertTrue(make("甲", a("内"), a("1"), "").ok());
        assertTrue(make("甲", a("内"), a("50000"), "").ok());
        assertTrue(make("甲", a("内"), a("0100"), "").ok());
        assertEquals(100, make("甲", a("内"), a("0100"), "").entry().rings().get(0).radiusM());
    }

    @Test
    public void w9_radiiMustGrow() {
        assertHasError(make("甲", a("内", "外"), a("100", "100"), ""), "輪 2: 距離は前の輪より大きくしてください");
        assertHasError(make("甲", a("内", "外"), a("100", "50"), ""), "輪 2: 距離は前の輪より大きくしてください");
        assertHasError(make("甲", a("a", "b", "c"), a("10", "20", "15"), ""), "輪 3: 距離は前の輪より大きくしてください");
    }

    @Test
    public void w9_growthIsCheckedAcrossEmptySlots() {
        assertHasError(make("甲", a("内", "", "外"), a("100", "", "50"), ""), "輪 3: 距離は前の輪より大きくしてください");
    }

    @Test
    public void w9_noRingIsError() {
        assertHasError(make("甲", a("", "", ""), a("", "", ""), ""), "輪を 1 つ以上入れてください");
        assertHasError(make("甲", a(), a(), ""), "輪を 1 つ以上入れてください");
        assertHasError(make("甲", null, null, ""), "輪を 1 つ以上入れてください");
    }

    @Test
    public void w9_labelRules() {
        assertHasError(make("", a("内"), a("1"), ""), "ラベル: 1 から 40 文字にしてください");
        assertHasError(make("   ", a("内"), a("1"), ""), "ラベル: 1 から 40 文字にしてください");
        assertHasError(make(null, a("内"), a("1"), ""), "ラベル: 1 から 40 文字にしてください");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 41; i++) {
            sb.append("😀");
        }
        assertHasError(make(sb.toString(), a("内"), a("1"), ""), "ラベル: 1 から 40 文字にしてください");
        assertTrue(make(sb.substring(0, 80), a("内"), a("1"), "").ok());
    }

    @Test
    public void w9_noteLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 201; i++) {
            sb.append('あ');
        }
        assertHasError(make("甲", a("内"), a("1"), sb.toString()), "メモ: 200 文字までにしてください");
        assertTrue(make("甲", a("内"), a("1"), sb.substring(0, 200)).ok());
    }

    @Test
    public void w9_ringNameLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 21; i++) {
            sb.append('あ');
        }
        assertHasError(make("甲", a(sb.toString()), a("1"), ""), "輪 1: 名前は 20 文字までにしてください");
        assertTrue(make("甲", a(sb.substring(0, 20)), a("1"), "").ok());
    }

    @Test
    public void w9_severalErrorsAreCollected() {
        WeaponEntryForm.FormResult r = make("", a("内", "外"), a("x", ""), "");
        assertFalse(r.ok());
        assertEquals(3, r.errors().size());
        assertTrue(r.errors().get(0).startsWith("ラベル: "));
        assertTrue(r.errors().get(1).startsWith("輪 1: "));
        assertTrue(r.errors().get(2).startsWith("輪 2: "));
    }

    @Test
    public void w9_readableRadiusIsComparedEvenWhenNameHasError() {
        // 輪 1 は 100、輪 2 は名前が長すぎるエラーで 300、輪 3 は 200。輪 2 と輪 3 のエラーが 1 回で両方出る（04 2.5 の 6）
        // Ring 1 is 100; ring 2 is 300 with a name-too-long error; ring 3 is 200. Both ring 2 and ring 3 errors are reported in one pass (04 §2.5, item 6).
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 21; i++) {
            longName.append('あ');
        }
        WeaponEntryForm.FormResult r = make("甲", a("内", longName.toString(), "外"), a("100", "300", "200"), "");
        assertFalse(r.ok());
        assertEquals(2, r.errors().size());
        assertTrue(r.errors().toString(), r.errors().get(0).startsWith("輪 2: 名前は 20 文字までにしてください"));
        assertTrue(r.errors().toString(), r.errors().get(1).startsWith("輪 3: 距離は前の輪より大きくしてください"));
    }

    @Test
    public void w9_madeEntryPassesCodecValidation() {
        WeaponEntryForm.FormResult r = make("甲", a("赤", "橙", "黄"), a("10", "20", "30"), "メモ");
        assertTrue(WeaponTableCodec.validate(new WeaponTable(Arrays.asList(r.entry())), Lang.JA).isEmpty());
    }

    @Test
    public void w9_ringSlotsIsThree() {
        assertEquals(3, WeaponEntryForm.RING_SLOTS);
        assertEquals(WeaponTableCodec.MAX_RINGS, WeaponEntryForm.RING_SLOTS);
    }

    // ---- W10 ----

    private static WeaponTable tableWithIds(String... ids) {
        List<WeaponEntry> list = new ArrayList<>();
        for (String id : ids) {
            list.add(new WeaponEntry(id, "x", Arrays.asList(new WeaponRing("内", 1)), ""));
        }
        return new WeaponTable(list);
    }

    @Test
    public void w10_nextIdStartsAtRow1() {
        assertEquals("row-1", WeaponEntryForm.nextId(DefaultWeaponTable.table(Lang.JA)));
        assertEquals("row-1", WeaponEntryForm.nextId(tableWithIds("a", "b")));
        assertEquals("row-1", WeaponEntryForm.nextId(new WeaponTable(new ArrayList<WeaponEntry>())));
    }

    @Test
    public void w10_nextIdSkipsUsedNumbers() {
        assertEquals("row-2", WeaponEntryForm.nextId(tableWithIds("row-1")));
        assertEquals("row-3", WeaponEntryForm.nextId(tableWithIds("row-1", "row-2")));
    }

    @Test
    public void w10_nextIdFillsTheSmallestGap() {
        assertEquals("row-1", WeaponEntryForm.nextId(tableWithIds("row-2", "row-3")));
        assertEquals("row-2", WeaponEntryForm.nextId(tableWithIds("row-1", "row-3")));
    }

    @Test
    public void w10_nextIdIgnoresOtherIdShapes() {
        assertEquals("row-1", WeaponEntryForm.nextId(tableWithIds("row-01", "row-x", "myrow-1")));
    }

    @Test
    public void englishFormErrors() {
        assertHasError(WeaponEntryForm.toEntry("row-1", "", a("Inner"), a("100"), "", Lang.EN),
                "Label: use 1 to 40 characters");
        assertHasError(WeaponEntryForm.toEntry("row-1", "Sample", a("Inner"), a("0"), "", Lang.EN),
                "Ring 1: the distance must be a whole number from 1 to 50000 (m)");
        assertHasError(WeaponEntryForm.toEntry("row-1", "Sample", a(), a(), "", Lang.EN),
                "Enter at least one ring");
        assertHasError(WeaponEntryForm.toEntry("row-1", "Sample", a("Inner"), a(""), "", Lang.EN),
                "Ring 1: enter both the name and the distance");
        assertHasError(WeaponEntryForm.toEntry("row-1", "Sample", a("Inner", "Outer"), a("100", "100"), "", Lang.EN),
                "Ring 2: the distance must be larger than the previous ring");
    }

}
