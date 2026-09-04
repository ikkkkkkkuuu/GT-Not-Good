package com.xyp.gtnotgood.common.mebridge;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.GTNotGood;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnection;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** Server-owned persistent wireless links created by {@link ItemMEWirelessTransceiver}. */
public final class MEWirelessLinkManager extends WorldSavedData {

    public static final String DATA_NAME = "GTNG_MEWirelessLinkData";
    private static final String KEY_LINKS = "links";
    private static final int REFRESH_INTERVAL = 40;
    private static final int CHECKS_PER_TICK = 32;
    private static final int RECONNECTS_PER_TICK = 8;

    private static MEWirelessLinkManager instance;
    private static boolean loaded;
    private static int reconnectTicker;
    private static final Map<TargetKey, IGridConnection> ACTIVE_CONNECTIONS = new HashMap<>();

    private final Map<TargetKey, Link> links = new HashMap<>();
    private final Map<ChunkKey, Set<TargetKey>> linksByChunk = new HashMap<>();
    private final Deque<TargetKey> refreshQueue = new ArrayDeque<>();
    private final Map<String, IGridNode> senderNodeCache = new HashMap<>();
    private long linkRevision;

    public MEWirelessLinkManager() {
        super(DATA_NAME);
    }

    public MEWirelessLinkManager(String name) {
        super(name);
    }

    public static void loadInstance(World world) {
        if (loaded || world == null || world.isRemote) return;
        MapStorage storage = world.mapStorage;
        instance = (MEWirelessLinkManager) storage.loadData(MEWirelessLinkManager.class, DATA_NAME);
        if (instance == null) {
            instance = new MEWirelessLinkManager();
            storage.setData(DATA_NAME, instance);
        }
        loaded = true;
    }

    public static void reset() {
        destroyAllRuntimeConnections();
        instance = null;
        loaded = false;
        reconnectTicker = 0;
    }

    public static BindResult toggle(EntityPlayerMP player, World world, int x, int y, int z, int side,
        String channelName) {
        if (player == null || world == null || world.isRemote) return BindResult.INVALID_TARGET;
        String channel = MEBridgeChannelName.normalize(channelName);
        if (channel.isEmpty() || !MEBridgeChannelName.isValid(channel)) return BindResult.NO_CHANNEL;
        if (instance == null) loadInstance(world);
        if (instance == null) return BindResult.FAILED;

        ForgeDirection direction = ForgeDirection.getOrientation(side);
        ResolvedTarget target = resolveTarget(world, x, y, z, direction);
        if (target == null || target.node == null) return BindResult.INVALID_TARGET;

        TargetKey key = new TargetKey(world.provider.dimensionId, x, y, z);
        Link existing = instance.links.get(key);
        if (existing != null && existing.channel.equals(channel)) {
            instance.remove(key);
            return BindResult.DISCONNECTED;
        }

        int authorizerPlayerId = AEApi.instance()
            .registries()
            .players()
            .getID(player);
        if (authorizerPlayerId < 0) return BindResult.FAILED;

        instance.destroyRuntimeConnection(key);
        Link link = new Link(
            key,
            direction.ordinal(),
            channel,
            target.signature,
            target.node.getPlayerID(),
            authorizerPlayerId,
            player.getUniqueID()
                .toString());
        instance.putLink(link);
        instance.markDirty();

        ConnectResult result = instance.tryConnect(link, target);
        if (result == ConnectResult.FAILED) {
            instance.remove(key);
            return BindResult.FAILED;
        }
        return result == ConnectResult.CONNECTED ? BindResult.CONNECTED : BindResult.WAITING;
    }

    public static void onServerTick() {
        if (instance == null) return;
        if (instance.refreshQueue.isEmpty()) {
            if (++reconnectTicker < REFRESH_INTERVAL) return;
            reconnectTicker = 0;
            instance.refreshQueue.addAll(instance.links.keySet());
        }
        instance.refreshConnectionsBatch();
    }

    public static void onChunkUnload(int dimension, int chunkX, int chunkZ) {
        if (instance == null) return;
        Set<TargetKey> keys = instance.linksByChunk.get(new ChunkKey(dimension, chunkX, chunkZ));
        if (keys == null) return;
        for (TargetKey key : keys) {
            instance.destroyRuntimeConnection(key);
        }
    }

    public static void onDimensionUnload(int dimension) {
        if (instance == null) return;
        for (TargetKey key : instance.links.keySet()) {
            if (key.dimension == dimension) instance.destroyRuntimeConnection(key);
        }
    }

    public static void removeAt(int dimension, int x, int y, int z) {
        if (instance != null) instance.remove(new TargetKey(dimension, x, y, z));
    }

