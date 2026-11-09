package jp.jtak.core.lrf;

/**
 * WGS84 の位置。高さは楕円体高（HAE）で、不明は {@link Double#NaN}。
 * <p>
 * WGS84 position. Altitude is height above ellipsoid (HAE), or {@link Double#NaN} if unknown.
 */
public final class GeoPosition {
    private final double latitudeDeg;
    private final double longitudeDeg;
    private final double haeMeters;

    public GeoPosition(double latitudeDeg, double longitudeDeg, double haeMeters) {
        this.latitudeDeg = latitudeDeg;
        this.longitudeDeg = longitudeDeg;
        this.haeMeters = haeMeters;
    }

    public double latitudeDeg() {
        return latitudeDeg;
    }

    public double longitudeDeg() {
        return longitudeDeg;
    }

    public double haeMeters() {
        return haeMeters;
    }

    public boolean hasAltitude() {
        return !Double.isNaN(haeMeters) && !Double.isInfinite(haeMeters);
    }
}
