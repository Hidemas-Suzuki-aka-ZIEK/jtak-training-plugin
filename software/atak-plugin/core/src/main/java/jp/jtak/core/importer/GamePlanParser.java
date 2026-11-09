package jp.jtak.core.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.jtak.core.cas.MethodOfAttack;
import jp.jtak.core.cas.TypeOfControl;

/**
 * ゲームプランの行から {@link GamePlanImport} を作る（設計 06 の 2.4.3）。
 * <p>
 * Builds {@link GamePlanImport} from game plan lines (design 06 §2.4.3).
 */
final class GamePlanParser {
    private static final String CALLSIGN = "Callsign";
    private static final String TYPE = "Type of control";
    private static final String METHOD = "Method of attack";
    /**
     * 1 行目の終わり。頭の 1 文字（A / a）だけ大小を問わず、残りは formatter の出力のとおりに合わせる。
     * <p>
     * End of the first line. Only the initial character (A / a) is case-insensitive; the rest must match formatter
     * output exactly.
     */
    private static final String HEAD_TAIL = "dvise when ready for game plan.";
    private static final String BODY_END = "; advise when ready for 9-line.";
    private static final Pattern LINE6 = Pattern.compile("^(.*), line 6 (.+) digits$");

    /**
     * 任意の項目の前置き（探す順）と知らせる名前。
     * <p>
     * Prefixes for optional items (search order) and their report names.
     */
    private static final String[][] EXTRAS = {
        {", intent ", "Intent"},
        {", CDE concerns ", "CDE concerns"},
        {", ordnance ", "Ordnance"},
        {", effects ", "Effects"},
        {", fuzing ", "Fuzing"},
        {", interval ", "Interval"},
    };

    private GamePlanParser() {}

    /**
     * 題名のあとの行を読む。読めた項目がなければ null。
     * <p>
     * Reads lines after the title. Returns null if no items could be read.
     */
    static GamePlanImport parse(List<ScenarioLines.Line> lines, ImportLog log) {
        GamePlanImport.Builder b = new GamePlanImport.Builder();
        String headCallsign = null;
        String bodyCallsign = null;
        boolean headSeen = false;
        boolean bodySeen = false;
        boolean decided = false;
        for (ScenarioLines.Line line : lines) {
            String t = line.text();
            if (t.endsWith(BODY_END)) {
                if (bodySeen) {
                    log.duplicate(TYPE);
                    continue;
                }
                bodySeen = true;
                bodyCallsign = readBody(t.substring(0, t.length() - BODY_END.length()), b, log);
            } else if (isHead(t)) {
                if (headSeen) {
                    log.duplicate(CALLSIGN);
                    continue;
                }
                headSeen = true;
                headCallsign = readHead(t.substring(0, t.length() - HEAD_TAIL.length() - 1), log);
            } else {
                log.unknownLine(line.number());
                continue;
            }
            // 呼出符号は 1 行目と本文の 2 か所に出る。そろった時点で決める。
            // The callsign occurs in the first line and the body. Finalize it once both agree.
            if (!decided && headSeen && bodySeen) {
                decided = true;
                if (headCallsign != null && bodyCallsign != null && !headCallsign.equals(bodyCallsign)) {
                    log.differs(CALLSIGN);
                }
            }
        }
        String callsign = headCallsign != null ? headCallsign : bodyCallsign;
        if (callsign != null) {
            b.callsign = callsign;
            log.hit();
        }
        if (!bodySeen) {
            log.missing(TYPE);
            log.missing(METHOD);
        }
        return log.anyRead() ? b.build() : null;
    }

    private static boolean isHead(String t) {
        int cut = t.length() - HEAD_TAIL.length() - 1;
        if (cut < 0 || !t.endsWith(HEAD_TAIL)) {
            return false;
        }
        char first = t.charAt(cut);
        return first == 'a' || first == 'A';
    }

    /**
     * 1 行目の「{呼出符号}, 」を取り出す。なければ null。
     * <p>
     * Extracts “{callsign}, ” from the first line. Returns null if absent.
     */
    private static String readHead(String before, ImportLog log) {
        String v = ScenarioLines.clean(before);
        if (v != null && v.endsWith(",")) {
            v = ScenarioLines.clean(v.substring(0, v.length() - 1));
        }
        if (v == null) {
            return null;
        }
        if (ScenarioLines.NOT_ENTERED.equals(v)) {
            log.notEntered(CALLSIGN);
            return null;
        }
        return v;
    }

