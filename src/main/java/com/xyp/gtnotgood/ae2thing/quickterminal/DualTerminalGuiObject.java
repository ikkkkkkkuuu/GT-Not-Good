package com.xyp.gtnotgood.ae2thing.quickterminal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.compat.FluidDropCompat;
import com.xyp.gtnotgood.common.packaged.ArcaneWorkbenchPatterns;

import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.events.MENetworkBootingStatusChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.parts.IInterfaceTerminal;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.items.contents.WirelessPatternTerminalGuiObject;
import appeng.tile.inventory.InvOperation;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

public final class DualTerminalGuiObject extends WirelessPatternTerminalGuiObject implements IInterfaceTerminal {

    private static final String QUICK_CRAFTING_MODE = "gtnhQolQuickCraftingMode";
    private static final String QUICK_PROCESSING_GRID_SIZE = "gtnhQolQuickProcessingGridSize";
    private static final String QUICK_CRAFTING_PIN_ROWS = "gtnhQolQuickCraftingPinRows";
    private static final String QUICK_PLAYER_PIN_ROWS = "gtnhQolQuickPlayerPinRows";
    private static final String QUICK_COMBINE = "gtnotgoodQuickCombine";
    private static final String QUICK_PRIORITIZE_FLUIDS = "gtnotgoodQuickPrioritize";
    private static final String QUICK_PROCESSING_FLUID_INPUTS = "gtnhQolQuickProcessingFluidInputs";
    private static final String QUICK_CRAFTING_SNAPSHOT = "gtnhQolQuickCraftingSnapshot";
    private static final String QUICK_PROCESSING_SNAPSHOT = "gtnhQolQuickProcessingSnapshot";
    private static final String QUICK_PATTERN_SNAPSHOTS_LINKED = "gtnhQolQuickPatternSnapshotsLinked";
    private static final String QUICK_LEGACY_32_SLOT_BACKUP = "gtnotgoodQuickLegacy32SlotBackup";
    private static final String SNAPSHOT_INPUTS = "Inputs";
    private static final String SNAPSHOT_OUTPUTS = "Outputs";
    private static final String NATIVE_PATTERN_DATA = "pattern_ex";
    private static final String LEGACY_CRAFTING = "crafting";
    private static final String LEGACY_PROCESSING_INPUTS = "crafting_ex";
    private static final String LEGACY_PROCESSING_OUTPUTS = "output_ex";

    private boolean needsUpdate = true;
    /** Suppresses output-slot layout replacement during native encoding and stored inventory restoration. */
    private boolean encodingPattern;

    /** Whether an imported arcane recipe is displayed in the workbench panel instead of the processing panel. */
    public boolean isArcaneWorkbenchView() {
        return !Platform.openNbtData(getItemStack())
            .getBoolean("GTNGArcaneProcessingView");
    }

    public void setArcaneWorkbenchView(boolean workbench) {
        Platform.openNbtData(getItemStack())
            .setBoolean("GTNGArcaneProcessingView", !workbench);
    }

    /** Restores shaped positions after AE loads the unordered bill from an encoded workbench pattern. */
    private void restoreArcaneInputs() {
        NBTTagList layout = getArcaneLayout();
        var inputs = getAEInventoryByName(StorageName.CRAFTING_INPUT);
        if (isCraftingRecipe() || inputs == null || !ArcaneWorkbenchPatterns.matchesInputs(layout, inputs)) return;
        for (int i = 0; i < inputs.getSizeInventory(); i++) {
            inputs.putAEStackInSlot(
                i,
                i < 9 ? AEItemStack.create(ItemStack.loadItemStackFromNBT(layout.getCompoundTagAt(i))) : null);
        }
    }

    /** Restoring stored inventories must not replace a pending NEI layout with the previous encoded output's tag. */
    @Override
    public void readInventory() {
        encodingPattern = true;
        try {
            super.readInventory();
            restoreArcaneInputs();
        } finally {
            encodingPattern = false;
        }
    }

    /** Persists the pending processing recipe's layout across terminal closes and crafting-mode round trips. */
    public void setArcaneLayout(NBTTagList layout) {
        NBTTagCompound data = Platform.openNbtData(getItemStack());
        if (layout == null || layout.tagCount() != 9) data.removeTag(ArcaneWorkbenchPatterns.GRID);
        else data.setTag(ArcaneWorkbenchPatterns.GRID, layout.copy());
    }

    public NBTTagList getArcaneLayout() {
        return (NBTTagList) Platform.openNbtData(getItemStack())
            .getTagList(ArcaneWorkbenchPatterns.GRID, 10)
            .copy();
    }

