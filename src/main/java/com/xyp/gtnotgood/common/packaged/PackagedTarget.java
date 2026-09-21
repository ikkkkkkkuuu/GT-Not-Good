// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Persistent bound face; resolving it never loads a dimension or chunk. */
public final class PackagedTarget {

    public final int dimension, x, y, z, face;

    public PackagedTarget(int dimension, int x, int y, int z, int face) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
    }

    public TileEntity resolve(World world) {
        if (world == null || world.provider.dimensionId != dimension
            || y < 0
            || y >= world.getHeight()
            || !world.getChunkProvider()
                .chunkExists(x >> 4, z >> 4))
            return null;
        return world.getTileEntity(x, y, z);
    }

    /** Uses upstream Pos/Face names, with an explicitly versioned 1.7 integer dimension field. */
    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("GTNGDimension", dimension);
        tag.setInteger("GTNGFormat", 1);
        tag.setLong("Pos", ((long) x & 0x3ffffffL) << 38 | ((long) z & 0x3ffffffL) << 12 | (y & 0xfffL));
        tag.setInteger("Face", face);
        return tag;
    }

    public static PackagedTarget read(NBTTagCompound tag) {
        if (tag.getInteger("GTNGFormat") != 1 || !tag.hasKey("GTNGDimension", 3) || !tag.hasKey("Pos", 4)) return null;
        long pos = tag.getLong("Pos");
        int face = tag.getInteger("Face");
        int y = (int) (pos << 52 >> 52);
        if (face < 0 || face > 5 || y < 0 || y > 255) return null;
        return new PackagedTarget(tag.getInteger("GTNGDimension"), (int) (pos >> 38), y, (int) (pos << 26 >> 38), face);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PackagedTarget t && dimension == t.dimension
            && x == t.x
            && y == t.y
            && z == t.z
            && face == t.face;
    }

    @Override
    public int hashCode() {
        return ((((dimension * 31 + x) * 31 + y) * 31 + z) * 31 + face);
    }

    /** A target block is a single crafting lane even if different faces were clicked. */
    public boolean sameBlock(PackagedTarget t) {
        return t != null && dimension == t.dimension && x == t.x && y == t.y && z == t.z;
    }
}