    /**
     * 本文を読み、本文にあった呼出符号（なければ null）を返す。
     * <p>
     * Reads the body and returns its callsign (null if absent).
     */
    private static String readBody(String rest, GamePlanImport.Builder b, ImportLog log) {
        String remainder = rest;
        Matcher m = LINE6.matcher(remainder);
        String line6 = null;
        boolean hasLine6 = false;
        if (m.matches()) {
            remainder = m.group(1);
            line6 = m.group(2);
            hasLine6 = true;
        }

        // 任意の前置きを順に探す。前に見つけた位置より後ろだけを探す。
        // Search optional prefixes in order, looking only after the previous match.
        List<Integer> starts = new ArrayList<>();
        List<Integer> index = new ArrayList<>();
        int from = 0;
        for (int i = 0; i < EXTRAS.length; i++) {
            int at = remainder.indexOf(EXTRAS[i][0], from);
            if (at >= 0) {
                starts.add(at);
                index.add(i);
                from = at + EXTRAS[i][0].length();
            }
        }
        String head = starts.isEmpty() ? remainder : remainder.substring(0, starts.get(0));
        String callsign = readHeadPart(head, b, log);

        for (int k = 0; k < starts.size(); k++) {
            int i = index.get(k);
            int valueStart = starts.get(k) + EXTRAS[i][0].length();
            int valueEnd = k + 1 < starts.size() ? starts.get(k + 1) : remainder.length();
            String v = log.entered(EXTRAS[i][1], remainder.substring(valueStart, valueEnd));
            assign(b, i, v);
        }
        if (hasLine6) {
            b.line6Digits = log.entered("Line 6 digits", line6);
        }
        return callsign;
    }

    private static void assign(GamePlanImport.Builder b, int i, String v) {
        switch (i) {
            case 0 -> b.intent = v;
            case 1 -> b.collateralConcerns = v;
            case 2 -> b.ordnance = v;
            case 3 -> b.desiredEffects = v;
            case 4 -> b.fuzing = v;
            default -> b.interval = v;
        }
    }

    /**
     * 本文の頭（呼出符号・型・方法）を読む。本文にあった呼出符号を返す。
     * <p>
     * Reads the start of the body (callsign, type, and method). Returns the callsign found in the body.
     */
    private static String readHeadPart(String head, GamePlanImport.Builder b, ImportLog log) {
        String[] parts = head.split(", ", -1);
        if (parts.length < 2) {
            log.unreadable(TYPE);
            log.unreadable(METHOD);
            log.claim(TYPE);
            log.claim(METHOD);
            return null;
        }
        String type = ScenarioLines.clean(parts[parts.length - 2]);
        String method = ScenarioLines.clean(parts[parts.length - 1]);
        log.claim(TYPE);
        log.claim(METHOD);
        b.typeOfControl = readType(type, log);
        b.methodOfAttack = readMethod(method, log);
        if (parts.length == 2) {
            return null;
        }
        String callsign = ScenarioLines.clean(String.join(", ", Arrays.copyOf(parts, parts.length - 2)));
        return callsign == null || ScenarioLines.NOT_ENTERED.equals(callsign) ? null : callsign;
    }

    private static TypeOfControl readType(String v, ImportLog log) {
        if (v == null || ScenarioLines.NOT_ENTERED.equals(v)) {
            log.notEntered(TYPE);
            return null;
        }
        TypeOfControl r = switch (v) {
            case "Type 1" -> TypeOfControl.TYPE_1;
            case "Type 2" -> TypeOfControl.TYPE_2;
            case "Type 3" -> TypeOfControl.TYPE_3;
            default -> null;
        };
        if (r == null) {
            log.unreadable(TYPE);
        } else {
            log.hit();
        }
        return r;
    }

    private static MethodOfAttack readMethod(String v, ImportLog log) {
        if (v == null || ScenarioLines.NOT_ENTERED.equals(v)) {
            log.notEntered(METHOD);
            return null;
        }
        MethodOfAttack r = switch (v) {
            case "bomb on target" -> MethodOfAttack.BOT;
            case "bomb on coordinate" -> MethodOfAttack.BOC;
            default -> null;
        };
        if (r == null) {
            log.unreadable(METHOD);
        } else {
            log.hit();
        }
        return r;
    }
}
