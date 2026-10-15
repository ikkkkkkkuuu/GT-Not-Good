package com.xyp.gtnotgood.client.radialmenu.core.script;

public final class Facing {

    private static final String[] COMPASS = { "south", "west", "north", "east" };

    private Facing() {}

    public static double normalizeYaw(double yaw) {
        double turned = yaw % 360.0;
        return turned < 0.0 ? turned + 360.0 : turned;
    }

    public static String of(double yaw) {
        int quarter = (int) Math.floor(normalizeYaw(yaw) / 90.0 + 0.5) & 3;
        return COMPASS[quarter];
    }
}
