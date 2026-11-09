package jp.jtak.plugin.cas;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MetaMapPoint;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.toolbars.DynamicRangeAndBearingEndpoint;
import com.atakmap.android.toolbars.RangeAndBearingEndpoint;
import com.atakmap.coremap.maps.coords.GeoPoint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jp.jtak.plugin.UidPrefixes;

/**
 * 地図の点の一覧を取る（02 3.2。表示だけで、送信や編集はしない）。
 * <p>
 * Retrieves the list of map points (02 §3.2; display only, without transmission or editing).
 */
public final class MapPointLister {
    /**
     * ATAK がターゲットバブル（拡大表示）の位置に使う内部の点の UID。地図の上に見えるマーカーではない（02 5.1）。
     * ATAK 自身も同じ組（{@link MetaMapPoint} で UID がこの文字）で見分けている
     * （com.atakmap.android.coordoverlay.CoordOverlayMapReceiver）。
     * <p>
     * UID of the internal point ATAK uses to position the target bubble (magnified view). This is not a visible
     * map marker (02 §5.1). ATAK itself identifies it by the same combination ({@link MetaMapPoint} with this UID
     * string) in com.atakmap.android.coordoverlay.CoordOverlayMapReceiver.
     */
    private static final String ATAK_INTERNAL_TARGET_POINT_UID = "TARGET_POINT";

    private MapPointLister() {}

    /**
     * JTAK が描いた兵器の範囲の円、またはその部品か（UID か shapeUID が {@code jtak-wpn-} で始まる）。
     * <p>
     * Whether this is a weapon effect circle drawn by JTAK or one of its components (UID or shapeUID starts with
     * {@code jtak-wpn-}).
     */
    static boolean isWeaponCircle(MapItem item) {
        String uid = item.getUID();
        if (uid != null && uid.startsWith(UidPrefixes.WEAPON_CIRCLE)) {
            return true;
        }
        String shape = item.getMetaString("shapeUID", null);
        return shape != null && shape.startsWith(UidPrefixes.WEAPON_CIRCLE);
    }

    /**
     * 一覧から外す点か（02 3.2）。外す点は次の 3 つで、どれもクラス（と UID）で名指しする。
     * <ul>
     *   <li>UID が {@code TARGET_POINT} の {@link MetaMapPoint}（ATAK がターゲットバブルに使う内部の点）</li>
     *   <li>測距線（Range &amp; Bearing）の端点 {@link RangeAndBearingEndpoint}（{@link MetaMapPoint} の子）</li>
     *   <li>測距線の端点 {@link DynamicRangeAndBearingEndpoint}（{@code Marker} の子）</li>
     * </ul>
     * 上の 3 つ以外の {@link MetaMapPoint} と、利用者のマーカー・自分の位置は外さない。
     * <p>
     * Whether to exclude a point from the list (02 §3.2). The following three kinds are explicitly identified by
     * class (and UID): <ul><li>{@link MetaMapPoint} with UID {@code TARGET_POINT} (ATAK's internal point for the
     * target bubble)</li><li>Range &amp; Bearing endpoint {@link RangeAndBearingEndpoint} (a subclass of {@link
     * MetaMapPoint})</li><li>Range &amp; Bearing endpoint {@link DynamicRangeAndBearingEndpoint} (a subclass of
     * {@code Marker})</li></ul> Does not exclude other {@link MetaMapPoint} instances, user markers, or the user's
     * own position.
     */
    static boolean isAtakInternal(MapItem item) {
        if (item instanceof RangeAndBearingEndpoint || item instanceof DynamicRangeAndBearingEndpoint) {
            return true;
        }
        return item instanceof MetaMapPoint
                && ATAK_INTERNAL_TARGET_POINT_UID.equals(item.getUID());
    }

    /**
     * 点の名前。自分の位置のマーカー（UID が {@code MapView.getDeviceUid()} と同じ）は callsign を使う。
     * callsign が取れなければ、ほかの点と同じく getTitle（なければ UID）を使う（02 5.1）。
     * <p>
     * Point name. Uses callsign for the self-position marker (whose UID equals {@code MapView.getDeviceUid()}). If
     * callsign is unavailable, uses getTitle (or UID if absent), as for other points (02 §5.1).
     */
    public static String nameOf(MapView mapView, MapItem item) {
        if (item.getUID() != null && item.getUID().equals(MapView.getDeviceUid())) {
            String callsign = mapView.getDeviceCallsign();
            if (callsign != null && !callsign.trim().isEmpty()) {
                return callsign.trim();
            }
        }
        String title = item.getTitle();
        return title == null || title.trim().isEmpty() ? item.getUID() : title.trim();
    }

    /**
     * 見えている点（PointMapItem）を、名前の順に返す。呼んだ時点の写し。
     * <p>
     * Returns visible points (PointMapItem) sorted by name. A snapshot at the time of the call.
     */
    public static List<MapPoint> list(MapView mapView) {
        List<MapPoint> raw = new ArrayList<>();
        MapGroup root = mapView.getRootGroup();
        Collection<MapItem> items = root == null ? null : root.getItemsRecursive();
        if (items != null) {
            for (MapItem item : items) {
                if (!(item instanceof PointMapItem) || !item.getVisible() || isAtakInternal(item)
                        || isWeaponCircle(item)) {
                    continue;
                }
                GeoPoint p = ((PointMapItem) item).getPoint();
                if (p == null || Double.isNaN(p.getLatitude()) || Double.isNaN(p.getLongitude())) {
                    continue;
                }
                String name = nameOf(mapView, item);
                raw.add(new MapPoint(item.getUID(), name, name, p, item.getType()));
            }
        }
        Collections.sort(raw, new Comparator<MapPoint>() {
            @Override
            public int compare(MapPoint a, MapPoint b) {
                return a.name().compareToIgnoreCase(b.name());
            }
        });
        Map<String, Integer> counts = new HashMap<>();
        for (MapPoint m : raw) {
            String key = m.name().toLowerCase(Locale.ROOT);
            Integer n = counts.get(key);
            counts.put(key, n == null ? 1 : n + 1);
        }
        List<MapPoint> result = new ArrayList<>(raw.size());
        for (MapPoint m : raw) {
            if (counts.get(m.name().toLowerCase(Locale.ROOT)) > 1) {
                String label = String.format(Locale.ROOT, "%s (%.4f, %.4f)", m.name(),
                        m.point().getLatitude(), m.point().getLongitude());
                result.add(new MapPoint(m.uid(), m.name(), label, m.point(), m.type()));
            } else {
                result.add(m);
            }
        }
        return result;
    }
}
