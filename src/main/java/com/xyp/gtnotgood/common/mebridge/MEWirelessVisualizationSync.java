package com.xyp.gtnotgood.common.mebridge;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtnotgood.GTNotGood;

/** Rate-limited server snapshots for the held transceiver's nearby-node visualisation. */
final class MEWirelessVisualizationSync {

    static final int MAX_VISIBLE_NODES = 2048;
    private static final int VISIBILITY_RADIUS = 96;
    private static final int UPDATE_INTERVAL = 40;
    private static final Map<EntityPlayerMP, LastUpdate> LAST_UPDATES = new WeakHashMap<>();

    private MEWirelessVisualizationSync() {}

    static void update(EntityPlayerMP player) {
        long tick = player.worldObj.getTotalWorldTime();
        int dimension = player.worldObj.provider.dimensionId;
        int chunkX = ((int) Math.floor(player.posX)) >> 4;
        int chunkZ = ((int) Math.floor(player.posZ)) >> 4;
        long revision = MEWirelessLinkManager.getLinkRevision();
        LastUpdate previous = LAST_UPDATES.get(player);
        if (previous != null && previous.matches(dimension, chunkX, chunkZ, revision, tick)) return;

        int[] positions = MEWirelessLinkManager.nearbyTargets(player, VISIBILITY_RADIUS, MAX_VISIBLE_NODES);
        GTNotGood.channel.sendTo(new MessageMEWirelessVisualization(dimension, positions), player);
        LAST_UPDATES.put(player, new LastUpdate(dimension, chunkX, chunkZ, revision, tick));
    }

    private static final class LastUpdate {

        private final int dimension;
        private final int chunkX;
        private final int chunkZ;
        private final long revision;
        private final long tick;

        private LastUpdate(int dimension, int chunkX, int chunkZ, long revision, long tick) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.revision = revision;
            this.tick = tick;
        }

        private boolean matches(int currentDimension, int currentChunkX, int currentChunkZ, long currentRevision,
            long currentTick) {
            return dimension == currentDimension && chunkX == currentChunkX
                && chunkZ == currentChunkZ
                && revision == currentRevision
                && currentTick >= tick
                && currentTick - tick < UPDATE_INTERVAL;
        }
    }
}
