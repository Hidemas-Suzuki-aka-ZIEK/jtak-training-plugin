package jp.jtak.plugin.export;

import java.io.IOException;

public final class ExportException extends IOException {
    private static final long serialVersionUID = 1L;

    public enum Reason { TEXT_WRITE, DIR_CREATE, DIR_OPEN, PDF_WRITE }

    private final Reason reason;
    private final String detail;

    public ExportException(Reason reason, String detail, Throwable cause) {
        super(reason + ": " + detail, cause);
        this.reason = reason;
        this.detail = detail;
    }

    public Reason reason() {
        return reason;
    }

    public String detail() {
        return detail;
    }
}
