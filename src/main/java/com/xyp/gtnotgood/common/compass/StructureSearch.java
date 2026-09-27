package com.xyp.gtnotgood.common.compass;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.compass.StructureLocations.Location;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/** Bounded, server-thread seed search. Only reads existing chunks; it never calls a generating chunk provider. */
public final class StructureSearch {

    public static final StructureSearch INSTANCE = new StructureSearch();
    public static final int RADIUS = 8192;
    private final Map<UUID, Search> searches = new HashMap<>();

    private StructureSearch() {}

    public void cancel(EntityPlayer player) {
        searches.remove(player.getUniqueID());
    }

    /** Server-side status used by the compass panel's synchronized state. */
    public boolean isSearching(EntityPlayer player) {
        return !player.worldObj.isRemote && searches.containsKey(player.getUniqueID());
    }

    public void start(EntityPlayer player, ItemStack stack) {
        if (searches.containsKey(player.getUniqueID())) return;
        if (searches.size() >= 4) {
            // #tr compass.busy
            // # The compass is busy. Try again shortly.
            // # zh_CN 罗盘搜索繁忙，请稍后重试。
            player.addChatMessage(new ChatComponentTranslation("compass.busy"));
            return;
        }
        if (StructureCompassItem.mode(stack) == 1) refreshLoadedGames(player);
        Location known = StructureLocations.get(player.worldObj)
            .nearest(StructureCompassItem.mode(stack), player.posX, player.posZ, RADIUS);
        if (known != null) {
            setTarget(player, stack, known, true);
            return;
        }
        try {
            Search search = new Search(player, stack);
            if (!search.enabled()) {
                // #tr compass.disabled
                // # Natural generation of this target is disabled in this dimension.
                // # zh_CN 当前维度未启用此目标的自然生成。
                player.addChatMessage(new ChatComponentTranslation("compass.disabled"));
                return;
            }
            StructureCompassItem.data(stack)
                .removeTag("Found");
            searches.put(player.getUniqueID(), search);
            // #tr compass.searching
            // # Reading the world seed (8192-block radius). Keep holding the compass.
            // # zh_CN 正在按种子搜索周围 8192 格，请保持手持罗盘。
            player.addChatMessage(new ChatComponentTranslation("compass.searching"));
        } catch (ReflectiveOperationException | LinkageError e) {
            failure(player, e);
        }
    }

    public static void setTarget(EntityPlayer player, ItemStack stack, Location target, boolean confirmed) {
        NBTTagCompound tag = StructureCompassItem.data(stack);
        tag.setBoolean("Found", true);
        tag.setBoolean("Confirmed", confirmed);
        tag.setInteger("Dimension", player.dimension);
        tag.setInteger("X", target.x);
        tag.setInteger("Y", target.y);
        tag.setInteger("Z", target.z);
        if (confirmed) {
            // #tr compass.confirmed
            // # Recorded structure: %s, %s, %s. The compass now points there.
            // # zh_CN 已记录遗迹：%s，%s，%s。罗盘已指向该位置。
            player.addChatMessage(new ChatComponentTranslation("compass.confirmed", target.x, target.y, target.z));
        } else {
            // #tr compass.candidate
            // # Unconfirmed area: X %s, Z %s. Approach to verify; terrain was not generated.
            // # zh_CN 待确认区域：X %s，Z %s。靠近后验证；本次未生成地形。
            player.addChatMessage(new ChatComponentTranslation("compass.candidate", target.x, target.z));
        }
        player.inventory.markDirty();
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Search> iterator = searches.values()
            .iterator();
        while (iterator.hasNext()) {
            Search search = iterator.next();
            if (search.player.isDead || search.player.worldObj != search.world
                || !search.world.playerEntities.contains(search.player)
                || search.player.getHeldItem() != search.stack
                || StructureCompassItem.mode(search.stack) != search.kind) {
                iterator.remove();
                continue;
            }
            try {
                if (search.step()) iterator.remove();
            } catch (IOException | ReflectiveOperationException | LinkageError e) {
                iterator.remove();
                failure(search.player, e);
            }
        }
    }

