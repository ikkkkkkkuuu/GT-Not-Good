package com.xyp.gtnotgood.ae2thing.quickterminal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.ae2thing.api.Constants;
import com.xyp.gtnotgood.ae2thing.inventory.InventoryHandler;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.ae2thing.util.BlockPos;
import com.xyp.gtnotgood.ae2thing.util.Util;

import appeng.api.config.Actionable;
import appeng.api.config.PinsRows;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.parts.IInterfaceTerminal;
import appeng.api.parts.IPatternTerminalEx;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IInterfaceViewable;
import appeng.container.ContainerOpenContext;
import appeng.container.PrimaryGui;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotRestrictedInput;
import appeng.container.sync.ActionHandler;
import appeng.container.sync.StreamCodecs;
import appeng.container.sync.SyncRegistrar;
import appeng.container.sync.handlers.BooleanSyncHandler;
import appeng.container.sync.handlers.IntSyncHandler;
import appeng.core.AEConfig;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.InventoryAction;
import appeng.helpers.MonitorableAction;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import it.unimi.dsi.fastutil.objects.ObjectLongPair;

/**
 * One server container that keeps AE2's storage/pattern terminal active while delegating
 * interface-terminal discovery and clicks to AE2's original implementation.
 */
public final class ContainerQuickEncodingTerminal extends ContainerPatternTerm {

    private static final Field TRACKED_BY_ID = findTrackedById();
    private static final Field TRACKED = findField(ContainerInterfaceTerminal.class, "tracked");
    private static final Field CRAFTING_MATRIX = findField(ContainerPatternTerm.class, "craftingMatrix");
    private static final Field INTERNAL_SIZE = findField(AppEngInternalInventory.class, "size");
    private static final Field INTERNAL_ITEMS = findField(AppEngInternalInventory.class, "inv");
    private static final Field AE_STACK_INVENTORY_SIZE = findField(IAEStackInventory.class, "size");
    private static final Method NATIVE_ENCODE = findMethod(ContainerPatternTerm.class, "encode");

    private final ContainerInterfaceTerminal interfaceDelegate;
    private final SlotRestrictedInput blankPatternSlot;
    private final SlotRestrictedInput encodedPatternSlot;

    public final BooleanSyncHandler invertedSync;
    public final BooleanSyncHandler combineSync;
    public final BooleanSyncHandler prioritizeFluidsSync;
    public final IntSyncHandler activePageSync;
    public final IntSyncHandler processingGridSizeSync;
    public final IntSyncHandler craftingPinRowsSync;
    public final IntSyncHandler playerPinRowsSync;
    public final ActionHandler<Boolean> setInvertedAction;
    public final ActionHandler<Boolean> setCombineAction;
    public final ActionHandler<Integer> setPrioritizeFluidsAction;
    public final ActionHandler<Integer> setProcessingGridSizeAction;
    public final ActionHandler<Integer> setPinRowsAction;
    public final ActionHandler<Void> refreshPinsAction;
    public final ActionHandler<RecipeTransferPayload> transferRecipeAction;
    public final ActionHandler<RecipeIngredientReplacement> replaceRecipeIngredientAction;
    public final ActionHandler<InterfacePatternTarget> placeEncodedPatternAction;
    public final ActionHandler<Integer> takeBlankPatternAction;
    public final ActionHandler<Integer> takeEncodedPatternAction;
    public final ActionHandler<InterfacePatternTarget> clickInterfacePatternAction;
    public final ActionHandler<InterfacePatternTarget> shiftClickInterfacePatternAction;
    public final ActionHandler<Integer> storeOneFromInventoryAction;
    public final ActionHandler<StorageFluidRequest> fillOneFluidUnitAction;
    public final ActionHandler<StorageFluidRequest> storeOneFluidUnitAction;
    public final ActionHandler<StorageFluidRequest> craftFluidAction;
    public final ActionHandler<Void> quickEncodeAction;
    public final ActionHandler<Boolean> quickEncodeAndMoveToInventoryAction;

    private final IAEStack<?>[] craftingInputSnapshot = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
    private final IAEStack<?>[] craftingOutputSnapshot = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
    private final IAEStack<?>[] processingInputSnapshot = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
    private final IAEStack<?>[] processingOutputSnapshot = new IAEStack<?>[RecipeTransferPayload.SLOT_COUNT];
    private boolean craftingSnapshotValid;
    private boolean processingSnapshotValid;
    private boolean appliedCraftingMode;
    private boolean appliedCraftingModeInitialized;
    private PinsRows nativeCraftingPinRows = PinsRows.DISABLED;
    private PinsRows nativePlayerPinRows = PinsRows.DISABLED;
    private boolean nativePinRowsCaptured;
    private boolean encodingWithLogicalInventorySize;
    /**
     * The two snapshots are representations of one recipe, never independent
     * recipes. The flag is valid only while the active representation still
     * matches its snapshot. Editing either view invalidates the relationship;
     * the next mode switch then regenerates the target view from the active one.
     */
    private boolean patternSnapshotsLinked;

    public ContainerQuickEncodingTerminal(InventoryPlayer inventoryPlayer, ITerminalHost host) {
        super(inventoryPlayer, host, true);
        ContainerOpenContext context = new ContainerOpenContext(host);
        context.setWorld(inventoryPlayer.player.worldObj);
        context.setX(getDualTerminal().getInventorySlot());
        context.setY(0);
        context.setZ(0);
        context.setSide(ForgeDirection.UNKNOWN);
        setOpenContext(context);
        expandCraftingMatrix();
        blankPatternSlot = findPatternSlot(SlotRestrictedInput.PlacableItemType.BLANK_PATTERN);
        encodedPatternSlot = findPatternSlot(SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN);
        loadPatternSnapshots();
        appliedCraftingMode = isCraftingMode();
        appliedCraftingModeInitialized = true;

        SyncRegistrar sync = syncRegistrar();
        invertedSync = sync.booleanSync("inverted")
            .onServerChange((oldValue, newValue) -> getExtendedPatternTerminal().setInverted(newValue));
        combineSync = sync.booleanSync("combine");
        prioritizeFluidsSync = sync.booleanSync("prioritizeFluids");
        activePageSync = sync.intSync("activePage")
            .onServerChange((oldValue, newValue) -> getExtendedPatternTerminal().setActivePage(newValue));
        processingGridSizeSync = sync.intSync("processingGridSize");
        craftingPinRowsSync = sync.intSync("craftingPinRows");
        playerPinRowsSync = sync.intSync("playerPinRows");
        setInvertedAction = sync.actionC2S("setInverted", StreamCodecs.booleanValue())
            .onServerAction(this::applyInverted);
        setCombineAction = sync.actionC2S("setCombine", StreamCodecs.booleanValue())
            .onServerAction(this::applyCombine);
        setPrioritizeFluidsAction = sync.actionC2S("setPrioritizeFluids", StreamCodecs.intValue())
            .onServerAction(this::applyPrioritizeFluids);
        setProcessingGridSizeAction = sync.actionC2S("setProcessingGridSize", StreamCodecs.intValue())
            .onServerAction(this::applyProcessingGridSize);
        setPinRowsAction = sync.actionC2S("setPinRows", StreamCodecs.intValue())
            .onServerAction(this::applyPinRows);
        refreshPinsAction = sync.actionC2S("refreshPins")
            .onServerAction(() -> updatePins(true));
        transferRecipeAction = sync.actionC2S("transferRecipe", RecipeTransferPayload.CODEC)
            .onServerAction(this::applyRecipeTransfer);
        replaceRecipeIngredientAction = sync.actionC2S("replaceRecipeIngredient", RecipeIngredientReplacement.CODEC)
            .onServerAction(this::applyRecipeIngredient);
        placeEncodedPatternAction = sync.actionC2S("placeEncodedPattern", InterfacePatternTarget.CODEC)
            .onServerAction(this::placeEncodedPattern);
        takeBlankPatternAction = sync.actionC2S("takeBlankPattern", StreamCodecs.intValue())
            .onServerAction(action -> clickPatternSlot(blankPatternSlot, action));
        takeEncodedPatternAction = sync.actionC2S("takeEncodedPattern", StreamCodecs.intValue())
            .onServerAction(action -> clickPatternSlot(encodedPatternSlot, action));
        clickInterfacePatternAction = sync.actionC2S("clickInterfacePattern", InterfacePatternTarget.CODEC)
            .onServerAction(target -> performInterfacePatternAction(target, InventoryAction.PICKUP_OR_SET_DOWN));
        shiftClickInterfacePatternAction = sync.actionC2S("shiftClickInterfacePattern", InterfacePatternTarget.CODEC)
            .onServerAction(target -> performInterfacePatternAction(target, InventoryAction.SHIFT_CLICK));
        storeOneFromInventoryAction = sync.actionC2S("storeOneFromInventory", StreamCodecs.intValue())
            .onServerAction(this::storeOneFromInventory);
        fillOneFluidUnitAction = sync.actionC2S("fillOneFluidUnit", StorageFluidRequest.CODEC)
            .onServerAction(this::fillOneFluidUnit);
        storeOneFluidUnitAction = sync.actionC2S("storeOneFluidUnit", StorageFluidRequest.CODEC)
            .onServerAction(this::storeOneFluidUnit);
        craftFluidAction = sync.actionC2S("craftFluid", StorageFluidRequest.CODEC)
            .onServerAction(this::openFluidCraftAmount);
        quickEncodeAction = sync.actionC2S("quickEncode")
            .onServerAction(this::quickEncode);
        quickEncodeAndMoveToInventoryAction = sync.actionC2S("quickEncodeAndMove", StreamCodecs.booleanValue())
            .onServerAction(this::quickEncodeAndMoveToInventory);
        if (Platform.isServer()) {
            nativeCraftingPinRows = getCraftingPinsRows();
            nativePlayerPinRows = getPlayerPinsRows();
            nativePinRowsCaptured = true;
            getExtendedPatternTerminal().setInverted(false);
            getExtendedPatternTerminal().setActivePage(0);
            invertedSync.set(false);
            combineSync.set(getDualTerminal().shouldCombine());
            prioritizeFluidsSync.set(getDualTerminal().shouldPrioritizeFluids());
            activePageSync.set(0);
            processingGridSizeSync.set(getDualTerminal().getProcessingGridSize());
            int craftingPinRows = getDualTerminal().getCraftingPinRows(getCraftingPinsRows().ordinal());
            int playerPinRows = getDualTerminal().getPlayerPinRows(getPlayerPinsRows().ordinal());
            applyPinRows(packPinRows(craftingPinRows, playerPinRows));
        }

        interfaceDelegate = new ContainerInterfaceTerminal(inventoryPlayer, (IInterfaceTerminal) host);
    }

