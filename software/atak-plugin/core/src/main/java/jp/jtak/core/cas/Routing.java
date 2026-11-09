package jp.jtak.core.cas;

/**
 * ルーティングの入力（docs/detailed-design/03-cas-other-products.md 2.2.1）。不変。
 * <p>
 * Routing input (docs/detailed-design/03-cas-other-products.md §2.2.1). Immutable.
 */
public final class Routing {
    private final String callsign;
    private final RoutingInstruction instruction;
    private final String place;
    private final String otherInstructions;
    private final boolean onlyAircraftOnStation;
    private final String otherAos;
    private final String threats;
    private final String weatherTerrain;
    private final String statusCalls;
    private final RoutingClosing closing;
    private final String holdReason;

    private Routing(Builder b) {
        callsign = b.callsign;
        instruction = b.instruction;
        place = b.place;
        otherInstructions = b.otherInstructions;
        onlyAircraftOnStation = b.onlyAircraftOnStation;
        otherAos = b.otherAos;
        threats = b.threats;
        weatherTerrain = b.weatherTerrain;
        statusCalls = b.statusCalls;
        closing = b.closing;
        holdReason = b.holdReason;
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

    public boolean onlyAircraftOnStation() {
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

    public static final class Builder {
        private String callsign;
        private RoutingInstruction instruction;
        private String place;
        private String otherInstructions;
        private boolean onlyAircraftOnStation;
        private String otherAos;
        private String threats;
        private String weatherTerrain;
        private String statusCalls;
        private RoutingClosing closing;
        private String holdReason;

        public Builder callsign(String v) {
            callsign = v;
            return this;
        }

        public Builder instruction(RoutingInstruction v) {
            instruction = v;
            return this;
        }

        public Builder place(String v) {
            place = v;
            return this;
        }

        public Builder otherInstructions(String v) {
            otherInstructions = v;
            return this;
        }

        public Builder onlyAircraftOnStation(boolean v) {
            onlyAircraftOnStation = v;
            return this;
        }

        public Builder otherAos(String v) {
            otherAos = v;
            return this;
        }

        public Builder threats(String v) {
            threats = v;
            return this;
        }

        public Builder weatherTerrain(String v) {
            weatherTerrain = v;
            return this;
        }

        public Builder statusCalls(String v) {
            statusCalls = v;
            return this;
        }

        public Builder closing(RoutingClosing v) {
            closing = v;
            return this;
        }

        public Builder holdReason(String v) {
            holdReason = v;
            return this;
        }

        /**
         * @throws IllegalArgumentException 「ほかの機はいない」と Other AOS の手入力を両方入れたとき if both “no other aircraft” and manual Other AOS input are supplied
         */
        public Routing build() {
            if (onlyAircraftOnStation && CasText.clean(otherAos) != null) {
                throw new IllegalArgumentException(
                        "onlyAircraftOnStation と otherAos は両方入れられない");
            }
            return new Routing(this);
        }
    }
}
