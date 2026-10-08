package com.xyp.gtnotgood.ae2thing.api;

/** Promotes coordinates before subtraction and squaring so distant access points cannot wrap into range. */
final class WirelessRange {

    private WirelessRange() {}

    static boolean contains(int accessX, int accessY, int accessZ, int playerX, int playerY, int playerZ,
        double range) {
        double dx = (double) accessX - playerX;
        double dy = (double) accessY - playerY;
        double dz = (double) accessZ - playerZ;
        return range >= 0 && dx * dx + dy * dy + dz * dz <= range * range;
    }
}
