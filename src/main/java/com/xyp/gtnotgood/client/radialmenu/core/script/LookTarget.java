package com.xyp.gtnotgood.client.radialmenu.core.script;

public final class LookTarget {

    public static final String BLOCK = "block";

    public static final String ENTITY = "entity";

    public final String kind;

    public final String id;

    public final String label;

    public final int meta;

    public final int x;

    public final int y;

    public final int z;

    public LookTarget(String kind, String id, String label, int meta, int x, int y, int z) {
        this.kind = kind;
        this.id = id == null ? "" : id;
        this.label = label == null ? this.id : label;
        this.meta = meta;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static LookTarget block(String id, String label, int meta, int x, int y, int z) {
        return new LookTarget(BLOCK, id, label, meta, x, y, z);
    }

    public static LookTarget entity(String id, String label, int x, int y, int z) {
        return new LookTarget(ENTITY, id, label, 0, x, y, z);
    }
}
