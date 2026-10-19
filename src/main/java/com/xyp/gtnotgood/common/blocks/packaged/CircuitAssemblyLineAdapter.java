// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.blocks.packaged;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.util.inv.MEInventoryCrafting;
import bartworks.API.recipe.BartWorksRecipeMaps;
import bartworks.common.tileentities.multis.MTECircuitAssemblyLine;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;

public final class CircuitAssemblyLineAdapter implements PackagedCoreRegistry.Adapter {

    private static MTECircuitAssemblyLine machine(TileEntity target) {
        return target instanceof IGregTechTileEntity base
            && base.getMetaTileEntity() instanceof MTECircuitAssemblyLine line ? line : null;
    }

    private static boolean calMode(MTECircuitAssemblyLine line) {
        return line != null && line.getMachineMode() == 0
            && line.getRecipeMap() == BartWorksRecipeMaps.circuitAssemblyLineRecipes;
    }

    @Override
    public boolean accepts(TileEntity target) {
        return calMode(machine(target));
    }

    @Override
    public ItemStack dispatch(TilePackagedProvider provider, PackagedTarget target, ICraftingPatternDetails pattern,
        InventoryCrafting ingredients) {
        MTECircuitAssemblyLine line = machine(target.resolve(provider.getWorldObj()));
        if (
            !calMode(line) || !line.mMachine
                || !line.isImprinted()
                || line.mMaxProgresstime > 0
                || !accessible(provider, line)
                || !line.getBaseMetaTileEntity().isAllowedToWork()
                || !provider.canQueue(target, this, pattern.getPattern())
                || line.getControllerSlot() != null
                || !line.mDualInputHatches.isEmpty()
                || line.mOutputBusses.size() != 1
        ) return null;
        var outputs = pattern.getCondensedOutputs();
        if (outputs == null || outputs.length != 1 || outputs[0] == null) return null;
        ItemStack expected = outputs[0].getItemStack();
        if (
            expected == null || outputs[0].getStackSize() <= 0 || outputs[0].getStackSize() > expected.getMaxStackSize()
        ) return null;
        expected = expected.copy();
        expected.stackSize = (int) outputs[0].getStackSize();
        var output = line.mOutputBusses.get(0);
        if (output.getClass() != MTEHatchOutputBus.class || !accessible(provider, output) || !empty(output))
            return null;
        for (var bus : line.mInputBusses) {
            if (bus.getClass() != MTEHatchInputBus.class || !accessible(provider, bus) || !empty(bus)) return null;
        }
        for (var hatch : line.mInputHatches) {
            if (
                hatch.getClass() != MTEHatchInput.class || !accessible(provider, hatch)
                    || !empty(hatch)
                    || hatch.getFluid() != null
            ) return null;
        }
        Inputs supplied = readInputs(ingredients);
        if (supplied == null) return null;
        var matches = line.getRecipeMap().findRecipeQuery().items(supplied.items.toArray(new ItemStack[0]))
            .fluids(supplied.fluids.toArray(new FluidStack[0])).specialSlot(line.getCircuitImprint().imprint.get(1))
            .voltage(line.getMaxInputVoltage()).findAll().iterator();
        while (matches.hasNext()) {
            GTRecipe recipe = matches.next();
            if (
                recipe.mOutputs.length != 1 || !TilePackagedProvider.sameItem(recipe.mOutputs[0], expected)
                    || recipe.mOutputs[0].stackSize != expected.stackSize
                    || recipe.getOutputChance(0) != 10000
                    || recipe.mFluidOutputs.length != 0
                    || recipe.mInputs.length > line.mInputBusses.size()
                    || recipe.mFluidInputs.length > line.mInputHatches.size()
            ) continue;
            ItemStack[] planned = planItems(recipe, supplied);
            if (planned == null || !matchesFluids(recipe, supplied)) continue;
            int[] slots = new int[planned.length];
            boolean fits = true;
            for (int i = 0; i < planned.length; i++) {
                slots[i] = AssemblyLineAdapter.insertionSlot(line.mInputBusses.get(i), planned[i]);
                if (slots[i] < 0) fits = false;
            }
            for (int i = 0; i < recipe.mFluidInputs.length; i++) {
                if (recipe.mFluidInputs[i].amount > line.mInputHatches.get(i).getCapacity()) fits = false;
            }
            if (!fits) continue;
            for (int i = 0; i < planned.length; i++) {
                var bus = line.mInputBusses.get(i);
                bus.setInventorySlotContents(slots[i], planned[i]);
                bus.markDirty();
            }
            for (int i = 0; i < recipe.mFluidInputs.length; i++) {
                var hatch = line.mInputHatches.get(i);
                hatch.mFluid = recipe.mFluidInputs[i].copy();
                hatch.markDirty();
            }
            line.markDirty();
            return expected.copy();
        }
        return null;
    }

    private static boolean loaded(World world, MetaTileEntity meta) {
        var base = meta.getBaseMetaTileEntity();
        return base != null && world != null
            && base.getWorld() == world
            && world.getChunkProvider().chunkExists(base.getXCoord() >> 4, base.getZCoord() >> 4)
            && world.getTileEntity(base.getXCoord(), base.getYCoord(), base.getZCoord()) == base
            && base.getMetaTileEntity() == meta;
    }

