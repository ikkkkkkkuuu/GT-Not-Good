package com.xyp.gtnotgood.common.network;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.utils.enums.ModList;

/** Non-ticking physical cable or connector. Loading changes invalidate cached network topology. */
public class TileNetworkNode extends TileEntity implements IGuiHolder<PosGuiData> {

    private String name = "";
    private int enabledFaces = 63;

    @Override
    public boolean canUpdate() {
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        if (worldObj == null || worldObj.isRemote) return;
        name = value == null ? "" : value.replaceAll("[\\p{Cntrl}§]", "");
        if (name.length() > 32) name = name.substring(0, 32);
        markDirty();
    }

    public boolean faceEnabled(int side) {
        return side >= 0 && side < 6 && (enabledFaces & (1 << side)) != 0;
    }

    public void toggleFace(int side) {
        if (worldObj == null || worldObj.isRemote || side < 0 || side >= 6) return;
        enabledFaces ^= 1 << side;
        markDirty();
        NetworkTopology.changed(worldObj);
    }

    @Override
    public void validate() {
        super.validate();
        NetworkTopology.changed(worldObj);
    }

    @Override
    public void invalidate() {
        NetworkTopology.changed(worldObj);
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        NetworkTopology.changed(worldObj);
        super.onChunkUnload();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        name = tag.getString("networkName");
        if (name.length() > 32) name = name.substring(0, 32);
        enabledFaces = tag.hasKey("networkFaces") ? tag.getInteger("networkFaces") & 63 : 63;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("networkName", name);
        tag.setInteger("networkFaces", enabledFaces);
    }

    @Override
    public ModularScreen createScreen(PosGuiData data, ModularPanel panel) {
        return new ModularScreen(ModList.GTNotGood.getID(), panel);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return NetworkGui.connector(this, sync);
    }
}
