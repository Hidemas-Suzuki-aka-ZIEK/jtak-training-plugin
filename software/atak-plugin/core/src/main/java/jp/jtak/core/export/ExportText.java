package jp.jtak.core.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.jtak.core.TrainingNotice;

/**
 * 画面の本文を保ち、ファイルの先頭と末尾に訓練用の表示を付ける。
 * <p>
 * Preserves screen body text and adds the training notice at the beginning and end of the file.
 */
public final class ExportText {
    private ExportText() { }

    public static List<String> bodyLines(String resultText) {
        if (resultText == null) {
            throw new IllegalArgumentException("書き出す本文がありません");
        }
        List<String> lines = new ArrayList<>(Arrays.asList(resultText.replace("\r\n", "\n").split("\n", -1)));
        trimEmptyEnd(lines);
        if (!lines.isEmpty() && TrainingNotice.text().equals(lines.get(0))) {
            lines.remove(0);
        }
        if (!lines.isEmpty() && TrainingNotice.text().equals(lines.get(lines.size() - 1))) {
            lines.remove(lines.size() - 1);
        }
        trimEmptyEnd(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("書き出す本文がありません");
        }
        return lines;
    }

    public static String fileContent(String resultText) {
        return TrainingNotice.text() + "\n" + String.join("\n", bodyLines(resultText))
                + "\n" + TrainingNotice.text() + "\n";
    }

    private static void trimEmptyEnd(List<String> lines) {
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
    }
}
