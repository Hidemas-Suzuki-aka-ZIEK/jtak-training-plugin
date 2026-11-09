package jp.jtak.core.lrf;

import java.util.Objects;
import jp.jtak.core.Lang;

/**
 * 測距値の入力の経路。
 * <p>
 * Measurement input source.
 */
public enum InputSource {
    ATAK_LRF("ATAK の LRF 連携", "ATAK LRF link"),
    BLE("BLE 直結", "BLE direct"),
    MANUAL("手入力", "Manual entry");

    private final String label;
    private final String englishLabel;

    InputSource(String label, String englishLabel) {
        this.label = label;
        this.englishLabel = englishLabel;
    }

    /**
     * 画面・説明に出す表示。
     * <p>
     * Label shown on screens and in descriptions.
     */
    public String label(Lang lang) {
        Objects.requireNonNull(lang, "lang");
        return lang == Lang.JA ? label : englishLabel;
    }
}
