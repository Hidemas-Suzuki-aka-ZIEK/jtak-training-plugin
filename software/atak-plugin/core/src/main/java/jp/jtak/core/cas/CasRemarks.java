package jp.jtak.core.cas;

/**
 * Remarks の 5 項目（F02 の Remarks の表の順）。空か null は出さない。不変。
 * <p>
 * Five Remarks items (in the order of the F02 Remarks table). Empty or null values are omitted. Immutable.
 */
public final class CasRemarks {
    private final String ltlPtl;
    private final String ordnance;
    private final String threat;
    private final String additionalRemarks;
    private final String additionalCalls;

    private CasRemarks(Builder b) {
        ltlPtl = b.ltlPtl;
        ordnance = b.ordnance;
        threat = b.threat;
        additionalRemarks = b.additionalRemarks;
        additionalCalls = b.additionalCalls;
    }

    public String ltlPtl() {
        return ltlPtl;
    }

    public String ordnance() {
        return ordnance;
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

    public static final class Builder {
        private String ltlPtl;
        private String ordnance;
        private String threat;
        private String additionalRemarks;
        private String additionalCalls;

        public Builder ltlPtl(String v) {
            ltlPtl = v;
            return this;
        }

        public Builder ordnance(String v) {
            ordnance = v;
            return this;
        }

        public Builder threat(String v) {
            threat = v;
            return this;
        }

        public Builder additionalRemarks(String v) {
            additionalRemarks = v;
            return this;
        }

        public Builder additionalCalls(String v) {
            additionalCalls = v;
            return this;
        }

        public CasRemarks build() {
            return new CasRemarks(this);
        }
    }
}
