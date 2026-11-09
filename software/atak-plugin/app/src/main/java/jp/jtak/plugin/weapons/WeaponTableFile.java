package jp.jtak.plugin.weapons;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import jp.jtak.core.Lang;
import jp.jtak.core.weapons.DefaultWeaponTable;
import jp.jtak.core.weapons.WeaponTable;
import jp.jtak.core.weapons.WeaponTableCodec;
import jp.jtak.core.weapons.WeaponTableResult;

/**
 * 兵器の表のファイルを読み書きする（04 3.5）。
 * 読むときは、ファイルがなければ既定の表を使い、ファイルは作らない。
 * 正しくないファイルは書き換えず、既定の表と理由を返す。
 * 書くときは、一時ファイルに書いてから名前を変える。中身は {@link WeaponTableCodec#write} の出力だけ。
 * <p>
 * Reads and writes the weapon table file (04 §3.5). When reading, uses the default table if no file exists and
 * does not create a file. Leaves invalid files unchanged and returns the default table with the reason. When
 * writing, writes to a temporary file, then renames it. Contents are solely the output of {@link
 * WeaponTableCodec#write}.
 */
final class WeaponTableFile {
    /**
     * ATAK のフォルダ（{@code FileSystemUtils.getItem} の基準）からの相対の道筋。
     * <p>
     * Path relative to ATAK's directory (the base for {@code FileSystemUtils.getItem}).
     */
    static final String RELATIVE_PATH = "tools/jtak/weapons.json";

    /**
     * 読めるファイルの大きさの上限。これを超えるものは正しくないファイルとして扱う。
     * <p>
     * Maximum readable file size. Larger files are treated as invalid.
     */
    private static final long MAX_FILE_BYTES = 1024 * 1024;

    /**
     * 表の出どころ。
     * <p>
     * Table source.
     */
    enum Source {
        /**
         * ファイルがないので、既定の表。
         * <p>
         * Default table because the file is absent.
         */
        DEFAULT,
        /**
         * ファイルの表。
         * <p>
         * Table from the file.
         */
        FILE,
        /**
         * ファイルが正しくないので、既定の表。理由は {@link Loaded#errors()}。
         * <p>
         * Default table because the file is invalid. See {@link Loaded#errors()} for reasons.
         */
        INVALID_USING_DEFAULT
    }

    /**
     * 読んだ結果。
     * <p>
     * Read result.
     */
    static final class Loaded {
        private final WeaponTable table;
        private final Source source;
        private final List<String> errors;
        private final WeaponTableFileException problem;

        Loaded(WeaponTable table, Source source, List<String> errors) {
            this(table, source, errors, null);
        }

        Loaded(WeaponTable table, Source source, List<String> errors, WeaponTableFileException problem) {
            this.table = table;
            this.source = source;
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
            this.problem = problem;
        }

        WeaponTable table() {
            return table;
        }

        Source source() {
            return source;
        }

        /**
         * 検査の理由（core が出す文）。ファイルを読めなかったときは空で、{@link #problem()} に原因が入る。
         * <p>
         * Validation reasons (text from core). Empty if the file could not be read; {@link #problem()} contains
         * the cause.
         */
        List<String> errors() {
            return errors;
        }

        /**
         * ファイルを読めなかった原因。検査に通らなかったとき・読めたときは null。
         * <p>
         * Cause of a file read failure. Null after failed validation or a successful read.
         */
        WeaponTableFileException problem() {
            return problem;
        }
    }

    private final File file;
    private final Lang lang;

    WeaponTableFile(File file, Lang lang) {
        this.file = file;
        this.lang = lang;
    }

    File file() {
        return file;
    }

