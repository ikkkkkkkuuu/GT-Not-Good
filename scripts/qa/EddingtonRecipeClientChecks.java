package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.dreammaster.lib.Refstrings;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

@Mod(
    modid = "eddingtonrecipeqa",
    name = "Eddington Recipe QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD + ";after:" + Refstrings.MODID)
public final class EddingtonRecipeClientChecks {

    private boolean complete;
    private boolean finished;
    private int ticks;
    private int checks;
    private int scanned;
    private Method collisionStream;
    private final List<String> report = new ArrayList<>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.eddingtonRecipe.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @Mod.EventHandler
    public void complete(FMLLoadCompleteEvent event) {
        complete = true;
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            if (++ticks > 2400) throw new AssertionError("Recipe QA did not reach the completed Forge load state");
            if (!complete || !(mc.currentScreen instanceof GuiMainMenu)) return;
            verify();
            finish("PASS; " + checks + " recipe checks; scanned " + scanned + " loaded assembler recipes");
        } catch (Throwable error) {
            error.printStackTrace();
            finish("FAIL; " + error);
        }
    }

    private void verify() throws Exception {
        require(GregTechAPI.sFullLoadFinished, "checks run after Forge loadComplete and GregTech recipe loading");
        require(Loader.isModLoaded(Refstrings.MODID), "the real NewHorizonsCoreMod is present during recipe loading");
        report.add(
            "NewHorizonsCoreMod version: " + Loader.instance().getIndexedModList().get(Refstrings.MODID).getVersion());
        List<GTRecipe> recipes = new ArrayList<>(RecipeMaps.assemblerRecipes.getAllRecipes());
        List<String> snapshot = recipes.stream().map(EddingtonRecipeClientChecks::signature)
            .collect(Collectors.toList());
        List<GTRecipe> matches = recipes.stream().filter(this::outputsCasing).collect(Collectors.toList());
        require(matches.size() == 1, "exactly one loaded assembler recipe outputs the Eddington wall casing");
        GTRecipe recipe = matches.get(0);
        report.add("Actual recipe EU/t=" + recipe.mEUt
            + ", duration="
            + recipe.mDuration
            + ", TierEU.RECIPE_MV="
            + TierEU.RECIPE_MV
            + ", recipeSpeedMode="
            + Config.recipeSpeedMode);
        require(Config.recipeSpeedMode == 0, "the isolated recipe baseline disables global duration acceleration");
        require(recipe.mOutputs.length == 1 && recipe.mOutputs[0].stackSize == 2,
            "the recipe outputs exactly two Eddington walls and no byproducts");
        require(recipe.mEUt == TierEU.RECIPE_MV && recipe.mEUt == 120 && recipe.mDuration == 200,
            "the recipe requires MV 120 EU/t for exactly ten seconds");
        require(recipe.mInputs.length == 4 && recipe.mFluidInputs.length == 1,
            "the recipe has three material inputs, one circuit selector and one fluid input");
        requireAmount(recipe, GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 1), 6, "steel plates");
        requireAmount(recipe, GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 1), 1, "steel frame");
        requireAmount(recipe, GTOreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 1), 4, "copper foils");
        requireAmount(recipe, GTUtility.getIntegratedCircuit(24), 0, "nonconsumed integrated circuit 24");
        require(
            recipe.mFluidInputs[0].isFluidEqual(Materials.Tin.getMolten(144)) && recipe.mFluidInputs[0].amount == 144,
            "the recipe consumes exactly 144 mB of molten tin");
        ItemStack[] items = inventory(recipe.mInputs);
        FluidStack[] fluids = fluids(recipe.mFluidInputs);
        require(find(items, fluids, GTValues.V[1]) == null, "a native LV assembler lookup cannot run the recipe");
        require(find(items, fluids, GTValues.V[2]) == recipe, "a native MV assembler lookup returns this exact recipe");
        require(find(items, fluids, GTValues.V[3]) == recipe, "higher-voltage native lookup retains the recipe");
        for (int input = 0; input < items.length; input++) {
            ItemStack[] shortItems = inventory(recipe.mInputs);
            shortItems[input].stackSize--;
            if (shortItems[input].stackSize == 0) shortItems[input] = null;
            require(!recipe.isRecipeInputEqual(false, false, fluids(fluids), shortItems),
                "native quantity checking rejects a shortage in input " + input);
        }
        require(find(items, new FluidStack[] { Materials.Tin.getMolten(143) }, GTValues.V[2]) != recipe,
            "the native assembler refuses a one-millibucket tin shortage");
        ItemStack[] wrongCircuit = inventory(recipe.mInputs);
        for (int input = 0; input < wrongCircuit.length; input++) {
            if (GTUtility.areStacksEqual(wrongCircuit[input], GTUtility.getIntegratedCircuit(24))) {
                wrongCircuit[input] = GTUtility.getIntegratedCircuit(23);
                wrongCircuit[input].stackSize = 1;
            }
        }
        require(find(wrongCircuit, fluids, GTValues.V[2]) != recipe, "circuit 23 cannot select the Eddington recipe");
        ItemStack[] consumed = inventory(recipe.mInputs);
        FluidStack[] consumedFluids = fluids(recipe.mFluidInputs);
        require(recipe.isRecipeInputEqual(true, false, consumedFluids, consumed),
            "the actual GT consumption routine accepts one complete batch");
        for (ItemStack stack : consumed) {
            boolean circuit = GTUtility.areStacksEqual(stack, GTUtility.getIntegratedCircuit(24));
            require(stack.stackSize == (circuit ? 1 : 0),
                circuit ? "the circuit selector remains in the machine" : "material input is consumed exactly once");
        }
        require(consumedFluids[0].amount == 0, "one batch consumes exactly the supplied molten tin");
        scanCollisions(recipe, recipes);
        require(
            recipes.size() == RecipeMaps.assemblerRecipes.getAllRecipes().size() && snapshot
                .equals(recipes.stream().map(EddingtonRecipeClientChecks::signature).collect(Collectors.toList())),
            "read-only native matching leaves every loaded assembler recipe unchanged");
    }

    private void scanCollisions(GTRecipe target, List<GTRecipe> recipes) throws Exception {
        collisionStream = RecipeMapBackend.class.getDeclaredMethod("matchRecipeStream", ItemStack[].class,
            FluidStack[].class, ItemStack.class, GTRecipe.class, boolean.class, boolean.class, boolean.class);
        collisionStream.setAccessible(true);
        List<GTRecipe> forward = collisions(inventory(target.mInputs), fluids(target.mFluidInputs));
        require(forward.size() == 1 && forward.get(0) == target,
            "native forCollisionCheck lookup ignores quantities and finds no other recipe for Eddington inputs");
        List<String> conflicts = new ArrayList<>();
        int skipped = 0;
        for (GTRecipe other : recipes) {
            scanned++;
            if (other == target || !other.mEnabled || other.mFakeRecipe) {
                skipped++;
                continue;
            }
            boolean existingAccepts = other.isRecipeInputEqual(false, true, fluids(target.mFluidInputs),
                inventory(target.mInputs));
            boolean targetAccepts = target.isRecipeInputEqual(false, true, fluids(other.mFluidInputs),
                inventory(other.mInputs));
            boolean nativeReverse = collisions(inventory(other.mInputs), fluids(other.mFluidInputs)).contains(target);
            if (existingAccepts || targetAccepts || nativeReverse) {
                conflicts.add("existingAccepts=" + existingAccepts
                    + ", targetAccepts="
                    + targetAccepts
                    + ", nativeReverse="
                    + nativeReverse
                    + ", "
                    + signature(other));
            }
        }
        report.add("Assembler recipes scanned: " + scanned);
        report.add("Skipped target/fake/disabled: " + skipped);
        report.add("Bidirectional quantity-independent conflicts: " + conflicts.size());
        report.addAll(conflicts);
        require(conflicts.isEmpty(),
            "all " + scanned + " loaded assembler recipes pass bidirectional quantity-independent collision checks");
    }

    @SuppressWarnings("unchecked")
    private List<GTRecipe> collisions(ItemStack[] items, FluidStack[] fluids) throws Exception {
        try (Stream<GTRecipe> stream = (Stream<GTRecipe>) collisionStream
            .invoke(RecipeMaps.assemblerRecipes.getBackend(), items, fluids, null, null, true, true, true)) {
            return stream.collect(Collectors.toList());
        }
    }

    private static GTRecipe find(ItemStack[] items, FluidStack[] fluids, long voltage) {
        return RecipeMaps.assemblerRecipes.findRecipeQuery().items(inventory(items)).fluids(fluids(fluids))
            .voltage(voltage).find();
    }

    private boolean outputsCasing(GTRecipe recipe) {
        return Arrays.stream(recipe.mOutputs).anyMatch(
            stack -> stack != null && GTUtility.areStacksEqual(stack, GTNGItemList.EddingtonWallCasing.get(1)));
    }

    private void requireAmount(GTRecipe recipe, ItemStack expected, int amount, String description) {
        List<ItemStack> found = Arrays.stream(recipe.mInputs).filter(stack -> GTUtility.areStacksEqual(stack, expected))
            .collect(Collectors.toList());
        require(found.size() == 1 && found.get(0).stackSize == amount,
            "registered input matches " + amount + " " + description);
    }

    private static ItemStack[] inventory(ItemStack[] inputs) {
        if (inputs == null) return new ItemStack[0];
        return Arrays.stream(inputs).map(stack -> {
            if (stack == null) return null;
            ItemStack copy = stack.copy();
            if (copy.stackSize == 0) copy.stackSize = 1;
            return copy;
        }).toArray(ItemStack[]::new);
    }

    private static FluidStack[] fluids(FluidStack[] inputs) {
        if (inputs == null) return new FluidStack[0];
        return Arrays.stream(inputs).map(stack -> stack == null ? null : stack.copy()).toArray(FluidStack[]::new);
    }

    private static String signature(GTRecipe recipe) {
        return "inputs=" + Arrays.toString(recipe.mInputs)
            + ";fluids="
            + Arrays.toString(recipe.mFluidInputs)
            + ";outputs="
            + Arrays.toString(recipe.mOutputs)
            + ";duration="
            + recipe.mDuration
            + ";eut="
            + recipe.mEUt
            + ";enabled="
            + recipe.mEnabled
            + ";fake="
            + recipe.mFakeRecipe;
    }

    private static File output() {
        return new File(System.getProperty("gtng.eddingtonRecipe.qa.output", "."));
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        report.add(message);
        System.out.println("EDDINGTON_RECIPE_QA: " + message);
    }

    private void finish(String result) {
        try {
            Files.write(new File(output(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
            Files.write(new File(output(), "checks.txt").toPath(), report, StandardCharsets.UTF_8);
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("EDDINGTON_RECIPE_QA: " + result);
        finished = true;
        FMLCommonHandler.instance().exitJava(result.startsWith("PASS") ? 0 : 1, false);
    }
}