    static Inspection inspect(World world, int x, int y, int z, int side) {
        if (world == null || world.isRemote) return Inspection.INVALID;
        ResolvedTarget target = resolveTarget(world, x, y, z, ForgeDirection.getOrientation(side));
        if (target == null) return Inspection.INVALID;
        if (instance == null) loadInstance(world);
        if (instance == null) return new Inspection(true, "");

        Link link = instance.links.get(new TargetKey(world.provider.dimensionId, x, y, z));
        return new Inspection(true, link == null ? "" : link.channel);
    }

    static long getLinkRevision() {
        return instance == null ? 0L : instance.linkRevision;
    }

    static void onChannelColorChanged() {
        if (instance != null) instance.linkRevision++;
    }

    static int[] nearbyTargets(EntityPlayerMP player, int radius, int limit) {
        if (instance == null || player == null || player.worldObj == null || radius <= 0 || limit <= 0) {
            return new int[0];
        }

        int dimension = player.worldObj.provider.dimensionId;
        int centerChunkX = MathHelper.floor_double(player.posX) >> 4;
        int centerChunkZ = MathHelper.floor_double(player.posZ) >> 4;
        int chunkRadius = (radius + 15) >> 4;
        double radiusSquared = (double) radius * radius;
        int[] positions = new int[limit * MessageMEWirelessVisualization.VALUES_PER_NODE];
        Map<String, Integer> channelColors = new HashMap<>();
        int count = 0;

        // Visit nearer chunk rings first so a pathological over-limit area still shows the closest nodes.
        for (int ring = 0; ring <= chunkRadius && count < limit; ring++) {
            for (int offsetX = -ring; offsetX <= ring && count < limit; offsetX++) {
                for (int offsetZ = -ring; offsetZ <= ring && count < limit; offsetZ++) {
                    if (Math.max(Math.abs(offsetX), Math.abs(offsetZ)) != ring) continue;
                    int chunkX = centerChunkX + offsetX;
                    int chunkZ = centerChunkZ + offsetZ;
                    if (!player.worldObj.getChunkProvider()
                        .chunkExists(chunkX, chunkZ)) {
                        continue;
                    }

                    Set<TargetKey> chunkLinks = instance.linksByChunk.get(new ChunkKey(dimension, chunkX, chunkZ));
                    if (chunkLinks == null) continue;
                    for (TargetKey key : chunkLinks) {
                        Link link = instance.links.get(key);
                        if (link == null) continue;
                        double dx = key.x + 0.5D - player.posX;
                        double dy = key.y + 0.5D - player.posY;
                        double dz = key.z + 0.5D - player.posZ;
                        if (dx * dx + dy * dy + dz * dz > radiusSquared) continue;

                        Integer color = channelColors.get(link.channel);
                        if (color == null) {
                            MEBridgeChannelInfo channel = MEBridgeChannelManager.get(link.channel);
                            color = channel == null ? MEBridgeChannelColor.defaultFor(link.channel) : channel.color;
                            channelColors.put(link.channel, color);
                        }
                        int index = count * MessageMEWirelessVisualization.VALUES_PER_NODE;
                        positions[index] = key.x;
                        positions[index + 1] = key.y;
                        positions[index + 2] = key.z;
                        positions[index + 3] = color;
                        if (++count >= limit) break;
                    }
                }
            }
        }
        return count == limit ? positions
            : Arrays.copyOf(positions, count * MessageMEWirelessVisualization.VALUES_PER_NODE);
    }

    private void refreshConnectionsBatch() {
        senderNodeCache.clear();
        int checks = 0;
        int reconnects = 0;
        while (checks < CHECKS_PER_TICK && !refreshQueue.isEmpty()) {
            TargetKey key = refreshQueue.removeFirst();
            Link link = links.get(key);
            checks++;
            if (link == null) continue;

            if (refreshConnection(key, link)) {
                reconnects++;
                if (reconnects >= RECONNECTS_PER_TICK) break;
            }
        }
        senderNodeCache.clear();
    }

    /** @return true when this check attempted an AE2 topology-changing reconnect. */
    private boolean refreshConnection(TargetKey key, Link link) {
        WorldServer world = DimensionManager.getWorld(key.dimension);
        if (world == null || !world.getChunkProvider()
            .chunkExists(key.x >> 4, key.z >> 4)) {
            destroyRuntimeConnection(key);
            return false;
        }

        TileEntity tile = world.getTileEntity(key.x, key.y, key.z);
        if (tile == null) {
            remove(key);
            return false;
        }

        ResolvedTarget target = resolveTarget(world, key.x, key.y, key.z, ForgeDirection.getOrientation(link.side));
        if (target == null) {
            destroyRuntimeConnection(key);
            return false;
        }
        if (!link.targetSignature.equals(target.signature)
            || link.targetPlayerId >= 0 && target.node.getPlayerID() != link.targetPlayerId) {
            remove(key);
            return false;
        }

        IGridNode senderNode = cachedSenderNode(link.channel);
        IGridConnection active = ACTIVE_CONNECTIONS.get(key);
        if (isCurrent(active, target.node, senderNode)) return false;

        destroyRuntimeConnection(key);
        if (senderNode == null || senderNode == target.node) return false;
        tryConnect(link, target, senderNode);
        return true;
    }

