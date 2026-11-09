package jp.jtak.core.weapons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.jtak.core.Lang;
import org.junit.Test;

/**
 * 04 の 2.9 の W1〜W8。
 * <p>
 * 04 §2.9: W1–W8.
 */
public class WeaponTableCodecTest {
    @Test
    public void defaultTableRejectsNullLang() {
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> DefaultWeaponTable.table(null)).getMessage());
    }

    @Test
    public void codecParseRejectsNullLang() {
        byte[] utf8 = b(docOf(okEntry()));
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> WeaponTableCodec.parse(utf8, null)).getMessage());
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> WeaponTableCodec.parse(null, null)).getMessage());
    }

    @Test
    public void codecValidateRejectsNullLang() {
        WeaponTable table = DefaultWeaponTable.table(Lang.JA);
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> WeaponTableCodec.validate(table, null)).getMessage());
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> WeaponTableCodec.validate(null, null)).getMessage());
    }

    @Test
    public void jsonParseRejectsNullLang() {
        byte[] utf8 = b(docOf(okEntry()));
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> WeaponJson.parse(utf8, null)).getMessage());
        assertEquals("lang", assertThrows(NullPointerException.class,
                () -> WeaponJson.parse(null, null)).getMessage());
    }

    // ---- 試験用の JSON を作る補助 ----
    // ---- Helpers for creating test JSON ----

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String ring(String name, String radius) {
        return "{\"name\":\"" + name + "\",\"radiusM\":" + radius + "}";
    }

    private static String entry(String id, String label, String rings, String extra) {
        return "{\"id\":\"" + id + "\",\"label\":\"" + label + "\",\"rings\":[" + rings + "]" + extra + "}";
    }

    private static String okEntry() {
        return entry("e1", "行1", ring("内側", "100") + "," + ring("外側", "300"), "");
    }

    private static String doc(String schemaVersion, String notice, String tsv, String entries) {
        return "{\"schemaVersion\":" + schemaVersion + ",\"notice\":" + notice + ",\"trainingSampleValues\":" + tsv
                + ",\"entries\":" + entries + "}";
    }

    private static String docOf(String... entries) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < entries.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(entries[i]);
        }
        sb.append(']');
        return doc("1", "\"x\"", "true", sb.toString());
    }

    private static WeaponTableResult parse(String json) {
        return WeaponTableCodec.parse(b(json), Lang.JA);
    }

    private static void assertError(String json, String expectedPart) {
        WeaponTableResult r = parse(json);
        assertFalse("エラーになるはず: " + json, r.ok());
        assertNull(r.table());
        for (String e : r.errors()) {
            if (e.contains(expectedPart)) {
                return;
            }
        }
        fail("「" + expectedPart + "」を含む理由がない: " + r.errors());
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static void assertSameTable(WeaponTable expected, WeaponTable actual) {
        assertEquals(expected.entries().size(), actual.entries().size());
        for (int i = 0; i < expected.entries().size(); i++) {
            WeaponEntry e = expected.entries().get(i);
            WeaponEntry a = actual.entries().get(i);
            assertEquals(e.id(), a.id());
            assertEquals(e.label(), a.label());
            assertEquals(e.note(), a.note());
            assertEquals(e.rings().size(), a.rings().size());
            for (int j = 0; j < e.rings().size(); j++) {
                assertEquals(e.rings().get(j).name(), a.rings().get(j).name());
                assertEquals(e.rings().get(j).radiusM(), a.rings().get(j).radiusM());
            }
        }
    }

    // ---- W1 ----

    @Test
    public void w1_defaultTablePassesValidation() {
        assertTrue(WeaponTableCodec.validate(DefaultWeaponTable.table(Lang.JA), Lang.JA).isEmpty());
    }

    @Test
    public void w1_defaultTableHasThreeRowsWithSampleValues() {
        List<WeaponEntry> rows = DefaultWeaponTable.table(Lang.JA).entries();
        assertEquals(3, rows.size());
        String[] ids = {"sample-small", "sample-medium", "sample-large"};
        String[] labels = {"サンプル 小", "サンプル 中", "サンプル 大"};
        int[][] radii = {{100, 300}, {250, 600}, {500, 1000}};
        for (int i = 0; i < 3; i++) {
            WeaponEntry e = rows.get(i);
            assertEquals(ids[i], e.id());
            assertEquals(labels[i], e.label());
            assertEquals("訓練用サンプル値。実在の兵器の値ではない / TRAINING SAMPLE VALUE — NOT A REAL WEAPON", e.note());
            assertEquals(2, e.rings().size());
            assertEquals("内側", e.rings().get(0).name());
            assertEquals("外側", e.rings().get(1).name());
            assertEquals(radii[i][0], e.rings().get(0).radiusM());
            assertEquals(radii[i][1], e.rings().get(1).radiusM());
        }
    }

    // ---- W2 ----

    @Test
    public void w2_writeThenParseGivesSameDefaultTable() {
        WeaponTable table = DefaultWeaponTable.table(Lang.JA);
        WeaponTableResult r = parse(WeaponTableCodec.write(table));
        assertTrue(r.errors().toString(), r.ok());
        assertSameTable(table, r.table());
    }

    @Test
    public void w2_writerOutputHasTheSpecifiedShape() {
        WeaponTable table = new WeaponTable(Arrays.asList(
                new WeaponEntry("a-1", "甲", Arrays.asList(new WeaponRing("内", 10), new WeaponRing("外", 20)), ""),
                new WeaponEntry("b-2", "乙", Arrays.asList(new WeaponRing("一", 5)), "メモ")));
        String expected = "{\n"
                + "  \"schemaVersion\": 1,\n"
                + "  \"notice\": \"" + WeaponTableCodec.FILE_NOTICE + "\",\n"
                + "  \"trainingSampleValues\": true,\n"
                + "  \"entries\": [\n"
                + "    {\n"
                + "      \"id\": \"a-1\",\n"
                + "      \"label\": \"甲\",\n"
                + "      \"rings\": [\n"
                + "        {\n"
                + "          \"name\": \"内\",\n"
                + "          \"radiusM\": 10\n"
                + "        },\n"
                + "        {\n"
                + "          \"name\": \"外\",\n"
                + "          \"radiusM\": 20\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"id\": \"b-2\",\n"
                + "      \"label\": \"乙\",\n"
                + "      \"rings\": [\n"
                + "        {\n"
                + "          \"name\": \"一\",\n"
                + "          \"radiusM\": 5\n"
                + "        }\n"
                + "      ],\n"
                + "      \"note\": \"メモ\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n";
        assertEquals(expected, WeaponTableCodec.write(table));
    }

    @Test
    public void w2_writerEndsWithOneNewlineAndUsesNoCarriageReturn() {
        String out = WeaponTableCodec.write(DefaultWeaponTable.table(Lang.JA));
        assertTrue(out.endsWith("}\n"));
        assertFalse(out.endsWith("\n\n"));
        assertFalse(out.contains("\r"));
    }

    @Test
    public void w2_writerOmitsEmptyNote() {
        WeaponTable table = new WeaponTable(Arrays.asList(
                new WeaponEntry("a", "甲", Arrays.asList(new WeaponRing("内", 10)), "")));
        assertFalse(WeaponTableCodec.write(table).contains("\"note\""));
    }

    @Test
    public void w2_writerKeyOrderMatchesTheInterfaceExample() {
        String out = WeaponTableCodec.write(DefaultWeaponTable.table(Lang.JA));
        int sv = out.indexOf("\"schemaVersion\"");
        int notice = out.indexOf("\"notice\"");
        int tsv = out.indexOf("\"trainingSampleValues\"");
        int entries = out.indexOf("\"entries\"");
        assertTrue(sv < notice && notice < tsv && tsv < entries);
        int id = out.indexOf("\"id\"");
        int label = out.indexOf("\"label\"");
        int rings = out.indexOf("\"rings\"");
        int note = out.indexOf("\"note\"");
        assertTrue(id < label && label < rings && rings < note);
    }

    @Test
    public void w2_writeNullIsRejected() {
        try {
            WeaponTableCodec.write(null);
            fail();
        } catch (IllegalArgumentException expected) {
            // よい
            // Valid.
        }
    }

    @Test
    public void w2_validateNullIsRejected() {
        try {
            WeaponTableCodec.validate(null, Lang.JA);
            fail();
        } catch (IllegalArgumentException expected) {
            // よい
            // Valid.
        }
    }

    @Test
    public void w2_validateGivesSameReasonsAsParse() {
        WeaponTable bad = new WeaponTable(Arrays.asList(
                new WeaponEntry("BAD", "甲", Arrays.asList(new WeaponRing("内", 20), new WeaponRing("外", 20)), "")));
        List<String> viaValidate = WeaponTableCodec.validate(bad, Lang.JA);
        assertEquals(2, viaValidate.size());
        WeaponTableResult viaParse = parse(WeaponTableCodec.write(bad));
        assertEquals(viaValidate, viaParse.errors());
        assertTrue(viaValidate.get(0).startsWith("entries[0].id: "));
        assertTrue(viaValidate.get(1).startsWith("entries[0].rings[1].radiusM: "));
    }

    @Test
    public void w2_validateRejectsDuplicateIds() {
        WeaponEntry a = new WeaponEntry("same", "甲", Arrays.asList(new WeaponRing("内", 1)), "");
        WeaponEntry b = new WeaponEntry("same", "乙", Arrays.asList(new WeaponRing("内", 1)), "");
        List<String> errors = WeaponTableCodec.validate(new WeaponTable(Arrays.asList(a, b)), Lang.JA);
        assertEquals(1, errors.size());
        assertEquals("entries[1].id: ほかの行と同じ id です（same）", errors.get(0));
    }

    @Test
    public void w2_validateRejectsEmptyTable() {
        List<String> errors = WeaponTableCodec.validate(new WeaponTable(new ArrayList<WeaponEntry>()), Lang.JA);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("entries: 1 行から 50 行までにしてください"));
    }

    // ---- W3 ----

    private static void assertSyntaxError(byte[] input, String reasonPart) {
        WeaponTableResult r = WeaponTableCodec.parse(input, Lang.JA);
        assertFalse(r.ok());
        assertNull(r.table());
        assertEquals(1, r.errors().size());
        String e = r.errors().get(0);
        assertTrue(e, e.matches("^\\d+行\\d+列: .+"));
        assertTrue(e, e.contains(reasonPart));
    }

    @Test
    public void w3_duplicateKeyIsErrorWithLineAndColumn() {
        assertSyntaxError(b("{\n  \"a\": 1,\n  \"a\": 2\n}"), "同じキーがあります");
        assertEquals("3行3列: 同じキーがあります（a）",
                WeaponTableCodec.parse(b("{\n  \"a\": 1,\n  \"a\": 2\n}"), Lang.JA).errors().get(0));
    }

    @Test
    public void w3_duplicateKeyInsideEntryIsError() {
        assertSyntaxError(b(doc("1", "\"x\"", "true",
                "[{\"id\":\"a\",\"id\":\"b\",\"label\":\"l\",\"rings\":[]}]")), "同じキーがあります");
    }

    @Test
    public void w3_trailingCharactersAreError() {
        assertSyntaxError(b(docOf(okEntry()) + " x"), "余分な文字");
        assertSyntaxError(b(docOf(okEntry()) + "{}"), "余分な文字");
    }

    @Test
    public void w3_trailingWhitespaceIsAllowed() {
        assertTrue(parse(docOf(okEntry()) + " \n\t\r\n").ok());
    }

    @Test
    public void w3_depth11IsErrorAndDepth10Passes() {
        assertSyntaxError(b(repeat("[", 11) + repeat("]", 11)), "入れ子が深すぎます");
        // 深さ 10 は文法として通る（値の検査では「全体はオブジェクト」で落ちる）
        // Depth 10 passes syntax checking (value validation rejects it because the root must be an object).
        WeaponTableResult r = WeaponTableCodec.parse(b(repeat("[", 10) + repeat("]", 10)), Lang.JA);
        assertEquals(1, r.errors().size());
        assertTrue(r.errors().get(0).contains("全体はオブジェクト"));
    }

    @Test
    public void w3_objectDepth11IsError() {
        String text = repeat("{\"a\":", 11) + "1" + repeat("}", 11);
        assertSyntaxError(b(text), "入れ子が深すぎます");
    }

    @Test
    public void w3_inputOver256KiBIsError() {
        byte[] big = new byte[262145];
        Arrays.fill(big, (byte) ' ');
        assertSyntaxError(big, "256 KiB");
    }

    @Test
    public void w3_inputOfExactly256KiBIsNotRejectedForSize() {
        byte[] exact = new byte[262144];
        Arrays.fill(exact, (byte) ' ');
        WeaponTableResult r = WeaponTableCodec.parse(exact, Lang.JA);
        assertFalse(r.ok());
        assertFalse(r.errors().get(0).contains("256 KiB"));
    }

    @Test
    public void w3_invalidUtf8IsErrorWithPosition() {
        byte[] head = b("{\"notice\":\"");
        byte[] bytes = new byte[head.length + 3];
        System.arraycopy(head, 0, bytes, 0, head.length);
        bytes[head.length] = (byte) 0xFF;
        bytes[head.length + 1] = '"';
        bytes[head.length + 2] = '}';
        assertSyntaxError(bytes, "UTF-8");
        assertTrue(WeaponTableCodec.parse(bytes, Lang.JA).errors().get(0).startsWith("1行" + (head.length + 1) + "列"));
    }

    @Test
    public void w3_truncatedUtf8SequenceIsError() {
        byte[] bytes = {'"', (byte) 0xE3, (byte) 0x81};
        assertSyntaxError(bytes, "UTF-8");
    }

    @Test
    public void w3_loneHighSurrogateIsError() {
        assertSyntaxError(b("\"\\ud800\""), "サロゲート");
        assertSyntaxError(b("\"\\ud800x\""), "サロゲート");
    }

    @Test
    public void w3_loneLowSurrogateIsError() {
        assertSyntaxError(b("\"\\udc00\""), "サロゲート");
    }

    @Test
    public void w3_highSurrogateFollowedByNonLowIsError() {
        assertSyntaxError(b("\"\\ud800\\u0041\""), "サロゲート");
    }

    @Test
    public void w3_unescapedControlCharacterIsError() {
        assertSyntaxError(b("\"a\u0001b\""), "制御文字");
        assertSyntaxError(b("\"a\nb\""), "制御文字");
    }

    @Test
    public void w3_badEscapeIsError() {
        assertSyntaxError(b("\"\\x\""), "使えないエスケープ");
        assertSyntaxError(b("\"\\u12G4\""), "16 進数");
        assertSyntaxError(b("\"\\u12\""), "16 進数");
    }

    @Test
    public void w3_unterminatedStringIsError() {
        assertSyntaxError(b("\"abc"), "文字列が閉じていません");
    }

    @Test
    public void w3_badNumbersAreErrors() {
        assertSyntaxError(b("[01]"), "");
        assertSyntaxError(b("[+1]"), "値として読めない");
        assertSyntaxError(b("[1.]"), "小数点の後");
        assertSyntaxError(b("[.5]"), "値として読めない");
        assertSyntaxError(b("[1e]"), "指数の後");
        assertSyntaxError(b("[-]"), "数の書き方");
    }

    @Test
    public void w3_structuralErrors() {
        assertSyntaxError(b(""), "値がありません");
        assertSyntaxError(b("{"), "キーは");
        assertSyntaxError(b("{\"a\"}"), "キーの後に :");
        assertSyntaxError(b("{\"a\":1"), "閉じていません");
        assertSyntaxError(b("{\"a\":1 \"b\":2}"), ", か }");
        assertSyntaxError(b("[1 2]"), ", か ]");
        assertSyntaxError(b("[1,"), "値がありません");
        assertSyntaxError(b("{a:1}"), "キーは");
        assertSyntaxError(b("tru"), "値として読めない");
    }

    @Test
    public void w3_errorLineAndColumnCountCodePoints() {
        // 1 行目に絵文字（サロゲート対）があっても、列は符号位置で数える
        // Even with an emoji (surrogate pair) on line 1, columns are counted in code points.
        String text = "{\"😀\": 1,\n \"x\": ?}";
        WeaponTableResult r = parse(text);
        assertEquals(1, r.errors().size());
        assertTrue(r.errors().get(0), r.errors().get(0).startsWith("2行7列: "));
    }

    @Test
    public void w3_nullInputIsRejected() {
        try {
            WeaponTableCodec.parse(null, Lang.JA);
            fail();
        } catch (IllegalArgumentException expected) {
            // よい
            // Valid.
        }
    }

    // ---- W4 ----

    @Test
    public void w4_bomIsAccepted() {
        byte[] body = b(docOf(okEntry()));
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);
        WeaponTableResult r = WeaponTableCodec.parse(withBom, Lang.JA);
        assertTrue(r.errors().toString(), r.ok());
    }

    @Test
    public void w4_unicodeEscapesAndSurrogatePairsAreDecoded() {
        String json = docOf(entry("e1", "\\u3042\\ud83d\\ude00\\u0041", ring("\\u5185", "1"), ""));
        WeaponTableResult r = parse(json);
        assertTrue(r.errors().toString(), r.ok());
        WeaponEntry e = r.table().entries().get(0);
        assertEquals("あ" + new String(Character.toChars(0x1F600)) + "A", e.label());
        assertEquals("内", e.rings().get(0).name());
    }

    @Test
    public void w4_shortEscapesAreDecoded() {
        String json = docOf(entry("e1", "a\\\"b\\\\c\\/d\\be\\ff\\ng\\rh\\ti", ring("x", "1"), ""));
        WeaponTableResult r = parse(json);
        assertTrue(r.errors().toString(), r.ok());
        assertEquals("a\"b\\c/d\be\ff\ng\rh\ti".trim(), r.table().entries().get(0).label());
    }

    @Test
    public void w4_upperAndLowerCaseHexAreAccepted() {
        String json = docOf(entry("e1", "\\u00e9\\u00E9", ring("x", "1"), ""));
        WeaponTableResult r = parse(json);
        assertTrue(r.errors().toString(), r.ok());
        assertEquals("éé", r.table().entries().get(0).label());
    }

    @Test
    public void w4_whitespaceBetweenTokensIsAccepted() {
        String json = "\n{ \"schemaVersion\" : 1 ,\r\n\t\"notice\" : \"x\" , \"trainingSampleValues\" : true ,"
                + " \"entries\" : [ " + okEntry() + " ] }\n";
        assertTrue(parse(json).ok());
    }

    @Test
    public void w4_valuesAreTrimmedButIdIsNot() {
        WeaponTableResult r = parse(docOf(entry("e1", "  甲  ", ring("  内  ", "1"), ",\"note\":\"  メモ  \"")));
        assertTrue(r.errors().toString(), r.ok());
        WeaponEntry e = r.table().entries().get(0);
        assertEquals("甲", e.label());
        assertEquals("内", e.rings().get(0).name());
        assertEquals("メモ", e.note());
        assertError(docOf(entry(" e1", "甲", ring("内", "1"), "")), "entries[0].id: ");
    }

    // ---- W5 ----

    @Test
    public void w5_validDocumentPasses() {
        WeaponTableResult r = parse(docOf(okEntry()));
        assertTrue(r.errors().toString(), r.ok());
        assertEquals(1, r.table().entries().size());
        assertEquals("", r.table().entries().get(0).note());
    }

    @Test
    public void w5_rootMustBeObject() {
        assertError("[]", "全体はオブジェクト");
        assertError("1", "全体はオブジェクト");
        assertError("null", "全体はオブジェクト");
    }

    @Test
    public void w5_schemaVersionMustBeIntegerOne() {
        assertError(doc("2", "\"x\"", "true", "[" + okEntry() + "]"), "schemaVersion: 1 にしてください");
        assertError(doc("1.0", "\"x\"", "true", "[" + okEntry() + "]"), "schemaVersion: 1 にしてください");
        assertError(doc("\"1\"", "\"x\"", "true", "[" + okEntry() + "]"), "schemaVersion: 1 にしてください");
        assertError(doc("1e0", "\"x\"", "true", "[" + okEntry() + "]"), "schemaVersion: 1 にしてください");
        assertError("{\"notice\":\"x\",\"trainingSampleValues\":true,\"entries\":[" + okEntry() + "]}",
                "schemaVersion: ありません");
    }

    @Test
    public void w5_noticeMustBeNonEmptyString() {
        assertError(doc("1", "\"\"", "true", "[" + okEntry() + "]"), "notice: 空でない文字列");
        assertError(doc("1", "\"   \"", "true", "[" + okEntry() + "]"), "notice: 空でない文字列");
        assertError(doc("1", "1", "true", "[" + okEntry() + "]"), "notice: 空でない文字列");
        assertError(doc("1", "null", "true", "[" + okEntry() + "]"), "notice: 空でない文字列");
        assertError("{\"schemaVersion\":1,\"trainingSampleValues\":true,\"entries\":[" + okEntry() + "]}",
                "notice: ありません");
    }

    @Test
    public void w5_trainingSampleValuesMustBeTrue() {
        assertError(doc("1", "\"x\"", "false", "[" + okEntry() + "]"), "trainingSampleValues: true にしてください");
        assertError(doc("1", "\"x\"", "\"true\"", "[" + okEntry() + "]"), "trainingSampleValues: true にしてください");
        assertError(doc("1", "\"x\"", "null", "[" + okEntry() + "]"), "trainingSampleValues: true にしてください");
        assertError("{\"schemaVersion\":1,\"notice\":\"x\",\"entries\":[" + okEntry() + "]}",
                "trainingSampleValues: ありません");
    }

    @Test
    public void w5_entriesMustBeArrayWithOneToFiftyRows() {
        assertError(doc("1", "\"x\"", "true", "[]"), "entries: 1 行から 50 行までにしてください");
        assertError(doc("1", "\"x\"", "true", "{}"), "entries: 配列にしてください");
        assertError("{\"schemaVersion\":1,\"notice\":\"x\",\"trainingSampleValues\":true}", "entries: ありません");
        String[] rows = new String[51];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = entry("id-" + i, "行", ring("内", "1"), "");
        }
        assertError(docOf(rows), "entries: 1 行から 50 行までにしてください");
        assertTrue(parse(docOf(Arrays.copyOf(rows, 50))).ok());
    }

    @Test
    public void w5_entryMustBeObject() {
        assertError(docOf("1"), "entries[0]: オブジェクトにしてください");
        assertError(docOf(okEntry(), "[]"), "entries[1]: オブジェクトにしてください");
    }

    @Test
    public void w5_idRules() {
        assertError(docOf(entry("", "甲", ring("内", "1"), "")), "entries[0].id: 空にできません");
        assertError(docOf(entry("Upper", "甲", ring("内", "1"), "")), "entries[0].id: 英小文字・数字・- だけ");
        assertError(docOf(entry("a_b", "甲", ring("内", "1"), "")), "entries[0].id: 英小文字・数字・- だけ");
        assertError(docOf(entry("a b", "甲", ring("内", "1"), "")), "entries[0].id: 英小文字・数字・- だけ");
        assertError(docOf(entry("あ", "甲", ring("内", "1"), "")), "entries[0].id: 英小文字・数字・- だけ");
        assertError(docOf(entry(repeat("a", 65), "甲", ring("内", "1"), "")), "entries[0].id: 64 文字まで");
        assertTrue(parse(docOf(entry(repeat("a", 64), "甲", ring("内", "1"), ""))).ok());
        assertTrue(parse(docOf(entry("a-0-z", "甲", ring("内", "1"), ""))).ok());
    }

    @Test
    public void w5_idMustBeString() {
        assertError(docOf("{\"id\":1,\"label\":\"a\",\"rings\":[" + ring("内", "1") + "]}"), "entries[0].id: 文字列");
        assertError(docOf("{\"label\":\"a\",\"rings\":[" + ring("内", "1") + "]}"), "entries[0].id: ありません");
    }

    @Test
    public void w5_idsMustBeUnique() {
        assertError(docOf(entry("sample-small", "甲", ring("内", "1"), ""),
                entry("ok", "乙", ring("内", "1"), ""),
                entry("sample-small", "丙", ring("内", "1"), "")),
                "entries[2].id: ほかの行と同じ id です（sample-small）");
    }

    @Test
    public void w5_labelRules() {
        assertError(docOf(entry("a", "", ring("内", "1"), "")), "entries[0].label: 空にできません");
        assertError(docOf(entry("a", "   ", ring("内", "1"), "")), "entries[0].label: 空にできません");
        assertError(docOf(entry("a", repeat("あ", 41), ring("内", "1"), "")), "entries[0].label: 40 文字まで");
        assertTrue(parse(docOf(entry("a", repeat("あ", 40), ring("内", "1"), ""))).ok());
        assertError(docOf("{\"id\":\"a\",\"label\":1,\"rings\":[" + ring("内", "1") + "]}"), "entries[0].label: 文字列");
        assertError(docOf("{\"id\":\"a\",\"rings\":[" + ring("内", "1") + "]}"), "entries[0].label: ありません");
    }

    @Test
    public void w5_ringsCount() {
        assertError(docOf(entry("a", "甲", "", "")), "entries[0].rings: 1 個から 3 個までにしてください");
        String four = ring("a", "1") + "," + ring("b", "2") + "," + ring("c", "3") + "," + ring("d", "4");
        assertError(docOf(entry("a", "甲", four, "")), "entries[0].rings: 1 個から 3 個までにしてください");
        String three = ring("a", "1") + "," + ring("b", "2") + "," + ring("c", "3");
        assertTrue(parse(docOf(entry("a", "甲", three, ""))).ok());
        assertError(docOf("{\"id\":\"a\",\"label\":\"x\",\"rings\":{}}"), "entries[0].rings: 配列にしてください");
        assertError(docOf("{\"id\":\"a\",\"label\":\"x\"}"), "entries[0].rings: ありません");
    }

    @Test
    public void w5_ringMustBeObject() {
        assertError(docOf(entry("a", "甲", "1", "")), "entries[0].rings[0]: オブジェクトにしてください");
    }

    @Test
    public void w5_ringNameRules() {
        assertError(docOf(entry("a", "甲", ring("", "1"), "")), "entries[0].rings[0].name: 空にできません");
        assertError(docOf(entry("a", "甲", ring(repeat("あ", 21), "1"), "")),
                "entries[0].rings[0].name: 20 文字まで");
        assertTrue(parse(docOf(entry("a", "甲", ring(repeat("あ", 20), "1"), ""))).ok());
        assertError(docOf(entry("a", "甲", "{\"radiusM\":1}", "")), "entries[0].rings[0].name: ありません");
    }

    @Test
    public void w5_radiusRange() {
        String path = "entries[0].rings[0].radiusM: 1 から 50000 までの整数にしてください";
        assertError(docOf(entry("a", "甲", ring("内", "0"), "")), path);
        assertError(docOf(entry("a", "甲", ring("内", "-1"), "")), path);
        assertError(docOf(entry("a", "甲", ring("内", "50001"), "")), path);
        assertError(docOf(entry("a", "甲", ring("内", "99999999999999999999"), "")), path);
        assertTrue(parse(docOf(entry("a", "甲", ring("内", "1"), ""))).ok());
        assertTrue(parse(docOf(entry("a", "甲", ring("内", "50000"), ""))).ok());
    }

    @Test
    public void w5_radiusMustBeWrittenAsInteger() {
        String path = "entries[0].rings[0].radiusM: 1 から 50000 までの整数にしてください";
        assertError(docOf(entry("a", "甲", ring("内", "100.0"), "")), path);
        assertError(docOf(entry("a", "甲", ring("内", "1e2"), "")), path);
        assertError(docOf(entry("a", "甲", ring("内", "1E2"), "")), path);
        assertError(docOf(entry("a", "甲", ring("内", "100.5"), "")), path);
        assertError(docOf(entry("a", "甲", ring("内", "\"100\""), "")), path);
        assertError(docOf(entry("a", "甲", ring("内", "null"), "")), path);
    }

    @Test
    public void w5_radiusMustBePresent() {
        assertError(docOf(entry("a", "甲", "{\"name\":\"内\"}", "")), "entries[0].rings[0].radiusM: ありません");
    }

    @Test
    public void w5_radiiMustGrowStrictly() {
        String same = ring("a", "100") + "," + ring("b", "100");
        assertError(docOf(entry("a", "甲", same, "")), "entries[0].rings[1].radiusM: 前の輪より大きい値にしてください");
        String smaller = ring("a", "100") + "," + ring("b", "50");
        assertError(docOf(entry("a", "甲", smaller, "")), "entries[0].rings[1].radiusM: 前の輪より大きい値にしてください");
        String third = ring("a", "100") + "," + ring("b", "200") + "," + ring("c", "150");
        assertError(docOf(entry("a", "甲", third, "")), "entries[0].rings[2].radiusM: 前の輪より大きい値にしてください");
    }

    @Test
    public void w5_noteRules() {
        assertError(docOf(entry("a", "甲", ring("内", "1"), ",\"note\":\"" + repeat("あ", 201) + "\"")),
                "entries[0].note: 200 文字まで");
        assertTrue(parse(docOf(entry("a", "甲", ring("内", "1"), ",\"note\":\"" + repeat("あ", 200) + "\""))).ok());
        assertError(docOf(entry("a", "甲", ring("内", "1"), ",\"note\":1")), "entries[0].note: 文字列");
        assertError(docOf(entry("a", "甲", ring("内", "1"), ",\"note\":null")), "entries[0].note: 文字列");
        WeaponTableResult empty = parse(docOf(entry("a", "甲", ring("内", "1"), ",\"note\":\"\"")));
        assertTrue(empty.ok());
        assertEquals("", empty.table().entries().get(0).note());
    }

    @Test
    public void w5_unknownKeysAreErrors() {
        assertError("{\"schemaVersion\":1,\"notice\":\"x\",\"trainingSampleValues\":true,\"extra\":1,\"entries\":["
                + okEntry() + "]}", "extra: 知らないキーです");
        assertError(docOf(entry("a", "甲", ring("内", "1"), ",\"extra\":1")), "entries[0].extra: 知らないキーです");
        assertError(docOf(entry("a", "甲", "{\"name\":\"内\",\"radiusM\":1,\"extra\":1}", "")),
                "entries[0].rings[0].extra: 知らないキーです");
    }

    @Test
    public void w5_errorsAreCappedAtTwenty() {
        String[] rows = new String[30];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = entry("id-" + i, "", ring("内", "1"), "");
        }
        WeaponTableResult r = parse(docOf(rows));
        assertFalse(r.ok());
        assertEquals(20, r.errors().size());
        assertTrue(r.errors().get(0).startsWith("entries[0].label: "));
        assertTrue(r.errors().get(19).startsWith("entries[19].label: "));
    }

    @Test
    public void w5_errorsAreCollectedInOrderOfDiscovery() {
        String json = "{\"schemaVersion\":2,\"notice\":\"\",\"trainingSampleValues\":false,\"entries\":[]}";
        WeaponTableResult r = parse(json);
        assertEquals(4, r.errors().size());
        assertTrue(r.errors().get(0).startsWith("schemaVersion: "));
        assertTrue(r.errors().get(1).startsWith("notice: "));
        assertTrue(r.errors().get(2).startsWith("trainingSampleValues: "));
        assertTrue(r.errors().get(3).startsWith("entries: "));
    }

    @Test
    public void w5_resultOfFailureHasNoTable() {
        WeaponTableResult r = parse("{}");
        assertFalse(r.ok());
        assertNull(r.table());
        assertFalse(r.errors().isEmpty());
    }

    @Test
    public void w5_resultOfSuccessHasTableAndNoErrors() {
        WeaponTableResult r = parse(docOf(okEntry()));
        assertTrue(r.ok());
        assertNotNull(r.table());
        assertTrue(r.errors().isEmpty());
    }

    @Test
    public void w5_parsedRingsKeepOrder() {
        WeaponTableResult r = parse(docOf(entry("a", "甲",
                ring("赤", "10") + "," + ring("橙", "20") + "," + ring("黄", "30"), "")));
        assertTrue(r.ok());
        List<WeaponRing> rings = r.table().entries().get(0).rings();
        assertEquals("赤", rings.get(0).name());
        assertEquals(10, rings.get(0).radiusM());
        assertEquals("黄", rings.get(2).name());
        assertEquals(30, rings.get(2).radiusM());
    }

    // ---- W6 ----

    @Test
    public void w6_emojiLabelOf40CodePointsPasses() {
        String label = repeat("😀", 40);
        assertEquals(80, label.length());
        WeaponTableResult r = parse(docOf(entry("a", label, ring("内", "1"), "")));
        assertTrue(r.errors().toString(), r.ok());
        assertEquals(label, r.table().entries().get(0).label());
    }

    @Test
    public void w6_emojiLabelOf41CodePointsIsError() {
        assertError(docOf(entry("a", repeat("😀", 41), ring("内", "1"), "")), "entries[0].label: 40 文字まで");
    }

    @Test
    public void w6_emojiRingNameAndNoteAreCountedByCodePoints() {
        assertTrue(parse(docOf(entry("a", "甲", ring(repeat("😀", 20), "1"),
                ",\"note\":\"" + repeat("😀", 200) + "\""))).ok());
        assertError(docOf(entry("a", "甲", ring(repeat("😀", 21), "1"), "")), "rings[0].name: 20 文字まで");
        assertError(docOf(entry("a", "甲", ring("内", "1"), ",\"note\":\"" + repeat("😀", 201) + "\"")),
                "entries[0].note: 200 文字まで");
    }

    @Test
    public void w6_validateCountsCodePointsToo() {
        WeaponEntry ok = new WeaponEntry("a", repeat("😀", 40), Arrays.asList(new WeaponRing("内", 1)), "");
        WeaponEntry bad = new WeaponEntry("b", repeat("😀", 41), Arrays.asList(new WeaponRing("内", 1)), "");
        assertTrue(WeaponTableCodec.validate(new WeaponTable(Arrays.asList(ok)), Lang.JA).isEmpty());
        assertEquals(1, WeaponTableCodec.validate(new WeaponTable(Arrays.asList(bad)), Lang.JA).size());
    }

    // ---- W7 ----

    @Test
    public void w7_noticeIsReplacedWithFileNoticeWhenRewritten() {
        WeaponTableResult r = parse(doc("1", "\"自分で書き換えた注意書き\"", "true", "[" + okEntry() + "]"));
        assertTrue(r.ok());
        String out = WeaponTableCodec.write(r.table());
        assertFalse(out.contains("自分で書き換えた注意書き"));
        assertTrue(out.contains(WeaponTableCodec.FILE_NOTICE));
        assertTrue(parse(out).ok());
    }

    @Test
    public void w7_trainingSampleValuesIsAlwaysTrueWhenWritten() {
        String out = WeaponTableCodec.write(DefaultWeaponTable.table(Lang.JA));
        assertTrue(out.contains("\"trainingSampleValues\": true"));
        assertFalse(out.contains("false"));
    }

    @Test
    public void w7_fileNoticeContainsTrainingWording() {
        assertTrue(WeaponTableCodec.FILE_NOTICE.contains("TRAINING USE ONLY / 訓練用"));
        assertTrue(WeaponTableCodec.FILE_NOTICE.contains("訓練用サンプル値"));
        assertEquals("TRAINING USE ONLY / 訓練用。訓練用サンプル値であり、実際の安全距離・危害半径ではない。",
                WeaponTableCodec.FILE_NOTICE);
    }

    // ---- W8 ----

    @Test
    public void w8_writerEscapesSpecialCharactersAndKeepsJapanese() {
        String label = "a\"b\\c\nd\te\u0001f日本語";
        WeaponTable table = new WeaponTable(Arrays.asList(
                new WeaponEntry("a", label, Arrays.asList(new WeaponRing("内", 1)), "")));
        String out = WeaponTableCodec.write(table);
        assertTrue(out, out.contains("\"label\": \"a\\\"b\\\\c\\nd\\te\\u0001f日本語\""));
        assertFalse(out.contains("\u0001"));
        WeaponTableResult back = parse(out);
        assertTrue(back.errors().toString(), back.ok());
        assertEquals(label, back.table().entries().get(0).label());
    }

    @Test
    public void w8_writerEscapesCarriageReturnBackspaceAndFormFeed() {
        String label = "x\ry\bz\fw";
        WeaponTable table = new WeaponTable(Arrays.asList(
                new WeaponEntry("a", label, Arrays.asList(new WeaponRing("内", 1)), "")));
        String out = WeaponTableCodec.write(table);
        assertTrue(out, out.contains("\"x\\ry\\bz\\fw\""));
        assertEquals(label, parse(out).table().entries().get(0).label());
    }

    @Test
    public void w8_writerDoesNotEscapeSlashOrNonAscii() {
        WeaponTable table = new WeaponTable(Arrays.asList(
                new WeaponEntry("a", "a/b 😀", Arrays.asList(new WeaponRing("内", 1)), "")));
        String out = WeaponTableCodec.write(table);
        assertTrue(out.contains("\"a/b 😀\""));
    }

    @Test
    public void w8_writerOutputIsUtf8WhenEncoded() {
        String out = WeaponTableCodec.write(DefaultWeaponTable.table(Lang.JA));
        WeaponTableResult r = WeaponTableCodec.parse(out.getBytes(StandardCharsets.UTF_8), Lang.JA);
        assertTrue(r.ok());
        assertEquals("サンプル 小", r.table().entries().get(0).label());
    }

    @Test
    public void w8_roundTripKeepsEveryFieldOfAnEditedTable() {
        WeaponTable table = new WeaponTable(Arrays.asList(
                new WeaponEntry("x-1", "甲", Arrays.asList(
                        new WeaponRing("赤", 1), new WeaponRing("橙", 22), new WeaponRing("黄", 50000)), "メモ\n二行目"),
                new WeaponEntry("x-2", "乙", Arrays.asList(new WeaponRing("一", 7)), "")));
        WeaponTableResult r = parse(WeaponTableCodec.write(table));
        assertTrue(r.errors().toString(), r.ok());
        assertSameTable(table, r.table());
    }

    @Test
    public void englishCodecErrorsKeepPaths() {
        String json = docOf(entry("a", "Sample", ring("Inner", "0"), ""));
        String ja = WeaponTableCodec.parse(b(json), Lang.JA).errors().get(0);
        String en = WeaponTableCodec.parse(b(json), Lang.EN).errors().get(0);
        assertEquals(ja.substring(0, ja.indexOf(": ")), en.substring(0, en.indexOf(": ")));
        assertEquals("entries[0].rings[0].radiusM: must be an integer from 1 to 50000", en);
        assertEquals("the whole file must be an object",
                WeaponTableCodec.parse(b("[]"), Lang.EN).errors().get(0));
        assertEquals("schemaVersion: missing",
                WeaponTableCodec.parse(b("{}"), Lang.EN).errors().get(0));
        WeaponTable invalid = new WeaponTable(Arrays.asList(new WeaponEntry("a", "",
                Arrays.asList(new WeaponRing("Inner", 100)), "")));
        assertEquals("entries[0].label: must not be empty", WeaponTableCodec.validate(invalid, Lang.EN).get(0));
    }

    @Test
    public void englishJsonErrors() {
        assertEquals("line 2, column 1: a key must be a string in double quotes",
                WeaponTableCodec.parse(b("{\nx"), Lang.EN).errors().get(0));
        assertEquals("line 1, column 1: input exceeds 256 KiB (262144 bytes)",
                WeaponTableCodec.parse(new byte[262145], Lang.EN).errors().get(0));
        assertEquals("line 1, column 1: invalid UTF-8 byte sequence",
                WeaponTableCodec.parse(new byte[] {(byte) 0xff}, Lang.EN).errors().get(0));
        byte[] prefix = b("{\n  ");
        byte[] bad = Arrays.copyOf(prefix, prefix.length + 1);
        bad[bad.length - 1] = (byte) 0xff;
        assertEquals("line 2, column 3: invalid UTF-8 byte sequence",
                WeaponTableCodec.parse(bad, Lang.EN).errors().get(0));
    }

    @Test
    public void englishDefaultTableOnlyChangesNames() {
        WeaponTable ja = DefaultWeaponTable.table(Lang.JA);
        WeaponTable en = DefaultWeaponTable.table(Lang.EN);
        String[] labels = {"Sample S", "Sample M", "Sample L"};
        assertEquals(3, en.entries().size());
        assertTrue(WeaponTableCodec.validate(en, Lang.EN).isEmpty());
        for (int i = 0; i < 3; i++) {
            WeaponEntry j = ja.entries().get(i);
            WeaponEntry e = en.entries().get(i);
            assertEquals(labels[i], e.label());
            assertEquals(j.id(), e.id());
            assertEquals(j.note(), e.note());
            assertEquals("訓練用サンプル値。実在の兵器の値ではない / TRAINING SAMPLE VALUE — NOT A REAL WEAPON", e.note());
            assertEquals("Inner", e.rings().get(0).name());
            assertEquals("Outer", e.rings().get(1).name());
            for (int k = 0; k < 2; k++) {
                assertEquals(j.rings().get(k).radiusM(), e.rings().get(k).radiusM());
            }
        }
        WeaponTable back = WeaponTableCodec.parse(b(WeaponTableCodec.write(ja)), Lang.EN).table();
        assertSameTable(ja, back);
    }

}
