package com.xyp.gtnotgood.common.wireless.monitor;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.network.WirelessMonitorSnapshot;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.misc.spaceprojects.SpaceProjectManager;

/** Services bounded, rate-limited requests on the server thread; disabling the HUD stops client polling. */
public final class WirelessMonitorService {

    private static final Map<EntityPlayerMP, Long> PENDING = new ConcurrentHashMap<>();
    private static final Map<EntityPlayerMP, Long> LAST_REQUEST = new WeakHashMap<>();

    public static void enqueue(EntityPlayerMP player, long requestId) {
        PENDING.put(player, requestId);
    }

    public static void reset() {
        PENDING.clear();
        LAST_REQUEST.clear();
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.player);
        LAST_REQUEST.remove(event.player);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServerForDimension(0) == null) return;
        long tick = server.worldServerForDimension(0)
            .getTotalWorldTime();
        PENDING.forEach((player, requestId) -> {
            if (!PENDING.remove(player, requestId)) return;
            if (!server.getConfigurationManager().playerEntityList.contains(player)) return;
            Long previous = LAST_REQUEST.get(player);
            // Allow normal 100t polling despite arrival jitter, while bounding malicious requests to once per second.
            if (previous != null && tick >= previous && tick - previous < 20) return;
            LAST_REQUEST.put(player, tick);
            GTNotGood.channel.sendTo(
                new WirelessMonitorSnapshot(
                    requestId,
                    tick,
                    SpaceProjectManager.getLeader(player.getUniqueID()),
                    WirelessNetworkManager.getUserEU(player.getUniqueID())),
                player);
        });
    }
}
