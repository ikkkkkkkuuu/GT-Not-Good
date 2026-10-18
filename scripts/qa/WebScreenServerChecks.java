package com.xyp.gtnotgood.qa;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.xyp.gtnotgood.common.blocks.webscreen.TileWebScreen;
import com.xyp.gtnotgood.common.blocks.webscreen.WebScreenShape;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "gtngwebscreenserverqa",
    name = "Web Screen Dedicated Server QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class WebScreenServerChecks {

    private final List<String> results = new ArrayList<>();
    private final List<UUID> identities = new ArrayList<>();
    private final String phase = System.getProperty("gtng.webscreen.server.phase", "place");
    private int ticks;
    private int x;
    private int z;
    private boolean finished;
    private WorldServer world;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.webscreen.server.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        try {
            if (++ticks == 20) {
                initializeWorld(server);
                if (phase.equals("place")) place();
                else if (phase.equals("reload")) loadState();
                else throw new AssertionError("Unknown QA phase: " + phase);
            }
            if (ticks == 40) {
                if (phase.equals("place")) saveState();
                else verifyReloaded();
                finish(server, "PASS: " + phase);
            }
        } catch (Throwable error) {
            error.printStackTrace();
            finish(server, "FAIL: " + error);
        }
    }

    private void initializeWorld(MinecraftServer server) throws Exception {
        require(server.isDedicatedServer() && FMLLaunchHandler.side() == Side.SERVER,
            "physical dedicated SERVER process");
        world = server.worldServerForDimension(0);
        Field accesses = ReflectionHelper.findField(World.class, "worldAccesses", "field_73021_x");
        boolean logisticsListener = false;
        for (Object access : (List<?>) accesses.get(world)) {
            if (access.getClass().getName().equals("logisticspipes.routing.pathfinder.changedetection.LPWorldAccess")) {
                logisticsListener = true;
            }
        }
        require(logisticsListener, "real LogisticsPipes block update listener attached");
        world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
    }

    private void place() throws Exception {
        x = ((world.getSpawnPoint().posX + 1024) & ~15) + 4;
        z = ((world.getSpawnPoint().posZ + 768) & ~15) + 4;
        Chunk fixture = world.theChunkProviderServer.loadChunk(x >> 4, z >> 4);
        require(fixture != null && !(fixture instanceof EmptyChunk)
            && world.theChunkProviderServer.chunkExists(x >> 4, z >> 4)
            && fixture.isChunkLoaded, "fixture chunk explicitly loaded before placement");
        EntityPlayerMP player = FakePlayerFactory.getMinecraft(world);
        player.setPosition(x + 0.5, 80, z + 2.5);
        ItemStack stack = GTNGItemList.WebScreen.get(1);
        ItemBlock item = (ItemBlock) stack.getItem();
        int[] facings = { 2, 5, 3, 4 };
        for (int direction = 0; direction < facings.length; direction++) {
            player.rotationYaw = direction * 90;
            require(item.placeBlockAt(stack.copy(), player, world, x + direction, 80, z + 4, 1, 0.5F, 1, 0.5F, 3),
                "ItemBlock placement facing " + facings[direction]);
            TileWebScreen tile = tile(x + direction, 80, z + 4);
            require(tile.getFacing() == facings[direction], "placement stores facing " + facings[direction]);
            verifyIdentity(tile);
        }
        player.rotationYaw = 180;
        Set<UUID> unique = new HashSet<>();
        for (int dy = 0; dy < 2; dy++) {
            for (int dx = 0; dx < 4; dx++) {
                require(item.placeBlockAt(stack.copy(), player, world, x + dx, 80 + dy, z, 1, 0.5F, 1, 0.5F, 3),
                    "wall placement " + dx + "," + dy);
                TileWebScreen tile = tile(x + dx, 80 + dy, z);
                verifyIdentity(tile);
                require(unique.add(tile.getScreenId()), "wall block has a unique UUID " + dx + "," + dy);
                identities.add(tile.getScreenId());
            }
        }
        verifyWall();
        NBTTagCompound legacyData = new NBTTagCompound();
        tile(x, 80, z).writeToNBT(legacyData);
        legacyData.removeTag("screenIdMost");
        legacyData.removeTag("screenIdLeast");
        legacyData.setInteger("x", x + 6);
        legacyData.setInteger("z", z + 4);
        require(world.setBlock(x + 6, 80, z + 4, Block.getBlockFromItem(stack.getItem()), 3, 3),
            "legacy fixture block placed");
        TileWebScreen legacy = (TileWebScreen) TileEntity.createAndLoadEntity(legacyData);
        world.setTileEntity(x + 6, 80, z + 4, legacy);
        verifyIdentity(legacy);
        require(unique.add(legacy.getScreenId()), "missing legacy UUID initialized independently");
        identities.add(legacy.getScreenId());
        world.markBlockForUpdate(x + 6, 80, z + 4);
        require(tile(x + 6, 80, z + 4) == legacy, "normal updates retain the registered legacy tile");
    }

    private void verifyIdentity(TileWebScreen tile) throws Exception {
        require(tile.getScreenId() != null, "server initializes screen identity");
        require(!tile.canUpdate(), "screen remains a non-ticking tile");
        UUID identity = tile.getScreenId();
        tile.validate();
        require(identity.equals(tile.getScreenId()), "repeat validation retains screen identity");
        NBTTagCompound saved = new NBTTagCompound();
        tile.writeToNBT(saved);
        require(identity.equals(identity(saved)), "tile NBT retains screen identity");
        S35PacketUpdateTileEntity packet = (S35PacketUpdateTileEntity) tile.getDescriptionPacket();
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writePacketData(buffer);
            require(buffer.readInt() == tile.xCoord && buffer.readShort() == tile.yCoord
                && buffer.readInt() == tile.zCoord
                && buffer.readUnsignedByte() == 0, "description packet carries exact block coordinates");
            NBTTagCompound payload = (NBTTagCompound) ReflectionHelper
                .findField(S35PacketUpdateTileEntity.class, "field_148860_e").get(packet);
            require(identity.equals(identity(payload)), "description packet carries server identity");
        } finally {
            buffer.release();
        }
    }

    private void saveState() throws Exception {
        world.saveAllChunks(true, null);
        ThreadedFileIOBase.threadedIOInstance.waitForFinish();
        Properties state = new Properties();
        state.setProperty("x", Integer.toString(x));
        state.setProperty("z", Integer.toString(z));
        state.setProperty("world", world.getWorldInfo().getWorldName());
        state.setProperty("processStart", Long.toString(ManagementFactory.getRuntimeMXBean().getStartTime()));
        for (int index = 0; index < identities.size(); index++) {
            state.setProperty("screen." + index, identities.get(index).toString());
        }
        Files.createDirectories(statePath().toAbsolutePath().getParent());
        try (OutputStream stream = Files.newOutputStream(statePath())) {
            state.store(stream, "Web screen dedicated server persistence QA");
        }
        require(identities.size() == 9, "saved nine screen identities for the next server process");
    }

    private void loadState() throws Exception {
        Properties state = new Properties();
        try (InputStream stream = Files.newInputStream(statePath())) {
            state.load(stream);
        }
        require(!Long.toString(ManagementFactory.getRuntimeMXBean().getStartTime())
            .equals(state.getProperty("processStart")), "reload runs in a new dedicated server process");
        require(world.getWorldInfo().getWorldName().equals(state.getProperty("world")),
            "reload uses the saved QA world");
        x = Integer.parseInt(state.getProperty("x"));
        z = Integer.parseInt(state.getProperty("z"));
        for (int index = 0; index < 9; index++) identities.add(UUID.fromString(state.getProperty("screen." + index)));
        Chunk loaded = world.theChunkProviderServer.loadChunk(x >> 4, z >> 4);
        require(loaded != null && loaded.isChunkLoaded, "saved fixture chunk loaded through server provider");
        verifyReloaded();
    }

    private Path statePath() {
        String fallback = Paths.get(System.getProperty("gtng.webscreen.server.report")).toAbsolutePath().getParent()
            .resolve("state.properties").toString();
        return Paths.get(System.getProperty("gtng.webscreen.server.state", fallback));
    }

    private void verifyReloaded() throws Exception {
        int index = 0;
        for (int dy = 0; dy < 2; dy++) {
            for (int dx = 0; dx < 4; dx++) verifyReloadedTile(tile(x + dx, 80 + dy, z), index++);
        }
        verifyReloadedTile(tile(x + 6, 80, z + 4), index);
        verifyWall();
    }

    private void verifyReloadedTile(TileWebScreen tile, int index) throws Exception {
        require(identities.get(index).equals(tile.getScreenId()), "saved UUID survives server restart " + index);
        verifyIdentity(tile);
    }

    private void verifyWall() {
        WebScreenShape shape = WebScreenShape.discover(x, 80, z, 3, (px, py, pz) -> {
            if (!world.blockExists(px, py, pz)) return false;
            TileEntity candidate = world.getTileEntity(px, py, pz);
            return candidate instanceof TileWebScreen screen && screen.getFacing() == 3;
        });
        require(
            shape.width == 4 && shape.height == 2 && shape.anchorX == x && shape.anchorY == 80 && shape.anchorZ == z,
            "4 by 2 wall shape survives dedicated server lifecycle");
    }

    private TileWebScreen tile(int px, int py, int pz) {
        TileEntity tile = world.getTileEntity(px, py, pz);
        require(tile instanceof TileWebScreen,
            "registered screen tile at " + px
                + ","
                + py
                + ","
                + pz
                + (tile instanceof TileWebScreen ? ""
                    : "; block=" + Block.blockRegistry.getNameForObject(world.getBlock(px, py, pz))
                        + "; tile="
                        + (tile == null ? "null" : tile.getClass().getName())));
        return (TileWebScreen) tile;
    }

    private static UUID identity(NBTTagCompound data) {
        if (data == null || !data.hasKey("screenIdMost", 4) || !data.hasKey("screenIdLeast", 4)) return null;
        return new UUID(data.getLong("screenIdMost"), data.getLong("screenIdLeast"));
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        results.add("PASS: " + message);
        System.out.println("WEB_SCREEN_SERVER_QA: " + message);
    }

    private void finish(MinecraftServer server, String result) {
        finished = true;
        try {
            Path report = Paths.get(System.getProperty("gtng.webscreen.server.report"));
            Files.createDirectories(report.toAbsolutePath().getParent());
            results.add(0, result);
            Files.write(report, results, StandardCharsets.UTF_8);
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("WEB_SCREEN_SERVER_QA: " + result + "; " + (results.size() - 1) + " checks");
        server.initiateShutdown();
    }
}
