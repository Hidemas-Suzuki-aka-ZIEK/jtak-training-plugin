package jp.jtak.core.lrf;

/**
 * 目標の計算の結果。{@code target} は {@link TargetStatus#OK} のときだけ非 null。
 * <p>
 * Target calculation result. {@code target} is non-null only for {@link TargetStatus#OK}.
 */
public final class TargetSolution {
    private final TargetStatus status;
    private final GeoPosition target;
    private final double trueAzimuthDeg;
    private final double declinationDeg;
    private final double horizontalDistanceMeters;
    private final double heightDifferenceMeters;

    public TargetSolution(TargetStatus status, GeoPosition target, double trueAzimuthDeg,
            double declinationDeg, double horizontalDistanceMeters, double heightDifferenceMeters) {
        this.status = status;
        this.target = target;
        this.trueAzimuthDeg = trueAzimuthDeg;
        this.declinationDeg = declinationDeg;
        this.horizontalDistanceMeters = horizontalDistanceMeters;
        this.heightDifferenceMeters = heightDifferenceMeters;
    }

    static TargetSolution failure(TargetStatus status) {
        return new TargetSolution(status, null, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    public TargetStatus status() {
        return status;
    }

    public GeoPosition target() {
        return target;
    }

    public double trueAzimuthDeg() {
        return trueAzimuthDeg;
    }

    /**
     * 真方位の入力のときは NaN。
     * <p>
     * NaN for true-bearing input.
     */
    public double declinationDeg() {
        return declinationDeg;
    }

    public double horizontalDistanceMeters() {
        return horizontalDistanceMeters;
    }

    public double heightDifferenceMeters() {
        return heightDifferenceMeters;
    }
}