    private static void failure(EntityPlayer player, Throwable error) {
        GTNotGood.LOG.warn("Structure compass search failed", error);
        // #tr compass.error
        // # Search failed: incompatible generation API or unreadable chunk. See the server log.
        // # zh_CN 搜索失败：生成接口不兼容或区块读取异常，详情见服务端日志。
        player.addChatMessage(new ChatComponentTranslation("compass.error"));
    }

    @SubscribeEvent
    public void load(ChunkEvent.Load event) {
        if (!event.world.isRemote) scan(event.world, snapshot(event.getChunk()));
    }

    @SubscribeEvent
    public void save(ChunkDataEvent.Save event) {
        if (!event.world.isRemote) scan(
            event.world,
            event.getData()
                .getCompoundTag("Level"));
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        searches.values()
            .removeIf(search -> search.world == event.world);
    }

    private static void scan(World world, NBTTagCompound level) {
        StructureMarkers.scan(level, StructureLocations.get(world), Block.getIdFromBlock(Blocks.brick_block));
    }

    /**
     * Discovers nearby existing game cores even when they predate the compass index or current seed settings.
     * Only examines already loaded tiles within 256 blocks; no terrain is loaded or generated.
     *
     * @param player search origin in the server world
     */
    private static void refreshLoadedGames(EntityPlayer player) {
        World world = player.worldObj;
        for (Object object : world.loadedTileEntityList) {
            TileEntity tile = (TileEntity) object;
            double dx = tile.xCoord + .5 - player.posX, dz = tile.zCoord + .5 - player.posZ;
            if (dx * dx + dz * dz > 256.0 * 256.0 || !isGameCore(tile)) continue;
            StructureLocations.get(world)
                .add(1, tile.xCoord, tile.yCoord, tile.zCoord);
        }
    }

    /** Recognizes registered game cores without depending on optional tile class names or loading chunks. */
    private static boolean isGameCore(TileEntity tile) {
        if (tile.isInvalid() || tile.getWorldObj() == null
            || !tile.getWorldObj()
                .getChunkProvider()
                .chunkExists(tile.xCoord >> 4, tile.zCoord >> 4))
            return false;
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(tile.getBlockType());
        return id != null && ModList.LootGames.getID()
            .equals(id.modId)
            && ("LootGamesMasterBlock".equals(id.name) || "gol_master".equals(id.name)
                || "ms_master".equals(id.name)
                || "sdk_master".equals(id.name));
    }

