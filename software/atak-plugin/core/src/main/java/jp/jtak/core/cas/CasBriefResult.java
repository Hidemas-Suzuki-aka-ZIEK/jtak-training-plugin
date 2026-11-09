package jp.jtak.core.cas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 9-line と Remarks の文字の並びと、未入力だった行の番号。不変。
 * <p>
 * 9-line and Remarks text with the numbers of missing lines. Immutable.
 */
public final class CasBriefResult {
    private final String text;
    private final List<Integer> missingLines;
    private final List<String> missingRestrictions;

    CasBriefResult(String text, List<Integer> missingLines, List<String> missingRestrictions) {
        this.text = text;
        this.missingLines = Collections.unmodifiableList(new ArrayList<>(missingLines));
        this.missingRestrictions = Collections.unmodifiableList(new ArrayList<>(missingRestrictions));
    }

    /**
     * 先頭と末尾が {@code TrainingNotice.text()} の文字の並び（改行は {@code \n}）。
     * <p>
     * Text beginning and ending with {@code TrainingNotice.text()} (line separator: {@code \n}).
     */
    public String text() {
        return text;
    }

    /**
     * 未入力の行の番号（昇順。変えられない）。
     * <p>
     * Numbers of missing lines (ascending order; unmodifiable).
     */
    public List<Integer> missingLines() {
        return missingLines;
    }

    /**
     * 未入力だった Restrictions の項目の名前（出す順。変えられない。2 つの引数の format では空）。
     * <p>
     * Names of missing Restrictions items (display order; unmodifiable; empty in the two-argument format).
     */
    public List<String> missingRestrictions() {
        return missingRestrictions;
    }
}
