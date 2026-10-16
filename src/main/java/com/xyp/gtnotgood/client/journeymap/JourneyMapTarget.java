package com.xyp.gtnotgood.client.journeymap;

public final class JourneyMapTarget {

    public final int x;
    public final int y;
    public final int z;
    public final int dimension;
    public final boolean cave;
    public final Object waypoint;
    public final String name;

    JourneyMapTarget(int x, int y, int z, int dimension, boolean cave, Object waypoint, String name) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.cave = cave;
        this.waypoint = waypoint;
        this.name = name;
    }
}
