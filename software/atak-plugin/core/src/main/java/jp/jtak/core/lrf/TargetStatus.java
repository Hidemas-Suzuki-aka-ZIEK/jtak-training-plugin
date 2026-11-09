package jp.jtak.core.lrf;

/**
 * 目標の計算の結果の状態。
 * <p>
 * Target calculation status.
 */
public enum TargetStatus {
    OK,
    NO_OBSERVER,
    INVALID_OBSERVER,
    INVALID_DISTANCE,
    DISTANCE_OUT_OF_RANGE,
    INVALID_ELEVATION,
    AZIMUTH_UNAVAILABLE,
    INVALID_DECLINATION
}
