package jp.jtak.core.weapons;

/**
 * 味方の点の判定に使う点（core は ATAK の型を知らない）。不変。
 * <p>
 * Point used to assess friendly points (core has no knowledge of ATAK types). Immutable.
 */
public final class PointRef {
    private final String uid;
    private final String name;
    private final String cotType;
    private final double lat;
    private final double lon;
    private final boolean self;

    /**
     * @param cotType null は空と同じ。 null is equivalent to empty
     * @param self 自分の点なら true true for the self point
     */
    public PointRef(String uid, String name, String cotType, double lat, double lon, boolean self) {
        this.uid = uid == null ? "" : uid;
        this.name = name == null ? "" : name;
        this.cotType = cotType == null ? "" : cotType;
        this.lat = lat;
        this.lon = lon;
        this.self = self;
    }

    public String uid() {
        return uid;
    }

    public String name() {
        return name;
    }

    public String cotType() {
        return cotType;
    }

    public double lat() {
        return lat;
    }

    public double lon() {
        return lon;
    }

    public boolean self() {
        return self;
    }
}
