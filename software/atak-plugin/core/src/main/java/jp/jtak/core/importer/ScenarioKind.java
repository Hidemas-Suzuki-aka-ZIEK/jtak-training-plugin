package jp.jtak.core.importer;

/**
 * 読み込む書式の種類。app はボタンを押した画面に合う値を渡す（設計 06 の 2.1.1）。
 * <p>
 * Type of format to import. App passes the value matching the screen whose button was pressed (design 06 §2.1.1).
 */
public enum ScenarioKind {
    ROUTING,
    CHECK_IN,
    GAME_PLAN,
    CAS_BRIEF
}
