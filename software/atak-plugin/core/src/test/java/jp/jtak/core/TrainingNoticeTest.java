package jp.jtak.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TrainingNoticeTest {
    @Test
    public void includesEnglishAndJapaneseTrainingNotice() {
        assertEquals("TRAINING USE ONLY / 訓練用", TrainingNotice.text());
    }

    // W12: 兵器の表の注意書き（04 の 2.9）
    // W12: Weapon table notice (04 §2.9)
    @Test
    public void weaponSampleTextIsFixed() {
        assertEquals(
                "訓練用サンプル値 — 実際の安全距離・危害半径ではない / TRAINING SAMPLE VALUES — NOT REAL SAFETY DATA",
                TrainingNotice.weaponSampleText());
    }
}
