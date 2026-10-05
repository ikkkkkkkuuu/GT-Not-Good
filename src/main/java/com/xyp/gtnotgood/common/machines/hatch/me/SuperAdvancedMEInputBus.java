// Item transaction and GUI integration adapted from GT5-Unofficial 5.09.54.183 (LGPL-3.0).
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
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import gregtech.api.interfaces.INonConsumedItemDisplay;
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
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.RecipeCheckReason;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Expanded native stocking bus with per-item reserves and fixed availability limits.
 * Recipe checks share simulated stacks with the controller and commit their consumed difference.
 * All marked items stay in ME until a recipe commits; marks never contain real items.
 */
@IMetaTileEntity.SkipGenerateDescription
public class SuperAdvancedMEInputBus extends MTEHatchInputBusME {

    public static final int SLOT_COUNT = 900;
    private static final String DATA_IDENTIFIER = "superAdvancedStockingBus";
    protected final Slot[] slots = new Slot[SLOT_COUNT];
    private final ItemPolicy[] policies = new ItemPolicy[SLOT_COUNT];
    private boolean autoPull;
    private boolean limitedMode;
    private boolean fixedMode;
    private int refreshTime = 100;
    private IStackWatcher watcher;

    public SuperAdvancedMEInputBus(int id, String name, String regionalName) {
        super(id, true, name, regionalName);
    }

    public SuperAdvancedMEInputBus(String name, int tier, String[] description, ITexture[][][] textures) {
        super(name, true, tier, description, textures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SuperAdvancedMEInputBus(mName, mTier, mDescriptionArray, mTextures);
    }

    public Slot[] getStorageSlots() {
        return slots;
    }

    public ItemPolicy getPolicy(int index) {
        if (index < 0 || index >= SLOT_COUNT) throw new IndexOutOfBoundsException();
        if (policies[index] == null) policies[index] = new ItemPolicy();
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
            if (autoPull) refreshItemList();
            updateAllInformationSlots();
        }
    }

    protected void refreshItemList() {
        if (!isAllowedToWork()) return;
        int free = 0;
        while (free < SLOT_COUNT && slots[free] != null) free++;
        if (free == SLOT_COUNT) return;
        try {
            boolean changed = false;
            for (IAEItemStack stack : getProxy().getStorage()
                .getItemInventory()
                .getStorageList()) {
                if (stack.getStackSize() < minAutoPullStackSize || getMatchingSlot(stack.getItemStack(), false) != null)
                    continue;
                slots[free] = new Slot(GTUtility.copyAmount(1, stack.getItemStack()));
                changed = true;
                while (free < SLOT_COUNT && slots[free] != null) free++;
                if (free == SLOT_COUNT) break;
            }
            // Retain marked items and their policies when ME runs out of stock.
            if (changed) {
                configureWatchers();
                policyChanged();
                scheduleRecipeCheck(RecipeCheckReason.IMMEDIATE);
            }
        } catch (GridAccessException ignored) {}
    }

    @Override
    public boolean isAutoPullItemList() {
        return autoPull;
    }

    @Override
    public void setAutoPullItemList(boolean enabled) {
        if (getBaseMetaTileEntity() == null || !getBaseMetaTileEntity().isServerSide()) return;
        autoPull = enabled;
        if (enabled) refreshItemList();
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
    public void setMinAutoPullStackSize(int amount) {
        minAutoPullStackSize = Math.max(1, amount);
        policyChanged();
    }

    @Override
    public void setSlotConfig(int index, ItemStack config) {
        if (index < 0 || index >= SLOT_COUNT || processingRecipe) return;
        if (config != null) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (i != index && slots[i] != null && GTUtility.areStacksEqual(slots[i].config, config)) return;
            }
        }
        if (slots[index] != null && GTUtility.areStacksEqual(slots[index].config, config)) return;
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

    private long available(IMEMonitor<IAEItemStack> inventory, ItemStack item) {
        IAEItemStack request = AEItemStack.create(item);
        request.setStackSize(Long.MAX_VALUE);
        IAEItemStack result = inventory.extractItems(request, Actionable.SIMULATE, actionSource());
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
        int amount = availableAmount(
            index,
            available(
                getProxy().getStorage()
                    .getItemInventory(),
                slot.config));
        slot.extracted = amount == 0 ? null : GTUtility.copyAmountUnsafe(amount, slot.config);
        slot.extractedAmount = amount;
    }

