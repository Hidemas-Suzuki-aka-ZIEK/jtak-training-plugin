package jp.jtak.core.cas;

import java.util.List;
import java.util.Locale;
import jp.jtak.core.lrf.GeoPosition;
import jp.jtak.core.lrf.Geodesy;

/**
 * 9-line の値のうち、地図の点から計算できるもの（1〜4・8 行目の元になる値）。
 * 計算できないとき（入力が不正・同じ点など）は null を返し、呼び出し側は未入力として扱う。
 * <p>
 * 9-line values calculable from map points (source values for lines 1–4 and 8). Returns null when calculation is
 * impossible (invalid input, coincident points, etc.); callers treat this as missing input.
 */
public final class NineLineCalculator {
    private static final double METERS_PER_NM = 1852.0;
    private static final double METERS_PER_FOOT = 0.3048;

    private NineLineCalculator() {}

    /**
     * 真方位と磁気偏角から、2 行目の磁方位（3 桁。0 と 360 は "360"）にする。
     * 磁方位 = 真方位 − 偏角（偏角は東が正）。NaN・無限大は null。
     * <p>
     * Converts true bearing and magnetic declination to the magnetic bearing for line 2 (three digits; 0 and 360
     * become "360"). Magnetic bearing = true bearing − declination (east positive). Returns null for NaN or
     * infinity.
     */
    public static String headingFromBearing(double trueBearingDeg, double declinationDeg) {
        if (!isFinite(trueBearingDeg) || !isFinite(declinationDeg)) {
            return null;
        }
        double magnetic = ((trueBearingDeg - declinationDeg) % 360.0 + 360.0) % 360.0;
        long rounded = Math.round(magnetic);
        if (rounded == 0L || rounded >= 360L) {
            return "360";
        }
        return String.format(Locale.ROOT, "%03d", rounded);
    }

    /**
     * IP/BP から目標へ向かう磁方位（2 行目）。同じ点・不正な点は null。
     * <p>
     * Magnetic bearing from IP/BP to target (line 2). Null for coincident or invalid points.
     */
    public static String heading(GeoPosition from, GeoPosition to, double declinationDeg) {
        double[] r = inverse(from, to);
        if (r == null) {
            return null;
        }
        return headingFromBearing(r[1], declinationDeg);
    }

    /**
     * 距離（m）を 3 行目の文字にする。IP は NM を小数 1 桁、BP は 100 m 単位の meters。
     * <p>
     * Formats distance (m) for line 3. IP uses NM with one decimal place; BP uses meters in 100 m increments.
     */
    public static String distanceText(double meters, AttackPointType type) {
        if (type == null || !isFinite(meters) || meters < 0.0) {
            return null;
        }
        if (type == AttackPointType.IP) {
            double nm = Math.round(meters / METERS_PER_NM * 10.0) / 10.0;
            return String.format(Locale.ROOT, "%.1f NM", nm);
        }
        long hundreds = Math.round(meters / 100.0) * 100L;
        return hundreds + " meters";
    }

    /**
     * IP/BP から目標までの距離（3 行目）。
     * <p>
     * Distance from IP/BP to target (line 3).
     */
    public static String distance(GeoPosition from, GeoPosition to, AttackPointType type) {
        double[] r = inverse(from, to);
        if (r == null) {
            return null;
        }
        return distanceText(r[0], type);
    }

    /**
     * 海抜（m）を 4 行目の feet にする。NaN・無限大は null。
     * <p>
     * Converts elevation above sea level (m) to feet for line 4. Returns null for NaN or infinity.
     */
    public static String elevationFeet(double meters) {
        if (!isFinite(meters)) {
            return null;
        }
        return Math.round(meters / METERS_PER_FOOT) + " feet";
    }

    /**
     * 8 行目: 目標から最も近い味方の「方角 距離（m）」（例 {@code South 300}）。
     * 不正な点と null は飛ばす。使える味方がいない場合、または 1 つでも目標と同じ点
     * （方角が決まらない点）があれば null（いちばん近い味方が黙って消えないようにするため）。
     * <p>
     * Line 8: “direction distance (m)” of the friendly point nearest the target (for example, {@code South 300}).
     * Skips invalid and null points. Returns null if no usable friendly point exists or if any point coincides
     * with the target (direction undefined), to avoid silently omitting the nearest friendly point.
     */
    public static String friendlies(GeoPosition target, List<GeoPosition> friendlies) {
        if (!valid(target) || friendlies == null) {
            return null;
        }
        double bestDistance = Double.POSITIVE_INFINITY;
        double bestBearing = Double.NaN;
        for (GeoPosition f : friendlies) {
            if (!valid(f)) {
                continue;
            }
            double[] r = inverse(target, f);
            if (r == null) {
                // 目標と同じ点・収束しない点がある。ほかの味方を出すと近い味方が消えるので null。
                // A coincident or nonconverging point exists. Return null because showing another friendly point would omit the nearby one.
                return null;
            }
            if (r[0] < bestDistance) {
                bestDistance = r[0];
                bestBearing = r[1];
            }
        }
        if (Double.isInfinite(bestDistance)) {
            return null;
        }
        Cardinal c = Cardinal.fromTrueBearing(bestBearing);
        if (c == null) {
            return null;
        }
        return c.word() + " " + Math.round(bestDistance);
    }

    /**
     * {距離 m, 前方位 度}。不正な点・同じ点・収束しないときは null。
     * <p>
     * {distance in m, forward azimuth in degrees}. Null for invalid or coincident points or failure to converge.
     */
    private static double[] inverse(GeoPosition from, GeoPosition to) {
        if (!valid(from) || !valid(to)) {
            return null;
        }
        double[] r;
        try {
            r = Geodesy.inverse(from.latitudeDeg(), from.longitudeDeg(), to.latitudeDeg(), to.longitudeDeg());
        } catch (IllegalStateException e) {
            return null;
        }
        if (Double.isNaN(r[1])) {
            return null;
        }
        return r;
    }

    private static boolean valid(GeoPosition p) {
        return p != null
                && isFinite(p.latitudeDeg()) && Math.abs(p.latitudeDeg()) <= 90.0
                && isFinite(p.longitudeDeg()) && Math.abs(p.longitudeDeg()) <= 180.0;
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }
}
