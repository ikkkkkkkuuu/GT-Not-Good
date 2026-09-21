package com.xyp.gtnotgood.common.packaged;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.util.inv.MEInventoryCrafting;
import ggfab.mte.MTEAdvAssLine;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.AssemblyLineUtils;
import gregtech.api.util.GTRecipe.RecipeAssemblyLine;
import gregtech.common.tileentities.machines.multi.MTEAssemblyLine;

/**
 * Delivers authorized batches to an assembly line, in the machine's own hatch order.
 * Only ordinary local hatches are supported: ME and shared inventories cannot provide
 * an atomic transfer. The real controller retains all processing, power and research checks.
 * Advanced lines keep their ingredients in later slices until their normal running tick consumes them.
 */
public final class AssemblyLineAdapter implements PackagedCoreRegistry.Adapter {

    private final boolean advanced;

    public AssemblyLineAdapter(boolean advanced) {
        this.advanced = advanced;
    }

    private MTEMultiBlockBase machine(TileEntity tile) {
        if (!(tile instanceof IGregTechTileEntity base)) return null;
        var meta = base.getMetaTileEntity();
        if (advanced && meta instanceof MTEAdvAssLine line) return line;
        if (!advanced && meta instanceof MTEAssemblyLine line) return line;
        return null;
    }

    @Override
    public boolean accepts(TileEntity target) {
        return machine(target) != null;
    }

    @Override
    public int maxInFlight() {
        return advanced ? 17 : 2;
    }

    /**
     * Uses GT's real shutdown path, including clearing advanced-line slice progress. Buffered ingredients and
     * finished outputs remain in the hatches; already consumed ingredients are not reconstructed or refunded.
     * The controller stays disabled until the user clears the remainder and explicitly enables it again.
     */
    @Override
    public boolean interrupt(TilePackagedProvider provider, TileEntity target, ItemStack expected) {
        MTEMultiBlockBase line = machine(target);
        if (line == null || !accessible(provider, line)) return false;
        line.stopMachine(gregtech.api.util.shutdown.ShutDownReasonRegistry.NONE);
        line.markDirty();
        return true;
    }

    @Override
    public ItemStack dispatch(TilePackagedProvider provider, PackagedTarget target, ICraftingPatternDetails pattern,
        InventoryCrafting ingredients) {
        MTEMultiBlockBase line = machine(target.resolve(provider.getWorldObj()));
        boolean continuing = provider.busy(target);
        if (!provider.canQueue(target, this, pattern.getPattern())) return null;
        if (line == null || !line.mMachine
            || !continuing && line.mMaxProgresstime > 0
            || !line.getBaseMetaTileEntity()
                .isAllowedToWork()
            || !accessible(provider, line)) return null;
        var outputs = pattern.getCondensedOutputs();
        if (outputs == null || outputs.length != 1 || outputs[0] == null) return null;
        ItemStack expected = outputs[0].getItemStack();
        if (expected == null || outputs[0].getStackSize() <= 0
            || outputs[0].getStackSize() > expected.getMaxStackSize()) return null;
        // A new lane must be empty; an owned lane may hold earlier batches of this exact pattern.
        if (line.mOutputBusses.size() != 1) return null;
        var output = line.mOutputBusses.get(0);
        if (output.getClass() != MTEHatchOutputBus.class || !accessible(provider, output)
            || !continuing && !empty(output)) return null;
        for (var bus : line.mInputBusses) {
            if (bus.getClass() != MTEHatchInputBus.class || !accessible(provider, bus) || !continuing && !empty(bus))
                return null;
        }
        for (var hatch : line.mInputHatches) {
            if (hatch.getClass() != MTEHatchInput.class || !accessible(provider, hatch)
                || !empty(hatch)
                || !continuing && hatch.getFluid() != null) return null;
        }
        List<RecipeAssemblyLine> recipes = new ArrayList<>();
        ItemStack stick = line.getStackInSlot(1);
        if (AssemblyLineUtils.isItemDataStick(stick))
            recipes.addAll(AssemblyLineUtils.findALRecipeFromDataStick(stick));
        var dataHatches = line instanceof MTEAssemblyLine ordinary ? ordinary.mDataAccessHatches
            : line instanceof AssemblyLineDataAccess access ? access.gtnotgood$getDataAccessHatches() : null;
        if (dataHatches != null) {
            for (var hatch : dataHatches) {
                if (!accessible(provider, hatch)) return null;
                recipes.addAll(hatch.getAssemblyLineRecipes());
            }
        }
        for (var recipe : recipes) {
            if (!TilePackagedProvider.sameItem(recipe.mOutput, expected)
                || recipe.mOutput.stackSize != expected.stackSize
                || recipe.mInputs.length > line.mInputBusses.size()
                || recipe.mFluidInputs.length > line.mInputHatches.size()) continue;
            ItemStack[] planned = planItems(recipe, ingredients);
            if (planned == null || !matchesFluids(recipe, ingredients)) continue;
            boolean fits = true;
            int[] slots = new int[planned.length];
            for (int i = 0; i < planned.length; i++) {
                slots[i] = insertionSlot(line.mInputBusses.get(i), planned[i]);
                if (slots[i] < 0) fits = false;
            }
            for (int i = 0; i < recipe.mFluidInputs.length; i++) {
                var hatch = line.mInputHatches.get(i);
                var stored = hatch.getFluid();
                var required = recipe.mFluidInputs[i];
                if (stored != null && !stored.isFluidEqual(required)
                    || (long) required.amount + (stored == null ? 0 : stored.amount) > hatch.getCapacity())
                    fits = false;
            }
            for (int i = planned.length; i < line.mInputBusses.size(); i++)
                if (!empty(line.mInputBusses.get(i))) fits = false;
            for (int i = recipe.mFluidInputs.length; i < line.mInputHatches.size(); i++) if (line.mInputHatches.get(i)
                .getFluid() != null) fits = false;
            if (!fits) continue;
            for (int i = 0; i < planned.length; i++) {
                var bus = line.mInputBusses.get(i);
                ItemStack stored = bus.getStackInSlot(slots[i]);
                if (stored != null) planned[i].stackSize += stored.stackSize;
                bus.setInventorySlotContents(slots[i], planned[i]);
                bus.markDirty();
            }
            for (int i = 0; i < recipe.mFluidInputs.length; i++) {
                var hatch = line.mInputHatches.get(i);
                FluidStack added = recipe.mFluidInputs[i].copy();
                if (hatch.getFluid() != null) added.amount += hatch.getFluid().amount;
                hatch.mFluid = added;
                hatch.markDirty();
            }
            line.markDirty();
            return expected.copy();
        }
        return null;
    }

