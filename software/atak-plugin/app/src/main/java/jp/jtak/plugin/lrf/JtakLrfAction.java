package jp.jtak.plugin.lrf;

import com.atakmap.android.lrf.RangeFinderAction2;
import com.atakmap.android.maps.MapView;
import jp.jtak.core.lrf.AzimuthReference;
import jp.jtak.core.lrf.InputSource;
import jp.jtak.core.lrf.RangeReading;

/**
 * ATAK 標準の LRF 連携（UDP 17211・Bluetooth の LRF）から測距値を受ける。UI のスレッドではない。
 * <p>
 * Receives measurements from ATAK's standard LRF integration (UDP 17211 and Bluetooth LRF). Not on the UI thread.
 */
public final class JtakLrfAction implements RangeFinderAction2 {
    private final MapView mapView;
    private final LrfMarkingController controller;
    /**
     * 切・停止の後に、予約済みの測距でマーカーを置かないための印。
     * <p>
     * Flag preventing markers from being placed by queued measurements after switching off or stopping.
     */
    private volatile boolean active;

    public JtakLrfAction(MapView mapView, LrfMarkingController controller) {
        this.mapView = mapView;
        this.controller = controller;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void onRangeFinderInfo(String uidPrefix, String name, double distance, double azimuth,
            double elevation) {
        final RangeReading reading = new RangeReading(distance, azimuth, AzimuthReference.MAGNETIC,
                elevation, System.currentTimeMillis(), InputSource.ATAK_LRF, name);
        mapView.post(() -> {
            if (active) {
                controller.handle(reading);
            }
        });
    }

    @Override
    public void onRangeFinderInfo(String uidPrefix, double distance, double azimuth, double elevation) {
        onRangeFinderInfo(uidPrefix, null, distance, azimuth, elevation);
    }
}
