package com.xyp.gtnotgood.common.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.config.Config;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.inv.MEInventoryCrafting;
import gregtech.api.enums.GTValues;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.common.items.ItemIntegratedCircuit;

/**
 * Selects a virtual circuit from a complete AE processing pattern and accepts its resources atomically.
 * All calls run synchronously on the server. Repeated batches of one recipe can refill a working machine;
 * changing circuits or molds requires an idle machine with empty inputs. Recipe definitions are assumed
 * stable after pack startup, like the encoded pattern definitions cached by AE.
 */
public final class AutomaticMachineCircuit {

    /** Weak pattern keys release cached matches when AE replaces or removes its pattern details. */
    private static final Map<RecipeMap<?>, Map<ICraftingPatternDetails, List<Match>>> MATCHES = new WeakHashMap<>();

    /** Successful deliveries only; weak machine keys do not keep unloaded tiles alive. */
    private static final Map<MTEBasicMachine, Match> LAST_DELIVERIES = new WeakHashMap<>();

    /** At most eight distinct diagnostic reports per loaded tile; ordinary busy/capacity retries remain silent. */
    private static final Map<BaseMetaTileEntity, Set<String>> REPORTED_REJECTIONS = new WeakHashMap<>();

    private AutomaticMachineCircuit() {}

    /**
     * Keeps unsupported GT tile entities on AE's ordinary insertion path.
     *
     * @param tile receiving GT tile entity
     * @return whether automatic pattern reception owns this machine's AE input
     */
    public static boolean supports(BaseMetaTileEntity tile) {
        return Config.enableAutomaticMachineCircuit && tile.getMetaTileEntity() instanceof MTEBasicMachine machine
            && machine.allowSelectCircuit()
            && !machine.isSteampowered()
            && machine.getRecipeMap() != null;
    }

