/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Adapted from GT5-Unofficial; copyright its contributors and KubaTech contributors. */
package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import static forestry.api.apiculture.BeeManager.beeRoot;
import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Alignment.CrossAxis;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DynamicSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.ItemSlotSH;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.EmptyWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.SlotGroup;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialApiary;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialApiary.BeeSimulator;

import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.SearchField;
import forestry.api.apiculture.EnumBeeType;
import gregtech.api.enums.Mods;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.modularui2.widget.SlotLikeButtonWidget;
import kubatech.api.helpers.GTHelper;

public class LargeIndustrialApiaryGui extends GTNGModernMultiBlockBaseGui<LargeIndustrialApiary> {

    private static final int SLOTS_PER_ROW = 9;
    private static final int BEE_ENTRIES_PER_PAGE = 36;
    private static final int WARNING_ANIM_FRAMES = 63;
    private static final int WARNING_ANIM_TICK_MS = 50;
    private static final UITexture OVERLAY_INVENTORY_FULL_WARNING_SHEET = UITexture.fullImage(Mods.ModIDs.KUBA_TECH,
        "gui/icons/inventory_full_warning");
    private static final UITexture OVERLAY_BEE_LIST = UITexture.fullImage(Mods.ModIDs.KUBA_TECH,
        "gui/overlay_button/bee_list");

    private boolean isInInventory = true;
    private List<GTHelper.StackableItemSlot> beeSlots = new ArrayList<>();
    private int maxSlots;
    private int usedSlots;
    private boolean machineRunning;
    private boolean productionRunning;
    private boolean secondaryModeLocked;
    private IntSyncValue beeClickSyncer;
    private DynamicSyncHandler beeInventoryHandler;
    private PanelSyncManager mainSyncManager;
    private int beePage;
    private int beePageCount = 1;
    private IntSyncValue beePageSyncer;
    private List<GTHelper.StackableItemSlot> cachedFullBeeList;
    private long cachedBeeListTick = -1;

    public LargeIndustrialApiaryGui(LargeIndustrialApiary multiblock) {
        super(multiblock);
    }

    @Override
    public ModularPanel build(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        isInInventory = multiblock.getBaseMetaTileEntity().isActive();
        setMachineModeIcons();
        registerSyncValues(syncManager);
        registerApiarySyncValues(syncManager);
        ModularPanel panel = getBasePanel(guiData, syncManager, uiSettings);
        return panel.child(createMainColumn(panel, syncManager));
    }

