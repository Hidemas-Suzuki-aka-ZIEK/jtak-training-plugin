package jp.jtak.core.export;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ExportFileName {
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private ExportFileName() { }

    public static String of(ExportKind kind, LocalDateTime time, String extension) {
        if (kind == null || time == null || !("txt".equals(extension) || "pdf".equals(extension))) {
            throw new IllegalArgumentException("書式・時刻・拡張子を確認してください");
        }
        return "jtak-" + kind.slug() + "-" + TIME.format(time) + "." + extension;
    }
}
