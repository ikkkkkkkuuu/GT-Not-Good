/* Adapted from GT-Not-Leisure, LGPL-3.0; see META-INF/shimmer-port/NOTICE.md. */
package com.xyp.gtnotgood.common.recipe.gtnotgood;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.enums.TierEU;
import gregtech.api.objects.GTItemStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gtnhintergalactic.recipe.IGRecipeMaps;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/** Shimmer recovery rules adapted to powered processing and native fluid output hatches. */
public final class TransmutationRecipes {

    private static final Map<Item, List<ItemStack>> REGISTERED = new HashMap<>();
    private static boolean loaded;

    private TransmutationRecipes() {}

    /** Runs after recipe registration; an installed GTNL supplies its actual conversion table. */
    public static void load() {
        if (loaded) return;
        loaded = true;
        if (!importShimmer()) {
            Map<GTItemStack, List<GTRecipe>> groups = new LinkedHashMap<>();
            for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (recipe.mInputs == null || recipe.mOutputs == null
                    || !ShimmerRecoveryRules.shouldDisassemble(recipe.mOutputs)) continue;
                groups.computeIfAbsent(new GTItemStack(recipe.mOutputs[0]), key -> new ArrayList<>())
                    .add(recipe);
            }
            for (List<GTRecipe> recipes : groups.values()) {
                GTRecipe first = recipes.get(0);
                ObjectOpenHashSet<ItemStack[]> alternatives = new ObjectOpenHashSet<>();
                for (GTRecipe recipe : recipes) alternatives.add(copy(recipe.mInputs));
                register(
                    first.mOutputs[0],
                    ShimmerRecoveryRules.handleRecipeTransformation(copy(first.mInputs), alternatives),
                    first.mFluidInputs);
            }
            collectDirect(RecipeMaps.assemblylineVisualRecipes.getAllRecipes());
            collectDirect(IGRecipeMaps.spaceAssemblerRecipes.getAllRecipes());
            ShimmerCraftingRegistry.registerAll();
        }
        ShimmerCraftingRegistry.clear();
        GTNotGood.LOG.info(
            "Registered {} Shimmer-compatible transmutation recipes",
            GTNGRecipeMaps.TransmutationRecipes.getAllRecipes()
                .size());
    }

    /** Optional reflection keeps GTNL absent-safe while honoring all its hard overrides and configuration. */
    private static boolean importShimmer() {
        if (!ModList.GTNotLeisure.isModLoaded()) return false;
        try {
            Class<?> owner = Class.forName("com.science.gtnl.common.recipe.gtnl.ShimmerRecipes");
            Map<?, ?> conversions = (Map<?, ?>) owner.getField("conversionMap")
                .get(null);
            for (Object entries : conversions.values()) for (Object entry : (Iterable<?>) entries) {
                ItemStack input = (ItemStack) entry.getClass()
                    .getMethod("input")
                    .invoke(entry);
                List<ItemStack> items = new ArrayList<>();
                List<FluidStack> fluids = new ArrayList<>();
                for (Object raw : (Iterable<?>) entry.getClass()
                    .getMethod("outputs")
                    .invoke(entry)) {
                    ItemStack stack = (ItemStack) raw;
                    if (stack == null || stack.stackSize <= 0) continue;
                    if (stack.getItem() instanceof ItemFluidPacket) {
                        FluidStack fluid = ItemFluidPacket.getFluidStack(stack);
                        if (fluid != null) {
                            fluid = fluid.copy();
                            fluid.amount = Math.toIntExact((long) fluid.amount * stack.stackSize);
                            fluids.add(fluid);
                        }
                    } else items.add(stack);
                }
                register(input, items, fluids.toArray(new FluidStack[0]));
            }
            return true;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read installed GT Not Leisure Shimmer conversions", e);
        }
    }

    private static void collectDirect(Iterable<GTRecipe> recipes) {
        for (GTRecipe recipe : recipes) {
            if (recipe.mOutputs == null || recipe.mOutputs.length == 0 || recipe.mInputs == null) continue;
            if (!contains(recipe.mOutputs[0]))
                register(recipe.mOutputs[0], Arrays.asList(recipe.mInputs), recipe.mFluidInputs);
        }
    }

    /** Preserves upstream first-conversion priority, including its batch-size comparison. */
    static boolean contains(ItemStack input) {
        if (input == null) return false;
        for (ItemStack existing : REGISTERED.getOrDefault(input.getItem(), Collections.emptyList()))
            if (input.stackSize >= existing.stackSize && GTUtility.areStacksEqual(input, existing, true)) return true;
        return false;
    }

    static void register(ItemStack input, List<ItemStack> outputs, FluidStack[] fluidOutputs) {
        if (input == null || input.stackSize <= 0) return;
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : outputs)
            if (GTUtility.isStackValid(stack) && stack.stackSize > 0) items.add(stack.copy());
        List<FluidStack> fluids = new ArrayList<>();
        if (fluidOutputs != null) for (FluidStack fluid : fluidOutputs)
            if (fluid != null && fluid.getFluid() != null && fluid.amount > 0) fluids.add(fluid.copy());
        if (items.isEmpty() && fluids.isEmpty()) return;
        GTRecipeBuilder.builder()
            .itemInputs(input.copy())
            .itemOutputs(items.toArray(new ItemStack[0]))
            .fluidOutputs(fluids.toArray(new FluidStack[0]))
            .duration(100)
            .eut(TierEU.RECIPE_EV)
            .nbtSensitive()
            .addTo(GTNGRecipeMaps.TransmutationRecipes);
        REGISTERED.computeIfAbsent(input.getItem(), key -> new ArrayList<>())
            .add(input.copy());
    }

    private static ItemStack[] copy(ItemStack[] source) {
        return Arrays.stream(source)
            .map(stack -> stack == null ? null : stack.copy())
            .toArray(ItemStack[]::new);
    }
}
