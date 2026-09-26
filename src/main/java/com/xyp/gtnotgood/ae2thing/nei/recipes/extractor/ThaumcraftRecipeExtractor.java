package com.xyp.gtnotgood.ae2thing.nei.recipes.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

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

    /** Returns whether the recipe needs a TC workbench layout in addition to its AE processing bill. */
    public static boolean isArcane(String identifier) {
        return "thaumcraft.arcane.shaped".equals(identifier) || "thaumcraft.arcane.shapeless".equals(identifier)
            || "thaumcraft.wands".equals(identifier);
    }

    /**
     * Converts ARI's workbench coordinates (47/75/103, 38/65/92) into the provider's nine-cell layout.
     * This runs before processing-input compression, retaining holes, repeated ingredients and selected NBT.
     *
     * @param ingredients displayed NEI ingredients
     * @return the nine vanilla ItemStack tags consumed by the arcane packaged core
     */
    public static NBTTagList arcaneLayout(List<PositionedStack> ingredients) {
        NBTTagCompound[] cells = new NBTTagCompound[9];
        for (PositionedStack ingredient : ingredients) {
            if (ingredient == null || ingredient.item == null || isAspectDisplay(ingredient.item)) continue;
            int x = ingredient.relx - 47;
            int y = ingredient.rely - 38;
            if (x < 0 || y < 0 || x % 28 != 0 || y % 27 != 0 || x / 28 > 2 || y / 27 > 2) {
                throw new IllegalArgumentException("Invalid arcane recipe position");
            }
            int slot = x / 28 + y / 27 * 3;
            if (cells[slot] != null) throw new IllegalArgumentException("Duplicate arcane recipe position");
            ItemStack item = ingredient.item.copy();
            item.stackSize = 1;
            cells[slot] = item.writeToNBT(new NBTTagCompound());
        }
        NBTTagList layout = new NBTTagList();
        for (NBTTagCompound cell : cells) layout.appendTag(cell == null ? new NBTTagCompound() : cell);
        return layout;
    }

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
