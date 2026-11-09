package jp.jtak.core.importer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import jp.jtak.core.Lang;

/**
 * JTAK が書き出したテキストを読み、4 つの画面の入力欄に入れる値にする（設計 06 の 2）。
 * 読むのは利用者が置いた端末内のテキストだけ。ファイルの中身はログに出さない。
 * <p>
 * Reads JTAK-exported text and converts it to values for the input fields on the four screens (design 06 §2).
 * Reads only local text files placed by the user on the device. Never logs file contents.
 */
public final class ScenarioImporter {
    /**
     * 読むファイルの大きさの上限（バイト）。
     * <p>
     * Maximum size of the file to read (bytes).
     */
    public static final int MAX_BYTES = 262_144;

    private static final String TITLE_ROUTING = "ROUTING (JP 3-09.3 2014 Fig. V-3)";
    private static final String TITLE_CHECK_IN = "CHECK-IN (JP 3-09.3 2014 Fig. V-4)";
    private static final String TITLE_GAME_PLAN = "GAME PLAN (JP 3-09.3 2014 Fig. V-9)";
    private static final String TITLE_CAS_BRIEF = "CAS 9-LINE (JP 3-09.3 2014 Fig. V-9) / Datum: WGS 84";

    private ScenarioImporter() {}

    /**
     * 入力を最大 {@link #MAX_BYTES}+1 バイトまで読み、{@link #parse} に渡す。入力は閉じない。
     * <p>
     * Reads up to {@link #MAX_BYTES}+1 bytes from the input and passes them to {@link #parse}. Does not close the
     * input.
     *
     * @throws IOException 入力を読めなかったとき if the input cannot be read
     */
    public static ScenarioImportResult read(InputStream in, ScenarioKind expected, Lang lang) throws IOException {
        Objects.requireNonNull(lang, "lang");
        if (in == null || expected == null) {
            throw new IllegalArgumentException("in と expected は必須");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        while (total <= MAX_BYTES) {
            int want = Math.min(buf.length, MAX_BYTES + 1 - total);
            int n = in.read(buf, 0, want);
            if (n < 0) {
                break;
            }
            out.write(buf, 0, n);
            total += n;
        }
        return parse(out.toByteArray(), expected, lang);
    }

    /**
     * バイト列を読む。読み込めないときは {@code error} に理由を入れて返す（例外にしない）。
     * <p>
     * Reads a byte sequence. If import is impossible, returns the reason in {@code error} (without throwing an
     * exception).
     */
    public static ScenarioImportResult parse(byte[] data, ScenarioKind expected, Lang lang) {
        Objects.requireNonNull(lang, "lang");
        if (data == null || expected == null) {
            throw new IllegalArgumentException("data と expected は必須");
        }
        if (data.length > MAX_BYTES) {
            return fail(null, ScenarioImportError.Code.TOO_LARGE, lang, null);
        }
        String text;
        try {
            text = ScenarioLines.decode(data);
        } catch (CharacterCodingException e) {
            return fail(null, ScenarioImportError.Code.NOT_UTF8, lang, null);
        }
        List<ScenarioLines.Line> lines = ScenarioLines.split(text);
        if (lines.isEmpty()) {
            return fail(null, ScenarioImportError.Code.EMPTY, lang, null);
        }
        ScenarioKind actual = kindOfTitle(lines.get(0).text());
        if (actual == null) {
            return fail(null, ScenarioImportError.Code.UNKNOWN_TITLE, lang, null);
        }
        if (actual != expected) {
            return fail(actual, ScenarioImportError.Code.WRONG_KIND, lang, actual);
        }
        List<ScenarioLines.Line> body = lines.subList(1, lines.size());
        ImportLog log = new ImportLog(lang);
        ScenarioImportResult result = parseBody(actual, body, log);
        if (result == null) {
            return fail(actual, ScenarioImportError.Code.NOTHING_READ, lang, null);
        }
        return result;
    }

    private static ScenarioImportResult parseBody(
            ScenarioKind kind, List<ScenarioLines.Line> body, ImportLog log) {
        switch (kind) {
            case ROUTING: {
                RoutingImport dto = RoutingParser.parse(body, log);
                return dto == null ? null : ScenarioImportResult.routing(dto, log.items());
            }
            case CHECK_IN: {
                CheckInImport dto = CheckInParser.parse(body, log);
                return dto == null ? null : ScenarioImportResult.checkIn(dto, log.items());
            }
            case GAME_PLAN: {
                GamePlanImport dto = GamePlanParser.parse(body, log);
                return dto == null ? null : ScenarioImportResult.gamePlan(dto, log.items());
            }
            default: {
                CasBriefImport dto = CasBriefParser.parse(body, log);
                return dto == null ? null : ScenarioImportResult.casBrief(dto, log.items());
            }
        }
    }

    private static ScenarioKind kindOfTitle(String title) {
        if (TITLE_ROUTING.equals(title)) {
            return ScenarioKind.ROUTING;
        }
        if (TITLE_CHECK_IN.equals(title)) {
            return ScenarioKind.CHECK_IN;
        }
        if (TITLE_GAME_PLAN.equals(title)) {
            return ScenarioKind.GAME_PLAN;
        }
        if (TITLE_CAS_BRIEF.equals(title)) {
            return ScenarioKind.CAS_BRIEF;
        }
        return null;
    }

    private static ScenarioImportResult fail(
            ScenarioKind kind, ScenarioImportError.Code code, Lang lang, ScenarioKind actual) {
        ScenarioImportError error = new ScenarioImportError(code, message(code, lang, actual), actual);
        return ScenarioImportResult.failure(kind, error, Collections.<String>emptyList());
    }

    private static String message(ScenarioImportError.Code code, Lang lang, ScenarioKind actual) {
        boolean ja = lang == Lang.JA;
        switch (code) {
            case TOO_LARGE:
                return ja ? "ファイルが大きすぎます（256 KiB まで）。" : "The file is too large (up to 256 KiB).";
            case NOT_UTF8:
                return ja ? "UTF-8 のテキストとして読めません。" : "Cannot read the file as UTF-8 text.";
            case EMPTY:
                return ja ? "ファイルに何も書かれていません。" : "The file is empty.";
            case UNKNOWN_TITLE:
                return ja
                        ? "題名の行が JTAK の書式ではありません。1 行目（TRAINING USE ONLY の行を除く）に書式の題名を書いてください。"
                        : "The title line is not a JTAK format. Write the format title on the first line "
                                + "(excluding the TRAINING USE ONLY line).";
            case WRONG_KIND: {
                String k = screenName(actual, lang);
                return ja
                        ? "このファイルは " + k + " の書式です。" + k + " の画面で読み込んでください。"
                        : "This file is in the " + k + " format. Load it on the " + k + " screen.";
            }
            default:
                return ja ? "読み込める項目がありませんでした。" : "No items could be read.";
        }
    }

    private static String screenName(ScenarioKind kind, Lang lang) {
        boolean ja = lang == Lang.JA;
        switch (kind) {
            case ROUTING:
                return ja ? "ルーティング" : "Routing";
            case CHECK_IN:
                return ja ? "チェックイン" : "Check-in";
            case GAME_PLAN:
                return ja ? "ゲームプラン" : "Game plan";
            default:
                return "CAS Brief";
        }
    }
}