    @Override
    public void detectAndSendChanges() {
        // ContainerPatternTerm.encode() synchronizes while encodeWithInventorySizes
        // temporarily exposes only the logical recipe slots. Allowing that sync
        // would shrink AE2's tracked snapshot and make its next full diff walk
        // beyond the snapshot array.
        if (encodingWithLogicalInventorySize) return;
        if (Platform.isServer()) {
            IPatternTerminalEx terminal = getExtendedPatternTerminal();
            invertedSync.set(terminal.isInverted());
            combineSync.set(getDualTerminal().shouldCombine());
            prioritizeFluidsSync.set(getDualTerminal().shouldPrioritizeFluids());
            activePageSync.set(terminal.getActivePage());
            processingGridSizeSync.set(getDualTerminal().getProcessingGridSize());
            craftingPinRowsSync.set(getDualTerminal().getCraftingPinRows(getCraftingPinsRows().ordinal()));
            playerPinRowsSync.set(getDualTerminal().getPlayerPinRows(getPlayerPinsRows().ordinal()));
        }
        super.detectAndSendChanges();
        // AE2's interface-terminal container assumes its actionable node can
        // never disappear after construction. A wireless terminal can lose
        // that node while this combined GUI is still open (for example after
        // disconnecting or moving out of range), so do not tick the delegate
        // until the node exists again.
        if (Platform.isServer() && getDualTerminal().getActionableNode() != null) {
            interfaceDelegate.detectAndSendChanges();
        }
    }

    @Override
    public int getPatternInputsWidth() {
        return 4;
    }

    @Override
    public int getPatternInputsHeigh() {
        return 4;
    }

    @Override
    public int getPatternInputPages() {
        return 1;
    }

    @Override
    public int getPatternOutputsWidth() {
        return 4;
    }

    @Override
    public int getPatternOutputsHeigh() {
        return 4;
    }

    @Override
    public int getPatternOutputPages() {
        return 1;
    }

    /**
     * AE2 cannot map this FML-owned container class back through GuiBridge.
     * Preserve the terminal item slot explicitly so crafting amount/confirm
     * sub-screens can return to this exact combined terminal.
     */
    @Override
    public PrimaryGui createPrimaryGui() {
        return new QuickTerminalPrimaryGui(getDualTerminal().getPrimaryGuiIcon(), getDualTerminal().getInventorySlot());
    }

    private IPatternTerminalEx getExtendedPatternTerminal() {
        return (IPatternTerminalEx) getPatternTerminal();
    }

    private DualTerminalGuiObject getDualTerminal() {
        return (DualTerminalGuiObject) getPatternTerminal();
    }

    public void requestInverted(boolean inverted) {
        invertedSync.setLocalValue(inverted);
        setInvertedAction.send(inverted);
    }

    public boolean isCombineEnabled() {
        return combineSync.get();
    }

    public void requestCombine(boolean combine) {
        combineSync.setLocalValue(combine);
        setCombineAction.send(combine);
    }

    public boolean isPrioritizeFluidsEnabled() {
        return prioritizeFluidsSync.get();
    }

    public void requestPrioritizeFluids(boolean prioritizeFluids, boolean sortOnly) {
        if (!sortOnly) prioritizeFluidsSync.setLocalValue(prioritizeFluids);
        setPrioritizeFluidsAction.send(sortOnly ? 2 : prioritizeFluids ? 1 : 0);
    }

    public void requestProcessingGridSize(int gridSize) {
        processingGridSizeSync.setLocalValue(4);
        setProcessingGridSizeAction.send(4);
    }

    public void requestPinRows(int craftingRows, int playerRows) {
        int normalizedCrafting = clampVisualPinRows(craftingRows, AEConfig.instance.maxCraftingPinRows);
        int normalizedPlayer = clampVisualPinRows(playerRows, AEConfig.instance.maxPlayerPinRows);
        craftingPinRowsSync.setLocalValue(normalizedCrafting);
        playerPinRowsSync.setLocalValue(normalizedPlayer);
        setPinRowsAction.send(packPinRows(normalizedCrafting, normalizedPlayer));
    }

    /**
     * Craft confirmation and other sub-screens can receive pin updates while this terminal is not the active client
     * screen. Ask the server for the authoritative pin list again when the combined terminal is restored.
     */
    public void requestPinRefresh() {
        refreshPinsAction.send();
    }

    /** Updates the visible tab immediately; authoritative slot contents arrive from the server action. */
    public void requestRecipeTransfer(RecipeTransferPayload payload) {
        craftingModeSync.setLocalValue(payload.isCrafting());
        if (!payload.isCrafting()) {
            processingGridSizeSync.setLocalValue(4);
            invertedSync.setLocalValue(payload.isInverted());
        }
        activePageSync.setLocalValue(0);
        for (int slot = 0; slot < RecipeTransferPayload.SLOT_COUNT; slot++) {
            updateVirtualSlot(appeng.api.storage.StorageName.CRAFTING_INPUT, slot, payload.getInput(slot));
            updateVirtualSlot(appeng.api.storage.StorageName.CRAFTING_OUTPUT, slot, payload.getOutput(slot));
        }
        transferRecipeAction.send(payload);
    }

    public void requestRecipeIngredientReplacement(IAEStack<?> from, IAEStack<?> to) {
        RecipeIngredientReplacement replacement = new RecipeIngredientReplacement(from, to);
        applyRecipeIngredientToInputs(replacement, isCraftingMode());
        replaceRecipeIngredientAction.send(replacement);
    }

    public void requestPlaceEncodedPattern(InterfacePatternTarget target) {
        if (target != null) placeEncodedPatternAction.send(target);
    }

