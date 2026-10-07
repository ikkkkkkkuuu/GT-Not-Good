package com.xyp.gtnotgood.common.recipe.gtnotgood;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.common.collect.Sets;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
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
    private static final OrePrefixes[] byproductStages = { OrePrefixes.crushed, OrePrefixes.crushedPurified,
        OrePrefixes.crushedCentrifuged, OrePrefixes.dustImpure, OrePrefixes.dustPure };
    private static final OrePrefixes[] inputForms = { OrePrefixes.crushedPurified, OrePrefixes.crushedCentrifuged,
        OrePrefixes.crushed, OrePrefixes.dustImpure, OrePrefixes.dustPure, OrePrefixes.rawOre,
        OrePrefixes.oreNetherrack, OrePrefixes.oreEndstone, OrePrefixes.oreBasalt, OrePrefixes.oreBlackgranite,
        OrePrefixes.oreRedgranite, OrePrefixes.oreMarble, OrePrefixes.ore };

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

    /** Registers external ores and imports native mechanical byproducts after recipe registration completes. */
    public static void loadExternalOreRecipes() {
        for (String oreName : OreDictionary.getOreNames()) {
            int dustAmount;
            String materialName;
            if (oreName.startsWith("oreNetherrack")) {
                materialName = oreName.substring("oreNetherrack".length());
                dustAmount = 16;
            } else if (oreName.startsWith("oreEndstone")) {
                materialName = oreName.substring("oreEndstone".length());
                dustAmount = 16;
            } else if (oreName.startsWith("rawOre")) {
                materialName = oreName.substring("rawOre".length());
                dustAmount = 8;
            } else if (oreName.startsWith("oreBasalt")) {
                materialName = oreName.substring("oreBasalt".length());
                dustAmount = 8;
            } else if (oreName.startsWith("oreBlackgranite")) {
                materialName = oreName.substring("oreBlackgranite".length());
                dustAmount = 8;
            } else if (oreName.startsWith("oreRedgranite")) {
                materialName = oreName.substring("oreRedgranite".length());
                dustAmount = 8;
            } else if (oreName.startsWith("oreMarble")) {
                materialName = oreName.substring("oreMarble".length());
                dustAmount = 8;
            } else if (oreName.startsWith("ore")) {
                materialName = oreName.substring("ore".length());
                dustAmount = 8;
            } else if (oreName.startsWith("crushedPurified")) {
                materialName = oreName.substring("crushedPurified".length());
                dustAmount = 8;
            } else if (oreName.startsWith("crushedCentrifuged")) {
                materialName = oreName.substring("crushedCentrifuged".length());
                dustAmount = 8;
            } else if (oreName.startsWith("crushed")) {
                materialName = oreName.substring("crushed".length());
                dustAmount = 8;
            } else if (oreName.startsWith("dustImpure")) {
                materialName = oreName.substring("dustImpure".length());
                dustAmount = 6;
            } else if (oreName.startsWith("dustPure")) {
                materialName = oreName.substring("dustPure".length());
                dustAmount = 7;
            } else {
                continue;
            }
            if (materialName.isEmpty()) continue;
            List<ItemStack> dusts = OreDictionary.getOres("dust" + materialName);
            if (dusts.isEmpty()) continue;
            ItemStack dust = dusts.get(0);
            if (dust == null || dust.getItem() == null) continue;

            for (ItemStack input : OreDictionary.getOres(oreName)) {
                if (input == null || input.getItem() == null) continue;
                ItemStack singleInput = GTUtility.copyAmountUnsafe(1, input);
                if (OreProcessingRecipes.findRecipeQuery()
                    .items(singleInput)
                    .notUnificated(true)
                    .find() != null) continue;
                addRecipe(singleInput, GTUtility.copyAmountUnsafe(dustAmount, dust));
            }
        }
        addSiftingOutputs();
        addMechanicalByproducts();
    }

    /**
     * Adds missing native grinding and centrifuging products without combining alternative processing routes.
     * Existing yields, including sifting products, take precedence. For each new item, the native slot group with the
     * highest expected amount wins; repeated slots inside that recipe remain independent.
     */
    private static void addMechanicalByproducts() {
        Map<String, List<GTRecipe>> sources = new HashMap<>();
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.maceratorRecipes, RecipeMaps.thermalCentrifugeRecipes,
            RecipeMaps.centrifugeRecipes }) {
            for (GTRecipe source : map.getAllRecipes()) {
                if (!source.mEnabled || source.mInputs.length != 1
                    || source.mInputs[0] == null
                    || source.mInputs[0].stackSize != 1
                    || source.mOutputs.length < 2
                    || source.mFluidInputs.length != 0
                    || source.mFluidOutputs.length != 0
                    || source.mSpecialItems != null) continue;
                for (int oreId : OreDictionary.getOreIDs(source.mInputs[0])) {
                    String name = OreDictionary.getOreName(oreId);
                    OrePrefixes prefix = inputPrefix(name);
                    if (prefix == null || BASIC_STONE_TYPES.contains(prefix) || prefix == OrePrefixes.rawOre) continue;
                    if (!matchesMaterial(
                        source.mInputs[0],
                        name.substring(
                            prefix.name()
                                .length())))
                        continue;
                    sources.computeIfAbsent(name, ignored -> new ArrayList<>())
                        .add(source);
                }
            }
        }

        int supplemented = 0;
        for (GTRecipe recipe : OreProcessingRecipes.getAllRecipes()) {
            if (recipe.mInputs.length != 1 || recipe.mInputs[0] == null) continue;
            for (int oreId : OreDictionary.getOreIDs(recipe.mInputs[0])) {
                String name = OreDictionary.getOreName(oreId);
                OrePrefixes prefix = inputPrefix(name);
                if (prefix == null) continue;
                String material = name.substring(
                    prefix.name()
                        .length());
                if (!matchesMaterial(recipe.mInputs[0], material)) continue;
                List<ByproductGroup> selected = new ArrayList<>();
                boolean found = false;
                for (OrePrefixes stage : remainingStages(prefix)) {
                    List<GTRecipe> stageRecipes = sources.get(stage.name() + material);
                    if (stageRecipes == null) continue;
                    found = true;
                    for (GTRecipe source : stageRecipes) {
                        List<ByproductGroup> groups = new ArrayList<>();
                        for (int slot = 1; slot < source.mOutputs.length; slot++) {
                            ItemStack output = source.mOutputs[slot];
                            if (output == null || output.stackSize <= 0
                                || source.getOutputChance(slot) <= 0
                                || isGangue(output)
                                || hasOutput(recipe.mOutputs, output)) continue;
                            ByproductGroup group = findGroup(groups, output);
                            if (group == null) {
                                group = new ByproductGroup(source, output);
                                groups.add(group);
                            }
                            group.slots.add(slot);
                            group.expectedAmount += (long) output.stackSize * source.getOutputChance(slot);
                        }
                        for (ByproductGroup group : groups) {
                            ByproductGroup current = findGroup(selected, group.item);
                            if (current == null) selected.add(group);
                            else if (group.expectedAmount > current.expectedAmount) {
                                selected.set(selected.indexOf(current), group);
                            }
                        }
                    }
                }
                if (!found) continue;
                if (!selected.isEmpty()) {
                    List<ItemStack> outputs = new ArrayList<>();
                    List<Integer> chances = new ArrayList<>();
                    for (int slot = 0; slot < recipe.mOutputs.length; slot++) {
                        outputs.add(recipe.mOutputs[slot]);
                        chances.add(recipe.getOutputChance(slot));
                    }
                    int multiplier = isRichOre(prefix) ? 2 : 1;
                    for (ByproductGroup group : selected) {
                        for (int slot : group.slots) {
                            outputs.add(
                                GTUtility.copyAmountUnsafe(
                                    group.source.mOutputs[slot].stackSize * multiplier,
                                    group.source.mOutputs[slot]));
                            chances.add(group.source.getOutputChance(slot));
                        }
                    }
                    recipe.mOutputs = outputs.toArray(new ItemStack[0]);
                    recipe.mOutputChances = chances.stream()
                        .mapToInt(Integer::intValue)
                        .toArray();
                    supplemented++;
                }
                break;
            }
        }
        GTNotGood.LOG
            .info("Added native grinding and centrifuging products to {} large ore processor recipes", supplemented);
    }

    /** Progressed inputs must not collect byproducts from stages they have already passed. */
    private static OrePrefixes[] remainingStages(OrePrefixes input) {
        if (input == OrePrefixes.crushedPurified) {
            return new OrePrefixes[] { OrePrefixes.crushedPurified, OrePrefixes.crushedCentrifuged,
                OrePrefixes.dustPure };
        }
        if (input == OrePrefixes.crushedCentrifuged || input == OrePrefixes.dustPure
            || input == OrePrefixes.dustImpure) {
            return new OrePrefixes[] { input };
        }
        return byproductStages;
    }

    private static OrePrefixes inputPrefix(String name) {
        for (OrePrefixes prefix : inputForms) {
            if (name.startsWith(prefix.name())) return prefix;
        }
        return null;
    }

    /** Existing full dust also covers smaller dust fractions of the same registered material. */
    private static boolean hasOutput(ItemStack[] outputs, ItemStack candidate) {
        ItemData expected = GTOreDictUnificator.getAssociation(candidate);
        for (ItemStack output : outputs) {
            if (output == null) continue;
            if (GTUtility.areStacksEqual(output, candidate)) return true;
            ItemData existing = GTOreDictUnificator.getAssociation(output);
            if (existing != null && expected != null
                && isDust(existing.mPrefix)
                && isDust(expected.mPrefix)
                && existing.mMaterial != null
                && expected.mMaterial != null
                && expected.mMaterial.mMaterial != null
                && existing.mMaterial.mMaterial == expected.mMaterial.mMaterial
                && (long) output.stackSize * existing.mPrefix.getMaterialAmount()
                    >= (long) candidate.stackSize * expected.mPrefix.getMaterialAmount())
                return true;
        }
        return false;
    }

    private static boolean isDust(OrePrefixes prefix) {
        return prefix == OrePrefixes.dust || prefix == OrePrefixes.dustSmall || prefix == OrePrefixes.dustTiny;
    }

    /** Host-rock waste is not a valuable ore byproduct. */
    private static boolean isGangue(ItemStack output) {
        for (int oreId : OreDictionary.getOreIDs(output)) {
            String name = OreDictionary.getOreName(oreId);
            for (String prefix : new String[] { "dustSmall", "dustTiny", "dust" }) {
                if (!name.startsWith(prefix)) continue;
                String material = name.substring(prefix.length());
                if (material.equals("Stone") || material.equals("Netherrack") || material.equals("Endstone"))
                    return true;
                break;
            }
        }
        return false;
    }

    private static ByproductGroup findGroup(List<ByproductGroup> groups, ItemStack item) {
        for (ByproductGroup group : groups) {
            if (GTUtility.areStacksEqual(group.item, item)) return group;
        }
        return null;
    }

    /** One source recipe's independent slots for a single byproduct item. */
    private static final class ByproductGroup {

        private final GTRecipe source;
        private final ItemStack item;
        private final List<Integer> slots = new ArrayList<>();
        private long expectedAmount;

        private ByproductGroup(GTRecipe source, ItemStack item) {
            this.source = source;
            this.item = item;
        }
    }

    /**
     * Supplements simplified ore recipes with native purified-ore sifting outputs. Existing dust and gem yields take
     * precedence; missing products retain their independent native rolls, including repeated output slots.
     */
    private static void addSiftingOutputs() {
        Map<String, GTRecipe> siftingRecipes = new HashMap<>();
        for (GTRecipe recipe : RecipeMaps.sifterRecipes.getAllRecipes()) {
            if (!recipe.mEnabled || recipe.mInputs.length != 1
                || recipe.mInputs[0] == null
                || recipe.mInputs[0].stackSize != 1
                || recipe.mFluidInputs.length != 0
                || recipe.mFluidOutputs.length != 0
                || recipe.mSpecialItems != null) continue;
            for (int oreId : OreDictionary.getOreIDs(recipe.mInputs[0])) {
                String oreName = OreDictionary.getOreName(oreId);
                if (oreName.startsWith("crushedPurified")) {
                    String materialName = oreName.substring("crushedPurified".length());
                    if (matchesMaterial(recipe.mInputs[0], materialName)) {
                        siftingRecipes.putIfAbsent(materialName, recipe);
                    }
                }
            }
        }

        int supplemented = 0;
        for (GTRecipe recipe : OreProcessingRecipes.getAllRecipes()) {
            ItemStack input = recipe.mInputs[0];
            for (int oreId : OreDictionary.getOreIDs(input)) {
                String oreName = OreDictionary.getOreName(oreId);
                String materialName = getSiftingMaterial(oreName);
                if (!matchesMaterial(input, materialName)) continue;
                GTRecipe sifting = siftingRecipes.get(materialName);
                if (sifting == null) continue;

                List<ItemStack> outputs = new ArrayList<>();
                List<Integer> chances = new ArrayList<>();
                for (int i = 0; i < recipe.mOutputs.length; i++) {
                    outputs.add(recipe.mOutputs[i]);
                    chances.add(recipe.getOutputChance(i));
                }
                int multiplier = oreName.startsWith("oreNetherrack") || oreName.startsWith("oreEndstone") ? 2 : 1;
                for (int i = 0; i < sifting.mOutputs.length; i++) {
                    ItemStack output = sifting.mOutputs[i];
                    if (output == null || output.stackSize <= 0 || sifting.getOutputChance(i) <= 0) continue;
                    boolean existing = false;
                    for (ItemStack original : recipe.mOutputs) {
                        if (GTUtility.areStacksEqual(original, output)) {
                            existing = true;
                            break;
                        }
                    }
                    if (existing) continue;
                    outputs.add(GTUtility.copyAmountUnsafe(output.stackSize * multiplier, output));
                    chances.add(sifting.getOutputChance(i));
                }
                if (outputs.size() > recipe.mOutputs.length) {
                    recipe.mOutputs = outputs.toArray(new ItemStack[0]);
                    recipe.mOutputChances = chances.stream()
                        .mapToInt(Integer::intValue)
                        .toArray();
                    supplemented++;
                }
                break;
            }
        }
        GTNotGood.LOG.info("Added native sifting products to {} large ore processor recipes", supplemented);
    }

    /** Broad ore-dictionary groups such as AnyCarbon must not share one material's sifting products. */
    private static boolean matchesMaterial(ItemStack input, String materialName) {
        ItemData association = GTOreDictUnificator.getAssociation(input);
        return association == null || association.mMaterial == null
            || association.mMaterial.mMaterial == null
            || association.mMaterial.mMaterial.mName.equals(materialName);
    }

    /** Only inputs before the sifting branch can receive sifting products; dusts and centrifuged ores cannot. */
    private static String getSiftingMaterial(String oreName) {
        if (oreName.startsWith("crushedCentrifuged")) return null;
        for (OrePrefixes prefix : new OrePrefixes[] { OrePrefixes.crushedPurified, OrePrefixes.crushed,
            OrePrefixes.rawOre, OrePrefixes.oreNetherrack, OrePrefixes.oreEndstone, OrePrefixes.oreBasalt,
            OrePrefixes.oreBlackgranite, OrePrefixes.oreRedgranite, OrePrefixes.oreMarble, OrePrefixes.ore }) {
            String name = prefix.name();
            if (oreName.startsWith(name)) return oreName.substring(name.length());
        }
        return null;
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
