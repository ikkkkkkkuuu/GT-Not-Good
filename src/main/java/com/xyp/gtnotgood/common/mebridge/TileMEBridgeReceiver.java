package com.xyp.gtnotgood.common.mebridge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.loader.BlockLoader;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnection;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;

public class TileMEBridgeReceiver extends TileMEBridgeBase implements IGuiHolder<PosGuiData> {

    private static final int RECONNECT_INTERVAL = 40;
    private static final String LOCAL_PLAYER_ID_NBT_KEY = "mebridge_local_player_id";

    private String channelName = "";
    private IGridConnection connection;
    private int reconnectCooldown;
    /**
     * The receiver owner's AE2 security ID, preserved while connection creation temporarily impersonates the sender.
     */
    private int localPlayerId = -1;

    public static int countReceiversOnChannel(String name) {
        return MEBridgeReceiverRegistry.count(name);
    }

    public String getChannelName() {
        return channelName == null ? "" : channelName;
    }

    public int getDimensionId() {
        return worldObj == null || worldObj.provider == null ? 0 : worldObj.provider.dimensionId;
    }

    public long getWorldTime() {
        return worldObj == null ? 0L : worldObj.getTotalWorldTime();
    }

    public void setChannelName(String name) {
        String newName = MEBridgeChannelName.normalize(name);
        if (!MEBridgeChannelName.isValid(newName) || newName.equals(channelName)) return;
        if (worldObj != null && worldObj.isRemote) {
            channelName = newName;
            return;
        }

        disconnect();
        channelName = newName;
        reconnectCooldown = 0;
        markDirty();
    }

    public boolean isConnected() {
        return connection != null;
    }

    public void teleportPlayerToSender(EntityPlayerMP player, String targetChannel) {
        if (player == null || worldObj == null || worldObj.isRemote) return;

        MEBridgeChannelInfo target = MEBridgeChannelManager.get(targetChannel);
        WorldServer destination = target == null ? null : DimensionManager.getWorld(target.dim);
        if (target == null || destination == null || target.getSenderTile() == null) {
            // #tr gui.mebridge.teleport.unavailable
            // # The target sender is unavailable.
            // # zh_CN 目标发起端当前不可用。
            player.addChatMessage(new ChatComponentTranslation("gui.mebridge.teleport.unavailable"));
            return;
        }

        Arrival arrival = findSafeArrival(destination, target);
        if (arrival == null) {
            // #tr gui.mebridge.teleport.no_safe_location
            // # No safe location was found near the target sender.
            // # zh_CN 未在目标发起端附近找到安全落点。
            player.addChatMessage(new ChatComponentTranslation("gui.mebridge.teleport.no_safe_location"));
            return;
        }

        if (player.dimension != target.dim) {
            MinecraftServer.getServer()
                .getConfigurationManager()
                .transferPlayerToDimension(player, target.dim, new DirectTeleporter(destination));
        }
        player.playerNetServerHandler
            .setPlayerLocation(arrival.x + 0.5D, arrival.y, arrival.z + 0.5D, player.rotationYaw, player.rotationPitch);
        // #tr gui.mebridge.teleport.success
        // # Teleported to channel %s.
        // # zh_CN 已传送至频道 %s。
        player.addChatMessage(new ChatComponentTranslation("gui.mebridge.teleport.success", target.name));
    }

    @Override
    protected void onProxyReady() {
        restoreLocalPlayerId(getGridNode(ForgeDirection.UNKNOWN));
        reconnectCooldown = Math
            .floorMod(xCoord * 31 + yCoord * 17 + zCoord * 13 + getDimensionId(), RECONNECT_INTERVAL);
    }

    @Override
    protected ItemStack getVisualRepresentation() {
        return BlockLoader.blockMEBridgeReceiver == null ? null : new ItemStack(BlockLoader.blockMEBridgeReceiver);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj.isRemote) return;
        if (reconnectCooldown > 0) {
            reconnectCooldown--;
            return;
        }
        reconnectCooldown = RECONNECT_INTERVAL;
        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        IGridNode receiverNode = getGridNode(ForgeDirection.UNKNOWN);
        IGridNode senderNode = findSenderNode(receiverNode);

        if (connection != null) {
            IGridNode first = connection.a();
            IGridNode second = connection.b();
            boolean isCurrentConnection = senderNode != null && receiverNode.getConnections()
                .contains(connection)
                && senderNode.getConnections()
                    .contains(connection)
                && ((first == receiverNode || second == receiverNode) && (first == senderNode || second == senderNode));
            if (isCurrentConnection) return;
            disconnect();
        }