    private ConnectResult tryConnect(Link link, ResolvedTarget target) {
        MEBridgeChannelInfo channel = MEBridgeChannelManager.get(link.channel);
        return tryConnect(link, target, senderNode(channel));
    }

    private ConnectResult tryConnect(Link link, ResolvedTarget target, IGridNode senderNode) {
        if (senderNode == null || target == null || target.node == null || senderNode == target.node) {
            return ConnectResult.WAITING;
        }

        int originalPlayerId = target.node.getPlayerID();
        boolean temporaryIdentity = originalPlayerId >= 0 && link.authorizerPlayerId >= 0
            && originalPlayerId != link.authorizerPlayerId;
        try {
            if (temporaryIdentity) target.node.setPlayerID(link.authorizerPlayerId);
            IGridConnection connection = AEApi.instance()
                .createGridConnection(target.node, senderNode);
            ACTIVE_CONNECTIONS.put(link.key, connection);
            return ConnectResult.CONNECTED;
        } catch (FailedConnection exception) {
            GTNotGood.LOG.debug(
                "[MEWirelessLink] failed to connect ({},{},{},{}) to channel '{}': {}",
                link.key.dimension,
                link.key.x,
                link.key.y,
                link.key.z,
                link.channel,
                exception.getClass()
                    .getSimpleName());
            return ConnectResult.FAILED;
        } finally {
            if (temporaryIdentity) target.node.setPlayerID(originalPlayerId);
        }
    }

    private static IGridNode senderNode(MEBridgeChannelInfo channel) {
        if (channel == null) return null;
        TileMEBridgeSender sender = channel.getSenderTile();
        return sender == null ? null : sender.getGridNode(ForgeDirection.UNKNOWN);
    }

    private IGridNode cachedSenderNode(String channelName) {
        if (senderNodeCache.containsKey(channelName)) return senderNodeCache.get(channelName);
        IGridNode node = senderNode(MEBridgeChannelManager.get(channelName));
        senderNodeCache.put(channelName, node);
        return node;
    }

    private static boolean isCurrent(IGridConnection connection, IGridNode targetNode, IGridNode senderNode) {
        if (connection == null || targetNode == null || senderNode == null) return false;
        boolean hasTargetEndpoint = connection.a() == targetNode || connection.b() == targetNode;
        boolean hasSenderEndpoint = connection.a() == senderNode || connection.b() == senderNode;
        return hasTargetEndpoint && hasSenderEndpoint
            && targetNode.getConnections()
                .contains(connection);
    }

    private void putLink(Link link) {
        links.put(link.key, link);
        linksByChunk.computeIfAbsent(ChunkKey.from(link.key), ignored -> new HashSet<>())
            .add(link.key);
        linkRevision++;
    }

    private void remove(TargetKey key) {
        destroyRuntimeConnection(key);
        if (links.remove(key) == null) return;
        ChunkKey chunkKey = ChunkKey.from(key);
        Set<TargetKey> chunkLinks = linksByChunk.get(chunkKey);
        if (chunkLinks != null) {
            chunkLinks.remove(key);
            if (chunkLinks.isEmpty()) linksByChunk.remove(chunkKey);
        }
        linkRevision++;
        markDirty();
    }

    private void destroyRuntimeConnection(TargetKey key) {
        IGridConnection connection = ACTIVE_CONNECTIONS.remove(key);
        if (connection == null) return;
        try {
            connection.destroy();
        } catch (RuntimeException exception) {
            GTNotGood.LOG.debug("[MEWirelessLink] connection was already destroyed");
        }
    }

    private static void destroyAllRuntimeConnections() {
        for (IGridConnection connection : ACTIVE_CONNECTIONS.values()) {
            try {
                connection.destroy();
            } catch (RuntimeException ignored) {}
        }
        ACTIVE_CONNECTIONS.clear();
    }

    private static ResolvedTarget resolveTarget(World world, int x, int y, int z, ForgeDirection side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile == null) return null;

        IGridHost host = tile instanceof IGridHost ? (IGridHost) tile : null;
        String signature = tile.getClass()
            .getName();
        if (tile instanceof IGregTechTileEntity) {
            IMetaTileEntity metaTile = ((IGregTechTileEntity) tile).getMetaTileEntity();
            if (metaTile != null) {
                signature += "|" + metaTile.getClass()
                    .getName();
                if (metaTile instanceof IGridHost) host = (IGridHost) metaTile;
            }
        }
        if (host == null) return null;

