package jp.jtak.core.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jp.jtak.core.cas.CasBriefFormatter;
import jp.jtak.core.cas.CasRemarks;
import jp.jtak.core.cas.CasRestrictions;
import jp.jtak.core.cas.CheckIn;
import jp.jtak.core.cas.CheckInFormatter;
import jp.jtak.core.cas.CheckInMode;
import jp.jtak.core.cas.GamePlan;
import jp.jtak.core.cas.GamePlanFormatter;
import jp.jtak.core.cas.MethodOfAttack;
import jp.jtak.core.cas.NineLine;
import jp.jtak.core.cas.Routing;
import jp.jtak.core.cas.RoutingClosing;
import jp.jtak.core.cas.RoutingFormatter;
import jp.jtak.core.cas.RoutingInstruction;
import jp.jtak.core.cas.TotType;
import jp.jtak.core.cas.TypeOfControl;
import org.junit.Test;

/**
 * D9: 書式を作り、読み込み、もう一度書式にしても同じ文字の並びになること（設計 06 の 2.8）。
 * <p>
 * D9: Formatting, importing, and formatting again produces the same text (design 06 §2.8).
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class ScenarioRoundTripTest {

    /**
     * 空と空白だけは null にそろえる（読み込みでは「ない」は null、model では null か空のどちらもあるため）。
     * <p>
     * Normalizes empty and whitespace-only values to null (import uses null for “absent,” while the model may
     * contain null or empty strings).
     */
    private static String n(String v) {
        if (v == null || v.trim().isEmpty()) {
            return null;
        }
        return v.trim();
    }

    private static void same(String field, String modelValue, String readValue) {
        assertEquals(field, n(modelValue), readValue);
    }

    // ---- ルーティング ----
    // ---- Routing ----

    private static void roundTripRouting(Routing model) {
        String before = RoutingFormatter.format(model).text();
        ScenarioImportResult r = TestSupport.parse(before, ScenarioKind.ROUTING);
        assertNull(before, r.error());
        assertTrue(before + "\n" + r.skippedItems(), r.skippedItems().isEmpty());
        assertEquals(before, TestSupport.text(r.routing()));
        sameFields(model, r.routing());
    }

    /**
     * 読んだ値を model と欄ごとに比べる（D9。文の一致とは別）。
     * <p>
     * Compares imported values with the model field by field (D9; separate from text equality).
     */
    private static void sameFields(Routing m, RoutingImport x) {
        same("callsign", m.callsign(), x.callsign());
        assertEquals("instruction", m.instruction(), x.instruction());
        same("place", m.place(), x.place());
        same("otherInstructions", m.otherInstructions(), x.otherInstructions());
        assertEquals("onlyAircraftOnStation", m.onlyAircraftOnStation(),
                Boolean.TRUE.equals(x.onlyAircraftOnStation()));
        same("otherAos", m.otherAos(), x.otherAos());
        same("threats", m.threats(), x.threats());
        same("weatherTerrain", m.weatherTerrain(), x.weatherTerrain());
        same("statusCalls", m.statusCalls(), x.statusCalls());
        assertEquals("closing", m.closing() == null ? RoutingClosing.NONE : m.closing(),
                x.closing() == null ? RoutingClosing.NONE : x.closing());
        same("holdReason", m.holdReason(), x.holdReason());
    }

    private static Routing.Builder routing(String callsign, RoutingInstruction ins) {
        return new Routing.Builder().callsign(callsign).instruction(ins).place("IP PINE");
    }

    @Test
    public void d9_ルーティング_呼出符号あり() {
        roundTripRouting(routing("KESTREL 11", RoutingInstruction.PROCEED_REPORT_ESTABLISHED)
                .otherInstructions("maintain 15000 ft MSL or above")
                .onlyAircraftOnStation(true)
                .threats("simulated MANPADS")
                .weatherTerrain("clear")
                .statusCalls("report established")
                .closing(RoutingClosing.SEND)
                .build());
    }

    @Test
    public void d9_ルーティング_呼出符号なしと3つの指示() {
        for (RoutingInstruction ins : RoutingInstruction.values()) {
            roundTripRouting(routing(null, ins).build());
            roundTripRouting(routing("OWL 31", ins).closing(RoutingClosing.SEND).build());
        }
    }

    @Test
    public void d9_ルーティング_Hold() {
        roundTripRouting(routing("KESTREL 11", RoutingInstruction.MAINTAIN).closing(RoutingClosing.HOLD).build());
        roundTripRouting(routing("KESTREL 11", RoutingInstruction.MAINTAIN)
                .closing(RoutingClosing.HOLD).holdReason("standby for tasking").build());
    }

    @Test
    public void d9_ルーティング_他の航空機() {
        roundTripRouting(routing("KESTREL 11", RoutingInstruction.MAINTAIN).otherAos("OWL 31, OWL 32").build());
    }

    // ---- チェックイン ----
    // ---- Check-in ----

    private static void roundTripCheckIn(CheckIn model) {
        String before = CheckInFormatter.format(model).text();
        ScenarioImportResult r = TestSupport.parse(before, ScenarioKind.CHECK_IN);
        assertNull(before, r.error());
        assertTrue(before + "\n" + r.skippedItems(), r.skippedItems().isEmpty());
        assertEquals(before, TestSupport.text(r.checkIn()));
        sameFields(model, r.checkIn());
    }

    private static void sameFields(CheckIn m, CheckInImport x) {
        same("jtacCallsign", m.jtacCallsign(), x.jtacCallsign());
        same("aircraftCallsign", m.aircraftCallsign(), x.aircraftCallsign());
        assertEquals("mode", m.mode(), x.mode());
        same("missionNumber", m.missionNumber(), x.missionNumber());
        same("aircraft", m.aircraft(), x.aircraft());
        same("positionAltitude", m.positionAltitude(), x.positionAltitude());
        same("ordnance", m.ordnance(), x.ordnance());
        same("playtime", m.playtime(), x.playtime());
        same("capabilities", m.capabilities(), x.capabilities());
        assertEquals("abortCodeNone", m.abortCodeNone(), Boolean.TRUE.equals(x.abortCodeNone()));
        // 「none」のときは Abort code の文字を書き出さないので、読んだ値は null（元の欄にも書いていない）
        // For “none,” Abort code text is not exported, so the imported value is null (also absent in the original field).
        same("abortCode", m.abortCodeNone() ? null : m.abortCode(), x.abortCode());
    }

    private static CheckIn.Builder checkIn(CheckInMode mode) {
        return new CheckIn.Builder()
                .jtacCallsign("OWL 21")
                .aircraftCallsign("KESTREL 11")
                .mode(mode)
                .missionNumber("TR0417")
                .aircraft("2 x strike fighter")
                .positionAltitude("IP PINE, 15000 ft MSL")
                .ordnance("4 x sample A, 2 x sample B")
                .playtime("45 minutes")
                .capabilities("targeting pod, laser designator");
    }

    @Test
    public void d9_チェックイン_3つのモードとAbortコード2種() {
        for (CheckInMode mode : CheckInMode.values()) {
            roundTripCheckIn(checkIn(mode).abortCode("BRAVO 7").build());
            roundTripCheckIn(checkIn(mode).abortCodeNone(true).build());
        }
    }

    @Test
    public void d9_チェックイン_FULLで一部の項目が空() {
        CheckIn model = new CheckIn.Builder()
                .jtacCallsign("OWL 21")
                .aircraftCallsign("KESTREL 11")
                .mode(CheckInMode.FULL)
                .missionNumber("TR0417")
                .positionAltitude("IP PINE, 15000 ft MSL")
                .playtime("45 minutes")
                .build();
        String text = CheckInFormatter.format(model).text();
        assertTrue(text, text.contains("（未入力）"));
        ScenarioImportResult r = TestSupport.parse(text, ScenarioKind.CHECK_IN);
        assertNull(text, r.error());
        // 空だった 4 項目（2 aircraft・4 Ordnance・6 Capabilities・7 Abort code）は「（未入力）」として知らせる
        // Report the four empty items (2 aircraft, 4 Ordnance, 6 Capabilities, and 7 Abort code) as “（未入力）”.
        assertEquals(java.util.Arrays.asList("2 Number and type of aircraft（未入力）", "4 Ordnance（未入力）",
                "6 Capabilities（未入力）", "7 Abort code（未入力）"), r.skippedItems());
        assertEquals(text, TestSupport.text(r.checkIn()));
        sameFields(model, r.checkIn());
        assertNull(r.checkIn().aircraft());
        assertNull(r.checkIn().abortCode());
    }

    // ---- ゲームプラン ----
    // ---- Game plan ----

    private static void roundTripGamePlan(GamePlan model) {
        String before = GamePlanFormatter.format(model).text();
        ScenarioImportResult r = TestSupport.parse(before, ScenarioKind.GAME_PLAN);
        assertNull(before, r.error());
        assertTrue(before + "\n" + r.skippedItems(), r.skippedItems().isEmpty());
        assertEquals(before, TestSupport.text(r.gamePlan()));
        sameFields(model, r.gamePlan());
    }

    private static void sameFields(GamePlan m, GamePlanImport x) {
        same("callsign", m.callsign(), x.callsign());
        assertEquals("typeOfControl", m.typeOfControl(), x.typeOfControl());
        assertEquals("methodOfAttack", m.methodOfAttack(), x.methodOfAttack());
        same("intent", m.intent(), x.intent());
        same("collateralConcerns", m.collateralConcerns(), x.collateralConcerns());
        same("ordnance", m.ordnance(), x.ordnance());
        same("desiredEffects", m.desiredEffects(), x.desiredEffects());
        same("fuzing", m.fuzing(), x.fuzing());
        same("interval", m.interval(), x.interval());
        same("line6Digits", m.line6Digits(), x.line6Digits());
    }

    private static GamePlan.Builder gamePlan(String callsign) {
        return new GamePlan.Builder()
                .callsign(callsign)
                .typeOfControl(TypeOfControl.TYPE_2)
                .methodOfAttack(MethodOfAttack.BOT);
    }

    @Test
    public void d9_ゲームプラン_呼出符号あり() {
        roundTripGamePlan(gamePlan("KESTREL 11")
                .intent("neutralize the site")
                .collateralConcerns("range boundary")
                .ordnance("1 x sample A")
                .desiredEffects("destroy")
                .fuzing("instantaneous")
                .interval("single")
                .line6Digits("8")
                .build());
    }

    @Test
    public void d9_ゲームプラン_呼出符号なしと最小() {
        roundTripGamePlan(gamePlan(null).build());
        roundTripGamePlan(gamePlan(null).typeOfControl(TypeOfControl.TYPE_1).methodOfAttack(MethodOfAttack.BOC).build());
    }

    @Test
    public void d9_ゲームプラン_値に区切りの文字がある() {
        roundTripGamePlan(gamePlan("KESTREL 11")
                .intent("destroy, then reattack")
                .ordnance("sample A; sample B")
                .desiredEffects("effects, with comma")
                .build());
    }

    @Test
    public void d9_ゲームプラン_呼出符号にカンマがある() {
        roundTripGamePlan(gamePlan("KESTREL 11, FLIGHT").intent("x").build());
        roundTripGamePlan(gamePlan("KESTREL 11, FLIGHT").build());
    }

    // ---- CAS Brief ----

    private static void roundTripCas(NineLine nine, CasRemarks remarks, CasRestrictions restrictions) {
        String before = CasBriefFormatter.format(nine, remarks, restrictions).text();
        ScenarioImportResult r = TestSupport.parse(before, ScenarioKind.CAS_BRIEF);
        assertNull(before, r.error());
        assertTrue(before + "\n" + r.skippedItems(), r.skippedItems().isEmpty());
        assertEquals(before, TestSupport.text(r.casBrief()));
        sameFields(nine, remarks, restrictions, r.casBrief());
    }

    private static void sameFields(NineLine nine, CasRemarks rem, CasRestrictions res, CasBriefImport x) {
        same("line1", nine.line(1), x.line1());
        same("line2", nine.line(2), x.line2());
        same("offset", nine.offset(), x.offset());
        same("line3", nine.line(3), x.line3());
        same("line4", nine.line(4), x.line4());
        same("line5", nine.line(5), x.line5());
        same("line6", nine.line(6), x.line6());
        same("line7", nine.line(7), x.line7());
        same("line8", nine.line(8), x.line8());
        // 9 行目だけは、頭の「Egress 」（大文字の E）を読み込みが取る。formatter は付け直すので文は同じ（2.4.4）
        // Only for line 9, import strips the leading “Egress ” (capital E). The formatter adds it back, so the text remains the same (§2.4.4).
        String line9 = n(nine.line(9));
        if (line9 != null && line9.startsWith("Egress ")) {
            line9 = line9.substring("Egress ".length());
        }
        assertEquals("line9", line9, x.line9());
        same("ltlPtl", rem.ltlPtl(), x.ltlPtl());
        same("remarksOrdnance", rem.ordnance(), x.remarksOrdnance());
        same("threat", rem.threat(), x.threat());
        same("additionalRemarks", rem.additionalRemarks(), x.additionalRemarks());
        same("additionalCalls", rem.additionalCalls(), x.additionalCalls());
        assertEquals("dangerClose", res.dangerClose(), Boolean.TRUE.equals(x.dangerClose()));
        same("dangerCloseInitials", res.dangerCloseInitials(), x.dangerCloseInitials());
        same("acas", res.acas(), x.acas());
        same("fahs", res.fahs(), x.fahs());
        same("separation", res.separation(), x.separation());
        same("rwOutOfBp", res.rwOutOfBp(), x.rwOutOfBp());
        same("pla", res.pla(), x.pla());
        assertEquals("tot", res.tot(), x.tot());
        same("totTime", res.totTime(), x.totTime());
    }

    private static NineLine.Builder nine() {
        return new NineLine.Builder()
                .line1("IP PINE")
                .line2("095 magnetic")
                .offset("left")
                .line3("8.5 NM")
                .line4("320 ft MSL")
                .line5("simulated SAM site, 1 x launcher vehicle")
                .line6("54S UE 12345 67890")
                .line7("laser, code 1688")
                .line8("south 1200 m")
                .line9("west to IP PINE, climb to 15000 ft MSL");
    }

    private static CasRemarks remarks() {
        return new CasRemarks.Builder()
                .ltlPtl("LTL 180")
                .ordnance("1 x sample A, destroy the vehicle")
                .threat("simulated MANPADS, no SEAD")
                .additionalRemarks("range boundary 2 km south")
                .additionalCalls("call IN with heading")
                .build();
    }

    private static CasRestrictions.Builder restrictions() {
        return new CasRestrictions.Builder();
    }

    @Test
    public void d9_CASBrief_全部入り() {
        roundTripCas(nine().build(), remarks(),
                restrictions().fahs("060-120").tot(TotType.TIME).totTime("1530Z").build());
    }

    @Test
    public void d9_CASBrief_1から3行がない() {
        roundTripCas(nine().line1(null).line2(null).offset(null).line3(null).build(),
                new CasRemarks.Builder().build(), restrictions().build());
    }

    @Test
    public void d9_CASBrief_9行目がEgressで始まる() {
        roundTripCas(nine().line9("Egress west").build(), new CasRemarks.Builder().build(), restrictions().build());
        roundTripCas(nine().line9("egress west").build(), new CasRemarks.Builder().build(), restrictions().build());
    }

    @Test
    public void d9_CASBrief_TOTの4種() {
        roundTripCas(nine().build(), new CasRemarks.Builder().build(),
                restrictions().tot(TotType.TIME).totTime("1530Z").build());
        roundTripCas(nine().build(), new CasRemarks.Builder().build(), restrictions().tot(TotType.STANDBY).build());
        roundTripCas(nine().build(), new CasRemarks.Builder().build(),
                restrictions().tot(TotType.AFTER_CORRELATION).build());
        roundTripCas(nine().build(), new CasRemarks.Builder().build(),
                restrictions().tot(TotType.PUSH_WHEN_READY).build());
    }

    @Test
    public void d9_CASBrief_DangerCloseとその他の制限() {
        roundTripCas(nine().build(), remarks(),
                restrictions()
                        .dangerClose(true)
                        .dangerCloseInitials("AB")
                        .acas("alt 5000")
                        .fahs("060-120")
                        .separation("1 km")
                        .rwOutOfBp("yes")
                        .pla("none")
                        .tot(TotType.STANDBY)
                        .build());
    }
}
