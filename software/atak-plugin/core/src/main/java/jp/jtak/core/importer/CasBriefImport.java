package jp.jtak.core.importer;

import jp.jtak.core.cas.TotType;

/**
 * CAS Brief（9-line・Remarks・Restrictions）の読み込み結果。null は「ファイルになかった・飛ばした」。（不変）。
 * <p>
 * CAS Brief (9-line, Remarks, and Restrictions) import result. Null means “absent from file or skipped.”
 * Immutable.
 */
public final class CasBriefImport {
    private final String line1;
    private final String line2;
    private final String offset;
    private final String line3;
    private final String line4;
    private final String line5;
    private final String line6;
    private final String line7;
    private final String line8;
    private final String line9;
    private final String ltlPtl;
    private final String remarksOrdnance;
    private final String threat;
    private final String additionalRemarks;
    private final String additionalCalls;
    private final Boolean dangerClose;
    private final String dangerCloseInitials;
    private final String acas;
    private final String fahs;
    private final String separation;
    private final String rwOutOfBp;
    private final String pla;
    private final TotType tot;
    private final String totTime;

    private CasBriefImport(Builder b) {
        this.line1 = b.line1;
        this.line2 = b.line2;
        this.offset = b.offset;
        this.line3 = b.line3;
        this.line4 = b.line4;
        this.line5 = b.line5;
        this.line6 = b.line6;
        this.line7 = b.line7;
        this.line8 = b.line8;
        this.line9 = b.line9;
        this.ltlPtl = b.ltlPtl;
        this.remarksOrdnance = b.remarksOrdnance;
        this.threat = b.threat;
        this.additionalRemarks = b.additionalRemarks;
        this.additionalCalls = b.additionalCalls;
        this.dangerClose = b.dangerClose;
        this.dangerCloseInitials = b.dangerCloseInitials;
        this.acas = b.acas;
        this.fahs = b.fahs;
        this.separation = b.separation;
        this.rwOutOfBp = b.rwOutOfBp;
        this.pla = b.pla;
        this.tot = b.tot;
        this.totTime = b.totTime;
    }

    public String line1() {
        return line1;
    }

    public String line2() {
        return line2;
    }

    public String offset() {
        return offset;
    }

    public String line3() {
        return line3;
    }

    public String line4() {
        return line4;
    }

    public String line5() {
        return line5;
    }

    public String line6() {
        return line6;
    }

    public String line7() {
        return line7;
    }

    public String line8() {
        return line8;
    }

    public String line9() {
        return line9;
    }

    public String ltlPtl() {
        return ltlPtl;
    }

    public String remarksOrdnance() {
        return remarksOrdnance;
    }

    public String threat() {
        return threat;
    }

    public String additionalRemarks() {
        return additionalRemarks;
    }

    public String additionalCalls() {
        return additionalCalls;
    }

    public Boolean dangerClose() {
        return dangerClose;
    }

    public String dangerCloseInitials() {
        return dangerCloseInitials;
    }

    public String acas() {
        return acas;
    }

    public String fahs() {
        return fahs;
    }

    public String separation() {
        return separation;
    }

    public String rwOutOfBp() {
        return rwOutOfBp;
    }

    public String pla() {
        return pla;
    }

    public TotType tot() {
        return tot;
    }

    public String totTime() {
        return totTime;
    }

    /**
     * パーサが値を集めるための入れ物。
     * <p>
     * Container for values collected by the parser.
     */
    static final class Builder {
        String line1;
        String line2;
        String offset;
        String line3;
        String line4;
        String line5;
        String line6;
        String line7;
        String line8;
        String line9;
        String ltlPtl;
        String remarksOrdnance;
        String threat;
        String additionalRemarks;
        String additionalCalls;
        Boolean dangerClose;
        String dangerCloseInitials;
        String acas;
        String fahs;
        String separation;
        String rwOutOfBp;
        String pla;
        TotType tot;
        String totTime;

        CasBriefImport build() {
            return new CasBriefImport(this);
        }
    }
}
