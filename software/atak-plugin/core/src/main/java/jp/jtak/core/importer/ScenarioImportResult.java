package jp.jtak.core.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 読み込みの結果（不変）。{@code error} があれば DTO はすべて null（設計 06 の 2.1.1）。
 * <p>
 * Import result (immutable). All DTOs are null if {@code error} is present (design 06 §2.1.1).
 */
public final class ScenarioImportResult {
    private final ScenarioKind kind;
    private final RoutingImport routing;
    private final CheckInImport checkIn;
    private final GamePlanImport gamePlan;
    private final CasBriefImport casBrief;
    private final List<String> skippedItems;
    private final ScenarioImportError error;

    private ScenarioImportResult(
            ScenarioKind kind,
            RoutingImport routing,
            CheckInImport checkIn,
            GamePlanImport gamePlan,
            CasBriefImport casBrief,
            List<String> skippedItems,
            ScenarioImportError error) {
        this.kind = kind;
        this.routing = routing;
        this.checkIn = checkIn;
        this.gamePlan = gamePlan;
        this.casBrief = casBrief;
        this.skippedItems = Collections.unmodifiableList(new ArrayList<>(skippedItems));
        this.error = error;
    }

    static ScenarioImportResult failure(ScenarioKind kind, ScenarioImportError error, List<String> skipped) {
        return new ScenarioImportResult(kind, null, null, null, null, skipped, error);
    }

    static ScenarioImportResult routing(RoutingImport dto, List<String> skipped) {
        return new ScenarioImportResult(ScenarioKind.ROUTING, dto, null, null, null, skipped, null);
    }

    static ScenarioImportResult checkIn(CheckInImport dto, List<String> skipped) {
        return new ScenarioImportResult(ScenarioKind.CHECK_IN, null, dto, null, null, skipped, null);
    }

    static ScenarioImportResult gamePlan(GamePlanImport dto, List<String> skipped) {
        return new ScenarioImportResult(ScenarioKind.GAME_PLAN, null, null, dto, null, skipped, null);
    }

    static ScenarioImportResult casBrief(CasBriefImport dto, List<String> skipped) {
        return new ScenarioImportResult(ScenarioKind.CAS_BRIEF, null, null, null, dto, skipped, null);
    }

    /**
     * 題名の行から決めた書式。題名が読めなければ null。
     * <p>
     * Format determined from the title line. Null if the title could not be read.
     */
    public ScenarioKind kind() {
        return kind;
    }

    public RoutingImport routing() {
        return routing;
    }

    public CheckInImport checkIn() {
        return checkIn;
    }

    public GamePlanImport gamePlan() {
        return gamePlan;
    }

    public CasBriefImport casBrief() {
        return casBrief;
    }

    /**
     * 飛ばした項目（ファイルの中の順。変えられない）。
     * <p>
     * Skipped items (file order; unmodifiable).
     */
    public List<String> skippedItems() {
        return skippedItems;
    }

    /**
     * 読み込まないときの理由。読めたときは null。
     * <p>
     * Reason for refusing an import. Null on successful import.
     */
    public ScenarioImportError error() {
        return error;
    }
}