    /**
     * Validates recipe identity, actual CPU inputs, receiving-face permissions and space before committing anything.
     * A failed push leaves the circuit, input slots, tank and CPU table untouched. AE retries the complete batch.
     *
     * @param tile    receiving machine
     * @param pattern processing pattern; virtual circuits and molds must be omitted from consumable inputs
     * @param table   actual resources supplied by AE, including native fluid stacks
     * @param side    receiving machine face
     * @return true only when every supplied resource has been inserted
     */
    public static boolean push(BaseMetaTileEntity tile, ICraftingPatternDetails pattern, InventoryCrafting table,
        ForgeDirection side) {
        if (!supports(tile) || tile.getWorldObj() == null
            || tile.getWorldObj().isRemote
            || side == null
            || side == ForgeDirection.UNKNOWN
            || pattern.isCraftable()
            || !(table instanceof MEInventoryCrafting aeTable)) return false;
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        if (!tile.isAllowedToWork()) return false;
        int first = machine.getInputSlot();
        Map<Ingredient, Long> buffered = new HashMap<>();
        for (int slot = first; slot < first + machine.mInputSlotCount; slot++) {
            ItemStack existing = machine.getStackInSlot(slot);
            if (existing != null && existing.stackSize > 0) {
                add(buffered, new Ingredient(existing, false), existing.stackSize);
            }
        }
        FluidStack oldFluid = machine.getFillableStack();
        if (oldFluid != null && oldFluid.amount > 0) {
            add(buffered, new Ingredient(oldFluid, false), oldFluid.amount);
        }

        Map<Ingredient, Long> actual = new HashMap<>();
        List<ItemStack> items = new ArrayList<>();
        FluidStack fluid = null;
        for (int slot = 0; slot < aeTable.getSizeInventory(); slot++) {
            IAEStack<?> stack = aeTable.getAEStackInSlot(slot);
            if (stack == null) continue;
            long amount = stack.getStackSize();
            if (amount <= 0 || amount > Integer.MAX_VALUE) return false;
            if (!addAE(actual, stack, false)) return false;
            if (stack instanceof IAEItemStack item) {
                ItemStack copy = item.getItemStack()
                    .copy();
                copy.stackSize = (int) amount;
                items.add(copy);
            } else if (stack instanceof IAEFluidStack liquid) {
                FluidStack copy = liquid.getFluidStack()
                    .copy();
                copy.amount = (int) amount;
                if (fluid == null) fluid = copy;
                else {
                    if (!fluid.isFluidEqual(copy) || (long) fluid.amount + copy.amount > Integer.MAX_VALUE)
                        return false;
                    fluid.amount += copy.amount;
                }
            } else return false;
        }
        Match selected = null;
        List<Match> candidates = matches(machine.getRecipeMap(), pattern);
        for (Match candidate : candidates) {
            if (candidate.recipe.mEUt > GTValues.V[machine.mTier] || matchingInputs(candidate, actual) == 0) continue;
            if (selected != null && !sameCircuit(selected.circuit, candidate.circuit))
                return reportRejection(tile, pattern, aeTable, "ambiguous-circuit", candidates);
            // Several catalog lenses can produce the same encoded output from the same delivery.
            // Keep the first valid choice unless another matches the installed mold, so retries and
            // continuous refills do not switch catalysts merely because registration order differs.
            if (selected == null || sameCircuit(VirtualMachineMolds.get(machine), candidate.mold)) selected = candidate;
        }
        if (selected == null) return reportRejection(
            tile,
            pattern,
            aeTable,
            candidates.isEmpty() ? "no-pattern-match" : "voltage-or-delivered-inputs",
            candidates);
        if (!CircuitRefillPolicy.canAccept(
            machine.mMaxProgresstime > 0,
            !buffered.isEmpty(),
            sameCircuit(machine.getStackInSlot(machine.getCircuitSlot()), selected.circuit)
                && sameCircuit(VirtualMachineMolds.get(machine), selected.mold),
            sameRecipe(LAST_DELIVERIES.get(machine), selected),
            buffered.isEmpty() || matchingInputs(selected, buffered) > 0)) return false;

        FluidStack combinedFluid = oldFluid == null ? null : oldFluid.copy();
        if (fluid != null) {
            if (combinedFluid == null || combinedFluid.amount <= 0) combinedFluid = fluid.copy();
            else {
                if (!combinedFluid.isFluidEqual(fluid)
                    || (long) combinedFluid.amount + fluid.amount > Integer.MAX_VALUE) return false;
                combinedFluid.amount += fluid.amount;
            }
        }

        // Simulate sequential slot insertion against a temporary inventory, including GT's multi-stack filter.
        // Restore the original array before notifying GT of the committed change.
        ItemStack[] original = machine.mInventory.clone();
        ItemStack[] staged = machine.mInventory;
        ItemStack[] committed;
        ItemStack originalMold = VirtualMachineMolds.get(machine);
        staged[machine.getCircuitSlot()] = selected.circuit == null ? null : selected.circuit.copy();
        VirtualMachineMolds.set(machine, selected.mold);
        try {
            if (fluid != null && tile.fill(side, fluid, false) != fluid.amount) return false;
            for (ItemStack item : items) {
                int remaining = item.stackSize;
                boolean allowedSlot = false;
                for (int slot = first; slot < first + machine.mInputSlotCount && remaining > 0; slot++) {
                    if (!tile.canInsertItem(slot, item, side.ordinal())) continue;
                    allowedSlot = true;
                    ItemStack existing = staged[slot];
                    if (existing != null && existing.stackSize > 0 && !sameItem(existing, item)) continue;
                    int present = existing == null ? 0 : Math.max(0, existing.stackSize);
                    int moved = Math
                        .min(remaining, Math.min(item.getMaxStackSize(), tile.getInventoryStackLimit()) - present);
                    if (moved <= 0) continue;
                    ItemStack copy = item.copy();
                    copy.stackSize = present + moved;
                    staged[slot] = copy;
                    remaining -= moved;
                }
                if (remaining != 0) {
                    if (!allowedSlot && buffered.isEmpty() && machine.mMaxProgresstime <= 0)
                        return reportRejection(tile, pattern, aeTable, "empty-machine-insertion-filter", candidates);
                    return false;
                }
            }
            ItemStack[] recipeInputs = new ItemStack[machine.mInputSlotCount + 1];
            System.arraycopy(staged, first, recipeInputs, 0, machine.mInputSlotCount);
            recipeInputs[machine.mInputSlotCount] = staged[machine.getCircuitSlot()];
            // Let GT choose a runnable recipe, then compare its contents. Duplicate registrations can
            // produce different recipe objects with the same inputs, outputs and configuration circuit.
            GTRecipe runnable = machine.getRecipeMap()
                .findRecipeQuery()
                .items(VirtualMachineMolds.append(recipeInputs, selected.mold))
                .fluids(combinedFluid)
                .specialSlot(machine.getStackInSlot(machine.getSpecialSlotIndex()))
                .voltage(GTValues.V[machine.mTier])
                .find();
            if (!sameRecipe(selected, describe(runnable))) return reportRejection(
                tile,
                pattern,
                aeTable,
                "final-lookup-mismatch: " + recipeDescription(runnable),
                candidates);
            committed = staged.clone();
        } finally {
            System.arraycopy(original, 0, machine.mInventory, 0, original.length);
            VirtualMachineMolds.set(machine, originalMold);
        }
        // There is no intervening tick between simulation and commit. Use the normal sided fluid handler;
        // guard against an inconsistent handler by restoring its input tank if it accepts only part of the batch.
        if (fluid != null) {
            ItemStack oldCircuit = machine.mInventory[machine.getCircuitSlot()];
            FluidStack originalFluid = oldFluid == null ? null : oldFluid.copy();
            machine.mInventory[machine.getCircuitSlot()] = committed[machine.getCircuitSlot()];
            VirtualMachineMolds.set(machine, selected.mold);
            boolean filled = false;
            try {
                filled = tile.fill(side, fluid, true) == fluid.amount;
                if (!filled) return false;
            } finally {
                machine.mInventory[machine.getCircuitSlot()] = oldCircuit;
                VirtualMachineMolds.set(machine, originalMold);
                if (!filled) machine.setFillableStack(originalFluid);
            }
        }
        VirtualMachineMolds.set(machine, selected.mold);
        tile.setInventorySlotContents(machine.getCircuitSlot(), committed[machine.getCircuitSlot()]);
        for (int slot = first; slot < first + machine.mInputSlotCount; slot++) {
            tile.setInventorySlotContents(slot, committed[slot]);
        }
        LAST_DELIVERIES.put(machine, selected);
        return true;
    }

