package jp.jtak.core.cas;

/**
 * ゲームプランの攻撃の方法（出典 JP 3-09.3 2014 図 III-7・V-9・V-20）。
 * <p>
 * Game plan method of attack (source: JP 3-09.3 2014, Figures III-7 and V-9, and V-20).
 */
public enum MethodOfAttack {
    /**
     * 目標への投弾
     * <p>
     * bomb on target
     */
    BOT("bomb on target"),
    /**
     * 座標への投弾
     * <p>
     * bomb on coordinate
     */
    BOC("bomb on coordinate");

    private final String phrase;

    MethodOfAttack(String phrase) {
        this.phrase = phrase;
    }

    String phrase() {
        return phrase;
    }
}
