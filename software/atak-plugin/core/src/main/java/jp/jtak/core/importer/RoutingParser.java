package jp.jtak.core.importer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.jtak.core.cas.RoutingClosing;
import jp.jtak.core.cas.RoutingInstruction;

/**
 * ルーティングの行から {@link RoutingImport} を作る（設計 06 の 2.4.1）。
 * <p>
 * Builds {@link RoutingImport} from routing lines (design 06 §2.4.1).
 */
final class RoutingParser {
    private static final String BODY = "Routing/Holding";
    private static final String ONLY = "Only aircraft on station";
    private static final String OTHER_AOS = "Other AOS";

    private static final Pattern PROCEED =
            Pattern.compile("^(?:(.+?), )?(proceed|Proceed) (.+) and report established\\.$");
    private static final Pattern MAINTAIN = Pattern.compile("^(?:(.+?), )?(maintain|Maintain) (.+)\\.$");
    private static final Pattern PASSING = Pattern.compile("^(?:(.+?), )?(report passing|Report passing) (.+)\\.$");

    private RoutingParser() {}

    /**
     * 題名のあとの行を読む。読めた項目がなければ null。
     * <p>
     * Reads lines after the title. Returns null if no items could be read.
     */
    static RoutingImport parse(List<ScenarioLines.Line> lines, ImportLog log) {
        RoutingImport.Builder b = new RoutingImport.Builder();
        boolean body = false;
        boolean only = false;
        boolean conflict = false;
        for (ScenarioLines.Line line : lines) {
            String t = line.text();
            String v;
            if ((v = ScenarioLines.after(t, "Other instructions:")) != null) {
                if (log.claim("Other instructions")) {
                    b.otherInstructions = log.entered("Other instructions", v);
                }
            } else if ((v = ScenarioLines.after(t, "Other AOS:")) != null) {
                if (log.claim(OTHER_AOS)) {
                    b.otherAos = log.entered(OTHER_AOS, v);
                }
            } else if ((v = ScenarioLines.after(t, "Immediate threats:")) != null) {
                if (log.claim("Immediate threats")) {
                    b.threats = log.entered("Immediate threats", v);
                }
            } else if ((v = ScenarioLines.after(t, "Significant weather/terrain:")) != null) {
                if (log.claim("Significant weather/terrain")) {
                    b.weatherTerrain = log.entered("Significant weather/terrain", v);
                }
            } else if ((v = ScenarioLines.after(t, "Status calls:")) != null) {
                if (log.claim("Status calls")) {
                    b.statusCalls = log.entered("Status calls", v);
                }
            } else if ("You are the only aircraft on station.".equals(t)) {
                if (log.claim(ONLY)) {
                    only = true;
                    b.onlyAircraftOnStation = Boolean.TRUE;
                    log.hit();
                }
            } else if (isClosing(t)) {
                readClosing(t, b, log);
            } else if (!body && readBody(t, b, log)) {
                body = true;
            } else if (body && isBody(t)) {
                log.duplicate(BODY);
            } else {
                log.unknownLine(line.number());
            }
            // 「ほかに航空機はいない」と「Other AOS」は両立しない（2.5）。2 つめを見た時点で両方を飛ばす。
            // “No other aircraft” and “Other AOS” are mutually exclusive (§2.5). Skip both when the second is encountered.
            if (!conflict && only && b.otherAos != null) {
                conflict = true;
                log.conflict(ONLY);
                log.conflict(OTHER_AOS);
                b.onlyAircraftOnStation = null;
                b.otherAos = null;
                // 飛ばした 2 項目は「読めた」に数えない（設計 06 の 2.7 の 10）
                // Do not count the two skipped items as successfully read (design 06 §2.7, item 10).
                log.unhit();
                log.unhit();
            }
        }
        if (!body) {
            log.missing(BODY);
        }
        return log.anyRead() ? b.build() : null;
    }

    private static boolean isClosing(String t) {
        return "Send your check-in.".equals(t) || "Hold your check-in.".equals(t)
                || (t.startsWith("Hold your check-in, ") && t.endsWith("."));
    }

    private static void readClosing(String t, RoutingImport.Builder b, ImportLog log) {
        if (!log.claim("Closing")) {
            return;
        }
        log.hit();
        if ("Send your check-in.".equals(t)) {
            b.closing = RoutingClosing.SEND;
            return;
        }
        b.closing = RoutingClosing.HOLD;
        if (!"Hold your check-in.".equals(t)) {
            String reason = "Hold your check-in, ".length() < t.length() - 1
                    ? ScenarioLines.clean(t.substring("Hold your check-in, ".length(), t.length() - 1))
                    : null;
            b.holdReason = reason;
        }
    }

    private static boolean isBody(String t) {
        return PROCEED.matcher(t).matches() || MAINTAIN.matcher(t).matches() || PASSING.matcher(t).matches()
                || ScenarioLines.NOT_ENTERED.equals(t) || t.endsWith(", " + ScenarioLines.NOT_ENTERED);
    }

    /**
     * 本文の行なら読んで true。
     * <p>
     * Reads the line and returns true if it belongs to the body.
     */
    private static boolean readBody(String t, RoutingImport.Builder b, ImportLog log) {
        Matcher m = PROCEED.matcher(t);
        RoutingInstruction ins = RoutingInstruction.PROCEED_REPORT_ESTABLISHED;
        if (!m.matches()) {
            m = MAINTAIN.matcher(t);
            ins = RoutingInstruction.MAINTAIN;
        }
        if (!m.matches()) {
            m = PASSING.matcher(t);
            ins = RoutingInstruction.REPORT_PASSING;
        }
        if (m.matches()) {
            String callsign = ScenarioLines.clean(m.group(1));
            String place = ScenarioLines.clean(m.group(3));
            if (callsign != null && !ScenarioLines.NOT_ENTERED.equals(callsign)) {
                b.callsign = callsign;
                log.hit();
            }
            if (place == null || ScenarioLines.NOT_ENTERED.equals(place)) {
                log.notEntered(BODY);
            } else {
                b.instruction = ins;
                b.place = place;
                log.hit();
            }
            return true;
        }
        if (ScenarioLines.NOT_ENTERED.equals(t)) {
            log.notEntered(BODY);
            return true;
        }
        String suffix = ", " + ScenarioLines.NOT_ENTERED;
        if (t.endsWith(suffix)) {
            String callsign = ScenarioLines.clean(t.substring(0, t.length() - suffix.length()));
            if (callsign != null && !ScenarioLines.NOT_ENTERED.equals(callsign)) {
                b.callsign = callsign;
                log.hit();
            }
            log.notEntered(BODY);
            return true;
        }
        return false;
    }
}
