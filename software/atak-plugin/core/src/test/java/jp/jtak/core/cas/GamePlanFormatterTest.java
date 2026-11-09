package jp.jtak.core.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import jp.jtak.core.TrainingNotice;
import org.junit.Test;

/**
 * docs/detailed-design/03-cas-other-products.md 2.4 の D12〜D14・D20・D21（ゲームプラン）。
 * <p>
 * docs/detailed-design/03-cas-other-products.md §2.4: D12–D14, D20, and D21 (game plan).
 */
@SuppressWarnings("PMD.MethodNamingConventions")
public class GamePlanFormatterTest {
    private static final String T = TrainingNotice.text();
    private static final String TITLE = "GAME PLAN (JP 3-09.3 2014 Fig. V-9)";

    @Test
    public void d12_type2BocNoExtras() {
        GamePlan g = new GamePlan.Builder().callsign("Razor 53")
                .typeOfControl(TypeOfControl.TYPE_2).methodOfAttack(MethodOfAttack.BOC).build();
        CasProductResult res = GamePlanFormatter.format(g);
        assertEquals(String.join("\n", T, TITLE,
                "Razor 53, advise when ready for game plan.",
                "Razor 53, Type 2, bomb on coordinate; advise when ready for 9-line.",
                T), res.text());
        assertTrue(res.missingItems().isEmpty());
    }

    @Test
    public void d13_extrasInF02Order() {
        GamePlan g = new GamePlan.Builder().callsign("Razor 53")
                .typeOfControl(TypeOfControl.TYPE_1).methodOfAttack(MethodOfAttack.BOT)
                .fuzing("instantaneous").intent("destroy the bridge").build();
        String text = GamePlanFormatter.format(g).text();
        assertTrue(text, text.contains("\nRazor 53, Type 1, bomb on target, "
                + "intent destroy the bridge, fuzing instantaneous; advise when ready for 9-line.\n"));
    }

    @Test
    public void d13b_allExtras() {
        GamePlan g = new GamePlan.Builder().callsign("Razor 53")
                .typeOfControl(TypeOfControl.TYPE_3).methodOfAttack(MethodOfAttack.BOT)
                .line6Digits("8").interval("5 seconds").fuzing("delay").desiredEffects("destroy")
                .ordnance("2x BOMB-A").collateralConcerns("school north").intent("deny crossing").build();
        String text = GamePlanFormatter.format(g).text();
        assertTrue(text, text.contains("\nRazor 53, Type 3, bomb on target, intent deny crossing, "
                + "CDE concerns school north, ordnance 2x BOMB-A, effects destroy, fuzing delay, "
                + "interval 5 seconds, line 6 8 digits; advise when ready for 9-line.\n"));
    }

    @Test
    public void d14_missingTypeAndMethodNoCallsign() {
        CasProductResult res = GamePlanFormatter.format(new GamePlan.Builder().build());
        assertEquals(String.join("\n", T, TITLE,
                "Advise when ready for game plan.",
                "（未入力）, （未入力）; advise when ready for 9-line.",
                T), res.text());
        assertEquals(Arrays.asList("Type of control", "Method of attack"), res.missingItems());
    }

    @Test
    public void d14b_noCallsignWithValues() {
        GamePlan g = new GamePlan.Builder().typeOfControl(TypeOfControl.TYPE_2)
                .methodOfAttack(MethodOfAttack.BOC).build();
        String text = GamePlanFormatter.format(g).text();
        assertTrue(text, text.contains("\nAdvise when ready for game plan.\n"));
        assertTrue(text, text.contains("\nType 2, bomb on coordinate; advise when ready for 9-line.\n"));
    }

    @Test
    public void d14c_onlyMethodMissing() {
        GamePlan g = new GamePlan.Builder().callsign("Razor 53").typeOfControl(TypeOfControl.TYPE_1).build();
        CasProductResult res = GamePlanFormatter.format(g);
        assertTrue(res.text(), res.text().contains("\nRazor 53, Type 1, （未入力）; advise when ready for 9-line.\n"));
        assertEquals(Collections.singletonList("Method of attack"), res.missingItems());
    }

    @Test
    public void d20_nullArgument() {
        try {
            GamePlanFormatter.format(null);
            fail("IllegalArgumentException のはず");
        } catch (IllegalArgumentException expected) {
            // 期待どおり
            // As expected.
        }
    }

    @Test
    public void d21_valuesAreCleaned() {
        GamePlan g = new GamePlan.Builder().callsign(" 　Razor 53")
                .typeOfControl(TypeOfControl.TYPE_1).methodOfAttack(MethodOfAttack.BOT)
                .intent("destroy\n the bridge　").ordnance("　").build();
        String text = GamePlanFormatter.format(g).text();
        assertTrue(text, text.contains("\nRazor 53, Type 1, bomb on target, intent destroy the bridge; "
                + "advise when ready for 9-line.\n"));
    }

    @Test
    public void startsAndEndsWithTrainingNotice() {
        String text = GamePlanFormatter.format(new GamePlan.Builder().build()).text();
        assertTrue(text.startsWith(T + "\n"));
        assertTrue(text.endsWith("\n" + T));
    }
}
