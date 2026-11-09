package jp.jtak.core;

/**
 * すべての出力で使う、無効化できない訓練用の表示。
 * <p>
 * Training notice used in every output; cannot be disabled.
 */
public final class TrainingNotice {
    private TrainingNotice() {}

    public static String text() {
        return "TRAINING USE ONLY / 訓練用";
    }

    /**
     * 兵器の範囲の画面・円の説明に必ず出す、訓練用サンプル値の注意（docs/usage-scope.md 3 章）。
     * <p>
     * Training sample values notice always shown on the weapon effects screen and in circle descriptions
     * (docs/usage-scope.md §3).
     */
    public static String weaponSampleText() {
        return "訓練用サンプル値 — 実際の安全距離・危害半径ではない / TRAINING SAMPLE VALUES — NOT REAL SAFETY DATA";
    }
}
