package com.xyp.gtnotgood.common.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.GregTechAPI;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.items.ItemIntegratedCircuit;

/** Resolves encoded catalog molds from actual GT recipes, preserving molds used as consumable ingredients. */
public final class CircuitPatternMolds {

    private static final Map<ICraftingPatternDetails, ItemStack[]> cache = new WeakHashMap<>();
    private static int cachedRecipeCount = -1;
    private static int cachedMapCount = -1;

    private CircuitPatternMolds() {}

    /**
     * Only molds required with zero quantity by every matching recipe become virtual tools. Complete consumed
     * inputs must match the advertised output yield; omitted byproducts and integer batches are supported.
     * Recipe definitions are assumed stable after startup, except additions/removals invalidate the cache.
     *
     * @param details original processing pattern, including any encoded circuit, molds and native fluids
     * @return copied zero-size molds; ambiguous, unsupported or unrecognized patterns retain their real inputs
     */
    public static synchronized ItemStack[] resolve(ICraftingPatternDetails details) {
        if (details == null || details.isCraftable() || details.isInputOnly()) return new ItemStack[0];
        if (!GregTechAPI.sFullLoadFinished) return findMolds(details);
        int recipeCount = 0;
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            if (!map.getBackend().doesOverwriteFindRecipe()) recipeCount += map.getAllRecipes().size();
        }
        if (cachedRecipeCount != recipeCount || cachedMapCount != RecipeMap.ALL_RECIPE_MAPS.size()) {
            cache.clear();
            cachedRecipeCount = recipeCount;
            cachedMapCount = RecipeMap.ALL_RECIPE_MAPS.size();
        }
        return copy(cache.computeIfAbsent(details, CircuitPatternMolds::findMolds));
    }

    private static ItemStack[] findMolds(ICraftingPatternDetails details) {
        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        List<ItemStack> encodedMolds = new ArrayList<>();
        for (IAEStack<?> stack : details.getAEInputs()) {
            if (stack == null) continue;
            if (stack.getStackSize() < 0 || stack.getStackSize() > Integer.MAX_VALUE) return new ItemStack[0];
            if (stack instanceof IAEItemStack item) {
                ItemStack value = item.getItemStack().copy();
                value.stackSize = (int) stack.getStackSize();
                // GT's integrated-circuit helper returns a zero-size catalyst, also accepted by native AE patterns.
                if (value.getItem() instanceof ItemIntegratedCircuit) value.stackSize = 1;
                else if (value.stackSize == 0) return new ItemStack[0];
                items.add(value);
                int mold = VirtualMachineMolds.indexOf(value);
                if (mold >= 0 && encodedMolds.stream().noneMatch(existing -> sameItem(existing, value))) {
                    encodedMolds.add(VirtualMachineMolds.at(mold));
                }
            } else if (stack instanceof IAEFluidStack fluid) {
                if (stack.getStackSize() == 0) return new ItemStack[0];
                FluidStack value = fluid.getFluidStack().copy();
                value.amount = (int) stack.getStackSize();
                fluids.add(value);
            } else return new ItemStack[0];
        }
        if (encodedMolds.isEmpty()) return new ItemStack[0];
        Map<Ingredient, Long> outputs = new HashMap<>();
        for (IAEStack<?> stack : details.getAEOutputs()) {
            if (stack == null) continue;
            if (stack.getStackSize() <= 0) return new ItemStack[0];
            Ingredient key;
            if (stack instanceof IAEItemStack item) key = new Ingredient(item.getItemStack());
            else if (stack instanceof IAEFluidStack fluid) key = new Ingredient(fluid.getFluidStack());
            else return new ItemStack[0];
            if (!add(outputs, key, stack.getStackSize())) return new ItemStack[0];
        }
        List<ItemStack> commonMolds = null;
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            // Dynamic backends can depend on machine state or perform diagnostic side effects during lookup.
            if (map.getBackend().doesOverwriteFindRecipe()) continue;
            ItemStack[] queryItems = copy(items.toArray(new ItemStack[0]));
            FluidStack[] queryFluids = fluids.stream().map(FluidStack::copy).toArray(FluidStack[]::new);
            try (Stream<GTRecipe> candidates = map.findRecipeQuery().items(queryItems).fluids(queryFluids).findAll()) {
                Iterator<GTRecipe> iterator = candidates.iterator();
                while (iterator.hasNext()) {
                    GTRecipe recipe = iterator.next();
                    long batches = outputBatches(recipe, outputs);
                    if (batches <= 0) continue;
                    List<ItemStack> nonConsumed = new ArrayList<>();
                    for (ItemStack mold : encodedMolds) {
                        boolean required = false;
                        boolean consumed = false;
                        for (ItemStack input : recipe.mInputs) {
                            if (input == null || !sameItem(input, mold)) continue;
                            if (input.stackSize == 0) required = true;
                            else if (input.stackSize > 0) consumed = true;
                        }
                        if (required && !consumed) nonConsumed.add(mold);
                    }
                    ItemStack[] supplied = copy(items.toArray(new ItemStack[0]));
                    for (ItemStack item : supplied) {
                        if (
                            item.getItem() instanceof ItemIntegratedCircuit
                                || nonConsumed.stream().anyMatch(mold -> sameItem(item, mold))
                        ) item.stackSize = 0;
                    }
                    if (CircuitRecipeInputs.batches(recipe, null, supplied, queryFluids) != batches) continue;
                    if (commonMolds == null) commonMolds = new ArrayList<>(nonConsumed);
                    else commonMolds.removeIf(mold -> nonConsumed.stream().noneMatch(other -> sameItem(mold, other)));
                    if (commonMolds.isEmpty()) return new ItemStack[0];
                }
            } catch (RuntimeException unsupportedMap) {
                // Custom backends may require machine state; keep all encoded molds physical in that case.
                return new ItemStack[0];
            }
        }
        return commonMolds == null ? new ItemStack[0] : copy(commonMolds.toArray(new ItemStack[0]));
    }

    private static long outputBatches(GTRecipe recipe, Map<Ingredient, Long> encoded) {
        if (recipe == null || !recipe.mEnabled || recipe.mFakeRecipe || !CircuitRecipeInputs.deterministic(recipe))
            return 0;
        Map<Ingredient, Long> outputs = new HashMap<>();
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            ItemStack item = recipe.mOutputs[i];
            if (
                item != null && item.stackSize > 0
                    && recipe.getOutputChance(i) == 10000
                    && !add(outputs, new Ingredient(item), item.stackSize)
            ) return 0;
        }
        for (int i = 0; i < recipe.mFluidOutputs.length; i++) {
            FluidStack fluid = recipe.mFluidOutputs[i];
            if (
                recipe.mFluidOutputChances != null && i < recipe.mFluidOutputChances.length
                    && recipe.mFluidOutputChances[i] != 10000
            ) continue;
            if (fluid != null && fluid.amount > 0 && !add(outputs, new Ingredient(fluid), fluid.amount)) return 0;
        }
        return CircuitPatternQuantities.requestedOutputBatches(outputs, encoded);
    }

    private static boolean add(Map<Ingredient, Long> quantities, Ingredient key, long amount) {
        long previous = quantities.getOrDefault(key, 0L);
        if (amount > Long.MAX_VALUE - previous) return false;
        quantities.put(key, previous + amount);
        return true;
    }

    private static boolean sameItem(ItemStack left, ItemStack right) {
        return GTUtility.areStacksEqual(GTOreDictUnificator.get(left), GTOreDictUnificator.get(right), false);
    }

    private static ItemStack[] copy(ItemStack[] items) {
        ItemStack[] copies = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) copies[i] = items[i].copy();
        return copies;
    }

    /** Quantity-independent item/fluid identity retains metadata and native fluid NBT. */
    private static final class Ingredient {

        private final Object type;
        private final int damage;
        private final NBTTagCompound tag;

        private Ingredient(ItemStack item) {
            ItemStack unified = GTOreDictUnificator.get(item);
            type = unified.getItem();
            damage = unified.getItemDamage();
            tag = unified.hasTagCompound() ? (NBTTagCompound) unified.getTagCompound().copy() : null;
        }

        private Ingredient(FluidStack fluid) {
            type = fluid.getFluid();
            damage = 0;
            tag = fluid.tag == null ? null : (NBTTagCompound) fluid.tag.copy();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Ingredient ingredient && type == ingredient.type
                && damage == ingredient.damage
                && Objects.equals(tag, ingredient.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, damage, tag);
        }
    }
}
