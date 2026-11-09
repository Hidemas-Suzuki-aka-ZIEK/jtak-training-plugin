package jp.jtak.core.cas;

/**
 * チェックインの出し方（出典 JP 3-09.3 2014 V-15。決定は 03-cas-other-products.md 2.3 の 4）。
 * <p>
 * Check-in output mode (source: JP 3-09.3 2014, V-15; decision: 03-cas-other-products.md §2.3, item 4).
 */
public enum CheckInMode {
    /**
     * 1〜7 をすべて出す。空の項目は未入力にする。
     * <p>
     * Outputs all items 1–7. Marks empty items as missing.
     */
    FULL,
    /**
     * {@code As fragged.} のあと、入れた項目だけを出す。
     * <p>
     * Outputs only entered items after {@code As fragged.}.
     */
    AS_FRAGGED,
    /**
     * {@code As fragged, with exception:} のあと、入れた項目だけを出す。
     * <p>
     * Outputs only entered items after {@code As fragged, with exception:}.
     */
    WITH_EXCEPTION
}