    public void requestTakeEncodedPattern(boolean shiftToInventory) {
        takeEncodedPatternAction.send(shiftToInventory ? 2 : 0);
    }

    public void requestInterfacePatternClick(InterfacePatternTarget target, boolean shiftClick) {
        if (target == null) return;
        if (shiftClick) {
            shiftClickInterfacePatternAction.send(target);
        } else {
            clickInterfacePatternAction.send(target);
        }
    }

    public void requestStoreOneFromInventory(int slotNumber) {
        storeOneFromInventoryAction.send(slotNumber);
    }

    public void requestFillOneFluidUnit(StorageFluidRequest request) {
        fillOneFluidUnitAction.send(request);
    }

    public void requestStoreOneFluidUnit(StorageFluidRequest request) {
        storeOneFluidUnitAction.send(request);
    }

    public void requestCraftFluid(StorageFluidRequest request) {
        craftFluidAction.send(request);
    }

    private void storeOneFromInventory(int slotNumber) {
        if (slotNumber < 0 || slotNumber >= inventorySlots.size()) return;
        if (!(getSlot(slotNumber) instanceof AppEngSlot playerSlot) || !playerSlot.isPlayerSide()) return;
        ItemStack stored = playerSlot.getStack();
        if (stored == null || stored.stackSize <= 0) return;

        ItemStack single = stored.copy();
        single.stackSize = 1;
        ItemStack leftover = shiftStoreItem(single);
        if (leftover != null && leftover.stackSize > 0) return;

        playerSlot.decrStackSize(1);
        playerSlot.onSlotChanged();
        detectAndSendChanges();
    }

    private void fillOneFluidUnit(StorageFluidRequest request) {
        IAEStack<?> requested = request == null ? null : request.getFluid();
        if (!(requested instanceof IAEFluidStack fluid)) return;
        IMEMonitor<IAEItemStack> itemMonitor = getItemMonitor();
        IMEMonitor<IAEFluidStack> fluidMonitor = getDualTerminal().getFluidInventory();
        if (itemMonitor == null || fluidMonitor == null || getPowerSource() == null) return;

        EmptyFluidContainer empty = findEmptyFluidContainer(itemMonitor, fluidMonitor, fluid);
        if (empty == null) return;

        IAEFluidStack fluidRequest = fluid.copy();
        fluidRequest.setStackSize(empty.capacity);
        IAEFluidStack availableFluid = fluidMonitor.extractItems(fluidRequest, Actionable.SIMULATE, getActionSource());
        if (availableFluid == null || availableFluid.getStackSize() < empty.capacity) return;

        IAEItemStack emptyRequest = empty.stack.copy();
        emptyRequest.setStackSize(1);
        IAEItemStack availableEmpty = itemMonitor.extractItems(emptyRequest, Actionable.SIMULATE, getActionSource());
        if (availableEmpty == null || availableEmpty.getStackSize() < 1) return;

        ObjectLongPair<ItemStack> preview = fluid.getStackType()
            .fillContainer(empty.item.copy(), fluidRequest);
        if (!isCompletelyFilled(preview, empty.capacity) || !canAcceptFilledContainer(preview.left())) return;

        IAEItemStack extractedEmpty = Platform
            .poweredExtraction(getPowerSource(), itemMonitor, emptyRequest, getActionSource());
        if (extractedEmpty == null || extractedEmpty.getStackSize() < 1) return;

        IAEFluidStack extractedFluid = Platform
            .poweredExtraction(getPowerSource(), fluidMonitor, fluidRequest, getActionSource());
        if (extractedFluid == null || extractedFluid.getStackSize() < empty.capacity) {
            restoreItem(itemMonitor, extractedEmpty);
            restoreFluid(fluidMonitor, extractedFluid);
            return;
        }

        ItemStack extractedItem = extractedEmpty.getItemStack();
        extractedItem.stackSize = 1;
        ObjectLongPair<ItemStack> filled = fluid.getStackType()
            .fillContainer(extractedItem, extractedFluid);
        if (!isCompletelyFilled(filled, empty.capacity)) {
            restoreItem(itemMonitor, extractedEmpty);
            restoreFluid(fluidMonitor, extractedFluid);
            return;
        }

        giveFilledContainer(filled.left());
        detectAndSendChanges();
    }

    private void storeOneFluidUnit(StorageFluidRequest request) {
        IAEStack<?> requested = request == null ? null : request.getFluid();
        if (!(requested instanceof IAEFluidStack fluid)) return;
        IMEMonitor<IAEItemStack> itemMonitor = getItemMonitor();
        IMEMonitor<IAEFluidStack> fluidMonitor = getDualTerminal().getFluidInventory();
        if (itemMonitor == null || fluidMonitor == null || getPowerSource() == null) return;

        FilledFluidContainer filled = findFilledFluidContainer(fluid);
        if (filled == null) return;

        IAEItemStack emptyRequest = AEItemStack.create(filled.empty.copy());
        IAEFluidStack fluidRequest = filled.fluid.copy();
        if (emptyRequest == null
            || !canInsertAll(itemMonitor.injectItems(emptyRequest.copy(), Actionable.SIMULATE, getActionSource()))
            || !canInsertAll(
                Platform.poweredInsert(
                    getPowerSource(),
                    fluidMonitor,
                    fluidRequest.copy(),
                    getActionSource(),
                    Actionable.SIMULATE))) {
            return;
        }

        IAEItemStack emptyLeftover = itemMonitor
            .injectItems(emptyRequest.copy(), Actionable.MODULATE, getActionSource());
        if (!canInsertAll(emptyLeftover)) {
            rollbackInserted(itemMonitor, emptyRequest, emptyLeftover);
            return;
        }

        IAEFluidStack fluidLeftover = Platform
            .poweredInsert(getPowerSource(), fluidMonitor, fluidRequest.copy(), getActionSource(), Actionable.MODULATE);
        if (!canInsertAll(fluidLeftover)) {
            rollbackInserted(itemMonitor, emptyRequest, null);
            rollbackInserted(fluidMonitor, fluidRequest, fluidLeftover);
            return;
        }

        ItemStack source = getPlayerInv().mainInventory[filled.inventorySlot];
        if (source == null || !Platform.isSameItemPrecise(source, filled.source)) {
            rollbackInserted(itemMonitor, emptyRequest, null);
            rollbackInserted(fluidMonitor, fluidRequest, null);
            return;
        }
        if (--source.stackSize <= 0) getPlayerInv().mainInventory[filled.inventorySlot] = null;
        getPlayerInv().markDirty();
        detectAndSendChanges();
    }

    private void openFluidCraftAmount(StorageFluidRequest request) {
        IAEStack<?> requested = request == null ? null : request.getFluid();
        if (!(requested instanceof IAEFluidStack fluid)) return;
        IGridNode node = getDualTerminal().getActionableNode();
        if (node == null || node.getGrid() == null
            || !isCraftableFluid(
                node.getGrid()
                    .getCache(ICraftingGrid.class),
                fluid))
            return;

        setTargetStack(fluid);
        PrimaryGui primaryGui = createPrimaryGui();
        ContainerOpenContext context = getOpenContext();
        if (context == null || !(getPlayerInv().player instanceof EntityPlayerMP player)) return;

        Platform
            .openGUI(player, context.getTile(), context.getSide(), GuiBridge.GUI_CRAFTING_AMOUNT, getTargetSlotIndex());
        if (player.openContainer instanceof ContainerCraftAmount amount) {
            amount.setPrimaryGui(primaryGui);
            amount.setItemToCraft(fluid);
            amount.setInitialCraftAmount(1);
            amount.detectAndSendChanges();
        }
    }

    private boolean isCraftableFluid(ICraftingGrid crafting, IAEFluidStack target) {
        if (crafting == null) return false;
        try {
            if (!crafting.getCraftingFor(target, null, 0, getPlayerInv().player.worldObj)
                .isEmpty()) {
                return true;
            }
            // AE2's direct query is item-biased on this baseline. Fluid outputs
            // remain authoritative in the generic multi-pattern output map.
            for (Map.Entry<IAEStack<?>, ? extends List<ICraftingPatternDetails>> entry : crafting
                .getCraftingMultiPatterns()
                .entrySet()) {
                IAEStack<?> output = entry.getKey();
                if (output != null && output.isSameType(target)
                    && !entry.getValue()
                        .isEmpty()) {
                    return true;
                }
            }
        } catch (RuntimeException | LinkageError ignored) {}
        return false;
    }

