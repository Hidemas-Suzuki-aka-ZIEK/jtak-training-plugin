package jp.jtak.core.importer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.jtak.core.cas.CheckInMode;

/**
 * チェックインの行から {@link CheckInImport} を作る（設計 06 の 2.4.2）。
 * <p>
 * Builds {@link CheckInImport} from check-in lines (design 06 §2.4.2).
 */
final class CheckInParser {
    private static final String JTAC = "JTAC callsign";
    private static final String AIRCRAFT = "Aircraft callsign";
    private static final String MODE = "Mode";
    private static final String ABORT = "7 Abort code";
    private static final String ABORT_NONE = "none (standard ABORT, ABORT, ABORT)";
    private static final Pattern CALL = Pattern.compile("^(.+), this is (.+)$");
    private static final String[] LABELS = {
        "1 Mission number",
        "2 Number and type of aircraft",
        "3 Position and altitude",
        "4 Ordnance",
        "5 Playtime",
        "6 Capabilities",
    };

    private CheckInParser() {}

    /**
     * 題名のあとの行を読む。読めた項目がなければ null（モードの FULL だけでは読めたとしない）。
     * <p>
     * Reads lines after the title. Returns null if no items could be read (the FULL mode alone does not count as a
     * successfully read item).
     */
    static CheckInImport parse(List<ScenarioLines.Line> lines, ImportLog log) {
        CheckInImport.Builder b = new CheckInImport.Builder();
        String[] values = new String[LABELS.length];
        boolean hasMode = false;
        for (ScenarioLines.Line line : lines) {
            String t = line.text();
            int idx = labelIndex(t);
            if (idx >= 0) {
                String v = ScenarioLines.after(t, LABELS[idx] + ":");
                if (log.claim(LABELS[idx])) {
                    values[idx] = log.entered(LABELS[idx], v);
                }
            } else if (t.startsWith("7 Abort code:") && ScenarioLines.after(t, "7 Abort code:") != null) {
                readAbort(ScenarioLines.after(t, "7 Abort code:"), b, log);
            } else if ("As fragged.".equals(t)) {
                hasMode = readMode(b, CheckInMode.AS_FRAGGED, hasMode, log);
            } else if ("As fragged, with exception:".equals(t)) {
                hasMode = readMode(b, CheckInMode.WITH_EXCEPTION, hasMode, log);
            } else {
                Matcher m = CALL.matcher(t);
                if (m.matches() && !log.seen(JTAC)) {
                    readCall(m.group(1), m.group(2), b, log);
                } else if (m.matches()) {
                    log.duplicate(JTAC);
                } else {
                    log.unknownLine(line.number());
                }
            }
        }
        if (!hasMode) {
            b.mode = CheckInMode.FULL;
        }
        b.missionNumber = values[0];
        b.aircraft = values[1];
        b.positionAltitude = values[2];
        b.ordnance = values[3];
        b.playtime = values[4];
        b.capabilities = values[5];

        log.missingIfUnseen(JTAC);
        log.missingIfUnseen(AIRCRAFT);
        // 1〜7 行目は FULL のときだけ必須（CheckInFormatter と同じ。設計 06 の 2.4.2）
        // Lines 1–7 are required only in FULL mode (as in CheckInFormatter; design 06 §2.4.2).
        if (b.mode == CheckInMode.FULL) {
            for (String label : LABELS) {
                log.missingIfUnseen(label);
            }
            log.missingIfUnseen(ABORT);
        }
        return log.anyRead() ? b.build() : null;
    }

    private static int labelIndex(String t) {
        for (int i = 0; i < LABELS.length; i++) {
            if (ScenarioLines.after(t, LABELS[i] + ":") != null) {
                return i;
            }
        }
        return -1;
    }

    private static boolean readMode(CheckInImport.Builder b, CheckInMode mode, boolean already, ImportLog log) {
        if (already) {
            log.duplicate(MODE);
            return true;
        }
        log.claim(MODE);
        b.mode = mode;
        log.hit();
        return true;
    }

    private static void readCall(String jtac, String aircraft, CheckInImport.Builder b, ImportLog log) {
        log.claim(JTAC);
        log.claim(AIRCRAFT);
        b.jtacCallsign = log.entered(JTAC, jtac);
        b.aircraftCallsign = log.entered(AIRCRAFT, aircraft);
    }

    private static void readAbort(String raw, CheckInImport.Builder b, ImportLog log) {
        if (!log.claim(ABORT)) {
            return;
        }
        String v = ScenarioLines.withoutReadback(raw);
        if (ABORT_NONE.equals(v)) {
            b.abortCodeNone = Boolean.TRUE;
            log.hit();
            return;
        }
        String code = log.entered(ABORT, v);
        if (code != null) {
            b.abortCode = code;
            b.abortCodeNone = Boolean.FALSE;
        }
    }
}
