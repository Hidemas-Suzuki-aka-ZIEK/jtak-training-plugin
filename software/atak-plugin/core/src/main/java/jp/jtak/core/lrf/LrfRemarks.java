package jp.jtak.core.lrf;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import jp.jtak.core.Lang;
import jp.jtak.core.TrainingNotice;

/**
 * マーカーの説明（remarks）の文。先頭と末尾に訓練用の表示を入れる（U1）。
 * <p>
 * Marker description (remarks). Includes the training notice at the beginning and end (U1).
 */
public final class LrfRemarks {
    private LrfRemarks() {}

    /**
     * 表示用に方位を小数 1 桁に丸めてから [0, 360) に直す（359.96 → 0.0。360.0 とは書かない）。
     * <p>
     * Rounds the bearing to one decimal place for display, then normalizes to [0, 360) (359.96 → 0.0, never
     * 360.0).
     */
    private static double displayAzimuth(double deg) {
        return TargetCalculator.normalizeDegrees(Math.round(deg * 10.0) / 10.0);
    }

    public static String format(RangeReading reading, TargetSolution solution, Lang lang) {
        Objects.requireNonNull(lang, "lang");
        StringBuilder sb = new StringBuilder();
        sb.append(TrainingNotice.text()).append('\n');
        sb.append((lang == Lang.JA ? "JTAK LRF 測距点" : "JTAK LRF range point")).append('\n');
        // 受け取った機器の名前（deviceName）は出さない（docs/usage-scope.md Q7）。
        // Do not display the received device name (deviceName; docs/usage-scope.md Q7).
        sb.append((lang == Lang.JA ? "入力: " : "Input: ")).append(reading.source().label(lang)).append('\n');
        sb.append(String.format(Locale.ROOT, (lang == Lang.JA ? "距離: %.1f m" : "Distance: %.1f m"), reading.distanceMeters())).append('\n');
        if (reading.azimuthReference() == AzimuthReference.MAGNETIC) {
            sb.append(String.format(Locale.ROOT, (lang == Lang.JA ? "方位: 磁 %05.1f° / 偏差 %+.1f° / 真 %05.1f°" : "Azimuth: magnetic %05.1f° / declination %+.1f° / true %05.1f°"),
                    displayAzimuth(reading.azimuthDeg()), solution.declinationDeg(), displayAzimuth(solution.trueAzimuthDeg())));
        } else {
            sb.append(String.format(Locale.ROOT, (lang == Lang.JA ? "方位: 真 %05.1f°" : "Azimuth: true %05.1f°"), displayAzimuth(solution.trueAzimuthDeg())));
        }
        sb.append('\n');
        sb.append(String.format(Locale.ROOT, (lang == Lang.JA ? "俯仰: %+.1f°" : "Elevation angle: %+.1f°"), reading.elevationDeg())).append('\n');
        sb.append(String.format(Locale.ROOT, (lang == Lang.JA ? "水平距離: %.1f m / 高低差: %+.1f m" : "Horizontal distance: %.1f m / Height difference: %+.1f m"),
                solution.horizontalDistanceMeters(), solution.heightDifferenceMeters())).append('\n');
        GeoPosition t = solution.target();
        String hae = t.hasAltitude() ? String.format(Locale.ROOT, "%.1f m", t.haeMeters()) : (lang == Lang.JA ? "不明" : "unknown");
        sb.append(String.format(Locale.ROOT, (lang == Lang.JA ? "目標: %.6f, %.6f / 標高(HAE): %s" : "Target: %.6f, %.6f / Altitude (HAE): %s"),
                t.latitudeDeg(), t.longitudeDeg(), hae)).append('\n');
        sb.append((lang == Lang.JA ? "時刻: " : "Time: ")).append(Instant.ofEpochMilli(reading.epochMillis())).append('\n');
        sb.append(TrainingNotice.text());
        return sb.toString();
    }
}
