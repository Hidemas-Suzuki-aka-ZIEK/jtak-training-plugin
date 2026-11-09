package jp.jtak.core.cas;

import java.util.ArrayList;
import java.util.List;
import jp.jtak.core.TrainingNotice;

/**
 * ルーティングを読み上げ用の文字の並びにする（docs/detailed-design/03-cas-other-products.md 2.2.1）。
 * <p>
 * Formats routing as read-aloud text (docs/detailed-design/03-cas-other-products.md §2.2.1).
 */
public final class RoutingFormatter {
    private static final String TITLE = "ROUTING (JP 3-09.3 2014 Fig. V-3)";

    private RoutingFormatter() {}

    public static CasProductResult format(Routing routing) {
        if (routing == null) {
            throw new IllegalArgumentException("routing は必須");
        }
        String callsign = CasText.clean(routing.callsign());
        String place = CasText.clean(routing.place());
        List<String> missing = new ArrayList<>();
        List<String> out = new ArrayList<>();
        out.add(TrainingNotice.text());
        out.add(TITLE);

        if (routing.instruction() == null || place == null) {
            missing.add("Routing/Holding");
            out.add(CasText.withCallsign(callsign, CasText.MISSING));
        } else {
            String sentence = routing.instruction().sentence(place);
            out.add(callsign == null ? sentence : callsign + ", " + CasText.lowerFirst(sentence));
        }
        addLine(out, "Other instructions: ", routing.otherInstructions());
        if (routing.onlyAircraftOnStation()) {
            out.add("You are the only aircraft on station.");
        }
        addLine(out, "Other AOS: ", routing.otherAos());
        addLine(out, "Immediate threats: ", routing.threats());
        addLine(out, "Significant weather/terrain: ", routing.weatherTerrain());
        addLine(out, "Status calls: ", routing.statusCalls());
        RoutingClosing closing = routing.closing();
        if (closing == RoutingClosing.SEND) {
            out.add("Send your check-in.");
        } else if (closing == RoutingClosing.HOLD) {
            String reason = CasText.clean(routing.holdReason());
            out.add(reason == null ? "Hold your check-in." : "Hold your check-in, " + reason + ".");
        }
        out.add(TrainingNotice.text());
        return new CasProductResult(String.join("\n", out), missing);
    }

    private static void addLine(List<String> out, String label, String value) {
        String v = CasText.clean(value);
        if (v != null) {
            out.add(label + v);
        }
    }
}
