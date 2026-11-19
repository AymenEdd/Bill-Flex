
package org.gmp.serviceabonnements.dto;

public class AbonnementStatisticsResponse {

    private long total;
    private long actifs;
    private long suspendus;
    private long resilies;
    private long expires;

    public AbonnementStatisticsResponse(
            long total,
            long actifs,
            long suspendus,
            long resilies,
            long expires) {
        this.total = total;
        this.actifs = actifs;
        this.suspendus = suspendus;
        this.resilies = resilies;
        this.expires = expires;
    }

    public long getTotal() {
        return total;
    }

    public long getActifs() {
        return actifs;
    }

    public long getSuspendus() {
        return suspendus;
    }

    public long getResilies() {
        return resilies;
    }

    public long getExpires() {
        return expires;
    }
}
