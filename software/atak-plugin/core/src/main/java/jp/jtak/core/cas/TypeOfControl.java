package jp.jtak.core.cas;

/**
 * ゲームプランの統制の種類（出典 JP 3-09.3 2014 図 III-7・V-9）。
 * <p>
 * Game plan types of control (source: JP 3-09.3 2014, Figures III-7 and V-9).
 */
public enum TypeOfControl {
    TYPE_1("Type 1"),
    TYPE_2("Type 2"),
    TYPE_3("Type 3");

    private final String phrase;

    TypeOfControl(String phrase) {
        this.phrase = phrase;
    }

    String phrase() {
        return phrase;
    }
}
