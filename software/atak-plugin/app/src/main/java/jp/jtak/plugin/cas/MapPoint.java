package jp.jtak.plugin.cas;

import com.atakmap.coremap.maps.coords.GeoPoint;

/**
 * 地図の点 1 つ（一覧に出す名前と位置）。
 * <p>
 * One map point (display name and position).
 */
public final class MapPoint {
    private final String uid;
    private final String name;
    private final String label;
    private final GeoPoint point;
    private final String type;

    MapPoint(String uid, String name, String label, GeoPoint point, String type) {
        this.uid = uid;
        this.name = name;
        this.label = label;
        this.point = point;
        this.type = type;
    }

    /**
     * 点の種類（CoT の type。{@code MapItem.getType()}）。種類がないときは {@code MapItem.getType()} の値（{@code MenuItem.EMPTY_TYPE}）のまま。5 行目の下書きに使う（02 3.3）。
     * <p>
     * Point type (CoT type; {@code MapItem.getType()}). If no type is set, retains the value from {@code
     * MapItem.getType()} ({@code MenuItem.EMPTY_TYPE}). Used to draft line 5 (02 §3.3).
     */
    public String type() {
        return type;
    }

    /**
     * 地図の点の UID。一覧を読み直したあとの選び直しに使う（ラベルは重なることがある）。
     * <p>
     * Map point UID. Used to restore selection after reloading the list (labels may be duplicated).
     */
    public String uid() {
        return uid;
    }

    /**
     * 点の名前（9-line の 1 行目に使う）。
     * <p>
     * Point name (used for line 1 of the 9-line).
     */
    public String name() {
        return name;
    }

    /**
     * 一覧に出す名前。同じ名前の点があるときは緯度経度の短い表示を足してある。
     * <p>
     * Name displayed in the list. A short latitude/longitude representation is appended when point names are
     * duplicated.
     */
    public String label() {
        return label;
    }

    public GeoPoint point() {
        return point;
    }

    @Override
    public String toString() {
        return label;
    }
}
