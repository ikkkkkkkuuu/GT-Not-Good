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
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import appeng.api.storage.StorageChannel;
import gregtech.api.enums.OutputHatchType;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputME;
import io.netty.buffer.ByteBuf;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

/**
 * ME output hatch that keeps the original GT5 AE2 behavior but starts with maximum cache capacity.
 * <p>
 * The vanilla GT5 ME output hatch begins with a small fluid cache and expects a fluid storage cell for larger capacity.
 * This version preserves the original filtering, cache mode, check mode, and AE2 connection behavior while forcing the
 * internal provider capacity to {@link Long#MAX_VALUE}.
 *
 * @see MTEHatchOutputME
 */
public class MaxCapacityMEOutputHatch extends MTEHatchOutputME {

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
        return filters.hasFluids() || super.isFiltered();
    }

    @Override
    public boolean isFluidLocked() {
        return isFiltered();
    }

    @Override
    public boolean isEmptyAndAcceptsAnyFluid() {
        return !filters.hasFluids() && super.isEmptyAndAcceptsAnyFluid();
    }

    @Override
    public boolean canStoreFluid(FluidStack stack) {
        return filters.accepts(stack) && super.canStoreFluid(stack);
    }

    @Override
    public boolean isFilteredToFluid(GTUtility.FluidId id) {
        return canStoreFluid(id.getFluidStack());
    }

    /** Uses native filtered-ME ordering so matching outputs precede unfiltered destinations. */
    @Override
    public OutputHatchType getHatchType() {
        if (getProvider().getCacheMode())
            return isFiltered() ? OutputHatchType.MECacheFiltered : OutputHatchType.MECacheUnfiltered;
        return isFiltered() ? OutputHatchType.MEFiltered : OutputHatchType.MEUnfiltered;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new MaxCapacityMEOutputGui.Fluids(this).build(data, sync, settings);
    }

    public MaxCapacityMEOutputHatch(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
        forceMaxCapacity();
    }

    public MaxCapacityMEOutputHatch(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
        forceMaxCapacity();
    }

    /**
     * Creates the runtime meta-tile entity instance placed in the world.
     *
     * @param aTileEntity base tile entity that will host the new meta-tile entity
     * @return fresh max-capacity ME output hatch instance
     */
    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MaxCapacityMEOutputHatch(this.mName, this.mTier, getDescription(), this.mTextures);
    }

    /**
     * Returns localized tooltip lines for the registered hatch stack.
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
     * @param aBaseMetaTileEntity base tile entity hosting this hatch
     */
    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        forceMaxCapacity();
    }

    /**
     * Keeps maximum capacity after inserting or removing an optional fluid storage cell.
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
     * @return original GT5 ME output hatch transaction with the forced capacity visible
     */
    @Override
    public IOutputHatchTransaction createTransaction() {
        forceMaxCapacity();
        return new FilteredMEOutputTransaction.Fluids(super.createTransaction(), filters::accepts);
    }

    /**
     * Keeps maximum capacity before direct fluid insertion.
     *
     * @param aFluid fluid being inserted into the hatch
     * @param doFill false to check insertion without mutating the hatch
     * @return amount of fluid accepted by the hatch
     */
    @Override
    public int fill(FluidStack aFluid, boolean doFill) {
        if (!filters.accepts(aFluid)) return 0;
        forceMaxCapacity();
        return super.fill(aFluid, doFill);
    }

    /**
     * Resets filters and omits intrinsic capacity on pickup so the hatch stacks with a freshly crafted hatch.
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
     * Builds the localized tooltip lines for this hatch.
     * <p>
     * Each translation comment stays directly above the key it declares so the Java lang preprocessor can extract it.
     *
     * @return localized tooltip lines
     */
    private static String[] getLocalizedDescription() {
        return new String[] {
            // #tr tooltip.gtnotgood.maxCapacityMEOutputHatch.0
            // # Fluid Output for Multiblocks
            // # zh_CN 多方块流体输出
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputHatch.0"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputHatch.1
            // # Stores directly into ME
            // # zh_CN 直接输出到ME网络
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputHatch.1"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputHatch.2
            // # Max cache capacity by default
            // # zh_CN 默认最大缓存容量
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputHatch.2"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputHatch.3
            // # 9 ghost filters; empty accepts all; matches get output priority
            // # zh_CN 9格虚拟过滤：全空接收全部，标记后仅接收并优先分配匹配流体
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputHatch.3"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputHatch.4
            // # Right click with screwdriver to toggle Cache Mode
            // # zh_CN 螺丝刀右键切换缓存模式
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputHatch.4"),
            // #tr tooltip.gtnotgood.maxCapacityMEOutputHatch.5
            // # Shift right click with screwdriver to toggle Check Mode
            // # zh_CN 潜行螺丝刀右键切换检查模式
            StatCollector.translateToLocal("tooltip.gtnotgood.maxCapacityMEOutputHatch.5") };
    }
}