    /**
     * Records actionable matching failures without changing reception decisions or repeatedly logging AE retries.
     * Reports include raw stack metadata/NBT so material unification and wildcard expansion can be distinguished.
     * The bounded state holds no world references and is released when the receiving tile unloads.
     *
     * @return false, allowing use at an existing rejection point
     */
    private static boolean reportRejection(BaseMetaTileEntity tile, ICraftingPatternDetails pattern,
        MEInventoryCrafting table, String reason, List<Match> candidates) {
        Set<String> reported = REPORTED_REJECTIONS.computeIfAbsent(tile, ignored -> new HashSet<>());
        if (reported.size() >= 8 || !reported.add(pattern.hashCode() + ":" + reason)) return false;
        MTEBasicMachine machine = (MTEBasicMachine) tile.getMetaTileEntity();
        List<IAEStack<?>> delivered = new ArrayList<>();
        for (int slot = 0; slot < table.getSizeInventory(); slot++) {
            IAEStack<?> stack = table.getAEStackInSlot(slot);
            if (stack != null) delivered.add(stack);
        }
        GTNotGood.LOG.info(
            "[AutoCircuit] rejected={} dimension={} pos={},{},{} tier={} pattern={} inputs={} outputs={} delivered={}",
            reason,
            tile.getWorldObj().provider.dimensionId,
            tile.xCoord,
            tile.yCoord,
            tile.zCoord,
            machine.mTier,
            pattern.getPattern()
                .writeToNBT(new NBTTagCompound()),
            Arrays.toString(pattern.getAEInputs()),
            Arrays.toString(pattern.getAEOutputs()),
            delivered);
        int shown = 0;
        for (GTRecipe recipe : machine.getRecipeMap()
            .getAllRecipes()) {
            boolean related = false;
            for (IAEStack<?> output : pattern.getAEOutputs()) {
                if (!(output instanceof IAEItemStack item)) continue;
                for (ItemStack recipeOutput : recipe.mOutputs) {
                    if (recipeOutput != null
                        && new Ingredient(recipeOutput, true).equals(new Ingredient(item.getItemStack(), true)))
                        related = true;
                }
            }
            if (!related) continue;
            GTNotGood.LOG.info("[AutoCircuit] output-related recipe: {}", recipeDescription(recipe));
            if (++shown >= 8) break;
        }
        GTNotGood.LOG.info("[AutoCircuit] cached candidates={} (at most 8 related recipes shown)", candidates.size());
        return false;
    }