    private static boolean accessible(TilePackagedProvider provider, MetaTileEntity meta) {
        var owner = provider.getOwnerPlayer();
        var world = provider.getWorldObj();
        var base = meta.getBaseMetaTileEntity();
        return owner != null && owner.worldObj == world
            && loaded(world, meta)
            && world.canMineBlock(owner, base.getXCoord(), base.getYCoord(), base.getZCoord());
    }

    private static boolean empty(IInventory inventory) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (inventory.getStackInSlot(slot) != null) return false;
        }
        return true;
    }

    static ItemStack[] planItems(GTRecipe recipe, InventoryCrafting ingredients) {
        Inputs supplied = readInputs(ingredients);
        return supplied == null ? null : planItems(recipe, supplied);
    }

    private static ItemStack[] planItems(GTRecipe recipe, Inputs supplied) {
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack item : supplied.items) remaining.add(item.copy());
        for (int lane = 0; lane < recipe.mInputs.length; lane++) {
            if (
                recipe.mInputs[lane] == null || recipe.mInputs[lane].stackSize <= 0
                    || recipe.getInputChance(lane) != 10000
            ) return null;
        }
        ItemStack[] result = new ItemStack[recipe.mInputs.length];
        return allocate(recipe, remaining, result, 0, new int[] { 4096 }) ? result : null;
    }

    private static boolean allocate(GTRecipe recipe, List<ItemStack> remaining, ItemStack[] result, int lane,
        int[] budget) {
        if (--budget[0] < 0) return false;
        if (lane == result.length) return remaining.stream().allMatch(item -> item.stackSize == 0);
        ItemStack required = recipe.mInputs[lane];
        for (ItemStack item : remaining) {
            if (!GTUtility.areStacksEqual(required, item, true) || item.stackSize < required.stackSize) continue;
            result[lane] = item.copy();
            result[lane].stackSize = required.stackSize;
            item.stackSize -= required.stackSize;
            if (allocate(recipe, remaining, result, lane + 1, budget)) return true;
            item.stackSize += required.stackSize;
        }
        return false;
    }

    static boolean matchesFluids(GTRecipe recipe, InventoryCrafting ingredients) {
        Inputs supplied = readInputs(ingredients);
        return supplied != null && matchesFluids(recipe, supplied);
    }

    private static boolean matchesFluids(GTRecipe recipe, Inputs supplied) {
        List<FluidStack> remaining = new ArrayList<>();
        for (FluidStack fluid : supplied.fluids) remaining.add(fluid.copy());
        for (int lane = 0; lane < recipe.mFluidInputs.length; lane++) {
            FluidStack required = recipe.mFluidInputs[lane];
            if (required == null || required.amount <= 0 || recipe.getFluidInputChance(lane) != 10000) return false;
            int needed = required.amount;
            for (FluidStack fluid : remaining) {
                if (!fluid.isFluidEqual(required)) continue;
                int taken = Math.min(needed, fluid.amount);
                fluid.amount -= taken;
                needed -= taken;
            }
            if (needed != 0) return false;
        }
        return remaining.stream().allMatch(fluid -> fluid.amount == 0);
    }

    private static Inputs readInputs(InventoryCrafting ingredients) {
        if (ingredients == null) return null;
        Inputs supplied = new Inputs();
        for (int slot = 0; slot < ingredients.getSizeInventory(); slot++) {
            if (
                ingredients instanceof MEInventoryCrafting nativeInventory
                    && nativeInventory.getAEStackInSlot(slot) instanceof IAEFluidStack nativeFluid
            ) {
                if (nativeFluid.getStackSize() <= 0 || nativeFluid.getStackSize() > Integer.MAX_VALUE) return null;
                FluidStack fluid = nativeFluid.getFluidStack();
                if (fluid == null) return null;
                fluid = fluid.copy();
                fluid.amount = (int) nativeFluid.getStackSize();
                supplied.fluids.add(fluid);
                continue;
            }
            ItemStack item = ingredients.getStackInSlot(slot);
            if (item == null) continue;
            if (item.stackSize <= 0) return null;
            if (FluidDropCompat.isFluidDrop(item)) {
                FluidStack fluid = FluidDropCompat.getFluidStack(item);
                if (fluid == null || fluid.amount <= 0) return null;
                supplied.fluids.add(fluid.copy());
                continue;
            }
            ItemStack existing = null;
            for (ItemStack stored : supplied.items) {
                if (TilePackagedProvider.sameItem(stored, item)) existing = stored;
            }
            if (existing == null) supplied.items.add(item.copy());
            else {
                if ((long) existing.stackSize + item.stackSize > Integer.MAX_VALUE) return null;
                existing.stackSize += item.stackSize;
            }
        }
        return supplied;
    }

    private static final class Inputs {

        final List<ItemStack> items = new ArrayList<>();
        final List<FluidStack> fluids = new ArrayList<>();
    }

    @Override
    public IInventory output(TileEntity target) {
        MTECircuitAssemblyLine line = machine(target);
        if (line == null || !line.mMachine || line.mOutputBusses.size() != 1) return null;
        var bus = line.mOutputBusses.get(0);
        return bus.getClass() == MTEHatchOutputBus.class && loaded(target.getWorldObj(), line)
            && loaded(target.getWorldObj(), bus) ? bus : null;
    }

    @Override
    public boolean interrupt(TilePackagedProvider provider, TileEntity target, ItemStack expected) {
        MTECircuitAssemblyLine line = machine(target);
        if (line == null || !accessible(provider, line)) return false;
        line.stopMachine(ShutDownReasonRegistry.NONE);
        line.markDirty();
        return true;
    }
}
