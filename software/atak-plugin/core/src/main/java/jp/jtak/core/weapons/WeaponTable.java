package jp.jtak.core.weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 兵器の表（行の並び）。ファイルの {@code notice} と {@code trainingSampleValues} は型に持たない
 * （書くときに {@link WeaponTableCodec} が必ず入れる。04 の 2.3）。
 * <p>
 * Weapon table (ordered rows). Does not store the file's {@code notice} or {@code trainingSampleValues} in the
 * type ({@link WeaponTableCodec} always inserts them when writing; 04 §2.3).
 */
public final class WeaponTable {
    private final List<WeaponEntry> entries;

    public WeaponTable(List<WeaponEntry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<WeaponEntry> entries() {
        return entries;
    }
}