    /** Copies only tile and block-ID data needed to recognize old structures; never reads neighboring chunks. */
    private static NBTTagCompound snapshot(Chunk chunk) {
        NBTTagCompound level = new NBTTagCompound();
        level.setBoolean("TerrainPopulated", chunk.isTerrainPopulated);
        NBTTagList tiles = new NBTTagList();
        boolean houseChest = false;
        for (Object object : chunk.chunkTileEntityMap.values()) {
            TileEntity tile = (TileEntity) object;
            if (!(tile instanceof TileEntityChest) && !isGameCore(tile)) continue;
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeToNBT(tag);
            tiles.appendTag(tag);
            houseChest |= StructureMarkers.isHouseChest(tag);
        }
        level.setTag("TileEntities", tiles);
        // EndlessIDs deliberately rejects vanilla block-array access. Use the stable block accessor, and only
        // inspect blocks when a surface chest already has the distinctive Roguelike book.
        int bricks = 0;
        if (houseChest) {
            outer: for (int y = 48; y < 256; y++) for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                if (chunk.getBlock(x, y, z) == Blocks.brick_block && ++bricks >= 16) break outer;
            }
        }
        level.setBoolean("GTNGHasBricks", bricks >= 16);
        return level;
    }

    /** A square spiral handles negative coordinates symmetrically and bounds work even in disabled/sparse worlds. */
    static final class Spiral {

        int x, z, dx, dz = -1;
        int visited;

        void advance() {
            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int old = dx;
                dx = -dz;
                dz = old;
            }
            x += dx;
            z += dz;
            visited++;
        }
    }

    /** Per-held-stack search state. No world/seed is sent to clients and no background thread accesses the world. */
    private static final class Search {

        final EntityPlayer player;
        final ItemStack stack;
        final WorldServer world;
        final int kind, centerX, centerZ;
        final double originX, originZ;
        final Spiral spiral = new Spiral();
        final RoguelikeCandidates rogue;
        final LootGamesCandidates loot;
        final File folder;
        int candidateX, candidateZ, scanOffset = -1;
        boolean populated;

        Search(EntityPlayer player, ItemStack stack) throws ReflectiveOperationException {
            this.player = player;
            this.stack = stack;
            world = (WorldServer) player.worldObj;
            kind = StructureCompassItem.mode(stack);
            originX = player.posX;
            originZ = player.posZ;
            centerX = MathHelper.floor_double(originX) >> 4;
            centerZ = MathHelper.floor_double(originZ) >> 4;
            rogue = kind == 0 ? new RoguelikeCandidates(world) : null;
            loot = kind == 1 ? new LootGamesCandidates() : null;
            File root = world.getSaveHandler()
                .getWorldDirectory();
            String dimensionFolder = world.provider.getSaveFolder();
            folder = dimensionFolder == null ? root : new File(root, dimensionFolder);
        }

        boolean enabled() {
            return kind == 0 ? RoguelikeCandidates.enabled(world) : LootGamesCandidates.enabled(world);
        }

        boolean step() throws ReflectiveOperationException, IOException {
            long deadline = System.nanoTime() + 2_000_000L;
            int reads = 0;
            for (int work = 0; work < 2048 && System.nanoTime() < deadline; work++) {
                if (scanOffset >= 0) {
                    // Roguelike can move up to 100 blocks from its seed chunk; LootGames is centered in its chunk.
                    int range = kind == 0 ? 7 : 0, width = range * 2 + 1;
                    int x = candidateX + scanOffset % width - range;
                    int z = candidateZ + scanOffset / width - range;
                    NBTTagCompound level = read(x, z);
                    if (x == candidateX && z == candidateZ)
                        populated = level != null && level.getBoolean("TerrainPopulated");
                    if (level != null) scan(world, level);
                    scanOffset++;
                    if (scanOffset >= width * width) {
                        Location found = StructureLocations.get(world)
                            .nearest(kind, candidateX * 16 + 8, candidateZ * 16 + 8, kind == 0 ? 128 : 24);
                        if (found != null) {
                            setTarget(player, stack, found, true);
                            return true;
                        }
                        if (!populated) {
                            setTarget(
                                player,
                                stack,
                                new Location(kind, candidateX * 16 + 8, 0, candidateZ * 16 + 8),
                                false);
                            return true;
                        }
                        scanOffset = -1;
                    }
                    if (++reads >= 8) break;
                    continue;
                }
                if (spiral.visited >= 1025 * 1025) {
                    // #tr compass.none
                    // # No recorded structure or ungenerated candidate within 8192 blocks. Try from another location.
                    // # zh_CN 8192 格内无已识别遗迹或未生成候选区域，请换个位置再试。
                    player.addChatMessage(new ChatComponentTranslation("compass.none"));
                    return true;
                }
                int x = centerX + spiral.x, z = centerZ + spiral.z;
                spiral.advance();
                double dx = x * 16.0 + 8 - originX, dz = z * 16.0 + 8 - originZ;
                if (dx * dx + dz * dz > (double) RADIUS * RADIUS) continue;
                if (kind == 0 ? rogue.test(x, z) : loot.test(world, x, z)) {
                    candidateX = x;
                    candidateZ = z;
                    scanOffset = 0;
                    populated = false;
                }
            }
            return false;
        }

        private NBTTagCompound read(int x, int z) throws IOException {
            if (world.getChunkProvider()
                .chunkExists(x, z)) {
                return snapshot(
                    world.getChunkProvider()
                        .provideChunk(x, z));
            }
            // RegionFileCache otherwise creates an empty region file even for a read of absent terrain.
            File region = new File(new File(folder, "region"), "r." + (x >> 5) + "." + (z >> 5) + ".mca");
            if (!region.isFile()) return null;
            try (DataInputStream input = RegionFileCache.getChunkInputStream(folder, x, z)) {
                return input == null ? null
                    : CompressedStreamTools.read(input)
                        .getCompoundTag("Level");
            }
        }
    }
}
