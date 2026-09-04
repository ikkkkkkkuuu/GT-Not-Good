package com.xyp.gtnotgood.common.mebridge;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.loader.BlockLoader;

public class TileMEBridgeSender extends TileMEBridgeBase implements IGuiHolder<PosGuiData> {

    private String channelName = "";
    private int channelColor = MEBridgeChannelColor.defaultFor("");
    private boolean channelColorAssigned;
    private long receiverTopologyRevision = Long.MIN_VALUE;
    private String receiverTopologyChannel = "";
    private String receiverTopologySnapshot = "";

    @Override
    protected ItemStack getVisualRepresentation() {
        return new ItemStack(BlockLoader.blockMEBridgeSender);
    }

    public String getChannelName() {
        return channelName;
    }

    public int getChannelColor() {
        return channelColor;
    }

    public void setChannelColor(int color) {
        if (worldObj == null || worldObj.isRemote) return;
        int sanitized = MEBridgeChannelColor.sanitize(color);
        if (channelColor == sanitized) {
            channelColorAssigned = true;
            return;
        }
        channelColor = sanitized;
        channelColorAssigned = true;
        if (!channelName.isEmpty()) registerSelf();
        MEWirelessLinkManager.onChannelColorChanged();
        markDirty();
    }

    public int getDimensionId() {
        return worldObj == null || worldObj.provider == null ? 0 : worldObj.provider.dimensionId;
    }

    public String getCoordinates() {
        return xCoord + ", " + yCoord + ", " + zCoord;
    }

    public boolean setChannelName(String name) {
        return trySetChannelName(name).isSuccess();
    }

    public MEBridgeChannelChangeResult trySetChannelName(String name) {
        if (worldObj == null || worldObj.isRemote) return MEBridgeChannelChangeResult.NOT_SERVER_SIDE;

        String normalized = MEBridgeChannelName.normalize(name);
        if (!MEBridgeChannelName.isValid(normalized)) return MEBridgeChannelChangeResult.INVALID_NAME;

        if (channelName.isEmpty() && !channelColorAssigned) {
            channelColor = MEBridgeChannelColor.defaultFor(normalized);
            channelColorAssigned = true;
        }

        MEBridgeChannelInfo info = new MEBridgeChannelInfo(
            normalized,
            xCoord,
            yCoord,
            zCoord,
            worldObj.provider.dimensionId,
            channelColor,
            null);
        MEBridgeChannelChangeResult result = MEBridgeChannelManager.replaceSenderChannel(channelName, info, this);
        if (!result.isSuccess()) return result;

        channelName = normalized;
        receiverTopologyRevision = Long.MIN_VALUE;
        markDirty();
        return MEBridgeChannelChangeResult.SUCCESS;
    }

    private boolean registerSelf() {
        if (channelName.isEmpty() || worldObj == null || worldObj.isRemote) return false;
        MEBridgeChannelInfo info = new MEBridgeChannelInfo(
            channelName,
            xCoord,
            yCoord,
            zCoord,
            worldObj.provider.dimensionId,
            channelColor,
            null);
        return MEBridgeChannelManager.register(info, this);
    }

    @Override
    protected void onProxyReady() {
        if (!channelName.isEmpty()) registerSelf();
    }

    @Override
    protected void onBridgeInvalidate() {
        if (worldObj != null && !worldObj.isRemote && !channelName.isEmpty()) {
            MEBridgeChannelManager.unregister(channelName, xCoord, yCoord, zCoord, worldObj.provider.dimensionId);
        }
    }

    public int getConnectedReceiverCount() {
        return TileMEBridgeReceiver.countReceiversOnChannel(channelName);
    }

    public String getReceiverTopologySnapshot() {
        long revision = MEBridgeReceiverRegistry.getRevision();
        if (revision != receiverTopologyRevision || !channelName.equals(receiverTopologyChannel)) {
            receiverTopologySnapshot = MEBridgeReceiverTopologyCodec
                .encode(MEBridgeReceiverRegistry.countByDimension(channelName));
            receiverTopologyRevision = MEBridgeReceiverRegistry.getRevision();
            receiverTopologyChannel = channelName;
        }
        return receiverTopologySnapshot;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        String savedName = MEBridgeChannelName.normalize(nbt.getString("mebridge_channel"));
        channelName = MEBridgeChannelName.isValid(savedName) ? savedName : "";
        channelColorAssigned = nbt.hasKey("mebridge_channel_color");
        channelColor = channelColorAssigned ? MEBridgeChannelColor.sanitize(nbt.getInteger("mebridge_channel_color"))
            : MEBridgeChannelColor.defaultFor(channelName);
        receiverTopologyRevision = Long.MIN_VALUE;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("mebridge_channel", channelName == null ? "" : channelName);
        nbt.setInteger("mebridge_channel_color", channelColor);
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public com.cleanroommc.modularui.screen.ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new com.cleanroommc.modularui.screen.ModularScreen(GTNotGood.MODID, mainPanel);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return MEBridgeSenderGui.build(this, syncManager);
    }
}
