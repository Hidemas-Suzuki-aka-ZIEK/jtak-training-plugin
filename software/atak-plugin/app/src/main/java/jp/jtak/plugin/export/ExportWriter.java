package jp.jtak.plugin.export;

import android.util.Log;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import jp.jtak.core.export.ExportFileName;
import jp.jtak.core.export.ExportKind;
import jp.jtak.core.export.ExportText;

/**
 * ATAK の export/jtak の下に UTF-8（BOM なし）で書く。
 * <p>
 * Writes UTF-8 without a BOM under ATAK's export/jtak directory.
 */
public final class ExportWriter {
    public File writeText(ExportKind kind, String resultText, LocalDateTime time) throws IOException {
        String content = ExportText.fileContent(resultText);
        File file = outputFile(kind, time, "txt");
        try {
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            return file;
        } catch (IOException | RuntimeException e) {
            removeIncomplete(file, e);
            throw new ExportException(ExportException.Reason.TEXT_WRITE, e.getMessage(), e);
        }
    }

    static File outputFile(ExportKind kind, LocalDateTime time, String extension) throws IOException {
        String name = ExportFileName.of(kind, time, extension);
        try {
            File directory = FileSystemUtils.getItem(FileSystemUtils.EXPORT_DIRECTORY + "/jtak");
            if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
                throw new ExportException(ExportException.Reason.DIR_CREATE, directory.toString(), null);
            }
            return new File(directory, name);
        } catch (RuntimeException e) {
            throw new ExportException(ExportException.Reason.DIR_OPEN, e.getMessage(), e);
        }
    }

    static void removeIncomplete(File file, Exception cause) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException | RuntimeException e) {
            cause.addSuppressed(e);
            Log.e("JtakExport", "途中まで書いたファイルを消せなかった", e);
        }
    }
}
