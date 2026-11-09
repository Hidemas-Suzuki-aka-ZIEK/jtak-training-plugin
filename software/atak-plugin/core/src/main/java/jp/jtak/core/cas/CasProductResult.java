package jp.jtak.core.cas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ルーティング・チェックイン・ゲームプランの文字の並びと、未入力だった項目の名前。不変。
 * <p>
 * Routing, check-in, or game plan text with the names of missing items. Immutable.
 */
public final class CasProductResult {
    private final String text;
    private final List<String> missingItems;

    CasProductResult(String text, List<String> missingItems) {
        this.text = text;
        this.missingItems = Collections.unmodifiableList(new ArrayList<>(missingItems));
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
     * 必須なのに空だった項目の名前（出す順。変えられない）。
     * <p>
     * Names of required items left empty (display order; unmodifiable).
     */
    public List<String> missingItems() {
        return missingItems;
    }
}
