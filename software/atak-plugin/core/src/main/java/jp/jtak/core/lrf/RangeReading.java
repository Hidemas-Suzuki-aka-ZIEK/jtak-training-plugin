package jp.jtak.core.lrf;

/**
 * LRF などの 1 回の測距値。俯仰は上が正。
 * <p>
 * One measurement from an LRF or similar source. Elevation angle is positive upward.
 */
public final class RangeReading {
    private final double distanceMeters;
    private final double azimuthDeg;
    private final AzimuthReference azimuthReference;
    private final double elevationDeg;
    private final long epochMillis;
    private final InputSource source;
    private final String deviceName;

    public RangeReading(double distanceMeters, double azimuthDeg, AzimuthReference azimuthReference,
            double elevationDeg, long epochMillis, InputSource source, String deviceName) {
        if (azimuthReference == null) {
            throw new IllegalArgumentException("azimuthReference must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.distanceMeters = distanceMeters;
        this.azimuthDeg = azimuthDeg;
        this.azimuthReference = azimuthReference;
        this.elevationDeg = elevationDeg;
        this.epochMillis = epochMillis;
        this.source = source;
        this.deviceName = deviceName;
    }

    public double distanceMeters() {
        return distanceMeters;
    }

    public double azimuthDeg() {
        return azimuthDeg;
    }

    public AzimuthReference azimuthReference() {
        return azimuthReference;
    }

    public double elevationDeg() {
        return elevationDeg;
    }

    public long epochMillis() {
        return epochMillis;
    }

    public InputSource source() {
        return source;
    }

    /**
     * null 可。
     * <p>
     * May be null.
     */
    public String deviceName() {
        return deviceName;
    }
}
