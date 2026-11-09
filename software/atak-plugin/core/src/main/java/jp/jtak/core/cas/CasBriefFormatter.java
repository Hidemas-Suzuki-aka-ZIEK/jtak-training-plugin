package jp.jtak.core.cas;

import java.util.ArrayList;
import java.util.List;
import jp.jtak.core.TrainingNotice;

/**
 * 9-line と Remarks を、読み上げ用の文字の並びにする（docs/detailed-design/02-cas-brief.md 2.5）。
 * <p>
 * Formats the 9-line and Remarks as read-aloud text (docs/detailed-design/02-cas-brief.md §2.5).
 */
public final class CasBriefFormatter {
    static final String MISSING = CasText.MISSING;
    private static final String TITLE = "CAS 9-LINE (JP 3-09.3 2014 Fig. V-9) / Datum: WGS 84";

    private CasBriefFormatter() {}

    public static CasBriefResult format(NineLine lines, CasRemarks remarks) {
        return format(lines, remarks, new CasRestrictions.Builder().build());
    }

    public static CasBriefResult format(
            NineLine lines, CasRemarks remarks, CasRestrictions restrictions) {
        if (lines == null || remarks == null || restrictions == null) {
            throw new IllegalArgumentException("lines・remarks・restrictions は必須");
        }
        String[] v = new String[10];
        for (int i = 1; i <= 9; i++) {
            v[i] = clean(lines.line(i));
        }
        String offset = clean(lines.offset());
        boolean omitHeader = v[1] == null && v[2] == null && v[3] == null;

        List<Integer> missing = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            if (v[i] == null && !(omitHeader && i <= 3)) {
                missing.add(i);
            }
        }

        List<String> out = new ArrayList<>();
        out.add(TrainingNotice.text());
        out.add(TITLE);
        if (!omitHeader) {
            out.add("[1-3]");
            out.add("1 IP/BP: " + orMissing(v[1]));
            String heading = orMissing(v[2]);
            if (v[2] != null && offset != null) {
                heading = heading + ", Offset " + offset;
            }
            out.add("2 Heading: " + heading);
            out.add("3 Distance: " + orMissing(v[3]));
        }
        out.add("[4-6]");
        String elevation = orMissing(v[4]);
        if (omitHeader && v[4] != null) {
            elevation = "Elevation " + elevation;
        }
        out.add("4 Target elevation: " + elevation + " (readback)");
        out.add("5 Target description: " + orMissing(v[5]));
        out.add("6 Target location: " + orMissing(v[6]) + " (readback)");
        out.add("[7-9]");
        out.add("7 Type mark / Terminal guidance: " + orMissing(v[7]));
        out.add("8 Location of friendlies: " + orMissing(v[8]));
        String egress = orMissing(v[9]);
        if (v[9] != null && !v[9].toLowerCase(java.util.Locale.ROOT).startsWith("egress")) {
            // clean() が全角空白も前後から取っているので、ここで先頭に空白は残らない。
            // clean() also trims full-width spaces, so no leading whitespace remains here.
            egress = "Egress " + egress;
        }
        out.add("9 Egress: " + egress);

        String[][] remarkItems = {
            {"LTL/PTL", clean(remarks.ltlPtl())},
            {"Ordnance / Effects", clean(remarks.ordnance())},
            {"Threat / SEAD", clean(remarks.threat())},
            {"Additional remarks", clean(remarks.additionalRemarks())},
            {"Additional calls", clean(remarks.additionalCalls())},
        };
        boolean header = false;
        for (String[] item : remarkItems) {
            if (item[1] == null) {
                continue;
            }
            if (!header) {
                out.add("[Remarks]");
                header = true;
            }
            out.add(item[0] + ": " + item[1]);
        }
        List<String> missingRestrictions = new ArrayList<>();
        out.addAll(restrictionLines(restrictions, missingRestrictions));
        out.add(TrainingNotice.text());
        return new CasBriefResult(String.join("\n", out), missing, missingRestrictions);
    }

    /**
     * {@code [Restrictions]} の節。どれも入れていなければ空のリスト。
     * <p>
     * The {@code [Restrictions]} section. An empty list if no items were entered.
     */
    private static List<String> restrictionLines(CasRestrictions r, List<String> missing) {
        List<String> lines = new ArrayList<>();
        if (r.dangerClose()) {
            String initials = clean(r.dangerCloseInitials());
            if (initials == null) {
                missing.add("Danger close initials");
            }
            lines.add("Danger close, initials " + orMissing(initials) + " (readback)");
        }
        addRestriction(lines, "ACAs: ", r.acas());
        addRestriction(lines, "FAHs: ", r.fahs());
        addRestriction(lines, "Separation: ", r.separation());
        addRestriction(lines, "RW approval out of BP: ", r.rwOutOfBp());
        addRestriction(lines, "PLA: ", r.pla());
        TotType tot = r.tot();
        if (tot == TotType.TIME) {
            String time = clean(r.totTime());
            if (time == null) {
                missing.add("TOT");
            }
            lines.add("TOT " + orMissing(time) + " (readback)");
        } else if (tot == TotType.STANDBY) {
            lines.add("Standby TOT (readback)");
        } else if (tot == TotType.AFTER_CORRELATION) {
            lines.add("TOT will be issued after correlation (readback)");
        } else if (tot == TotType.PUSH_WHEN_READY) {
            lines.add("Push when ready (readback)");
        }
        if (!lines.isEmpty()) {
            lines.add(0, "[Restrictions]");
        }
        return lines;
    }

    private static void addRestriction(List<String> lines, String label, String value) {
        String v = clean(value);
        if (v != null) {
            lines.add(label + v + " (readback)");
        }
    }

    private static String orMissing(String v) {
        return CasText.orMissing(v);
    }

    private static String clean(String s) {
        return CasText.clean(s);
    }
}
