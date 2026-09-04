package com.xyp.gtnotgood.common.recipe.gtnotgood;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.google.common.collect.Sets;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

/**
 * Generates the recipe entries consumed by the Large Ore Processor recipe map.
 * <p>
 * Recipes are generated from GregTech's material registry instead of being listed by hand. That keeps the machine in
 * sync with generated ores, raw ores, crushed forms, impure dusts, pure dusts, and gem-bearing materials available in
 * the current GTNH environment.
 */
public class OreProcessingRecipes {

    /**
     * Alias to the actual GregTech recipe map used by this loader.
     * <p>
     * The field name intentionally mirrors the class name from GT-Not-Cool's style, so migrated recipe code can call
     * {@code OreProcessingRecipes.addTo(...)} style helpers without searching for a different registry location.
     */
    public static final RecipeMap<?> OreProcessingRecipes = GTNGRecipeMaps.OreProcessingRecipes;

    private static final int EUT = 0;
    private static final int DURATION_TICKS = 20;

    public static final Set<OrePrefixes> BASIC_STONE_TYPES = Sets.newHashSet(
        OrePrefixes.ore,
        OrePrefixes.oreBasalt,
        OrePrefixes.oreBlackgranite,
        OrePrefixes.oreRedgranite,
        OrePrefixes.oreMarble,
        OrePrefixes.oreNetherrack,
        OrePrefixes.oreEndstone);

    public static final Set<OrePrefixes> BASIC_STONE_TYPES_EXCEPT_NORMAL = Sets.newHashSet(
        OrePrefixes.oreBasalt,
        OrePrefixes.oreBlackgranite,
        OrePrefixes.oreRedgranite,
        OrePrefixes.oreMarble,
        OrePrefixes.oreNetherrack,
        OrePrefixes.oreEndstone);

    /**
     * Registers every Large Ore Processor recipe.
     * <p>
     * This method should be called during normal Forge initialization after machine registration, because the recipe
     * map's NEI display stack depends on {@link com.xyp.gtnotgood.utils.enums.GTNGItemList#LargeOreProcessor}.
     */
    public static void loadOreProcessingRecipes() {
        processGTMaterials();
        processIntermediateProducts();
        processOtherModOre(new ItemStack(Blocks.iron_ore), Materials.Iron, false);

        GTNotGood.LOG.info("Loaded large ore processor recipes");
    }

    /**
     * Scans GregTech's generated material list and creates base ore recipes for each material with a normal ore form.
     *
     * @see GregTechAPI#sGeneratedMaterials
     */
    private static void processGTMaterials() {
        for (Materials material : GregTechAPI.sGeneratedMaterials) {
            if (material == null) continue;
            processGTMaterialOre(material);
        }
    }

    /**
     * Creates ore, raw ore, and alternate-stone ore recipes for one material.
     * <p>
     * Materials without a generated normal ore are skipped, because they usually represent fluids, chemicals, or
     * special
     * materials that should not receive automatic ore-processing recipes.
     *
     * @param material GregTech material being converted to ore-processing recipes
     */
    private static void processGTMaterialOre(Materials material) {
        if (GTOreDictUnificator.get(OrePrefixes.ore, material, 1) == null) return;

        ItemStack[] normalOutputs = getOutputs(material, false);
        ItemStack[] richOutputs = getOutputs(material, true);

        addRecipe(GTOreDictUnificator.get(OrePrefixes.ore, material, 1), normalOutputs);
        addRecipe(GTOreDictUnificator.get(OrePrefixes.rawOre, material, 1), normalOutputs);

        for (OrePrefixes prefix : BASIC_STONE_TYPES_EXCEPT_NORMAL) {
            ItemStack ore = GTOreDictUnificator.get(prefix, material, 1);
            if (ore != null) {
                addRecipe(ore, isRichOre(prefix) ? richOutputs : normalOutputs);
            }
        }
    }

    /**
     * Calculates output stacks for a material's ore-processing recipe.
     * <p>
     * Byproduct rules intentionally follow the simplified port from GT-Not-Cool: materials with byproducts receive main
     * dust plus byproduct dusts, while materials without byproducts receive a larger amount of main dust. Rich ores
     * multiply every non-null output stack.
     *
     * @param material GregTech material being processed
     * @param isRich   true to double all generated outputs for rich ore variants
     * @return output stacks to register on the custom recipe map
     */
    private static ItemStack[] getOutputs(Materials material, boolean isRich) {
        List<ItemStack> outputs = new ArrayList<>();

        if (material.mOreByProducts != null && !material.mOreByProducts.isEmpty()) {
            outputs.add(getDustStack(material, 4));

            if (material.mOreByProducts.size() == 1) {
                for (Materials byproduct : material.mOreByProducts) {
                    if (byproduct != null) {
                        outputs.add(getDustStack(byproduct, 3));
                    }
                }
            } else {
                for (Materials byproduct : material.mOreByProducts) {
                    if (byproduct == null || byproduct == Materials.Netherrack
                        || byproduct == Materials.Endstone
                        || byproduct == Materials.Stone) continue;
                    outputs.add(getDustStack(byproduct, 2));
                }
            }
        } else {
            outputs.add(getDustStack(material, 8));
        }

        addGemOutputs(outputs, material);

        if (isRich) {
            for (ItemStack stack : outputs) {
                if (stack != null) stack.stackSize *= 2;
            }
        }

        return outputs.toArray(new ItemStack[0]);
    }

