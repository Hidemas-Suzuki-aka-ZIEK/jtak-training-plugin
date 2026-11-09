package jp.jtak.core.cas;

/**
 * 9-line の 5 行目（Target description）の下書き（02 2.3.1）。
 * 目標に選んだ点の種類（CoT の type）から、下書きの言葉を返す。
 *
 * <p>下書きを入れるのは敵（{@code a-h-}）の陸上（{@code G}）の点だけ。言葉は公開の MIL-STD-2525E
 * w/Change 1 の陸上の部隊（TABLE A-XXIII）と装備（TABLE A-XXIX）の名前から作った表で決める
 * （出典は docs/references.md S6・S7。訓練用。docs/usage-scope.md Q8）。数・姿勢・色などは利用者が書き足す。
 * 返すのは言葉だけで、TrainingNotice の文言は付けない（U1 は CasBriefFormatter 側で守る）。
 * <p>
 * Draft for line 5 of the 9-line (Target description; 02 §2.3.1). Returns draft wording based on the
 * selected target point's type (CoT type). <p> Drafts only hostile ({@code a-h-}) ground ({@code G}) points.
 * Wording is determined by a table based on ground unit names (TABLE A-XXIII) and equipment names (TABLE A-XXIX)
 * in the public MIL-STD-2525E w/Change 1 (sources: docs/references.md S6 and S7; training use only;
 * docs/usage-scope.md Q8). Users add quantities, posture, color, etc. Returns only the wording without
 * TrainingNotice text (CasBriefFormatter enforces U1).
 */
public final class TargetDescriptionDraft {
    /**
     * {CoT の type の頭, 下書きの言葉}。02 2.3.1 の表の 27 行。
     * <p>
     * {CoT type prefix, draft wording}. The 27 rows of the table in 02 §2.3.1.
     */
    private static final String[][] TABLE = {
        // 陸上の部隊（E Change 1 の TABLE A-XXIII）
        // Ground units (E Change 1, TABLE A-XXIII)
        {"a-h-G-U-C", "Combat"},
        {"a-h-G-U-C-I", "Infantry"},
        {"a-h-G-U-C-A", "Armor/Mechanized"},
        {"a-h-G-U-C-A-A", "Antitank/Antiarmor"},
        {"a-h-G-U-C-R", "Reconnaissance/Cavalry/Scout"},
        {"a-h-G-U-C-D", "Air Defense"},
        {"a-h-G-U-C-F", "Field Artillery"},
        {"a-h-G-U-C-F-M", "Mortar"},
        {"a-h-G-U-C-E", "Engineer"},
        // 陸上の装備（E Change 1 の TABLE A-XXIX）
        // Ground equipment (E Change 1, TABLE A-XXIX)
        {"a-h-G-E-W", "Weapon/Weapon System"},
        {"a-h-G-E-W-R-R", "Rifle"},
        {"a-h-G-E-W-R-L", "Machine Gun"},
        {"a-h-G-E-W-R-H", "Machine Gun"},
        {"a-h-G-E-W-A", "Air Defense Gun"},
        {"a-h-G-E-W-G", "Antitank Gun"},
        {"a-h-G-E-W-H", "Howitzer"},
        {"a-h-G-E-W-M", "Missile Launcher"},
        {"a-h-G-E-W-M-T", "Antitank Missile Launcher"},
        {"a-h-G-E-W-O", "Mortar"},
        {"a-h-G-E-W-X", "Multiple Rocket Launcher"},
        {"a-h-G-E-V", "Vehicle"},
        {"a-h-G-E-V-A", "Armored"},
        {"a-h-G-E-V-A-A", "Armored Personnel Carrier"},
        {"a-h-G-E-V-A-T", "Tank"},
        {"a-h-G-E-V-E", "Engineer Vehicle and Equipment"},
        {"a-h-G-E-V-U", "Utility Vehicle"},
        {"a-h-G-E-V-C", "Civilian Vehicle"},
    };

    private TargetDescriptionDraft() {}

    /**
     * 表の行の数。試験で 27 行から増えていないことを確かめるために使う（パッケージ内だけ）。
     * <p>
     * Number of table rows. Used by tests to verify that the table has not grown beyond 27 rows (package-private).
     */
    static int rowCount() {
        return TABLE.length;
    }

    /**
     * CoT の type から下書きの言葉を返す。前後の空白を取り、{@code -} で区切った字の並びが先頭から
     * すべて同じになる表の行のうち、いちばん長い行の言葉を返す（字の途中では当てない。
     * 大文字と小文字は区別する）。合う行がないとき（null・空・空白だけ・味方や中立や不明・陸上でない・
     * 表にない種類）は null を返す。
     * <p>
     * Returns draft wording for a CoT type. Trims leading/trailing whitespace and returns the wording of the
     * longest table row whose {@code -}-separated segments all match from the beginning (no partial-character
     * matches; case-sensitive). Returns null if no row matches (null, empty, whitespace-only, friendly, neutral,
     * unknown, non-ground, or unlisted types).
     */
    public static String fromCotType(String cotType) {
        String type = trim(cotType);
        if (type == null) {
            return null;
        }
        String[] segments = type.split("-", -1);
        String best = null;
        int bestLength = -1;
        for (String[] row : TABLE) {
            String[] head = row[0].split("-", -1);
            if (head.length > bestLength && startsWith(segments, head)) {
                best = row[1];
                bestLength = head.length;
            }
        }
        return best;
    }

    /**
     * segments の先頭が head と字ごとにすべて同じか。
     * <p>
     * Whether the beginning of segments matches head character for character.
     */
    private static boolean startsWith(String[] segments, String[] head) {
        if (head.length > segments.length) {
            return false;
        }
        for (int i = 0; i < head.length; i++) {
            if (!head[i].equals(segments[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 前後の空白（全角空白を含む）を取る。null か、空になったら null。
     * <p>
     * Trims leading/trailing whitespace (including full-width spaces). Returns null for null input or an empty
     * result.
     */
    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        int start = 0;
        int end = s.length();
        while (start < end && Character.isWhitespace(s.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return start == end ? null : s.substring(start, end);
    }
}
