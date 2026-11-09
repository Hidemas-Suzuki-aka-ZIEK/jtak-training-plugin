package jp.jtak.core.weapons;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jp.jtak.core.Lang;

/**
 * 兵器の表（weapons.json）の読み書きと検査（04 の 2.2・2.3）。
 * 値の決まりは {@link #parse} と {@link #validate} で同じ道筋を通る（同じ理由の文になる）。
 * <p>
 * Reads, writes, and validates the weapon table (weapons.json; 04 §§2.2, 2.3). {@link #parse} and {@link
 * #validate} use the same path for value rules (producing the same reason messages).
 */
public final class WeaponTableCodec {
    /**
     * ファイルにだけ書く注意書き。画面・円の説明には出さない（04 の 2.6）。
     * <p>
     * Notice written only to the file. Not shown on the screen or in circle descriptions (04 §2.6).
     */
    public static final String FILE_NOTICE =
            "TRAINING USE ONLY / 訓練用。訓練用サンプル値であり、実際の安全距離・危害半径ではない。";

    public static final int MAX_ENTRIES = 50;
    static final int MAX_RINGS = 3;
    static final int MAX_ERRORS = 20;
    static final int MAX_ID_LENGTH = 64;
    static final int MAX_LABEL_LENGTH = 40;
    static final int MAX_RING_NAME_LENGTH = 20;
    static final int MAX_NOTE_LENGTH = 200;
    static final int MIN_RADIUS_M = 1;
    static final int MAX_RADIUS_M = 50000;

    private WeaponTableCodec() {}

    /**
     * 読んで検査する。文法のエラーは 1 件、値のエラーは最大 20 件。
     * <p>
     * Reads and validates. Reports one syntax error or up to 20 value errors.
     */
    public static WeaponTableResult parse(byte[] utf8, Lang lang) {
        Objects.requireNonNull(lang, "lang");
        if (utf8 == null) {
            throw new IllegalArgumentException("utf8 is null");
        }
        Object root;
        try {
            root = WeaponJson.parse(utf8, lang);
        } catch (WeaponJson.JsonException e) {
            return new WeaponTableResult(null, Collections.singletonList(e.getMessage()));
        }
        return check(root, lang);
    }

    /**
     * 型の表を、読んだときと同じ値の決まりで検査する。空なら成功。
     * <p>
     * Validates the typed table using the same value rules as reading. An empty result means success.
     */
    public static List<String> validate(WeaponTable table, Lang lang) {
        Objects.requireNonNull(lang, "lang");
        if (table == null) {
            throw new IllegalArgumentException("table is null");
        }
        return check(toGeneric(table), lang).errors();
    }

