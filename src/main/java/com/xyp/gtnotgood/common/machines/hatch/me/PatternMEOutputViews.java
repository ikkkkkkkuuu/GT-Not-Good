package com.xyp.gtnotgood.common.machines.hatch.me;

import java.lang.reflect.Field;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputSlave;

import gregtech.api.enums.OutputBusType;
import gregtech.api.enums.OutputHatchType;
import gregtech.api.interfaces.IOutputBusTransaction;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.util.GTUtility;

/**
 * Typed output views for GT's concrete hatch lists and structure counters.
 * These are not registered machines and never replace the real input meta-tile. They only expose the same physical
 * block to old APIs requiring MTEHatchOutput/Bus, while all products go to the master's native ME output providers.
 */
public final class PatternMEOutputViews {

    private static final Field BASE_TILE = baseTileField();

    private PatternMEOutputViews() {}

    /** Resolves a live master each time, so relinking a mirror cannot leave products routed to its old master. */
    public static PatternMEOutput resolve(MetaTileEntity input) {
        if (!input.isValid()) return null;
        SuperMTEHatchCraftingInputME master = input instanceof SuperMTEHatchCraftingInputME hatch ? hatch
            : input instanceof SuperMTEHatchCraftingInputSlave mirror ? mirror.getMaster() : null;
        return master != null && master.isValid() ? master.getMEOutput() : null;
    }

    /**
     * Attaches a read-through view to the real base without calling setBaseMetaTileEntity: that public setter would
     * replace the world's input hatch. Validity is explicitly delegated to the physical input instead.
     */
    private static void attach(MetaTileEntity view, MetaTileEntity input) {
        try {
            BASE_TILE.set(view, input.getBaseMetaTileEntity());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot attach pattern output view", e);
        }
    }

    private static Field baseTileField() {
        try {
            Field field = MetaTileEntity.class.getDeclaredField("mBaseMetaTileEntity");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Missing GT meta-tile base reference", e);
        }
    }

    /** An item-output role for one physical input; its inventory stays empty because the master owns the cache. */
    public static final class ItemView extends MTEHatchOutputBus {

        public final MetaTileEntity input;

        public ItemView(MetaTileEntity input) {
            super("pattern_me_item_output_view", 6, 0, new String[0], null);
            this.input = input;
            attach(this, input);
        }

        @Override
        public boolean isValid() {
            return input != null && input.isValid();
        }

        @Override
        public boolean isFiltered() {
            return false;
        }

        @Override
        public boolean isFilteredToItem(GTUtility.ItemId id) {
            return true;
        }

        @Override
        public OutputBusType getBusType() {
            return OutputBusType.MEUnfiltered;
        }

        @Override
        public boolean storePartial(ItemStack stack, boolean simulate) {
            PatternMEOutput output = resolve(input);
            return output != null && output.itemOutput.storePartial(stack, simulate);
        }

        @Override
        public IOutputBusTransaction createTransaction() {
            PatternMEOutput output = resolve(input);
            return output == null ? super.createTransaction() : output.itemOutput.createTransaction();
        }
    }

    /** A fluid-output role sharing the same physical input, independent of its supported input stack types. */
    public static final class FluidView extends MTEHatchOutput {

        public final MetaTileEntity input;

        public FluidView(MetaTileEntity input) {
            super("pattern_me_fluid_output_view", 6, 0, new String[0], null);
            this.input = input;
            attach(this, input);
        }

        @Override
        public boolean isValid() {
            return input != null && input.isValid();
        }

        @Override
        public int getCapacity() {
            return 0;
        }

        @Override
        public boolean isFiltered() {
            return false;
        }

        @Override
        public boolean isFilteredToFluid(GTUtility.FluidId id) {
            return true;
        }

        @Override
        public boolean canStoreFluid(FluidStack stack) {
            return resolve(input) != null;
        }

        @Override
        public OutputHatchType getHatchType() {
            return OutputHatchType.MEUnfiltered;
        }

        @Override
        public int fill(FluidStack stack, boolean doFill) {
            if (stack == null) return 0;
            FluidStack remaining = stack.copy();
            storePartial(remaining, !doFill);
            return stack.amount - remaining.amount;
        }

        @Override
        public boolean storePartial(FluidStack stack, boolean simulate) {
            PatternMEOutput output = resolve(input);
            return output != null && output.fluidOutput.storePartial(stack, simulate);
        }

        @Override
        public IOutputHatchTransaction createTransaction() {
            PatternMEOutput output = resolve(input);
            return output == null ? super.createTransaction() : output.fluidOutput.createTransaction();
        }
    }
}
