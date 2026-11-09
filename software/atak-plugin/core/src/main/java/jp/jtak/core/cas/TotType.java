package jp.jtak.core.cas;

/**
 * Restrictions の TOT/TTT の種類（出典 JP 3-09.3 2014 V-32）。
 * <p>
 * Restrictions TOT/TTT types (source: JP 3-09.3 2014, V-32).
 */
public enum TotType {
    /**
     * 時刻を伝える（{@code TOT {時刻}}）。
     * <p>
     * Specifies a time ({@code TOT {time}}).
     */
    TIME,
    /**
     * TOT の指定を待つ文（{@code Standby TOT}）。
     * <p>
     * {@code Standby TOT}
     */
    STANDBY,
    /**
     * correlation のあとで TOT を伝える文（{@code TOT will be issued after correlation}）。
     * <p>
     * {@code TOT will be issued after correlation}
     */
    AFTER_CORRELATION,
    /**
     * 準備ができたら進むよう求める文（{@code Push when ready}）。
     * <p>
     * {@code Push when ready}
     */
    PUSH_WHEN_READY
}
