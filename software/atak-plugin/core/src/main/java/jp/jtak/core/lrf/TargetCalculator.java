package jp.jtak.core.lrf;

/**
 * 観測者の位置と測距値から目標の位置を出す。
 * <p>
 * Calculates the target position from the observer position and a measurement.
 */
public final class TargetCalculator {
    private TargetCalculator() {}

    /**
     * 受け付ける距離の上限（m）。超えたら点を置かない。20,000 m ちょうどは置く。
     * <p>
     * Maximum accepted distance (m). Does not place a point above this limit. Places a point at exactly 20,000 m.
     */
    public static final double MAX_DISTANCE_METERS = 20000.0;

    /**
     * 目標の位置を計算する。
     *
     * <p>真方位 = 磁方位 + 偏差（東が正）。水平距離 = d·cos e、高低差 = d·sin e。
     * 地球の曲率と大気の屈折は入れない。高さの誤差のめやすは 水平距離² / (2R)
     * で、1 km で約 0.08 m、5 km で約 2 m、10 km で約 8 m、上限の 20 km で約 31 m。訓練用の精度として受け入れる。
     * <p>
     * Calculates the target position. <p> True bearing = magnetic bearing + declination (east positive).
     * Horizontal distance = d·cos e; height difference = d·sin e. Does not account for Earth's curvature or
     * atmospheric refraction. Estimated altitude error is horizontal distance² / (2R): about 0.08 m at 1 km, 2 m
     * at 5 km, 8 m at 10 km, and 31 m at the 20 km limit. This accuracy is accepted for training use.
     *
     * @param reading null のときは呼び出し側の誤りとして {@link IllegalArgumentException}（状態では返さない） null is a caller error and throws {@link IllegalArgumentException} (not returned as a status)
     * @param declinationDeg 磁方位のときの偏差（度）。真方位のときは見ない。 declination for magnetic bearings (degrees); ignored for true bearings
     */
    public static TargetSolution solve(GeoPosition observer, RangeReading reading, double declinationDeg) {
        if (reading == null) {
            throw new IllegalArgumentException("reading must not be null");
        }
        if (observer == null) {
            return TargetSolution.failure(TargetStatus.NO_OBSERVER);
        }
        double lat = observer.latitudeDeg();
        double lon = observer.longitudeDeg();
        if (!isFinite(lat) || !isFinite(lon) || Math.abs(lat) > 90.0 || Math.abs(lon) > 180.0) {
            return TargetSolution.failure(TargetStatus.INVALID_OBSERVER);
        }
        double d = reading.distanceMeters();
        if (!isFinite(d) || d < 0.0) {
            return TargetSolution.failure(TargetStatus.INVALID_DISTANCE);
        }
        if (d > MAX_DISTANCE_METERS) {
            return TargetSolution.failure(TargetStatus.DISTANCE_OUT_OF_RANGE);
        }
        double e = reading.elevationDeg();
        if (!isFinite(e) || Math.abs(e) > 90.0) {
            return TargetSolution.failure(TargetStatus.INVALID_ELEVATION);
        }
        double az = reading.azimuthDeg();
        if (!isFinite(az)) {
            return TargetSolution.failure(TargetStatus.AZIMUTH_UNAVAILABLE);
        }
        boolean magnetic = reading.azimuthReference() == AzimuthReference.MAGNETIC;
        if (magnetic && !isFinite(declinationDeg)) {
            return TargetSolution.failure(TargetStatus.INVALID_DECLINATION);
        }

        az = normalizeDegrees(az);
        double trueAz = magnetic ? az + declinationDeg : az;
        trueAz = ((trueAz % 360.0) + 360.0) % 360.0;
        double eRad = Math.toRadians(e);
        double horizontal = d * Math.cos(eRad);
        double height = d * Math.sin(eRad);

        double[] p = Geodesy.direct(lat, lon, trueAz, horizontal);
        double hae = observer.hasAltitude() ? observer.haeMeters() + height : Double.NaN;
        return new TargetSolution(TargetStatus.OK, new GeoPosition(p[0], p[1], hae), trueAz,
                magnetic ? declinationDeg : Double.NaN, horizontal, height);
    }

    /**
     * 角度を [0, 360) にする。有限でない値は呼び出し側で先に除く。
     * <p>
     * Normalizes an angle to [0, 360). The caller must exclude non-finite values first.
     */
    public static double normalizeDegrees(double deg) {
        double r = ((deg % 360.0) + 360.0) % 360.0;
        return r >= 360.0 ? 0.0 : r;
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }
}
