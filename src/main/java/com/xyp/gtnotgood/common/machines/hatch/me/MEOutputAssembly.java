package com.xyp.gtnotgood.common.machines.hatch.me;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.storage.StorageChannel;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.client.iconContainers.blocks.GTCustomBlockIconContainer;

public class MEOutputAssembly extends MaxCapacityMEOutputBus {

    private static final IIconContainer OVERLAY = GTCustomBlockIconContainer
        .create(ModList.GTNotGood.getResourceLocation(), "iconsets/OVERLAY_ME_OUTPUT_ASSEMBLY");
    private static final IIconContainer OVERLAY_ACTIVE = GTCustomBlockIconContainer
        .create(ModList.GTNotGood.getResourceLocation(), "iconsets/OVERLAY_ME_OUTPUT_ASSEMBLY_ACTIVE");

    private MEOutputAssemblyFluidOutput fluidOutput;

    public MEOutputAssembly(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public MEOutputAssembly(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, tier, description, textures);
    }

    public MEOutputAssemblyFluidOutput getFluidOutput() {
        if (fluidOutput == null) fluidOutput = new MEOutputAssemblyFluidOutput(this);
        fluidOutput.attach();
        return fluidOutput;
    }

    @Override
    public int fill(FluidStack stack, boolean doFill) {
        return getFluidOutput().fill(stack, doFill);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MEOutputAssembly(mName, mTier, getDescription(), mTextures);
    }

    @Override
    public ItemStack getVisual() {
        return GTNGItemList.MEOutputAssembly.get(1);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(OVERLAY_ACTIVE) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(OVERLAY) };
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new MaxCapacityMEOutputGui.Assembly(this).build(data, sync, settings);
    }

    @Override
    public void onFirstTick(IGregTechTileEntity tile) {
        super.onFirstTick(tile);
        getFluidOutput().updateSettings();
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide()) getFluidOutput().tick(tick);
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        super.onScrewdriverRightClick(side, player, x, y, z, tool);
        getFluidOutput().updateSettings();
    }

    @Override
    public List getCellArray(StorageChannel channel) {
        return channel == StorageChannel.FLUIDS ? getFluidOutput().getProvider().getCellArray(channel)
            : super.getCellArray(channel);
    }

    @Override
    public void setItemNBT(NBTTagCompound tag) {
        super.setItemNBT(tag);
        tag.removeTag("cache");
        getFluidOutput().setItemNBT(tag);
    }

    @Override
    public void notifyOutputSpaceChanged() {
        super.notifyOutputSpaceChanged();
        if (fluidOutput != null) fluidOutput.filtersChanged();
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        getFluidOutput().save(tag);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        getFluidOutput().load(tag);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr tooltip.gtnotgood.me_output_assembly.outputs
            // # Outputs items and fluids from multiblocks directly to ME
            // # zh_CN 将多方块产出的物品和流体直接输出至ME网络
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.outputs"),
            // #tr tooltip.gtnotgood.me_output_assembly.capacity
            // # Independent item and fluid caches: Long.MAX_VALUE each
            // # zh_CN 物品与流体各有独立Long.MAX_VALUE容量缓存
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.capacity"),
            // #tr tooltip.gtnotgood.me_output_assembly.filters
            // # 9 item and 9 fluid filters; empty accepts all; matches get priority
            // # zh_CN 物品与流体各9格筛选：全空接收全部，设置后仅接收并优先分配匹配产物
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.filters"),
            // #tr tooltip.gtnotgood.me_output_assembly.pickup
            // # Pickup clears caches and filters; stacks with newly crafted assemblies
            // # zh_CN 拆下时清空缓存和筛选标记，可与新合成的总成堆叠
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.pickup"),
            // #tr tooltip.gtnotgood.me_output_assembly.controls
            // # Screwdriver: cache mode; Shift + screwdriver: check mode
            // # zh_CN 螺丝刀切换缓存模式，潜行螺丝刀切换检查模式
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.controls") };
    }
}
