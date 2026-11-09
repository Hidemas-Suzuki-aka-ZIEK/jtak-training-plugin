package jp.jtak.plugin.cas;

import android.hardware.GeomagneticField;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.maps.conversion.EGM96;
import com.atakmap.coremap.maps.coords.GeoPoint;
import jp.jtak.core.cas.MgrsText;
import jp.jtak.core.cas.NineLineCalculator;
import jp.jtak.core.lrf.GeoPosition;

/**
 * 地図の点から 9-line の値を読む（02 3.3）。方位・距離・単位・方角の計算は core の
 * {@link NineLineCalculator} に任せ、ここでは ATAK の API から値を取り出すだけにする。
 * 取れない値は null を返す（画面では空のままにする）。
 * <p>
 * Reads 9-line values from map points (02 §3.3). Delegates bearing, distance, unit, and direction calculations to
 * core's {@link NineLineCalculator}; this class only extracts values from ATAK APIs. Returns null for unavailable
 * values (the screen leaves them empty).
 */
final class CasValueReader {
    private CasValueReader() {}

    /**
     * 高さは有効で、基準が HAE のときだけ使う（無効・AGL なら NaN。AGL を HAE の欄に入れない）。
     * <p>
     * Uses altitude only when valid and referenced to HAE (NaN if invalid or AGL; never puts AGL in an HAE field).
     */
    static GeoPosition position(GeoPoint p) {
        boolean usable = p.isAltitudeValid()
                && p.getAltitudeReference() == GeoPoint.AltitudeReference.HAE;
        double hae = usable ? p.getAltitude() : Double.NaN;
        return new GeoPosition(p.getLatitude(), p.getLongitude(), hae);
    }

    /**
     * 4 行目。EGM96 の MSL（m）を feet にする。HAE の点だけ使い、取れなければ null。
     * <p>
     * Line 4. Converts EGM96 MSL (m) to feet. Uses only HAE points and returns null if unavailable.
     */
    static String elevationText(GeoPoint p) {
        if (p.getAltitudeReference() != GeoPoint.AltitudeReference.HAE) {
            return null;
        }
        double msl = EGM96.getMSL(p);
        return Double.isNaN(msl) ? null : NineLineCalculator.elevationFeet(msl);
    }

    /**
     * 6 行目。MGRS の文字列。ATAK が入れる見えない文字（U+200E・U+200F）と前後の空白を取る（02 5.1）。
     * 取れなければ null。
     * <p>
     * Line 6. MGRS string. Removes invisible characters inserted by ATAK (U+200E and U+200F) and leading/trailing
     * whitespace (02 §5.1). Returns null if unavailable.
     */
    static String mgrs(GeoPoint p) {
        try {
            return MgrsText.clean(
                    CoordinateFormatUtilities.formatToString(p, CoordinateFormat.MGRS));
        } catch (IllegalArgumentException e) {
            // formatToString が IllegalArgumentException を投げた点は、取れないものとして null を返す。
            // Return null as unavailable for points where formatToString throws IllegalArgumentException.
            return null;
        }
    }

    /**
     * 2 行目の磁気偏角（度。東が正）。高さが分からなければ 0 m とする。
     * <p>
     * Magnetic declination for line 2 (degrees; east positive). Uses 0 m if altitude is unknown.
     */
    static double declination(GeoPoint p) {
        float alt = p.isAltitudeValid() ? (float) p.getAltitude() : 0f;
        return new GeomagneticField((float) p.getLatitude(), (float) p.getLongitude(), alt,
                System.currentTimeMillis()).getDeclination();
    }
}
