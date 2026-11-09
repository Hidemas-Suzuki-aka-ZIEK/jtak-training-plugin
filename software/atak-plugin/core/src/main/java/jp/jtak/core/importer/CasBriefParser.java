package jp.jtak.core.importer;

import java.util.List;
import jp.jtak.core.cas.TotType;

/**
 * CAS Brief（9-line・Remarks・Restrictions）の行から {@link CasBriefImport} を作る（設計 06 の 2.4.4）。
 * <p>
 * Builds {@link CasBriefImport} from CAS Brief (9-line, Remarks, and Restrictions) lines (design 06 §2.4.4).
 */
final class CasBriefParser {
    private static final String TOT = "TOT";
    private static final String OFFSET_SEP = ", Offset ";

    /**
     * 9-line の行ラベル（添字 0 が 1 行目）。
     * <p>
     * 9-line labels (index 0 is line 1).
     */
    private static final String[] NINE_LABELS = {
        "1 IP/BP:",
        "2 Heading:",
        "3 Distance:",
        "4 Target elevation:",
        "5 Target description:",
        "6 Target location:",
        "7 Type mark / Terminal guidance:",
        "8 Location of friendlies:",
        "9 Egress:",
    };

    /**
     * そのほかの項目のラベルと、知らせる名前（{@link #assignOther} の番号と同じ並び）。
     * <p>
     * Labels and report names for other items (in the same order as the indices of {@link #assignOther}).
     */
    private static final String[] OTHER_LABELS = {
        "LTL/PTL:",
        "Ordnance / Effects:",
        "Threat / SEAD:",
        "Additional remarks:",
        "Additional calls:",
        "ACAs:",
        "FAHs:",
        "Separation:",
        "RW approval out of BP:",
        "PLA:",
    };

    private CasBriefParser() {}

    /**
     * 題名のあとの行を読む。読めた項目がなければ null。
     * <p>
     * Reads lines after the title. Returns null if no items could be read.
     */
    static CasBriefImport parse(List<ScenarioLines.Line> lines, ImportLog log) {
        boolean header = hasLine123(lines);
        CasBriefImport.Builder b = new CasBriefImport.Builder();
        for (ScenarioLines.Line line : lines) {
            String t = line.text();
            if (isSectionHeader(t)) {
                continue;
            }
            if (readNine(t, header, b, log) || readOther(t, b, log) || readRestriction(t, b, log)) {
                continue;
            }
            log.unknownLine(line.number());
        }
        for (int i = 1; i <= 9; i++) {
            if (i >= 4 || header) {
                log.missingIfUnseen(nineName(i));
            }
        }
        return log.anyRead() ? b.build() : null;
    }

    private static boolean isSectionHeader(String t) {
        return "[1-3]".equals(t) || "[4-6]".equals(t) || "[7-9]".equals(t)
                || "[Remarks]".equals(t) || "[Restrictions]".equals(t);
    }

    private static boolean hasLine123(List<ScenarioLines.Line> lines) {
        for (ScenarioLines.Line line : lines) {
            for (int i = 0; i < 3; i++) {
                if (ScenarioLines.after(line.text(), NINE_LABELS[i]) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String nineName(int n) {
        return "9-line " + n;
    }

    private static boolean readNine(String t, boolean header, CasBriefImport.Builder b, ImportLog log) {
        for (int i = 0; i < NINE_LABELS.length; i++) {
            String raw = ScenarioLines.after(t, NINE_LABELS[i]);
            if (raw == null) {
                continue;
            }
            int n = i + 1;
            if (!log.claim(nineName(n))) {
                return true;
            }
            String v = ScenarioLines.withoutReadback(raw);
            if (n == 2) {
                readHeading(v, b, log);
                return true;
            }
            if (n == 9 && v.startsWith("Egress ")) {
                v = v.substring("Egress ".length());
            } else if (n == 4 && !header && v.startsWith("Elevation ")) {
                v = v.substring("Elevation ".length());
            }
            assignNine(b, n, log.entered(nineName(n), v));
            return true;
        }
        return false;
    }

    private static void readHeading(String v, CasBriefImport.Builder b, ImportLog log) {
        int at = v.lastIndexOf(OFFSET_SEP);
        String heading = at >= 0 ? v.substring(0, at) : v;
        b.line2 = log.entered(nineName(2), heading);
        if (at >= 0) {
            b.offset = log.entered("Offset", v.substring(at + OFFSET_SEP.length()));
        }
    }

    private static void assignNine(CasBriefImport.Builder b, int n, String v) {
        switch (n) {
            case 1 -> b.line1 = v;
            case 3 -> b.line3 = v;
            case 4 -> b.line4 = v;
            case 5 -> b.line5 = v;
            case 6 -> b.line6 = v;
            case 7 -> b.line7 = v;
            case 8 -> b.line8 = v;
            default -> b.line9 = v;
        }
    }

    private static boolean readOther(String t, CasBriefImport.Builder b, ImportLog log) {
        for (int i = 0; i < OTHER_LABELS.length; i++) {
            String raw = ScenarioLines.after(t, OTHER_LABELS[i]);
            if (raw == null) {
                continue;
            }
            String name = OTHER_LABELS[i].substring(0, OTHER_LABELS[i].length() - 1);
            if (log.claim(name)) {
                assignOther(b, i, log.entered(name, ScenarioLines.withoutReadback(raw)));
            }
            return true;
        }
        return false;
    }

    private static void assignOther(CasBriefImport.Builder b, int i, String v) {
        switch (i) {
            case 0 -> b.ltlPtl = v;
            case 1 -> b.remarksOrdnance = v;
            case 2 -> b.threat = v;
            case 3 -> b.additionalRemarks = v;
            case 4 -> b.additionalCalls = v;
            case 5 -> b.acas = v;
            case 6 -> b.fahs = v;
            case 7 -> b.separation = v;
            case 8 -> b.rwOutOfBp = v;
            default -> b.pla = v;
        }
    }

    /**
     * Danger close と TOT の行。
     * <p>
     * Danger close and TOT lines.
     */
    private static boolean readRestriction(String t, CasBriefImport.Builder b, ImportLog log) {
        String initials = ScenarioLines.after(t, "Danger close, initials");
        if (initials != null) {
            if (log.claim("Danger close")) {
                b.dangerClose = Boolean.TRUE;
                log.hit();
                b.dangerCloseInitials = log.entered("Danger close", ScenarioLines.withoutReadback(initials));
            }
            return true;
        }
        String line = ScenarioLines.withoutReadback(t);
        if ("Standby TOT".equals(line)) {
            readTot(b, TotType.STANDBY, null, log);
        } else if ("TOT will be issued after correlation".equals(line)) {
            readTot(b, TotType.AFTER_CORRELATION, null, log);
        } else if ("Push when ready".equals(line)) {
            readTot(b, TotType.PUSH_WHEN_READY, null, log);
        } else if (line.startsWith("TOT ")) {
            readTot(b, TotType.TIME, line.substring("TOT ".length()), log);
        } else {
            return false;
        }
        return true;
    }

    private static void readTot(CasBriefImport.Builder b, TotType type, String time, ImportLog log) {
        if (!log.claim(TOT)) {
            return;
        }
        if (type == TotType.TIME) {
            String v = log.entered(TOT, time);
            if (v == null) {
                return;
            }
            b.totTime = v;
        } else {
            log.hit();
        }
        b.tot = type;
    }
}