    /** Rejects stale structure references and checks protection without loading a missing chunk. */
    private static boolean accessible(TilePackagedProvider provider, MetaTileEntity meta) {
        var base = meta.getBaseMetaTileEntity();
        var owner = provider.getOwnerPlayer();
        var world = provider.getWorldObj();
        return base != null && owner != null
            && owner.worldObj == world
            && base.getWorld() == world
            && world.getChunkProvider()
                .chunkExists(base.getXCoord() >> 4, base.getZCoord() >> 4)
            && world.getTileEntity(base.getXCoord(), base.getYCoord(), base.getZCoord()) == base
            && base.getMetaTileEntity() == meta
            && world.canMineBlock(owner, base.getXCoord(), base.getYCoord(), base.getZCoord());
    }

    private static boolean empty(IInventory inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            if (inventory.getStackInSlot(i) != null) return false;
        }
        return true;
    }

    /**
     * Reserves one complete lane ingredient without splitting it across slots, since GT reads the first stack.
     * Existing stacks must match exactly; no caller-owned stack is changed until all lanes have passed planning.
     * 
     * @return a slot able to hold the whole ingredient, or -1 if occupied by another item or out of space
     */
    static int insertionSlot(IInventory inventory, ItemStack ingredient) {
        int limit = Math.min(ingredient.getMaxStackSize(), inventory.getInventoryStackLimit());
        if (ingredient.stackSize <= 0 || ingredient.stackSize > limit) return -1;
        int selected = -1;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (inventory instanceof MTEHatchInputBus bus && !bus.isValidSlot(slot)) continue;
            ItemStack stored = inventory.getStackInSlot(slot);
            if (stored != null
                && (!TilePackagedProvider.sameItem(stored, ingredient) || stored.stackSize % ingredient.stackSize != 0))
                return -1;
            // Sorting may compact into full stacks. Avoid a short first stack blocking later ingredients.
            if (slot > 0 && limit % ingredient.stackSize != 0) continue;
            if (selected < 0 && (stored == null || (long) stored.stackSize + ingredient.stackSize <= limit))
                selected = slot;
        }
        return selected;
    }

    /**
     * Splits condensed AE inputs into ordered recipe lanes, including repeated ingredients and alternatives.
     * Search is bounded to prevent pathological alternative recipes from blocking the server tick.
     *
     * @return independent stacks for each lane, or null when the exact input multiset cannot be allocated
     */
    static ItemStack[] planItems(RecipeAssemblyLine recipe, InventoryCrafting ingredients) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < ingredients.getSizeInventory(); i++) {
            ItemStack stack = ingredients.getStackInSlot(i);
            if (stack == null) continue;
            if (stack.stackSize <= 0) return null;
            if (fluidInput(ingredients, i) != null || FluidDropCompat.isFluidDrop(stack)) continue;
            ItemStack existing = null;
            for (var item : items) if (TilePackagedProvider.sameItem(item, stack)) existing = item;
            if (existing == null) items.add(stack.copy());
            else {
                if ((long) existing.stackSize + stack.stackSize > Integer.MAX_VALUE) return null;
                existing.stackSize += stack.stackSize;
            }
        }
        ItemStack[] result = new ItemStack[recipe.mInputs.length];
        return allocate(recipe, items, result, 0, new int[] { 4096 }) ? result : null;
    }

    private static boolean allocate(RecipeAssemblyLine recipe, List<ItemStack> items, ItemStack[] result, int lane,
        int[] budget) {
        if (--budget[0] < 0) return false;
        if (lane == result.length) return items.stream()
            .allMatch(stack -> stack.stackSize == 0);
        ItemStack[] alternatives = recipe.mOreDictAlt == null ? null : recipe.mOreDictAlt[lane];
        for (ItemStack item : items) {
            int amount = RecipeAssemblyLine.getMatchedIngredientAmount(item, recipe.mInputs[lane], alternatives);
            if (amount <= 0 || item.stackSize < amount) continue;
            result[lane] = item.copy();
            result[lane].stackSize = amount;
            item.stackSize -= amount;
            if (allocate(recipe, items, result, lane + 1, budget)) return true;
            item.stackSize += amount;
        }
        return false;
    }

    /** Matches exact fluid amounts while allowing repeated fluids in separate recipe hatches. */
    static boolean matchesFluids(RecipeAssemblyLine recipe, InventoryCrafting ingredients) {
        List<FluidStack> supplied = new ArrayList<>();
        for (int i = 0; i < ingredients.getSizeInventory(); i++) {
            ItemStack stack = ingredients.getStackInSlot(i);
            FluidStack fluid = fluidInput(ingredients, i);
            if (fluid == null && !FluidDropCompat.isFluidDrop(stack)) continue;
            if (fluid == null || fluid.amount <= 0) return false;
            supplied.add(fluid.copy());
        }
        for (FluidStack required : recipe.mFluidInputs) {
            int remaining = required.amount;
            for (FluidStack fluid : supplied) {
                if (!fluid.isFluidEqual(required)) continue;
                int taken = Math.min(remaining, fluid.amount);
                remaining -= taken;
                fluid.amount -= taken;
            }
            if (remaining != 0) return false;
        }
        return supplied.stream()
            .allMatch(fluid -> fluid.amount == 0);
    }

    /**
     * Reads AE's native fluid payload before the legacy ItemStack view, which exposes a fluid packet.
     * Legacy integrations may still deliver fluid drops in a vanilla crafting inventory.
     *
     * @param ingredients crafting CPU's supplied inventory
     * @param slot        input slot to inspect
     * @return an independent fluid stack, or null for a nonfluid/invalid slot
     */
    private static FluidStack fluidInput(InventoryCrafting ingredients, int slot) {
        if (ingredients instanceof MEInventoryCrafting nativeInventory
            && nativeInventory.getAEStackInSlot(slot) instanceof IAEFluidStack fluid) {
            if (fluid.getStackSize() <= 0 || fluid.getStackSize() > Integer.MAX_VALUE) return null;
            return fluid.getFluidStack()
                .copy();
        }
        return FluidDropCompat.getFluidStack(ingredients.getStackInSlot(slot));
    }

    @Override
    public IInventory output(TileEntity target) {
        MTEMultiBlockBase line = machine(target);
        if (line == null || !line.mMachine || line.mOutputBusses.size() != 1) return null;
        var bus = line.mOutputBusses.get(0);
        var base = bus.getBaseMetaTileEntity();
        if (bus.getClass() != MTEHatchOutputBus.class || base == null
            || !target.getWorldObj()
                .getChunkProvider()
                .chunkExists(base.getXCoord() >> 4, base.getZCoord() >> 4)
            || target.getWorldObj()
                .getTileEntity(base.getXCoord(), base.getYCoord(), base.getZCoord()) != base
            || base.getMetaTileEntity() != bus) return null;
        return bus;
    }
}