    protected void registerApiarySyncValues(PanelSyncManager syncManager) {
        this.mainSyncManager = syncManager;

        syncManager.syncValue("apiaryPrimaryMode", new IntSyncValue(() -> multiblock.primaryMode));
        syncManager.syncValue("apiarySecondaryMode", new IntSyncValue(() -> multiblock.secondaryMode));
        syncManager.syncValue("apiaryPrimaryModeClick", new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.side.isClient() || mouse.mouseButton > 1) return;
            int direction = mouse.mouseButton == 1 ? -1 : 1;
            multiblock.setPrimaryMode((multiblock.primaryMode + direction + 3) % 3);
        }));
        syncManager.syncValue("apiarySecondaryModeClick", new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.side.isClient() || mouse.mouseButton > 1) return;
            multiblock.setSecondaryMode((multiblock.secondaryMode + 1) % 2);
        }));

        IntSyncValue maxSlotsSyncer = new IntSyncValue(() -> multiblock.maxSlots, val -> maxSlots = val);
        syncManager.syncValue("apiaryMaxSlots", maxSlotsSyncer);

        IntSyncValue usedSlotsSyncer = new IntSyncValue(() -> multiblock.storage.size(), val -> usedSlots = val);
        syncManager.syncValue("apiaryUsedSlots", usedSlotsSyncer);

        beeClickSyncer = new IntSyncValue(() -> 0, this::handleBeeClick).allowC2S();
        syncManager.syncValue("beeClick", beeClickSyncer);

        beePageSyncer = new IntSyncValue(() -> beePage, val -> beePage = Math.max(0, Math.min(val, beePageCount - 1)))
            .allowC2S();
        syncManager.syncValue("apiaryBeePage", beePageSyncer);

        syncManager.syncValue("apiaryBeePageCount", new IntSyncValue(() -> beePageCount, val -> beePageCount = val));

        GenericListSyncHandler<GTHelper.StackableItemSlot> beeListSyncer = createBeeListSyncer();
        syncManager.syncValue("apiaryBeeList", beeListSyncer);

        beeInventoryHandler = new DynamicSyncHandler().widgetProvider((sm, packet) -> {
            if (packet == null) return new EmptyWidget();
            return createBeeSlotGrid(packet.readInt());
        });

        if (!syncManager.isClient()) {
            beeListSyncer.setChangeListener(() -> notifyBeeInventoryUpdate(beeListSyncer.getValue().size()));
            maxSlotsSyncer.setChangeListener(() -> notifyBeeInventoryUpdate(buildPagedAggregatedBeeList().size()));
            usedSlotsSyncer.setChangeListener(() -> notifyBeeInventoryUpdate(buildPagedAggregatedBeeList().size()));
            beePageSyncer.setChangeListener(() -> notifyBeeInventoryUpdate(buildPagedAggregatedBeeList().size()));
        }

        syncManager.syncValue("apiaryRunning",
            new BooleanSyncValue(() -> multiblock.mMaxProgresstime > 0, val -> machineRunning = val));
        syncManager.syncValue("apiaryProductionRunning",
            new BooleanSyncValue(() -> multiblock.primaryMode == LargeIndustrialApiary.MODE_PRIMARY_OPERATING
                && multiblock.mMaxProgresstime > 0, val -> productionRunning = val));
        syncManager.syncValue("apiarySecondaryModeLocked",
            new BooleanSyncValue(() -> !multiblock.canChangeSecondaryMode(), val -> secondaryModeLocked = val));

        registerQueenBufferSlot(syncManager);
    }

    private GenericListSyncHandler<GTHelper.StackableItemSlot> createBeeListSyncer() {
        return new GenericListSyncHandler<>(this::buildPagedAggregatedBeeList, val -> beeSlots = val,
            GTHelper.StackableItemSlot::read, (buffer, slot) -> slot.write(buffer),
            (a, b) -> a.count == b.count && ItemStack.areItemStacksEqual(a.stack, b.stack), null);
    }

    private void registerQueenBufferSlot(PanelSyncManager syncManager) {
        ItemStackHandler queenBufferInv = new ItemStackHandler(1) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        ModularSlot queenBufferSlot = new ModularSlot(queenBufferInv, 0).filter(this::canAcceptQueen)
            .singletonSlotGroup(SlotGroup.STORAGE_SLOT_PRIO)
            .changeListener((newItem, onlyAmountChanged, client, init) -> {
                if (client || init || newItem == null) return;
                if (!tryAddBeeToStorage(newItem.copy())) {
                    EntityPlayer player = syncManager.getPlayer();
                    ItemStack returned = newItem.copy();
                    if (!player.inventory.addItemStackToInventory(returned)) player.entityDropItem(returned, 0);
                    player.inventoryContainer.detectAndSendChanges();
                }
                queenBufferInv.setStackInSlot(0, null);
            });
        syncManager.syncValue("queenBuffer", new ItemSlotSH(queenBufferSlot));
    }

    private boolean canAcceptQueen(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || beeRoot.getType(stack) != EnumBeeType.QUEEN) return false;
        if (mainSyncManager.isClient()) return !productionRunning && usedSlots < maxSlots;
        if (multiblock.primaryMode == LargeIndustrialApiary.MODE_PRIMARY_OPERATING && multiblock.mMaxProgresstime > 0)
            return false;
        return multiblock.storage.size() < multiblock.maxSlots;
    }

    private boolean tryAddBeeToStorage(ItemStack queenStack) {
        if (mainSyncManager.isClient() || !canAcceptQueen(queenStack)) return false;
        World world = multiblock.getBaseMetaTileEntity().getWorld();
        float voltageTier = (float) multiblock.getVoltageTierExact();
        BeeSimulator bee = new BeeSimulator(queenStack, world, voltageTier);
        if (bee.isValid) {
            multiblock.storage.add(bee);
            multiblock.onStorageContentChanged(false);
            invalidateBeeListCache();
            return true;
        }
        return false;
    }

    private void notifyBeeInventoryUpdate(int pagedListSize) {
        boolean hasEmptySlot = multiblock.storage.size() < multiblock.maxSlots;
        boolean isLastPage = (beePage >= beePageCount - 1);
        int activeCount = pagedListSize + (hasEmptySlot && isLastPage ? 1 : 0);
        beeInventoryHandler.notifyUpdate(buf -> buf.writeInt(activeCount));
    }

    private static ItemStack createDisplayStack(ItemStack original) {
        ItemStack display = new ItemStack(original.getItem(), 1, original.getItemDamage());
        if (original.getTagCompound() == null) return display;

        NBTTagCompound originalTag = original.getTagCompound();
        NBTTagCompound newTag = new NBTTagCompound();

        NBTTagCompound genomeTag = originalTag.getCompoundTag("Genome");
        if (!genomeTag.hasNoTags()) {
            NBTTagList oldChromosomes = genomeTag.getTagList("Chromosomes", 10);
            if (oldChromosomes.tagCount() > 1) {
                NBTTagList newChromosomes = new NBTTagList();
                // Chromosome 0 = species, chromosome 1 = speed
                newChromosomes.appendTag(oldChromosomes.getCompoundTagAt(0).copy());
                newChromosomes.appendTag(oldChromosomes.getCompoundTagAt(1).copy());
                NBTTagCompound newGenome = new NBTTagCompound();
                newGenome.setTag("Chromosomes", newChromosomes);
                newTag.setTag("Genome", newGenome);
            }
        }

        if (originalTag.hasKey("IsAnalyzed")) {
            newTag.setBoolean("IsAnalyzed", originalTag.getBoolean("IsAnalyzed"));
        }

        display.setTagCompound(newTag);
        return display;
    }

    private List<GTHelper.StackableItemSlot> buildAggregatedBeeList() {
        HashMap<String, Integer> countMap = new HashMap<>();
        HashMap<String, ItemStack> stackMap = new HashMap<>();
        HashMap<String, Integer> firstSlotMap = new HashMap<>();
        for (int i = 0; i < multiblock.storage.size(); i++) {
            BeeSimulator bee = multiblock.storage.get(i);
            String key = bee.speciesKey;
            countMap.merge(key, 1, Integer::sum);
            stackMap.computeIfAbsent(key, unused -> createDisplayStack(bee.queenStack));
            firstSlotMap.putIfAbsent(key, i);
        }
        List<GTHelper.StackableItemSlot> result = new ArrayList<>(countMap.size());
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            ArrayList<Integer> slots = new ArrayList<>(1);
            slots.add(firstSlotMap.get(entry.getKey()));
            result.add(new GTHelper.StackableItemSlot(entry.getValue(), stackMap.get(entry.getKey()), slots));
        }
        return result;
    }

    private List<GTHelper.StackableItemSlot> getFullAggregatedBeeList() {
        if (mainSyncManager == null || mainSyncManager.isClient()) {
            return cachedFullBeeList != null ? cachedFullBeeList : buildAggregatedBeeList();
        }
        World world = multiblock.getBaseMetaTileEntity().getWorld();
        long currentTick = world != null ? world.getTotalWorldTime() : -1;
        if (cachedFullBeeList == null || cachedBeeListTick != currentTick) {
            cachedFullBeeList = buildAggregatedBeeList();
            cachedBeeListTick = currentTick;
        }
        return cachedFullBeeList;
    }

    private List<GTHelper.StackableItemSlot> buildPagedAggregatedBeeList() {
        List<GTHelper.StackableItemSlot> full = getFullAggregatedBeeList();
        int totalPages = Math.max(1, (full.size() + BEE_ENTRIES_PER_PAGE - 1) / BEE_ENTRIES_PER_PAGE);
        beePageCount = totalPages;
        if (beePage >= totalPages) beePage = totalPages - 1;
        if (beePage < 0) beePage = 0;
        int start = beePage * BEE_ENTRIES_PER_PAGE;
        int end = Math.min(start + BEE_ENTRIES_PER_PAGE, full.size());
        if (start >= full.size()) return new ArrayList<>();
        return new ArrayList<>(full.subList(start, end));
    }

    private void invalidateBeeListCache() {
        cachedFullBeeList = null;
        cachedBeeListTick = -1;
    }

    @Override
    protected ParentWidget<?> createTerminalParentWidget(ModularPanel panel, PanelSyncManager syncManager) {
        return new ParentWidget<>().size(getTerminalWidgetWidth(), getTerminalWidgetHeight()).paddingTop(4)
            .paddingBottom(4).paddingLeft(4).paddingRight(0).background(GTNGGuiTextures.MODERN_VAULT_PANEL_BORDER)
            .child(createTerminalTextWidget(syncManager, panel)
                .size(getTerminalWidgetWidth() - 4, getTerminalWidgetHeight() - 8).collapseDisabledChild()
                .setEnabledIf(w -> !isInInventory))
            .child(Flow.column().size(getTerminalWidgetWidth() - 4, getTerminalWidgetHeight() - 8)
                .setEnabledIf(w -> isInInventory).child(createBeeInventoryWidget()).child(createPageNavigationRow()))
            .childIf(multiblock.supportsTerminalRightCornerColumn(),
                () -> createTerminalRightCornerColumn(panel, syncManager));
    }

    @Override
    protected Flow createTerminalRightCornerColumn(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.column().coverChildren().rightRel(0, 6, 0).bottomRel(0, 6, 0).child(createInventoryFullWarning())
            .childIf(multiblock.supportsShutdownReasonHoverable(),
                () -> createShutdownReasonHoverableTerminal(syncManager))
            .childIf(multiblock.supportsMaintenanceIssueHoverable(),
                () -> createMaintIssueHoverableTerminal(syncManager))
            .childIf(multiblock.supportsLogo(), () -> makeLogoWidget(syncManager, panel));
    }

    protected IWidget createInventoryFullWarning() {
        return new DynamicDrawable(() -> {
            if (maxSlots <= 0 || usedSlots < maxSlots) return IDrawable.EMPTY;
            int frame = (int) ((System.currentTimeMillis() / WARNING_ANIM_TICK_MS) % WARNING_ANIM_FRAMES);
            float v0 = (float) frame / WARNING_ANIM_FRAMES;
            float v1 = (float) (frame + 1) / WARNING_ANIM_FRAMES;
            return OVERLAY_INVENTORY_FULL_WARNING_SHEET.getSubArea(0f, v0, 1f, v1);
        }).asWidget().size(18, 18).marginBottom(4)
            .tooltipBuilder(t -> t.addLine(IKey.lang("kubatech.gui.text.mia.inventory_full_warning")))
            .tooltipAutoUpdate(true).setEnabledIf(w -> maxSlots > 0 && usedSlots >= maxSlots);
    }

    @Override
    protected IWidget createMaintIssueHoverableTerminal(PanelSyncManager syncManager) {
        return ((Widget<?>) super.createMaintIssueHoverableTerminal(syncManager)).marginTop(1);
    }

    private IWidget createBeeInventoryWidget() {
        return new DynamicSyncedWidget<>().widthRel(1f).expanded().syncHandler(beeInventoryHandler);
    }

    private IWidget createPageNavigationRow() {
        return Flow.row().widthRel(1f).height(14).mainAxisAlignment(Alignment.MainAxis.CENTER)
            .crossAxisAlignment(CrossAxis.CENTER).setEnabledIf(w -> beePageCount > 1)
            .child(new ButtonWidget<>().size(14, 14).background(GTNGGuiTextures.MODERN_BUTTON)
                .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER).overlay(IKey.str("<")).onMousePressed(btn -> {
                    if (beePage > 0) {
                        beePage--;
                        beePageSyncer.setIntValue(beePage, true, true);
                    }
                    return true;
                }))
            .child(new TextWidget<>(IKey.dynamic(() -> (beePage + 1) + " / " + beePageCount))
                .alignment(Alignment.Center).width(50).height(14))
            .child(new ButtonWidget<>().size(14, 14).background(GTNGGuiTextures.MODERN_BUTTON)
                .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER).overlay(IKey.str(">")).onMousePressed(btn -> {
                    if (beePage < beePageCount - 1) {
                        beePage++;
                        beePageSyncer.setIntValue(beePage, true, true);
                    }
                    return true;
                }));
    }

    private IWidget createBeeSlotGrid(int activeCount) {
        if (activeCount <= 0) return new EmptyWidget();

        ListWidget<IWidget, ?> listWidget = new ListWidget<>().crossAxisAlignment(CrossAxis.START).widthRel(1f)
            .heightRel(1f);

        for (int rowStart = 0; rowStart < activeCount; rowStart += SLOTS_PER_ROW) {
            Flow row = Flow.row().coverChildren();
            int rowEnd = Math.min(rowStart + SLOTS_PER_ROW, activeCount);
            for (int j = rowStart; j < rowEnd; j++) {
                row.child(createBeeSlot(j));
            }
            listWidget.child(row);
        }

        return listWidget;
    }

    private IWidget createBeeSlot(int idx) {
        SlotLikeButtonWidget slb = new SlotLikeButtonWidget(() -> getBeeStack(idx)) {

            @Override
            public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetThemeEntry) {
                super.draw(context, widgetThemeEntry);
                int count = getBeeSlotCount(idx);
                boolean isEmptySlot = idx >= beeSlots.size();
                if (count > 1 || isEmptySlot) {
                    String format = isNEIFilteredOut(getBeeStack(idx)) ? EnumChatFormatting.GRAY.toString() : null;
                    GuiDraw.drawStandardSlotAmountText(count, format, getArea());
                }
                if (productionRunning) {
                    GuiDraw.drawRect(0, 0, 18, 18, 0x80000000);
                }
            }
        };
        slb.size(18, 18).background(GTNGGuiTextures.MODERN_VAULT_ITEM_SLOT);

        slb.onMousePressed(mouseButton -> {
            if (productionRunning || mouseButton > 2) return true;
            boolean shift = Interactable.hasShiftDown();
            int encoded = ((idx + 1) << 3) | (mouseButton << 1) | (shift ? 1 : 0);
            beeClickSyncer.setIntValue(encoded, true, true);
            return true;
        });

        slb.tooltipBuilder(t -> {
            t.setAutoUpdate(true);
            buildBeeSlotTooltip(t, idx);
        });

        return slb;
    }

    private ItemStack getBeeStack(int idx) {
        return idx < beeSlots.size() ? beeSlots.get(idx).stack : null;
    }

    private void buildBeeSlotTooltip(RichTooltip tooltip, int idx) {
        if (productionRunning) {
            tooltip.addLine(
                EnumChatFormatting.RED + StatCollector.translateToLocal("kubatech.gui.text.mia.locked_while_running"));
            return;
        }
        if (idx < beeSlots.size()) {
            GTHelper.StackableItemSlot slot = beeSlots.get(idx);
            tooltip.addLine(slot.stack.getDisplayName());
            if (slot.count > 1) {
                tooltip.addLine(EnumChatFormatting.DARK_PURPLE + StatCollector
                    .translateToLocalFormatted("kubatech.gui.tooltip.dynamic_inventory.identical_slots", slot.count));
            }
        } else if (idx == beeSlots.size() && maxSlots > usedSlots) {
            tooltip.addLine(EnumChatFormatting.GRAY
                + StatCollector.translateToLocal("kubatech.gui.tooltip.dynamic_inventory.empty_slot"));
            int remaining = maxSlots - usedSlots;
            if (remaining > 1) {
                tooltip.addLine(EnumChatFormatting.DARK_PURPLE + StatCollector
                    .translateToLocalFormatted("kubatech.gui.tooltip.dynamic_inventory.identical_slots", remaining));
            }
        }
    }

    private static boolean isNEIFilteredOut(ItemStack item) {
        if (!ModularUI.Mods.NEI.isLoaded()) return false;
        if (!SearchField.searchInventories()) return false;
        if (item == null) return !NEIClientConfig.getSearchExpression().isEmpty();
        return !LayoutManager.searchField.getFilter().matches(item);
    }

    private int getBeeSlotCount(int idx) {
        if (idx < beeSlots.size()) return beeSlots.get(idx).count;
        return maxSlots - usedSlots;
    }

    private void handleBeeClick(int encoded) {
        if (encoded == 0) return;
        if (mainSyncManager == null || mainSyncManager.isClient()) return;
        if (multiblock.primaryMode == LargeIndustrialApiary.MODE_PRIMARY_OPERATING && multiblock.mMaxProgresstime > 0)
            return;

        int visibleSlotIdx = (encoded >>> 3) - 1;
        int mouseButton = (encoded >>> 1) & 0x3;
        boolean shift = (encoded & 1) != 0;
        if (visibleSlotIdx < 0 || visibleSlotIdx > BEE_ENTRIES_PER_PAGE || mouseButton > 2) return;

        EntityPlayer player = mainSyncManager.getPlayer();
        if (!(player instanceof EntityPlayerMP playerMP)) return;

        List<GTHelper.StackableItemSlot> serverSlots = getFullAggregatedBeeList();
        int slotIdx = beePage * BEE_ENTRIES_PER_PAGE + visibleSlotIdx;

        if (slotIdx >= 0 && slotIdx < serverSlots.size()) {
            handleOccupiedSlotClick(serverSlots.get(slotIdx), mouseButton, shift, player, playerMP);
        } else if (slotIdx == serverSlots.size() && beePage == beePageCount - 1 && mouseButton < 2) {
            handleEmptySlotClick(mouseButton, player, playerMP);
        }
        invalidateBeeListCache();
    }

    private void handleOccupiedSlotClick(GTHelper.StackableItemSlot serverSlot, int mouseButton, boolean shift,
        EntityPlayer player, EntityPlayerMP playerMP) {
        if (mouseButton == 2) {
            creativePickBee(serverSlot, player, playerMP);
        } else if (shift) {
            extractBeeToInventory(serverSlot, player, playerMP);
        } else if (player.inventory.getItemStack() != null) {
            replaceBeeWithHeldItem(serverSlot, player, playerMP);
        } else {
            extractBeeToCursor(serverSlot, player, playerMP);
        }
    }

    private void creativePickBee(GTHelper.StackableItemSlot serverSlot, EntityPlayer player, EntityPlayerMP playerMP) {
        if (!player.capabilities.isCreativeMode || player.inventory.getItemStack() != null) return;
        int realSlot = serverSlot.realSlots.get(0);
        if (realSlot >= multiblock.storage.size()) return;
        ItemStack stack = multiblock.storage.get(realSlot).queenStack.copy();
        stack.stackSize = stack.getMaxStackSize();
        player.inventory.setItemStack(stack);
        playerMP.isChangingQuantityOnly = false;
        playerMP.updateHeldItem();
    }

    private void extractBeeToInventory(GTHelper.StackableItemSlot serverSlot, EntityPlayer player,
        EntityPlayerMP playerMP) {
        int realSlot = serverSlot.realSlots.get(0);
        if (realSlot >= multiblock.storage.size()) return;
        BeeSimulator removed = multiblock.storage.remove(realSlot);
        multiblock.onStorageContentChanged(false);
        if (removed == null) return;
        if (player.inventory.addItemStackToInventory(removed.queenStack)) {
            player.inventoryContainer.detectAndSendChanges();
        } else {
            player.entityDropItem(removed.queenStack, 0.f);
        }
    }

    private void replaceBeeWithHeldItem(GTHelper.StackableItemSlot serverSlot, EntityPlayer player,
        EntityPlayerMP playerMP) {
        ItemStack input = player.inventory.getItemStack();
        if (input.stackSize != 1) return;
        int realSlot = serverSlot.realSlots.get(0);
        if (realSlot >= multiblock.storage.size()) return;
        World world = multiblock.getBaseMetaTileEntity().getWorld();
        float voltageTier = (float) multiblock.getVoltageTierExact();
        BeeSimulator bee = new BeeSimulator(input, world, voltageTier);
        if (!bee.isValid) return;
        BeeSimulator removed = multiblock.storage.remove(realSlot);
        multiblock.storage.add(realSlot, bee);
        multiblock.onStorageContentChanged(false);
        player.inventory.setItemStack(removed.queenStack);
        playerMP.isChangingQuantityOnly = false;
        playerMP.updateHeldItem();
    }

    private void extractBeeToCursor(GTHelper.StackableItemSlot serverSlot, EntityPlayer player,
        EntityPlayerMP playerMP) {
        int realSlot = serverSlot.realSlots.get(0);
        if (realSlot >= multiblock.storage.size()) return;
        BeeSimulator removed = multiblock.storage.remove(realSlot);
        multiblock.onStorageContentChanged(false);
        if (removed == null) return;
        player.inventory.setItemStack(removed.queenStack);
        playerMP.isChangingQuantityOnly = false;
        playerMP.updateHeldItem();
    }

    private void handleEmptySlotClick(int mouseButton, EntityPlayer player, EntityPlayerMP playerMP) {
        ItemStack input = player.inventory.getItemStack();
        if (input == null) return;
        if (multiblock.storage.size() >= multiblock.maxSlots) return;
        if (beeRoot.getType(input) != EnumBeeType.QUEEN) return;

        if (mouseButton == 1) {
            injectSingleBee(input, player, playerMP);
        } else {
            injectEntireStack(input, player, playerMP);
        }
    }

    private void injectSingleBee(ItemStack input, EntityPlayer player, EntityPlayerMP playerMP) {
        ItemStack singleQueen = input.copy();
        singleQueen.stackSize = 1;
        if (!tryAddBeeToStorage(singleQueen)) return;
        input.stackSize--;
        if (input.stackSize <= 0) {
            player.inventory.setItemStack(null);
        }
        playerMP.isChangingQuantityOnly = input.stackSize > 0;
        playerMP.updateHeldItem();
    }

    private void injectEntireStack(ItemStack input, EntityPlayer player, EntityPlayerMP playerMP) {
        if (!tryAddBeeToStorage(input)) return;
        if (input.stackSize > 0) {
            playerMP.isChangingQuantityOnly = true;
            playerMP.updateHeldItem();
            return;
        }
        player.inventory.setItemStack(null);
        playerMP.isChangingQuantityOnly = false;
        playerMP.updateHeldItem();
    }

    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        return super.createRightPanelGapRow(parent, syncManager).child(createInventoryToggleButton())
            .child(createConfigurationButton(syncManager, parent));
    }

    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.column().width(18).leftRel(1, -2, 1).mainAxisAlignment(Alignment.MainAxis.END).reverseLayout(true)
            .child(createPowerSwitchButton()).child(createStructureUpdateButton(syncManager));
    }

    private IWidget createInventoryToggleButton() {
        return new ButtonWidget<>().size(18, 18).background(GTNGGuiTextures.MODERN_BUTTON)
            .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER)
            .overlay(
                new DynamicDrawable(() -> isInInventory ? GTGuiTextures.OVERLAY_BUTTON_WHITELIST : OVERLAY_BEE_LIST))
            .onMousePressed(button -> {
                isInInventory = !isInInventory;
                return true;
            })
            .tooltipBuilder(t -> t.addLine(IKey.dynamic(
                () -> isInInventory ? StatCollector.translateToLocal("kubatech.gui.text.mia.show_machine_status")
                    : StatCollector.translateToLocal("kubatech.gui.text.mia.show_bee_inventory"))))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    private IWidget createConfigurationButton(PanelSyncManager syncManager, ModularPanel parent) {
        IPanelHandler configPanel = syncManager.syncedPanel("apiaryConfigPanel", true,
            (panelSyncManager, syncHandler) -> createConfigurationPanel(parent, syncManager));
        return new ButtonWidget<>().size(18, 18).background(GTNGGuiTextures.MODERN_BUTTON)
            .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER).overlay(GuiTextures.GEAR).onMousePressed(d -> {
                if (!configPanel.isPanelOpen()) {
                    configPanel.openPanel();
                } else {
                    configPanel.closePanel();
                }
                return true;
            }).tooltipBuilder(t -> t.addLine(IKey.lang("kubatech.gui.text.configuration")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    private ModularPanel createConfigurationPanel(ModularPanel parent, PanelSyncManager mainSyncManager) {
        IntSyncValue primaryModeSyncer = mainSyncManager.findSyncHandler("apiaryPrimaryMode", IntSyncValue.class);
        IntSyncValue secondaryModeSyncer = mainSyncManager.findSyncHandler("apiarySecondaryMode", IntSyncValue.class);
        InteractionSyncHandler primaryModeClick = mainSyncManager.findSyncHandler("apiaryPrimaryModeClick",
            InteractionSyncHandler.class);
        InteractionSyncHandler secondaryModeClick = mainSyncManager.findSyncHandler("apiarySecondaryModeClick",
            InteractionSyncHandler.class);

        return new ModularPanel("apiaryConfigPanel").relative(parent).leftRel(1).topRel(0).size(90, 70)
            .background(GTNGGuiTextures.MODERN_BACKGROUND)
            .child(Flow.column().sizeRel(1).padding(4)
                .child(new TextWidget<>(
                    EnumChatFormatting.UNDERLINE + StatCollector.translateToLocal("kubatech.gui.text.configuration"))
                        .alignment(Alignment.Center).height(10).marginBottom(1))
                .child(createModeEntry(primaryModeSyncer, "kubatech.gui.text.mia.primary_mode", primaryModeClick,
                    () -> machineRunning, LargeIndustrialApiaryGui::getPrimaryModeText))
                .child(createModeEntry(secondaryModeSyncer, "kubatech.gui.text.mia.secondary_mode", secondaryModeClick,
                    () -> secondaryModeLocked, LargeIndustrialApiaryGui::getSecondaryModeText)));
    }

    private IWidget createModeEntry(IntSyncValue modeSyncer, String labelKey, InteractionSyncHandler interaction,
        BooleanSupplier locked, IntFunction<String> modeTextProvider) {
        return Flow.column().widthRel(1).coverChildrenHeight().crossAxisAlignment(CrossAxis.START).marginBottom(1)
            .child(new TextWidget<>(StatCollector.translateToLocal(labelKey)).widthRel(1).height(9).marginBottom(2))
            .child(new ButtonWidget<>().overlay(new DynamicDrawable(() -> {
                IKey key = IKey.str(modeTextProvider.apply(modeSyncer.getIntValue())).alignment(Alignment.Center);
                return locked.getAsBoolean() ? key.color(0xFFA0A0A0) : key;
            })).background(new DynamicDrawable(
                () -> locked.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_DISABLED : GTNGGuiTextures.MODERN_BUTTON))
                .hoverBackground(
                    new DynamicDrawable(() -> locked.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_DISABLED
                        : GTNGGuiTextures.MODERN_BUTTON_HOVER))
                .syncHandler(interaction).tooltipBuilder(t -> {
                    t.setAutoUpdate(true);
                    t.addLine(modeTextProvider.apply(modeSyncer.getIntValue()));
                    if (machineRunning) {
                        t.addLine(EnumChatFormatting.RED
                            + StatCollector.translateToLocal("GT5U.gui.text.cannot_change_when_running"));
                    } else if (locked.getAsBoolean()) {
                        // #tr gui.gtnotgood.industrial_apiary.empty_before_mode_change
                        // # Remove all queens before changing this mode
                        // # zh_CN 清空蜂后后才能切换此模式
                        t.addLine(EnumChatFormatting.RED + StatCollector
                            .translateToLocal("gui.gtnotgood.industrial_apiary.empty_before_mode_change"));
                    }
                }).width(65).height(12).marginBottom(2));
    }

    private static String getPrimaryModeText(int mode) {
        return switch (mode) {
            case 0 -> StatCollector.translateToLocal("kubatech.gui.text.input");
            case 1 -> StatCollector.translateToLocal("kubatech.gui.text.output");
            case 2 -> StatCollector.translateToLocal("kubatech.gui.text.operating");
            default -> "";
        };
    }

    private static String getSecondaryModeText(int mode) {
        return switch (mode) {
            case 0 -> StatCollector.translateToLocal("kubatech.gui.text.mia.normal");
            case 1 -> StatCollector.translateToLocal("kubatech.gui.text.mia.swarmer");
            default -> "";
        };
    }
}
