package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.machines.hatch.modular.ModularHatch;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.GTMod;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.AssemblyLineUtils;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTScannerResult;
import gregtech.api.util.GTUtility;
import gregtech.loaders.postload.ScannerHandlerLoader;
import gregtech.nei.GTNEIDefaultHandler;

@Mod(
    modid = "corerecipesqa",
    name = "Core Recipes QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class CoreRecipeClientChecks {

    private final StringBuilder report = new StringBuilder();
    private boolean finished;
    private int ticks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.coreRecipes.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F);
        mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0F);
        mc.gameSettings.limitFramerate = 30;
        if (mc.currentScreen == null || ++ticks < 40) return;
        try {
            ItemStack flowCircuit = ModsItemlist.HighEnergyFlowCircuit.get(1);
            require(GTUtility.isStackValid(flowCircuit),
                "Default runtime registers the native high energy flow circuit");
            verifyModuleInventory();
            GTMod.proxy.reloadNEICache();
            verifyCore(GTNGItemList.ExecutionCore, GTNGItemList.StaticParallelControllerT1, 24, 240, TierEU.RECIPE_UV,
                flowCircuit, 36, new int[] { 1, 16, 12, 18, 12, 1, 1, 2, 36, 32, 6, 16, 32, 16 },
                new int[] { 9216, 9216, 147456, 18432 });
            verifyCore(GTNGItemList.AdvancedExecutionCore, GTNGItemList.ExecutionCore, 48, 1200, TierEU.RECIPE_UMV,
                flowCircuit, 64, new int[] { 1, 64, 32, 48, 32, 48, 8, 64, 64, 64, 16, 64, 64, 64, 64, 64 },
                new int[] { 2359296, 2359296, 9437184, 73728 });
            verifyCore(GTNGItemList.PerfectExecutionCore, GTNGItemList.AdvancedExecutionCore, 96, 3600,
                TierEU.RECIPE_UXV, flowCircuit, 0,
                new int[] { 1, 64, 48, 64, 48, 16, 16, 64, 64, 64, 64, 64, 64, 64, 64, 64 },
                new int[] { 3538944, 7077888, 14155776, 589824 });
            verifyLowSpeedOverclock(flowCircuit);
            report.append("Default Gradle runtime dependencies only; no QA mod dependency injection.\n");
            Files.write(new File(output(), "result.txt").toPath(),
                ("PASS\n" + report).getBytes(StandardCharsets.UTF_8));
            System.out.println("CORE_RECIPES_QA_PASS " + report);
            finished = true;
            mc.shutdown();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void verifyModuleInventory() {
        int modules = 0;
        for (IMetaTileEntity prototype : GregTechAPI.METATILEENTITIES) if (prototype instanceof ModularHatch hatch) {
            ItemStack output = hatch.getStackForm(1);
            long assembler = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
                .filter(recipe -> hasOutput(recipe, output)).count();
            long assemblyLine = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
                .filter(recipe -> GTUtility.areStacksEqual(recipe.mOutput, output)).count();
            require(assembler + assemblyLine == 1, "One survival recipe for module " + output.getItemDamage());
            modules++;
        }
        require(modules == 46, "All 46 modules register");
        report.append("All 46 modules retain exactly one original survival recipe.\n");
    }

    private void verifyCore(GTNGItemList output, GTNGItemList research, int hours, int seconds, long eut,
        ItemStack flowCircuit, int flowAmount, int[] itemAmounts, int[] fluidAmounts) {
        ItemStack expected = output.get(1);
        List<GTRecipe.RecipeAssemblyLine> recipes = GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.stream()
            .filter(recipe -> GTUtility.areStacksEqual(recipe.mOutput, expected)).collect(Collectors.toList());
        require(recipes.size() == 1, "Exactly one native assembly-line recipe: " + output);
        GTRecipe.RecipeAssemblyLine recipe = recipes.get(0);
        require(GTUtility.areStacksEqual(recipe.mResearchItem, research.get(1))
            && recipe.mResearchTime == hours * GTRecipeBuilder.HOURS
            && recipe.mResearchVoltage == 30, "Original research gate: " + output);
        require(recipe.mDuration == seconds * 20 && recipe.mEUt == eut, "Original processing gate: " + output);
        require(Arrays.equals(itemAmounts, Arrays.stream(recipe.mInputs).mapToInt(stack -> stack.stackSize).toArray()),
            "Original item amounts: " + output);
        require(
            Arrays.equals(fluidAmounts, Arrays.stream(recipe.mFluidInputs).mapToInt(fluid -> fluid.amount).toArray()),
            "Original fluid amounts: " + output);
        require(amount(recipe.mInputs, flowCircuit) == flowAmount, "Native high energy flow circuit count: " + output);
        verifyScanner(recipe, expected);
        verifyNei(RecipeMaps.assemblylineVisualRecipes, expected, recipe.mInputs.length, recipe.mFluidInputs.length);
        report.append(output).append(": native AL and NEI recipe, research=").append(research).append(", scanning=")
            .append(hours).append("h at 30 EU/t, processing=").append(seconds).append("s at ").append(eut)
            .append(" EU/t, native flow circuits=").append(flowAmount).append("; exact item/fluid counts pass.\n");
    }

    private void verifyLowSpeedOverclock(ItemStack flowCircuit) {
        ItemStack output = GTNGItemList.LowSpeedPerfectOverclockController.get(1);
        List<GTRecipe> recipes = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
            .filter(recipe -> hasOutput(recipe, output)).collect(Collectors.toList());
        require(recipes.size() == 1, "Exactly one low-speed overclock assembler recipe");
        GTRecipe recipe = recipes.get(0);
        require(
            amount(recipe.mInputs, flowCircuit) == 64 && recipe.mDuration == 20 * 336
                && recipe.mEUt == TierEU.RECIPE_UIV,
            "Low-speed overclock keeps its native circuit and processing gates");
        require(Arrays.stream(recipe.mInputs).anyMatch(
            stack -> GTUtility.isAnyIntegratedCircuit(stack) && stack.getItemDamage() == 19 && stack.stackSize == 0),
            "Module selector remains non-consumed circuit 19");
        verifyNei(RecipeMaps.assemblerRecipes, output, recipe.mInputs.length, recipe.mFluidInputs.length);
        report.append("Low-speed perfect overclock: assembler and NEI recipe; 64 native flow circuits, 336s, UIV, ")
            .append("non-consumed circuit 19.\n");
    }

    private static void verifyScanner(GTRecipe.RecipeAssemblyLine recipe, ItemStack expected) {
        MetaTileEntity scanner = (MetaTileEntity) GregTechAPI.METATILEENTITIES[ItemList.Machine_LV_Scanner.get(1)
            .getItemDamage()];
        GTScannerResult result = ScannerHandlerLoader.doAssemblyLineResearch(scanner, recipe.mResearchItem.copy(),
            ItemList.Tool_DataStick.get(1), null);
        require(result instanceof GTScannerResult.ALScannerResult
            && ((GTScannerResult.ALScannerResult) result).alRecipe == recipe
            && result.duration == recipe.mResearchTime
            && result.eut == recipe.mResearchVoltage, "Native scanner resolves the original research gate");
        require(GTUtility.areStacksEqual(AssemblyLineUtils.getDataStickOutput(result.output), expected),
            "Native scanner data stick identifies the correct core");
        Collection<GTRecipe.RecipeAssemblyLine> selected = AssemblyLineUtils.findALRecipeFromDataStick(result.output);
        require(selected != null && selected.size() == 1 && selected.contains(recipe),
            "Native data-stick NBT resolves exactly the intended core recipe");
    }

    private static void verifyNei(RecipeMap<?> map, ItemStack expected, int items, int fluids) {
        GTNEIDefaultHandler handler = new GTNEIDefaultHandler(map.getDefaultRecipeCategory());
        handler.loadCraftingRecipes(expected);
        require(handler.arecipes.size() == 1, "Native NEI output lookup finds exactly one recipe: " + expected);
        GTRecipe recipe = ((GTNEIDefaultHandler.CachedDefaultRecipe) handler.arecipes.get(0)).mRecipe;
        require(hasOutput(recipe, expected) && recipe.mInputs.length == items && recipe.mFluidInputs.length == fluids,
            "Native NEI exposes the correct output and all input slots");
    }

    private static boolean hasOutput(GTRecipe recipe, ItemStack expected) {
        return Arrays.stream(recipe.mOutputs)
            .anyMatch(output -> output != null && output.stackSize == 1 && GTUtility.areStacksEqual(output, expected));
    }

    private static int amount(ItemStack[] inputs, ItemStack expected) {
        return Arrays.stream(inputs).filter(input -> GTUtility.areStacksEqual(input, expected))
            .mapToInt(input -> input.stackSize).sum();
    }

    private static File output() {
        File directory = new File(System.getProperty("gtng.coreRecipes.output"));
        directory.mkdirs();
        return directory;
    }

    private void fail(Throwable failure) {
        finished = true;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
