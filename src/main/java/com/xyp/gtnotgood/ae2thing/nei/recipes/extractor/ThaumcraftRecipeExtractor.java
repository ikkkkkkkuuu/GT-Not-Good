package com.xyp.gtnotgood.ae2thing.nei.recipes.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.ae2thing.nei.object.IRecipeExtractor;
import com.xyp.gtnotgood.ae2thing.nei.object.OrderStack;
import com.xyp.gtnotgood.ae2thing.nei.recipes.FluidRecipe;

import codechicken.nei.PositionedStack;

/**
 * Imports only physical items from Aspect Recipe Index recipes as processing patterns. Alchemy and infusion
 * expose essentia among their ingredients; arcane handlers expose wand Vis in other stacks. Neither belongs
 * in these patterns: the target machine or altar supplies its own magical resources.
 */
public final class ThaumcraftRecipeExtractor implements IRecipeExtractor {

    /** Registers overlay identifiers without loading the optional Aspect Recipe Index classes. */
    public static void register() {
        ThaumcraftRecipeExtractor extractor = new ThaumcraftRecipeExtractor();
        FluidRecipe.addRecipeMap("thaumcraft.arcane.shaped", extractor);
        FluidRecipe.addRecipeMap("thaumcraft.arcane.shapeless", extractor);
        FluidRecipe.addRecipeMap("thaumcraft.wands", extractor);
        FluidRecipe.addRecipeMap("thaumcraft.alchemy", extractor);
        FluidRecipe.addRecipeMap("thaumcraft.infusion", extractor);
    }

    @Override
    public List<OrderStack<?>> getInputIngredients(List<PositionedStack> rawInputs) {
        List<OrderStack<?>> result = new ArrayList<>();
        for (PositionedStack ingredient : rawInputs) {
            if (ingredient == null || ingredient.item == null || isAspectDisplay(ingredient.item)) continue;
            // Copy the displayed permutation, including its NBT and count, before optional input compression.
            result.add(new OrderStack<>(ingredient.item.copy(), result.size(), ingredient.items));
        }
        return result;
    }

    @Override
    public List<OrderStack<?>> getOutputIngredients(List<PositionedStack> rawOutputs) {
        // FluidRecipe always places getResultStack first; later entries are informational other stacks.
        if (rawOutputs.isEmpty() || rawOutputs.get(0) == null
            || rawOutputs.get(0).item == null
            || isAspectDisplay(rawOutputs.get(0).item)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new OrderStack<>(rawOutputs.get(0).item.copy(), 0));
    }

    /**
     * Recognizes synthetic Vis/essentia icons without linking either optional NEI plugin. Check the class
     * hierarchy so subclasses are covered, while real jars, phials and crystals remain valid ingredients.
     *
     * @param stack displayed recipe stack
     * @return whether the stack represents an aspect rather than a physical ingredient
     */
    private static boolean isAspectDisplay(ItemStack stack) {
        for (Class<?> type = stack.getItem()
            .getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if ("com.gtnewhorizons.aspectrecipeindex.common.items.ItemAspect".equals(name)
                || "com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect".equals(name)) return true;
        }
        return false;
    }
}