    private FilledFluidContainer findFilledFluidContainer(IAEFluidStack target) {
        IAEStackType<IAEFluidStack> type = target.getStackType();
        for (int slot = 0; slot < getPlayerInv().mainInventory.length; slot++) {
            ItemStack stored = getPlayerInv().mainInventory[slot];
            if (stored == null || stored.stackSize <= 0 || !type.isContainerItemForType(stored)) continue;
            ItemStack single = stored.copy();
            single.stackSize = 1;
            IAEFluidStack contained = type.getStackFromContainerItem(single);
            if (contained == null || contained.getStackSize() <= 0 || !contained.isSameType(target)) continue;
            ObjectLongPair<ItemStack> drained = type.drainStackFromContainer(single.copy(), contained.copy());
            if (drained.left() == null || drained.rightLong() != contained.getStackSize()) continue;
            IAEFluidStack drainedFluid = contained.copy();
            drainedFluid.setStackSize(drained.rightLong());
            return new FilledFluidContainer(slot, single, drained.left(), drainedFluid);
        }
        return null;
    }

    private static boolean canInsertAll(IAEStack<?> leftover) {
        return leftover == null || leftover.getStackSize() <= 0;
    }

    private <T extends IAEStack<T>> void rollbackInserted(IMEMonitor<T> monitor, T requested, T leftover) {
        long leftoverAmount = leftover == null ? 0 : leftover.getStackSize();
        long insertedAmount = requested.getStackSize() - leftoverAmount;
        if (insertedAmount <= 0) return;
        T rollback = requested.copy();
        rollback.setStackSize(insertedAmount);
        monitor.extractItems(rollback, Actionable.MODULATE, getActionSource());
    }

    private EmptyFluidContainer findEmptyFluidContainer(IMEMonitor<IAEItemStack> itemMonitor,
        IMEMonitor<IAEFluidStack> fluidMonitor, IAEFluidStack fluid) {
        IAEStackType<IAEFluidStack> type = fluid.getStackType();
        for (IAEItemStack stored : itemMonitor.getStorageList()) {
            if (stored == null || stored.getStackSize() <= 0) continue;
            ItemStack item = stored.getItemStack();
            if (item == null) continue;
            item.stackSize = 1;
            if (!type.isContainerItemForType(item) || type.getStackFromContainerItem(item) != null) continue;
            long capacity = type.getContainerItemCapacity(item, fluid);
            if (capacity <= 0) continue;
            IAEFluidStack full = fluid.copy();
            full.setStackSize(capacity);
            ObjectLongPair<ItemStack> filled = type.fillContainer(item.copy(), full);
            if (!isCompletelyFilled(filled, capacity)) continue;
            IAEFluidStack available = fluidMonitor.extractItems(full, Actionable.SIMULATE, getActionSource());
            if (available != null && available.getStackSize() >= capacity) {
                return new EmptyFluidContainer(stored.copy(), item, capacity);
            }
        }
        return null;
    }

    private static boolean isCompletelyFilled(ObjectLongPair<ItemStack> result, long capacity) {
        return result != null && result.left() != null && result.rightLong() == capacity;
    }

    private boolean canAcceptFilledContainer(ItemStack filled) {
        if (filled == null) return false;
        InventoryAdaptor inventory = InventoryAdaptor.getAdaptor(getPlayerInv().player, ForgeDirection.UNKNOWN);
        return inventory.simulateAdd(filled.copy()) == null;
    }

    private void giveFilledContainer(ItemStack filled) {
        InventoryAdaptor.getAdaptor(getPlayerInv().player, ForgeDirection.UNKNOWN)
            .addItems(filled);
    }

    private void restoreItem(IMEMonitor<IAEItemStack> monitor, IAEItemStack stack) {
        if (stack == null || stack.getStackSize() <= 0) return;
        IAEItemStack leftover = monitor.injectItems(stack, Actionable.MODULATE, getActionSource());
        if (leftover != null) Platform.handleLeftover(getPlayerInv().player, leftover);
    }

    private void restoreFluid(IMEMonitor<IAEFluidStack> monitor, IAEFluidStack stack) {
        if (stack == null || stack.getStackSize() <= 0) return;
        IAEFluidStack leftover = monitor.injectItems(stack, Actionable.MODULATE, getActionSource());
        if (leftover != null) Platform.handleLeftover(getPlayerInv().player, leftover);
    }

    private static final class EmptyFluidContainer {

        private final IAEItemStack stack;
        private final ItemStack item;
        private final long capacity;

        private EmptyFluidContainer(IAEItemStack stack, ItemStack item, long capacity) {
            this.stack = stack;
            this.item = item;
            this.capacity = capacity;
        }
    }

    private static final class FilledFluidContainer {

        private final int inventorySlot;
        private final ItemStack source;
        private final ItemStack empty;
        private final IAEFluidStack fluid;

        private FilledFluidContainer(int inventorySlot, ItemStack source, ItemStack empty, IAEFluidStack fluid) {
            this.inventorySlot = inventorySlot;
            this.source = source;
            this.empty = empty;
            this.fluid = fluid;
        }
    }

    private void applyInverted(boolean inverted) {
        getExtendedPatternTerminal().setInverted(inverted);
        invertedSync.set(inverted);
    }

    private void applyCombine(boolean combine) {
        getDualTerminal().setCombine(combine);
        combineSync.set(combine);
    }

    private void applyPrioritizeFluids(int action) {
        if (action != 2) {
            boolean prioritizeFluids = action != 0;
            getDualTerminal().setPrioritizeFluids(prioritizeFluids);
            prioritizeFluidsSync.set(prioritizeFluids);
            return;
        }

        IAEStackInventory inputInventory = inputsSync.get();
        List<IAEStack<?>> fluids = new ArrayList<>();
        List<IAEStack<?>> items = new ArrayList<>();
        for (int slot = 0; slot < inputInventory.getSizeInventory(); slot++) {
            IAEStack<?> stack = inputInventory.getAEStackInSlot(slot);
            if (stack == null) continue;
            (stack instanceof IAEFluidStack ? fluids : items).add(stack.copy());
        }

        List<IAEStack<?>> ordered = new ArrayList<>(fluids.size() + items.size());
        if (getDualTerminal().shouldPrioritizeFluids()) {
            ordered.addAll(fluids);
            ordered.addAll(items);
        } else {
            ordered.addAll(items);
            ordered.addAll(fluids);
        }
        restoreInventory(inputInventory, ordered.toArray(new IAEStack<?>[0]));
        inputInventory.markDirty();
        rememberActivePattern(false);
        saveChanges();
    }

    private void applyProcessingGridSize(int gridSize) {
        int previous = getDualTerminal().getProcessingGridSize();
        getDualTerminal().setProcessingGridSize(4);
        processingGridSizeSync.set(4);
        boolean currentlyCrafting = appliedCraftingModeInitialized ? appliedCraftingMode : isCraftingMode();
        if (Platform.isServer() && !currentlyCrafting && previous != 4) {
            // A 3x3 processing view may intentionally retain the holes of a
            // shaped crafting recipe. The 4x4 view is an unordered processing
            // list, so compact it when that view becomes active. Updating the
            // linked processing snapshot keeps the exact crafting layout in
            // its own representation for a later switch back.
            compactInventory(inputsSync.get());
            compactInventory(outputsSync.get());
            rememberActivePattern(false);
        }
    }

    private void applyPinRows(int packedRows) {
        int craftingRows = clampVisualPinRows(packedRows & 0xFFFF, AEConfig.instance.maxCraftingPinRows);
        int playerRows = clampVisualPinRows(packedRows >>> 16, AEConfig.instance.maxPlayerPinRows);
        int craftingGroups = groupsForVisualPinRows(craftingRows);
        int playerGroups = groupsForVisualPinRows(playerRows);

        getDualTerminal().setPinRows(craftingRows, playerRows);
        craftingPinRowsSync.set(craftingRows);
        playerPinRowsSync.set(playerRows);
        setPinsRows(PinsRows.fromOrdinal(craftingGroups), PinsRows.fromOrdinal(playerGroups));
    }

