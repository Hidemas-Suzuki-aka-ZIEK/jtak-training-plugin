package jp.jtak.core.cas;

import java.util.ArrayList;
import java.util.List;
import jp.jtak.core.TrainingNotice;

/**
 * ゲームプランを読み上げ用の文字の並びにする（docs/detailed-design/03-cas-other-products.md 2.2.3）。
 * <p>
 * Formats a game plan as read-aloud text (docs/detailed-design/03-cas-other-products.md §2.2.3).
 */
public final class GamePlanFormatter {
    private static final String TITLE = "GAME PLAN (JP 3-09.3 2014 Fig. V-9)";

    private GamePlanFormatter() {}

    public static CasProductResult format(GamePlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan は必須");
        }
        String callsign = CasText.clean(plan.callsign());
        List<String> missing = new ArrayList<>();
        if (plan.typeOfControl() == null) {
            missing.add("Type of control");
        }
        if (plan.methodOfAttack() == null) {
            missing.add("Method of attack");
        }

        StringBuilder body = new StringBuilder();
        body.append(plan.typeOfControl() == null ? CasText.MISSING : plan.typeOfControl().phrase());
        body.append(", ");
        body.append(plan.methodOfAttack() == null ? CasText.MISSING : plan.methodOfAttack().phrase());
        appendExtra(body, "intent ", plan.intent(), "");
        appendExtra(body, "CDE concerns ", plan.collateralConcerns(), "");
        appendExtra(body, "ordnance ", plan.ordnance(), "");
        appendExtra(body, "effects ", plan.desiredEffects(), "");
        appendExtra(body, "fuzing ", plan.fuzing(), "");
        appendExtra(body, "interval ", plan.interval(), "");
        appendExtra(body, "line 6 ", plan.line6Digits(), " digits");
        body.append("; advise when ready for 9-line.");

        List<String> out = new ArrayList<>();
        out.add(TrainingNotice.text());
        out.add(TITLE);
        out.add(callsign == null
                ? "Advise when ready for game plan."
                : callsign + ", advise when ready for game plan.");
        out.add(CasText.withCallsign(callsign, body.toString()));
        out.add(TrainingNotice.text());
        return new CasProductResult(String.join("\n", out), missing);
    }

    private static void appendExtra(StringBuilder sb, String label, String value, String suffix) {
        String v = CasText.clean(value);
        if (v != null) {
            sb.append(", ").append(label).append(v).append(suffix);
        }
    }
}