    /** Serializes registered stacks, preserving quantities, metadata and tags for runtime recipe diagnosis. */
    private static String recipeDescription(GTRecipe recipe) {
        if (recipe == null) return "none";
        List<NBTTagCompound> inputs = new ArrayList<>();
        List<NBTTagCompound> outputs = new ArrayList<>();
        int[] outputChances = new int[recipe.mOutputs.length];
        for (int i = 0; i < outputChances.length; i++) outputChances[i] = recipe.getOutputChance(i);
        for (ItemStack item : recipe.mInputs) if (item != null) inputs.add(item.writeToNBT(new NBTTagCompound()));
        for (ItemStack item : recipe.mOutputs) if (item != null) outputs.add(item.writeToNBT(new NBTTagCompound()));
        return "inputs=" + inputs
            + " outputs="
            + outputs
            + " fluidInputs="
            + Arrays.toString(recipe.mFluidInputs)
            + " fluidOutputs="
            + Arrays.toString(recipe.mFluidOutputs)
            + " EUt="
            + recipe.mEUt
            + " enabled="
            + recipe.mEnabled
            + " fake="
            + recipe.mFakeRecipe
            + " special="
            + recipe.mSpecialItems
            + " inputChances="
            + Arrays.toString(recipe.mInputChances)
            + " fluidInputChances="
            + Arrays.toString(recipe.mFluidInputChances)
            + " outputChances="
            + Arrays.toString(outputChances);
    }

    /** Caches full recipe comparisons; repeated busy retries do not scan the recipe registry. */
    private static List<Match> matches(RecipeMap<?> map, ICraftingPatternDetails pattern) {
        Map<ICraftingPatternDetails, List<Match>> patterns = MATCHES
            .computeIfAbsent(map, ignored -> new WeakHashMap<>());
        return patterns.computeIfAbsent(pattern, ignored -> {
            List<Match> result = new ArrayList<>();
            Map<Ingredient, Long> encoded = new HashMap<>();
            Map<Ingredient, Long> outputs = new HashMap<>();
            for (IAEStack<?> stack : pattern.getAEInputs()) {
                if (stack != null && !addAE(encoded, stack, false)) return result;
            }
            boolean hasOutput = false;
            for (IAEStack<?> stack : pattern.getAEOutputs()) {
                if (stack == null) continue;
                if (!addAE(outputs, stack, true)) return result;
                hasOutput = true;
            }
            if (!hasOutput) return result;
            boolean validEncodedRecipe = false;
            ItemStack[] encodedItems = itemInputs(encoded);
            FluidStack[] encodedFluids = fluidInputs(encoded);
            if (encodedItems == null || encodedFluids == null) return result;
            for (GTRecipe recipe : map.getAllRecipes()) {
                Match match = describe(recipe);
                if (match == null) continue;
                long operations = CircuitPatternQuantities.requestedOutputBatches(match.outputs, outputs);
                if (operations <= 0
                    || CircuitRecipeInputs.quantityBatches(recipe, encodedItems, encodedFluids) != operations) continue;
                // Retain registered variants with the same yield and quantity ratio. The CPU may supply an
                // allowed substitute, but both the encoded pattern and actual delivery must be valid GT inputs.
                result.add(match);
                if (CircuitRecipeInputs
                    .batches(recipe, match.circuit, VirtualMachineMolds.append(encodedItems, match.mold), encodedFluids)
                    == operations) validEncodedRecipe = true;
            }
            if (!validEncodedRecipe) result.clear();
            return result;
        });
    }

