package jp.jtak.core.importer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jp.jtak.core.Lang;

/**
 * 飛ばした項目を集める（設計 06 の 2.6）。文は {@code lang} の言語で作る。
 * <p>
 * Collects skipped items (design 06 §2.6). Creates messages in the {@code lang} language.
 */
final class ImportLog {
    private final Lang lang;
    private final List<String> items = new ArrayList<>();
    private final Set<String> claimed = new HashSet<>();
    private int hits;

    ImportLog(Lang lang) {
        this.lang = lang;
    }

    /**
     * 値が空・{@code （未入力）} だった。
     * <p>
     * The value was empty or {@code （未入力）}.
     */
    void notEntered(String name) {
        add(name, lang == Lang.JA ? "未入力" : "not entered");
    }

    /**
     * 読めない値だった。
     * <p>
     * The value could not be read.
     */
    void unreadable(String name) {
        add(name, lang == Lang.JA ? "読めない" : "unreadable");
    }

    /**
     * 同じ項目が 2 回以上あった。
     * <p>
     * The same item occurred more than once.
     */
    void duplicate(String name) {
        add(name, lang == Lang.JA ? "重複" : "duplicate");
    }

    /**
     * 両立しない組み合わせだった。
     * <p>
     * The combination was incompatible.
     */
    void conflict(String name) {
        add(name, lang == Lang.JA ? "組み合わせ" : "conflict");
    }

    /**
     * 必須の項目がファイルになかった。
     * <p>
     * A required item was absent from the file.
     */
    void missing(String name) {
        add(name, lang == Lang.JA ? "なし" : "missing");
    }

    /**
     * 2 か所で値が違った。
     * <p>
     * Values differed between two locations.
     */
    void differs(String name) {
        add(name, lang == Lang.JA ? "2 か所で違う" : "differs in 2 places");
    }

    /**
     * どのラベルにも合わない行。
     * <p>
     * A line matching no label.
     */
    void unknownLine(int number) {
        items.add(lang == Lang.JA ? number + " 行目" : "line " + number);
    }

    /**
     * その項目を初めて見たなら true。2 回目以降は {@code （重複）} を記録して false。
     * <p>
     * True on the first occurrence of an item. Records {@code （重複）} and returns false on subsequent occurrences.
     */
    boolean claim(String name) {
        if (claimed.add(name)) {
            return true;
        }
        duplicate(name);
        return false;
    }

    /**
     * その項目をもう見たか（記録はしない）。
     * <p>
     * Whether the item has already occurred (without recording anything).
     */
    boolean seen(String name) {
        return claimed.contains(name);
    }

    /**
     * 見ていない項目だけ {@code （なし）} を記録する。
     * <p>
     * Records {@code （なし）} only for items not yet encountered.
     */
    void missingIfUnseen(String name) {
        if (!claimed.contains(name)) {
            missing(name);
        }
    }

    /**
     * 値を整える。空・{@code （未入力）} なら {@code （未入力）} を記録して null。値があれば読めた数に入れる。
     * <p>
     * Normalizes a value. If empty or {@code （未入力）}, records {@code （未入力）} and returns null. Counts nonempty
     * values as successfully read.
     */
    String entered(String name, String raw) {
        String v = ScenarioLines.clean(raw);
        if (v == null || ScenarioLines.NOT_ENTERED.equals(v)) {
            notEntered(name);
            return null;
        }
        hit();
        return v;
    }

    /**
     * 項目を 1 つ読めたと数える。
     * <p>
     * Counts one item as successfully read.
     */
    void hit() {
        hits++;
    }

    /**
     * 読めたと数えた項目を 1 つ取り消す（組み合わせで飛ばしたとき。設計 06 の 2.5・2.7 の 10）。
     * <p>
     * Reverses the successful-read count for one item (when skipped due to a combination; design 06 §§2.5, 2.7
     * item 10).
     */
    void unhit() {
        if (hits > 0) {
            hits--;
        }
    }

    boolean anyRead() {
        return hits > 0;
    }

    List<String> items() {
        return new ArrayList<>(items);
    }

    private void add(String name, String reason) {
        items.add(lang == Lang.JA ? name + "（" + reason + "）" : name + " (" + reason + ")");
    }
}
