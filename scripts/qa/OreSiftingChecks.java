package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.xyp.gtnotgood.common.machines.multiblock.LargeOreProcessor;
import com.xyp.gtnotgood.common.recipe.gtnotgood.OreProcessingRecipes;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.ModList;

import bartworks.system.material.WerkstoffLoader;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.material.Material;
import gtPlusPlus.core.material.MaterialsOres;

/** Exercises native BartWorks recipes and the ore controller's batch execution in a real Forge lifecycle. */
@Mod(
    modid = "oresiftingqa",
    name = "Ore Sifting QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class OreSiftingChecks {

    private Item fixture;
    private GTRecipe diamondBefore;
    private GTRecipe graphiteBefore;
    private final Map<GTRecipe, String> fixtureSources = new IdentityHashMap<>();
    private final Map<GTRecipe, GTRecipe> baseRecipes = new IdentityHashMap<>();
    private int ticks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.oreSifting.qa")) return;
        diamondBefore = recipe(GTOreDictUnificator.get(OrePrefixes.ore, Materials.Diamond, 1)).copy();
        graphiteBefore = recipe(GTOreDictUnificator.get(OrePrefixes.ore, Materials.Graphite, 1)).copy();
        for (GTRecipe converted : GTNGRecipeMaps.OreProcessingRecipes.getAllRecipes())
            baseRecipes.put(converted, converted.copy());
        fixture = new Item().setHasSubtypes(true)
            .setUnlocalizedName("oreSiftingFixture");
        GameRegistry.registerItem(fixture, "fixture");
        String[] forms = { "ore", "crushedPurified", "dust", "oreNetherrack", "crushedCentrifuged", "dustPure",
            "dustImpure", "crushed", "rawOre" };
        for (int i = 0; i < forms.length; i++) OreDictionary.registerOre(forms[i] + "OreSiftingQa", stack(i, 1));
        GTRecipeBuilder.builder()
            .itemInputs(stack(1, 1))
            .itemOutputs(stack(2, 1), stack(10, 1), stack(10, 3), stack(11, 2))
            .outputChances(7000, 2500, 1000, 10000)
            .duration(20)
            .eut(32)
            .addTo(RecipeMaps.sifterRecipes);
        registerByproductFixtures(forms);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    /** Separate material fixtures keep the original sifter checks independent of the added processing paths. */
    private void registerByproductFixtures(String[] forms) {
        for (int i = 0; i < forms.length; i++)
            OreDictionary.registerOre(forms[i] + "OreByproductsQa", stack(20 + i, 1));
        remember(
            fixtureRecipe(stack(27, 1)).itemOutputs(stack(26, 1), stack(40, 1), stack(41, 1))
                .outputChances(10000, 2000, 5000),
            RecipeMaps.maceratorRecipes);
        remember(
            fixtureRecipe(stack(21, 1)).itemOutputs(stack(24, 1), stack(40, 2), stack(43, 1))
                .outputChances(10000, 1111, 2000),
            RecipeMaps.thermalCentrifugeRecipes);
        remember(
            fixtureRecipe(stack(25, 1))
                .itemOutputs(stack(22, 1), stack(40, 2), stack(44, 1), stack(42, 1), stack(42, 3))
                .outputChances(10000, 1111, 5000, 2500, 1000),
            RecipeMaps.centrifugeRecipes);
        remember(
            fixtureRecipe(stack(24, 1)).itemOutputs(stack(22, 1), stack(45, 1), stack(47, 1), stack(48, 1))
                .outputChances(10000, 3000, 10000, 10000),
            RecipeMaps.maceratorRecipes);
        remember(
            fixtureRecipe(stack(26, 1)).itemOutputs(stack(22, 1), stack(46, 1))
                .outputChances(10000, 4000),
            RecipeMaps.centrifugeRecipes);
        for (int meta = 60; meta <= 67; meta++) OreDictionary.registerOre("dustPureOreByproductsQa", stack(meta, 1));
        remember(fixtureRecipe(stack(60, 2)).itemOutputs(stack(24, 1), stack(50, 1)), RecipeMaps.centrifugeRecipes);
        remember(
            fixtureRecipe(stack(61, 1)).itemOutputs(stack(24, 1), stack(51, 1))
                .fluidInputs(Materials.Water.getFluid(1000)),
            RecipeMaps.centrifugeRecipes);
        remember(
            fixtureRecipe(stack(62, 1)).itemOutputs(stack(24, 1), stack(52, 1))
                .fluidOutputs(Materials.Water.getFluid(1000)),
            RecipeMaps.centrifugeRecipes);
        remember(
            fixtureRecipe(stack(63, 1), stack(59, 1)).itemOutputs(stack(24, 1), stack(53, 1)),
            RecipeMaps.centrifugeRecipes);
        remember(
            fixtureRecipe(stack(64, 1)).itemOutputs(stack(24, 1), stack(54, 1))
                .special(stack(59, 1)),
            RecipeMaps.centrifugeRecipes);
        for (GTRecipe disabled : fixtureRecipe(stack(65, 1)).itemOutputs(stack(24, 1), stack(55, 1))
            .addTo(RecipeMaps.centrifugeRecipes)) {
            disabled.mEnabled = false;
            fixtureSources.put(disabled, recipeState(disabled));
        }
        remember(
            fixtureRecipe(stack(66, 1)).itemOutputs(stack(24, 1), stack(56, 1))
                .fluidInputs(Materials.Water.getFluid(1000)),
            RecipeMaps.chemicalBathRecipes);
        remember(
            fixtureRecipe(stack(67, 1)).itemOutputs(stack(58, 1), stack(59, 1))
                .outputChances(10000, 0),
            RecipeMaps.centrifugeRecipes);
    }

    private static GTRecipeBuilder fixtureRecipe(ItemStack... inputs) {
        return GTRecipeBuilder.builder()
            .itemInputs(inputs)
            .duration(20)
            .eut(32);
    }

    private void remember(GTRecipeBuilder builder, RecipeMap<?> map) {
        int added = 0;
        for (GTRecipe source : builder.addTo(map)) {
            fixtureSources.put(source, recipeState(source));
            added++;
        }
        require(added > 0, "Native fixture recipe registered");
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (fixture == null || event.phase != TickEvent.Phase.END || ++ticks != 2) return;
        try {
            check();
        } finally {
            Minecraft.getMinecraft()
                .shutdown();
        }
    }

    private void check() throws Exception {
        GTRecipe nativeThorianite = RecipeMaps.sifterRecipes.findRecipeQuery()
            .items(WerkstoffLoader.Thorianit.get(OrePrefixes.crushedPurified))
            .find();
        require(nativeThorianite != null, "Native thorianite sifting recipe");
        GTRecipe thorianite = recipe(WerkstoffLoader.Thorianit.get(OrePrefixes.ore));
        require(thorianite.mOutputs.length == 4, "Thorianite main dust and three independent sifting slots");
        require(
            output(thorianite, WerkstoffLoader.Thorianit.get(OrePrefixes.dust), 8, 10000) == 1,
            "Main dust retained");
        require(output(thorianite, Materials.Thorium.getDust(1), 1, 600) == 1, "Native 6% thorium roll");
        require(output(thorianite, Materials.Thorium.getDust(1), 1, 300) == 1, "Native 3% thorium roll");
        require(
            output(thorianite, WerkstoffLoader.Thorium232.get(OrePrefixes.dust), 1, 100) == 1,
            "Native 1% thorium-232");
        for (OrePrefixes form : new OrePrefixes[] { OrePrefixes.rawOre, OrePrefixes.crushed,
            OrePrefixes.crushedPurified }) {
            require(
                output(
                    recipe(WerkstoffLoader.Thorianit.get(form)),
                    WerkstoffLoader.Thorium232.get(OrePrefixes.dust),
                    1,
                    100) == 1,
                "Native thorianite intermediate " + form);
        }
        require(
            Arrays.equals(nativeThorianite.mOutputChances, new int[] { 7000, 1300, 700, 600, 300, 100 }),
            "Native chances untouched");
        GTRecipe diamond = recipe(GTOreDictUnificator.get(OrePrefixes.ore, Materials.Diamond, 1));
        for (int i = 0; i < diamondBefore.mOutputs.length; i++) {
            require(
                output(
                    diamond,
                    diamondBefore.mOutputs[i],
                    diamondBefore.mOutputs[i].stackSize,
                    diamondBefore.getOutputChance(i)) > 0,
                "Existing GT dust and gem yields retained");
        }
        require(
            diamond.mOutputs.length > diamondBefore.mOutputs.length,
            "GT gem ore also imports missing sifting outputs");
        GTRecipe graphite = recipe(GTOreDictUnificator.get(OrePrefixes.ore, Materials.Graphite, 1));
        require(
            graphite.mOutputs.length == graphiteBefore.mOutputs.length,
            "AnyCarbon does not import coal products into graphite");

        for (int form : new int[] { 0, 1, 7, 8 }) {
            GTRecipe imported = recipe(stack(form, 1));
            require(imported.mOutputs.length == 4, "Fixture output slots " + form);
            require(output(imported, stack(2, 1), 8, 10000) == 1, "No duplicate main dust " + form);
            require(output(imported, stack(10, 1), 1, 2500) == 1, "First repeated native output " + form);
            require(output(imported, stack(10, 1), 3, 1000) == 1, "Second repeated native output " + form);
        }
        GTRecipe rich = recipe(stack(3, 1));
        require(output(rich, stack(2, 1), 16, 10000) == 1, "Rich main dust doubled");
        require(output(rich, stack(10, 1), 2, 2500) == 1, "Rich sifting amount doubled");
        require(output(rich, stack(10, 1), 6, 1000) == 1, "Rich repeated output retains chance");
        for (int form : new int[] { 4, 5, 6 })
            require(recipe(stack(form, 1)).mOutputs.length == 1, "Downstream form excludes sifting " + form);
        checkByproductFixtures();
        checkNativeResidues();
        Map<GTRecipe, String> beforeReload = new IdentityHashMap<>();
        for (GTRecipe converted : GTNGRecipeMaps.OreProcessingRecipes.getAllRecipes())
            beforeReload.put(converted, recipeState(converted));
        Map<GTRecipe, String> nativeBeforeReload = new IdentityHashMap<>();
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.sifterRecipes, RecipeMaps.maceratorRecipes,
            RecipeMaps.thermalCentrifugeRecipes, RecipeMaps.centrifugeRecipes }) {
            for (GTRecipe source : map.getAllRecipes()) nativeBeforeReload.put(source, recipeState(source));
        }
        int recipesBefore = GTNGRecipeMaps.OreProcessingRecipes.getAllRecipes()
            .size();
        OreProcessingRecipes.loadExternalOreRecipes();
        require(thorianite.mOutputs.length == 4, "Repeated loading does not duplicate products");
        require(
            recipesBefore == GTNGRecipeMaps.OreProcessingRecipes.getAllRecipes()
                .size(),
            "Repeated loading does not duplicate recipes");
        beforeReload.forEach(
            (converted,
                state) -> require(state.equals(recipeState(converted)), "Reload keeps output content and order"));
        nativeBeforeReload.forEach(
            (source, state) -> require(state.equals(recipeState(source)), "Reload leaves native recipes unchanged"));

        Method process = LargeOreProcessor.class
            .getDeclaredMethod("processRecipe", ItemStack.class, GTRecipe.class, int.class, ArrayList.class);
        process.setAccessible(true);
        LargeOreProcessor controller = new LargeOreProcessor("oreSiftingQa");
        ArrayList<ItemStack> outputs = new ArrayList<>();
        ItemStack batch = GTUtility.copyAmountUnsafe(200000, WerkstoffLoader.Thorianit.get(OrePrefixes.ore));
        process.invoke(controller, batch, thorianite, batch.stackSize, outputs);
        require(batch.stackSize == 0, "Batch consumed");
        require(
            amount(outputs, WerkstoffLoader.Thorianit.get(OrePrefixes.dust)) == 1600000,
            "Batch main dust quantity");
        long thorium232 = amount(outputs, WerkstoffLoader.Thorium232.get(OrePrefixes.dust));
        require(thorium232 > 1500 && thorium232 < 2500, "Batch 1% roll, not guaranteed output: " + thorium232);
        long thorium = amount(outputs, Materials.Thorium.getDust(1));
        require(thorium > 16500 && thorium < 19500, "Independent thorium rolls: " + thorium);
        outputs.clear();
        ItemStack mechanicalBatch = stack(20, 200000);
        process.invoke(controller, mechanicalBatch, recipe(stack(20, 1)), mechanicalBatch.stackSize, outputs);
        require(mechanicalBatch.stackSize == 0, "Mechanical byproduct batch consumed");
        long mechanical = amount(outputs, stack(40, 1));
        require(mechanical > 42000 && mechanical < 47000, "Mechanical byproduct native chance: " + mechanical);
        long repeated = amount(outputs, stack(42, 1));
        require(repeated > 105000 && repeated < 115000, "Mechanical repeated independent slots: " + repeated);
        GTRecipe probe = recipe(stack(0, 1)).copy();
        probe.mOutputChances = new int[] { 10000, 0, 0, 10000 };
        outputs.clear();
        process.invoke(controller, stack(0, Integer.MAX_VALUE), probe, Integer.MAX_VALUE, outputs);
        require(amount(outputs, stack(2, 1)) == 8L * Integer.MAX_VALUE, "Large parallel count avoids integer overflow");
        require(amount(outputs, stack(10, 1)) == 0, "Zero-chance output absent");
        require(amount(outputs, stack(11, 1)) == 2L * Integer.MAX_VALUE, "Guaranteed output preserved");
        require(
            outputs.stream()
                .allMatch(s -> s.stackSize > 0),
            "Only positive output stacks");
        int maxOutputs = GTNGRecipeMaps.OreProcessingRecipes.getAllRecipes()
            .stream()
            .mapToInt(r -> r.mOutputs.length)
            .max()
            .orElse(0);
        require(maxOutputs <= 9, "All imported products fit the NEI output grid: " + maxOutputs);
        String coverage = auditSiftingCoverage() + auditByproductCoverage();
        String report = "PASS\nThorianite dust retained; native 6%, 3%, 1% slots; raw/crushed/purified forms; rich ores; "
            + "downstream exclusions; processing-stage byproducts; alternate-path deduplication; repeated native slots; "
            + "fluid/chemical/special/multiple-input exclusions; dust-size coverage; native recipe preservation; "
            + "repeated loading; independent batch rolls; zero chance; long output quantities.\n"
            + "Thorium-232 batch="
            + thorium232
            + "; maximum recipe output slots="
            + maxOutputs
            + "\n"
            + coverage;
        Files.write(
            new File(System.getProperty("gtng.oreSifting.output")).toPath(),
            report.getBytes(StandardCharsets.UTF_8));
        System.out.println("ORE_SIFTING_QA " + report);
    }

    private void checkByproductFixtures() {
        for (int form : new int[] { 20, 27, 28 }) {
            GTRecipe converted = recipe(stack(form, 1));
            require(converted.mOutputs.length == 9, "All remaining processing stages represented " + form);
            require(output(converted, stack(22, 1), 8, 10000) == 1, "Byproduct fixture main yield retained " + form);
            require(output(converted, stack(40, 1), 2, 1111) == 1, "One strongest alternate-path group " + form);
            require(slots(converted, stack(40, 1)) == 1, "No combined macerator/thermal/centrifuge rolls " + form);
            require(output(converted, stack(41, 1), 1, 5000) == 1, "Crushed-stage product " + form);
            require(output(converted, stack(42, 1), 1, 2500) == 1, "Independent group first slot " + form);
            require(output(converted, stack(42, 1), 3, 1000) == 1, "Independent group second slot " + form);
            require(output(converted, stack(43, 1), 1, 2000) == 1, "Purified-stage product " + form);
            require(output(converted, stack(44, 1), 1, 5000) == 1, "Pure-dust-stage product " + form);
            require(output(converted, stack(45, 1), 1, 3000) == 1, "Centrifuged-stage product " + form);
            require(output(converted, stack(46, 1), 1, 4000) == 1, "Impure-dust-stage product " + form);
        }
        GTRecipe purified = recipe(stack(21, 1));
        require(purified.mOutputs.length == 7, "Purified input imports only remaining stages");
        require(slots(purified, stack(41, 1)) == 0, "Purified input does not replay crushed processing");
        require(slots(purified, stack(46, 1)) == 0, "Purified input does not return to impure dust");
        GTRecipe centrifuged = recipe(stack(24, 1));
        require(centrifuged.mOutputs.length == 2, "Centrifuged input has its own macerator byproduct only");
        require(output(centrifuged, stack(45, 1), 1, 3000) == 1, "Centrifuged macerator chance retained");
        GTRecipe impure = recipe(stack(26, 1));
        require(impure.mOutputs.length == 2, "Impure dust cannot enter purified processing");
        require(output(impure, stack(22, 1), 6, 10000) == 1, "Impure dust main yield retained");
        require(output(impure, stack(46, 1), 1, 4000) == 1, "Impure dust own centrifuge product");
        GTRecipe pure = recipe(stack(25, 1));
        require(pure.mOutputs.length == 5, "Pure dust only imports its own centrifuge groups");
        require(output(pure, stack(22, 1), 7, 10000) == 1, "Pure dust main yield retained");
        require(output(pure, stack(40, 1), 2, 1111) == 1, "Pure dust own centrifuge probability");
        GTRecipe rich = recipe(stack(23, 1));
        require(rich.mOutputs.length == 9, "Rich input includes the same groups");
        require(output(rich, stack(22, 1), 16, 10000) == 1, "Rich main yield retained");
        require(output(rich, stack(40, 1), 4, 1111) == 1, "Rich byproduct quantity doubles, chance unchanged");
        require(output(rich, stack(42, 1), 2, 2500) == 1, "Rich repeated first slot quantity");
        require(output(rich, stack(42, 1), 6, 1000) == 1, "Rich repeated second slot quantity");
        for (int form : new int[] { 20, 21, 23, 24, 25, 26, 27, 28 }) {
            GTRecipe converted = recipe(stack(form, 1));
            require(slots(converted, stack(47, 1)) == 0, "Existing full dust covers small dust " + form);
            require(slots(converted, stack(48, 1)) == 0, "Existing full dust covers tiny dust " + form);
            for (int meta = 50; meta <= 59; meta++) require(
                slots(converted, stack(meta, 1)) == 0,
                "Ineligible source/product excluded " + form + "/" + meta);
            require(slots(converted, stack(26, 1)) == 0, "Native main transition slot is not a byproduct " + form);
        }
        require(fixtureSources.size() >= 13, "All valid and excluded source fixtures registered");
        fixtureSources.forEach(
            (source,
                state) -> require(state.equals(recipeState(source)), "First import leaves source fixtures unchanged"));
    }

    private static void checkNativeResidues() {
        ItemStack residue = WerkstoffLoader.IrLeachResidue.get(OrePrefixes.dust, 1);
        for (Material material : new Material[] { MaterialsOres.KASHINITE, MaterialsOres.IRARSITE }) {
            GTRecipe nativePure = RecipeMaps.centrifugeRecipes.findRecipeQuery()
                .items(material.getDustPurified(1))
                .find();
            require(nativePure != null, "Native GT++ pure-dust centrifuging " + material.getLocalizedNameKey());
            require(output(nativePure, residue, 2, 1111) == 1, "Native converted iridium residue and chance");
            for (ItemStack input : new ItemStack[] { material.getOre(1), material.getRawOre(1), material.getCrushed(1),
                material.getCrushedPurified(1) }) {
                GTRecipe converted = recipe(input);
                require(output(converted, material.getDust(1), 8, 10000) == 1, "GT++ main eight-dust yield retained");
                require(output(converted, residue, 2, 1111) == 1, "GT++ missing iridium residue imported");
                require(slots(converted, residue) == 1, "GT++ iridium residue not summed across alternate paths");
            }
            GTRecipe centrifuged = recipe(material.getCrushedCentrifuged(1));
            require(output(centrifuged, residue, 2, 1000) == 1, "GT++ centrifuged input uses remaining macerator");
            GTRecipe pure = recipe(material.getDustPurified(1));
            require(output(pure, material.getDust(1), 7, 10000) == 1, "GT++ pure-dust main yield retained");
            require(output(pure, residue, 2, 1111) == 1, "GT++ pure-dust native centrifuge product");
            GTRecipe impure = recipe(material.getDustImpure(1));
            require(output(impure, material.getDust(1), 6, 10000) == 1, "GT++ impure-dust main yield retained");
            GTRecipe nativeImpure = RecipeMaps.centrifugeRecipes.findRecipeQuery()
                .items(material.getDustImpure(1))
                .find();
            require(nativeImpure != null, "Native GT++ impure-dust centrifuging");
            for (int slot = 1; slot < nativeImpure.mOutputs.length; slot++) {
                ItemStack byproduct = nativeImpure.mOutputs[slot];
                if (byproduct == null || nativeImpure.getOutputChance(slot) <= 0) continue;
                require(
                    covers(impure.mOutputs, byproduct) && (slots(impure, byproduct) == 0
                        || output(impure, byproduct, byproduct.stackSize, nativeImpure.getOutputChance(slot)) == 1),
                    "GT++ impure input imports its own registered byproduct");
            }
        }
    }

    /** Audits every registered ore material against actual native outputs, rather than a fixed example list. */
    private static String auditSiftingCoverage() throws Exception {
        TreeMap<String, String> materials = new TreeMap<>();
        Set<GTRecipe> covered = new HashSet<>();
        String[] prefixes = { "ore", "rawOre", "crushed", "crushedPurified", "oreBasalt", "oreBlackgranite",
            "oreRedgranite", "oreMarble", "oreNetherrack", "oreEndstone" };
        for (GTRecipe source : RecipeMaps.sifterRecipes.getAllRecipes()) {
            if (!source.mEnabled || source.mInputs.length != 1
                || source.mInputs[0] == null
                || source.mInputs[0].stackSize != 1
                || source.mFluidInputs.length > 0
                || source.mFluidOutputs.length > 0
                || source.mSpecialItems != null) continue;
            for (int oreId : OreDictionary.getOreIDs(source.mInputs[0])) {
                String name = OreDictionary.getOreName(oreId);
                if (!name.startsWith("crushedPurified") || name.endsWith("OreSiftingQa")) continue;
                String material = name.substring("crushedPurified".length());
                ItemData association = GTOreDictUnificator.getAssociation(source.mInputs[0]);
                if (association != null && association.mMaterial != null
                    && association.mMaterial.mMaterial != null
                    && !material.equals(association.mMaterial.mMaterial.mName)) continue;
                Set<GTRecipe> variants = new HashSet<>();
                for (String prefix : prefixes) {
                    int multiplier = prefix.equals("oreNetherrack") || prefix.equals("oreEndstone") ? 2 : 1;
                    for (ItemStack input : OreDictionary.getOres(prefix + material)) {
                        GTRecipe converted = recipe(GTUtility.copyAmountUnsafe(1, input));
                        for (int i = 0; i < source.mOutputs.length; i++) {
                            ItemStack item = source.mOutputs[i];
                            if (item == null || item.stackSize <= 0 || source.getOutputChance(i) <= 0) continue;
                            boolean retained = false;
                            for (int j = 0; j < converted.mOutputs.length; j++) {
                                ItemStack result = converted.mOutputs[j];
                                if (GTUtility.areStacksEqual(item, result) && converted.getOutputChance(j) == 10000
                                    && result.stackSize >= item.stackSize * multiplier) retained = true;
                            }
                            require(
                                retained
                                    || output(converted, item, item.stackSize * multiplier, source.getOutputChance(i))
                                        > 0,
                                "Missing native sifting product: " + material
                                    + "/"
                                    + prefix
                                    + "/"
                                    + item.getDisplayName());
                        }
                        variants.add(converted);
                        covered.add(converted);
                    }
                }
                require(!variants.isEmpty(), "No covered input forms: " + material);
                materials.put(material, source.mInputs[0].getDisplayName() + "\t" + variants.size());
            }
        }
        StringBuilder table = new StringBuilder("Material\tNative purified ore\tVerified input recipes\n");
        materials.forEach(
            (material, row) -> table.append(material)
                .append('\t')
                .append(row)
                .append('\n'));
        File folder = new File(System.getProperty("gtng.oreSifting.output")).getParentFile();
        Files.write(
            new File(folder, "coverage.tsv").toPath(),
            table.toString()
                .getBytes(StandardCharsets.UTF_8));
        return "Audited native materials=" + materials.size() + "; input recipes=" + covered.size() + "\n";
    }

    /** Checks real registered products and per-item yield bounds across every supported mechanical input stage. */
    private String auditByproductCoverage() throws Exception {
        Map<String, List<GTRecipe>> sources = new LinkedHashMap<>();
        Set<GTRecipe> eligibleSources = new HashSet<>();
        Set<GTRecipe> excludedSources = new HashSet<>();
        for (RecipeMap<?> map : new RecipeMap<?>[] { RecipeMaps.maceratorRecipes, RecipeMaps.thermalCentrifugeRecipes,
            RecipeMaps.centrifugeRecipes }) {
            for (GTRecipe source : map.getAllRecipes()) {
                if (source.mInputs.length == 0 || source.mInputs[0] == null) continue;
                String[] form = registeredForm(source.mInputs[0]);
                if (form == null || form[1].endsWith("Qa") || !isIntermediate(form[0])) continue;
                if (!eligibleSource(source) || source.mOutputs.length < 2) {
                    excludedSources.add(source);
                    continue;
                }
                sources.computeIfAbsent(form[0] + form[1], ignored -> new ArrayList<>())
                    .add(source);
                eligibleSources.add(source);
            }
        }
        TreeMap<String, Set<GTRecipe>> materials = new TreeMap<>();
        Set<GTRecipe> covered = new HashSet<>();
        int verifiedGroups = 0;
        for (GTRecipe converted : GTNGRecipeMaps.OreProcessingRecipes.getAllRecipes()) {
            if (converted.mInputs.length != 1 || converted.mInputs[0] == null) continue;
            String[] form = registeredForm(converted.mInputs[0]);
            if (form == null || form[1].endsWith("Qa")) continue;
            int multiplier = form[0].equals("oreNetherrack") || form[0].equals("oreEndstone") ? 2 : 1;
            Map<String, Long> maximum = new LinkedHashMap<>();
            Map<String, ItemStack> products = new LinkedHashMap<>();
            for (String stage : remainingStages(form[0])) {
                List<GTRecipe> stageSources = sources.get(stage + form[1]);
                if (stageSources == null) continue;
                for (GTRecipe source : stageSources) {
                    Map<String, Long> groups = new LinkedHashMap<>();
                    for (int slot = 1; slot < source.mOutputs.length; slot++) {
                        ItemStack product = source.mOutputs[slot];
                        if (product == null || product.stackSize <= 0
                            || source.getOutputChance(slot) <= 0
                            || gangue(product)) continue;
                        String key = itemKey(product);
                        groups.merge(key, (long) product.stackSize * source.getOutputChance(slot), Long::sum);
                        products.putIfAbsent(key, product);
                    }
                    groups.forEach((key, expected) -> maximum.merge(key, expected, Math::max));
                }
            }
            if (maximum.isEmpty()) continue;
            GTRecipe base = baseRecipes.get(converted);
            ItemStack[] original = base == null ? new ItemStack[] { converted.mOutputs[0] } : base.mOutputs;
            for (Map.Entry<String, Long> entry : maximum.entrySet()) {
                ItemStack product = products.get(entry.getKey());
                require(
                    covers(converted.mOutputs, product),
                    "Missing native mechanical product: " + form[1] + "/" + form[0] + "/" + product.getDisplayName());
                if (!covers(original, product) && !siftingPriority(form, product, multiplier)) {
                    long actual = 0;
                    for (int slot = 0; slot < converted.mOutputs.length; slot++) {
                        if (GTUtility.areStacksEqual(converted.mOutputs[slot], product))
                            actual += (long) converted.mOutputs[slot].stackSize * converted.getOutputChance(slot);
                    }
                    require(
                        actual == entry.getValue() * multiplier,
                        "Mechanical group yield differs from best native source: " + form[1]
                            + "/"
                            + form[0]
                            + "/"
                            + product.getDisplayName()
                            + "/expected="
                            + entry.getValue() * multiplier
                            + "/actual="
                            + actual);
                }
                verifiedGroups++;
            }
            materials.computeIfAbsent(form[1], ignored -> new HashSet<>())
                .add(converted);
            covered.add(converted);
        }
        require(!materials.isEmpty() && !covered.isEmpty(), "Native mechanical coverage is populated");
        StringBuilder table = new StringBuilder("Material\tVerified input recipes\n");
        materials.forEach(
            (material, variants) -> table.append(material)
                .append('\t')
                .append(variants.size())
                .append('\n'));
        File folder = new File(System.getProperty("gtng.oreSifting.output")).getParentFile();
        Files.write(
            new File(folder, "byproduct-coverage.tsv").toPath(),
            table.toString()
                .getBytes(StandardCharsets.UTF_8));
        return "Audited mechanical materials=" + materials.size()
            + "; input recipes="
            + covered.size()
            + "; native source recipes="
            + eligibleSources.size()
            + "; verified product groups="
            + verifiedGroups
            + "; excluded intermediate source recipes="
            + excludedSources.size()
            + "\n";
    }

    private static boolean eligibleSource(GTRecipe source) {
        return source.mEnabled && source.mInputs.length == 1
            && source.mInputs[0] != null
            && source.mInputs[0].stackSize == 1
            && source.mFluidInputs.length == 0
            && source.mFluidOutputs.length == 0
            && source.mSpecialItems == null;
    }

    private static String[] registeredForm(ItemStack input) {
        String[] prefixes = { "crushedCentrifuged", "crushedPurified", "crushed", "dustImpure", "dustPure", "rawOre",
            "oreNetherrack", "oreEndstone", "oreBlackgranite", "oreRedgranite", "oreBasalt", "oreMarble", "ore" };
        ItemData association = GTOreDictUnificator.getAssociation(input);
        for (int oreId : OreDictionary.getOreIDs(input)) {
            String name = OreDictionary.getOreName(oreId);
            for (String prefix : prefixes) {
                if (!name.startsWith(prefix)) continue;
                String material = name.substring(prefix.length());
                if (!material.isEmpty() && (association == null || association.mMaterial == null
                    || association.mMaterial.mMaterial == null
                    || material.equals(association.mMaterial.mMaterial.mName)))
                    return new String[] { prefix, material };
                break;
            }
        }
        return null;
    }

    private static boolean isIntermediate(String form) {
        return form.startsWith("crushed") || form.equals("dustImpure") || form.equals("dustPure");
    }

    private static String[] remainingStages(String form) {
        switch (form) {
            case "crushedPurified":
                return new String[] { "crushedPurified", "crushedCentrifuged", "dustPure" };
            case "crushedCentrifuged":
            case "dustImpure":
            case "dustPure":
                return new String[] { form };
            default:
                return new String[] { "crushed", "crushedPurified", "crushedCentrifuged", "dustImpure", "dustPure" };
        }
    }

    private static boolean siftingPriority(String[] form, ItemStack product, int multiplier) {
        if (form[0].equals("crushedCentrifuged") || form[0].startsWith("dust")) return false;
        for (GTRecipe source : RecipeMaps.sifterRecipes.getAllRecipes()) {
            if (!eligibleSource(source)) continue;
            String[] sourceForm = registeredForm(source.mInputs[0]);
            if (sourceForm == null || !sourceForm[0].equals("crushedPurified") || !sourceForm[1].equals(form[1]))
                continue;
            for (int slot = 0; slot < source.mOutputs.length; slot++) {
                ItemStack output = source.mOutputs[slot];
                if (output != null && output.stackSize > 0
                    && source.getOutputChance(slot) > 0
                    && covers(
                        new ItemStack[] { GTUtility.copyAmountUnsafe(output.stackSize * multiplier, output) },
                        product))
                    return true;
            }
        }
        return false;
    }

    private static boolean covers(ItemStack[] outputs, ItemStack product) {
        ItemData expected = GTOreDictUnificator.getAssociation(product);
        for (ItemStack output : outputs) {
            if (output == null) continue;
            if (GTUtility.areStacksEqual(output, product)) return true;
            ItemData existing = GTOreDictUnificator.getAssociation(output);
            if (expected == null || existing == null || expected.mMaterial == null || existing.mMaterial == null)
                continue;
            if (dustPrefix(expected.mPrefix) && dustPrefix(existing.mPrefix)
                && expected.mMaterial.mMaterial == existing.mMaterial.mMaterial
                && (long) output.stackSize * existing.mPrefix.getMaterialAmount()
                    >= (long) product.stackSize * expected.mPrefix.getMaterialAmount())
                return true;
        }
        return false;
    }

    private static boolean dustPrefix(OrePrefixes prefix) {
        return prefix == OrePrefixes.dust || prefix == OrePrefixes.dustSmall || prefix == OrePrefixes.dustTiny;
    }

    private static boolean gangue(ItemStack product) {
        for (int oreId : OreDictionary.getOreIDs(product)) {
            String name = OreDictionary.getOreName(oreId);
            if (name.matches("dust(?:Small|Tiny)?(?:Stone|Netherrack|Endstone)")) return true;
        }
        return false;
    }

    private ItemStack stack(int meta, int amount) {
        if (meta == 22) return Materials.Nickel.getDust(amount);
        if (meta == 47) return GTOreDictUnificator.get(OrePrefixes.dustSmall, Materials.Nickel, amount);
        if (meta == 48) return GTOreDictUnificator.get(OrePrefixes.dustTiny, Materials.Nickel, amount);
        return new ItemStack(fixture, amount, meta);
    }

    private static GTRecipe recipe(ItemStack input) {
        GTRecipe recipe = GTNGRecipeMaps.OreProcessingRecipes.findRecipeQuery()
            .items(input)
            .notUnificated(true)
            .find();
        require(recipe != null, "Ore processor recipe: " + input);
        return recipe;
    }

    private static int output(GTRecipe recipe, ItemStack item, int amount, int chance) {
        int count = 0;
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            if (GTUtility.areStacksEqual(recipe.mOutputs[i], item) && recipe.mOutputs[i].stackSize == amount
                && recipe.getOutputChance(i) == chance) count++;
        }
        return count;
    }

    private static int slots(GTRecipe recipe, ItemStack item) {
        int count = 0;
        for (ItemStack output : recipe.mOutputs) if (GTUtility.areStacksEqual(output, item)) count++;
        return count;
    }

    private static String itemKey(ItemStack stack) {
        return Item.getIdFromItem(stack.getItem()) + ":" + stack.getItemDamage() + ":" + stack.getTagCompound();
    }

    /** Captures quantity, order, chance, fluids and special slot so reload assertions detect more than slot counts. */
    private static String recipeState(GTRecipe recipe) {
        StringBuilder state = new StringBuilder();
        for (ItemStack[] stacks : new ItemStack[][] { recipe.mInputs, recipe.mOutputs }) {
            state.append('[');
            for (ItemStack stack : stacks) {
                state.append(stack == null ? "null" : itemKey(stack) + "*" + stack.stackSize)
                    .append(';');
            }
            state.append(']');
        }
        for (int slot = 0; slot < recipe.mOutputs.length; slot++) {
            state.append(recipe.getOutputChance(slot))
                .append(';');
        }
        for (FluidStack[] stacks : new FluidStack[][] { recipe.mFluidInputs, recipe.mFluidOutputs }) {
            state.append('[');
            for (FluidStack stack : stacks) {
                state.append(
                    stack == null ? "null"
                        : stack.getFluid()
                            .getName() + "*"
                            + stack.amount
                            + ":"
                            + stack.tag)
                    .append(';');
            }
            state.append(']');
        }
        return state.append(recipe.mEnabled)
            .append(':')
            .append(recipe.mDuration)
            .append(':')
            .append(recipe.mEUt)
            .append(':')
            .append(recipe.mSpecialItems)
            .toString();
    }

    private static long amount(ArrayList<ItemStack> outputs, ItemStack item) {
        return outputs.stream()
            .filter(s -> GTUtility.areStacksEqual(s, item))
            .mapToLong(s -> s.stackSize)
            .sum();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
