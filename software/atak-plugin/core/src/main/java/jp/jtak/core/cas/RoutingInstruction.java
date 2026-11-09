package jp.jtak.core.cas;

import java.util.Locale;

/**
 * ルーティングの指示の種類（出典 JP 3-09.3 2014 図 V-3）。{@code %s} に場所が入る。
 * <p>
 * Routing instruction types (source: JP 3-09.3 2014, Figure V-3). The location replaces {@code %s}.
 */
public enum RoutingInstruction {
    PROCEED_REPORT_ESTABLISHED("Proceed %s and report established."),
    MAINTAIN("Maintain %s."),
    REPORT_PASSING("Report passing %s.");

    private final String pattern;

    RoutingInstruction(String pattern) {
        this.pattern = pattern;
    }

    String sentence(String place) {
        return String.format(Locale.ROOT, pattern, place);
    }
}
