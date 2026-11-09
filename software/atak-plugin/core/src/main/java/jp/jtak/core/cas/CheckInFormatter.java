package jp.jtak.core.cas;

import java.util.ArrayList;
import java.util.List;
import jp.jtak.core.TrainingNotice;

/**
 * チェックインを記録用の文字の並びにする（docs/detailed-design/03-cas-other-products.md 2.2.2）。
 * <p>
 * Formats a check-in as text for recording (docs/detailed-design/03-cas-other-products.md §2.2.2).
 */
public final class CheckInFormatter {
    private static final String TITLE = "CHECK-IN (JP 3-09.3 2014 Fig. V-4)";
    private static final String ABORT_NONE = "none (standard ABORT, ABORT, ABORT)";

    private CheckInFormatter() {}

    public static CasProductResult format(CheckIn checkIn) {
        if (checkIn == null) {
            throw new IllegalArgumentException("checkIn は必須");
        }
        List<String> missing = new ArrayList<>();
        List<String> out = new ArrayList<>();
        out.add(TrainingNotice.text());
        out.add(TITLE);

        String jtac = CasText.clean(checkIn.jtacCallsign());
        String aircraftCallsign = CasText.clean(checkIn.aircraftCallsign());
        if (jtac == null) {
            missing.add("JTAC callsign");
        }
        if (aircraftCallsign == null) {
            missing.add("Aircraft callsign");
        }
        out.add(CasText.orMissing(jtac) + ", this is " + CasText.orMissing(aircraftCallsign));

        CheckInMode mode = checkIn.mode();
        if (mode == CheckInMode.AS_FRAGGED) {
            out.add("As fragged.");
        } else if (mode == CheckInMode.WITH_EXCEPTION) {
            out.add("As fragged, with exception:");
        }
        boolean full = mode == CheckInMode.FULL;

        String abort;
        if (checkIn.abortCodeNone()) {
            abort = ABORT_NONE;
        } else {
            String code = CasText.clean(checkIn.abortCode());
            abort = code == null ? null : code + " (readback)";
        }
        String[][] items = {
            {"1 Mission number", CasText.clean(checkIn.missionNumber())},
            {"2 Number and type of aircraft", CasText.clean(checkIn.aircraft())},
            {"3 Position and altitude", CasText.clean(checkIn.positionAltitude())},
            {"4 Ordnance", CasText.clean(checkIn.ordnance())},
            {"5 Playtime", CasText.clean(checkIn.playtime())},
            {"6 Capabilities", CasText.clean(checkIn.capabilities())},
            {"7 Abort code", abort},
        };
        for (String[] item : items) {
            if (item[1] != null) {
                out.add(item[0] + ": " + item[1]);
            } else if (full) {
                missing.add(item[0]);
                out.add(item[0] + ": " + CasText.MISSING);
            }
        }
        out.add(TrainingNotice.text());
        return new CasProductResult(String.join("\n", out), missing);
    }
}