    /** Loading an existing pattern restores its layout; taking the encoded result leaves the pending recipe intact. */
    @Override
    public void onChangeInventory(IInventory inventory, int slot, InvOperation operation, ItemStack removed,
        ItemStack added) {
        if (!encodingPattern && inventory == getInventoryByName("pattern") && slot == 1 && added != null) {
            setArcaneLayout(
                added.hasTagCompound() ? added.getTagCompound()
                    .getTagList(ArcaneWorkbenchPatterns.GRID, 10) : null);
        }
        super.onChangeInventory(inventory, slot, operation, removed, added);
        if (!encodingPattern && inventory == getInventoryByName("pattern") && slot == 1 && added != null) {
            restoreArcaneInputs();
        }
    }

    /**
     * Adds the workbench layout inside AE's native encoding transaction, before container synchronization or
     * automatic placement. All native blank-pattern consumption and output handling remain authoritative.
     * A changed bill cannot be encoded with stale layout metadata.
     */
    @Override
    public boolean encode(IEnergySource powerSource, IMEMonitor<IAEItemStack> itemMonitor,
        BaseActionSource actionSource, String author, World world) {
        NBTTagList layout = getArcaneLayout();
        boolean arcane = !isCraftingRecipe() && layout.tagCount() == 9;
        if (arcane
            && !ArcaneWorkbenchPatterns.matchesInputs(layout, getAEInventoryByName(StorageName.CRAFTING_INPUT))) {
            return false;
        }
        encodingPattern = true;
        try {
            if (!super.encode(powerSource, itemMonitor, actionSource, author, world)) return false;
            if (arcane) {
                IInventory patterns = getInventoryByName("pattern");
                NBTTagCompound encoded = patterns.getStackInSlot(1)
                    .getTagCompound();
                encoded.setTag(ArcaneWorkbenchPatterns.GRID, layout);
                encoded.setBoolean("substitute", false);
                patterns.markDirty();
            }
            return true;
        } finally {
            encodingPattern = false;
        }
    }

