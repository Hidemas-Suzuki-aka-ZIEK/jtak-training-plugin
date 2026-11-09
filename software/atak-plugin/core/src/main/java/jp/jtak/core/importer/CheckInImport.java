package jp.jtak.core.importer;

import jp.jtak.core.cas.CheckInMode;

/**
 * チェックインの読み込み結果。null は「ファイルになかった・飛ばした」。（不変）。
 * <p>
 * Check-in import result. Null means “absent from file or skipped.” Immutable.
 */
public final class CheckInImport {
    private final String jtacCallsign;
    private final String aircraftCallsign;
    private final CheckInMode mode;
    private final String missionNumber;
    private final String aircraft;
    private final String positionAltitude;
    private final String ordnance;
    private final String playtime;
    private final String capabilities;
    private final String abortCode;
    private final Boolean abortCodeNone;

    private CheckInImport(Builder b) {
        this.jtacCallsign = b.jtacCallsign;
        this.aircraftCallsign = b.aircraftCallsign;
        this.mode = b.mode;
        this.missionNumber = b.missionNumber;
        this.aircraft = b.aircraft;
        this.positionAltitude = b.positionAltitude;
        this.ordnance = b.ordnance;
        this.playtime = b.playtime;
        this.capabilities = b.capabilities;
        this.abortCode = b.abortCode;
        this.abortCodeNone = b.abortCodeNone;
    }

    public String jtacCallsign() {
        return jtacCallsign;
    }

    public String aircraftCallsign() {
        return aircraftCallsign;
    }

    public CheckInMode mode() {
        return mode;
    }

    public String missionNumber() {
        return missionNumber;
    }

    public String aircraft() {
        return aircraft;
    }

    public String positionAltitude() {
        return positionAltitude;
    }

    public String ordnance() {
        return ordnance;
    }

    public String playtime() {
        return playtime;
    }

    public String capabilities() {
        return capabilities;
    }

    public String abortCode() {
        return abortCode;
    }

    public Boolean abortCodeNone() {
        return abortCodeNone;
    }

    /**
     * パーサが値を集めるための入れ物。
     * <p>
     * Container for values collected by the parser.
     */
    static final class Builder {
        String jtacCallsign;
        String aircraftCallsign;
        CheckInMode mode;
        String missionNumber;
        String aircraft;
        String positionAltitude;
        String ordnance;
        String playtime;
        String capabilities;
        String abortCode;
        Boolean abortCodeNone;

        CheckInImport build() {
            return new CheckInImport(this);
        }
    }
}
