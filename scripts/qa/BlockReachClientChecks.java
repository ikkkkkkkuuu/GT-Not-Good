package com.xyp.gtnotgood.qa;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.gtnewhorizon.gtnhlib.config.ConfigSyncHandler;
import com.gtnewhorizon.gtnhlib.config.PacketSyncConfig;
import com.xyp.gtnotgood.config.InteractionConfig;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "blockreachclientqa",
    name = "Block Reach Client QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class BlockReachClientChecks {

    private boolean started;
    private volatile boolean serverVerified;
    private volatile boolean finished;
    private int ticks;
    private int serverTicks;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.blockreach.qa") && !Boolean.getBoolean("gtng.blockreach.serverQA"))
            FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
        if (finished) {
            FMLCommonHandler.instance().exitJava(0, false);
            return;
        }
        try {
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("block-reach-qa-" + System.currentTimeMillis(), "Block Reach QA",
                    new WorldSettings(74109L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (++ticks > 3000) throw new AssertionError("block reach client QA timed out");
            if (serverVerified && mc.thePlayer != null && mc.theWorld != null) {
                verifyPicking(mc);
                writeResult("PASS");
                System.out.println("BLOCK_REACH_CLIENT_QA: PASS (" + checks + " client checks)");
                finished = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || serverVerified || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        if (++serverTicks < 60) return;
        try {
            EntityPlayerMP connectedPlayer = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            equal(16, connectedPlayer.theItemInWorldManager.getBlockReachDistance(),
                "logged-in player's reach survives normal player ticks");
            int verified = new BlockReachServerChecks().verify(server);
            System.out.println("BLOCK_REACH_INTEGRATED_SERVER_QA: PASS (" + verified + " checks)");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            serverVerified = true;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void verifyPicking(Minecraft mc) throws Exception {
        int x = (int) Math.floor(mc.thePlayer.posX), z = (int) Math.floor(mc.thePlayer.posZ), y = 82;
        double original = InteractionConfig.blockReachDistance;
        try {
            mc.displayGuiScreen(null);
            mc.renderViewEntity = mc.thePlayer;
            for (int dz = -1; dz <= 18; dz++)
                for (int dy = -1; dy <= 2; dy++) mc.theWorld.setBlockToAir(x, y + dy, z + dz);
            for (WorldSettings.GameType mode : new WorldSettings.GameType[] { WorldSettings.GameType.SURVIVAL,
                WorldSettings.GameType.CREATIVE }) {
                mc.playerController.setGameType(mode);
                equal(16, mc.playerController.getBlockReachDistance(), mode + " client configured reach");
                require(mc.theWorld.setBlock(x, y, z + 16, Blocks.stone), "loaded picking fixture");
                position(mc, x + 0.5, y + 0.5, z + 0.5);
                mc.entityRenderer.getMouseOver(1);
                require(blockAt(mc.objectMouseOver, x, y, z + 16), mode + " picks block 15.5 blocks away");
                position(mc, x + 0.5, y + 0.5, z - 0.01);
                mc.entityRenderer.getMouseOver(1);
                require(!blockAt(mc.objectMouseOver, x, y, z + 16), mode + " rejects block face beyond sixteen blocks");
                position(mc, x + 0.5, y + 0.5, z + 0.5);
                mc.theWorld.setBlock(x, y, z + 4, Blocks.stone);
                mc.entityRenderer.getMouseOver(1);
                require(blockAt(mc.objectMouseOver, x, y, z + 4), mode + " nearer wall occludes distant block");
                mc.theWorld.setBlockToAir(x, y, z + 4);
                mc.theWorld.setBlockToAir(x, y, z + 16);
                verifyEntityTargeting(mc, x, y, z, mode);
            }
            verifyItemRay(mc, x, y, z);
            InteractionConfig.blockReachDistance = 6.5;
            mc.playerController.setGameType(WorldSettings.GameType.SURVIVAL);
            equal(6.5, mc.playerController.getBlockReachDistance(), "client reads non-integer configured reach");
            position(mc, x + 0.5, y + 0.5, z + 0.5);
            mc.theWorld.setBlock(x, y, z + 6, Blocks.stone);
            mc.entityRenderer.getMouseOver(1);
            require(blockAt(mc.objectMouseOver, x, y, z + 6), "custom shorter reach accepts nearer block");
            mc.theWorld.setBlockToAir(x, y, z + 6);
            mc.theWorld.setBlock(x, y, z + 8, Blocks.stone);
            mc.entityRenderer.getMouseOver(1);
            require(!blockAt(mc.objectMouseOver, x, y, z + 8), "custom shorter reach rejects farther block");
            mc.theWorld.setBlockToAir(x, y, z + 8);
            InteractionConfig.blockReachDistance = 2;
            equal(5, mc.playerController.getBlockReachDistance(), "manual config cannot shorten entity reach");
        } finally {
            InteractionConfig.blockReachDistance = original;
        }
        verifyConfigSync(mc);
    }

    private void verifyItemRay(Minecraft mc, int x, int y, int z) {
        RayItem item = new RayItem();
        position(mc, x + 0.5, y + 0.5, z + 0.5);
        mc.theWorld.setBlock(x, y, z + 15, Blocks.water, 0, 3);
        require(blockAt(item.trace(mc.theWorld, mc.thePlayer), x, y, z + 15),
            "native item liquid ray reaches source beyond vanilla five blocks");
        mc.theWorld.setBlockToAir(x, y, z + 15);
        mc.theWorld.setBlock(x, y, z + 16, Blocks.water, 0, 3);
        position(mc, x + 0.5, y + 0.5, z - 0.01);
        require(!blockAt(item.trace(mc.theWorld, mc.thePlayer), x, y, z + 16),
            "native item liquid ray rejects source beyond configured sixteen blocks");
        mc.theWorld.setBlockToAir(x, y, z + 16);
    }

    private static final class RayItem extends Item {

        private MovingObjectPosition trace(World world, EntityPlayer player) {
            return getMovingObjectPositionFromPlayer(world, player, true);
        }
    }

    private void verifyConfigSync(Minecraft mc) throws Exception {
        equal(16, InteractionConfig.blockReachDistance, "local value before remote synchronization");
        PacketSyncConfig outbound = new PacketSyncConfig();
        String key = InteractionConfig.class.getField("blockReachDistance").toString();
        outbound.syncedElements.put(key, "6.5");
        PacketBuffer bytes = new PacketBuffer(Unpooled.buffer());
        try {
            outbound.encode(bytes);
            PacketSyncConfig inbound = new PacketSyncConfig();
            inbound.decode(bytes);
            require("6.5".equals(inbound.syncedElements.get(key)), "GTNHLib configuration packet round trip");
            inbound.executeClient(mc.getNetHandler());
            equal(6.5, InteractionConfig.blockReachDistance, "registered field accepts actual remote config handler");
            equal(6.5, mc.playerController.getBlockReachDistance(), "client getter follows synchronized server value");
        } finally {
            bytes.release();
            new ConfigSyncHandler().onClientDisconnect(
                new FMLNetworkEvent.ClientDisconnectionFromServerEvent(mc.getNetHandler().getNetworkManager()));
        }
        equal(16, InteractionConfig.blockReachDistance, "actual GTNHLib disconnect restores local configuration");
        equal(16, mc.playerController.getBlockReachDistance(), "client getter restores local value after disconnect");
    }

    private void verifyEntityTargeting(Minecraft mc, int x, int y, int z, WorldSettings.GameType mode) {
        EntityPig pig = new EntityPig(mc.theWorld);
        pig.setPosition(x + 0.5, y, z + (mode.isCreative() ? 7.5 : 4.5));
        mc.theWorld.addEntityToWorld(-9123, pig);
        mc.entityRenderer.getMouseOver(1);
        require(mc.objectMouseOver == null || mc.objectMouseOver.entityHit != pig,
            mode + " entity targeting does not inherit sixteen-block block reach");
        pig.setPosition(x + 0.5, y, z + (mode.isCreative() ? 5.5 : 2.5));
        mc.entityRenderer.getMouseOver(1);
        require(mc.objectMouseOver != null && mc.objectMouseOver.entityHit == pig,
            mode + " normal entity targeting still works");
        mc.theWorld.removeEntityFromWorld(-9123);
    }

    private static void position(Minecraft mc, double x, double y, double z) {
        mc.thePlayer.setPosition(x, y, z);
        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 0;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 0;
    }

    private static boolean blockAt(MovingObjectPosition hit, int x, int y, int z) {
        return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && hit.blockX == x
            && hit.blockY == y
            && hit.blockZ == z;
    }

    private static void writeResult(String value) throws Exception {
        Files.write(Path.of(System.getProperty("gtng.blockreach.qa.output"), "result.txt"),
            value.getBytes(StandardCharsets.UTF_8));
    }

    private static void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            writeResult("FAIL: " + failure);
        } catch (Exception writeFailure) {
            failure.addSuppressed(writeFailure);
        }
        FMLCommonHandler.instance().exitJava(1, false);
    }

    private void equal(double expected, double actual, String message) {
        require(Math.abs(expected - actual) < 0.000001, message + ": " + actual);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
