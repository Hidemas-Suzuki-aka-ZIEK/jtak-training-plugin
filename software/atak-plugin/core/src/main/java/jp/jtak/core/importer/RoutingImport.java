package jp.jtak.core.importer;

import jp.jtak.core.cas.RoutingClosing;
import jp.jtak.core.cas.RoutingInstruction;

/**
 * ルーティングの読み込み結果。null は「ファイルになかった・飛ばした」。（不変）。
 * <p>
 * Routing import result. Null means “absent from file or skipped.” Immutable.
 */
public final class RoutingImport {
    private final String callsign;
    private final RoutingInstruction instruction;
    private final String place;
    private final String otherInstructions;
    private final Boolean onlyAircraftOnStation;
    private final String otherAos;
    private final String threats;
    private final String weatherTerrain;
    private final String statusCalls;
    private final RoutingClosing closing;
    private final String holdReason;

    private RoutingImport(Builder b) {
        this.callsign = b.callsign;
        this.instruction = b.instruction;
        this.place = b.place;
        this.otherInstructions = b.otherInstructions;
        this.onlyAircraftOnStation = b.onlyAircraftOnStation;
        this.otherAos = b.otherAos;
        this.threats = b.threats;
        this.weatherTerrain = b.weatherTerrain;
        this.statusCalls = b.statusCalls;
        this.closing = b.closing;
        this.holdReason = b.holdReason;
    }

    public String callsign() {
        return callsign;
    }

    public RoutingInstruction instruction() {
        return instruction;
    }

    public String place() {
        return place;
    }

    public String otherInstructions() {
        return otherInstructions;
    }

    public Boolean onlyAircraftOnStation() {
        return onlyAircraftOnStation;
    }

    public String otherAos() {
        return otherAos;
    }

    public String threats() {
        return threats;
    }

    public String weatherTerrain() {
        return weatherTerrain;
    }

    public String statusCalls() {
        return statusCalls;
    }

    public RoutingClosing closing() {
        return closing;
    }

    public String holdReason() {
        return holdReason;
    }

    /**
     * パーサが値を集めるための入れ物。
     * <p>
     * Container for values collected by the parser.
     */
    static final class Builder {
        String callsign;
        RoutingInstruction instruction;
        String place;
        String otherInstructions;
        Boolean onlyAircraftOnStation;
        String otherAos;
        String threats;
        String weatherTerrain;
        String statusCalls;
        RoutingClosing closing;
        String holdReason;

        RoutingImport build() {
            return new RoutingImport(this);
        }
    }
}
