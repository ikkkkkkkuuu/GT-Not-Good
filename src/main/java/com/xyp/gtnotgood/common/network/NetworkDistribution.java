package com.xyp.gtnotgood.common.network;

/** Bounded allocation shared by item, fluid and EU routing; demands already include endpoint limits and capacity. */
final class NetworkDistribution {

    private NetworkDistribution() {}

    /**
     * Allocates a batch across destinations, using max-min fairness for even mode and sequential filling otherwise.
     * The caller rotates destination order for round robin or sorts it for priority mode. A full destination must
     * not stop the rest of a batch from reaching other destinations.
     */
    static long[] allocate(long available, long[] demand, int mode) {
        long[] result = new long[demand.length];
        long remaining = Math.max(0, available);
        if (mode != 1) {
            for (int i = 0; i < demand.length && remaining > 0; i++) {
                result[i] = Math.min(remaining, Math.max(0, demand[i]));
                remaining -= result[i];
            }
            return result;
        }
        int active = 0;
        for (long amount : demand) if (amount > 0) active++;
        while (remaining > 0 && active > 0) {
            long share = Math.max(1, remaining / active);
            for (int i = 0; i < demand.length && remaining > 0; i++) {
                long room = demand[i] - result[i];
                if (room <= 0) continue;
                long grant = Math.min(remaining, Math.min(share, room));
                result[i] += grant;
                remaining -= grant;
                if (result[i] == demand[i]) active--;
            }
        }
        return result;
    }
}
