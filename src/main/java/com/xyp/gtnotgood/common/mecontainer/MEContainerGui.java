package com.xyp.gtnotgood.common.mecontainer;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;

/** Two 36-slot ghost grids with live network amounts and recipe-viewer drag-and-drop support. */
final class MEContainerGui {

    private MEContainerGui() {}

    static ModularPanel build(TileMEContainer tile, PanelSyncManager sync) {
        ModularPanel panel = ModularPanel.defaultPanel("me_container", 446, 332);
        panel.child(
            IKey.lang("tile.me_container.name")
                .asWidget()
                .pos(8, 7));
        StringSyncValue state = new StringSyncValue(() -> tile.online ? "1" : "0");
        sync.syncValue("state", state);
        // #tr gui.me_container.online
        // # Online (uses one channel)
        // # zh_CN 在线（占用一个频道）
        // #tr gui.me_container.offline
        // # Offline: check power and channel
        // # zh_CN 离线：检查供电与频道
        panel.child(
            IKey.dynamic(
                () -> net.minecraft.util.StatCollector.translateToLocal(
                    "1".equals(state.getValue()) ? "gui.me_container.online" : "gui.me_container.offline"))
                .asWidget()
                .pos(8, 24));
        // #tr gui.me_container.items
        // # Items - 36 ghost slots
        // # zh_CN 物品 — 36 个虚拟槽
        panel.child(
            IKey.lang("gui.me_container.items")
                .asWidget()
                .pos(8, 43));
        // #tr gui.me_container.fluid
        // # Fluids - 36 ghost slots
        // # zh_CN 流体 — 36 个虚拟槽
        panel.child(
            IKey.lang("gui.me_container.fluid")
                .asWidget()
                .pos(230, 43));
        ItemSample itemSamples = new ItemSample(tile);
        for (int i = 0; i < TileMEContainer.SLOT_COUNT; i++) {
            final int slot = i;
            int column = i % 9;
            int row = i / 9;
            StringSyncValue itemCount = new StringSyncValue(() -> Long.toString(tile.networkItems[slot]));
            StringSyncValue fluidCount = new StringSyncValue(() -> Long.toString(tile.networkFluid[slot]));
            sync.syncValue("item_count_" + i, itemCount);
            sync.syncValue("fluid_count_" + i, fluidCount);
            // #tr gui.me_container.amount
            // # Available in ME:
            // # zh_CN ME 网络库存：
            panel.child(
                new PhantomItemSlot().slot(new SampleSlot(itemSamples, i).singletonSlotGroup())
                    .tooltip(
                        t -> t.addLine(
                            IKey.dynamic(
                                () -> net.minecraft.util.StatCollector.translateToLocal("gui.me_container.amount") + " "
                                    + itemCount.getValue())))
                    .pos(8 + column * 24, 60 + row * 35));
            FluidSlotSyncHandler fluidSample = new FluidSlotSyncHandler(new FluidSample(tile, i)).phantom(true)
                .controlsAmount(false);
            sync.syncValue("fluid_sample_" + i, fluidSample);
            panel.child(
                new FluidSlot().syncHandler(fluidSample)
                    .tooltip(
                        t -> t.addLine(
                            IKey.dynamic(
                                () -> net.minecraft.util.StatCollector.translateToLocal("gui.me_container.amount") + " "
                                    + fluidCount.getValue()
                                    + " mB")))
                    .pos(230 + column * 24, 60 + row * 35));
            panel.child(
                IKey.dynamic(() -> compact(itemCount.getValue()))
                    .asWidget()
                    .pos(8 + column * 24, 80 + row * 35)
                    .width(23));
            panel.child(
                IKey.dynamic(() -> compact(fluidCount.getValue()))
                    .asWidget()
                    .pos(230 + column * 24, 80 + row * 35)
                    .width(23));
        }
        // #tr gui.me_container.hint
        // # Drag in samples; empty left-click clears. Samples are not consumed.
        // # zh_CN 可拖入虚拟样本，空手左键清空；样本不消耗。
        panel.child(
            IKey.lang("gui.me_container.hint")
                .asWidget()
                .pos(8, 203));
        // #tr gui.me_container.io
        // # All sides: input to ME / extract selected resources.
        // # zh_CN 所有方向：输入至 ME 网络 / 抽取已配置资源。
        panel.child(
            IKey.lang("gui.me_container.io")
                .asWidget()
                .pos(8, 218));
        for (int i = 0; i < 36; i++) {
            int column = i < 9 ? i : (i - 9) % 9;
            int row = i < 9 ? 3 : (i - 9) / 9;
            panel.child(
                new ItemSlot().syncHandler("player", i)
                    .pos(142 + column * 18, 248 + row * 18 + (i < 9 ? 4 : 0)));
        }
        return panel;
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
