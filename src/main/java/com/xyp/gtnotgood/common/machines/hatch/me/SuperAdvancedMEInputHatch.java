// Fluid transaction and GUI integration adapted from GT5-Unofficial 5.09.54.183 (LGPL-3.0).
// See META-INF/super-storage-input-port/NOTICE.md.
package com.xyp.gtnotgood.common.machines.hatch.me;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStackWatcher;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.util.GTDataUtils;
import gregtech.api.util.GTUtility;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.common.config.MachineStats;
import gregtech.common.tileentities.machines.MTEHatchInputME;
import gregtech.common.tileentities.machines.RecipeCheckReason;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Expanded native stocking hatch with per-fluid reserves and fixed availability limits.
 * Recipe checks share simulated stacks with the controller and commit their consumed difference.
 * All fluid stays in ME until a recipe commits; GUI marks never contain real fluid.
 */
@IMetaTileEntity.SkipGenerateDescription
public class SuperAdvancedMEInputHatch extends MTEHatchInputME {

    public static final int SLOT_COUNT = 900;
    private static final String DATA_IDENTIFIER = "superAdvancedStockingHatch";
    protected final Slot[] slots = new Slot[SLOT_COUNT];
    private final FluidPolicy[] policies = new FluidPolicy[SLOT_COUNT];
    private boolean autoPull;
    private boolean limitedMode;
    private boolean fixedMode;
    private int refreshTime = 100;
    private IStackWatcher watcher;

    public SuperAdvancedMEInputHatch(int id, String name, String regionalName) {
        super(id, true, name, regionalName);
    }

    public SuperAdvancedMEInputHatch(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, true, tier, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SuperAdvancedMEInputHatch(mName, mTier, mDescriptionArray, mTextures);
    }

    public Slot[] getStorageSlots() {
        return slots;
    }

    public FluidPolicy getPolicy(int index) {
        if (index < 0 || index >= SLOT_COUNT) throw new IndexOutOfBoundsException();
        if (policies[index] == null) policies[index] = new FluidPolicy();
        return policies[index];
    }

    public boolean isLimitedMode() {
        return limitedMode;
    }

