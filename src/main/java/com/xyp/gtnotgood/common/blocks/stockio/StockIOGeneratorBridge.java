package com.xyp.gtnotgood.common.blocks.stockio;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicGenerator;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.common.pollution.Pollution;

/**
 * Applies native generator fuel values and burn cadence to virtual ME stock. Local fuel runs first;
 * network fuel never enters the physical input slot or tank, and EU is granted only after ME commits.
 */
public final class StockIOGeneratorBridge {

    private StockIOGeneratorBridge() {}

    public static void onPostTick(MTEBasicGenerator generator, IGregTechTileEntity base, long tick, Runnable original) {
        if (!burnTick(base, tick)) {
            original.run();
            return;
        }
        long storedBefore = base.getUniversalEnergyStored();
        FluidStack fluidBefore = generator.mFluid;
        int fluidAmountBefore = fluidBefore == null ? 0 : fluidBefore.amount;
        ItemStack inputBefore = base.getStackInSlot(generator.getInputSlot());
        int itemAmountBefore = inputBefore == null ? 0 : inputBefore.stackSize;
        original.run();
        ItemStack inputAfter = base.getStackInSlot(generator.getInputSlot());
        if (!burnTick(base, tick) || base.getUniversalEnergyStored() > storedBefore
            || (fluidBefore != null && fluidBefore.amount < fluidAmountBefore)
            || (inputBefore != null && (inputAfter == null || inputAfter.stackSize < itemAmountBefore))) return;
        long stored = base.getUniversalEnergyStored();
        boolean fluidBudget = generator.maxEUStore() > stored;
        boolean itemBudget = stored < itemThreshold(generator);
        if (!fluidBudget && !itemBudget) return;
        StockIOLogic logic = StockIORecipeBridge.find(base);
        if (logic == null) return;
        if (fluidBudget && tryFluids(generator, base, logic)) return;
        if (itemBudget) tryItems(generator, base, logic);
    }

    private static boolean burnTick(IGregTechTileEntity base, long tick) {
        return base != null && base.isServerSide() && base.isAllowedToWork() && tick % 10 == 0;
    }

    /** Returns true once a burn is attempted, including a rejected ME commit. */
    private static boolean tryFluids(MTEBasicGenerator generator, IGregTechTileEntity base, StockIOLogic logic) {
        for (int slot = 0; slot < StockIOLogic.SLOT_COUNT; slot++) {
            FluidStack sample = logic.fluidFilters[slot];
            if (sample == null) continue;
            FluidStack probe = sample.copy();
            if (!generator.isFluidInputAllowed(probe) || generator.getFuelValue(probe) <= 0
                || generator.consumedFluidPerOperation(probe) <= 0) continue;
            StockIOSnapshot snapshot = logic.startRecipe(true, slot);
            if (snapshot == null) return false;
            try {
                FluidStack offered = snapshot.fluids[slot];
                if (offered == null) continue;
                FluidStack fuel = offered.copy();
                if (!generator.isFluidInputAllowed(fuel) || fuel.amount <= 0) continue;
                long value = generator.getFuelValue(fuel);
                int consumed = generator.consumedFluidPerOperation(fuel);
                if (value <= 0 || consumed <= 0) continue;
                long operations = Math
                    .min(fuel.amount / consumed, (generator.maxEUStore() - base.getUniversalEnergyStored()) / value);
                if (operations <= 0) continue;
                long energy = operations * value;
                offered.amount -= (int) (operations * consumed);
                if (logic.endRecipe(snapshot, () -> commitFluid(generator, base, energy))) finishBurn(generator, base);
                return true;
            } finally {
                logic.cancelRecipe(snapshot);
            }
        }
        return false;
    }

    private static boolean commitFluid(MTEBasicGenerator generator, IGregTechTileEntity base, long energy) {
        return liveGenerator(generator, base) && generator.maxEUStore() - base.getUniversalEnergyStored() >= energy
            && energy <= Long.MAX_VALUE - Math.max(0, generator.getEUVar())
            && base.increaseStoredEnergyUnits(energy, true);
    }