    /** Accepts one circuit and one catalog mold as zero-size catalysts; excludes unsupported/probabilistic recipes. */
    private static Match describe(GTRecipe recipe) {
        if (recipe == null || !recipe.mEnabled
            || recipe.mFakeRecipe
            || recipe.mSpecialItems != null
            || !CircuitRecipeInputs.deterministic(recipe)) return null;
        Match match = new Match(recipe);
        for (ItemStack item : recipe.mInputs) {
            if (item == null) continue;
            if (item.getItem() instanceof ItemIntegratedCircuit && item.stackSize == 0) {
                if (match.circuit != null && !sameCircuit(match.circuit, item)) return null;
                match.circuit = item.copy();
            } else if (item.stackSize == 0 && VirtualMachineMolds.indexOf(item) >= 0) {
                if (match.mold != null && !sameCircuit(match.mold, item)) return null;
                match.mold = item.copy();
            } else {
                if (item.stackSize <= 0) return null;
                add(match.inputs, new Ingredient(item, false), item.stackSize);
            }
        }
        for (FluidStack fluid : recipe.mFluidInputs) {
            if (fluid == null) continue;
            if (fluid.amount <= 0) return null;
            add(match.inputs, new Ingredient(fluid, false), fluid.amount);
        }
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            ItemStack item = recipe.mOutputs[i];
            if (item == null || item.stackSize <= 0) continue;
            if (recipe.getOutputChance(i) != 10000) return null;
            add(match.outputs, new Ingredient(item, true), item.stackSize);
        }
        for (FluidStack fluid : recipe.mFluidOutputs) {
            if (fluid != null && fluid.amount > 0) add(match.outputs, new Ingredient(fluid, true), fluid.amount);
        }
        return match;
    }

    private static boolean addAE(Map<Ingredient, Long> quantities, IAEStack<?> stack, boolean output) {
        if (stack.getStackSize() <= 0) return false;
        if (stack instanceof IAEItemStack item) {
            ItemStack value = item.getItemStack();
            if (!output && value.getItem() instanceof ItemIntegratedCircuit) return false;
            return add(quantities, new Ingredient(value, output), stack.getStackSize());
        }
        if (stack instanceof IAEFluidStack fluid) {
            return add(quantities, new Ingredient(fluid.getFluidStack(), output), stack.getStackSize());
        }
        return false;
    }

    private static boolean add(Map<Ingredient, Long> quantities, Ingredient ingredient, long amount) {
        long previous = quantities.getOrDefault(ingredient, 0L);
        if (amount <= 0 || previous > Long.MAX_VALUE - amount) return false;
        quantities.put(ingredient, previous + amount);
        return true;
    }

    private static boolean sameCircuit(ItemStack a, ItemStack b) {
        return a == null ? b == null : b != null && sameItem(a, b);
    }

    /**
     * Compares recipe contents for both GT lookup validation and continuous refilling. Object identity, registration
     * order, duration and EU/t are not recipe identity here; GT's lookup still enforces the machine's voltage limit.
     * Unsupported catalysts, special-slot recipes and probabilistic outputs remain excluded by {@link #describe}.
     *
     * @param left  previously selected or delivered recipe; null means there is no known match
     * @param right recipe being checked against it
     * @return true only for identical outputs, circuits and molds with mutually accepted input definitions
     */
    private static boolean sameRecipe(Match left, Match right) {
        return left != null && right != null
            && CircuitPatternQuantities.sameRecipe(
                left.outputs,
                right.outputs,
                sameCircuit(left.circuit, right.circuit) && sameCircuit(left.mold, right.mold))
            && matchingInputs(left, right.inputs) == 1
            && matchingInputs(right, left.inputs) == 1;
    }

    /** Uses copied resource stacks so GT can test alternative ingredients without changing inventories. */
    private static long matchingInputs(Match match, Map<Ingredient, Long> inputs) {
        ItemStack[] items = itemInputs(inputs);
        FluidStack[] fluids = fluidInputs(inputs);
        return items == null || fluids == null ? 0
            : CircuitRecipeInputs
                .batches(match.recipe, match.circuit, VirtualMachineMolds.append(items, match.mold), fluids);
    }

    private static ItemStack[] itemInputs(Map<Ingredient, Long> inputs) {
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<Ingredient, Long> entry : inputs.entrySet()) {
            Ingredient key = entry.getKey();
            if (!(key.type instanceof Item item)) continue;
            long count = entry.getValue();
            if (count <= 0 || count > Integer.MAX_VALUE) return null;
            ItemStack stack = new ItemStack(item, (int) count, key.damage);
            if (key.tag != null) stack.setTagCompound((NBTTagCompound) key.tag.copy());
            result.add(stack);
        }
        return result.toArray(new ItemStack[0]);
    }

    private static FluidStack[] fluidInputs(Map<Ingredient, Long> inputs) {
        List<FluidStack> result = new ArrayList<>();
        for (Map.Entry<Ingredient, Long> entry : inputs.entrySet()) {
            Ingredient key = entry.getKey();
            if (!(key.type instanceof Fluid fluid)) continue;
            long amount = entry.getValue();
            if (amount <= 0 || amount > Integer.MAX_VALUE) return null;
            result.add(new FluidStack(fluid, (int) amount, key.tag == null ? null : (NBTTagCompound) key.tag.copy()));
        }
        return result.toArray(new FluidStack[0]);
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
            && ItemStack.areItemStackTagsEqual(a, b);
    }

    /** A recipe's consumed quantities and optional non-consumed circuit and mold configuration. */
    private static final class Match {

        private final GTRecipe recipe;
        private final Map<Ingredient, Long> inputs = new HashMap<>();
        private final Map<Ingredient, Long> outputs = new HashMap<>();
        private ItemStack circuit;
        private ItemStack mold;

        private Match(GTRecipe recipe) {
            this.recipe = recipe;
        }
    }

    /** Quantity-independent, NBT-sensitive identity with separate namespaces for inputs and outputs. */
    private static final class Ingredient {

        private final Object type;
        private final int damage;
        private final NBTTagCompound tag;
        private final boolean output;

        private Ingredient(ItemStack item, boolean output) {
            ItemStack unified = GTOreDictUnificator.get(item);
            this.type = unified.getItem();
            this.damage = unified.getItemDamage();
            this.tag = unified.hasTagCompound() ? (NBTTagCompound) unified.getTagCompound()
                .copy() : null;
            this.output = output;
        }

        private Ingredient(FluidStack fluid, boolean output) {
            this.type = fluid.getFluid();
            this.damage = 0;
            this.tag = fluid.tag == null ? null : (NBTTagCompound) fluid.tag.copy();
            this.output = output;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Ingredient ingredient && type == ingredient.type
                && damage == ingredient.damage
                && output == ingredient.output
                && Objects.equals(tag, ingredient.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, damage, tag, output);
        }
    }
}
