package jp.jtak.core.weapons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import jp.jtak.core.Lang;

/**
 * 既定の表。汎用の区分 3 行だけで、値は出典のない、意味のない切りのよい数（訓練用サンプル値）。
 * 実在の兵器・実際の安全距離・危害半径とは関係がない（04 の 2.4。U3）。
 * <p>
 * Default table. Only three generic categories; values are arbitrary round numbers with no source or real meaning
 * (training sample values). Unrelated to real weapons, actual safety distances, or hazard radii (04 §2.4; U3).
 */
public final class DefaultWeaponTable {
    static final String NOTE = "訓練用サンプル値。実在の兵器の値ではない / TRAINING SAMPLE VALUE — NOT A REAL WEAPON";

    private DefaultWeaponTable() {}

    public static WeaponTable table(Lang lang) {
        Objects.requireNonNull(lang, "lang");
        List<WeaponEntry> entries = new ArrayList<>();
        entries.add(entry("sample-small", lang == Lang.JA ? "サンプル 小" : "Sample S", 100, 300, lang));
        entries.add(entry("sample-medium", lang == Lang.JA ? "サンプル 中" : "Sample M", 250, 600, lang));
        entries.add(entry("sample-large", lang == Lang.JA ? "サンプル 大" : "Sample L", 500, 1000, lang));
        return new WeaponTable(entries);
    }

    private static WeaponEntry entry(String id, String label, int inner, int outer, Lang lang) {
        return new WeaponEntry(id, label,
                Arrays.asList(new WeaponRing(lang == Lang.JA ? "内側" : "Inner", inner),
                        new WeaponRing(lang == Lang.JA ? "外側" : "Outer", outer)), NOTE);
    }
}
