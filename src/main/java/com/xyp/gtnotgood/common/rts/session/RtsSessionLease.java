package com.xyp.gtnotgood.common.rts.session;

/**
 * Immutable server-issued RTS session bounds. A lease authorizes a session, not a world modification;
 * future action handlers must still check the current player, loaded target, inventory and Forge permissions.
 */
public final class RtsSessionLease {

    public final long requestId;
    public final long token;
    public final int dimension;
    public final double anchorX, anchorY, anchorZ;
    public final int radius;

    public RtsSessionLease(long requestId, long token, int dimension, double x, double y, double z, int radius) {
        if (requestId <= 0 || token == 0
            || !Double.isFinite(x)
            || !Double.isFinite(y)
            || !Double.isFinite(z)
            || Math.abs(x) > 30000000
            || Math.abs(z) > 30000000
            || y < -4096
            || y > 4096
            || radius < 1
            || radius > 512) {
            throw new IllegalArgumentException("Invalid RTS session lease");
        }
        this.requestId = requestId;
        this.token = token;
        this.dimension = dimension;
        this.anchorX = x;
        this.anchorY = y;
        this.anchorZ = z;
        this.radius = radius;
    }
}
