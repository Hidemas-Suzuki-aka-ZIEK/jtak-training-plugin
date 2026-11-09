package jp.jtak.core.importer;

import jp.jtak.core.cas.MethodOfAttack;
import jp.jtak.core.cas.TypeOfControl;

/**
 * ゲームプランの読み込み結果。null は「ファイルになかった・飛ばした」。（不変）。
 * <p>
 * Game plan import result. Null means “absent from file or skipped.” Immutable.
 */
public final class GamePlanImport {
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

    private GamePlanImport(Builder b) {
        this.callsign = b.callsign;
        this.typeOfControl = b.typeOfControl;
        this.methodOfAttack = b.methodOfAttack;
        this.intent = b.intent;
        this.collateralConcerns = b.collateralConcerns;
        this.ordnance = b.ordnance;
        this.desiredEffects = b.desiredEffects;
        this.fuzing = b.fuzing;
        this.interval = b.interval;
        this.line6Digits = b.line6Digits;
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

    /**
     * パーサが値を集めるための入れ物。
     * <p>
     * Container for values collected by the parser.
     */
    static final class Builder {
        String callsign;
        TypeOfControl typeOfControl;
        MethodOfAttack methodOfAttack;
        String intent;
        String collateralConcerns;
        String ordnance;
        String desiredEffects;
        String fuzing;
        String interval;
        String line6Digits;

        GamePlanImport build() {
            return new GamePlanImport(this);
        }
    }
}
