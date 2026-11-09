package jp.jtak.core.weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jp.jtak.core.Lang;

/**
 * 画面の編集の欄（文字のまま）から表の行を作る（04 の 2.5）。
 * <p>
 * Builds a table row from the screen's editing fields (still text; 04 §2.5).
 */
public final class WeaponEntryForm {
    /**
     * 編集の欄の輪の数。
     * <p>
     * Number of rings in the editing fields.
     */
    public static final int RING_SLOTS = 3;

    private WeaponEntryForm() {}

    /**
     * 行にした結果。{@code errors} が空なら成功で、エラーなら {@code entry} は null。
     * <p>
     * Result of building a row. Success if {@code errors} is empty; {@code entry} is null on error.
     */
    public static final class FormResult {
        private final WeaponEntry entry;
        private final List<String> errors;

        FormResult(WeaponEntry entry, List<String> errors) {
            this.entry = entry;
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        }

        public WeaponEntry entry() {
            return entry;
        }

        public List<String> errors() {
            return errors;
        }

        public boolean ok() {
            return errors.isEmpty() && entry != null;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static int length(String s) {
        return s.codePointCount(0, s.length());
    }

    /**
     * @param ringNames 輪の名前。長さ 3（内側から）。null の要素は空と同じ ring names; length 3 (innermost first); null elements are equivalent to empty
     * @param ringRadii 輪の距離（m）の文字。長さ 3（内側から）。null の要素は空と同じ ring distances (m) as text; length 3 (innermost first); null elements are equivalent to empty
     */
    public static FormResult toEntry(String id, String label, String[] ringNames, String[] ringRadii, String note, Lang lang) {
        Objects.requireNonNull(lang, "lang");
        List<String> errors = new ArrayList<>();
        String l = nz(label).trim();
        if (l.isEmpty() || length(l) > WeaponTableCodec.MAX_LABEL_LENGTH) {
            errors.add(lang == Lang.JA ? "ラベル: 1 から " + WeaponTableCodec.MAX_LABEL_LENGTH + " 文字にしてください"
                        : "Label: use 1 to " + WeaponTableCodec.MAX_LABEL_LENGTH + " characters");
        }
        String n = nz(note).trim();
        if (length(n) > WeaponTableCodec.MAX_NOTE_LENGTH) {
            errors.add(lang == Lang.JA ? "メモ: " + WeaponTableCodec.MAX_NOTE_LENGTH + " 文字までにしてください"
                        : "Note: use up to " + WeaponTableCodec.MAX_NOTE_LENGTH + " characters");
        }

        List<WeaponRing> rings = new ArrayList<>();
        int previous = 0;
        int slots = Math.max(ringNames == null ? 0 : ringNames.length, ringRadii == null ? 0 : ringRadii.length);
        for (int i = 0; i < slots; i++) {
            int number = i + 1;
            String name = nz(ringNames != null && i < ringNames.length ? ringNames[i] : null).trim();
            String radiusText = nz(ringRadii != null && i < ringRadii.length ? ringRadii[i] : null).trim();
            if (name.isEmpty() && radiusText.isEmpty()) {
                continue; // 空の枠は捨てる / Discard empty slots.
            }
            if (name.isEmpty() || radiusText.isEmpty()) {
                errors.add(lang == Lang.JA ? "輪 " + number + ": 名前と距離の両方を入れてください"
                        : "Ring " + number + ": enter both the name and the distance");
                continue;
            }
            boolean bad = false;
            if (length(name) > WeaponTableCodec.MAX_RING_NAME_LENGTH) {
                errors.add(lang == Lang.JA ? "輪 " + number + ": 名前は " + WeaponTableCodec.MAX_RING_NAME_LENGTH + " 文字までにしてください"
                        : "Ring " + number + ": the name must be up to " + WeaponTableCodec.MAX_RING_NAME_LENGTH + " characters");
                bad = true;
            }
            int radius = parseRadius(radiusText);
            if (radius < 0) {
                errors.add(lang == Lang.JA ? "輪 " + number + ": 距離は " + WeaponTableCodec.MIN_RADIUS_M + " から "
                        + WeaponTableCodec.MAX_RADIUS_M + " までの整数（m）にしてください"
                        : "Ring " + number + ": the distance must be a whole number from " + WeaponTableCodec.MIN_RADIUS_M + " to "
                        + WeaponTableCodec.MAX_RADIUS_M + " (m)");
                bad = true;
            } else if (radius <= previous) {
                errors.add(lang == Lang.JA ? "輪 " + number + ": 距離は前の輪より大きくしてください"
                        : "Ring " + number + ": the distance must be larger than the previous ring");
                bad = true;
            }
            if (!bad) {
                rings.add(new WeaponRing(name, radius));
            }
            if (radius >= 0) {
                // 距離が数として読めた枠は、名前のエラーがあっても、次の枠の比べる相手にする（04 2.5 の 6）
                // If a slot's distance was parsed as a number, use it to compare with the next slot even if its name has an error (04 §2.5, item 6).
                previous = radius;
            }
        }
        if (errors.isEmpty() && rings.isEmpty()) {
            errors.add(lang == Lang.JA ? "輪を 1 つ以上入れてください"
                        : "Enter at least one ring");
        }
        if (!errors.isEmpty()) {
            return new FormResult(null, errors);
        }
        return new FormResult(new WeaponEntry(id, l, rings, n), errors);
    }

    /**
     * 半角の数字だけの 1〜50000 なら値、そうでなければ -1。
     * <p>
     * Returns the value for 1–50000 written with ASCII digits only; otherwise -1.
     */
    private static int parseRadius(String s) {
        if (s.length() > 6) {
            return -1;
        }
        int v = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        if (v < WeaponTableCodec.MIN_RADIUS_M || v > WeaponTableCodec.MAX_RADIUS_M) {
            return -1;
        }
        return v;
    }

    /**
     * {@code row-1}、{@code row-2}… のうち、表で使っていないいちばん小さい番号の id。
     * <p>
     * ID with the smallest unused number among {@code row-1}, {@code row-2}, etc. in the table.
     */
    public static String nextId(WeaponTable table) {
        Set<String> used = new HashSet<>();
        for (WeaponEntry e : table.entries()) {
            used.add(e.id());
        }
        int n = 1;
        while (used.contains("row-" + n)) {
            n++;
        }
        return "row-" + n;
    }
}