    private static int clampVisualPinRows(int rows, int maxNativeGroups) {
        return Math.max(0, Math.min(rows, maxNativeGroups * 9 / 4));
    }

    private static int groupsForVisualPinRows(int rows) {
        return (rows * 4 + 8) / 9;
    }

    private static int packPinRows(int craftingRows, int playerRows) {
        return craftingRows & 0xFFFF | (playerRows & 0xFFFF) << 16;
    }

    @Override
    public void setCraftingMode(boolean craftingMode) {
        // BooleanSyncHandler installs the received value before invoking its
        // server callback. isCraftingMode() therefore already reports the new
        // value here and cannot be used to discover the mode being left.
        boolean wasCrafting = appliedCraftingModeInitialized ? appliedCraftingMode : isCraftingMode();
        if (craftingInputSnapshot == null || wasCrafting == craftingMode || !Platform.isServer()) {
            super.setCraftingMode(craftingMode);
            appliedCraftingMode = craftingMode;
            appliedCraftingModeInitialized = true;
            return;
        }

        IAEStack<?> craftingResult = wasCrafting ? getCraftingResult() : null;
        boolean canRestoreLinkedView = patternSnapshotsLinked && activePatternMatches(wasCrafting);
        rememberActivePattern(wasCrafting);
        super.setCraftingMode(craftingMode);
        if (craftingMode) {
            if (!canRestoreLinkedView || !craftingSnapshotValid) seedCraftingFromProcessing();
            restorePattern(craftingInputSnapshot, craftingOutputSnapshot);
            // AE2 copied the old processing inventory into its crafting matrix
            // before the snapshot was restored. A second native update rebuilds
            // the matrix and its temporary crafting-result slot from the
            // restored 3x3 recipe. The snapshot itself remains untouched, so
            // processing quantities can still be restored exactly later.
            super.setCraftingMode(true);
        } else {
            if (!canRestoreLinkedView || !processingSnapshotValid) {
                seedProcessingFromCrafting(craftingResult);
            }
            restorePattern(processingInputSnapshot, processingOutputSnapshot);
        }
        patternSnapshotsLinked = true;
        persistPatternSnapshots();
        appliedCraftingMode = craftingMode;
        appliedCraftingModeInitialized = true;
    }

    private void loadPatternSnapshots() {
        DualTerminalGuiObject terminal = getDualTerminal();
        craftingSnapshotValid = terminal.hasPatternSnapshot(true);
        processingSnapshotValid = terminal.hasPatternSnapshot(false);
        patternSnapshotsLinked = terminal.arePatternSnapshotsLinked();
        copyArray(terminal.getPatternSnapshotInputs(true, craftingInputSnapshot.length), craftingInputSnapshot);
        copyArray(terminal.getPatternSnapshotOutputs(true, craftingOutputSnapshot.length), craftingOutputSnapshot);
        copyArray(terminal.getPatternSnapshotInputs(false, processingInputSnapshot.length), processingInputSnapshot);
        copyArray(terminal.getPatternSnapshotOutputs(false, processingOutputSnapshot.length), processingOutputSnapshot);

        if (isCraftingMode()) {
            patternSnapshotsLinked &= activePatternMatches(true);
            copyInventory(inputsSync.get(), craftingInputSnapshot);
            // Crafting uses AE2's separate computed cOut slot. Anything left in
            // the shared processing-output inventory is stale and must not
            // become part of the crafting snapshot.
            clearArray(craftingOutputSnapshot);
            craftingSnapshotValid = true;
            migrateLegacyProcessingFluids();
        } else {
            patternSnapshotsLinked &= activePatternMatches(false);
            copyInventory(inputsSync.get(), processingInputSnapshot);
            copyInventory(outputsSync.get(), processingOutputSnapshot);
            processingSnapshotValid = true;
        }
        terminal.setPatternSnapshotsLinked(patternSnapshotsLinked);
    }

    private void migrateLegacyProcessingFluids() {
        if (processingSnapshotValid) return;
        IAEStack<?>[] legacyFluids = getDualTerminal().getProcessingFluidInputs(processingInputSnapshot.length);
        if (!containsStack(legacyFluids)) return;

        copyInventory(inputsSync.get(), processingInputSnapshot);
        copyInventory(outputsSync.get(), processingOutputSnapshot);
        for (int slot = 0; slot < legacyFluids.length; slot++) {
            if (legacyFluids[slot] != null) processingInputSnapshot[slot] = legacyFluids[slot].copy();
        }
        processingSnapshotValid = true;
        persistPatternSnapshot(false);
    }

    private void rememberActivePattern(boolean crafting) {
        IAEStack<?>[] inputs = crafting ? craftingInputSnapshot : processingInputSnapshot;
        IAEStack<?>[] outputs = crafting ? craftingOutputSnapshot : processingOutputSnapshot;
        copyInventory(inputsSync.get(), inputs);
        if (crafting) {
            clearArray(outputs);
        } else {
            copyInventory(outputsSync.get(), outputs);
        }
        if (crafting) {
            craftingSnapshotValid = true;
        } else {
            processingSnapshotValid = true;
        }
        persistPatternSnapshot(crafting);
    }

    private void seedCraftingFromProcessing() {
        clearArray(craftingInputSnapshot);
        clearArray(craftingOutputSnapshot);
        int target = 0;
        for (IAEStack<?> stack : processingInputSnapshot) {
            if (!(stack instanceof IAEItemStack) || target >= 9) continue;
            craftingInputSnapshot[target++] = stack.copy();
        }
        craftingSnapshotValid = true;
        persistPatternSnapshot(true);
    }

    private void seedProcessingFromCrafting(IAEStack<?> craftingResult) {
        clearArray(processingInputSnapshot);
        clearArray(processingOutputSnapshot);
        boolean compact = getDualTerminal().getProcessingGridSize() == 4;
        int packedSlot = 0;
        for (int sourceSlot = 0; sourceSlot < 9; sourceSlot++) {
            IAEStack<?> stack = craftingInputSnapshot[sourceSlot];
            if (stack == null) continue;
            int targetSlot = compact ? packedSlot++ : sourceSlot;
            processingInputSnapshot[targetSlot] = stack.copy();
        }
        if (craftingResult != null) processingOutputSnapshot[0] = craftingResult.copy();
        processingSnapshotValid = true;
        persistPatternSnapshot(false);
    }

    private IAEStack<?> getCraftingResult() {
        for (Object value : inventorySlots) {
            if (!(value instanceof appeng.container.slot.SlotPatternTerm slot)) continue;
            ItemStack result = slot.getStack();
            return result == null ? null : AEItemStack.create(result.copy());
        }
        return null;
    }

    private void restorePattern(IAEStack<?>[] inputSnapshot, IAEStack<?>[] outputSnapshot) {
        restoreInventory(inputsSync.get(), inputSnapshot);
        restoreInventory(outputsSync.get(), outputSnapshot);
        inputsSync.markDirty();
        outputsSync.markDirty();
    }

    private void persistPatternSnapshot(boolean crafting) {
        getDualTerminal().setPatternSnapshot(
            crafting,
            crafting ? craftingInputSnapshot : processingInputSnapshot,
            crafting ? craftingOutputSnapshot : processingOutputSnapshot);
    }

    private void persistPatternSnapshots() {
        persistPatternSnapshot(true);
        persistPatternSnapshot(false);
        getDualTerminal().setPatternSnapshotsLinked(patternSnapshotsLinked);
    }

    private boolean activePatternMatches(boolean crafting) {
        if (crafting) {
            return craftingSnapshotValid && craftingInventoryMatches(inputsSync.get(), craftingInputSnapshot);
        }
        return processingSnapshotValid && inventoryMatches(inputsSync.get(), processingInputSnapshot)
            && inventoryMatches(outputsSync.get(), processingOutputSnapshot);
    }

