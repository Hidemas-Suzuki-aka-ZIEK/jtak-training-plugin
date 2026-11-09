package jp.jtak.plugin.export;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jp.jtak.core.TrainingNotice;
import jp.jtak.core.export.ExportKind;
import jp.jtak.core.export.ExportText;

/**
 * A4 縦の各ページの上下に、必ず訓練用の表示を描く。
 * <p>
 * Always draws the training notice at the top and bottom of every portrait A4 page.
 */
public final class PdfExporter {
    private static final int WIDTH = 595;
    private static final int HEIGHT = 842;
    private static final float MARGIN = 40;
    private static final float TEXT_SIZE = 10;
    private static final float LINE_HEIGHT = TEXT_SIZE * 1.4f;

    public File writePdf(ExportKind kind, String resultText, LocalDateTime time) throws IOException {
        List<String> body = ExportText.bodyLines(resultText);
        File file = ExportWriter.outputFile(kind, time, "pdf");
        try {
            PdfDocument document = new PdfDocument();
            try {
                Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
                text.setTypeface(Typeface.MONOSPACE);
                text.setTextSize(TEXT_SIZE);
                text.setColor(Color.BLACK);
                Paint notice = new Paint(text);
                notice.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                List<String> lines = wrap(body, text);
                // 行の上端と下端が余白を越えないよう、フォントのメトリクスから基線を決める。
                // Determine the baseline from font metrics so the top and bottom of each line stay within the margins.
                float top = MARGIN - notice.ascent();
                float bottom = HEIGHT - MARGIN - notice.descent();
                float firstBody = top + LINE_HEIGHT;
                float lastBody = bottom - LINE_HEIGHT;
                int perPage = (int) Math.floor((lastBody - firstBody) / LINE_HEIGHT) + 1;
                for (int offset = 0, pageNumber = 1; offset < lines.size(); offset += perPage, pageNumber++) {
                    PdfDocument.Page page = document.startPage(
                            new PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, pageNumber).create());
                    try {
                        Canvas canvas = page.getCanvas();
                        canvas.drawText(TrainingNotice.text(), MARGIN, top, notice);
                        canvas.drawText(TrainingNotice.text(), MARGIN, bottom, notice);
                        int count = Math.min(perPage, lines.size() - offset);
                        for (int i = 0; i < count; i++) {
                            canvas.drawText(lines.get(offset + i), MARGIN, firstBody + i * LINE_HEIGHT, text);
                        }
                    } finally {
                        document.finishPage(page);
                    }
                }
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    document.writeTo(stream);
                }
            } finally {
                document.close();
            }
            return file;
        } catch (IOException | RuntimeException e) {
            ExportWriter.removeIncomplete(file, e);
            throw new ExportException(ExportException.Reason.PDF_WRITE, e.getMessage(), e);
        }
    }

    private static List<String> wrap(List<String> body, Paint paint) {
        List<String> lines = new ArrayList<>();
        for (String line : body) {
            if (line.isEmpty()) {
                lines.add("");
                continue;
            }
            int start = 0;
            while (start < line.length()) {
                int end = start;
                while (end < line.length()) {
                    int next = end + Character.charCount(line.codePointAt(end));
                    if (end > start && paint.measureText(line, start, next) > WIDTH - MARGIN * 2) {
                        break;
                    }
                    end = next;
                }
                lines.add(line.substring(start, end));
                start = end;
            }
        }
        return lines;
    }
}