    @Override
    public int getSizeInventory() {
        return SLOT_COUNT + 2;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index < 0 || index >= getSizeInventory()) return null;
        int offset = processingRecipe ? SLOT_COUNT : 0;
        if (index == getCircuitSlot() + offset) return mInventory[getCircuitSlot()];
        if (index == getManualSlot() + offset) return mInventory[getManualSlot()];
        if (!processingRecipe || !isAllowedToWork() || index >= SLOT_COUNT) return null;
        return slots[index] == null ? null : slots[index].extracted;
    }

    /** Recipe helpers may consume through inventory APIs instead of mutating the shared snapshot. */
    @Override
    public ItemStack decrStackSize(int index, int amount) {
        if (amount <= 0) return null;
        if (!processingRecipe) return index >= 0 && index < 2 ? super.decrStackSize(index, amount) : null;
        if (index >= SLOT_COUNT && index < SLOT_COUNT + 2) {
            int physical = index - SLOT_COUNT;
            ItemStack stack = mInventory[physical];
            if (stack == null) return null;
            ItemStack taken = stack.splitStack(Math.min(amount, stack.stackSize));
            if (stack.stackSize == 0) mInventory[physical] = null;
            return taken;
        }
        ItemStack stack = getStackInSlot(index);
        if (stack == null || stack.stackSize <= 0) return null;
        int taken = Math.min(amount, stack.stackSize);
        stack.stackSize -= taken;
        return GTUtility.copyAmountUnsafe(taken, stack);
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
            IMEMonitor<IAEItemStack> inventory = getProxy().getStorage()
                .getItemInventory();
            IEnergyGrid energy = getProxy().getEnergy();
            for (int i = 0; i < SLOT_COUNT; i++) {
                Slot slot = slots[i];
                if (slot == null || slot.extracted == null) continue;
                int consumed = slot.extractedAmount - slot.extracted.stackSize;
                slot.extractedAmount = slot.extracted.stackSize;
                if (consumed <= 0) continue;
                // Another hatch may have consumed stock since the simulation. Recheck the reserve at commit.
                if (availableAmount(i, available(inventory, slot.config)) < consumed) {
                    result = extractionFailed(controller);
                    break;
                }
                IAEItemStack request = AEItemStack.create(slot.config);
                request.setStackSize(consumed);
                IAEItemStack extracted = Platform.poweredExtraction(energy, inventory, request, actionSource());
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
        return SimpleCheckRecipeResult.ofFailurePersistOnShutdown("stocking_bus_fail_extraction");
    }

    private NBTTagList writeConfiguration() {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slots[i] == null) continue;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("index", i);
            tag.setTag("item", slots[i].config.writeToNBT(new NBTTagCompound()));
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
            ItemStack item = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("item"));
            if (item == null) continue;
            setSlotConfig(index, item);
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
        autoPullItemList = false;
        autoPull = tag.getBoolean("superAutoPull");
        limitedMode = tag.getBoolean("limitedMode");
        fixedMode = tag.getBoolean("fixedMode");
        minAutoPullStackSize = Math.max(1, minAutoPullStackSize);
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
        tag.setInteger("minAmount", minAutoPullStackSize);
        tag.setInteger("superRefreshTime", refreshTime);
        tag.setBoolean("additionalConnection", connectsToAllSides());
        tag.setByte("color", getColor());
        tag.setTag("circuit", GTUtility.saveItem(mInventory[getCircuitSlot()]));
        return tag;
    }

    @Override
    public boolean pasteCopiedData(EntityPlayer player, NBTTagCompound tag) {
        if (processingRecipe || tag == null || !DATA_IDENTIFIER.equals(tag.getString("type"))) return false;
        readConfiguration(tag.getTagList("superStorageSlots", 10));
        autoPull = tag.getBoolean("superAutoPull");
        limitedMode = tag.getBoolean("limitedMode");
        fixedMode = tag.getBoolean("fixedMode");
        setMinAutoPullStackSize(tag.getInteger("minAmount"));
        setAutoPullRefreshTime(tag.getInteger("superRefreshTime"));
        setConnectsToAllSides(tag.getBoolean("additionalConnection"));
        getBaseMetaTileEntity().setColorization(tag.getByte("color"));
        mInventory[getCircuitSlot()] = GTUtility.loadItem(tag.getCompoundTag("circuit"));
        policyChanged();
        return true;
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        setAutoPullItemList(!autoPull);
        player.addChatMessage(
            new ChatComponentTranslation(
                "GT5U.machines.stocking_bus.auto_pull_toggle." + (autoPull ? "enabled" : "disabled")));
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setBoolean("autoPull", autoPull);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return new SuperAdvancedMEInputBusGui(this, slots).build(data, sync, settings);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr gtng.super_storage_bus.slots
            // # Marks up to 900 items; connects to ME using one channel
            // # zh_CN 可标记900种物品，连接ME网络并占用一个频道
            StatCollector.translateToLocal("gtng.super_storage_bus.slots"),
            // #tr gtng.super_storage_bus.reserve
            // # Per-item reserve: only stock above the reserve can be consumed
            // # zh_CN 每种物品可设置保留量，仅允许使用超出保留量的部分
            StatCollector.translateToLocal("gtng.super_storage_bus.reserve"),
            // #tr gtng.super_storage_bus.fixed
            // # Fixed mode: offers the configured amount per recipe check; waits if insufficient
            // # zh_CN 固定模式：每次配方检查提供设定量，数量不足时等待
            StatCollector.translateToLocal("gtng.super_storage_bus.fixed"),
            // #tr gtng.super_storage_bus.virtual
            // # No item buffer: consumes only recipe inputs; pickup clears marks and settings
            // # zh_CN 无实体缓存，仅扣除配方消耗；挖掉后清除标记和设置
            StatCollector.translateToLocal("gtng.super_storage_bus.virtual"),
            // #tr gtng.super_storage_bus.config
            // # Right-click a marked item to configure; values are item counts
            // # zh_CN 右键标记物品进行配置，数量单位为个
            StatCollector.translateToLocal("gtng.super_storage_bus.config"),
            // #tr gtng.super_storage_bus.autopull
            // # Automatic marking retains existing marks and policies; data-stick copy supported
            // # zh_CN 自动标记保留已有标记及规则，支持数据棒复制配置
            StatCollector.translateToLocal("gtng.super_storage_bus.autopull") };
    }

    /** Reserve uses long ME quantities; Minecraft recipe stacks remain bounded to an int. */
    public static final class ItemPolicy {

        public long reserve;
        public int batch = 64;

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
            batch = tag.hasKey("batch") ? Math.max(1, tag.getInteger("batch")) : 64;
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

    public ItemStack getSlotConfig(int index) {
        Slot slot = GTDataUtils.getIndexSafe(slots, index);

        return slot == null || slot.config == null ? null : slot.config.copy();
    }

    public boolean setSlotConfigAndUpdate(int index, ItemStack config) {
        if (index < 0 || index >= slots.length || processingRecipe) return false;

        setSlotConfig(index, config);
        if (!GTUtility.areStacksEqual(getSlotConfig(index), config)) return false;

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

    protected Slot getMatchingSlot(ItemStack itemStack, boolean requireExtracted) {
        if (itemStack == null) return null;
        if (!isAllowedToWork()) return null;

        for (int i = 0; i < slots.length; i++) {
            Slot slot = slots[i];

            if (slot == null) continue;

            if (requireExtracted && (slot.extracted == null || slot.extractedAmount == 0)) continue;

            if (!GTUtility.areStacksEqual(slot.config, itemStack)) continue;

            return slot;
        }

        return null;
    }

    public ItemStack getFirstValidStack() {
        return getFirstValidStack(false);
    }

    public ItemStack getFirstValidStack(boolean slotsMustMatch) {
        if (slotsMustMatch) {
            ItemStack firstValid = null;

            for (Slot slot : slots) {
                if (slot == null || slot.extracted == null) continue;

                if (firstValid == null) {
                    firstValid = slot.extracted;
                } else {
                    if (!GTUtility.areStacksEqual(firstValid, slot.extracted)) {
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
                        watcher.add(AEItemStack.create(slot.config));
                    }
                }
            }
            scheduleRecipeCheck(RecipeCheckReason.THROTTLED);
        }
    }

    @Override
    public List<Integer> getPhysicalCircuitNumbers() {
        List<Integer> result = new ObjectArrayList<>();
        for (Slot slot : slots) {
            if (slot != null && GTUtility.isAnyIntegratedCircuit(slot.config)) result.add(slot.config.getItemDamage());
        }
        return result;
    }

    @Override
    public List<ItemStack> getNonConsumedInputDisplayItems() {
        List<ItemStack> result = new ObjectArrayList<>();
        for (Slot slot : slots) {
            if (slot != null && INonConsumedItemDisplay.isDisplayableItem(mRecipeMap, slot.config))
                result.add(slot.config);
        }
        return result;
    }

    @Override
    public List<ItemStack> getItemsForHoloGlasses() {
        List<ItemStack> result = new ObjectArrayList<>();
        for (Slot slot : slots) {
            if (slot != null && slot.extracted != null) result.add(slot.extracted);
        }
        return result;
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
