package jp.jtak.core.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jp.jtak.core.Lang;
import jp.jtak.core.cas.CasBriefFormatter;
import jp.jtak.core.cas.CasRemarks;
import jp.jtak.core.cas.CasRestrictions;
import jp.jtak.core.cas.CheckIn;
import jp.jtak.core.cas.CheckInFormatter;
import jp.jtak.core.cas.GamePlan;
import jp.jtak.core.cas.GamePlanFormatter;
import jp.jtak.core.cas.NineLine;
import jp.jtak.core.cas.Routing;
import jp.jtak.core.cas.RoutingClosing;
import jp.jtak.core.cas.RoutingFormatter;

/**
 * importer の試験の共通の部品。サンプルの読み込みと、DTO から書式の文字の並びへの組み直し。
 * <p>
 * Shared importer test components: sample loading and rebuilding formatted text from DTOs.
 */
final class TestSupport {
    static final String U1 = "TRAINING USE ONLY / 訓練用";

    static final String T_ROUTING = "ROUTING (JP 3-09.3 2014 Fig. V-3)";
    static final String T_CHECK_IN = "CHECK-IN (JP 3-09.3 2014 Fig. V-4)";
    static final String T_GAME_PLAN = "GAME PLAN (JP 3-09.3 2014 Fig. V-9)";
    static final String T_CAS_BRIEF = "CAS 9-LINE (JP 3-09.3 2014 Fig. V-9) / Datum: WGS 84";

    private TestSupport() {}

    /**
     * 作業ディレクトリから上へたどって {@code docs/interfaces/scenario-samples} を探す。
     * <p>
     * Searches upward from the working directory for {@code docs/interfaces/scenario-samples}.
     */
    static Path samplesDir() {
        Path p = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (p != null) {
            Path c = p.resolve("docs/interfaces/scenario-samples");
            if (Files.isDirectory(c)) {
                return c;
            }
            p = p.getParent();
        }
        throw new IllegalStateException("scenario-samples が見つからない");
    }

    static String demo(String name) {
        return read("demo/jtak-demo-" + name + ".txt");
    }

    static String template(String name) {
        return read("template/jtak-template-" + name + ".txt");
    }

    private static String read(String rel) {
        try {
            return new String(Files.readAllBytes(samplesDir().resolve(rel)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    static ScenarioImportResult parse(String text, ScenarioKind kind) {
        return ScenarioImporter.parse(bytes(text), kind, Lang.JA);
    }

    static ScenarioImportResult parse(String text, ScenarioKind kind, Lang lang) {
        return ScenarioImporter.parse(bytes(text), kind, lang);
    }

    /**
     * text の行のうち、prefix で始まる最初の行の、prefix のあとの値（前後の空白は除く）。
     * <p>
     * Value after prefix in the first line of text starting with prefix (trimmed).
     */
    static String lineValue(String text, String prefix) {
        for (String line : text.split("\\r?\\n")) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        throw new IllegalStateException("行がない: " + prefix);
    }

    /**
     * text の中で、start のあとから end の前までの文字（end が最初に現れる所まで）。
     * <p>
     * Text between start and end in text (up to the first occurrence of end).
     */
    static String between(String text, String start, String end) {
        int from = text.indexOf(start);
        if (from < 0) {
            throw new IllegalStateException("文字がない: " + start);
        }
        from += start.length();
        int to = text.indexOf(end, from);
        if (to < 0) {
            throw new IllegalStateException("文字がない: " + end);
        }
        return text.substring(from, to);
    }

    static String join(String... lines) {
        return String.join("\n", lines);
    }

    // ---- DTO から書式の文字の並びへ（null は空として扱う）----
    // ---- DTO to formatted text (treat null as empty) ----

    static Routing toModel(RoutingImport r) {
        return new Routing.Builder()
                .callsign(r.callsign())
                .instruction(r.instruction())
                .place(r.place())
                .otherInstructions(r.otherInstructions())
                .onlyAircraftOnStation(Boolean.TRUE.equals(r.onlyAircraftOnStation()))
                .otherAos(r.otherAos())
                .threats(r.threats())
                .weatherTerrain(r.weatherTerrain())
                .statusCalls(r.statusCalls())
                .closing(r.closing() == null ? RoutingClosing.NONE : r.closing())
                .holdReason(r.holdReason())
                .build();
    }

    static String text(RoutingImport r) {
        return RoutingFormatter.format(toModel(r)).text();
    }

    static CheckIn toModel(CheckInImport c) {
        return new CheckIn.Builder()
                .jtacCallsign(c.jtacCallsign())
                .aircraftCallsign(c.aircraftCallsign())
                .mode(c.mode())
                .missionNumber(c.missionNumber())
                .aircraft(c.aircraft())
                .positionAltitude(c.positionAltitude())
                .ordnance(c.ordnance())
                .playtime(c.playtime())
                .capabilities(c.capabilities())
                .abortCode(c.abortCode())
                .abortCodeNone(Boolean.TRUE.equals(c.abortCodeNone()))
                .build();
    }

    static String text(CheckInImport c) {
        return CheckInFormatter.format(toModel(c)).text();
    }

    static GamePlan toModel(GamePlanImport g) {
        return new GamePlan.Builder()
                .callsign(g.callsign())
                .typeOfControl(g.typeOfControl())
                .methodOfAttack(g.methodOfAttack())
                .intent(g.intent())
                .collateralConcerns(g.collateralConcerns())
                .ordnance(g.ordnance())
                .desiredEffects(g.desiredEffects())
                .fuzing(g.fuzing())
                .interval(g.interval())
                .line6Digits(g.line6Digits())
                .build();
    }

    static String text(GamePlanImport g) {
        return GamePlanFormatter.format(toModel(g)).text();
    }

    static String text(CasBriefImport b) {
        NineLine nine = new NineLine.Builder()
                .line1(b.line1())
                .line2(b.line2())
                .offset(b.offset())
                .line3(b.line3())
                .line4(b.line4())
                .line5(b.line5())
                .line6(b.line6())
                .line7(b.line7())
                .line8(b.line8())
                .line9(b.line9())
                .build();
        CasRemarks remarks = new CasRemarks.Builder()
                .ltlPtl(b.ltlPtl())
                .ordnance(b.remarksOrdnance())
                .threat(b.threat())
                .additionalRemarks(b.additionalRemarks())
                .additionalCalls(b.additionalCalls())
                .build();
        boolean dc = Boolean.TRUE.equals(b.dangerClose());
        CasRestrictions restrictions = new CasRestrictions.Builder()
                .dangerClose(dc)
                .dangerCloseInitials(dc ? b.dangerCloseInitials() : null)
                .acas(b.acas())
                .fahs(b.fahs())
                .separation(b.separation())
                .rwOutOfBp(b.rwOutOfBp())
                .pla(b.pla())
                .tot(b.tot())
                .totTime(b.totTime())
                .build();
        return CasBriefFormatter.format(nine, remarks, restrictions).text();
    }
}
