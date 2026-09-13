package com.xyp.gtnotgood.common.mecontainer;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.FluidDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.ModernThemeAdapter;

/** LDLib-themed item/fluid pages with 36 ghost slots each, live ME amounts and recipe-viewer drag support. */
final class MEContainerGui {

    private MEContainerGui() {}

    private static final ModernThemeAdapter THEME = new ModernThemeAdapter(
        path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));

    /** Two pages share the original sample handlers; changing pages never rebuilds or clears filters. */
    static ModularPanel build(TileMEContainer tile, PanelSyncManager sync) {
        PagedWidget.Controller controller = new PagedWidget.Controller();
        ParentWidget<?> items = new ParentWidget<>().size(244, 144);
        ParentWidget<?> fluids = new ParentWidget<>().size(244, 144);
        IDrawable shell = (context, x, y, w, h, style) -> {
            THEME.panel.draw(context, x + 30, y + 20, w - 30, h - 20, style);
            THEME.panel.draw(context, x + 40, y, w - 50, 22, style);
        };
        ModularPanel panel = ModularPanel.defaultPanel("me_container", 294, 320)
            .background(shell)
            .disableHoverBackground();
        panel.child(
            IKey.lang("tile.me_container.name")
                .color(0xFF202830)
                .asWidget()
                .pos(45, 5)
                .size(217, 12));
        panel.child(
            THEME.button()
                .pos(265, 4)
                .size(14)
                .overlay(
                    UITexture.builder()
                        .location(ModList.ModIds.GT_NOT_GOOD, "gui/ldlib/modern/close")
                        .build())
                .onMousePressed(mouse -> {
                    if (mouse != 0) return false;
                    panel.closeIfOpen();
                    return true;
                }));
        StringSyncValue state = new StringSyncValue(() -> tile.online ? "1" : "0");
        sync.syncValue("state", state);
        panel.child(IKey.dynamic(() -> {
            // #tr gui.me_container.online
            // # Online
            // # zh_CN 在线
            if ("1".equals(state.getValue())) return IKey.lang("gui.me_container.online")
                .get();
            // #tr gui.me_container.offline
            // # Offline: check power and channel
            // # zh_CN 离线：检查供电与频道
            return IKey.lang("gui.me_container.offline")
                .get();
        })
            .color(() -> "1".equals(state.getValue()) ? 0xFF28613D : 0xFF903636)
            .asWidget()
            .pos(40, 29)
            .size(244, 12));
        // #tr gui.me_container.items
        // # Items - 36 ghost slots
        // # zh_CN 物品 — 36 个虚拟槽
        items.child(
            IKey.lang("gui.me_container.items")
                .color(0xFF202830)
                .asWidget()
                .pos(0, 0)
                .size(244, 12));
        // #tr gui.me_container.fluid
        // # Fluids - 36 ghost slots
        // # zh_CN 流体 — 36 个虚拟槽
        fluids.child(
            IKey.lang("gui.me_container.fluid")
                .color(0xFF202830)
                .asWidget()
                .pos(0, 0)
                .size(244, 12));
        addGridHeadings(items);
        addGridHeadings(fluids);
        ItemSample itemSamples = new ItemSample(tile);
        for (int i = 0; i < TileMEContainer.SLOT_COUNT; i++) {
            final int slot = i;
            int x = i % 6 * 18, y = 31 + i / 6 * 18;
            StringSyncValue itemCount = new StringSyncValue(() -> Long.toString(tile.networkItems[slot]));
            StringSyncValue fluidCount = new StringSyncValue(() -> Long.toString(tile.networkFluid[slot]));
            sync.syncValue("item_count_" + i, itemCount);
            sync.syncValue("fluid_count_" + i, fluidCount);
            items.child(
                new PhantomItemSlot().slot(new SampleSlot(itemSamples, i).singletonSlotGroup())
                    .background(THEME.slot, gregtech.api.modularui2.GTGuiTextures.OVERLAY_SLOT_ARROW_ME)
                    .tooltip(t -> t.addLine(IKey.dynamic(() -> amountLabel(itemCount.getValue(), false))))
                    .pos(x, y));
            FluidSample sampleTank = new FluidSample(tile, i);
            FluidSlotSyncHandler fluidSample = new FluidSlotSyncHandler(sampleTank).phantom(true)
                .controlsAmount(false);
            sync.syncValue("fluid_sample_" + i, fluidSample);
            fluids.child(
                new FluidSlot().syncHandler(fluidSample)
                    .background(THEME.slot, gregtech.api.modularui2.GTGuiTextures.OVERLAY_SLOT_ARROW_ME)
                    .tooltip(t -> t.addLine(IKey.dynamic(() -> amountLabel(fluidCount.getValue(), true))))
                    .pos(x, y));
            items.child(
                new StockDisplay(() -> itemSamples.getStackInSlot(slot), null, itemCount::getValue).pos(136 + x, y));
            fluids.child(new StockDisplay(null, sampleTank::getFluid, fluidCount::getValue).pos(136 + x, y));
        }
        panel.child(
            new PagedWidget<>().controller(controller)
                .pos(40, 44)
                .size(244, 144)
                .addPage(items)
                .addPage(fluids));
        // #tr gui.me_container.tab_items
        // # Items
        // # zh_CN 物品
        panel.child(tab(controller, 0, IKey.lang("gui.me_container.tab_items")).pos(2, 35));
        // #tr gui.me_container.tab_fluids
        // # Fluids
        // # zh_CN 流体
        panel.child(tab(controller, 1, IKey.lang("gui.me_container.tab_fluids")).pos(2, 65));
        // #tr gui.me_container.hint
        // # Drag samples; empty left-click clears. No items consumed.
        // # zh_CN 拖入样本；空手左键清空。样本不消耗。
        panel.child(
            IKey.lang("gui.me_container.hint")
                .color(0xFF4A5060)
                .asWidget()
                .pos(40, 191)
                .size(244, 22));
        for (int i = 0; i < 36; i++) {
            int column = i < 9 ? i : (i - 9) % 9;
            int row = i < 9 ? 3 : (i - 9) / 9;
            panel.child(
                new ItemSlot().syncHandler("player", i)
                    .background(THEME.slot)
                    .pos(81 + column * 18, 219 + row * 18 + (i < 9 ? 4 : 0)));
        }
        // #tr gui.me_container.io
        // # All sides: ME input / selected output.
        // # zh_CN 所有面：存入 ME / 抽取已选资源
        panel.child(
            IKey.lang("gui.me_container.io")
                .color(0xFF4A5060)
                .asWidget()
                .pos(40, 302)
                .size(244, 10));
        return panel;
    }

    /** Mirrors the GT stocking bus/hatch's configuration-to-stock pairing for all 36 indices. */
    private static void addGridHeadings(ParentWidget<?> page) {
        // #tr gui.me_container.samples
        // # Marked samples
        // # zh_CN 标记样本
        page.child(
            IKey.lang("gui.me_container.samples")
                .color(0xFF4A5060)
                .asWidget()
                .pos(0, 16)
                .size(108, 10));
        // #tr gui.me_container.stock
        // # ME network stock
        // # zh_CN ME 网络库存
        page.child(
            IKey.lang("gui.me_container.stock")
                .color(0xFF4A5060)
                .asWidget()
                .pos(136, 16)
                .size(108, 10));
        page.child(
            gregtech.api.modularui2.GTGuiTextures.PICTURE_ARROW_DOUBLE.asWidget()
                .pos(114, 76)
                .size(16));
    }

    /**
     * Read-only view of a synchronized sample and its 64-bit ME count. This is deliberately not an inventory
     * slot: clicking or dragging here cannot extract items, fill containers, or change the filter.
     * Zero-stock selections keep their tooltip but do not draw a stocked resource, matching GT's stock grid.
     */
    private static final class StockDisplay extends Widget<StockDisplay> {

        private final Supplier<ItemStack> item;
        private final Supplier<FluidStack> fluid;
        private final Supplier<String> count;

        StockDisplay(Supplier<ItemStack> item, Supplier<FluidStack> fluid, Supplier<String> count) {
            this.item = item;
            this.fluid = fluid;
            this.count = count;
            size(18);
            background(THEME.slot);
            disableHoverBackground();
            tooltip().setAutoUpdate(true)
                .tooltipBuilder(tooltip -> {
                    if (item != null) {
                        ItemStack stack = item.get();
                        if (stack == null || stack.getItem() == null) return;
                        tooltip.addFromItem(stack);
                    } else {
                        FluidStack stack = fluid.get();
                        if (stack == null || stack.getFluid() == null) return;
                        tooltip.addFromFluid(stack);
                    }
                    tooltip.addLine(IKey.str(amountLabel(count.get(), fluid != null)));
                });
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            long amount;
            try {
                amount = Long.parseLong(count.get());
            } catch (NumberFormatException ignored) {
                return;
            }
            if (amount <= 0) return;
            if (item != null) {
                ItemStack stack = item.get();
                if (stack == null || stack.getItem() == null) return;
                ItemStack icon = stack.copy();
                icon.stackSize = 1;
                new ItemDrawable(icon).draw(context, 1, 1, 16, 16, theme.getTheme());
            } else {
                FluidStack stack = fluid.get();
                if (stack == null || stack.getFluid() == null) return;
                new FluidDrawable(stack).draw(context, 1, 1, 16, 16, theme.getTheme());
            }
        }

        @Override
        public void drawOverlay(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            super.drawOverlay(context, theme);
            String quantity = count.get();
            if (quantity == null || "0".equals(quantity)) return;
            if (item != null ? item.get() == null : fluid.get() == null) return;
            IKey.str(compact(quantity))
                .color(0xFFFFFFFF)
                .shadow(true)
                .scale(0.5f)
                .alignment(Alignment.BottomRight)
                .draw(context, 1, 9, 16, 8, theme.getTheme());
        }
    }

    /** Keeps tab text inside the protruding portion, with the selected tab joined to the main panel. */
    private static PageButton tab(PagedWidget.Controller controller, int page, IKey label) {
        IDrawable caption = label.color(0xFF202830);
        IDrawable text = (context, x, y, w, h, style) -> caption.draw(context, x + 2, y + 3, w - 6, h - 6, style);
        return new PageButton(page, controller).size(32, 26)
            .background(false, THEME.tab, text)
            .background(true, THEME.selectedTab, text);
    }

    /** Exact synchronized amounts remain available in tooltips despite compact grid labels. */
    private static String amountLabel(String count, boolean fluid) {
        // #tr gui.me_container.amount
        // # Available in ME:
        // # zh_CN ME 网络库存：
        return IKey.lang("gui.me_container.amount")
            .get() + " "
            + count
            + (fluid ? " mB" : "");
    }

    /** Compact grid labels; hover text retains the exact 64-bit count. */
    private static String compact(String value) {
        long amount;
        try {
            amount = Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return "0";
        }
        if (amount < 1000) return Long.toString(amount);
        String[] units = { "k", "M", "G", "T", "P", "E" };
        double scaled = amount;
        int unit = -1;
        do {
            scaled /= 1000;
            unit++;
        } while (scaled >= 1000 && unit < units.length - 1);
        return (long) scaled + units[unit];
    }

    /** Accepts any real item as a single phantom identity, without admitting items into the export buffer. */
    private static final class SampleSlot extends ModularSlot {

        SampleSlot(IItemHandlerModifiable handler, int slot) {
            super(handler, slot);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && stack.stackSize > 0;
        }

        @Override
        public int getItemStackLimit(ItemStack stack) {
            return 1;
        }
    }

    /** GUI-only filter handler; client synchronization never writes the tile's authoritative filter. */
    private static final class ItemSample implements IItemHandlerModifiable {

        private final TileMEContainer tile;
        private final ItemStack[] client = new ItemStack[TileMEContainer.SLOT_COUNT];

        ItemSample(TileMEContainer tile) {
            this.tile = tile;
        }

        public int getSlots() {
            return TileMEContainer.SLOT_COUNT;
        }

        public int getSlotLimit(int slot) {
            return 1;
        }

        public ItemStack getStackInSlot(int slot) {
            return tile.isServerSide() ? tile.itemFilters[slot] : client[slot];
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return null;
        }

        public void setStackInSlot(int slot, ItemStack stack) {
            ItemStack copy = stack == null ? null : stack.copy();
            if (copy != null) copy.stackSize = 1;
            if (tile.isServerSide()) {
                tile.setItemFilter(slot, copy);
            } else client[slot] = copy;
        }
    }

    /** GUI-only fluid sample. Phantom slot clicks use this tank without draining the cursor container. */
    private static final class FluidSample implements IFluidTank {

        private final TileMEContainer tile;
        private FluidStack client;
        private final int slot;

        FluidSample(TileMEContainer tile, int slot) {
            this.tile = tile;
            this.slot = slot;
        }

        public FluidStack getFluid() {
            return tile.isServerSide() ? tile.fluidFilters[slot] : client;
        }

        public int getFluidAmount() {
            return getFluid() == null ? 0 : 1;
        }

        public int getCapacity() {
            return 1;
        }

        public FluidTankInfo getInfo() {
            return new FluidTankInfo(getFluid(), 1);
        }

        private void sample(FluidStack stack) {
            FluidStack copy = stack == null ? null : stack.copy();
            if (copy != null) copy.amount = 1;
            if (tile.isServerSide()) {
                tile.setFluidFilter(slot, copy);
            } else client = copy;
        }

        public int fill(FluidStack resource, boolean doFill) {
            if (resource == null || resource.amount <= 0) return 0;
            if (doFill) sample(resource);
            return 1;
        }

        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (maxDrain <= 0 || getFluid() == null) return null;
            FluidStack result = getFluid().copy();
            if (doDrain) sample(null);
            return result;
        }
    }
}