    public void setLimitedMode(boolean enabled) {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isServerSide() || processingRecipe) return;
        limitedMode = enabled;
        policyChanged();
    }

    public boolean isFixedMode() {
        return fixedMode;
    }

    public void setFixedMode(boolean enabled) {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isServerSide() || processingRecipe) return;
        fixedMode = enabled;
        policyChanged();
    }

    private int availableAmount(int index, long stored) {
        return getPolicy(index).offered(stored, limitedMode, fixedMode);
    }

    public void policyChanged() {
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isServerSide()) {
            getBaseMetaTileEntity().markDirty();
            scheduleRecipeCheck(RecipeCheckReason.THROTTLED);
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long timer) {
        // Native automatic selection has a fixed 16-slot loop. Its flag stays disabled.
        super.onPostTick(tile, timer);
        if (tile.isServerSide() && timer % refreshTime == 0 && !processingRecipe) {
            if (autoPull) refreshFluidList();
            updateAllInformationSlots();
        }
    }

    private void refreshFluidList() {
        if (!isAllowedToWork()) return;
        int free = 0;
        while (free < SLOT_COUNT && slots[free] != null) free++;
        if (free == SLOT_COUNT) return;
        try {
            boolean changed = false;
            for (IAEFluidStack stack : getProxy().getStorage().getFluidInventory().getStorageList()) {
                if (stack.getStackSize() < minAutoPullAmount || getMatchingSlot(stack.getFluidStack(), false) != null)
                    continue;
                slots[free] = new Slot(GTUtility.copyAmount(1, stack.getFluidStack()));
                changed = true;
                while (free < SLOT_COUNT && slots[free] != null) free++;
                if (free == SLOT_COUNT) break;
            }
            // Retain marked fluids and their policies when ME runs out of stock.
            if (changed) {
                configureWatchers();
                policyChanged();
                scheduleRecipeCheck(RecipeCheckReason.IMMEDIATE);
            }
        } catch (GridAccessException ignored) {}
    }

    @Override
    public boolean isAutoPullFluidList() {
        return autoPull;
    }

    @Override
    public void setAutoPullFluidList(boolean enabled) {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isServerSide()) return;
        autoPull = enabled;
        if (enabled) refreshFluidList();
        policyChanged();
    }

    @Override
    public int getAutoPullRefreshTime() {
        return refreshTime;
    }

    @Override
    public void setAutoPullRefreshTime(int ticks) {
        refreshTime = Math.max(1, ticks);
        policyChanged();
    }

    @Override
    public void setMinAutoPullAmount(int amount) {
        minAutoPullAmount = Math.max(1, amount);
        policyChanged();
    }

    @Override
    public void setSlotConfig(int index, FluidStack config) {
        if (index < 0 || index >= SLOT_COUNT || processingRecipe) return;
        if (config != null) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (i != index && slots[i] != null && GTUtility.areFluidsEqual(slots[i].config, config)) return;
            }
        }
        if (slots[index] != null && GTUtility.areFluidsEqual(slots[index].config, config)) return;
        slots[index] = config == null ? null : new Slot(GTUtility.copyAmount(1, config));
        policies[index] = null;
        configureWatchers();
        policyChanged();
    }

    @Override
    protected void clearSlotConfigs() {
        if (processingRecipe) return;
        Arrays.fill(slots, null);
        Arrays.fill(policies, null);
        configureWatchers();
        policyChanged();
    }

    private BaseActionSource actionSource() {
        if (requestSource == null) requestSource = new MachineSource((IActionHost) getBaseMetaTileEntity());
        return requestSource;
    }

    private long available(IMEMonitor<IAEFluidStack> inventory, FluidStack fluid) {
        IAEFluidStack request = AEFluidStack.create(fluid);
        request.setStackSize(Long.MAX_VALUE);
        IAEFluidStack result = inventory.extractItems(request, Actionable.SIMULATE, actionSource());
        return result == null ? 0 : Math.max(0, result.getStackSize());
    }

    @Override
    public void updateInformationSlot(int index) throws GridAccessException {
        if (index < 0 || index >= SLOT_COUNT || slots[index] == null) return;
        Slot slot = slots[index];
        if (!isAllowedToWork()) {
            slot.resetExtracted();
            return;
        }
        int amount = availableAmount(index, available(getProxy().getStorage().getFluidInventory(), slot.config));
        slot.extracted = amount == 0 ? null : GTUtility.copyAmount(amount, slot.config);
        slot.extractedAmount = amount;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection side) {
        if (side != ForgeDirection.UNKNOWN || !isAllowedToWork()) return EMPTY_FLUID_TANK_INFOS;
        List<FluidTankInfo> tanks = new ObjectArrayList<>();
        try {
            IMEMonitor<IAEFluidStack> inventory = getProxy().getStorage().getFluidInventory();
            for (int i = 0; i < SLOT_COUNT; i++) {
                Slot slot = slots[i];
                if (slot == null) continue;
                FluidStack fluid = processingRecipe ? slot.extracted
                    : GTUtility.copyAmount(availableAmount(i, available(inventory, slot.config)), slot.config);
                if (fluid != null && fluid.amount > 0) tanks.add(new FluidTankInfo(fluid, Integer.MAX_VALUE));
            }
        } catch (GridAccessException ignored) {}
        return tanks.toArray(EMPTY_FLUID_TANK_INFOS);
    }

    @Override
    public FluidStack drain(ForgeDirection side, FluidStack fluid, boolean doDrain) {
        return drain(side, fluid, fluid == null ? 0 : fluid.amount, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection side, FluidStack fluid, int amount, boolean doDrain) {
        if (side != ForgeDirection.UNKNOWN || fluid == null || amount < 0) return null;
        Slot slot = getMatchingSlot(fluid, processingRecipe);
        if (slot == null) return null;
        if (processingRecipe) {
            int drained = Math.min(amount, slot.extracted.amount);
            if (slot.extracted.amount <= 0) return null;
            FluidStack result = GTUtility.copyAmount(drained, slot.config);
            if (doDrain) slot.extracted.amount -= drained;
            return result;
        }
        try {
            IMEMonitor<IAEFluidStack> inventory = getProxy().getStorage().getFluidInventory();
            int index = Arrays.asList(slots).indexOf(slot);
            int offered = availableAmount(index, available(inventory, fluid));
            int drained = Math.min(amount, offered);
            if (offered == 0) return null;
            IAEFluidStack request = AEFluidStack.create(fluid);
            request.setStackSize(drained);
            if (drained == 0) return GTUtility.copyAmount(0, fluid);
            IAEFluidStack result = doDrain
                ? Platform.poweredExtraction(getProxy().getEnergy(), inventory, request, actionSource())
                : inventory.extractItems(request, Actionable.SIMULATE, actionSource());
            if (doDrain) updateAllInformationSlots();
            return result == null ? null : result.getFluidStack();
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    @Override
    public void startRecipeProcessing() {
        if (processingRecipe) return;
        cachedActivity = isAllowedToWork();
        processingRecipe = true;
        updateAllInformationSlots();
    }

    @Override
    public CheckRecipeResult endRecipeProcessing(MTEMultiBlockBase controller) {
        if (!processingRecipe) return CheckRecipeResultRegistry.SUCCESSFUL;
        CheckRecipeResult result = CheckRecipeResultRegistry.SUCCESSFUL;
        try {
            IMEMonitor<IAEFluidStack> inventory = getProxy().getStorage().getFluidInventory();
            IEnergyGrid energy = getProxy().getEnergy();
            for (int i = 0; i < SLOT_COUNT; i++) {
                Slot slot = slots[i];
                if (slot == null || slot.extracted == null) continue;
                int consumed = slot.extractedAmount - slot.extracted.amount;
                slot.extractedAmount = slot.extracted.amount;
                if (consumed <= 0) continue;
                // Another hatch may have consumed stock since the simulation. Recheck the reserve at commit.
                if (availableAmount(i, available(inventory, slot.config)) < consumed) {
                    result = extractionFailed(controller);
                    break;
                }
                IAEFluidStack request = AEFluidStack.create(slot.config);
                request.setStackSize(consumed);
                IAEFluidStack extracted = Platform.poweredExtraction(energy, inventory, request, actionSource());
                if (extracted == null || extracted.getStackSize() != consumed) {
                    result = extractionFailed(controller);
                    break;
                }
            }
        } catch (GridAccessException ignored) {
            result = extractionFailed(controller);
        } finally {
            processingRecipe = false;
        }
        return result;
    }

    private CheckRecipeResult extractionFailed(MTEMultiBlockBase controller) {
        controller.stopMachine(ShutDownReasonRegistry.CRITICAL_NONE);
        return SimpleCheckRecipeResult.ofFailurePersistOnShutdown("stocking_hatch_fail_extraction");
    }

    private NBTTagList writeConfiguration() {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slots[i] == null) continue;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("index", i);
            tag.setTag("fluid", slots[i].config.writeToNBT(new NBTTagCompound()));
            getPolicy(i).write(tag);
            list.appendTag(tag);
        }
        return list;
    }

    private void readConfiguration(NBTTagList list) {
        Arrays.fill(slots, null);
        Arrays.fill(policies, null);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int index = tag.getInteger("index");
            if (index < 0 || index >= SLOT_COUNT) continue;
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("fluid"));
            if (fluid == null) continue;
            setSlotConfig(index, fluid);
            getPolicy(index).read(tag);
        }
        configureWatchers();
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setTag("superStorageSlots", writeConfiguration());
        tag.setBoolean("superAutoPull", autoPull);
        tag.setBoolean("limitedMode", limitedMode);
        tag.setBoolean("fixedMode", fixedMode);
        tag.setInteger("superRefreshTime", refreshTime);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        autoPullFluidList = false;
        autoPull = tag.getBoolean("superAutoPull");
        limitedMode = tag.getBoolean("limitedMode");
        fixedMode = tag.getBoolean("fixedMode");
        minAutoPullAmount = Math.max(1, minAutoPullAmount);
        refreshTime = tag.hasKey("superRefreshTime") ? Math.max(1, tag.getInteger("superRefreshTime")) : 100;
        readConfiguration(tag.getTagList("superStorageSlots", 10));
    }

    @Override
    public String getCopiedDataIdentifier(EntityPlayer player) {
        return DATA_IDENTIFIER;
    }

    @Override
    public NBTTagCompound getCopiedData(EntityPlayer player) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", DATA_IDENTIFIER);
        tag.setTag("superStorageSlots", writeConfiguration());
        tag.setBoolean("superAutoPull", autoPull);
        tag.setBoolean("limitedMode", limitedMode);
        tag.setBoolean("fixedMode", fixedMode);
        tag.setInteger("minAmount", minAutoPullAmount);
        tag.setInteger("superRefreshTime", refreshTime);
        tag.setBoolean("additionalConnection", connectsToAllSides());
        tag.setByte("color", getColor());
        return tag;
    }

    @Override
    public boolean pasteCopiedData(EntityPlayer player, NBTTagCompound tag) {
        if (processingRecipe || tag == null || !DATA_IDENTIFIER.equals(tag.getString("type"))) return false;
        readConfiguration(tag.getTagList("superStorageSlots", 10));
        autoPull = tag.getBoolean("superAutoPull");
        limitedMode = tag.getBoolean("limitedMode");
        fixedMode = tag.getBoolean("fixedMode");
        setMinAutoPullAmount(tag.getInteger("minAmount"));
        setAutoPullRefreshTime(tag.getInteger("superRefreshTime"));
        setConnectsToAllSides(tag.getBoolean("additionalConnection"));
        getBaseMetaTileEntity().setColorization(tag.getByte("color"));
        policyChanged();
        return true;
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        setAutoPullFluidList(!autoPull);
        player.addChatMessage(new ChatComponentTranslation(
            "GT5U.machines.stocking_hatch.auto_pull_toggle." + (autoPull ? "enabled" : "disabled")));
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setBoolean("autoPull", autoPull);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new SuperAdvancedMEInputHatchGui(this, slots).build(data, sync, settings);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr gtng.super_storage.slots
            // # Marks up to 900 fluids; connects to ME using one channel
            // # zh_CN 可标记900种流体，连接ME网络并占用一个频道
            StatCollector.translateToLocal("gtng.super_storage.slots"),
            // #tr gtng.super_storage.reserve
            // # Per-fluid reserve: only stock above the reserve can be consumed
            // # zh_CN 每种流体可设置保留量，仅允许使用超出保留量的部分
            StatCollector.translateToLocal("gtng.super_storage.reserve"),
            // #tr gtng.super_storage.fixed
            // # Fixed mode: offers the configured amount per recipe check; waits if insufficient
            // # zh_CN 固定模式：每次配方检查提供设定量，数量不足时等待
            StatCollector.translateToLocal("gtng.super_storage.fixed"),
            // #tr gtng.super_storage.virtual
            // # No fluid buffer: consumes only recipe inputs; pickup clears marks and settings
            // # zh_CN 无实体缓存，仅扣除配方消耗；挖掉后清除标记和设置
            StatCollector.translateToLocal("gtng.super_storage.virtual"),
            // #tr gtng.super_storage.config
            // # Right-click a marked fluid to configure; values are in mB
            // # zh_CN 右键标记流体进行配置，数量单位为mB
            StatCollector.translateToLocal("gtng.super_storage.config"),
            // #tr gtng.super_storage.autopull
            // # Automatic marking retains existing marks and policies; data-stick copy supported
            // # zh_CN 自动标记保留已有标记及规则，支持数据棒复制配置
            StatCollector.translateToLocal("gtng.super_storage.autopull") };
    }

    /** Reserve uses long ME quantities; Forge recipe stacks remain bounded to an int. */
    public static final class FluidPolicy {

        public long reserve;
        public int batch = 1000;

        public int offered(long stored, boolean limited, boolean fixed) {
            long usable = Math.max(0, stored - (limited ? Math.max(0, reserve) : 0));
            int quantity = Math.max(1, batch);
            return fixed ? (usable >= quantity ? quantity : 0) : (int) Math.min(Integer.MAX_VALUE, usable);
        }

        private void write(NBTTagCompound tag) {
            tag.setLong("reserve", Math.max(0, reserve));
            tag.setInteger("batch", Math.max(1, batch));
        }

        private void read(NBTTagCompound tag) {
            reserve = Math.max(0, tag.getLong("reserve"));
            batch = tag.hasKey("batch") ? Math.max(1, tag.getInteger("batch")) : 1000;
        }
    }

    public FluidStack[] getStoredFluids() {
        if (!isAllowedToWork()) {
            return new FluidStack[0];
        }

        if (processingRecipe) {
            List<FluidStack> fluids = new ObjectArrayList<>(GTDataUtils.countNonNulls(slots));

            for (Slot slot : slots) {
                if (slot == null) continue;

                // Must pass the reference out to the multi
                if (slot.extracted != null) fluids.add(slot.extracted);
            }

            return fluids.toArray(GTValues.emptyFluidStackArray);
        } else {
            List<FluidStack> fluids = new ObjectArrayList<>(GTDataUtils.countNonNulls(slots));

            for (Slot slot : slots) {
                if (slot == null) continue;

                // The caller should only use this to determine the configuration.
                // If it wants to know more, it can query AE itself.
                fluids.add(GTUtility.copyAmount(1, slot.config));
            }

            return fluids.toArray(GTValues.emptyFluidStackArray);
        }
    }

    protected void updateAllInformationSlots() {
        clearExtractedStacks();
        if (isAllowedToWork()) {
            try {
                for (int index = 0; index < SLOT_COUNT; index++) {
                    updateInformationSlot(index);
                }
            } catch (GridAccessException e) {
                clearExtractedStacks();
            }
        } else {
            clearExtractedStacks();
        }
    }

    public FluidStack getSlotConfig(int index) {
        Slot slot = GTDataUtils.getIndexSafe(slots, index);

        return slot == null || slot.config == null ? null : slot.config.copy();
    }

    public boolean setSlotConfigAndUpdate(int index, FluidStack config) {
        if (index < 0 || index >= slots.length || processingRecipe) return false;

        setSlotConfig(index, config);
        if (!GTUtility.areFluidsEqual(getSlotConfig(index), config)) return false;

        try {
            updateInformationSlot(index);
        } catch (GridAccessException ignored) {}

        return true;
    }

    protected void clearExtractedStacks() {
        for (Slot slot : slots) {
            if (slot == null) continue;

            slot.resetExtracted();
        }
    }

    protected Slot getMatchingSlot(FluidStack fluidStack, boolean requireExtracted) {
        if (fluidStack == null) return null;
        if (!isAllowedToWork()) return null;

        for (int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];

            if (slot == null) continue;

            if (requireExtracted && (slot.extracted == null || slot.extractedAmount == 0)) continue;

            if (!GTUtility.areFluidsEqual(slot.config, fluidStack)) continue;

            return slot;
        }

        return null;
    }

    public FluidStack getFirstValidStack() {
        return getFirstValidStack(false);
    }

    public FluidStack getFirstValidStack(boolean slotsMustMatch) {
        if (slotsMustMatch) {
            FluidStack firstValid = null;

            for (Slot slot : slots) {
                if (slot == null || slot.extracted == null) continue;

                if (firstValid == null) {
                    firstValid = slot.extracted;
                } else {
                    if (!GTUtility.areFluidsEqual(firstValid, slot.extracted)) {
                        return null;
                    }
                }
            }

            return firstValid;
        } else {
            for (Slot slot : slots) {
                if (slot == null || slot.extracted == null) continue;

                return slot.extracted;
            }

            return null;
        }
    }

    private void configureWatchers() {
        if (this.watcher != null) {
            this.watcher.clear();
            if (MachineStats.machines.useStackWatcher) {
                for (Slot slot : slots) {
                    if (slot != null && slot.config != null) {
                        watcher.add(AEFluidStack.create(slot.config));
                    }
                }
            }
            scheduleRecipeCheck(RecipeCheckReason.THROTTLED);
        }
    }

    public void updateWatcher(IStackWatcher newWatcher) {
        watcher = newWatcher;
        configureWatchers();
    }

    private void scheduleRecipeCheck(RecipeCheckReason reason) {
        for (var multi : watchers) {
            multi.scheduleRecipeCheck(reason);
        }
    }
}