    /**
     * 表を JSON の文字にする。
     * <p>
     * Converts the table to JSON text.
     */
    public static String write(WeaponTable table) {
        if (table == null) {
            throw new IllegalArgumentException("table is null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"schemaVersion\": 1,\n");
        sb.append("  \"notice\": ").append(WeaponJson.quote(FILE_NOTICE)).append(",\n");
        sb.append("  \"trainingSampleValues\": true,\n");
        sb.append("  \"entries\": [");
        List<WeaponEntry> entries = table.entries();
        for (int i = 0; i < entries.size(); i++) {
            WeaponEntry e = entries.get(i);
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("    {\n");
            sb.append("      \"id\": ").append(WeaponJson.quote(e.id())).append(",\n");
            sb.append("      \"label\": ").append(WeaponJson.quote(e.label())).append(",\n");
            sb.append("      \"rings\": [");
            List<WeaponRing> rings = e.rings();
            for (int j = 0; j < rings.size(); j++) {
                WeaponRing r = rings.get(j);
                sb.append(j == 0 ? "\n" : ",\n");
                sb.append("        {\n");
                sb.append("          \"name\": ").append(WeaponJson.quote(r.name())).append(",\n");
                sb.append("          \"radiusM\": ").append(r.radiusM()).append('\n');
                sb.append("        }");
            }
            sb.append(rings.isEmpty() ? "]" : "\n      ]");
            if (!e.note().isEmpty()) {
                sb.append(",\n      \"note\": ").append(WeaponJson.quote(e.note()));
            }
            sb.append("\n    }");
        }
        sb.append(entries.isEmpty() ? "]\n" : "\n  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ---- 型の表を、読んだ値と同じ形（Map・List・String・JsonNumber）にする ----
    // ---- Convert the typed table to the same representation as parsed values (Map, List, String, JsonNumber) ----

    private static Object toGeneric(WeaponTable table) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", new WeaponJson.JsonNumber("1"));
        root.put("notice", FILE_NOTICE);
        root.put("trainingSampleValues", Boolean.TRUE);
        List<Object> entries = new ArrayList<>();
        for (WeaponEntry e : table.entries()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.id());
            m.put("label", e.label());
            List<Object> rings = new ArrayList<>();
            for (WeaponRing r : e.rings()) {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("name", r.name());
                rm.put("radiusM", new WeaponJson.JsonNumber(Integer.toString(r.radiusM())));
                rings.add(rm);
            }
            m.put("rings", rings);
            if (!e.note().isEmpty()) {
                m.put("note", e.note());
            }
            entries.add(m);
        }
        root.put("entries", entries);
        return root;
    }

    // ---- 形と値の検査（04 の 2.3） ----
    // ---- Structure and value validation (04 §2.3) ----

    private static final class Errors {
        private final List<String> list = new ArrayList<>();
        private final Lang lang;

        Errors(Lang lang) {
            this.lang = lang;
        }

        String text(String ja, String en) {
            return lang == Lang.JA ? ja : en;
        }

        void add(String path, String reason) {
            if (list.size() < MAX_ERRORS) {
                list.add(path.isEmpty() ? reason : path + ": " + reason);
            }
        }

        boolean isEmpty() {
            return list.isEmpty();
        }
    }

    private static String child(String path, String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    private static WeaponTableResult check(Object root, Lang lang) {
        Errors errors = new Errors(lang);
        List<WeaponEntry> entries = new ArrayList<>();
        if (!(root instanceof Map)) {
            errors.add("", errors.text("全体はオブジェクトにしてください", "the whole file must be an object"));
            return new WeaponTableResult(null, errors.list);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) root;
        checkKeys(obj, "", new String[] {"schemaVersion", "notice", "trainingSampleValues", "entries"}, errors);

        // schemaVersion
        Object sv = obj.get("schemaVersion");
        if (!obj.containsKey("schemaVersion")) {
            errors.add("schemaVersion", errors.text("ありません", "missing"));
        } else if (!(sv instanceof WeaponJson.JsonNumber)
                || !((WeaponJson.JsonNumber) sv).isInteger()
                || ((WeaponJson.JsonNumber) sv).value().compareTo(BigDecimal.ONE) != 0) {
            errors.add("schemaVersion", errors.text("1 にしてください", "must be 1"));
        }

        // notice
        Object notice = obj.get("notice");
        if (!obj.containsKey("notice")) {
            errors.add("notice", errors.text("ありません", "missing"));
        } else if (!(notice instanceof String) || ((String) notice).trim().isEmpty()) {
            errors.add("notice", errors.text("空でない文字列にしてください", "must be a non-empty string"));
        }

        // trainingSampleValues
        if (!obj.containsKey("trainingSampleValues")) {
            errors.add("trainingSampleValues", errors.text("ありません", "missing"));
        } else if (!Boolean.TRUE.equals(obj.get("trainingSampleValues"))) {
            errors.add("trainingSampleValues", errors.text("true にしてください", "must be true"));
        }

        // entries
        if (!obj.containsKey("entries")) {
            errors.add("entries", errors.text("ありません", "missing"));
        } else if (!(obj.get("entries") instanceof List)) {
            errors.add("entries", errors.text("配列にしてください", "must be an array"));
        } else {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) obj.get("entries");
            if (list.size() < 1 || list.size() > MAX_ENTRIES) {
                errors.add("entries", errors.text("1 行から " + MAX_ENTRIES + " 行までにしてください（いまは " + list.size() + " 行）", "must have 1 to " + MAX_ENTRIES + " rows (now " + list.size() + ")"));
            }
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                WeaponEntry e = checkEntry(list.get(i), "entries[" + i + "]", ids, errors);
                if (e != null) {
                    entries.add(e);
                }
            }
        }

        if (!errors.isEmpty()) {
            return new WeaponTableResult(null, errors.list);
        }
        return new WeaponTableResult(new WeaponTable(entries), Collections.<String>emptyList());
    }

