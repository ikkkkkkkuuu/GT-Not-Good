package com.xyp.gtnotgood.common.machines.hatch.me;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;

import gregtech.common.gui.modularui.hatch.MTEHatchOutputBusMEGui;
import gregtech.common.gui.modularui.hatch.MTEHatchOutputMEGui;

/** Extends the native ME output controls with a separate row of nine non-consuming filter samples. */
final class MaxCapacityMEOutputGui {

    private MaxCapacityMEOutputGui() {}

    /** Leaves room for both the native controls and the filter row above the player inventory. */
    private static ParentWidget<?> content(ParentWidget<?> nativeControls) {
        ParentWidget<?> content = new ParentWidget<>().fullWidth()
            .expanded();
        content.child(
            Flow.column()
                .fullWidth()
                .height(24)
                .top(0)
                .child(nativeControls));
        // #tr gui.gtnotgood.me_output.filters
        // # Filters (empty: accept all)
        // # zh_CN 过滤标记（全空：接收全部）
        content.child(
            IKey.lang("gui.gtnotgood.me_output.filters")
                .asWidget()
                .pos(0, 28));
        return content;
    }

    /** Native item bus GUI with an additional whitelist row. */
    static final class Items extends MTEHatchOutputBusMEGui {

        private final MaxCapacityMEOutputBus bus;

        Items(MaxCapacityMEOutputBus bus) {
            super(bus);
            this.bus = bus;
        }

        @Override
        protected int getBasePanelWidth() {
            return 188;
        }

        @Override
        protected int getBasePanelHeight() {
            return 196;
        }

        @Override
        protected boolean supportsBottomRowOverlap() {
            return false;
        }

        @Override
        protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager sync) {
            ParentWidget<?> content = content(super.createContentSection(panel, sync));
            ItemSamples samples = new ItemSamples(bus);
            for (int i = 0; i < MaxCapacityMEOutputFilters.SLOT_COUNT; i++) {
                content.child(
                    new PhantomItemSlot().slot(new SampleSlot(samples, i).singletonSlotGroup())
                        .pos(i * 18, 42));
            }
            return content;
        }
    }

    /** Native fluid hatch GUI with phantom fluid slots that also accept fluid containers. */
    static final class Fluids extends MTEHatchOutputMEGui {

        private final MaxCapacityMEOutputHatch hatch;

        Fluids(MaxCapacityMEOutputHatch hatch) {
            super(hatch);
            this.hatch = hatch;
        }

        @Override
        protected int getBasePanelWidth() {
            return 188;
        }

        @Override
        protected int getBasePanelHeight() {
            return 196;
        }

        @Override
        protected boolean supportsBottomRowOverlap() {
            return false;
        }

        @Override
        protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager sync) {
            ParentWidget<?> content = content(super.createContentSection(panel, sync));
            for (int i = 0; i < MaxCapacityMEOutputFilters.SLOT_COUNT; i++) {
                FluidSlotSyncHandler handler = new FluidSlotSyncHandler(new FluidSample(hatch, i)).phantom(true)
                    .controlsAmount(false);
                sync.syncValue("output_filter_" + i, handler);
                content.child(
                    new FluidSlot().syncHandler(handler)
                        .pos(i * 18, 42));
            }
            return content;
        }
    }

    /** Any item can be marked, but the ghost slot always contains exactly one sample. */
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

    /** Only server clicks update machine state; client packets populate a GUI-local mirror. */
    private static final class ItemSamples implements IItemHandlerModifiable {

        private final MaxCapacityMEOutputBus bus;
        private final ItemStack[] client = new ItemStack[MaxCapacityMEOutputFilters.SLOT_COUNT];

        ItemSamples(MaxCapacityMEOutputBus bus) {
            this.bus = bus;
        }

        public int getSlots() {
            return MaxCapacityMEOutputFilters.SLOT_COUNT;
        }

        public int getSlotLimit(int slot) {
            return 1;
        }

        public ItemStack getStackInSlot(int slot) {
            return bus.getBaseMetaTileEntity()
                .isServerSide()
                    ? bus.getFilters()
                        .getItem(slot)
                    : client[slot];
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return null;
        }

        public void setStackInSlot(int slot, ItemStack stack) {
            if (bus.getBaseMetaTileEntity()
                .isServerSide())
                bus.getFilters()
                    .setItem(slot, stack);
            else {
                client[slot] = stack == null ? null : stack.copy();
                if (client[slot] != null) client[slot].stackSize = 1;
            }
        }
    }

    /** A sample tank used only by MUI's phantom synchronization, never connected to real fluid storage. */
    private static final class FluidSample implements IFluidTank {

        private final MaxCapacityMEOutputHatch hatch;
        private final int slot;
        private FluidStack client;

        FluidSample(MaxCapacityMEOutputHatch hatch, int slot) {
            this.hatch = hatch;
            this.slot = slot;
        }

        public FluidStack getFluid() {
            return hatch.getBaseMetaTileEntity()
                .isServerSide()
                    ? hatch.getFilters()
                        .getFluid(slot)
                    : client;
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
            if (hatch.getBaseMetaTileEntity()
                .isServerSide())
                hatch.getFilters()
                    .setFluid(slot, stack);
            else {
                client = stack == null ? null : stack.copy();
                if (client != null) client.amount = 1;
            }
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
