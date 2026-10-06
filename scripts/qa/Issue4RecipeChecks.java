package com.xyp.gtnotgood.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

/** Exercises the actual optional GTNL table and Forge lifecycle; excluded from release artifacts. */
@Mod(
    modid = "gtngissue4qa",
    name = "Issue 4 Recipe QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD + ";after:" + ModList.ModIds.GT_NOT_LEISURE)
public final class Issue4RecipeChecks {

    private Item fixture;
    private Item overrideFixture;
    private boolean ready;
    private int ticks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) throws Exception {
        if (System.getProperty("gtng.issue4.qa.mode") == null) return;
        fixture = new Item().setUnlocalizedName("issue4Fixture");
        GameRegistry.registerItem(fixture, "fixture");
        GTRecipeBuilder.builder()
            .itemInputs(new ItemStack(Items.stick, 3))
            .fluidInputs(new FluidStack(FluidRegistry.WATER, 250))
            .itemOutputs(input(2))
            .duration(100)
            .eut(32)
            .addTo(RecipeMaps.assemblerRecipes);
        if (System.getProperty("gtng.issue4.qa.mode")
            .equals("enabled")) {
            overrideFixture = new Item().setUnlocalizedName("issue4Override");
            GameRegistry.registerItem(overrideFixture, "override");
            GTRecipeBuilder.builder()
                .itemInputs(new ItemStack(Items.stick, 3))
                .itemOutputs(new ItemStack(overrideFixture))
                .duration(100)
                .eut(32)
                .addTo(RecipeMaps.assemblerRecipes);
            // The installed GTNL table must win over the ordinary assembler recovery.
            ObjectList<ItemStack> outputs = new ObjectArrayList<>();
            outputs.add(new ItemStack(Items.diamond, 2));
            Class.forName("com.science.gtnl.common.recipe.gtnl.ShimmerRecipes")
                .getMethod("registerConversion", ItemStack.class, ObjectList.class)
                .invoke(null, new ItemStack(overrideFixture), outputs);
        }
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @Mod.EventHandler
    public void complete(FMLLoadCompleteEvent event) {
        ready = fixture != null;
    }

    private void checkRecipes() throws Exception {
        if (fixture == null) return;
        String mode = System.getProperty("gtng.issue4.qa.mode");
        require(ModList.GTNotLeisure.isModLoaded() == !mode.equals("absent"), "GTNL presence");
        if (!mode.equals("absent")) {
            Class<?> config = Class.forName("com.science.gtnl.config.MainConfig");
            Object recipeConfig = config.getField("recipe")
                .get(null);
            require(
                recipeConfig.getClass()
                    .getField("enableShimmerDisassemblyRecipes")
                    .getBoolean(recipeConfig) == mode.equals("enabled"),
                "Shimmer config unchanged");
            Map<?, ?> conversions = (Map<?, ?>) Class.forName("com.science.gtnl.common.recipe.gtnl.ShimmerRecipes")
                .getField("conversionMap")
                .get(null);
            require(conversions.isEmpty() == mode.equals("disabled"), "Real GTNL conversion table state");
        }
        if (overrideFixture != null) {
            GTRecipe override = GTNGRecipeMaps.TransmutationRecipes.findRecipeQuery()
                .items(new ItemStack(overrideFixture))
                .find();
            require(
                override != null && override.mOutputs.length == 1
                    && override.mOutputs[0].getItem() == Items.diamond
                    && override.mOutputs[0].stackSize == 2,
                "GTNL override retained instead of local assembler recovery");
        }
        GTRecipe recovered = GTNGRecipeMaps.TransmutationRecipes.findRecipeQuery()
            .items(input(2))
            .find();
        require(recovered != null, "Generated assembler recovery is missing");
        require(recovered.mInputs[0].stackSize == 2, "Recovery batch size");
        require(
            recovered.mOutputs.length == 1 && recovered.mOutputs[0].getItem() == Items.stick
                && recovered.mOutputs[0].stackSize == 3,
            "Recovered item quantity");
        require(
            recovered.mFluidOutputs.length == 1 && recovered.mFluidOutputs[0].getFluid() == FluidRegistry.WATER
                && recovered.mFluidOutputs[0].amount == 250,
            "Recovered native fluid quantity");
        require(!recovered.isRecipeInputEqual(false, false, new FluidStack[0], input(1)), "Partial batch rejected");
        ItemStack batch = input(3);
        require(recovered.isRecipeInputEqual(true, false, new FluidStack[0], batch), "Full batch accepted");
        require(batch.stackSize == 1, "Batch remainder preserved");
        require(
            GTNGRecipeMaps.TransmutationRecipes.findRecipeQuery()
                .items(ItemList.Machine_LV_Macerator.get(1))
                .find() != null,
            "GT machine crafting recovery");

        ItemStack controller = GTNGItemList.LargeOreProcessor.get(1);
        GTRecipe assembly = RecipeMaps.assemblerRecipes.getAllRecipes()
            .stream()
            .filter(
                r -> Arrays.stream(r.mOutputs)
                    .anyMatch(s -> GTUtility.areStacksEqual(s, controller)))
            .findFirst()
            .orElse(null);
        require(assembly != null, "Ore processor controller recipe");
        ItemStack[] parts = { GTOreDictUnificator.get(OrePrefixes.plate, Materials.Copper, 4),
            GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 4),
            GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 4), GTUtility.getIntegratedCircuit(24) };
        require(assembly.mEUt == 32 && assembly.mDuration > 0, "LV assembler recipe");
        require(assembly.isRecipeInputEqual(false, false, new FluidStack[0], parts), "Original controller ingredients");
        require(
            RecipeMaps.assemblerRecipes.findRecipeQuery()
                .items(parts)
                .voltage(32)
                .find() == assembly,
            "Controller recipe available through assembler lookup");
        String report = "PASS mode=" + mode
            + "\n"
            + "Assembler and GT crafting recovery; native item/fluid quantities; partial batches and remainder; "
            + "LV ore controller recipe and ingredients; GTNL config unchanged.\n"
            + "Transmutation recipes="
            + GTNGRecipeMaps.TransmutationRecipes.getAllRecipes()
                .size()
            + "; controller duration="
            + assembly.mDuration
            + " ticks\n";
        Files.write(
            new File(System.getProperty("gtng.issue4.qa.output")).toPath(),
            report.getBytes(StandardCharsets.UTF_8));
        System.out.println("ISSUE_4_QA " + report);
    }

    private ItemStack input(int count) {
        return new ItemStack(fixture, count);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        // Both mods finish their one-shot END tick loaders before the second tick's assertions.
        if (!ready || event.phase != TickEvent.Phase.END || ++ticks != 2) return;
        checkRecipes();
        Minecraft.getMinecraft()
            .shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