    private static void checkKeys(Map<String, Object> obj, String path, String[] known, Errors errors) {
        for (String key : obj.keySet()) {
            boolean ok = false;
            for (String k : known) {
                if (k.equals(key)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                errors.add(child(path, key), errors.text("知らないキーです", "unknown key"));
            }
        }
    }

    private static int length(String s) {
        return s.codePointCount(0, s.length());
    }

    private static WeaponEntry checkEntry(Object value, String path, Set<String> ids, Errors errors) {
        if (!(value instanceof Map)) {
            errors.add(path, errors.text("オブジェクトにしてください", "must be an object"));
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) value;
        boolean ok = true;
        checkKeys(obj, path, new String[] {"id", "label", "rings", "note"}, errors);
        if (!isKnownOnly(obj, new String[] {"id", "label", "rings", "note"})) {
            ok = false;
        }

        // id
        String id = null;
        String idPath = child(path, "id");
        if (!obj.containsKey("id")) {
            errors.add(idPath, errors.text("ありません", "missing"));
            ok = false;
        } else if (!(obj.get("id") instanceof String)) {
            errors.add(idPath, errors.text("文字列にしてください", "must be a string"));
            ok = false;
        } else {
            id = (String) obj.get("id");
            if (id.isEmpty()) {
                errors.add(idPath, errors.text("空にできません", "must not be empty"));
                ok = false;
            } else if (length(id) > MAX_ID_LENGTH) {
                errors.add(idPath, errors.text(MAX_ID_LENGTH + " 文字までにしてください", "must be up to " + MAX_ID_LENGTH + " characters"));
                ok = false;
            } else if (!isValidId(id)) {
                errors.add(idPath, errors.text("英小文字・数字・- だけにしてください", "use only lowercase letters, digits and -"));
                ok = false;
            } else if (!ids.add(id)) {
                errors.add(idPath, errors.text("ほかの行と同じ id です（" + id + "）", "same id as another row (" + id + ")"));
                ok = false;
            }
        }

        // label
        String label = checkText(obj, path, "label", MAX_LABEL_LENGTH, true, errors);
        if (label == null) {
            ok = false;
        }

        // rings
        List<WeaponRing> rings = null;
        String ringsPath = child(path, "rings");
        if (!obj.containsKey("rings")) {
            errors.add(ringsPath, errors.text("ありません", "missing"));
            ok = false;
        } else if (!(obj.get("rings") instanceof List)) {
            errors.add(ringsPath, errors.text("配列にしてください", "must be an array"));
            ok = false;
        } else {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) obj.get("rings");
            if (list.size() < 1 || list.size() > MAX_RINGS) {
                errors.add(ringsPath, errors.text("1 個から " + MAX_RINGS + " 個までにしてください（いまは " + list.size() + " 個）", "must have 1 to " + MAX_RINGS + " items (now " + list.size() + ")"));
                ok = false;
            }
            rings = new ArrayList<>();
            int previous = 0;
            for (int j = 0; j < list.size(); j++) {
                WeaponRing r = checkRing(list.get(j), ringsPath + "[" + j + "]", previous, errors);
                if (r == null) {
                    ok = false;
                } else {
                    previous = r.radiusM();
                    rings.add(r);
                }
            }
        }

        // note
        String note = "";
        if (obj.containsKey("note")) {
            String n = checkText(obj, path, "note", MAX_NOTE_LENGTH, false, errors);
            if (n == null) {
                ok = false;
            } else {
                note = n;
            }
        }

        if (!ok || id == null || label == null || rings == null) {
            return null;
        }
        return new WeaponEntry(id, label, rings, note);
    }

    private static boolean isKnownOnly(Map<String, Object> obj, String[] known) {
        for (String key : obj.keySet()) {
            boolean found = false;
            for (String k : known) {
                if (k.equals(key)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidId(String id) {
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static WeaponRing checkRing(Object value, String path, int previous, Errors errors) {
        if (!(value instanceof Map)) {
            errors.add(path, errors.text("オブジェクトにしてください", "must be an object"));
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) value;
        String[] known = {"name", "radiusM"};
        checkKeys(obj, path, known, errors);
        boolean ok = isKnownOnly(obj, known);

        String name = checkText(obj, path, "name", MAX_RING_NAME_LENGTH, true, errors);
        if (name == null) {
            ok = false;
        }

        int radius = 0;
        String rPath = child(path, "radiusM");
        if (!obj.containsKey("radiusM")) {
            errors.add(rPath, errors.text("ありません", "missing"));
            ok = false;
        } else {
            Object r = obj.get("radiusM");
            BigDecimal min = BigDecimal.valueOf(MIN_RADIUS_M);
            BigDecimal max = BigDecimal.valueOf(MAX_RADIUS_M);
            if (!(r instanceof WeaponJson.JsonNumber)
                    || !((WeaponJson.JsonNumber) r).isInteger()
                    || ((WeaponJson.JsonNumber) r).value().compareTo(min) < 0
                    || ((WeaponJson.JsonNumber) r).value().compareTo(max) > 0) {
                errors.add(rPath, errors.text(MIN_RADIUS_M + " から " + MAX_RADIUS_M + " までの整数にしてください", "must be an integer from " + MIN_RADIUS_M + " to " + MAX_RADIUS_M));
                ok = false;
            } else {
                radius = ((WeaponJson.JsonNumber) r).value().intValue();
                if (radius <= previous) {
                    errors.add(rPath, errors.text("前の輪より大きい値にしてください", "must be larger than the previous ring"));
                    ok = false;
                }
            }
        }
        return ok ? new WeaponRing(name, radius) : null;
    }

    /**
     * 文字列の欄を調べる。前後の空白を取った値を返す。誤りなら null（理由は errors に入れる）。
     * <p>
     * Checks a string field. Returns the trimmed value, or null on error (reasons are added to errors).
     */
    private static String checkText(
            Map<String, Object> obj, String path, String key, int maxLength, boolean required, Errors errors) {
        String p = child(path, key);
        if (!obj.containsKey(key)) {
            errors.add(p, errors.text("ありません", "missing"));
            return null;
        }
        Object v = obj.get(key);
        if (!(v instanceof String)) {
            errors.add(p, errors.text("文字列にしてください", "must be a string"));
            return null;
        }
        String s = ((String) v).trim();
        if (required && s.isEmpty()) {
            errors.add(p, errors.text("空にできません", "must not be empty"));
            return null;
        }
        if (length(s) > maxLength) {
            errors.add(p, errors.text(maxLength + " 文字までにしてください（いまは " + length(s) + " 文字）", "must be up to " + maxLength + " characters (now " + length(s) + ")"));
            return null;
        }
        return s;
    }
}
