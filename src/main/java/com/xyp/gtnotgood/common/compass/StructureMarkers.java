package com.xyp.gtnotgood.common.compass;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** Reads saved tile markers without instantiating tiles or loading/generating chunks. */
public final class StructureMarkers {

    private StructureMarkers() {}

    /**
     * Imports intact old structures. Roguelike's surface starter chest contains Greymerk's Statistics book;
     * red bricks in that chunk distinguish the house from other tower styles. LootGames keeps its original
     * puzzle master or a started game's master tile. Modified/looted old houses can be unrecognizable.
     *
     * @param level   chunk Level compound, or a synthetic snapshot of the live chunk
     * @param index   dimension-local destination
     * @param brickId current world's registered vanilla brick block ID
     */
    public static void scan(NBTTagCompound level, StructureLocations index, int brickId) {
        NBTTagList tiles = level.getTagList("TileEntities", 10);
        for (int i = 0; i < tiles.tagCount(); i++) {
            NBTTagCompound tile = tiles.getCompoundTagAt(i);
            String id = tile.getString("id");
            int x = tile.getInteger("x"), y = tile.getInteger("y"), z = tile.getInteger("z");
            if (isGameMaster(id)) index.add(1, x, y, z);
            if (isHouseChest(tile) && (level.hasKey("GTNGHasBricks") ? level.getBoolean("GTNGHasBricks")
                : hasBricks(level.getTagList("Sections", 10), brickId))) index.add(0, x, y, z);
        }
    }

    public static boolean isGameMaster(String id) {
        return "LOOTGAMES_MASTER_TE".equals(id) || "gol_master".equals(id)
            || "ms_master".equals(id)
            || "sdk_master".equals(id);
    }

    static boolean isHouseChest(NBTTagCompound chest) {
        if (!"Chest".equals(chest.getString("id")) || chest.getInteger("y") < 60) return false;
        NBTTagList items = chest.getTagList("Items", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound tag = items.getCompoundTagAt(i)
                .getCompoundTag("tag");
            if ("Greymerk".equals(tag.getString("author")) && "Statistics".equals(tag.getString("title"))) return true;
        }
        return false;
    }

    private static boolean hasBricks(NBTTagList sections, int brickId) {
        int count = 0;
        for (int s = 0; s < sections.tagCount(); s++) {
            NBTTagCompound section = sections.getCompoundTagAt(s);
            if ((section.getByte("Y") & 255) < 3) continue;
            byte[] blocks = section.getByteArray("Blocks"), high = section.getByteArray("Add");
            byte[] extended = section.getByteArray("BlocksB2Hi"), highest = section.getByteArray("BlocksB3");
            byte[] legacy = section.getByteArray("Blocks16");
            if (legacy.length != 0) {
                for (int i = 0; i + 1 < legacy.length; i += 2) {
                    if ((((legacy[i] & 255) << 8) | (legacy[i + 1] & 255)) == brickId && ++count >= 16) return true;
                }
                continue;
            }
            for (int i = 0; i < blocks.length; i++) {
                int upper = i / 2 < high.length ? (high[i / 2] >> ((i & 1) * 4)) & 15 : 0;
                int extra = i / 2 < extended.length ? (extended[i / 2] >> ((i & 1) * 4)) & 15 : 0;
                int top = i < highest.length ? highest[i] & 255 : 0;
                if (((blocks[i] & 255) | (upper << 8) | (extra << 12) | (top << 16)) == brickId && ++count >= 16)
                    return true;
            }
        }
        return false;
    }
}
