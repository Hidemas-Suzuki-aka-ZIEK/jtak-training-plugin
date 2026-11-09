package jp.jtak.core.weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表を読んだ結果。{@code errors} が空なら成功で、{@code table} は検査に通らなければ null。
 * <p>
 * Table read result. Success if {@code errors} is empty; {@code table} is null if validation fails.
 */
public final class WeaponTableResult {
    private final WeaponTable table;
    private final List<String> errors;

    WeaponTableResult(WeaponTable table, List<String> errors) {
        this.table = table;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public WeaponTable table() {
        return table;
    }

    /**
     * 理由（日本語。場所の道筋が頭に付く）。空なら成功。
     * <p>
     * Reasons (Japanese; prefixed with the location path). Empty on success.
     */
    public List<String> errors() {
        return errors;
    }

    public boolean ok() {
        return errors.isEmpty() && table != null;
    }
}
