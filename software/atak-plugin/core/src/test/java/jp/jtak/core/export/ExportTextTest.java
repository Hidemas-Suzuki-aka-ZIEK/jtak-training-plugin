package jp.jtak.core.export;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import jp.jtak.core.TrainingNotice;
import jp.jtak.core.cas.*;
import org.junit.Test;

/**
 * 詳細設計 05 の 2.4（1〜5）。
 * <p>
 * Detailed design 05 §2.4 (items 1–5).
 */
public class ExportTextTest {
    private static final String NOTICE = TrainingNotice.text();

    private List<String> results() {
        return Arrays.asList(
                CasBriefFormatter.format(new NineLine.Builder().build(),
                        new CasRemarks.Builder().build(), new CasRestrictions.Builder().build()).text(),
                RoutingFormatter.format(new Routing.Builder().build()).text(),
                CheckInFormatter.format(new CheckIn.Builder().build()).text(),
                GamePlanFormatter.format(new GamePlan.Builder().build()).text());
    }

    @Test public void allFourProductsHaveExactlyTwoNoticeLines() {
        for (String result : results()) {
            String[] lines = ExportText.fileContent(result).split("\n");
            assertEquals(NOTICE, lines[0]);
            assertEquals(NOTICE, lines[lines.length - 1]);
            assertEquals(2, Arrays.stream(lines).filter(NOTICE::equals).count());
        }
    }

    @Test public void allFourProductsKeepBodyCharactersAndOrder() {
        for (String result : results()) {
            List<String> original = Arrays.asList(result.split("\n"));
            List<String> expected = original.subList(1, original.size() - 1);
            assertEquals(expected, ExportText.bodyLines(result));
            assertEquals(NOTICE + "\n" + String.join("\n", expected) + "\n" + NOTICE + "\n",
                    ExportText.fileContent(result));
        }
    }

    @Test public void missingNoticeAndOneSidedNoticesAreAdded() {
        String body = "見出し\nline 1\n\nline 2";
        String expected = NOTICE + "\n" + body + "\n" + NOTICE + "\n";
        for (String input : Arrays.asList(body, NOTICE + "\n" + body,
                body + "\n" + NOTICE, NOTICE + "\n" + body + "\n" + NOTICE)) {
            assertEquals(expected, ExportText.fileContent(input));
        }
    }

    @Test public void crlfAndTrailingEmptyLinesAreNormalized() {
        assertEquals(NOTICE + "\n見出し\n本文\n" + NOTICE + "\n",
                ExportText.fileContent(NOTICE + "\r\n見出し\r\n本文\r\n" + NOTICE + "\r\n\r\n"));
        assertEquals(Arrays.asList("本文"), ExportText.bodyLines("本文\n\n"));
    }

    @Test public void emptyBodiesAreRejectedByBothMethods() {
        for (String input : Arrays.asList(null, "", "\n\n", NOTICE,
                NOTICE + "\n" + NOTICE, NOTICE + "\r\n\r\n")) {
            assertThrows(IllegalArgumentException.class, () -> ExportText.bodyLines(input));
            assertThrows(IllegalArgumentException.class, () -> ExportText.fileContent(input));
        }
    }
}
