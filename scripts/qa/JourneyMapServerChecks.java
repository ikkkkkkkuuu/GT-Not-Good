package com.xyp.gtnotgood.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = "journeymapserverqa", name = "JourneyMap Server QA", version = "1")
public final class JourneyMapServerChecks {

    private int ticks;
    private int deniedTicks;
    private boolean ready;
    private boolean joined;
    private boolean granted;
    private boolean teleported;
    private boolean finished;
    private EntityPlayerMP player;

    @NetworkCheckHandler
    public boolean accept(Map<String, String> mods, Side side) {
        return true;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.journeyMap.remote.server")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        try {
            if (++ticks > 7200) throw new AssertionError("Dedicated QA timed out");
            if (!ready) {
                require(server.isDedicatedServer() && FMLLaunchHandler.side() == Side.SERVER,
                    "physical dedicated SERVER process");
                require(
                    !Loader.isModLoaded(ModList.JourneyMap.getID())
                        && Launch.classLoader.getClassBytes("journeymap.client.ui.fullscreen.Fullscreen") == null,
                    "dedicated server has no JourneyMap");
                require(Launch.classLoader.getClassBytes("com.xyp.gtnotgood.qa.JourneyMapRemoteClientChecks") == null,
                    "server snapshot excludes the client fixture");
                WorldServer world = server.worldServerForDimension(0);
                world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
                for (int x = -45; x <= -21; x++) {
                    for (int z = -59; z <= -35; z++) {
                        world.setBlock(x, 70, z, Blocks.stone, 0, 2);
                        for (int y = 71; y <= 75; y++) world.setBlockToAir(x, y, z);
                    }
                }
                write("ready.txt", "SERVER; no JourneyMap; 127.0.0.1:25576");
                ready = true;
            }
            for (Object value : server.getConfigurationManager().playerEntityList) {
                EntityPlayerMP candidate = (EntityPlayerMP) value;
                if (candidate.getCommandSenderName().equals("JourneyMapQA")) player = candidate;
            }
            if (player != null && !joined) {
                server.getConfigurationManager().func_152610_b(player.getGameProfile());
                player.setGameType(WorldSettings.GameType.CREATIVE);
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.setPositionAndUpdate(-22.5, 71, -36.5);
                require(!server.getConfigurationManager().func_152596_g(player.getGameProfile()),
                    "QA player is not OP");
                write("joined.txt", "-22.5,71.0,-36.5");
                joined = true;
            }
            if (joined && file("deny-attempt.txt").isFile() && !file("denied.txt").isFile()) {
                require(Math.abs(player.posX + 22.5) < 0.01 && Math.abs(player.posY - 71) < 0.01
                    && Math.abs(player.posZ + 36.5) < 0.01, "non-OP teleport leaves the server position unchanged");
                if (++deniedTicks >= 40) write("denied.txt", "PASS: unchanged for 40 server ticks without OP");
            }
            if (joined && file("grant-op.txt").isFile() && !granted) {
                require(file("denied.txt").isFile(), "permission refusal is verified before granting OP");
                server.getConfigurationManager().func_152605_a(player.getGameProfile());
                require(server.getConfigurationManager().func_152596_g(player.getGameProfile()), "QA player is now OP");
                write("op-granted.txt", player.getCommandSenderName());
                granted = true;
            }
            if (granted && file("allowed-attempt.txt").isFile() && !teleported) {
                String[] target = new String(Files.readAllBytes(file("target.txt").toPath()), StandardCharsets.UTF_8)
                    .trim().split(",");
                int x = Integer.parseInt(target[0]);
                int z = Integer.parseInt(target[1]);
                if (MathHelper.floor_double(player.posX) == x && MathHelper.floor_double(player.posZ) == z) {
                    require(
                        Math.abs(player.posX - x - 0.5) < 0.01 && Math.abs(player.posY - 71) < 0.01
                            && Math.abs(player.posZ - z - 0.5) < 0.01,
                        "OP vanilla /tp reaches the exact safe destination");
                    write("teleported.txt", player.posX + "," + player.posY + "," + player.posZ);
                    teleported = true;
                }
            }
            if (file("stop-server.txt").isFile()) {
                require(teleported && file("client-result.txt").isFile()
                    && new String(Files.readAllBytes(file("client-result.txt").toPath()), StandardCharsets.UTF_8).trim()
                        .equals("PASS"),
                    "the remote client also verified the synchronized destination");
                finish(server, "PASS");
            }
        } catch (Throwable error) {
            error.printStackTrace();
            finish(server, "FAIL: " + error);
        }
    }

    private void finish(MinecraftServer server, String result) {
        try {
            if (player != null) server.getConfigurationManager().func_152610_b(player.getGameProfile());
            write("server-result.txt", result);
        } catch (Exception error) {
            error.printStackTrace();
        }
        finished = true;
        server.initiateShutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("JOURNEYMAP_REMOTE_SERVER_QA: " + message);
    }

    private static File file(String name) {
        return new File(System.getProperty("gtng.journeyMap.remote.output"), name);
    }

    private static void write(String name, String text) throws Exception {
        Files.write(file(name).toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
}
