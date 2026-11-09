package jp.jtak.core.cas;

/**
 * ルーティングの結び（図 V-3。V-15–V-16）。
 * <p>
 * Routing closing (Figure V-3; V-15–V-16).
 */
public enum RoutingClosing {
    /**
     * 結びの行を出さない。
     * <p>
     * Omits the closing line.
     */
    NONE,
    /**
     * チェックインを送るよう求める文（{@code Send your check-in.}）。
     * <p>
     * {@code Send your check-in.}
     */
    SEND,
    /**
     * {@code Hold your check-in.}（理由があれば {@code Hold your check-in, {理由}.}）
     * <p>
     * {@code Hold your check-in.} (with a reason: {@code Hold your check-in, {reason}.}).
     */
    HOLD
}
