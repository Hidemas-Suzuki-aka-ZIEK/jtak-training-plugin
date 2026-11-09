package jp.jtak.core.weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 兵器の表の行 1 つ。輪は内側から外側の順（半径の小さい順）。
 * <p>
 * One row in the weapon table. Rings are ordered from innermost to outermost (increasing radius).
 */
public final class WeaponEntry {
    private final String id;
    private final String label;
    private final List<WeaponRing> rings;
    private final String note;

    /**
     * @param note null のときは空文字にする converted to an empty string if null
     */
    public WeaponEntry(String id, String label, List<WeaponRing> rings, String note) {
        this.id = id;
        this.label = label;
        this.rings = Collections.unmodifiableList(new ArrayList<>(rings));
        this.note = note == null ? "" : note;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public List<WeaponRing> rings() {
        return rings;
    }

    /**
     * メモ。ないときは空文字。
     * <p>
     * Note. Empty string if absent.
     */
    public String note() {
        return note;
    }
}
