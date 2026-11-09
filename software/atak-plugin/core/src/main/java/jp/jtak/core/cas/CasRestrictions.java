package jp.jtak.core.cas;

/**
 * Restrictions の入力（docs/detailed-design/03-cas-other-products.md 2.2.4）。不変。
 * <p>
 * Restrictions input (docs/detailed-design/03-cas-other-products.md §2.2.4). Immutable.
 */
public final class CasRestrictions {
    private final boolean dangerClose;
    private final String dangerCloseInitials;
    private final String acas;
    private final String fahs;
    private final String separation;
    private final String rwOutOfBp;
    private final String pla;
    private final TotType tot;
    private final String totTime;

    private CasRestrictions(Builder b) {
        dangerClose = b.dangerClose;
        dangerCloseInitials = b.dangerCloseInitials;
        acas = b.acas;
        fahs = b.fahs;
        separation = b.separation;
        rwOutOfBp = b.rwOutOfBp;
        pla = b.pla;
        tot = b.tot;
        totTime = b.totTime;
    }

    public boolean dangerClose() {
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

    public static final class Builder {
        private boolean dangerClose;
        private String dangerCloseInitials;
        private String acas;
        private String fahs;
        private String separation;
        private String rwOutOfBp;
        private String pla;
        private TotType tot;
        private String totTime;

        public Builder dangerClose(boolean v) {
            dangerClose = v;
            return this;
        }

        public Builder dangerCloseInitials(String v) {
            dangerCloseInitials = v;
            return this;
        }

        public Builder acas(String v) {
            acas = v;
            return this;
        }

        public Builder fahs(String v) {
            fahs = v;
            return this;
        }

        public Builder separation(String v) {
            separation = v;
            return this;
        }

        public Builder rwOutOfBp(String v) {
            rwOutOfBp = v;
            return this;
        }

        public Builder pla(String v) {
            pla = v;
            return this;
        }

        public Builder tot(TotType v) {
            tot = v;
            return this;
        }

        public Builder totTime(String v) {
            totTime = v;
            return this;
        }

        /**
         * @throws IllegalArgumentException dangerClose が false なのにイニシャルがあるとき、
         *     または tot が TIME 以外なのに totTime があるとき if initials are supplied while dangerClose is false, or totTime is supplied while tot is not TIME
         */
        public CasRestrictions build() {
            if (!dangerClose && CasText.clean(dangerCloseInitials) != null) {
                throw new IllegalArgumentException("dangerClose でないのに dangerCloseInitials がある");
            }
            if (tot != TotType.TIME && CasText.clean(totTime) != null) {
                throw new IllegalArgumentException("tot が TIME でないのに totTime がある");
            }
            return new CasRestrictions(this);
        }
    }
}
