package jp.jtak.plugin.lrf;

import android.hardware.GeomagneticField;
import android.util.Log;
import android.widget.Toast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.maps.coords.GeoPoint;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import jp.jtak.core.Lang;
import jp.jtak.core.lrf.GeoPosition;
import jp.jtak.core.lrf.LrfMessages;
import jp.jtak.core.lrf.LrfRemarks;
import jp.jtak.core.lrf.RangeReading;
import jp.jtak.core.lrf.TargetCalculator;
import jp.jtak.core.lrf.TargetSolution;
import jp.jtak.core.lrf.TargetStatus;

/**
 * 測距値から目標を計算し、地図にマーカーを置く。UI のスレッドで呼ぶ。送信はしない（U8）。
 * <p>
 * Calculates a target from a measurement and places a marker on the map. Called on the UI thread. Does not
 * transmit (U8).
 */
public final class LrfMarkingController {
    private static final String TAG = "JtakLrf";
    private final MapView mapView;
    private final Lang lang;
    public LrfMarkingController(MapView mapView, Lang lang) {
        this.mapView = mapView;
        this.lang = lang;
    }

    public void handle(RangeReading reading) {
        GeoPosition observer = currentObserver();
        double declination = Double.NaN;
        if (observer != null) {
            double alt = observer.hasAltitude() ? observer.haeMeters() : 0.0;
            declination = new GeomagneticField((float) observer.latitudeDeg(),
                    (float) observer.longitudeDeg(), (float) alt, System.currentTimeMillis())
                    .getDeclination();
        }
        TargetSolution solution = TargetCalculator.solve(observer, reading, declination);
        if (solution.status() != TargetStatus.OK) {
            String msg = LrfMessages.forStatus(solution.status(), reading, lang);
            Log.w(TAG, msg);
            Toast.makeText(mapView.getContext(), msg, Toast.LENGTH_LONG).show();
            return;
        }
        placeMarker(reading, solution);
    }

    private GeoPosition currentObserver() {
        Marker self = mapView.getSelfMarker();
        if (self == null) {
            return null;
        }
        GeoPoint p = self.getPoint();
        if (p == null || !p.isValid()) {
            return null;
        }
        boolean hae = p.isAltitudeValid() && p.getAltitudeReference() == GeoPoint.AltitudeReference.HAE;
        return new GeoPosition(p.getLatitude(), p.getLongitude(), hae ? p.getAltitude() : Double.NaN);
    }

    private void placeMarker(RangeReading reading, TargetSolution solution) {
        GeoPosition t = solution.target();
        double alt = t.hasAltitude() ? t.haeMeters() : GeoPoint.UNKNOWN;
        GeoPoint point = new GeoPoint(t.latitudeDeg(), t.longitudeDeg(), alt);
        String callsign = "JTAK LRF "
                + new SimpleDateFormat("HHmmss", Locale.ROOT).format(new Date());
        new PlacePointTool.MarkerCreator(point)
                .setType("a-u-G")
                .setCallsign(callsign)
                .setMetaString("remarks", LrfRemarks.format(reading, solution, lang))
                .showCotDetails(false)
                .placePoint();
    }

    /**
     * 画面に文を出す（手入力の入力誤りなど）。
     * <p>
     * Displays a message on the screen (such as invalid manual input).
     */
    public void toast(String message) {
        Toast.makeText(mapView.getContext(), message, Toast.LENGTH_LONG).show();
    }
}
