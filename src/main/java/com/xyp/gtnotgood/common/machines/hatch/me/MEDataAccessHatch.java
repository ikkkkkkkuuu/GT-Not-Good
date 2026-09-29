// Adapted from reobf/Programmable-Hatches-Mod DataHatchME; MIT. See META-INF/me-data-access-port/.
package com.xyp.gtnotgood.common.machines.hatch.me;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.config.FuzzyMode;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.item.AEItemStack;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Textures.BlockIcons;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchDataAccess;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTRecipe.RecipeAssemblyLine;

/**
 * Exposes ME data sticks to assembly lines without moving or owning the physical items.
 * Network callbacks invalidate a snapshot; the server refreshes it before recipe reads and notifies GT watchers.
 * Only AE node state is persisted, so a loaded hatch must reconnect before granting access to research.
 */
@IMetaTileEntity.SkipGenerateDescription
public class MEDataAccessHatch extends MTEHatchDataAccess
    implements IGridProxyable, IPowerChannelState, IMEMonitorHandlerReceiver<IAEItemStack> {

    private final MEDataStickSnapshot snapshot = new MEDataStickSnapshot();
    private final ItemStackHandler automationInventory = new ItemStackHandler(0);
    private AENetworkProxy proxy;
    private IMEMonitor<IAEItemStack> monitor;
    private boolean refreshPending = true;
    private boolean refreshing;
    private boolean ready;

    public MEDataAccessHatch(int id, String name, String regionalName) {
        super(id, name, regionalName, VoltageIndex.IV);
    }

    public MEDataAccessHatch(String name, String[] description, ITexture[][][] textures) {
        super(name, VoltageIndex.IV, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MEDataAccessHatch(mName, mDescriptionArray, mTextures);
    }

    @Override
    public AENetworkProxy getProxy() {
        if (proxy == null) {
            proxy = new AENetworkProxy(this, "proxy", GTNGItemList.MEDataAccessHatch.get(1), true);
            proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            proxy.setValidSides(EnumSet.of(getBaseMetaTileEntity().getFrontFacing()));
            if (getBaseMetaTileEntity().getWorld() != null) {
                proxy.setOwner(
                    getBaseMetaTileEntity().getWorld()
                        .getPlayerEntityByName(getBaseMetaTileEntity().getOwnerName()));
            }
        }
        return proxy;
    }

    @Override
    public void onFirstTick(IGregTechTileEntity tile) {
        super.onFirstTick(tile);
        if (tile.isServerSide()) {
            ready = true;
            getProxy().onReady();
            refreshSnapshot();
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide()) {
            if (tick % 400 == 0) refreshPending = true;
            refreshSnapshot();
        }
    }

    @Override
    public void onFacingChange() {
        getProxy().setValidSides(EnumSet.of(getBaseMetaTileEntity().getFrontFacing()));
        refreshPending = true;
    }

    @Override
    public void gridChanged() {
        refreshPending = true;
    }

    private void setMonitor(IMEMonitor<IAEItemStack> next) {
        if (monitor == next) return;
        if (monitor != null) monitor.removeListener(this);
        monitor = next;
        refreshPending = true;
        if (monitor != null) monitor.addListener(this, monitor);
    }

    /** Invalidates the inherited recipe cache only when the visible research actually changes. */
    private void refreshSnapshot() {
        if (refreshing || !ready || !getBaseMetaTileEntity().isServerSide()) return;
        refreshing = true;
        try {
            if (!isActive() || !isPowered()) {
                setMonitor(null);
                if (snapshot.clear()) super.onContentsChanged(-1);
                return;
            }
            setMonitor(
                getProxy().getStorage()
                    .getItemInventory());
            if (!refreshPending) return;
            refreshPending = false;
            ItemStack dataStick = ItemList.Tool_DataStick.get(1);
            List<ItemStack> contents = new ArrayList<>();
            for (IAEItemStack stack : monitor.getStorageList()
                .findFuzzy(AEItemStack.create(dataStick), FuzzyMode.IGNORE_ALL)) {
                // Craftable-only entries and zero-sized cached entries must not authorize research.
                if (stack.getStackSize() <= 0) continue;
                ItemStack copy = stack.getItemStack();
                copy.stackSize = 1;
                contents.add(copy);
            }
            if (snapshot.replace(contents, dataStick)) super.onContentsChanged(-1);
        } catch (GridAccessException ignored) {
            setMonitor(null);
            refreshPending = true;
            if (snapshot.clear()) super.onContentsChanged(-1);
        } finally {
            refreshing = false;
        }
    }

    @Override
    public void postChange(IBaseMonitor<IAEItemStack> source, Iterable<IAEItemStack> changes,
        BaseActionSource actionSource) {
        if (source != monitor) return;
        ItemStack dataStick = ItemList.Tool_DataStick.get(1);
        for (IAEItemStack stack : changes) {
            if (MEDataStickSnapshot.matches(stack.getItemStack(), dataStick)) {
                refreshPending = true;
                return;
            }
        }
    }

    @Override
    public void onListUpdate() {
        refreshPending = true;
    }

    @Override
    public boolean isValid(Object token) {
        return ready && monitor != null && token == monitor;
    }

    @Override
    public List<RecipeAssemblyLine> getAssemblyLineRecipes() {
        refreshSnapshot();
        return super.getAssemblyLineRecipes();
    }

    @Override
    public int getSizeInventory() {
        refreshSnapshot();
        return snapshot.size();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        refreshSnapshot();
        return snapshot.get(index);
    }

    /** The virtual research slots must never be offered to item-transfer handlers. */
    @Override
    public IItemHandlerModifiable getInventoryHandler() {
        return automationInventory;
    }

    @Override
    public boolean isValidSlot(int index) {
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {}

    @Override
    public boolean shouldDropItemAt(int index) {
        return false;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity tile, EntityPlayer player) {
        return false;
    }

    @Override
    public boolean isPowered() {
        return proxy != null && proxy.isPowered();
    }

    @Override
    public boolean isActive() {
        return proxy != null && proxy.isActive();
    }

    @Override
    public IGridNode getGridNode(ForgeDirection side) {
        return getProxy().getNode();
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection side) {
        return side == getBaseMetaTileEntity().getFrontFacing() ? AECableType.SMART : AECableType.NONE;
    }

    @Override
    public DimensionalCoord getLocation() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        return new DimensionalCoord(tile.getWorld(), tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
    }

    @Override
    public void securityBreak() {}

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        getProxy().writeToNBT(nbt);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        ready = false;
        setMonitor(null);
        snapshot.clear();
        refreshPending = true;
        getProxy().readFromNBT(nbt);
        super.onContentsChanged(-1);
    }

    private void disconnect() {
        ready = false;
        setMonitor(null);
        if (snapshot.clear()) super.onContentsChanged(-1);
    }

    @Override
    public void onUnload() {
        disconnect();
        super.onUnload();
    }

    @Override
    public void onRemoval() {
        disconnect();
        super.onRemoval();
    }

    @Override
    public ITexture[] getTexturesActive(ITexture background) {
        return new ITexture[] { background, TextureFactory.of(BlockIcons.OVERLAY_ME_INPUT_HATCH_ACTIVE) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture background) {
        return new ITexture[] { background, TextureFactory.of(BlockIcons.OVERLAY_ME_INPUT_HATCH) };
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr tooltip.gtnotgood.meDataAccess.0
            // # Data Access for Multiblocks
            // # zh_CN 多方块结构的数据访问
            StatCollector.translateToLocal("tooltip.gtnotgood.meDataAccess.0"),
            // #tr tooltip.gtnotgood.meDataAccess.1
            // # Reads all data sticks stored in the ME network
            // # zh_CN 读取 ME 网络中存储的所有数据棒
            StatCollector.translateToLocal("tooltip.gtnotgood.meDataAccess.1"),
            // #tr tooltip.gtnotgood.meDataAccess.2
            // # Data sticks remain in ME storage and can be shared
            // # zh_CN 数据棒保留在 ME 存储中，可供多台机器共享
            StatCollector.translateToLocal("tooltip.gtnotgood.meDataAccess.2"),
            // #tr tooltip.gtnotgood.meDataAccess.3
            // # Connect on the front; requires ME power and one channel
            // # zh_CN 从正面接入，需要 ME 供电和一个频道
            StatCollector.translateToLocal("tooltip.gtnotgood.meDataAccess.3") };
    }
}