        ForgeDirection requestedSide = side == null ? ForgeDirection.UNKNOWN : side;
        IGridNode node = host.getGridNode(requestedSide);
        if (node == null && requestedSide != ForgeDirection.UNKNOWN) node = host.getGridNode(ForgeDirection.UNKNOWN);
        return node == null ? null : new ResolvedTarget(node, signature);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        links.clear();
        linksByChunk.clear();
        refreshQueue.clear();
        senderNodeCache.clear();
        linkRevision = 0L;
        NBTTagList list = nbt.getTagList(KEY_LINKS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            try {
                TargetKey key = new TargetKey(
                    tag.getInteger("dim"),
                    tag.getInteger("x"),
                    tag.getInteger("y"),
                    tag.getInteger("z"));
                String channel = MEBridgeChannelName.normalize(tag.getString("channel"));
                String signature = tag.getString("target_signature");
                int side = tag.getInteger("side");
                if (channel.isEmpty() || !MEBridgeChannelName.isValid(channel)
                    || signature.isEmpty()
                    || side < 0
                    || side >= ForgeDirection.values().length) {
                    continue;
                }
                putLink(
                    new Link(
                        key,
                        side,
                        channel,
                        signature,
                        tag.getInteger("target_player_id"),
                        tag.getInteger("authorizer_player_id"),
                        tag.getString("authorizer_uuid")));
            } catch (RuntimeException exception) {
                GTNotGood.LOG.warn("[MEWirelessLink] skipping invalid saved link {}", i);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Link link : links.values()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dim", link.key.dimension);
            tag.setInteger("x", link.key.x);
            tag.setInteger("y", link.key.y);
            tag.setInteger("z", link.key.z);
            tag.setInteger("side", link.side);
            tag.setString("channel", link.channel);
            tag.setString("target_signature", link.targetSignature);
            tag.setInteger("target_player_id", link.targetPlayerId);
            tag.setInteger("authorizer_player_id", link.authorizerPlayerId);
            tag.setString("authorizer_uuid", link.authorizerUuid);
            list.appendTag(tag);
        }
        nbt.setTag(KEY_LINKS, list);
    }

    public enum BindResult {
        CONNECTED,
        WAITING,
        DISCONNECTED,
        NO_CHANNEL,
        INVALID_TARGET,
        FAILED
    }

    static final class Inspection {

        private static final Inspection INVALID = new Inspection(false, "");

        final boolean validTarget;
        final String channel;

        private Inspection(boolean validTarget, String channel) {
            this.validTarget = validTarget;
            this.channel = channel == null ? "" : channel;
        }
    }

    private enum ConnectResult {
        CONNECTED,
        WAITING,
        FAILED
    }

    private static final class ResolvedTarget {

        private final IGridNode node;
        private final String signature;

        private ResolvedTarget(IGridNode node, String signature) {
            this.node = node;
            this.signature = signature;
        }
    }

    private static final class Link {

        private final TargetKey key;
        private final int side;
        private final String channel;
        private final String targetSignature;
        private final int targetPlayerId;
        private final int authorizerPlayerId;
        private final String authorizerUuid;

        private Link(TargetKey key, int side, String channel, String targetSignature, int targetPlayerId,
            int authorizerPlayerId, String authorizerUuid) {
            this.key = key;
            this.side = side;
            this.channel = channel;
            this.targetSignature = targetSignature;
            this.targetPlayerId = targetPlayerId;
            this.authorizerPlayerId = authorizerPlayerId;
            this.authorizerUuid = authorizerUuid == null ? "" : authorizerUuid;
        }
    }

    private static final class TargetKey {

        private final int dimension;
        private final int x;
        private final int y;
        private final int z;

        private TargetKey(int dimension, int x, int y, int z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof TargetKey)) return false;
            TargetKey key = (TargetKey) object;
            return dimension == key.dimension && x == key.x && y == key.y && z == key.z;
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + x;
            result = 31 * result + y;
            return 31 * result + z;
        }
    }

    private static final class ChunkKey {

        private final int dimension;
        private final int x;
        private final int z;

        private ChunkKey(int dimension, int x, int z) {
            this.dimension = dimension;
            this.x = x;
            this.z = z;
        }

        private static ChunkKey from(TargetKey key) {
            return new ChunkKey(key.dimension, key.x >> 4, key.z >> 4);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof ChunkKey)) return false;
            ChunkKey key = (ChunkKey) object;
            return dimension == key.dimension && x == key.x && z == key.z;
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + x;
            return 31 * result + z;
        }
    }
}