        if (senderNode == null) return;
        connect(receiverNode, senderNode);
    }

    private IGridNode findSenderNode(IGridNode receiverNode) {
        if (channelName == null || channelName.isEmpty() || receiverNode == null) return null;

        MEBridgeChannelInfo info = MEBridgeChannelManager.get(channelName);
        if (info == null) return null;

        TileMEBridgeSender sender = info.getSenderTile();
        if (sender == null) return null;

        IGridNode senderNode = sender.getGridNode(ForgeDirection.UNKNOWN);
        return senderNode == receiverNode ? null : senderNode;
    }

    private void connect(IGridNode receiverNode, IGridNode senderNode) {
        if (!captureLocalPlayerId(receiverNode)) return;
        try {
            receiverNode.setPlayerID(senderNode.getPlayerID());
            connection = AEApi.instance()
                .createGridConnection(receiverNode, senderNode);
            MEBridgeReceiverRegistry.add(channelName, this);
        } catch (FailedConnection exception) {
            connection = null;
            GTNotGood.LOG.warn(
                "[MEBridge] receiver ({},{},{}) failed to connect channel '{}': {}",
                xCoord,
                yCoord,
                zCoord,
                channelName,
                exception.getClass()
                    .getSimpleName() + " - "
                    + exception.getMessage());
        } finally {
            restoreLocalPlayerId(receiverNode);
        }
    }

    private void disconnect() {
        IGridNode receiverNode = getGridNode(ForgeDirection.UNKNOWN);
        if (connection != null) {
            try {
                connection.destroy();
            } catch (RuntimeException exception) {
                GTNotGood.LOG.warn("[MEBridge] receiver connection destroy failed", exception);
            }
            connection = null;
        }
        MEBridgeReceiverRegistry.remove(channelName, this);
        restoreLocalPlayerId(receiverNode);
    }

    private boolean captureLocalPlayerId(IGridNode receiverNode) {
        if (receiverNode == null) return false;
        if (localPlayerId >= 0) return true;

        net.minecraft.entity.player.EntityPlayer owner = getOwnerPlayer();
        if (owner == null) return false;

        int ownerPlayerId = AEApi.instance()
            .registries()
            .players()
            .getID(owner);
        if (ownerPlayerId < 0) return false;

        localPlayerId = ownerPlayerId;
        markDirty();
        return true;
    }

    private void restoreLocalPlayerId(IGridNode receiverNode) {
        if (receiverNode != null && localPlayerId >= 0) receiverNode.setPlayerID(localPlayerId);
    }

    private static Arrival findSafeArrival(WorldServer world, MEBridgeChannelInfo target) {
        int[][] offsets = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 2, 0 }, { -2, 0 }, { 0, 2 }, { 0, -2 },
            { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
        int minimumY = Math.max(1, target.y - 4);
        int maximumY = Math.min(world.getActualHeight() - 3, target.y + 4);
        for (int[] offset : offsets) {
            int x = target.x + offset[0];
            int z = target.z + offset[1];
            for (int floorY = maximumY; floorY >= minimumY; floorY--) {
                if (isSafeArrival(world, x, floorY, z)) return new Arrival(x, floorY + 1, z);
            }
        }
        return null;
    }

    private static boolean isSafeArrival(WorldServer world, int x, int floorY, int z) {
        net.minecraft.block.material.Material floor = world.getBlock(x, floorY, z)
            .getMaterial();
        net.minecraft.block.material.Material feet = world.getBlock(x, floorY + 1, z)
            .getMaterial();
        net.minecraft.block.material.Material head = world.getBlock(x, floorY + 2, z)
            .getMaterial();
        return floor.blocksMovement() && !floor.isLiquid()
            && !feet.blocksMovement()
            && !feet.isLiquid()
            && !head.blocksMovement()
            && !head.isLiquid();
    }

    private static final class Arrival {

        private final int x;
        private final int y;
        private final int z;

        private Arrival(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class DirectTeleporter extends Teleporter {

        private DirectTeleporter(WorldServer world) {
            super(world);
        }

        @Override
        public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {}
    }

    @Override
    protected void onBridgeInvalidate() {
        disconnect();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        String savedName = MEBridgeChannelName.normalize(nbt.getString("mebridge_channel"));
        channelName = MEBridgeChannelName.isValid(savedName) ? savedName : "";
        localPlayerId = nbt.hasKey(LOCAL_PLAYER_ID_NBT_KEY) ? nbt.getInteger(LOCAL_PLAYER_ID_NBT_KEY) : -1;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("mebridge_channel", getChannelName());
        if (localPlayerId >= 0) nbt.setInteger(LOCAL_PLAYER_ID_NBT_KEY, localPlayerId);
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public com.cleanroommc.modularui.screen.ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new com.cleanroommc.modularui.screen.ModularScreen(GTNotGood.MODID, mainPanel);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return MEBridgeReceiverGui.build(this, syncManager);
    }
}
