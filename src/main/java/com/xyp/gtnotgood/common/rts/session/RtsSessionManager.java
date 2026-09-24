package com.xyp.gtnotgood.common.rts.session;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.packet.RtsSessionMessage;
import com.xyp.gtnotgood.config.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Owns RTS leases on the server thread. Connection identity, generation tombstones and an idle timeout
 * prevent old packets from reviving an exited session. No camera entity or player teleport is needed.
 */
public final class RtsSessionManager {

    public static final RtsSessionManager INSTANCE = new RtsSessionManager();
    public static final int TIMEOUT_TICKS = 200;
    private static final Map<EntityPlayerMP, RtsSessionMessage> PENDING = new ConcurrentHashMap<>();
    private final Map<EntityPlayerMP, Entry> sessions = new IdentityHashMap<>();
    private long ticks;

    private RtsSessionManager() {}

    /**
     * Coalesces to one request per connection. A newer generation wins, and close dominates other
     * messages in its generation, including heartbeats already in flight.
     *
     * @param player  authenticated connection owner supplied by FML
     * @param message decoded control request
     */
    public static void enqueue(EntityPlayerMP player, RtsSessionMessage message) {
        if (player == null || message.action < RtsSessionMessage.OPEN || message.action > RtsSessionMessage.HEARTBEAT)
            return;
        PENDING.merge(player, message, (old, next) -> {
            if (old.requestId > next.requestId) return old;
            if (old.requestId == next.requestId && old.action == RtsSessionMessage.CLOSE) return old;
            return next;
        });
    }

    /** Returns the current lease for a server-thread action handler, or null when revoked/expired. */
    public RtsSessionLease getLease(EntityPlayerMP player) {
        Entry entry = sessions.get(player);
        return isLive(player, entry) ? entry.lease : null;
    }

    private boolean isLive(EntityPlayerMP player, Entry entry) {
        return player != null && entry != null
            && entry.lease != null
            && Config.enableRTSBuilding
            && player.isEntityAlive()
            && player.dimension == entry.lease.dimension
            && ticks - entry.lastSeen <= TIMEOUT_TICKS;
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        ticks++;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            clear();
            return;
        }
        PENDING.forEach((player, message) -> {
            if (PENDING.remove(player, message) && server.getConfigurationManager().playerEntityList.contains(player)) {
                process(player, message);
            }
        });
        Iterator<Map.Entry<EntityPlayerMP, Entry>> iterator = sessions.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<EntityPlayerMP, Entry> item = iterator.next();
            EntityPlayerMP player = item.getKey();
            Entry entry = item.getValue();
            if (!server.getConfigurationManager().playerEntityList.contains(player)) {
                iterator.remove();
            } else if (entry.lease != null && (!Config.enableRTSBuilding || !player.isEntityAlive()
                || player.dimension != entry.lease.dimension
                || ticks - entry.lastSeen > TIMEOUT_TICKS)) {
                    revoke(player, entry);
                }
        }
    }

    private void process(EntityPlayerMP player, RtsSessionMessage message) {
        Entry entry = sessions.computeIfAbsent(player, ignored -> new Entry());
        if (message.requestId < entry.generation) return;
        if (entry.lease != null && !isLive(player, entry)) revoke(player, entry);
        if (message.action == RtsSessionMessage.OPEN) {
            if (message.requestId == entry.generation) {
                if (entry.lease != null) reply(player, RtsSessionMessage.grant(entry.lease));
                else reply(
                    player,
                    RtsSessionMessage.control(RtsSessionMessage.CLOSED, message.requestId, 0, player.dimension));
                return;
            }
            entry.generation = message.requestId;
            entry.lease = null;
            if (!Config.enableRTSBuilding || player.worldObj == null
                || !player.isEntityAlive()
                || player.dimension != message.dimension
                || player.isPlayerSleeping()
                || player.ridingEntity != null
                || !Double.isFinite(player.posX)
                || !Double.isFinite(player.posY)
                || !Double.isFinite(player.posZ)
                || Math.abs(player.posX) >= 30000000
                || Math.abs(player.posZ) >= 30000000
                || player.posY < -4096
                || player.posY > 4096) {
                reply(
                    player,
                    RtsSessionMessage.control(RtsSessionMessage.DENIED, message.requestId, 0, player.dimension));
                return;
            }
            long token;
            do {
                token = UUID.randomUUID()
                    .getMostSignificantBits();
            } while (token == 0);
            entry.lease = new RtsSessionLease(
                message.requestId,
                token,
                player.dimension,
                Math.floor(player.posX) + 0.5,
                player.posY,
                Math.floor(player.posZ) + 0.5,
                Config.rtsMaxDistance);
            entry.lastSeen = ticks;
            reply(player, RtsSessionMessage.grant(entry.lease));
        } else if (message.action == RtsSessionMessage.CLOSE) {
            if (message.requestId > entry.generation) {
                // Close may arrive before a queued open is processed. Keep the tombstone.
                entry.generation = message.requestId;
                revoke(player, entry);
            } else if (entry.lease == null || message.token == 0 || message.token == entry.lease.token) {
                revoke(player, entry);
            }
        } else if (entry.lease != null && message.requestId == entry.generation
            && message.token == entry.lease.token
            && message.dimension == entry.lease.dimension) {
                entry.lastSeen = ticks;
                reply(player, RtsSessionMessage.grant(entry.lease));
            }
    }

    private void revoke(EntityPlayerMP player, Entry entry) {
        long token = entry.lease == null ? 0 : entry.lease.token;
        int dimension = entry.lease == null ? player.dimension : entry.lease.dimension;
        entry.lease = null;
        reply(player, RtsSessionMessage.control(RtsSessionMessage.CLOSED, entry.generation, token, dimension));
    }

    private static void reply(EntityPlayerMP player, RtsSessionMessage message) {
        GTNotGood.channel.sendTo(message, player);
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.player);
        sessions.remove(event.player);
    }

    @SubscribeEvent
    public void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        closePlayer(event.player);
    }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        closePlayer(event.player);
    }

    @SubscribeEvent
    public void death(LivingDeathEvent event) {
        if (!event.entityLiving.worldObj.isRemote) closePlayer(event.entityLiving);
    }

    private void closePlayer(Object player) {
        PENDING.remove(player);
        Entry entry = sessions.get(player);
        if (entry != null && entry.lease != null) revoke((EntityPlayerMP) player, entry);
    }

    /** Called by the mod's server-stopped lifecycle, including integrated-server world switches. */
    public void clear() {
        PENDING.clear();
        sessions.clear();
        ticks = 0;
    }

    /** One connection's tombstone survives closing until that connection leaves. */
    private static final class Entry {

        private long generation;
        private long lastSeen;
        private RtsSessionLease lease;
    }
}