    /** Updates every occurrence in the shaped layout when the terminal cycles an NEI ingredient alternative. */
    public void replaceArcaneIngredient(IAEStack<?> from, IAEStack<?> to) {
        if (from == null || to == null || !from.isItem() || !to.isItem()) return;
        NBTTagList layout = getArcaneLayout();
        if (layout.tagCount() != 9) return;
        ItemStack previous = ((IAEItemStack) from).getItemStack();
        NBTTagList updated = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            NBTTagCompound entry = layout.getCompoundTagAt(i);
            ItemStack current = ItemStack.loadItemStackFromNBT(entry);
            if (current != null && current.isItemEqual(previous)
                && ItemStack.areItemStackTagsEqual(current, previous)) {
                ItemStack replacement = ((IAEItemStack) to).getItemStack()
                    .copy();
                replacement.stackSize = 1;
                entry = replacement.writeToNBT(new NBTTagCompound());
            }
            updated.appendTag(entry);
        }
        setArcaneLayout(updated);
    }

    public DualTerminalGuiObject(IWirelessTermHandler handler, ItemStack stack, EntityPlayer player, World world,
        int slot) {
        super(handler, stack, player, world, slot, 2, 0);
        migrateLegacyTerminalData();
        if (!world.isRemote) {
            archiveLegacy32SlotData();
            setProcessingGridSize(4);
        }
    }

    /**
     * Converts the old AE2Things-style item inventories into AE2's native
     * wireless-pattern-terminal compound without deleting the legacy data.
     * Existing auxiliary GT-Not-Cool screens can therefore continue reading
     * the old tags during the staged refactor.
     */
    private void migrateLegacyTerminalData() {
        NBTTagCompound data = Platform.openNbtData(getItemStack());
        if (!data.hasKey(QUICK_COMBINE) && data.hasKey("combine")) {
            data.setBoolean(QUICK_COMBINE, data.getBoolean("combine"));
        }
        if (!data.hasKey(QUICK_PRIORITIZE_FLUIDS) && data.hasKey("priorization")) {
            data.setBoolean(QUICK_PRIORITIZE_FLUIDS, data.getBoolean("priorization"));
        }
        if (data.hasKey(NATIVE_PATTERN_DATA, 10)) return;
        if (!data.hasKey(LEGACY_CRAFTING, 10) && !data.hasKey(LEGACY_PROCESSING_INPUTS, 10)
            && !data.hasKey(LEGACY_PROCESSING_OUTPUTS, 10)
            && !data.hasKey("pattern", 10)) return;

        IAEStack<?>[] craftingInputs = readLegacyInventory(data, LEGACY_CRAFTING, 32);
        IAEStack<?>[] processingInputs = readLegacyInventory(data, LEGACY_PROCESSING_INPUTS, 32);
        IAEStack<?>[] processingOutputs = readLegacyInventory(data, LEGACY_PROCESSING_OUTPUTS, 32);
        boolean craftingMode = data.getBoolean("craftingMode");

        NBTTagCompound nativePattern = new NBTTagCompound();
        nativePattern.setBoolean("substitute", data.getBoolean("substitute"));
        nativePattern.setBoolean("beSubstitute", data.getBoolean("beSubstitute"));
        nativePattern.setBoolean("inverted", data.getBoolean("inverted"));
        nativePattern.setInteger("activePage", data.getInteger("activePage"));
        nativePattern.setTag("craftingGrid", writeStackInventory(craftingMode ? craftingInputs : processingInputs));
        if (!craftingMode) nativePattern.setTag("outputList", writeStackInventory(processingOutputs));
        if (data.hasKey("pattern", 10)) {
            nativePattern.setTag(
                "pattern",
                data.getCompoundTag("pattern")
                    .copy());
        }
        data.setTag(NATIVE_PATTERN_DATA, nativePattern);
        data.setBoolean(QUICK_CRAFTING_MODE, craftingMode);
        setPatternSnapshot(true, craftingInputs, new IAEStack<?>[32]);
        setPatternSnapshot(false, processingInputs, processingOutputs);
        setPatternSnapshotsLinked(false);
    }

    /**
     * The previous quick-terminal layout allocated two pages (32 slots) per
     * processing side. Keep one untouched backup before the new single-page
     * 16+16 inventory is opened, so slots 17-32 are never destroyed merely by
     * upgrading and opening the terminal.
     */
    private void archiveLegacy32SlotData() {
        NBTTagCompound data = Platform.openNbtData(getItemStack());
        if (data.hasKey(QUICK_LEGACY_32_SLOT_BACKUP, 10) || !data.hasKey(NATIVE_PATTERN_DATA, 10)) return;

        NBTTagCompound nativePattern = data.getCompoundTag(NATIVE_PATTERN_DATA);
        boolean hasOverflow = hasInventoryOverflow(nativePattern, "craftingGrid")
            || hasInventoryOverflow(nativePattern, "outputList")
            || hasSnapshotOverflow(data, QUICK_CRAFTING_SNAPSHOT)
            || hasSnapshotOverflow(data, QUICK_PROCESSING_SNAPSHOT);
        if (!hasOverflow) return;

        NBTTagCompound backup = new NBTTagCompound();
        backup.setTag(NATIVE_PATTERN_DATA, nativePattern.copy());
        if (data.hasKey(QUICK_CRAFTING_SNAPSHOT, 10)) {
            backup.setTag(
                QUICK_CRAFTING_SNAPSHOT,
                data.getCompoundTag(QUICK_CRAFTING_SNAPSHOT)
                    .copy());
        }
        if (data.hasKey(QUICK_PROCESSING_SNAPSHOT, 10)) {
            backup.setTag(
                QUICK_PROCESSING_SNAPSHOT,
                data.getCompoundTag(QUICK_PROCESSING_SNAPSHOT)
                    .copy());
        }
        data.setTag(QUICK_LEGACY_32_SLOT_BACKUP, backup);
    }

    private static boolean hasInventoryOverflow(NBTTagCompound parent, String inventoryName) {
        if (!parent.hasKey(inventoryName, 10)) return false;
        NBTTagCompound inventory = parent.getCompoundTag(inventoryName);
        for (int slot = RecipeTransferPayload.SLOT_COUNT; slot < 32; slot++) {
            if (inventory.hasKey("#" + slot, 10)) return true;
        }
        return false;
    }

    private static boolean hasSnapshotOverflow(NBTTagCompound data, String snapshotName) {
        if (!data.hasKey(snapshotName, 10)) return false;
        NBTTagCompound snapshot = data.getCompoundTag(snapshotName);
        return hasStackListOverflow(snapshot.getTagList(SNAPSHOT_INPUTS, 10))
            || hasStackListOverflow(snapshot.getTagList(SNAPSHOT_OUTPUTS, 10));
    }

    private static boolean hasStackListOverflow(NBTTagList entries) {
        for (int index = 0; index < entries.tagCount(); index++) {
            if (entries.getCompoundTagAt(index)
                .getInteger("Slot") >= RecipeTransferPayload.SLOT_COUNT) return true;
        }
        return false;
    }

    private static IAEStack<?>[] readLegacyInventory(NBTTagCompound data, String key, int size) {
        IAEStack<?>[] result = new IAEStack<?>[size];
        if (!data.hasKey(key, 10)) return result;
        NBTTagCompound inventory = data.getCompoundTag(key);
        for (int slot = 0; slot < size; slot++) {
            NBTTagCompound itemTag = inventory.getCompoundTag("#" + slot);
            if (itemTag.hasNoTags()) continue;
            ItemStack item = Platform.loadItemStackFromNBT(itemTag);
            if (item == null) continue;
            if (FluidDropCompat.isFluidDrop(item)) {
                net.minecraftforge.fluids.FluidStack fluid = FluidDropCompat.getFluidStack(item);
                result[slot] = fluid == null ? null : AEFluidStack.create(fluid);
            } else {
                result[slot] = AEItemStack.create(item);
            }
        }
        return result;
    }

    private static NBTTagCompound writeStackInventory(IAEStack<?>[] stacks) {
        NBTTagCompound result = new NBTTagCompound();
        for (int slot = 0; slot < stacks.length; slot++) {
            if (stacks[slot] != null) result.setTag("#" + slot, stacks[slot].toNBTGeneric());
        }
        return result;
    }

    @Override
    public boolean needsUpdate() {
        boolean result = needsUpdate;
        needsUpdate = false;
        return result;
    }

    @MENetworkEventSubscribe
    public void onNetworkBootingChanged(MENetworkBootingStatusChange event) {
        if (!event.isBooting) {
            needsUpdate = true;
        }
    }

    @Override
    public boolean isCraftingRecipe() {
        NBTTagCompound data = getItemStack().getTagCompound();
        return data == null || !data.hasKey(QUICK_CRAFTING_MODE) || data.getBoolean(QUICK_CRAFTING_MODE);
    }

    @Override
    public void setCraftingRecipe(boolean craftingMode) {
        super.setCraftingRecipe(craftingMode);
        NBTTagCompound data = getItemStack().getTagCompound();
        if (data != null) {
            data.setBoolean(QUICK_CRAFTING_MODE, craftingMode);
        }
    }

    public int getProcessingGridSize() {
        return 4;
    }

    public void setProcessingGridSize(int gridSize) {
        NBTTagCompound data = getItemStack().getTagCompound();
        if (data == null) {
            data = new NBTTagCompound();
            getItemStack().setTagCompound(data);
        }
        data.setInteger(QUICK_PROCESSING_GRID_SIZE, 4);
    }

    public boolean shouldCombine() {
        return Platform.openNbtData(getItemStack())
            .getBoolean(QUICK_COMBINE);
    }

    public void setCombine(boolean combine) {
        Platform.openNbtData(getItemStack())
            .setBoolean(QUICK_COMBINE, combine);
    }

    public boolean shouldPrioritizeFluids() {
        return Platform.openNbtData(getItemStack())
            .getBoolean(QUICK_PRIORITIZE_FLUIDS);
    }

    public void setPrioritizeFluids(boolean prioritizeFluids) {
        Platform.openNbtData(getItemStack())
            .setBoolean(QUICK_PRIORITIZE_FLUIDS, prioritizeFluids);
    }

    public int getCraftingPinRows(int fallback) {
        return getPinRows(QUICK_CRAFTING_PIN_ROWS, fallback);
    }

    public int getPlayerPinRows(int fallback) {
        return getPinRows(QUICK_PLAYER_PIN_ROWS, fallback);
    }

    public void setPinRows(int craftingRows, int playerRows) {
        NBTTagCompound data = getItemStack().getTagCompound();
        if (data == null) {
            data = new NBTTagCompound();
            getItemStack().setTagCompound(data);
        }
        data.setInteger(QUICK_CRAFTING_PIN_ROWS, Math.max(0, craftingRows));
        data.setInteger(QUICK_PLAYER_PIN_ROWS, Math.max(0, playerRows));
    }

    private int getPinRows(String key, int fallback) {
        NBTTagCompound data = getItemStack().getTagCompound();
        return data == null || !data.hasKey(key) ? Math.max(0, fallback) : Math.max(0, data.getInteger(key));
    }

    public IAEStack<?>[] getProcessingFluidInputs(int size) {
        IAEStack<?>[] result = new IAEStack<?>[size];
        NBTTagCompound data = getItemStack().getTagCompound();
        if (data == null) return result;

        NBTTagList entries = data.getTagList(QUICK_PROCESSING_FLUID_INPUTS, 10);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            int slot = entry.getInteger("Slot");
            if (slot >= 0 && slot < result.length && entry.hasKey("Stack")) {
                result[slot] = IAEStack.fromNBTGeneric(entry.getCompoundTag("Stack"));
            }
        }
        return result;
    }

    public void setProcessingFluidInputs(IAEStack<?>[] inputs) {
        NBTTagCompound data = getItemStack().getTagCompound();
        if (data == null) {
            data = new NBTTagCompound();
            getItemStack().setTagCompound(data);
        }
        NBTTagList entries = new NBTTagList();
        for (int slot = 0; slot < inputs.length; slot++) {
            IAEStack<?> stack = inputs[slot];
            if (stack == null) continue;
            NBTTagCompound entry = new NBTTagCompound();
            NBTTagCompound stackTag = new NBTTagCompound();
            stack.writeToNBTGeneric(stackTag);
            entry.setInteger("Slot", slot);
            entry.setTag("Stack", stackTag);
            entries.appendTag(entry);
        }
        data.setTag(QUICK_PROCESSING_FLUID_INPUTS, entries);
    }

    public boolean hasPatternSnapshot(boolean crafting) {
        NBTTagCompound data = getItemStack().getTagCompound();
        return data != null && data.hasKey(snapshotKey(crafting), 10);
    }

    public IAEStack<?>[] getPatternSnapshotInputs(boolean crafting, int size) {
        return readSnapshotInventory(crafting, SNAPSHOT_INPUTS, size);
    }

    public IAEStack<?>[] getPatternSnapshotOutputs(boolean crafting, int size) {
        return readSnapshotInventory(crafting, SNAPSHOT_OUTPUTS, size);
    }

    public void setPatternSnapshot(boolean crafting, IAEStack<?>[] inputs, IAEStack<?>[] outputs) {
        NBTTagCompound data = getItemStack().getTagCompound();
        if (data == null) {
            data = new NBTTagCompound();
            getItemStack().setTagCompound(data);
        }
        NBTTagCompound snapshot = new NBTTagCompound();
        snapshot.setTag(SNAPSHOT_INPUTS, writeStackList(inputs));
        snapshot.setTag(SNAPSHOT_OUTPUTS, writeStackList(outputs));
        data.setTag(snapshotKey(crafting), snapshot);
    }

    public boolean arePatternSnapshotsLinked() {
        NBTTagCompound data = getItemStack().getTagCompound();
        return data != null && data.getBoolean(QUICK_PATTERN_SNAPSHOTS_LINKED);
    }

    public void setPatternSnapshotsLinked(boolean linked) {
        NBTTagCompound data = getItemStack().getTagCompound();
        if (data == null) {
            data = new NBTTagCompound();
            getItemStack().setTagCompound(data);
        }
        data.setBoolean(QUICK_PATTERN_SNAPSHOTS_LINKED, linked);
    }

    private IAEStack<?>[] readSnapshotInventory(boolean crafting, String inventoryName, int size) {
        IAEStack<?>[] result = new IAEStack<?>[size];
        NBTTagCompound data = getItemStack().getTagCompound();
        if (data == null || !data.hasKey(snapshotKey(crafting), 10)) return result;

        NBTTagCompound snapshot = data.getCompoundTag(snapshotKey(crafting));
        NBTTagList entries = snapshot.getTagList(inventoryName, 10);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            int slot = entry.getInteger("Slot");
            if (slot >= 0 && slot < result.length && entry.hasKey("Stack", 10)) {
                result[slot] = IAEStack.fromNBTGeneric(entry.getCompoundTag("Stack"));
            }
        }
        return result;
    }

    private static NBTTagList writeStackList(IAEStack<?>[] stacks) {
        NBTTagList entries = new NBTTagList();
        for (int slot = 0; slot < stacks.length; slot++) {
            IAEStack<?> stack = stacks[slot];
            if (stack == null) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("Slot", slot);
            entry.setTag("Stack", stack.toNBTGeneric());
            entries.appendTag(entry);
        }
        return entries;
    }

    private static String snapshotKey(boolean crafting) {
        return crafting ? QUICK_CRAFTING_SNAPSHOT : QUICK_PROCESSING_SNAPSHOT;
    }

    @Override
    public void writeCustomButtonData() {}

    @Override
    public void readCustomButtonData() {}
}
