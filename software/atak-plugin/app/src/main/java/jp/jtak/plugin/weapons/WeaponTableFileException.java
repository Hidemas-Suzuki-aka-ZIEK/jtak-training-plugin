package jp.jtak.plugin.weapons;

import java.io.IOException;

/**
 * 表のファイルを読み書きできなかったこと（04 3.5・3.6）。
 * 日本語の文は持たない。原因を表す {@link Reason} と、文に入れる値だけを持ち、
 * 画面の文は {@code WeaponPanel} が strings.xml の文言に変えて出す。
 * <p>
 * Failure to read or write the table file (04 §§3.5, 3.6). Contains no Japanese messages. Holds only a {@link
 * Reason} and values to insert into messages; {@code WeaponPanel} displays screen messages using strings.xml text.
 */
final class WeaponTableFileException extends IOException {
    private static final long serialVersionUID = 1L;

    /**
     * 失敗の原因。
     * <p>
     * Cause of failure.
     */
    enum Reason {
        /**
         * 表のファイルを読めない。
         * <p>
         * Cannot read the table file.
         */
        READ_FAILED,
        /**
         * ファイルが大きすぎる。{@link #detail()} は上限のバイト数。
         * <p>
         * File is too large. {@link #detail()} is the byte limit.
         */
        FILE_TOO_LARGE,
        /**
         * 親のフォルダを作れない。{@link #detail()} はフォルダの道筋。
         * <p>
         * Cannot create the parent directory. {@link #detail()} is the directory path.
         */
        MKDIR_FAILED,
        /**
         * 一時ファイルに書けない。{@link #detail()} は一時ファイルの道筋。
         * <p>
         * Cannot write to the temporary file. {@link #detail()} is the temporary file path.
         */
        TMP_WRITE_FAILED,
        /**
         * 正しくないファイルの名前を変えられない。{@link #detail()} は変えたあとの名前。
         * <p>
         * Cannot rename the invalid file. {@link #detail()} is the destination name.
         */
        RENAME_INVALID_FAILED,
        /**
         * 一時ファイルを表のファイルの名前にできない。{@link #detail()} は表のファイルの道筋。
         * <p>
         * Cannot rename the temporary file to the table file name. {@link #detail()} is the table file path.
         */
        RENAME_FAILED
    }

    private final Reason reason;
    private final String detail;
    private final String keptFileName;

    /**
     * @param keptFileName 前のファイルを別の名前（{@code weapons.invalid-*.json}）で残したときのその名前。残していなければ null The name under which the previous file was preserved ({@code weapons.invalid-*.json}), or null if not preserved.
     */
    WeaponTableFileException(Reason reason, String detail, String keptFileName, Throwable cause) {
        super(reason + (detail == null ? "" : ": " + detail), cause);
        this.reason = reason;
        this.detail = detail;
        this.keptFileName = keptFileName;
    }

    Reason reason() {
        return reason;
    }

    /**
     * 文に入れる値（原因によって、上限・道筋・名前）。ないときは null。
     * <p>
     * Value to insert into a message (limit, path, or name depending on the cause). Null if absent.
     */
    String detail() {
        return detail;
    }

    /**
     * 前のファイルを {@code weapons.invalid-*.json} の名前で残したときのその名前。残していなければ null。
     * <p>
     * The name under which the previous file was preserved as {@code weapons.invalid-*.json}, or null if not
     * preserved.
     */
    String keptFileName() {
        return keptFileName;
    }
}
