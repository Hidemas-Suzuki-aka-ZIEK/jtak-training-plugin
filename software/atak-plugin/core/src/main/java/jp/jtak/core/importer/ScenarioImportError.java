package jp.jtak.core.importer;

/**
 * 読み込まないときの理由（設計 06 の 2.2）。
 * <p>
 * Reason for refusing an import (design 06 §2.2).
 */
public final class ScenarioImportError {
    /**
     * 理由の種類。
     * <p>
     * Reason type.
     */
    public enum Code {
        TOO_LARGE,
        NOT_UTF8,
        EMPTY,
        UNKNOWN_TITLE,
        WRONG_KIND,
        NOTHING_READ
    }

    private final Code code;
    private final String message;
    private final ScenarioKind actualKind;

    ScenarioImportError(Code code, String message, ScenarioKind actualKind) {
        this.code = code;
        this.message = message;
        this.actualKind = actualKind;
    }

    public Code code() {
        return code;
    }

    /**
     * 画面にそのまま出す文（{@code lang} の言語）。
     * <p>
     * Message displayed directly on the screen (in the {@code lang} language).
     */
    public String message() {
        return message;
    }

    /**
     * {@link Code#WRONG_KIND} のときの、ファイルの書式。それ以外は null。
     * <p>
     * File format when {@link Code#WRONG_KIND}; null otherwise.
     */
    public ScenarioKind actualKind() {
        return actualKind;
    }
}
