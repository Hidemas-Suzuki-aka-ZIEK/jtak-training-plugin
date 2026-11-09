package jp.jtak.core.cas;

/**
 * チェックインの入力（docs/detailed-design/03-cas-other-products.md 2.2.2）。不変。
 * <p>
 * Check-in input (docs/detailed-design/03-cas-other-products.md §2.2.2). Immutable.
 */
public final class CheckIn {
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
    private final boolean abortCodeNone;

    private CheckIn(Builder b) {
        jtacCallsign = b.jtacCallsign;
        aircraftCallsign = b.aircraftCallsign;
        mode = b.mode == null ? CheckInMode.FULL : b.mode;
        missionNumber = b.missionNumber;
        aircraft = b.aircraft;
        positionAltitude = b.positionAltitude;
        ordnance = b.ordnance;
        playtime = b.playtime;
        capabilities = b.capabilities;
        abortCode = b.abortCode;
        abortCodeNone = b.abortCodeNone;
    }

    public String jtacCallsign() {
        return jtacCallsign;
    }

    public String aircraftCallsign() {
        return aircraftCallsign;
    }

    /**
     * null を渡しても {@link CheckInMode#FULL} になる。
     * <p>
     * Passing null also selects {@link CheckInMode#FULL}.
     */
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

    public boolean abortCodeNone() {
        return abortCodeNone;
    }

    public static final class Builder {
        private String jtacCallsign;
        private String aircraftCallsign;
        private CheckInMode mode;
        private String missionNumber;
        private String aircraft;
        private String positionAltitude;
        private String ordnance;
        private String playtime;
        private String capabilities;
        private String abortCode;
        private boolean abortCodeNone;

        public Builder jtacCallsign(String v) {
            jtacCallsign = v;
            return this;
        }

        public Builder aircraftCallsign(String v) {
            aircraftCallsign = v;
            return this;
        }

        public Builder mode(CheckInMode v) {
            mode = v;
            return this;
        }

        public Builder missionNumber(String v) {
            missionNumber = v;
            return this;
        }

        public Builder aircraft(String v) {
            aircraft = v;
            return this;
        }

        public Builder positionAltitude(String v) {
            positionAltitude = v;
            return this;
        }

        public Builder ordnance(String v) {
            ordnance = v;
            return this;
        }

        public Builder playtime(String v) {
            playtime = v;
            return this;
        }

        public Builder capabilities(String v) {
            capabilities = v;
            return this;
        }

        public Builder abortCode(String v) {
            abortCode = v;
            return this;
        }

        public Builder abortCodeNone(boolean v) {
            abortCodeNone = v;
            return this;
        }

        /**
         * @throws IllegalArgumentException abortCode と abortCodeNone を両方入れたとき if both abortCode and abortCodeNone are supplied
         */
        public CheckIn build() {
            if (abortCodeNone && CasText.clean(abortCode) != null) {
                throw new IllegalArgumentException("abortCode と abortCodeNone は両方入れられない");
            }
            return new CheckIn(this);
        }
    }
}