    private static void tryItems(MTEBasicGenerator generator, IGregTechTileEntity base, StockIOLogic logic) {
        for (int slot = 0; slot < StockIOLogic.SLOT_COUNT; slot++) {
            ItemStack sample = logic.itemFilters[slot];
            if (sample == null
                || !generator.allowPutStack(base, generator.getInputSlot(), logic.getTargetFace(), sample.copy())
                || itemValue(generator, sample.copy()) <= 0) continue;
            StockIOSnapshot snapshot = logic.startRecipe(false, slot);
            if (snapshot == null) return;
            try {
                ItemStack offered = snapshot.items[slot];
                if (offered == null || offered.stackSize <= 0) continue;
                ItemStack fuel = offered.copy();
                long energy = itemValue(generator, fuel);
                if (energy <= 0
                    || !generator.allowPutStack(base, generator.getInputSlot(), logic.getTargetFace(), fuel.copy()))
                    continue;
                ItemStack empty = generator.getEmptyContainer(fuel);
                if (!canAcceptContainer(generator, base, empty)) continue;
                offered.stackSize--;
                if (logic.endRecipe(snapshot, () -> commitItem(generator, base, energy, empty)))
                    finishBurn(generator, base);
                return;
            } finally {
                logic.cancelRecipe(snapshot);
            }
        }
    }

    private static long itemValue(MTEBasicGenerator generator, ItemStack fuel) {
        if (GTUtility.getFluidForFilledItem(fuel, true) == null && !generator.solidFuelOverride(fuel)) return 0;
        long value = generator.getFuelValue(fuel);
        return value > 0 ? value : generator.getFuelValue(fuel, true);
    }

    private static long itemThreshold(MTEBasicGenerator generator) {
        return generator.maxEUOutput() * 20 + generator.getMinimumStoredEU();
    }

    private static boolean canAcceptContainer(MTEBasicGenerator generator, IGregTechTileEntity base, ItemStack empty) {
        if (GTUtility.isStackInvalid(empty)) return true;
        int output = generator.getOutputSlot();
        if (output < 0 || output >= base.getSizeInventory()) return false;
        ItemStack existing = base.getStackInSlot(output);
        if (GTUtility.isStackInvalid(existing)) return true;
        ItemStack unified = GTOreDictUnificator.get(empty);
        return GTUtility.areStacksEqual(existing, unified) && (long) existing.stackSize + unified.stackSize
            <= Math.min(unified.getMaxStackSize(), base.getInventoryStackLimit());
    }

    private static boolean commitItem(MTEBasicGenerator generator, IGregTechTileEntity base, long energy,
        ItemStack empty) {
        if (!liveGenerator(generator, base) || base.getUniversalEnergyStored() >= itemThreshold(generator)
            || energy > Long.MAX_VALUE - Math.max(0, generator.getEUVar())
            || !canAcceptContainer(generator, base, empty)) return false;
        int output = generator.getOutputSlot();
        ItemStack previous = base.getStackInSlot(output);
        ItemStack saved = previous == null ? null : previous.copy();
        if (!base.addStackToSlot(output, empty == null ? null : empty.copy())) return false;
        if (base.increaseStoredEnergyUnits(energy, true)) return true;
        if (!GTUtility.isStackInvalid(empty)) base.setInventorySlotContents(output, saved);
        return false;
    }

    private static boolean liveGenerator(MTEBasicGenerator generator, IGregTechTileEntity base) {
        return base.isServerSide() && base.isAllowedToWork()
            && base.getMetaTileEntity() == generator
            && generator.getBaseMetaTileEntity() == base
            && (!(base instanceof TileEntity tile) || !tile.isInvalid());
    }

    private static void finishBurn(MTEBasicGenerator generator, IGregTechTileEntity base) {
        Pollution.addPollution(base, generator.getPollution() / 2);
        base.setActive(
            base.isAllowedToWork()
                && base.getUniversalEnergyStored() >= generator.maxEUOutput() + generator.getMinimumStoredEU());
    }
}
