package jp.jtak.core.export;

/**
 * 書き出す書式と、ファイル名に使う語。
 * <p>
 * Export formats and the terms used in file names.
 */
public enum ExportKind {
    CAS_BRIEF("cas-brief"),
    ROUTING("routing"),
    CHECK_IN("check-in"),
    GAME_PLAN("game-plan");

    private final String slug;

    ExportKind(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }
}
