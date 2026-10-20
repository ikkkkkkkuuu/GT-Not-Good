package com.xyp.gtnotgood.common.items.compass;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class StructureMarkers {

    private StructureMarkers() {}

    public static void scan(NBTTagCompound level, StructureLocations index) {
        NBTTagList tiles = level.getTagList("TileEntities", 10);
        for (int i = 0; i < tiles.tagCount(); i++) {
            NBTTagCompound tile = tiles.getCompoundTagAt(i);
            if (isGameMaster(tile.getString("id"))) {
                index.add(1, tile.getInteger("x"), tile.getInteger("y"), tile.getInteger("z"));
            }
        }
    }

    public static boolean isGameMaster(String id) {
        return "LOOTGAMES_MASTER_TE".equals(id) || "gol_master".equals(id)
            || "ms_master".equals(id)
            || "sdk_master".equals(id);
    }
}
