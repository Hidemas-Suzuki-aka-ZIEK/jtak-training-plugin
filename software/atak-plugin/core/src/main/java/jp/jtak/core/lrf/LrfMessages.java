package jp.jtak.core.lrf;

import java.util.Locale;
import java.util.Objects;
import jp.jtak.core.Lang;
import jp.jtak.core.TrainingNotice;

/**
 * 点を置けなかったときに画面に出す文。距離だけの値は捨てず、この文とログに残す。
 * <p>
 * Messages displayed when a point cannot be placed. Distance-only readings are retained in these messages and the
 * log rather than discarded.
 */
public final class LrfMessages {
    private LrfMessages() {}

    public static String forStatus(TargetStatus status, RangeReading reading, Lang lang) {
        Objects.requireNonNull(lang, "lang");
        String distance = String.format(Locale.ROOT, lang == Lang.JA ? "距離 %.1f m" : "distance %.1f m", reading.distanceMeters());
        String body;
        switch (status) {
            case OK:
                body = lang == Lang.JA ? "点を置きました（" + distance + "）"
                        : "Point placed (" + distance + ")";
                break;
            case NO_OBSERVER:
                body = lang == Lang.JA ? "自分の位置が取れないため点を置きません（" + distance + "）"
                        : "Point not placed: own position unavailable (" + distance + ")";
                break;
            case INVALID_OBSERVER:
                body = lang == Lang.JA ? "自分の位置が正しくないため点を置きません（" + distance + "）"
                        : "Point not placed: own position invalid (" + distance + ")";
                break;
            case INVALID_DISTANCE:
                body = lang == Lang.JA ? "距離が正しくないため点を置きません（" + distance + "）"
                        : "Point not placed: distance invalid (" + distance + ")";
                break;
            case DISTANCE_OUT_OF_RANGE:
                body = lang == Lang.JA ? "距離が上限 20 km を超えたため点を置きません（" + distance + "）"
                        : "Point not placed: distance exceeds the 20 km limit (" + distance + ")";
                break;
            case INVALID_ELEVATION:
                body = lang == Lang.JA ? "俯仰が正しくないため点を置きません（" + distance + "）"
                        : "Point not placed: elevation angle invalid (" + distance + ")";
                break;
            case AZIMUTH_UNAVAILABLE:
                body = lang == Lang.JA ? "方位が取れなかったため点を置きません（" + distance + "）"
                        : "Point not placed: azimuth unavailable (" + distance + ")";
                break;
            case INVALID_DECLINATION:
                body = lang == Lang.JA ? "磁気偏差が取れないため点を置きません（" + distance + "）"
                        : "Point not placed: magnetic declination unavailable (" + distance + ")";
                break;
            default:
                throw new IllegalArgumentException("未対応の状態: " + status);
        }
        return TrainingNotice.text() + " — " + body;
    }
}
