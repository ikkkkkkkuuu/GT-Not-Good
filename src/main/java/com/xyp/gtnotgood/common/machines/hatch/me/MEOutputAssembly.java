package com.xyp.gtnotgood.common.machines.hatch.me;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.getFluidUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.storage.StorageChannel;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechDeviceInformation;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.client.iconContainers.blocks.GTCustomBlockIconContainer;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class MEOutputAssembly extends MaxCapacityMEOutputBus {

    private static final String WAILA_FLUID_CACHE = "assemblyFluidCache";
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
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        NBTTagCompound fluids = new NBTTagCompound();
        getFluidOutput().getProvider().getWailaNBTData(player, tile, fluids, world, x, y, z);
        tag.setTag(WAILA_FLUID_CACHE, fluids);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getWailaBody(ItemStack stack, List<String> tips, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, tips, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (tag.hasKey(WAILA_FLUID_CACHE)) {
            tips.add(StatCollector.translateToLocalFormatted("GT5U.waila.hatch.outputme.fluid_cache_capacity",
                formatNumber(tag.getCompoundTag(WAILA_FLUID_CACHE).getLong("cacheCapacity"))));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getWailaAdvancedBody(ItemStack stack, List<String> tips, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaAdvancedBody(stack, tips, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (!tag.hasKey(WAILA_FLUID_CACHE)) return;
        NBTTagCompound fluids = tag.getCompoundTag(WAILA_FLUID_CACHE);
        int count = fluids.getInteger("stackCount");
        if (count == 0) {
            tips.add(StatCollector.translateToLocal("GT5U.waila.hatch.outputme.fluid_cache_empty"));
            return;
        }
        tips.add(StatCollector.translateToLocalFormatted("GT5U.waila.hatch.outputme.fluid_cache_detail", count,
            count > 1 ? "s" : ""));
        NBTTagList cached = fluids.getTagList("stacks", 10);
        for (int i = 0; i < cached.tagCount(); i++) {
            NBTTagCompound entry = cached.getCompoundTagAt(i);
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(entry);
            if (fluid != null) {
                tips.add(String.format("%s: %s%s %s%s", fluid.getLocalizedName(), EnumChatFormatting.GOLD,
                    formatNumber(entry.getLong("Amount")), getFluidUnit(), EnumChatFormatting.RESET));
            }
        }
        if (count > cached.tagCount()) {
            tips.add(StatCollector.translateToLocalFormatted("GT5U.waila.hatch.outputme.fluid_cache_detail.more",
                count - cached.tagCount()));
        }
    }

    @Override
    public String[] getInfoData() {
        List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        List<String> fluids = new ArrayList<>(Arrays.asList(getFluidOutput().getProvider().getInfoData(
            getAEDiagnostics(), "GT5U.infodata.hatch.output_me", fluid -> fluid.getFluidStack().getLocalizedName())));
        fluids.set(1, IGregTechDeviceInformation.encode("GT5U.waila.hatch.outputme.fluid_cache_capacity",
            formatNumber(getFluidOutput().getProvider().getCacheCapacity())));
        info.addAll(fluids.subList(1, fluids.size()));
        return info.toArray(new String[0]);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr tooltip.gtnotgood.me_output_assembly.outputs
            // # Transfers multiblock item and fluid outputs to the ME network
            // # zh_CN 将多方块机器产出的物品和流体存入 ME 网络
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.outputs"),
            // #tr tooltip.gtnotgood.me_output_assembly.capacity
            // # Separate caches: 2^63 - 1 items and 2^63 - 1 L of fluid
            // # zh_CN 独立输出缓存：物品 2^63 - 1 个，流体 2^63 - 1 L
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.capacity"),
            // #tr tooltip.gtnotgood.me_output_assembly.filters
            // # 9 item and 9 fluid filters; empty accepts all; configured filters accept matching outputs only
            // # zh_CN 物品和流体各有 9 格筛选；全空时接收全部，设置后仅接收匹配产物
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.filters"),
            // #tr tooltip.gtnotgood.me_output_assembly.pickup
            // # Pickup clears cached products and filters; drain the caches before removal
            // # zh_CN 拆下时清空缓存产物和筛选设置；请先排空缓存
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.pickup"),
            // #tr tooltip.gtnotgood.me_output_assembly.controls
            // # Screwdriver: toggle cache mode; Shift + screwdriver: toggle check mode
            // # zh_CN 螺丝刀右键切换缓存模式；潜行螺丝刀右键切换检查模式
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.controls"),
            // #tr tooltip.gtnotgood.me_output_assembly.priority
            // # Matching filters take priority over unfiltered outputs
            // # zh_CN 筛选匹配的输出总成优先于未设置筛选的输出
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.priority"),
            // #tr tooltip.gtnotgood.me_output_assembly.network
            // # Requires ME power and one channel for both items and fluids
            // # zh_CN 物品和流体共用一个 ME 频道，需要网络供电
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.network"),
            // #tr tooltip.gtnotgood.me_output_assembly.stacking
            // # Picked-up assemblies stack with newly crafted assemblies
            // # zh_CN 拆下的总成可与新合成的总成堆叠
            StatCollector.translateToLocal("tooltip.gtnotgood.me_output_assembly.stacking") };
    }
}
