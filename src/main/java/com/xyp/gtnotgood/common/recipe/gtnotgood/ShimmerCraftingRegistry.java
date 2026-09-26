/* Adapted from GT-Not-Leisure, LGPL-3.0; see META-INF/shimmer-port/NOTICE.md. */
package com.xyp.gtnotgood.common.recipe.gtnotgood;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.GTNotGood;

import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.ItemMachines;

/** Captures only GT machine crafting, matching Shimmer's constructor hooks. */
public final class ShimmerCraftingRegistry {

    private static final List<Entry> RECIPES = new ArrayList<>();

    private ShimmerCraftingRegistry() {}

    /** Captures original ore names before Forge expands them; crafting tools are not recovered. */
    public static void capture(ItemStack output, Object[] inputs, boolean shaped) {
        if (output == null || !(output.getItem() instanceof ItemMachines)) return;
        Object[] copy = Arrays.stream(inputs)
            .filter(value -> !(value instanceof String name && name.startsWith("craftingTool")))
            .map(value -> value instanceof ItemStack stack ? stack.copy() : value)
            .toArray();
        RECIPES.add(new Entry(output.copy(), copy, shaped));
    }

    static void registerAll() {
        for (Entry entry : RECIPES) {
            try {
                GTRecipe reversed = (entry.shaped ? GTUtility.reverseShapedRecipe(entry.output, entry.inputs)
                    : GTUtility.reverseShapelessRecipe(entry.output, entry.inputs)).orElse(null);
                if (reversed == null || reversed.mInputs == null
                    || !ShimmerRecoveryRules.shouldDisassemble(reversed.mInputs)
                    || TransmutationRecipes.contains(reversed.mInputs[0])) continue;
                TransmutationRecipes.register(
                    reversed.mInputs[0],
                    ShimmerRecoveryRules.handleRecipeTransformation(reversed.mOutputs, null),
                    null);
            } catch (IllegalStateException e) {
                GTNotGood.LOG.warn("Skipping invalid reversed GT crafting recipe", e);
            }
        }
    }

    static void clear() {
        RECIPES.clear();
    }

    /** Immutable snapshot of a constructor's original crafting arguments. */
    private static final class Entry {

        private final ItemStack output;
        private final Object[] inputs;
        private final boolean shaped;

        private Entry(ItemStack output, Object[] inputs, boolean shaped) {
            this.output = output;
            this.inputs = inputs;
            this.shaped = shaped;
        }
    }
}
