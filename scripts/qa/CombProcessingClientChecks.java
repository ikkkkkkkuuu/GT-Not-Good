package com.xyp.gtnotgood.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.dreammaster.lib.Refstrings;
import com.xyp.gtnotgood.common.machines.multiblock.LargeCombProcessor;
import com.xyp.gtnotgood.common.recipe.gtnotgood.CombProcessingRecipes;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.items.CombType;
import gregtech.loaders.misc.GTBees;
import gtPlusPlus.core.material.MaterialsElements;
import gtPlusPlus.xmod.forestry.bees.handler.GTPPCombType;

@Mod(
    modid = "combprocessingqa",
    name = "Comb Processing QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD + ";after:" + Refstrings.MODID)
public final class CombProcessingClientChecks {

    private RecipeMap<?> target;
    private List<RecipeMap<?>> nativeMaps;
    private final List<String> report = new ArrayList<>();
    private boolean complete;
    private boolean finished;
    private int ticks;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.combProcessing.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @Mod.EventHandler
    public void complete(FMLLoadCompleteEvent event) {
        complete = true;
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings != null) mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0);
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            if (++ticks > 2400) throw new AssertionError("Comb QA did not reach the completed Forge load state");
            if (!complete || !(mc.currentScreen instanceof GuiMainMenu)) return;
            verify();
            finish("PASS; " + checks + " checks; all seven combs have one usable processor recipe");
        } catch (Throwable error) {
            error.printStackTrace();
            finish("FAIL; " + error);
        }
    }

    private void verify() {
        require(GregTechAPI.sFullLoadFinished, "checks run after Forge loadComplete and GregTech recipe loading");
        target = GTNGRecipeMaps.CombProcessingRecipes;
        nativeMaps = Arrays.asList(RecipeMaps.centrifugeRecipes, RecipeMaps.multiblockChemicalReactorRecipes,
            RecipeMaps.chemicalReactorRecipes, RecipeMaps.fluidExtractionRecipes, RecipeMaps.autoclaveRecipes,
            RecipeMaps.chemicalPlantRecipes);
        require(Loader.isModLoaded(Refstrings.MODID), "the real NewHorizonsCoreMod is present during recipe loading");
        report.add(
            "NewHorizonsCoreMod version: " + Loader.instance().getIndexedModList().get(Refstrings.MODID).getVersion());
        require(Config.recipeSpeedMode == 0, "the isolated baseline disables global duration acceleration");
        require(new LargeCombProcessor("combProcessingQa").getRecipeMap() == target,
            "the actual LargeCombProcessor uses the tested recipe map");
        require(GTBees.combs != null, "the real GregTech comb item is registered");
        Map<String, List<String>> nativeSnapshot = nativeSnapshot();
        Set<Object> nativeStacks = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RecipeMap<?> map : nativeMaps) {
            for (GTRecipe recipe : map.getAllRecipes()) {
                Collections.addAll(nativeStacks, recipe.mInputs);
                Collections.addAll(nativeStacks, recipe.mOutputs);
                Collections.addAll(nativeStacks, recipe.mFluidInputs);
                Collections.addAll(nativeStacks, recipe.mFluidOutputs);
            }
        }
        verifyComb("Prismatic", GTBees.combs.getStackForType(CombType.PRISMATIC),
            GTOreDictUnificator.get(OrePrefixes.shard, Materials.Prismarine, 1), null, nativeStacks);
        verifyComb("Netherite", GTBees.combs.getStackForType(CombType.NETHERITE),
            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Netherite, 1), null, nativeStacks);
        verifyComb("Kevlar", GTBees.combs.getStackForType(CombType.KEVLAR), null, Materials.Kevlar.getMolten(144),
            nativeStacks);
        verifyComb("Cosmic Neutronium", GTBees.combs.getStackForType(CombType.COSMICNEUTRONIUM),
            GTOreDictUnificator.get(OrePrefixes.dust, Materials.CosmicNeutronium, 1), null, nativeStacks);
        verifyComb("Infinity", GTBees.combs.getStackForType(CombType.INFINITY),
            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Infinity, 1), null, nativeStacks);
        verifyComb("Dragonblood", GTPPCombType.DRAGONBLOOD.getStackForType(1),
            MaterialsElements.STANDALONE.DRAGON_METAL.getDust(1), null, nativeStacks);
        verifyComb("Force", GTPPCombType.FORCE.getStackForType(1), MaterialsElements.STANDALONE.FORCE.getDust(1), null,
            nativeStacks);
        List<String> processorSnapshot = snapshot(target);
        CombProcessingRecipes.loadRecipes();
        CombProcessingRecipes.loadRecipes();
        require(processorSnapshot.equals(snapshot(target)), "repeated recipe loading changes no processor recipes");
        require(nativeSnapshot.equals(nativeSnapshot()),
            "matching, consumption copies and reloads leave native recipes unchanged");
        report.add("Processor recipes: " + target.getAllRecipes().size());
        for (RecipeMap<?> map : nativeMaps) {
            report.add("Native recipe snapshot: " + map.unlocalizedName + " = " + map.getAllRecipes().size());
        }
    }

    private void verifyComb(String name, ItemStack comb, ItemStack expectedItem, FluidStack expectedFluid,
        Set<Object> nativeStacks) {
        require(comb != null && comb.getItem() != null, name + " uses a registered comb item");
        require(expectedItem != null || expectedFluid != null, name + " primary material is registered");
        List<GTRecipe> matches = target.getAllRecipes().stream()
            .filter(recipe -> Arrays.stream(recipe.mInputs).anyMatch(input -> GTUtility.areStacksEqual(input, comb)))
            .collect(Collectors.toList());
        require(matches.size() == 1, name + " has exactly one processor recipe");
        GTRecipe recipe = matches.get(0);
        report.add(name + ": " + signature(recipe));
        require(recipe.mEnabled && !recipe.mFakeRecipe && recipe.mDuration > 0 && recipe.mEUt > 0,
            name + " recipe is enabled with positive processing time and power");
        require(recipe.mInputs.length == 1 && recipe.mInputs[0].stackSize == 1 && recipe.mFluidInputs.length == 0,
            name + " consumes one comb with no other items or fluids");
        require(
            Arrays.stream(recipe.mOutputs).anyMatch(stack -> stack != null && stack.stackSize > 0)
                || Arrays.stream(recipe.mFluidOutputs).anyMatch(fluid -> fluid != null && fluid.amount > 0),
            name + " has a usable material output");
        if (expectedItem != null) {
            require(
                recipe.mOutputs.length == 1 && recipe.mFluidOutputs.length == 0
                    && GTUtility.areStacksEqual(recipe.mOutputs[0], expectedItem)
                    && recipe.mOutputs[0].stackSize == expectedItem.stackSize
                    && recipe.getOutputChance(0) == 10000,
                name + " yields exactly one guaranteed primary material with no byproducts");
        } else {
            require(
                recipe.mOutputs.length == 0 && recipe.mFluidOutputs.length == 1
                    && recipe.mFluidOutputs[0].isFluidEqual(expectedFluid)
                    && recipe.mFluidOutputs[0].amount == expectedFluid.amount,
                name + " yields exactly 144 mB of molten Kevlar with no byproducts");
        }
        require(recipe.mEUt == 30 && recipe.mDuration == 100,
            name + " uses the standard simplified processing baseline of 30 EU/t for five seconds");
        List<Object> stacks = new ArrayList<>();
        Collections.addAll(stacks, recipe.mInputs);
        Collections.addAll(stacks, recipe.mOutputs);
        Collections.addAll(stacks, recipe.mFluidInputs);
        Collections.addAll(stacks, recipe.mFluidOutputs);
        require(stacks.stream().filter(stack -> stack != null).noneMatch(nativeStacks::contains),
            name + " recipe stacks do not alias mutable native recipe stacks");
        require(find(inventory(recipe.mInputs), fluids(recipe.mFluidInputs)) == recipe,
            name + " is returned by the real recipe map query with the actual registered inputs");
        ItemStack[] shortage = inventory(recipe.mInputs);
        shortage[0] = null;
        require(find(shortage, fluids(recipe.mFluidInputs)) != recipe, name + " cannot run when its comb is missing");
        require(!recipe.isRecipeInputEqual(false, false, fluids(recipe.mFluidInputs), shortage),
            name + " native quantity checking rejects the missing comb");
        ItemStack[] consumed = inventory(recipe.mInputs);
        require(
            recipe.isRecipeInputEqual(true, false, fluids(recipe.mFluidInputs), consumed) && consumed[0].stackSize == 0,
            name + " native consumption consumes exactly one comb");
    }

    private GTRecipe find(ItemStack[] items, FluidStack[] fluids) {
        return target.findRecipeQuery().items(inventory(items)).fluids(fluids(fluids)).find();
    }

    private static ItemStack[] inventory(ItemStack[] inputs) {
        return Arrays.stream(inputs).map(stack -> stack == null ? null : stack.copy()).toArray(ItemStack[]::new);
    }

    private static FluidStack[] fluids(FluidStack[] inputs) {
        return Arrays.stream(inputs).map(stack -> stack == null ? null : stack.copy()).toArray(FluidStack[]::new);
    }

    private Map<String, List<String>> nativeSnapshot() {
        Map<String, List<String>> result = new TreeMap<>();
        for (RecipeMap<?> map : nativeMaps) result.put(map.unlocalizedName, snapshot(map));
        return result;
    }

    private static List<String> snapshot(RecipeMap<?> map) {
        return map.getAllRecipes().stream().map(CombProcessingClientChecks::signature).sorted()
            .collect(Collectors.toList());
    }

    private static String signature(GTRecipe recipe) {
        return "items=" + items(recipe.mInputs)
            + ";fluids="
            + fluidSignature(recipe.mFluidInputs)
            + ";outputs="
            + items(recipe.mOutputs)
            + ";chances="
            + Arrays.toString(recipe.mOutputChances)
            + ";fluidOutputs="
            + fluidSignature(recipe.mFluidOutputs)
            + ";duration="
            + recipe.mDuration
            + ";eut="
            + recipe.mEUt
            + ";enabled="
            + recipe.mEnabled
            + ";fake="
            + recipe.mFakeRecipe;
    }

    private static String items(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(stack -> stack == null ? "null"
            : Item.itemRegistry.getNameForObject(
                stack.getItem()) + ":" + stack.getItemDamage() + " x" + stack.stackSize + " " + stack.getTagCompound())
            .collect(Collectors.joining(",", "[", "]"));
    }

    private static String fluidSignature(FluidStack[] stacks) {
        return Arrays.stream(stacks)
            .map(stack -> stack == null ? "null" : stack.getFluid().getName() + " x" + stack.amount + " " + stack.tag)
            .collect(Collectors.joining(",", "[", "]"));
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        report.add(message);
        System.out.println("COMB_PROCESSING_QA: " + message);
    }

    private void finish(String result) {
        try {
            File output = new File(System.getProperty("gtng.combProcessing.qa.output", "."));
            Files.write(new File(output, "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
            Files.write(new File(output, "checks.txt").toPath(), report, StandardCharsets.UTF_8);
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("COMB_PROCESSING_QA: " + result);
        finished = true;
        FMLCommonHandler.instance().exitJava(result.startsWith("PASS") ? 0 : 1, false);
    }
}