    private static boolean craftingInventoryMatches(IAEStackInventory inventory, IAEStack<?>[] snapshot) {
        int size = Math.max(inventory.getSizeInventory(), snapshot.length);
        for (int slot = 0; slot < size; slot++) {
            IAEStack<?> active = slot < inventory.getSizeInventory() ? inventory.getAEStackInSlot(slot) : null;
            IAEStack<?> saved = slot < snapshot.length ? snapshot[slot] : null;
            if (active == null || saved == null) {
                if (active != saved) return false;
            } else if (!(active instanceof IAEItemStack activeItem) || !(saved instanceof IAEItemStack savedItem)
                || !activeItem.isSameType(savedItem)) {
                    return false;
                }
        }
        return true;
    }

    private static boolean inventoryMatches(IAEStackInventory inventory, IAEStack<?>[] snapshot) {
        int size = Math.max(inventory.getSizeInventory(), snapshot.length);
        for (int slot = 0; slot < size; slot++) {
            IAEStack<?> active = slot < inventory.getSizeInventory() ? inventory.getAEStackInSlot(slot) : null;
            IAEStack<?> saved = slot < snapshot.length ? snapshot[slot] : null;
            if (active == null || saved == null) {
                if (active != saved) return false;
            } else if (!active.toNBTGeneric()
                .equals(saved.toNBTGeneric())) {
                    return false;
                }
        }
        return true;
    }

    private static void copyInventory(IAEStackInventory source, IAEStack<?>[] destination) {
        clearArray(destination);
        int size = Math.min(source.getSizeInventory(), destination.length);
        for (int slot = 0; slot < size; slot++) {
            IAEStack<?> stack = source.getAEStackInSlot(slot);
            destination[slot] = stack == null ? null : stack.copy();
        }
    }

    private static void restoreInventory(IAEStackInventory destination, IAEStack<?>[] source) {
        for (int slot = 0; slot < destination.getSizeInventory(); slot++) {
            IAEStack<?> stack = slot < source.length ? source[slot] : null;
            destination.putAEStackInSlot(slot, stack == null ? null : stack.copy());
        }
    }

