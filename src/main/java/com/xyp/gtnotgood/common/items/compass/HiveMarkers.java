package com.xyp.gtnotgood.common.items.compass;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.xyp.gtnotgood.utils.enums.ModsItemlist;

public final class HiveMarkers {

    private HiveMarkers() {}

    public static boolean isHive(Block block) {
        if (block == null) return false;
        for (Block hive : Hives.blocks) if (block == hive) return true;
        return false;
    }

    public static void scan(Chunk chunk, StructureLocations index) {
        index.removeHivesInChunk(chunk.xPosition, chunk.zPosition);
        for (ExtendedBlockStorage section : chunk.getBlockStorageArray()) {
            if (section == null) continue;
            for (int slot = 0; slot < 4096; slot++) {
                int x = slot & 15, y = section.getYLocation() + (slot >> 8), z = (slot >> 4) & 15;
                if (isHive(chunk.getBlock(x, y, z))) {
                    index.add(0, chunk.xPosition * 16 + x, y, chunk.zPosition * 16 + z);
                }
            }
        }
    }

    public static void scan(NBTTagCompound level, StructureLocations index) {
        int chunkX = level.getInteger("xPos"), chunkZ = level.getInteger("zPos");
        index.removeHivesInChunk(chunkX, chunkZ);
        int[] hiveIds = Hives.ids();
        NBTTagList sections = level.getTagList("Sections", 10);
        for (int s = 0; s < sections.tagCount(); s++) {
            NBTTagCompound section = sections.getCompoundTagAt(s);
            int baseY = (section.getByte("Y") & 255) << 4;
            if (baseY >= 256) continue;
            byte[] legacy = section.getByteArray("Blocks16"), low = section.getByteArray("Blocks");
            byte[] high = section.getByteArray("Add"), extended = section.getByteArray("BlocksB2Hi");
            byte[] top = section.getByteArray("BlocksB3");
            if (legacy.length != 8192 && low.length != 4096) continue;
            for (int slot = 0; slot < 4096; slot++) {
                int id = legacy.length == 8192 ? ((legacy[slot * 2] & 255) << 8) | (legacy[slot * 2 + 1] & 255)
                    : (low[slot] & 255) | (nibble(high, slot) << 8)
                        | (nibble(extended, slot) << 12)
                        | (slot < top.length ? (top[slot] & 255) << 16 : 0);
                if (isHiveId(id, hiveIds)) {
                    index.add(0, chunkX * 16 + (slot & 15), baseY + (slot >> 8), chunkZ * 16 + ((slot >> 4) & 15));
                }
            }
        }
    }

    private static boolean isHiveId(int id, int[] hiveIds) {
        for (int hive : hiveIds) if (id == hive) return true;
        return false;
    }

    private static int nibble(byte[] data, int slot) {
        return slot / 2 < data.length ? (data[slot / 2] >> ((slot & 1) * 4)) & 15 : 0;
    }

    private static final class Hives {

        static final Block[] blocks = { ModsItemlist.ForestryHive.getBlock(), ModsItemlist.MagicBeesHive.getBlock(),
            ModsItemlist.ExtraBeesHive.getBlock() };

        private static int[] ids() {
            int[] result = new int[blocks.length];
            for (int i = 0; i < blocks.length; i++) {
                result[i] = blocks[i] == null ? -1 : Block.getIdFromBlock(blocks[i]);
            }
            return result;
        }
    }
}
