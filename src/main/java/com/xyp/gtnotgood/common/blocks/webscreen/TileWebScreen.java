package com.xyp.gtnotgood.common.blocks.webscreen;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import com.xyp.gtnotgood.client.webscreen.ClientWebScreens;
import com.xyp.gtnotgood.config.Config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class TileWebScreen extends TileEntity {

    private UUID screenId;

    public UUID getScreenId() {
        return screenId;
    }

    public int getFacing() {
        int facing = getBlockMetadata();
        return facing >= 2 && facing <= 5 ? facing : 3;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return ClientWebScreens.getRenderBounds(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        double range = Config.webScreenRange + WebScreenShape.MAX_WIDTH + WebScreenShape.MAX_HEIGHT;
        return range * range;
    }

    @Override
    public void validate() {
        super.validate();
        // Block notifications here can re-enter tile creation before chunk registration.
        if (worldObj != null && !worldObj.isRemote && screenId == null) {
            screenId = UUID.randomUUID();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        readIdentity(data);
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        writeIdentity(data);
    }

    private void readIdentity(NBTTagCompound data) {
        screenId = data.hasKey("screenIdMost", 4) && data.hasKey("screenIdLeast", 4)
            ? new UUID(data.getLong("screenIdMost"), data.getLong("screenIdLeast"))
            : null;
    }

    private void writeIdentity(NBTTagCompound data) {
        if (screenId == null) return;
        data.setLong("screenIdMost", screenId.getMostSignificantBits());
        data.setLong("screenIdLeast", screenId.getLeastSignificantBits());
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound data = new NBTTagCompound();
        writeIdentity(data);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, data);
    }

    @Override
    public void onDataPacket(NetworkManager manager, S35PacketUpdateTileEntity packet) {
        readIdentity(packet.func_148857_g());
    }
}