    /**
     * Adds gem-related bonus outputs for gem materials.
     * <p>
     * Exquisite and flawless gems are added only when the material exposes those ore-dictionary forms. The normal gem
     * is
     * always added when a gem form exists.
     *
     * @param outputs  mutable output list being built
     * @param material GregTech material being processed
     */
    private static void addGemOutputs(List<ItemStack> outputs, Materials material) {
        ItemStack gem = GTOreDictUnificator.get(OrePrefixes.gem, material, 1);
        if (gem == null) return;

        ItemStack gemExquisite = GTOreDictUnificator.get(OrePrefixes.gemExquisite, material, 1);
        if (gemExquisite != null) {
            outputs.add(gemExquisite);
            outputs.add(GTOreDictUnificator.get(OrePrefixes.gemFlawless, material, 2));
        }
        outputs.add(gem);
    }

    /**
     * Returns a copied dust stack for the requested material and amount.
     *
     * @param material material whose dust should be looked up
     * @param amount   amount to copy into the returned stack
     * @return copied dust stack, or {@code null} when the material has no dust form
     */
    private static ItemStack getDustStack(Materials material, int amount) {
        ItemStack dust = GTOreDictUnificator.get(OrePrefixes.dust, material, 1);
        if (dust == null) return null;
        return GTUtility.copyAmountUnsafe(amount, dust);
    }

    /**
     * Registers recipes for intermediate ore-processing products.
     * <p>
     * This lets the Large Ore Processor accept crushed ore, purified crushed ore, centrifuged crushed ore, impure dust,
     * and pure dust directly instead of only accepting raw ore blocks.
     */
    private static void processIntermediateProducts() {
        for (Materials material : GregTechAPI.sGeneratedMaterials) {
            if (material == null) continue;

            processIntermediateForm(material, OrePrefixes.crushed, input -> getOutputs(input, false));
            processIntermediateForm(material, OrePrefixes.crushedPurified, input -> getOutputs(input, false));
            processIntermediateForm(material, OrePrefixes.crushedCentrifuged, input -> getOutputs(input, false));
            processIntermediateForm(
                material,
                OrePrefixes.dustImpure,
                input -> new ItemStack[] { getDustStack(input, 6) });
            processIntermediateForm(
                material,
                OrePrefixes.dustPure,
                input -> new ItemStack[] { getDustStack(input, 7) });
        }
    }

    /**
     * Registers one intermediate-form recipe when the ore dictionary contains that form.
     *
     * @param material       material being scanned
     * @param prefix         ore prefix for the intermediate input form
     * @param outputProvider function that produces outputs for the material
     */
    private static void processIntermediateForm(Materials material, OrePrefixes prefix,
        Function<Materials, ItemStack[]> outputProvider) {
        ItemStack input = GTOreDictUnificator.get(prefix, material, 1);
        if (input != null) {
            addRecipe(input, outputProvider.apply(material));
        }
    }

    /**
     * Registers a manually supplied ore stack for a GregTech material.
     * <p>
     * Use this for vanilla or third-party ore blocks that are not covered by GregTech's generated ore prefixes but
     * should still feed into the same output logic.
     *
     * @param ore      input ore stack
     * @param material material whose outputs should be generated
     * @param isRich   true to double the generated outputs
     */
    private static void processOtherModOre(ItemStack ore, Materials material, boolean isRich) {
        if (ore != null) {
            addRecipe(ore, getOutputs(material, isRich));
        }
    }

    /**
     * Adds one recipe to the custom recipe map after filtering null outputs.
     * <p>
     * Generated material lookups can legally return {@code null} for unsupported forms. Filtering here keeps recipe
     * generation robust and avoids registering recipes with empty output arrays.
     *
     * @param input   input stack for the recipe
     * @param outputs candidate output stacks, some of which may be null
     */
    private static void addRecipe(ItemStack input, ItemStack... outputs) {
        if (input == null || outputs == null) return;

        List<ItemStack> nonNullOutputs = new ArrayList<>();
        for (ItemStack output : outputs) {
            if (output != null) {
                nonNullOutputs.add(output);
            }
        }
        if (nonNullOutputs.isEmpty()) return;

        GTRecipeBuilder.builder()
            .itemInputs(input)
            .itemOutputs(nonNullOutputs.toArray(new ItemStack[0]))
            .eut(EUT)
            .duration(DURATION_TICKS)
            .addTo(OreProcessingRecipes);
    }

    /**
     * Checks whether an alternate stone prefix should be treated as rich ore.
     *
     * @param prefix ore prefix being registered
     * @return true for Nether and End ore variants, false otherwise
     */
    private static boolean isRichOre(OrePrefixes prefix) {
        return prefix == OrePrefixes.oreNetherrack || prefix == OrePrefixes.oreEndstone;
    }
}