    /**
     * 表を読む。ファイルは変えない。
     * <p>
     * Reads the table without changing the file.
     */
    Loaded read() {
        if (!file.exists()) {
            return new Loaded(DefaultWeaponTable.table(lang), Source.DEFAULT, Collections.<String>emptyList());
        }
        byte[] bytes;
        try {
            bytes = readBytes(file);
        } catch (WeaponTableFileException e) {
            return invalid(e);
        }
        WeaponTableResult result = WeaponTableCodec.parse(bytes, lang);
        if (result.ok()) {
            return new Loaded(result.table(), Source.FILE, Collections.<String>emptyList());
        }
        return new Loaded(DefaultWeaponTable.table(lang), Source.INVALID_USING_DEFAULT, result.errors());
    }

    private Loaded invalid(WeaponTableFileException problem) {
        return new Loaded(DefaultWeaponTable.table(lang), Source.INVALID_USING_DEFAULT,
                Collections.<String>emptyList(), problem);
    }

    private static byte[] readBytes(File f) throws WeaponTableFileException {
        if (f.length() > MAX_FILE_BYTES) {
            throw tooLarge();
        }
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_FILE_BYTES) {
                    throw tooLarge();
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new WeaponTableFileException(WeaponTableFileException.Reason.READ_FAILED, null, null, e);
        }
    }

    private static WeaponTableFileException tooLarge() {
        return new WeaponTableFileException(WeaponTableFileException.Reason.FILE_TOO_LARGE,
                String.valueOf(MAX_FILE_BYTES), null, null);
    }

    /**
     * 表を書く。呼ぶ側が先に {@link WeaponTableCodec#validate} を通しておく。
     * 順は次のとおり（04 3.5 の文はこの順に直す）: 一時ファイルに書く → いまのファイルが正しくないなら
     * {@code weapons.invalid-YYYYMMDD-HHmmss.json} へ名前を変えて残す → 一時ファイルを表のファイルの名前にする。
     * 書けなかったときは、一時ファイルを消して {@link WeaponTableFileException} を投げる。
     * 最後の名前の変更に失敗したときは、前のファイルを残した名前が {@link WeaponTableFileException#keptFileName()} に入る。
     * <p>
     * Writes the table. The caller must first pass {@link WeaponTableCodec#validate}.
     * Order (update 04 §3.5 to this order): write a temporary file → if the current file is invalid,
     * preserve it by renaming it to {@code weapons.invalid-YYYYMMDD-HHmmss.json} → rename the temporary file to
     * the table file name. If writing fails, deletes the temporary file and throws {@link
     * WeaponTableFileException}. If the final rename fails, {@link WeaponTableFileException#keptFileName()}
     * contains the name under which the previous file was preserved.
     */
    void write(WeaponTable table) throws WeaponTableFileException {
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new WeaponTableFileException(WeaponTableFileException.Reason.MKDIR_FAILED, dir.getPath(), null,
                    null);
        }
        byte[] data = WeaponTableCodec.write(table).getBytes(StandardCharsets.UTF_8);
        File tmp = new File(dir, file.getName() + ".tmp");
        String keptName = null;
        try {
            try (OutputStream out = new FileOutputStream(tmp)) {
                out.write(data);
                out.flush();
            } catch (IOException e) {
                throw new WeaponTableFileException(WeaponTableFileException.Reason.TMP_WRITE_FAILED, tmp.getPath(),
                        null, e);
            }
            if (file.exists() && read().source() == Source.INVALID_USING_DEFAULT) {
                String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
                File kept = new File(dir, "weapons.invalid-" + stamp + ".json");
                if (!file.renameTo(kept)) {
                    throw new WeaponTableFileException(WeaponTableFileException.Reason.RENAME_INVALID_FAILED,
                            kept.getName(), null, null);
                }
                keptName = kept.getName();
            }
            if (!tmp.renameTo(file)) {
                throw new WeaponTableFileException(WeaponTableFileException.Reason.RENAME_FAILED, file.getPath(),
                        keptName, null);
            }
        } catch (WeaponTableFileException | RuntimeException e) {
            if (tmp.exists() && !tmp.delete()) {
                tmp.deleteOnExit();
            }
            throw e;
        }
    }
}
