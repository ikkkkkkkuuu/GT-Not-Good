package com.xyp.gtnotgood.common.machines.hatch.me;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import appeng.api.storage.StorageChannel;
import gregtech.api.enums.OutputBusType;
import gregtech.api.interfaces.IOutputBusTransaction;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;
import io.netty.buffer.ByteBuf;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

/**
 * ME output bus that keeps the original GT5 AE2 behavior but starts with maximum cache capacity.
 * <p>
 * Unlike the vanilla GT5 ME output bus, this bus does not require inserting a storage cell to grow its cache. Storage
 * cells may still be inserted for filtering and cache-mode checks, but the cache capacity is restored to
 * {@link Long#MAX_VALUE} after every vanilla provider update that would normally derive capacity from the inserted
 * cell.
 *
 * @see MTEHatchOutputBusME
 */
public class MaxCapacityMEOutputBus extends MTEHatchOutputBusME {

    private final MaxCapacityMEOutputFilters filters = new MaxCapacityMEOutputFilters(this::filtersChanged);

    public MaxCapacityMEOutputFilters getFilters() {
        return filters;
    }

    /** Persists server edits and wakes controllers blocked on a previously excluded output. */
    private void filtersChanged() {
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) {
            markDirty();
            notifyOutputSpaceChanged();
        }
    }

    @Override
    public boolean isFiltered() {
        return filters.hasItems() || super.isFiltered();
    }

    @Override
    public boolean isLocked() {
        return isFiltered();
    }

    @Override
    public boolean isFilteredToItem(GTUtility.ItemId id) {
        return filters.accepts(id.getItemStack()) && super.isFilteredToItem(id);
    }

    /** Uses native filtered-ME ordering so matching outputs precede unfiltered destinations. */
    @Override
    public OutputBusType getBusType() {
        if (getProvider().getCacheMode())
            return isFiltered() ? OutputBusType.MECacheFiltered : OutputBusType.MECacheUnfiltered;
        return isFiltered() ? OutputBusType.MEFiltered : OutputBusType.MEUnfiltered;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new MaxCapacityMEOutputGui.Items(this).build(data, sync, settings);
    }

    public MaxCapacityMEOutputBus(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
        forceMaxCapacity();
    }

    public MaxCapacityMEOutputBus(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
        forceMaxCapacity();
    }

    /**
     * Creates the runtime meta-tile entity instance placed in the world.
     *
     * @param aTileEntity base tile entity that will host the new meta-tile entity
     * @return fresh max-capacity ME output bus instance
     */
    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MaxCapacityMEOutputBus(this.mName, this.mTier, getDescription(), this.mTextures);
    }

    /**
     * Returns localized tooltip lines for the registered bus stack.
     *
     * @return description lines shown on the item tooltip
     */
    @Override
    public String[] getDescription() {
        return getLocalizedDescription();
    }

    /**
     * Keeps maximum capacity after GT5 initializes the AE2 proxy and provider.
     *
     * @param aBaseMetaTileEntity base tile entity hosting this bus
     */
    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        forceMaxCapacity();
    }

    /**
     * Keeps maximum capacity after inserting or removing an optional storage cell.
     *
     * @param slot inventory slot whose contents changed
     */
    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        forceMaxCapacity();
    }

    /**
     * Keeps maximum capacity after cache mode or check mode changes.
     */
    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        super.onScrewdriverRightClick(side, aPlayer, aX, aY, aZ, aTool);
        forceMaxCapacity();
    }

    /**
     * Keeps maximum capacity before recipe checks create an output transaction.
     *
     * @return original GT5 ME output bus transaction with the forced capacity visible
     */
    @Override
    public IOutputBusTransaction createTransaction() {
        forceMaxCapacity();
        return new FilteredMEOutputTransaction.Items(super.createTransaction(), filters::accepts);
    }

    /**
     * Keeps maximum capacity before direct item insertion.
     *
     * @param stack    stack being inserted into the bus
     * @param simulate true to check insertion without mutating the bus
     * @return true when the full stack was accepted
     */
    @Override
    public boolean storePartial(ItemStack stack, boolean simulate) {
        if (!filters.accepts(stack)) return false;
        forceMaxCapacity();
        return super.storePartial(stack, simulate);
    }

    /**
     * Resets filters and omits intrinsic capacity on pickup so the bus stacks with a freshly crafted bus.
     * Unrelated upgrade/cover data remain intact; placement always restores maximum capacity.
     *
     * @param aNBT dropped-item data populated by GregTech
     */
    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        forceMaxCapacity();
        super.setItemNBT(aNBT);
        aNBT.removeTag("baseCapacity");
        MaxCapacityMEOutputFilters.clearItemData(aNBT);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        forceMaxCapacity();
        super.saveNBTData(aNBT);
        filters.save(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        filters.load(aNBT);
        forceMaxCapacity();
    }

    @Override
    public void writeToStream(ByteBuf buffer) {
        forceMaxCapacity();
        super.writeToStream(buffer);
    }

    @Override
    public void readFromStream(ByteBuf buffer) {
        super.readFromStream(buffer);
        forceMaxCapacity();
    }

    @Override
    public boolean hasAvailableSpace() {
        forceMaxCapacity();
        return super.hasAvailableSpace();
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        forceMaxCapacity();
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> ss, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        forceMaxCapacity();
        super.getWailaBody(itemStack, ss, accessor, config);
    }

    @Override
    public List getCellArray(StorageChannel channel) {
        forceMaxCapacity();
        return super.getCellArray(channel);
    }

    /**
     * Applies the project max-capacity policy to the original GT5 provider.
     */
    private void forceMaxCapacity() {
        MaxCapacityMEOutputCapacity.forceMaxCapacity(getProvider());
    }

    /**
     * Builds the localized tooltip lines for this bus.
     * <p>
     * Each translation comment stays directly above the key it declares so the Java lang preprocessor can extract it.
     *
     * @return localized tooltip lines
     */
    private static String[] getLocalizedDescription() {
        return new String[] {
            // #tr tooltip.gtnotgood.maxCapacityMEOutputBus.0
            // # Transfers multiblock item outputs to the ME network
            // # zh_CN 将多方块机器产出的物品存入 ME 网络
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputBus.0"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputBus.1
            // # Requires a powered ME network and one channel
            // # zh_CN 需要 ME 网络供电和一个频道
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputBus.1"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputBus.2
            // # Output cache capacity: 2^63 - 1 items
            // # zh_CN 输出缓存容量：2^63 - 1 个
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputBus.2"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputBus.3
            // # 9 filters; empty accepts all; configured filters accept matching outputs only
            // # zh_CN 9 格物品筛选；全空时接收全部，设置后仅接收匹配产物
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputBus.3"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputBus.4
            // # Screwdriver: toggle cache mode
            // # zh_CN 螺丝刀右键：切换缓存模式
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputBus.4"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputBus.5
            // # Shift + screwdriver: toggle check mode
            // # zh_CN 潜行螺丝刀右键：切换检查模式
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputBus.5"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputBus.priority
            // # Matching filters take priority over unfiltered outputs
            // # zh_CN 筛选匹配的输出总线优先于未设置筛选的输出总线
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputBus.priority") };
    }
}
