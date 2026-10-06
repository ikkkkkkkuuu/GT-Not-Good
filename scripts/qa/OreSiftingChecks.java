package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

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
    private int ticks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.oreSifting.qa")) return;
        diamondBefore = recipe(GTOreDictUnificator.get(OrePrefixes.ore, Materials.Diamond, 1)).copy();
        graphiteBefore = recipe(GTOreDictUnificator.get(OrePrefixes.ore, Materials.Graphite, 1)).copy();
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
        FMLCommonHandler.instance()
            .bus()
            .register(this);
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
        int recipesBefore = GTNGRecipeMaps.OreProcessingRecipes.getAllRecipes()
            .size();
        OreProcessingRecipes.loadExternalOreRecipes();
        require(thorianite.mOutputs.length == 4, "Repeated loading does not duplicate products");
        require(
            recipesBefore == GTNGRecipeMaps.OreProcessingRecipes.getAllRecipes()
                .size(),
            "Repeated loading does not duplicate recipes");

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
        String coverage = auditSiftingCoverage();
        String report = "PASS\nThorianite dust retained; native 6%, 3%, 1% slots; raw/crushed/purified forms; rich ores; "
            + "downstream exclusions; repeated loading; independent batch rolls; zero chance; long output quantities.\n"
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

    private ItemStack stack(int meta, int amount) {
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
