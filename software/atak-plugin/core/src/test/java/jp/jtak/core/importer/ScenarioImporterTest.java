package jp.jtak.core.importer;

import static jp.jtak.core.importer.TestSupport.U1;
import static jp.jtak.core.importer.TestSupport.between;
import static jp.jtak.core.importer.TestSupport.bytes;
import static jp.jtak.core.importer.TestSupport.demo;
import static jp.jtak.core.importer.TestSupport.join;
import static jp.jtak.core.importer.TestSupport.lineValue;
import static jp.jtak.core.importer.TestSupport.parse;
import static jp.jtak.core.importer.TestSupport.template;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import jp.jtak.core.Lang;
import jp.jtak.core.cas.CheckInMode;
import jp.jtak.core.cas.MethodOfAttack;
import jp.jtak.core.cas.RoutingClosing;
import jp.jtak.core.cas.RoutingInstruction;
import jp.jtak.core.cas.TotType;
import jp.jtak.core.cas.TypeOfControl;
import org.junit.Test;

/**
 * D1〜D15・D17: ScenarioImporter の試験（設計 06 の 2.8）。D1〜D16 は Lang.JA で行う。
 * <p>
 * D1–D15 and D17: ScenarioImporter tests (design 06 §2.8). D1–D16 run with Lang.JA.
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class ScenarioImporterTest {
    // ---- D1: サンプル（demo）----
    // ---- D1: Samples (demo) ----

    @Test
    public void d1_ルーティングのdemoを読む() {
        ScenarioImportResult r = parse(demo("1-routing"), ScenarioKind.ROUTING);
        assertNull(r.error());
        assertEquals(ScenarioKind.ROUTING, r.kind());
        assertTrue(r.skippedItems().isEmpty());
        assertNull(r.checkIn());
        assertNull(r.gamePlan());
        assertNull(r.casBrief());
        RoutingImport x = r.routing();
        assertEquals("KESTREL 11", x.callsign());
        assertEquals(RoutingInstruction.PROCEED_REPORT_ESTABLISHED, x.instruction());
        assertEquals("IP PINE", x.place());
        assertEquals("maintain 15000 ft MSL or above", x.otherInstructions());
        assertEquals(Boolean.TRUE, x.onlyAircraftOnStation());
        assertNull(x.otherAos());
        assertEquals("simulated MANPADS north of the river", x.threats());
        assertEquals(
                "scattered clouds at 8000 ft MSL, ridge line east of the target area", x.weatherTerrain());
        assertEquals("report established at IP PINE", x.statusCalls());
        assertEquals(RoutingClosing.SEND, x.closing());
        assertNull(x.holdReason());
    }

    @Test
    public void d1_チェックインのdemoを読む() {
        ScenarioImportResult r = parse(demo("2-check-in"), ScenarioKind.CHECK_IN);
        assertNull(r.error());
        assertEquals(ScenarioKind.CHECK_IN, r.kind());
        assertTrue(r.skippedItems().isEmpty());
        CheckInImport x = r.checkIn();
        assertEquals("OWL 21", x.jtacCallsign());
        assertEquals("KESTREL 11", x.aircraftCallsign());
        assertEquals(CheckInMode.FULL, x.mode());
        assertEquals("TR0417", x.missionNumber());
        assertEquals("2 x strike fighter", x.aircraft());
        assertEquals("IP PINE, 15000 ft MSL", x.positionAltitude());
        assertEquals(lineValue(demo("2-check-in"), "4 Ordnance:"), x.ordnance());
        assertEquals("45 minutes", x.playtime());
        assertEquals("targeting pod, laser designator, IR pointer", x.capabilities());
        assertEquals("BRAVO 7", x.abortCode());
        assertEquals(Boolean.FALSE, x.abortCodeNone());
    }

    @Test
    public void d1_ゲームプランのdemoを読む() {
        ScenarioImportResult r = parse(demo("3-game-plan"), ScenarioKind.GAME_PLAN);
        assertNull(r.error());
        assertEquals(ScenarioKind.GAME_PLAN, r.kind());
        assertTrue(r.skippedItems().isEmpty());
        GamePlanImport x = r.gamePlan();
        assertEquals("KESTREL 11", x.callsign());
        assertEquals(TypeOfControl.TYPE_2, x.typeOfControl());
        assertEquals(MethodOfAttack.BOT, x.methodOfAttack());
        assertEquals("neutralize the simulated air defense site", x.intent());
        assertEquals("training range boundary 2 km south", x.collateralConcerns());
        assertEquals(between(demo("3-game-plan"), ", ordnance ", ", effects "), x.ordnance());
        assertEquals("destroy the launcher vehicle", x.desiredEffects());
        assertEquals("instantaneous", x.fuzing());
        assertEquals("single", x.interval());
        assertEquals("8", x.line6Digits());
    }

    @Test
    public void d1_CASBriefのdemoを読む() {
        ScenarioImportResult r = parse(demo("4-cas-brief"), ScenarioKind.CAS_BRIEF);
        assertNull(r.error());
        assertEquals(ScenarioKind.CAS_BRIEF, r.kind());
        assertTrue(r.skippedItems().isEmpty());
        CasBriefImport x = r.casBrief();
        assertEquals("IP PINE", x.line1());
        assertEquals("095 magnetic", x.line2());
        assertEquals("left", x.offset());
        assertEquals("8.5 NM", x.line3());
        assertEquals("320 ft MSL", x.line4());
        assertEquals("simulated SAM site, 1 x launcher vehicle", x.line5());
        assertEquals("54S UE 12345 67890", x.line6());
        assertEquals("laser, code 1688", x.line7());
        assertEquals("south 1200 m", x.line8());
        assertEquals("west to IP PINE, climb to 15000 ft MSL", x.line9());
        assertEquals("LTL 180", x.ltlPtl());
        assertEquals(lineValue(demo("4-cas-brief"), "Ordnance / Effects:"), x.remarksOrdnance());
        assertEquals("simulated MANPADS north of the river, no SEAD", x.threat());
        assertEquals("training range boundary 2 km south of the target", x.additionalRemarks());
        assertEquals("call IN with heading", x.additionalCalls());
        assertNull(x.dangerClose());
        assertNull(x.dangerCloseInitials());
        assertNull(x.acas());
        assertEquals("060-120", x.fahs());
        assertNull(x.separation());
        assertNull(x.rwOutOfBp());
        assertNull(x.pla());
        assertEquals(TotType.TIME, x.tot());
        assertEquals("1530Z", x.totTime());
    }

    // ---- D2: ひな形 ----
    // ---- D2: Templates ----

    @Test
    public void d2_ひな形は何も読めない() {
        assertNothingRead(template("routing"), ScenarioKind.ROUTING);
        assertNothingRead(template("check-in"), ScenarioKind.CHECK_IN);
        assertNothingRead(template("game-plan"), ScenarioKind.GAME_PLAN);
        assertNothingRead(template("cas-brief"), ScenarioKind.CAS_BRIEF);
    }

    private static void assertNothingRead(String text, ScenarioKind kind) {
        ScenarioImportResult r = parse(text, kind);
        assertNotNull(kind.name(), r.error());
        assertEquals(kind.name(), ScenarioImportError.Code.NOTHING_READ, r.error().code());
        assertNull(r.routing());
        assertNull(r.checkIn());
        assertNull(r.gamePlan());
        assertNull(r.casBrief());
    }

    // ---- D3: 一部が（未入力）----
    // ---- D3: Some items are missing ----

    @Test
    public void d3_ルーティングの一部が未入力() {
        String text = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "KESTREL 11, proceed IP PINE and report established.",
                "Other AOS: （未入力）",
                "Status calls: （未入力）",
                "Send your check-in.", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.ROUTING);
        assertNull(r.error());
        assertEquals(Arrays.asList("Other AOS（未入力）", "Status calls（未入力）"), r.skippedItems());
        assertNull(r.routing().otherAos());
        assertNull(r.routing().statusCalls());
        assertEquals("IP PINE", r.routing().place());
    }

    @Test
    public void d3_ルーティングの本文が未入力でも呼出符号は読む() {
        String text = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "KESTREL 11, （未入力）", "Send your check-in.", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.ROUTING);
        assertNull(r.error());
        assertEquals("KESTREL 11", r.routing().callsign());
        assertNull(r.routing().instruction());
        assertNull(r.routing().place());
        assertEquals(Arrays.asList("Routing/Holding（未入力）"), r.skippedItems());
    }

    @Test
    public void d3_チェックインの呼出符号の片方だけ未入力() {
        String text = demo("2-check-in").replace("OWL 21, this is KESTREL 11", "OWL 21, this is （未入力）")
                .replace("5 Playtime: 45 minutes", "5 Playtime: （未入力）");
        ScenarioImportResult r = parse(text, ScenarioKind.CHECK_IN);
        assertNull(r.error());
        assertEquals("OWL 21", r.checkIn().jtacCallsign());
        assertNull(r.checkIn().aircraftCallsign());
        assertNull(r.checkIn().playtime());
        assertEquals(Arrays.asList("Aircraft callsign（未入力）", "5 Playtime（未入力）"), r.skippedItems());
    }

    @Test
    public void d3_CASBriefの一部が未入力() {
        String text = demo("4-cas-brief")
                .replace("4 Target elevation: 320 ft MSL (readback)", "4 Target elevation: （未入力）")
                .replace("TOT 1530Z (readback)", "TOT （未入力）");
        ScenarioImportResult r = parse(text, ScenarioKind.CAS_BRIEF);
        assertNull(r.error());
        assertNull(r.casBrief().line4());
        assertNull(r.casBrief().tot());
        assertNull(r.casBrief().totTime());
        assertEquals(Arrays.asList("9-line 4（未入力）", "TOT（未入力）"), r.skippedItems());
    }

    @Test
    public void d3_DangerCloseのイニシャルだけ未入力() {
        String text = demo("4-cas-brief").replace("FAHs: 060-120 (readback)",
                "Danger close, initials （未入力）");
        ScenarioImportResult r = parse(text, ScenarioKind.CAS_BRIEF);
        assertNull(r.error());
        assertEquals(Boolean.TRUE, r.casBrief().dangerClose());
        assertNull(r.casBrief().dangerCloseInitials());
        assertTrue(r.skippedItems().contains("Danger close（未入力）"));
    }

    // ---- D4: 読めないファイル ----
    // ---- D4: Unreadable files ----

    @Test
    public void d4_UTF8でないとNOT_UTF8() {
        ScenarioImportResult r = ScenarioImporter.parse(
                new byte[] {'a', (byte) 0xC3, 0x28}, ScenarioKind.ROUTING, Lang.JA);
        assertEquals(ScenarioImportError.Code.NOT_UTF8, r.error().code());
        assertNull(r.kind());
        assertTrue(r.skippedItems().isEmpty());
    }

    @Test
    public void d4_大きすぎるとTOO_LARGE() {
        byte[] b = new byte[ScenarioImporter.MAX_BYTES + 1];
        Arrays.fill(b, (byte) 'a');
        ScenarioImportResult r = ScenarioImporter.parse(b, ScenarioKind.ROUTING, Lang.JA);
        assertEquals(ScenarioImportError.Code.TOO_LARGE, r.error().code());
    }

    @Test
    public void d4_空とU1だけはEMPTY() {
        assertEquals(ScenarioImportError.Code.EMPTY,
                ScenarioImporter.parse(new byte[0], ScenarioKind.ROUTING, Lang.JA).error().code());
        assertEquals(ScenarioImportError.Code.EMPTY,
                parse(U1 + "\n\n" + U1 + "\n", ScenarioKind.ROUTING).error().code());
    }

    // ---- D5: 題名 ----
    // ---- D5: Title ----

    @Test
    public void d5_題名が1文字違うとUNKNOWN_TITLE() {
        String text = demo("1-routing").replace("Fig. V-3", "Fig. V-4");
        ScenarioImportResult r = parse(text, ScenarioKind.ROUTING);
        assertEquals(ScenarioImportError.Code.UNKNOWN_TITLE, r.error().code());
        assertNull(r.kind());
    }

    @Test
    public void d5_題名の大文字小文字が違うとUNKNOWN_TITLE() {
        String text = demo("1-routing").replace("ROUTING (JP", "Routing (JP");
        assertEquals(ScenarioImportError.Code.UNKNOWN_TITLE,
                parse(text, ScenarioKind.ROUTING).error().code());
    }

    // ---- D6: 画面と書式の違い ----
    // ---- D6: Screen/format mismatch ----

    @Test
    public void d6_別の書式のファイルはWRONG_KIND() {
        ScenarioImportResult r = parse(demo("1-routing"), ScenarioKind.CHECK_IN);
        assertEquals(ScenarioImportError.Code.WRONG_KIND, r.error().code());
        assertEquals(ScenarioKind.ROUTING, r.error().actualKind());
        assertEquals(ScenarioKind.ROUTING, r.kind());
        assertNull(r.routing());
        assertNull(r.checkIn());
    }

    @Test
    public void d6_ほかの組み合わせ() {
        assertEquals(ScenarioKind.CHECK_IN,
                parse(demo("2-check-in"), ScenarioKind.GAME_PLAN).error().actualKind());
        assertEquals(ScenarioKind.GAME_PLAN,
                parse(demo("3-game-plan"), ScenarioKind.CAS_BRIEF).error().actualKind());
        assertEquals(ScenarioKind.CAS_BRIEF,
                parse(demo("4-cas-brief"), ScenarioKind.ROUTING).error().actualKind());
    }

    // ---- D7: 改行・BOM・U1 の有無 ----
    // ---- D7: Newlines, BOM, and presence/absence of U1 ----

    @Test
    public void d7_CRLFとBOMとU1の有無でも結果が同じ() {
        String[] names = {"1-routing", "2-check-in", "3-game-plan", "4-cas-brief"};
        ScenarioKind[] kinds = {
            ScenarioKind.ROUTING, ScenarioKind.CHECK_IN, ScenarioKind.GAME_PLAN, ScenarioKind.CAS_BRIEF
        };
        for (int i = 0; i < names.length; i++) {
            String base = demo(names[i]);
            String expected = textOf(parse(base, kinds[i]));
            String crlf = base.replace("\n", "\r\n");
            String bom = "﻿" + base;
            String noU1 = base.replace(U1 + "\n", "").replace("\n" + U1, "");
            for (String variant : new String[] {crlf, bom, noU1, "﻿" + crlf}) {
                ScenarioImportResult r = parse(variant, kinds[i]);
                assertNull(names[i], r.error());
                assertTrue(names[i], r.skippedItems().isEmpty());
                assertEquals(names[i], expected, textOf(r));
            }
        }
    }

    private static String textOf(ScenarioImportResult r) {
        if (r.routing() != null) {
            return TestSupport.text(r.routing());
        }
        if (r.checkIn() != null) {
            return TestSupport.text(r.checkIn());
        }
        if (r.gamePlan() != null) {
            return TestSupport.text(r.gamePlan());
        }
        return TestSupport.text(r.casBrief());
    }

    // ---- D8: 知らない行・重複・組み合わせ ----
    // ---- D8: Unknown lines, duplicates, and combinations ----

    @Test
    public void d8_知らない行は行の番号で知らせる() {
        // 1: U1  2: 題名  3: 本文  4: 知らない行
        // 1: U1  2: Title  3: Body  4: Unknown line
        String text = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "KESTREL 11, maintain IP PINE.", "Hello world", "Send your check-in.", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.ROUTING);
        assertNull(r.error());
        assertEquals(Arrays.asList("4 行目"), r.skippedItems());
        assertEquals(RoutingInstruction.MAINTAIN, r.routing().instruction());
    }

    @Test
    public void d8_重複は最初の行を使い項目を知らせる() {
        String text = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "KESTREL 11, maintain IP PINE.",
                "Other instructions: first",
                "Other instructions: second", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.ROUTING);
        assertEquals("first", r.routing().otherInstructions());
        assertEquals(Arrays.asList("Other instructions（重複）"), r.skippedItems());
    }

    @Test
    public void d8_組み合わせで飛ばした項目だけなら何も読めなかったことにする() {
        String text = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "You are the only aircraft on station.",
                "Other AOS: OWL 31", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.ROUTING);
        assertNotNull(r.error());
        assertEquals(ScenarioImportError.Code.NOTHING_READ, r.error().code());
        assertNull(r.routing());
        assertTrue(r.skippedItems().isEmpty());
    }

    @Test
    public void d8_コロンの直後に空白がないOtherAOSは行の番号で知らせる() {
        String text = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "KESTREL 11, maintain IP PINE.",
                "Other AOS:X", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.ROUTING);
        assertNull(r.error());
        assertNull(r.routing().otherAos());
        assertEquals(Arrays.asList("4 行目"), r.skippedItems());
    }

    @Test
    public void d8_OtherAOSが行末で終わると未入力として知らせる() {
        String text = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "KESTREL 11, maintain IP PINE.",
                "Other AOS:", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.ROUTING);
        assertNull(r.error());
        assertNull(r.routing().otherAos());
        assertEquals(Arrays.asList("Other AOS（未入力）"), r.skippedItems());
    }

    @Test
    public void d8_他の航空機なしと他の航空機の両方は両方を飛ばす() {
        String text = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "KESTREL 11, maintain IP PINE.",
                "You are the only aircraft on station.",
                "Other AOS: OWL 31", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.ROUTING);
        assertNull(r.error());
        assertNull(r.routing().onlyAircraftOnStation());
        assertNull(r.routing().otherAos());
        assertEquals(Arrays.asList("Only aircraft on station（組み合わせ）", "Other AOS（組み合わせ）"),
                r.skippedItems());
    }

    @Test
    public void d8_ルーティングの結び() {
        String hold = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "KESTREL 11, maintain IP PINE.", "Hold your check-in, standby for tasking.", U1);
        ScenarioImportResult r = parse(hold, ScenarioKind.ROUTING);
        assertEquals(RoutingClosing.HOLD, r.routing().closing());
        assertEquals("standby for tasking", r.routing().holdReason());
        String hold2 = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "maintain IP PINE.", "Hold your check-in.", U1);
        r = parse(hold2, ScenarioKind.ROUTING);
        assertEquals(RoutingClosing.HOLD, r.routing().closing());
        assertNull(r.routing().holdReason());
        assertNull(r.routing().callsign());
        assertEquals("IP PINE", r.routing().place());
    }

    @Test
    public void d8_ルーティングの3つの指示() {
        for (String[] c : new String[][] {
            {"Proceed IP PINE and report established.", "PROCEED_REPORT_ESTABLISHED"},
            {"Maintain IP PINE.", "MAINTAIN"},
            {"Report passing IP PINE.", "REPORT_PASSING"},
            {"KESTREL 11, report passing IP PINE.", "REPORT_PASSING"},
        }) {
            ScenarioImportResult r = parse(
                    join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)", c[0], U1), ScenarioKind.ROUTING);
            assertNull(c[0], r.error());
            assertEquals(c[0], RoutingInstruction.valueOf(c[1]), r.routing().instruction());
            assertEquals(c[0], "IP PINE", r.routing().place());
        }
    }

    @Test
    public void d8_チェックインのAbortコード2種と重複() {
        String none = demo("2-check-in").replace("7 Abort code: BRAVO 7 (readback)",
                "7 Abort code: none (standard ABORT, ABORT, ABORT)");
        ScenarioImportResult r = parse(none, ScenarioKind.CHECK_IN);
        assertNull(r.error());
        assertEquals(Boolean.TRUE, r.checkIn().abortCodeNone());
        assertNull(r.checkIn().abortCode());
        assertTrue(r.skippedItems().isEmpty());

        String both = demo("2-check-in") + "\n7 Abort code: none (standard ABORT, ABORT, ABORT)";
        r = parse(both, ScenarioKind.CHECK_IN);
        assertEquals("BRAVO 7", r.checkIn().abortCode());
        assertEquals(Boolean.FALSE, r.checkIn().abortCodeNone());
        assertEquals(Arrays.asList("7 Abort code（重複）"), r.skippedItems());
    }

    @Test
    public void d8_チェックインの必須はモードで変わる() {
        // (a) As fragged だけで 1〜7 行目がない → 1〜7 行目の（なし）は出ない
        // (a) As fragged alone, without lines 1–7 → no “absent” notices for lines 1–7
        ScenarioImportResult r = parse(
                join("CHECK-IN (JP 3-09.3 2014 Fig. V-4)", "OWL 21, this is KESTREL 11", "As fragged."),
                ScenarioKind.CHECK_IN);
        assertNull(r.error());
        assertEquals(CheckInMode.AS_FRAGGED, r.checkIn().mode());
        assertTrue(r.skippedItems().isEmpty());
        // (b) モードの行も 1〜7 行目もない → FULL になり、1〜7 行目の（なし）が出る
        // (b) Neither a mode line nor lines 1–7 → defaults to FULL and reports “absent” for lines 1–7
        r = parse(join("CHECK-IN (JP 3-09.3 2014 Fig. V-4)", "OWL 21, this is KESTREL 11"), ScenarioKind.CHECK_IN);
        assertNull(r.error());
        assertEquals(CheckInMode.FULL, r.checkIn().mode());
        assertEquals(7, r.skippedItems().size());
        assertTrue(r.skippedItems().contains("1 Mission number（なし）"));
        assertTrue(r.skippedItems().contains("7 Abort code（なし）"));
        // (c) コールサインの行がない As fragged → コールサインの（なし）が出る
        // (c) As fragged without a callsign line → reports “absent” for callsign
        r = parse(join("CHECK-IN (JP 3-09.3 2014 Fig. V-4)", "As fragged."), ScenarioKind.CHECK_IN);
        assertNull(r.error());
        assertEquals(Arrays.asList("JTAC callsign（なし）", "Aircraft callsign（なし）"), r.skippedItems());
    }

    @Test
    public void d8_チェックインの3つのモード() {
        String asFragged = demo("2-check-in").replace("1 Mission number: TR0417\n", "As fragged.\n");
        ScenarioImportResult r = parse(asFragged, ScenarioKind.CHECK_IN);
        assertEquals(CheckInMode.AS_FRAGGED, r.checkIn().mode());
        assertNull(r.checkIn().missionNumber());
        String exc = demo("2-check-in").replace("1 Mission number: TR0417\n",
                "As fragged, with exception:\n1 Mission number: TR0417\n");
        r = parse(exc, ScenarioKind.CHECK_IN);
        assertEquals(CheckInMode.WITH_EXCEPTION, r.checkIn().mode());
        assertEquals("TR0417", r.checkIn().missionNumber());
        assertTrue(r.skippedItems().isEmpty());
    }

    @Test
    public void d8_CASBriefのTOTの4種() {
        String base = demo("4-cas-brief");
        String time = "TOT 1530Z (readback)";
        ScenarioImportResult r = parse(base.replace(time, "Standby TOT (readback)"), ScenarioKind.CAS_BRIEF);
        assertEquals(TotType.STANDBY, r.casBrief().tot());
        assertNull(r.casBrief().totTime());
        r = parse(base.replace(time, "TOT will be issued after correlation (readback)"), ScenarioKind.CAS_BRIEF);
        assertEquals(TotType.AFTER_CORRELATION, r.casBrief().tot());
        r = parse(base.replace(time, "Push when ready (readback)"), ScenarioKind.CAS_BRIEF);
        assertEquals(TotType.PUSH_WHEN_READY, r.casBrief().tot());
        r = parse(base.replace(time, "Standby TOT (readback)\nTOT 1600Z (readback)"), ScenarioKind.CAS_BRIEF);
        assertEquals(TotType.STANDBY, r.casBrief().tot());
        assertEquals(Arrays.asList("TOT（重複）"), r.skippedItems());
    }

    @Test
    public void d8_CASBriefのDangerCloseとその他の制限() {
        String text = demo("4-cas-brief").replace("FAHs: 060-120 (readback)",
                "Danger close, initials AB (readback)\nACAs: alt 5000 (readback)\n"
                        + "Separation: 1 km (readback)\nRW approval out of BP: yes (readback)\nPLA: none (readback)");
        ScenarioImportResult r = parse(text, ScenarioKind.CAS_BRIEF);
        assertNull(r.error());
        assertEquals(Boolean.TRUE, r.casBrief().dangerClose());
        assertEquals("AB", r.casBrief().dangerCloseInitials());
        assertEquals("alt 5000", r.casBrief().acas());
        assertEquals("1 km", r.casBrief().separation());
        assertEquals("yes", r.casBrief().rwOutOfBp());
        assertEquals("none", r.casBrief().pla());
        assertNull(r.casBrief().fahs());
        assertTrue(r.skippedItems().isEmpty());
    }

    // ---- D10: 行の順番 ----
    // ---- D10: Line order ----

    @Test
    public void d10_行の順番を入れ替えても同じ() {
        String[] names = {"1-routing", "2-check-in", "3-game-plan", "4-cas-brief"};
        ScenarioKind[] kinds = {
            ScenarioKind.ROUTING, ScenarioKind.CHECK_IN, ScenarioKind.GAME_PLAN, ScenarioKind.CAS_BRIEF
        };
        for (int i = 0; i < names.length; i++) {
            String base = demo(names[i]);
            List<String> all = new ArrayList<>(Arrays.asList(base.split("\n")));
            String title = all.get(1);
            List<String> body = new ArrayList<>(all.subList(2, all.size() - 1));
            Collections.shuffle(body, new Random(7 + i));
            List<String> shuffled = new ArrayList<>();
            shuffled.add(U1);
            shuffled.add(title);
            shuffled.addAll(body);
            shuffled.add(U1);
            ScenarioImportResult r = parse(String.join("\n", shuffled), kinds[i]);
            assertNull(names[i], r.error());
            assertTrue(names[i] + " " + r.skippedItems(), r.skippedItems().isEmpty());
            assertEquals(names[i], textOf(parse(base, kinds[i])), textOf(r));
        }
    }

    // ---- D11: ゲームプランの呼出符号 ----
    // ---- D11: Game plan callsign ----

    @Test
    public void d11_呼出符号が2行で違えば1行目を使い知らせる() {
        String text = join(U1, "GAME PLAN (JP 3-09.3 2014 Fig. V-9)",
                "KESTREL 11, advise when ready for game plan.",
                "OWL 21, Type 2, bomb on target; advise when ready for 9-line.", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.GAME_PLAN);
        assertNull(r.error());
        assertEquals("KESTREL 11", r.gamePlan().callsign());
        assertEquals(TypeOfControl.TYPE_2, r.gamePlan().typeOfControl());
        assertEquals(Arrays.asList("Callsign（2 か所で違う）"), r.skippedItems());
    }

    // ---- D12: 必須の行がない ----
    // ---- D12: Missing required lines ----

    @Test
    public void d12_ルーティングの本文がない() {
        String text = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)", "Send your check-in.", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.ROUTING);
        assertNull(r.error());
        assertEquals(RoutingClosing.SEND, r.routing().closing());
        assertEquals(Arrays.asList("Routing/Holding（なし）"), r.skippedItems());
    }

    @Test
    public void d12_CASBriefの6行目がない() {
        String text = demo("4-cas-brief").replace("6 Target location: 54S UE 12345 67890 (readback)\n", "");
        ScenarioImportResult r = parse(text, ScenarioKind.CAS_BRIEF);
        assertNull(r.error());
        assertNull(r.casBrief().line6());
        assertEquals(Arrays.asList("9-line 6（なし）"), r.skippedItems());
    }

    @Test
    public void d12_チェックインの必須の項目がない() {
        String text = join(U1, "CHECK-IN (JP 3-09.3 2014 Fig. V-4)", "1 Mission number: TR1", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.CHECK_IN);
        assertNull(r.error());
        assertEquals(Arrays.asList("JTAC callsign（なし）", "Aircraft callsign（なし）",
                "2 Number and type of aircraft（なし）", "3 Position and altitude（なし）",
                "4 Ordnance（なし）", "5 Playtime（なし）", "6 Capabilities（なし）",
                "7 Abort code（なし）"), r.skippedItems());
    }

    @Test
    public void d12_CASBriefの1から3行がなければ4から9行だけ必須() {
        String text = demo("4-cas-brief")
                .replace("[1-3]\n1 IP/BP: IP PINE\n2 Heading: 095 magnetic, Offset left\n3 Distance: 8.5 NM\n", "")
                .replace("4 Target elevation: 320 ft MSL (readback)", "4 Target elevation: Elevation 320 ft MSL (readback)");
        ScenarioImportResult r = parse(text, ScenarioKind.CAS_BRIEF);
        assertNull(r.error());
        assertTrue(r.skippedItems().toString(), r.skippedItems().isEmpty());
        assertNull(r.casBrief().line1());
        assertEquals("320 ft MSL", r.casBrief().line4());
    }

    // ---- D13: ゲームプランの値に ", " や "; " がある ----
    // ---- D13: Game plan values containing ", " or "; " ----

    @Test
    public void d13_値に区切りの文字があっても読める() {
        String text = join(U1, "GAME PLAN (JP 3-09.3 2014 Fig. V-9)",
                "KESTREL 11, FLIGHT, advise when ready for game plan.",
                "KESTREL 11, FLIGHT, Type 1, bomb on coordinate, intent destroy, then reattack, "
                        + "ordnance sample A; sample B, interval 2 s, line 6 10 digits; advise when ready for 9-line.",
                U1);
        ScenarioImportResult r = parse(text, ScenarioKind.GAME_PLAN);
        assertNull(r.error());
        assertTrue(r.skippedItems().toString(), r.skippedItems().isEmpty());
        GamePlanImport g = r.gamePlan();
        assertEquals("KESTREL 11, FLIGHT", g.callsign());
        assertEquals(TypeOfControl.TYPE_1, g.typeOfControl());
        assertEquals(MethodOfAttack.BOC, g.methodOfAttack());
        assertEquals("destroy, then reattack", g.intent());
        assertNull(g.collateralConcerns());
        assertEquals("sample A; sample B", g.ordnance());
        assertNull(g.desiredEffects());
        assertNull(g.fuzing());
        assertEquals("2 s", g.interval());
        assertEquals("10", g.line6Digits());
    }

    @Test
    public void d13_呼出符号がなく任意の部分もない() {
        String text = join(U1, "GAME PLAN (JP 3-09.3 2014 Fig. V-9)",
                "Advise when ready for game plan.",
                "Type 3, bomb on target; advise when ready for 9-line.", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.GAME_PLAN);
        assertNull(r.error());
        assertTrue(r.skippedItems().isEmpty());
        assertNull(r.gamePlan().callsign());
        assertEquals(TypeOfControl.TYPE_3, r.gamePlan().typeOfControl());
        assertEquals(MethodOfAttack.BOT, r.gamePlan().methodOfAttack());
    }

    // ---- D14: read ----

    @Test
    public void d14_ちょうどMAX_BYTESは読めMAX_BYTES超はTOO_LARGE() throws IOException {
        byte[] base = bytes(demo("1-routing"));
        byte[] exact = new byte[ScenarioImporter.MAX_BYTES];
        Arrays.fill(exact, (byte) '\n');
        System.arraycopy(base, 0, exact, 0, base.length);
        ScenarioImportResult ok = ScenarioImporter.read(
                new ByteArrayInputStream(exact), ScenarioKind.ROUTING, Lang.JA);
        assertNull(ok.error());
        assertEquals("KESTREL 11", ok.routing().callsign());

        byte[] over = new byte[ScenarioImporter.MAX_BYTES + 1];
        Arrays.fill(over, (byte) '\n');
        System.arraycopy(base, 0, over, 0, base.length);
        ScenarioImportResult ng = ScenarioImporter.read(
                new ByteArrayInputStream(over), ScenarioKind.ROUTING, Lang.JA);
        assertEquals(ScenarioImportError.Code.TOO_LARGE, ng.error().code());
    }

    /**
     * 終わらない入力。読んだ量と閉じられたかを数える。
     * <p>
     * Endless input. Tracks the amount read and whether it was closed.
     */
    private static final class EndlessStream extends InputStream {
        private int counted;
        private boolean closed;

        @Override
        public int read() {
            counted++;
            return 'a';
        }

        @Override
        public int read(byte[] b, int off, int len) {
            Arrays.fill(b, off, off + len, (byte) 'a');
            counted += len;
            return len;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /**
     * 読むと必ず失敗する入力。
     * <p>
     * Input that always fails on reading.
     */
    private static final class BrokenStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("boom");
        }
    }

    @Test
    public void d14_readはMAX_BYTES_プラス1バイトまでしか読まず閉じない() throws IOException {
        try (EndlessStream in = new EndlessStream()) {
            ScenarioImportResult r = ScenarioImporter.read(in, ScenarioKind.ROUTING, Lang.JA);
            assertEquals(ScenarioImportError.Code.TOO_LARGE, r.error().code());
            assertTrue("読んだ量: " + in.counted, in.counted <= ScenarioImporter.MAX_BYTES + 1);
            assertFalse(in.closed);
        }
    }

    @Test
    public void d14_読めなければIOException() throws IOException {
        try (BrokenStream in = new BrokenStream()) {
            ScenarioImporter.read(in, ScenarioKind.ROUTING, Lang.JA);
            fail("IOException になるはず");
        } catch (IOException expected) {
            assertEquals("boom", expected.getMessage());
        }
    }

    @Test
    public void d14_引数がnullならIllegalArgumentException() throws IOException {
        for (int i = 0; i < 4; i++) {
            try {
                switch (i) {
                    case 0:
                        ScenarioImporter.read(null, ScenarioKind.ROUTING, Lang.JA);
                        break;
                    case 1:
                        ScenarioImporter.read(new ByteArrayInputStream(new byte[0]), null, Lang.JA);
                        break;
                    case 2:
                        ScenarioImporter.parse(null, ScenarioKind.ROUTING, Lang.JA);
                        break;
                    default:
                        ScenarioImporter.parse(new byte[0], null, Lang.JA);
                        break;
                }
                fail("case " + i);
            } catch (IllegalArgumentException expected) {
                // 期待どおり
                // As expected.
            }
        }
    }

    // ---- D15: ゲームプランの読めない値 ----
    // ---- D15: Unreadable game plan values ----

    @Test
    public void d15_型や方法が読めなければ読めないと知らせる() {
        String bad = join(U1, "GAME PLAN (JP 3-09.3 2014 Fig. V-9)",
                "KESTREL 11, advise when ready for game plan.",
                "KESTREL 11, Type 9, bomb on target, intent X; advise when ready for 9-line.", U1);
        ScenarioImportResult r = parse(bad, ScenarioKind.GAME_PLAN);
        assertNull(r.error());
        assertNull(r.gamePlan().typeOfControl());
        assertEquals(MethodOfAttack.BOT, r.gamePlan().methodOfAttack());
        assertEquals("X", r.gamePlan().intent());
        assertEquals(Arrays.asList("Type of control（読めない）"), r.skippedItems());

        String bad2 = join(U1, "GAME PLAN (JP 3-09.3 2014 Fig. V-9)",
                "Type 2, bomb on moon, intent X; advise when ready for 9-line.", U1);
        r = parse(bad2, ScenarioKind.GAME_PLAN);
        assertEquals(TypeOfControl.TYPE_2, r.gamePlan().typeOfControl());
        assertNull(r.gamePlan().methodOfAttack());
        assertEquals(Arrays.asList("Method of attack（読めない）"), r.skippedItems());
    }

    @Test
    public void d15_頭が2つに分けられなければ両方読めない() {
        String text = join(U1, "GAME PLAN (JP 3-09.3 2014 Fig. V-9)",
                "Type 2; advise when ready for 9-line.", "intent X", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.GAME_PLAN);
        assertNotNull(r.error());
        assertEquals(ScenarioImportError.Code.NOTHING_READ, r.error().code());
    }

    @Test
    public void d15_頭が分けられなくても他の項目があれば読める() {
        String text = join(U1, "GAME PLAN (JP 3-09.3 2014 Fig. V-9)",
                "KESTREL 11, advise when ready for game plan.",
                "KESTREL 11; advise when ready for 9-line.", U1);
        ScenarioImportResult r = parse(text, ScenarioKind.GAME_PLAN);
        // 呼出符号は 1 行目から読める
        // The callsign can be read from the first line.
        assertNull(r.error());
        assertEquals("KESTREL 11", r.gamePlan().callsign());
        assertTrue(r.skippedItems().contains("Type of control（読めない）"));
        assertTrue(r.skippedItems().contains("Method of attack（読めない）"));
    }

    // ---- D17: 言語 ----
    // ---- D17: Language ----

    @Test
    public void d17_エラーの文() {
        assertError(ScenarioImporter.parse(new byte[ScenarioImporter.MAX_BYTES + 1], ScenarioKind.ROUTING, Lang.JA),
                "ファイルが大きすぎます（256 KiB まで）。");
        assertError(ScenarioImporter.parse(new byte[ScenarioImporter.MAX_BYTES + 1], ScenarioKind.ROUTING, Lang.EN),
                "The file is too large (up to 256 KiB).");
        byte[] bad = {'a', (byte) 0xC3, 0x28};
        assertError(ScenarioImporter.parse(bad, ScenarioKind.ROUTING, Lang.JA), "UTF-8 のテキストとして読めません。");
        assertError(ScenarioImporter.parse(bad, ScenarioKind.ROUTING, Lang.EN), "Cannot read the file as UTF-8 text.");
        assertError(ScenarioImporter.parse(new byte[0], ScenarioKind.ROUTING, Lang.JA), "ファイルに何も書かれていません。");
        assertError(ScenarioImporter.parse(new byte[0], ScenarioKind.ROUTING, Lang.EN), "The file is empty.");
        assertError(parse("hello", ScenarioKind.ROUTING, Lang.JA),
                "題名の行が JTAK の書式ではありません。1 行目（TRAINING USE ONLY の行を除く）に書式の題名を書いてください。");
        assertError(parse("hello", ScenarioKind.ROUTING, Lang.EN),
                "The title line is not a JTAK format. Write the format title on the first line "
                        + "(excluding the TRAINING USE ONLY line).");
        assertError(parse(demo("1-routing"), ScenarioKind.CHECK_IN, Lang.JA),
                "このファイルは ルーティング の書式です。ルーティング の画面で読み込んでください。");
        assertError(parse(demo("1-routing"), ScenarioKind.CHECK_IN, Lang.EN),
                "This file is in the Routing format. Load it on the Routing screen.");
        assertError(parse(demo("2-check-in"), ScenarioKind.GAME_PLAN, Lang.JA),
                "このファイルは チェックイン の書式です。チェックイン の画面で読み込んでください。");
        assertError(parse(demo("2-check-in"), ScenarioKind.GAME_PLAN, Lang.EN),
                "This file is in the Check-in format. Load it on the Check-in screen.");
        assertError(parse(demo("3-game-plan"), ScenarioKind.CAS_BRIEF, Lang.EN),
                "This file is in the Game plan format. Load it on the Game plan screen.");
        assertError(parse(demo("3-game-plan"), ScenarioKind.CAS_BRIEF, Lang.JA),
                "このファイルは ゲームプラン の書式です。ゲームプラン の画面で読み込んでください。");
        assertError(parse(demo("4-cas-brief"), ScenarioKind.ROUTING, Lang.EN),
                "This file is in the CAS Brief format. Load it on the CAS Brief screen.");
        assertError(parse(template("routing"), ScenarioKind.ROUTING, Lang.JA), "読み込める項目がありませんでした。");
        assertError(parse(template("routing"), ScenarioKind.ROUTING, Lang.EN), "No items could be read.");
    }

    private static void assertError(ScenarioImportResult r, String message) {
        assertNotNull(r.error());
        assertEquals(message, r.error().message());
    }

    @Test
    public void d17_飛ばした項目の理由() {
        String routing = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)",
                "KESTREL 11, maintain IP PINE.",
                "Other instructions: a",
                "Other instructions: b",
                "Status calls: （未入力）",
                "Mystery line",
                "You are the only aircraft on station.",
                "Other AOS: OWL 31", U1);
        // 行番号: 1 U1・2 題名・3 本文・4〜5 Other instructions・6 Status calls・7 Mystery line
        // Line numbers: 1 U1; 2 Title; 3 Body; 4–5 Other instructions; 6 Status calls; 7 Mystery line
        assertEquals(Arrays.asList("Other instructions（重複）", "Status calls（未入力）", "7 行目",
                "Only aircraft on station（組み合わせ）", "Other AOS（組み合わせ）"),
                parse(routing, ScenarioKind.ROUTING, Lang.JA).skippedItems());
        assertEquals(Arrays.asList("Other instructions (duplicate)", "Status calls (not entered)", "line 7",
                "Only aircraft on station (conflict)", "Other AOS (conflict)"),
                parse(routing, ScenarioKind.ROUTING, Lang.EN).skippedItems());

        String noBody = join(U1, "ROUTING (JP 3-09.3 2014 Fig. V-3)", "Send your check-in.", U1);
        assertEquals(Arrays.asList("Routing/Holding (missing)"),
                parse(noBody, ScenarioKind.ROUTING, Lang.EN).skippedItems());

        String gp = join(U1, "GAME PLAN (JP 3-09.3 2014 Fig. V-9)",
                "KESTREL 11, advise when ready for game plan.",
                "OWL 21, Type 9, bomb on target; advise when ready for 9-line.", U1);
        assertEquals(Arrays.asList("Callsign (differs in 2 places)", "Type of control (unreadable)"),
                new ArrayList<>(sorted(parse(gp, ScenarioKind.GAME_PLAN, Lang.EN).skippedItems())));
        assertEquals(Arrays.asList("Callsign（2 か所で違う）", "Type of control（読めない）"),
                new ArrayList<>(sorted(parse(gp, ScenarioKind.GAME_PLAN, Lang.JA).skippedItems())));
    }

    private static List<String> sorted(List<String> in) {
        List<String> l = new ArrayList<>(in);
        Collections.sort(l);
        return l;
    }

    @Test
    public void d17_langがnullならNullPointerExceptionで先に投げる() throws IOException {
        try {
            ScenarioImporter.parse(new byte[0], ScenarioKind.ROUTING, null);
            fail();
        } catch (NullPointerException expected) {
            assertEquals("lang", expected.getMessage());
        }
        try {
            ScenarioImporter.parse(null, null, null);
            fail();
        } catch (NullPointerException expected) {
            assertEquals("lang", expected.getMessage());
        }
        try {
            ScenarioImporter.read(null, null, null);
            fail();
        } catch (NullPointerException expected) {
            assertEquals("lang", expected.getMessage());
        }
        try {
            ScenarioImporter.read(new ByteArrayInputStream(new byte[0]), ScenarioKind.ROUTING, null);
            fail();
        } catch (NullPointerException expected) {
            assertEquals("lang", expected.getMessage());
        }
    }
}
