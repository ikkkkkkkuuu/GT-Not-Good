// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Modified for compact, fixed-maximum, energy-free GT Not Good machines.
package com.xyp.gtnotgood.common.machines.multiblock;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.utils.ECraftingCPUCluster;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkCraftingCpuChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.WorldCoord;
import appeng.helpers.ICustomNameObject;
import appeng.me.GridAccessException;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoidingMode;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.modularui.IAddGregtechLogo;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

@IMetaTileEntity.SkipGenerateDescription
@IMetaTileEntity.SkipGenerateName
/**
 * Compact, fixed-singularity AE crafting CPU controller. Five GregTech casings surround the controller in the
 * same 3x2x1 layout as the ore processor. Each accepted order receives its own persistent CPU; completed CPUs
 * are reclaimed only after both item and native fluid inventories are empty. The AE grid remains responsible
 * for channels, while this controller and its dispatches consume no energy.
 *
 * @see ECraftingCPUCluster
 */
public class QuantumComputer extends GTNGMultiBlockBase<QuantumComputer>
    implements ISurvivalConstructable, IActionHost, IGridProxyable, IAddGregtechLogo, ICustomNameObject {

    public int width = 3;
    public int height = 2;
    public int depth = 1;

    public long maximumStorage = Long.MAX_VALUE;
    public int maximumParallel = Integer.MAX_VALUE;
    public long usedStorage = 0;
    public int usedParallel = 0;
    public boolean enabledSingularityCore = true;
    public String customName = "";
    public ECraftingCPUCluster virtualCPU = null;
    public final List<ECraftingCPUCluster> cpus = new ReferenceArrayList<>();

    private AENetworkProxy gridProxy;
    private boolean wasActive = false;

    public long getMaximumStorage() {
        return Long.MAX_VALUE;
    }

    public int getWidthForGui() {
        return width;
    }

    public void setWidthFromGui(int width) {
        this.width = width;
    }

    public int getHeightForGui() {
        return height;
    }

    public void setHeightFromGui(int height) {
        this.height = height;
    }

    public int getDepthForGui() {
        return depth;
    }

    public void setDepthFromGui(int depth) {
        this.depth = depth;
    }

    public int getMaximumParallelForGui() {
        return maximumParallel;
    }

    public void setMaximumParallelFromGui(int maximumParallel) {
        this.maximumParallel = maximumParallel;
    }

    public int getUsedParallelForGui() {
        return getUsedParallel();
    }

    public void setUsedParallelFromGui(int usedParallel) {
        this.usedParallel = usedParallel;
    }

    public long getMaximumStorageForGui() {
        return getMaximumStorage();
    }

    public void setMaximumStorageFromGui(long maximumStorage) {
        this.maximumStorage = maximumStorage;
    }

    public long getUsedStorageForGui() {
        return getUsedBytes();
    }

    public void setUsedStorageFromGui(long usedStorage) {
        this.usedStorage = usedStorage;
    }

    public String getDisplayNameForGui() {
        return hasCustomName() ? customName : getMachineCraftingIcon().getDisplayName();
    }

    public QuantumComputer(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public QuantumComputer(String aName) {
        super(aName);
    }

    @Override
    public String getLocalNameKey() {
        return "gtng.QuantumComputer.name";
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new QuantumComputer(mName);
    }

    @Override
    public MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr gtng.QuantumComputer.name
        // # QuantumComputer
        // # zh_CN 量子计算机
        tt.addMachineType(StatCollector.translateToLocal("gtng.QuantumComputer.name"))
            // #tr gtng.QuantumComputer.maximum
            // # Singularity storage and maximum coprocessors; dynamic crafting CPUs
            // # zh_CN 奇点容量、最高协处理器数量，动态分配合成CPU
            .addInfo(StatCollector.translateToLocal("gtng.QuantumComputer.maximum"))
            // #tr gtng.QuantumComputer.free
            // # No upgrades, EU or AE crafting energy required
            // # zh_CN 无需升级，不消耗EU或AE合成能量
            .addInfo(StatCollector.translateToLocal("gtng.QuantumComputer.free"))
            .beginStructureBlock(3, 2, 1, false)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public void onFirstTick(IGregTechTileEntity baseMetaTileEntity) {
        super.onFirstTick(baseMetaTileEntity);
        if (checkStructure(true, getBaseMetaTileEntity())) {
            this.mStartUpCheck = -1;
            this.mUpdate = 200;
        }
        getProxy().onReady();
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isAllowedToWork()) aBaseMetaTileEntity.disableWorking();
        if (aBaseMetaTileEntity.isServerSide()) {
            boolean active = isActive();
            if (active != wasActive) {
                wasActive = active;
                if (active) createVirtualCPU();
                postCPUClusterChangeEvent();
            }
        }
    }

    @Override
    public void onBlockDestroyed() {
        super.onBlockDestroyed();
        clearCPUs();
        postCPUClusterChangeEvent();
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setInteger("width", width);
        aNBT.setInteger("height", height);
        aNBT.setInteger("depth", depth);
        aNBT.setLong("maximumStorage", getMaximumStorage());
        aNBT.setInteger("maximumParallel", maximumParallel);
        aNBT.setBoolean("enabledSingularityCore", enabledSingularityCore);

        if (customName != null) aNBT.setString("customName", customName);

        getProxy().writeToNBT(aNBT);
        writeCPUNBT(aNBT);

        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        width = 3;
        height = 2;
        depth = 1;
        maximumStorage = Long.MAX_VALUE;
        maximumParallel = Integer.MAX_VALUE;
        enabledSingularityCore = true;

        if (aNBT.hasKey("customName")) setCustomName(aNBT.getString("customName"));

        getProxy().readFromNBT(aNBT);
        readCPUNBT(aNBT);

        super.loadNBTData(aNBT);
    }

    public void writeCPUNBT(final NBTTagCompound compound) {
        final NBTTagList clustersTag = new NBTTagList();
        cpus.forEach(cluster -> {
            NBTTagCompound clusterTag = new NBTTagCompound();
            cluster.writeToNBT(clusterTag);
            clusterTag.setLong("availableStorage", cluster.getAvailableStorage());
            clustersTag.appendTag(clusterTag);
        });
        compound.setTag("clusters", clustersTag);
    }

    public void readCPUNBT(final NBTTagCompound compound) {
        new ReferenceArrayList<>(cpus).forEach(CraftingCPUCluster::destroy);
        cpus.clear();

        final NBTTagList clustersTag = compound.getTagList("clusters", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < clustersTag.tagCount(); i++) {
            NBTTagCompound clusterTag = clustersTag.getCompoundTagAt(i);

            WorldCoord coord = getWorldCoord();
            ECraftingCPUCluster cluster = new ECraftingCPUCluster(coord, coord);
            cluster.setVirtualCPUOwner(this);
            cluster.setAvailableStorage(clusterTag.getLong("availableStorage"));
            cluster.readFromNBT(clusterTag);
            cluster.setAccelerators(Integer.MAX_VALUE);
            cpus.add(cluster);
        }
        updateCPUNames();
    }

    @Override
    public boolean isRecipeLockingEnabled() {
        return false;
    }

    @Override
    public VoidingMode getVoidingMode() {
        return VoidingMode.VOID_NONE;
    }

    @Override
    public boolean isInputSeparationEnabled() {
        return false;
    }

    @Override
    public boolean isBatchModeEnabled() {
        return false;
    }

    @Override
    public IGridNode getActionableNode() {
        return getProxy().getNode();
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(
            getBaseMetaTileEntity().getWorld(),
            getBaseMetaTileEntity().getXCoord(),
            getBaseMetaTileEntity().getYCoord(),
            getBaseMetaTileEntity().getZCoord());
    }

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        return getProxy().getNode();
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection forgeDirection) {
        return AECableType.DENSE_COVERED;
    }

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            var bmte = getBaseMetaTileEntity();
            if (bmte instanceof IGridProxyable) {
                gridProxy = new AENetworkProxy(this, "proxy", GTNGItemList.QuantumComputer.get(1), true);
                gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
                gridProxy.setIdlePowerUsage(0);
                if (bmte.getWorld() != null) {
                    gridProxy.setOwner(
                        bmte.getWorld()
                            .getPlayerEntityByName(bmte.getOwnerName()));
                }
            }
        }
        return gridProxy;
    }

    @Override
    public String getCustomName() {
        return customName != null ? customName : getMachineCraftingIcon().getDisplayName();
    }

    @Override
    public boolean hasCustomName() {
        return customName != null && !customName.isEmpty();
    }

    @Override
    public void setCustomName(String name) {
        customName = name;
        updateCPUNames();
        if (virtualCPU != null || !cpus.isEmpty()) {
            postCPUClusterChangeEvent();
        }
    }

    public void updateCPUNames() {
        final String parentName = hasCustomName() ? customName : "";
        if (virtualCPU != null) {
            virtualCPU.setName(parentName);
        }
        for (int index = 0; index < cpus.size(); index++) {
            cpus.get(index)
                .setName(parentName.isEmpty() ? "" : parentName + " #" + (index + 1));
        }
    }

    @Override
    public void securityBreak() {}

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkPowerStatusChange c) {
        final boolean currentActive = isActive();
        if (wasActive != currentActive) {
            wasActive = currentActive;
            postCPUClusterChangeEvent();
        }
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkChannelsChanged c) {
        final boolean currentActive = isActive();
        if (wasActive != currentActive) {
            wasActive = currentActive;
            postCPUClusterChangeEvent();
        }
    }

    public void postCPUClusterChangeEvent() {
        if (getProxy().getNode() == null) return;
        try {
            getProxy().getGrid()
                .postEvent(new MENetworkCraftingCpuChange(getProxy().getNode()));
        } catch (GridAccessException ignored) {}
    }

    public boolean isActive() {
        return mMachine && getProxy().isActive();
    }

    public boolean isVirtualCPU(Object cluster) {
        return virtualCPU == cluster;
    }

    public void forEachCPU(final Consumer<CraftingCPUCluster> consumer) {
        if (!isActive()) return;

        for (final CraftingCPUCluster cpu : cpus) {
            consumer.accept(cpu);
        }
        if (virtualCPU != null) {
            virtualCPU.setVirtualCPUOwner(this);
            consumer.accept(virtualCPU);
        }
    }

    public void onVirtualCPUSubmitJob(final long usedBytes) {
        final boolean prevEmpty = cpus.isEmpty();

        virtualCPU.setVirtualCPUOwner(this);
        cpus.add(virtualCPU);

        if (prevEmpty) {
            markDirty();
        }

        virtualCPU.setAvailableStorage(usedBytes);
        virtualCPU = null;
        updateCPUNames();
        createVirtualCPU();
    }

    public long getAvailableBytes() {
        return Long.MAX_VALUE;
    }

    public long getUsedBytes() {
        if (enabledSingularityCore) return 0;
        long storage = 0;
        for (final CraftingCPUCluster cpu : cpus) {
            storage += cpu.getAvailableStorage();
        }
        usedStorage = storage;
        return usedStorage;
    }

    public int getUsedParallel() {
        return (int) Math.min(Integer.MAX_VALUE, (long) cpus.size() * Integer.MAX_VALUE);
    }

    public void createVirtualCPU() {
        final long availableBytes = getAvailableBytes();
        if (virtualCPU != null) {
            virtualCPU.setAvailableStorage(availableBytes);
            virtualCPU.setAccelerators(maximumParallel);
            return;
        }

        WorldCoord pos = getWorldCoord();
        virtualCPU = new ECraftingCPUCluster(pos, pos);
        virtualCPU.setVirtualCPUOwner(this);
        virtualCPU.setAvailableStorage(availableBytes);
        virtualCPU.setAccelerators(maximumParallel);
        if (hasCustomName()) virtualCPU.setName(customName);

        postCPUClusterChangeEvent();
    }

    public void clearCPUs() {
        IMEMonitor<IAEItemStack> itemInventory = null;
        try {
            var t = getBaseMetaTileEntity();
            var te = t.getWorld()
                .getTileEntity(t.getXCoord(), t.getYCoord() + 1, t.getZCoord());
            if (te instanceof IGridHost igh) itemInventory = igh.getGridNode(ForgeDirection.UNKNOWN)
                .getGrid()
                .<IStorageGrid>getCache(IStorageGrid.class)
                .getItemInventory();
        } catch (Exception ignored) {}
        final var s = new MachineSource(this);
        for (var cpu : cpus) {
            if (itemInventory != null) {
                IItemList<IAEItemStack> itemList = AEApi.instance()
                    .storage()
                    .createItemList();
                cpu.getInventory()
                    .getAvailableItems(itemList);
                for (var stack : itemList) {
                    itemInventory.injectItems(stack, Actionable.MODULATE, s);
                }
            }
            cpu.markDestroyed();
        }
        cpus.clear();
    }

    public WorldCoord getWorldCoord() {
        return new WorldCoord(
            getBaseMetaTileEntity().getXCoord(),
            getBaseMetaTileEntity().getYCoord(),
            getBaseMetaTileEntity().getZCoord());
    }

    public void onCPUDestroyed(final ECraftingCPUCluster cluster) {
        cpus.remove(cluster);
        updateCPUNames();
        createVirtualCPU();
        postCPUClusterChangeEvent();
        if (cpus.isEmpty()) {
            markDirty();
        }
    }

    @Override
    public IStructureDefinition<QuantumComputer> getStructureDefinition() {
        return StructureDefinition.<QuantumComputer>builder()
            .addShape("main", StructureUtility.transpose(new String[][] { { "AAA" }, { "A~A" } }))
            .addElement('A', StructureUtility.ofBlock(GregTechAPI.sBlockCasings2, 0))
            .build();
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, 1, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece("main", stack, 1, 1, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        boolean formed = checkPiece("main", 1, 1, 0, errors);
        getProxy().setValidSides(
            formed ? EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)) : EnumSet.noneOf(ForgeDirection.class));
        if (formed) createVirtualCPU();
        postCPUClusterChangeEvent();
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    public void checkMaintenance() {}

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        return side == facing
            ? new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()),
                TextureFactory.of(
                    active ? Textures.BlockIcons.OVERLAY_ME_INPUT_HATCH_ACTIVE
                        : Textures.BlockIcons.OVERLAY_ME_INPUT_HATCH) }
            : new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()) };
    }

    public int getCasingTextureID() {
        return GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings2, 0);
    }

    @Override
    protected gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui<?> getGui() {
        return new com.xyp.gtnotgood.common.gui.modularui.QuantumComputerGui(this);
    }
}
