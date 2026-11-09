package jp.jtak.core.cas;

/**
 * ゲームプランの入力（docs/detailed-design/03-cas-other-products.md 2.2.3）。不変。
 * <p>
 * Game plan input (docs/detailed-design/03-cas-other-products.md §2.2.3). Immutable.
 */
public final class GamePlan {
    private final String callsign;
    private final TypeOfControl typeOfControl;
    private final MethodOfAttack methodOfAttack;
    private final String intent;
    private final String collateralConcerns;
    private final String ordnance;
    private final String desiredEffects;
    private final String fuzing;
    private final String interval;
    private final String line6Digits;

    private GamePlan(Builder b) {
        callsign = b.callsign;
        typeOfControl = b.typeOfControl;
        methodOfAttack = b.methodOfAttack;
        intent = b.intent;
        collateralConcerns = b.collateralConcerns;
        ordnance = b.ordnance;
        desiredEffects = b.desiredEffects;
        fuzing = b.fuzing;
        interval = b.interval;
        line6Digits = b.line6Digits;
    }

    public String callsign() {
        return callsign;
    }

    public TypeOfControl typeOfControl() {
        return typeOfControl;
    }

    public MethodOfAttack methodOfAttack() {
        return methodOfAttack;
    }

    public String intent() {
        return intent;
    }

    public String collateralConcerns() {
        return collateralConcerns;
    }

    public String ordnance() {
        return ordnance;
    }

    public String desiredEffects() {
        return desiredEffects;
    }

    public String fuzing() {
        return fuzing;
    }

    public String interval() {
        return interval;
    }

    public String line6Digits() {
        return line6Digits;
    }

    public static final class Builder {
        private String callsign;
        private TypeOfControl typeOfControl;
        private MethodOfAttack methodOfAttack;
        private String intent;
        private String collateralConcerns;
        private String ordnance;
        private String desiredEffects;
        private String fuzing;
        private String interval;
        private String line6Digits;

        public Builder callsign(String v) {
            callsign = v;
            return this;
        }

        public Builder typeOfControl(TypeOfControl v) {
            typeOfControl = v;
            return this;
        }

        public Builder methodOfAttack(MethodOfAttack v) {
            methodOfAttack = v;
            return this;
        }

        public Builder intent(String v) {
            intent = v;
            return this;
        }

        public Builder collateralConcerns(String v) {
            collateralConcerns = v;
            return this;
        }

        public Builder ordnance(String v) {
            ordnance = v;
            return this;
        }

        public Builder desiredEffects(String v) {
            desiredEffects = v;
            return this;
        }

        public Builder fuzing(String v) {
            fuzing = v;
            return this;
        }

        public Builder interval(String v) {
            interval = v;
            return this;
        }

        public Builder line6Digits(String v) {
            line6Digits = v;
            return this;
        }

        public GamePlan build() {
            return new GamePlan(this);
        }
    }
}