    private static void compactInventory(IAEStackInventory inventory) {
        IAEStack<?>[] compacted = new IAEStack<?>[inventory.getSizeInventory()];
        int target = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            IAEStack<?> stack = inventory.getAEStackInSlot(slot);
            if (stack != null) compacted[target++] = stack.copy();
        }
        restoreInventory(inventory, compacted);
        inventory.markDirty();
    }

    private static void copyArray(IAEStack<?>[] source, IAEStack<?>[] destination) {
        clearArray(destination);
        int size = Math.min(source.length, destination.length);
        for (int slot = 0; slot < size; slot++) {
            destination[slot] = source[slot] == null ? null : source[slot].copy();
        }
    }

    private static void clearArray(IAEStack<?>[] stacks) {
        java.util.Arrays.fill(stacks, null);
    }

    private static boolean containsStack(IAEStack<?>[] stacks) {
        for (IAEStack<?> stack : stacks) {
            if (stack != null) return true;
        }
        return false;
    }

    private void applyRecipeTransfer(RecipeTransferPayload payload) {
        if (!payload.isCrafting()) {
            applyProcessingGridSize(payload.getProcessingGridSize());
            applyInverted(payload.isInverted());
        }
        setCraftingMode(payload.isCrafting());
        getExtendedPatternTerminal().setActivePage(0);
        activePageSync.set(0);

        for (int slot = 0; slot < RecipeTransferPayload.SLOT_COUNT; slot++) {
            updateVirtualSlot(appeng.api.storage.StorageName.CRAFTING_INPUT, slot, payload.getInput(slot));
            updateVirtualSlot(appeng.api.storage.StorageName.CRAFTING_OUTPUT, slot, payload.getOutput(slot));
        }
        rememberActivePattern(payload.isCrafting());
        // A NEI transfer establishes a new authoritative recipe. The other
        // representation is regenerated from it on the next mode switch.
        patternSnapshotsLinked = false;
        getDualTerminal().setPatternSnapshotsLinked(false);
        saveChanges();
        if (payload.shouldEncode()) quickEncode();
    }

    private void applyRecipeIngredient(RecipeIngredientReplacement replacement) {
        boolean crafting = appliedCraftingModeInitialized ? appliedCraftingMode : isCraftingMode();
        if (!applyRecipeIngredientToInputs(replacement, crafting)) return;
        rememberActivePattern(crafting);
        patternSnapshotsLinked = false;
        getDualTerminal().setPatternSnapshotsLinked(false);
        saveChanges();
    }

    private boolean applyRecipeIngredientToInputs(RecipeIngredientReplacement replacement, boolean crafting) {
        IAEStack<?> from = replacement.getFrom();
        IAEStack<?> to = replacement.getTo();
        if (from == null || to == null || from.getStackSize() <= 0 || to.getStackSize() <= 0) return false;
        if (crafting && (!(from instanceof IAEItemStack) || !(to instanceof IAEItemStack))) return false;
        if (from.getStackType() != to.getStackType()) return false;

        IAEStackInventory inputInventory = inputsSync.get();
        boolean changed = false;
        for (int slot = 0; slot < inputInventory.getSizeInventory(); slot++) {
            IAEStack<?> current = inputInventory.getAEStackInSlot(slot);
            if (!sameIngredientType(current, from)) continue;
            IAEStack<?> replacementStack = to.copy();
            replacementStack.setStackSize(crafting ? 1 : current.getStackSize());
            updateVirtualSlot(appeng.api.storage.StorageName.CRAFTING_INPUT, slot, replacementStack);
            changed = true;
        }
        return changed;
    }

    private static boolean sameIngredientType(IAEStack<?> first, IAEStack<?> second) {
        return first != null && second != null
            && first.getStackType() == second.getStackType()
            && first.isSameType((Object) second);
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        if (Platform.isServer()) {
            boolean activeMode = appliedCraftingModeInitialized ? appliedCraftingMode : isCraftingMode();
            patternSnapshotsLinked &= activePatternMatches(activeMode);
            rememberActivePattern(activeMode);
            getDualTerminal().setPatternSnapshotsLinked(patternSnapshotsLinked);
            restoreNativePinRows();
        }
        interfaceDelegate.onContainerClosed(player);
        super.onContainerClosed(player);
    }

    /**
     * This four-wide GUI temporarily maps its visual pin rows onto AE2's native
     * nine-wide pin sections. Restore the crafting terminal's independent row
     * settings before another container reads the shared terminal item NBT.
     */
    public void restoreNativePinRows() {
        if (!Platform.isServer() || !nativePinRowsCaptured) return;
        nativePinRowsCaptured = false;
        setPinsRows(nativeCraftingPinRows, nativePlayerPinRows);
    }

    /**
     * Moves the freshly encoded pattern straight into the highlighted interface
     * slot. The target id comes from this delegate's own synchronized entry list,
     * so no world-coordinate lookup or cross-dimensional tile access is needed.
     */
    private void placeEncodedPattern(InterfacePatternTarget target) {
        if (encodedPatternSlot == null || !encodedPatternSlot.getHasStack() || TRACKED_BY_ID == null) return;
        try {
            Object tracker = ((Map<?, ?>) TRACKED_BY_ID.get(interfaceDelegate)).get(target.getEntryId());
            if (tracker == null) return;
            Field patternsField = findField(tracker.getClass(), "patterns");
            if (patternsField == null) return;
            IInventory patterns = (IInventory) patternsField.get(tracker);
            int slot = target.getSlot();
            if (slot < 0 || slot >= patterns.getSizeInventory() || patterns.getStackInSlot(slot) != null) return;

            ItemStack pattern = encodedPatternSlot.getStack();
            if (pattern == null || !patterns.isItemValidForSlot(slot, pattern)) return;
            InventoryPlayer playerInventory = getPlayerInv();
            if (!(playerInventory.player instanceof EntityPlayerMP player) || playerInventory.getItemStack() != null)
                return;

            // ContainerInterfaceTerminal.doAction is the authoritative path:
            // besides changing the real interface inventory it also updates
            // InvTracker's NBT cache and queues the exact client-side slot delta.
            encodedPatternSlot.putStack(null);
            playerInventory.setItemStack(pattern.copy());
            interfaceDelegate.doAction(player, InventoryAction.PICKUP_OR_SET_DOWN, slot, target.getEntryId());
            if (playerInventory.getItemStack() != null) {
                // The target changed between selection and this action. Restore
                // the encoder output instead of leaving an unexpected cursor item.
                encodedPatternSlot.putStack(playerInventory.getItemStack());
                playerInventory.setItemStack(null);
                updateHeld(player);
                return;
            }
            interfaceDelegate.scheduleUpdate();
            queuePatternOutputs(pattern);
            refreshCraftingAvailability(target);
            detectAndSendChanges();
        } catch (IllegalAccessException ignored) {}
    }

    /**
     * These slots are outside GuiContainer's native rectangle. Send one explicit
     * action, but let AE2/Minecraft perform the real click so item validation,
     * right-click splitting and shift transfer remain identical to the original
     * pattern terminal.
     */
    private void clickPatternSlot(SlotRestrictedInput patternSlot, int action) {
        if (patternSlot == null || !(getPlayerInv().player instanceof EntityPlayerMP player)) return;
        int mouseButton = action & 1;
        int clickMode = (action & 2) == 0 ? 0 : 1;
        super.slotClick(patternSlot.slotNumber, mouseButton, clickMode, player);
        updateHeld(player);
        detectAndSendChanges();
    }

    private void performInterfacePatternAction(InterfacePatternTarget target, InventoryAction action) {
        if (!(getPlayerInv().player instanceof EntityPlayerMP player) || !isInterfaceEntry(target.getEntryId())) return;
        IInventory patterns = trackedPatterns(target.getEntryId());
        ItemStack previous = stackAt(patterns, target.getSlot());
        interfaceDelegate.doAction(player, action, target.getSlot(), target.getEntryId());
        interfaceDelegate.scheduleUpdate();
        refreshCraftingAvailability(target);
        queuePatternOutputs(previous);
        queuePatternOutputs(stackAt(patterns, target.getSlot()));
        detectAndSendChanges();
    }

    private IInventory trackedPatterns(long entryId) {
        if (TRACKED_BY_ID == null) return null;
        try {
            Object tracker = ((Map<?, ?>) TRACKED_BY_ID.get(interfaceDelegate)).get(entryId);
            if (tracker == null) return null;
            Field patternsField = findField(tracker.getClass(), "patterns");
            Object patterns = patternsField == null ? null : patternsField.get(tracker);
            return patterns instanceof IInventory inventory ? inventory : null;
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static ItemStack stackAt(IInventory inventory, int slot) {
        if (inventory == null || slot < 0 || slot >= inventory.getSizeInventory()) return null;
        ItemStack stack = inventory.getStackInSlot(slot);
        return stack == null ? null : stack.copy();
    }

    /**
     * Crafting-cache rebuilds do not necessarily include products which just
     * disappeared. Queue the affected products explicitly so the monitor sends
     * either their new state or a zero-sized removal to the embedded storage UI.
     */
    private void queuePatternOutputs(ItemStack pattern) {
        if (pattern == null || !(pattern.getItem() instanceof ICraftingPatternItem patternItem)) return;
        ICraftingPatternDetails details = patternItem.getPatternForItem(pattern, getPlayerInv().player.worldObj);
        if (details == null) return;
        List<IAEStack<?>> changed = new ArrayList<>();
        for (IAEStack<?> output : details.getAEOutputs()) {
            if (output != null) changed.add(
                output.copy()
                    .reset());
        }
        if (!changed.isEmpty()) postChange(null, changed, getActionSource());
    }

    private void refreshCraftingAvailability(InterfacePatternTarget target) {
        if (TRACKED == null || TRACKED_BY_ID == null || getDualTerminal().getActionableNode() == null) return;
        try {
            Object tracker = ((Map<?, ?>) TRACKED_BY_ID.get(interfaceDelegate)).get(target.getEntryId());
            if (tracker == null) return;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) TRACKED.get(interfaceDelegate)).entrySet()) {
                if (entry.getValue() != tracker || !(entry.getKey() instanceof IInterfaceViewable viewable)) continue;
                if (viewable instanceof IInterfaceHost interfaceHost) {
                    interfaceHost.getInterfaceDuality()
                        .updateCraftingList();
                }
                IGridNode node = interfaceNode(viewable);
                ICraftingProvider provider = viewable instanceof ICraftingProvider craftingProvider ? craftingProvider
                    : null;
                (node == null ? getDualTerminal().getActionableNode()
                    .getGrid() : node.getGrid()).postEvent(new MENetworkCraftingPatternChange(provider, node));
                return;
            }
        } catch (IllegalAccessException ignored) {}
    }

    private static IGridNode interfaceNode(IInterfaceViewable viewable) {
        for (ForgeDirection side : ForgeDirection.values()) {
            IGridNode node = viewable.getGridNode(side);
            if (node != null) return node;
        }
        return null;
    }

    private SlotRestrictedInput findPatternSlot(SlotRestrictedInput.PlacableItemType type) {
        for (Object value : inventorySlots) {
            if (value instanceof SlotRestrictedInput slot && slot.getItemType() == type) {
                return slot;
            }
        }
        return null;
    }

    private void quickEncode() {
        if (isCraftingMode()) {
            encodeWithInventorySizes(9, -1);
            return;
        }
        encodeWithInventorySizes(RecipeTransferPayload.SLOT_COUNT, RecipeTransferPayload.SLOT_COUNT);
    }

    private void quickEncodeAndMoveToInventory(boolean encodeWholeStack) {
        quickEncode();
        ItemStack output = encodedPatternSlot.getStack();
        if (output == null) return;
        if (encodeWholeStack) {
            ItemStack blanks = blankPatternSlot.getStack();
            blankPatternSlot.putStack(null);
            if (blanks != null) output.stackSize += blanks.stackSize;
        }
        if (!getPlayerInv().addItemStackToInventory(output)) {
            getPlayerInv().player.entityDropItem(output, 0);
        }
        encodedPatternSlot.putStack(null);
    }

    /**
     * PatternEncodingHelper serializes every slot reported by the backing AE
     * inventories. A crafting pattern must contain exactly the visible 3x3 input
     * grid, while this terminal's processing layout contains one 4x4 page for
     * inputs and one 4x4 page for outputs. Temporarily expose those logical sizes
     * while encoding without reallocating the backing inventories.
     */
    private void encodeWithInventorySizes(int inputSize, int outputSize) {
        if (AE_STACK_INVENTORY_SIZE == null) {
            throw new IllegalStateException("Unable to access AE2 pattern inventory size");
        }
        IAEStackInventory inputs = inputsSync.get();
        IAEStackInventory outputs = outputsSync.get();
        int oldInputSize = -1;
        int oldOutputSize = -1;
        encodingWithLogicalInventorySize = true;
        try {
            oldInputSize = AE_STACK_INVENTORY_SIZE.getInt(inputs);
            oldOutputSize = AE_STACK_INVENTORY_SIZE.getInt(outputs);
            AE_STACK_INVENTORY_SIZE.setInt(inputs, inputSize);
            if (outputSize >= 0) AE_STACK_INVENTORY_SIZE.setInt(outputs, outputSize);
            invokeNativeEncode();
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to resize AE2 pattern inventory for encoding", exception);
        } finally {
            try {
                if (oldInputSize >= 0) AE_STACK_INVENTORY_SIZE.setInt(inputs, oldInputSize);
                if (oldOutputSize >= 0 && outputSize >= 0) AE_STACK_INVENTORY_SIZE.setInt(outputs, oldOutputSize);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Unable to restore AE2 pattern inventory size", exception);
            } finally {
                encodingWithLogicalInventorySize = false;
            }
        }
        // The native encode-time sync above was deliberately suppressed. Send
        // the encoded output slot and the restored full inventories together.
        if (Platform.isServer()) detectAndSendChanges();
    }

    /**
     * AE2's hybrid container assumes the backing crafting matrix has exactly the
     * same size as its input inventory while its recipe calculation still uses
     * only the first 3x3 cells. Expand that existing object (which is also held by
     * SlotPatternTerm) so switching between 3x3 crafting and 4x4 processing is safe.
     */
    private void expandCraftingMatrix() {
        if (CRAFTING_MATRIX == null || INTERNAL_SIZE == null || INTERNAL_ITEMS == null) {
            throw new IllegalStateException("Unable to access AE2 crafting matrix fields");
        }
        try {
            IInventory matrix = (IInventory) CRAFTING_MATRIX.get(this);
            int size = getPatternInputsWidth() * getPatternInputsHeigh() * getPatternInputPages();
            INTERNAL_SIZE.setInt(matrix, size);
            INTERNAL_ITEMS.set(matrix, new ItemStack[size]);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to expand AE2 crafting matrix", exception);
        }
    }

    @Override
    public void doAction(EntityPlayerMP player, InventoryAction action, int slot, long id) {
        if (isInterfaceEntry(id)) {
            interfaceDelegate.doAction(player, action, slot, id);
        } else {
            super.doAction(player, action, slot, id);
        }
    }

    @Override
    public void doMonitorableAction(MonitorableAction action, int custom, EntityPlayerMP player) {
        if (action == MonitorableAction.FILL_SINGLE_CONTAINER && !prepareSingleContainerFill(player)) return;
        super.doMonitorableAction(action, custom, player);
    }

    /**
     * AE2 normally sends the filled result from a stacked cursor into the player inventory and drops it when the
     * inventory cannot accept it. This terminal keeps the one container being filled on the cursor instead, after
     * safely moving the unused empty containers into the inventory.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean prepareSingleContainerFill(EntityPlayerMP player) {
        ItemStack hand = player.inventory.getItemStack();
        IAEStack<?> target = getTargetStack();
        if (hand == null || hand.stackSize <= 1 || target == null) return true;

        IAEStackType type = target.getStackType();
        if (!type.isContainerItemForType(hand)) return true;
        ItemStack singleContainer = hand.copy();
        singleContainer.stackSize = 1;
        var simulatedFill = type.fillContainer(singleContainer.copy(), target);
        if (simulatedFill.left() == null || simulatedFill.rightLong() <= 0) return true;

        ItemStack unusedContainers = hand.copy();
        unusedContainers.stackSize--;
        if (!canFullyStore(player.inventory, unusedContainers)) {
            // #tr gui.gtnotgood.quick_terminal.container_inventory_full
            // # No inventory space for the remaining empty containers.
            // # zh_CN 物品栏没有空间存放剩余的空容器。
            player
                .addChatMessage(new ChatComponentTranslation("gui.gtnotgood.quick_terminal.container_inventory_full"));
            return false;
        }

        player.inventory.addItemStackToInventory(unusedContainers);
        player.inventory.setItemStack(singleContainer);
        player.inventory.markDirty();
        return true;
    }

    private static boolean canFullyStore(InventoryPlayer inventory, ItemStack incoming) {
        int remaining = incoming.stackSize;
        for (ItemStack existing : inventory.mainInventory) {
            if (existing == null || existing.getItem() != incoming.getItem()
                || existing.getItemDamage() != incoming.getItemDamage()
                || !ItemStack.areItemStackTagsEqual(existing, incoming)) continue;
            int limit = Math.min(existing.getMaxStackSize(), inventory.getInventoryStackLimit());
            remaining -= Math.max(0, limit - existing.stackSize);
            if (remaining <= 0) return true;
        }
        for (ItemStack existing : inventory.mainInventory) {
            if (existing != null) continue;
            remaining -= Math.min(incoming.getMaxStackSize(), inventory.getInventoryStackLimit());
            if (remaining <= 0) return true;
        }
        return false;
    }

    private boolean isInterfaceEntry(long id) {
        if (TRACKED_BY_ID == null) return false;
        try {
            return ((Map<?, ?>) TRACKED_BY_ID.get(interfaceDelegate)).containsKey(id);
        } catch (IllegalAccessException ignored) {
            return false;
        }
    }

    /** Resolves only entries currently tracked by this open terminal, preventing arbitrary client-supplied targets. */
    public Util.DimensionalCoordSide getTrackedInterfaceLocation(long id) {
        if (TRACKED_BY_ID == null || getDualTerminal().getActionableNode() == null) return null;
        try {
            Object tracker = ((Map<?, ?>) TRACKED_BY_ID.get(interfaceDelegate)).get(id);
            if (tracker == null || !readBooleanField(tracker, "online", false)
                || !readBooleanField(tracker, "shouldDisplay", false)) return null;
            ForgeDirection side = readField(tracker, "side") instanceof ForgeDirection direction ? direction
                : ForgeDirection.UNKNOWN;
            int y = readIntField(tracker, "y", -1);
            int dimension = readIntField(tracker, "dim", Integer.MIN_VALUE);
            if (y < 0 || dimension == Integer.MIN_VALUE) return null;
            return new Util.DimensionalCoordSide(
                readIntField(tracker, "x", 0),
                y,
                readIntField(tracker, "z", 0),
                dimension,
                side,
                "");
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    /** Toggles visibility for an interface selected from this container's server-owned tracker. */
    public boolean toggleTrackedInterfaceVisibility(long id) {
        if (TRACKED == null || TRACKED_BY_ID == null || getDualTerminal().getActionableNode() == null) return false;
        try {
            Object tracker = ((Map<?, ?>) TRACKED_BY_ID.get(interfaceDelegate)).get(id);
            if (tracker == null) return false;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) TRACKED.get(interfaceDelegate)).entrySet()) {
                if (entry.getValue() != tracker) continue;
                if (entry.getKey() instanceof IInterfaceHost host) {
                    var config = host.getInterfaceDuality()
                        .getConfigManager();
                    YesNo current = (YesNo) config.getSetting(Settings.INTERFACE_TERMINAL);
                    config.putSetting(Settings.INTERFACE_TERMINAL, current == YesNo.YES ? YesNo.NO : YesNo.YES);
                    host.saveChanges();
                } else if (entry.getKey() instanceof ITerminalVisibilityToggle toggle) {
                    toggle.toggleTerminalVisibility();
                } else {
                    return false;
                }
                interfaceDelegate.scheduleUpdate();
                return true;
            }
        } catch (IllegalAccessException ignored) {}
        return false;
    }

    private static Object readField(Object owner, String name) {
        Field field = findField(owner.getClass(), name);
        if (field == null) return null;
        try {
            return field.get(owner);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static int readIntField(Object owner, String name, int fallback) {
        Field field = findField(owner.getClass(), name);
        if (field == null) return fallback;
        try {
            return field.getInt(owner);
        } catch (IllegalAccessException ignored) {
            return fallback;
        }
    }

    private static boolean readBooleanField(Object owner, String name, boolean fallback) {
        Field field = findField(owner.getClass(), name);
        if (field == null) return fallback;
        try {
            return field.getBoolean(owner);
        } catch (IllegalAccessException ignored) {
            return fallback;
        }
    }

    private static Field findTrackedById() {
        return findField(ContainerInterfaceTerminal.class, "trackedById");
    }

    private static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> owner, String name) {
        try {
            Method method = owner.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private void invokeNativeEncode() {
        if (NATIVE_ENCODE == null) throw new IllegalStateException("Unable to access AE2 pattern encoder");
        try {
            NATIVE_ENCODE.invoke(this);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke AE2 pattern encoder", exception);
        }
    }

    private static final class QuickTerminalPrimaryGui extends PrimaryGui {

        private QuickTerminalPrimaryGui(ItemStack icon, int terminalSlot) {
            super(null, icon, null, ForgeDirection.UNKNOWN);
            setSlotIndex(terminalSlot);
        }

        @Override
        public void open(EntityPlayer player) {
            int terminalSlot = slotIndex;
            if (slotIndex >= Platform.baublesSlotsOffset) {
                terminalSlot = Constants.BAUBLE_SLOT_OFFSET + (slotIndex - Platform.baublesSlotsOffset);
            }
            InventoryHandler.openGui(
                player,
                player.worldObj,
                new BlockPos(terminalSlot, 0, 0),
                ForgeDirection.UNKNOWN,
                GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL);
        }
    }
}
