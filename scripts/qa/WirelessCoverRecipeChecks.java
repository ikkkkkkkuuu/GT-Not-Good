package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.recipe.gregtech.WirelessEnergyCoverRecipes;
import com.xyp.gtnotgood.config.Config;
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
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

@Mod(
    modid = "wirelesscoverrecipeqa",
    name = "Wireless Cover Recipe QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD + ";after:" + ModList.ModIds.GT_NOT_LEISURE)
public final class WirelessCoverRecipeChecks {

    private static final String[] TIERS = { "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV", "UIV",
        "UMV", "UXV", "MAX" };
    private static final String[] CABLES = { "Tin", "Copper", "Gold", "Aluminium", "Tungsten", "VanadiumGallium",
        "Naquadah", "NaquadahAlloy", "Bedrockium", "Draconium", "NetherStar", "Quantium", "BlackPlutonium",
        "DraconiumAwakened" };
    private static final String[] PLATES = { "Steel", "Aluminium", "StainlessSteel", "Titanium", "TungstenSteel",
        "Iridium", "NaquadahAlloy", "Osmium", "Neutronium", "Bedrockium", "BlackPlutonium", "SpaceTime", "MagMatter",
        "MHDCSM" };
    private static final String[] CHIPS = { "", "ULPIC", "LPIC", "PIC", "HPIC", "UHPIC", "NPIC", "PPIC", "QPIC", "QPIC",
        "QPIC", "QPIC", "QPIC", "QPIC" };
    private static final int[] COIL_COUNTS = { 1, 2, 3, 3, 4, 8, 8, 8, 8, 8, 12, 12, 16, 32 };
    private static final int[] CHIP_COUNTS = { 16, 2, 2, 3, 3, 4, 4, 6, 6, 8, 8, 12, 16, 32 };
    private final List<String> report = new ArrayList<>();
    private boolean complete;
    private boolean finished;
    private int ticks;
    private int completedTicks;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.wirelessCover.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @Mod.EventHandler
    public void complete(FMLLoadCompleteEvent event) {
        complete = true;
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            if (++ticks > 2400) throw new AssertionError("Recipe loading did not complete");
            if (!complete || !GregTechAPI.sFullLoadFinished || ++completedTicks < 20) return;
            System.out.println("WIRELESS_COVER_QA: current screen " + Minecraft.getMinecraft().currentScreen);
            verify();
            finish("PASS; " + checks + " checks; GTNL=" + ModList.GTNotLeisure.isModLoaded());
        } catch (Throwable error) {
            error.printStackTrace();
            finish("FAIL; " + error);
        }
    }

    private void verify() throws Exception {
        require(GregTechAPI.sFullLoadFinished, "Forge and GregTech recipe loading completed");
        boolean gtnl = Loader.isModLoaded(ModList.ModIds.GT_NOT_LEISURE);
        require(gtnl == Boolean.getBoolean("gtng.wirelessCover.gtnl"), "actual GTNL presence matches the QA profile");
        require(ModList.GTNotLeisure.isModLoaded() == gtnl, "cached optional-mod detection matches Forge");
        require(Config.recipeSpeedMode == 0, "isolated recipe baseline disables global duration acceleration");
        Map<GTRecipe, String> snapshot = snapshot();
        List<String> failures = new ArrayList<>();
        for (int index = 0; index < TIERS.length; index++) {
            try {
                verifyTier(index);
            } catch (AssertionError error) {
                failures.add(error.getMessage());
                report.add("FAIL: " + error.getMessage());
                System.out.println("WIRELESS_COVER_QA: FAIL: " + error.getMessage());
            }
        }
        requireUnchanged(snapshot, "native matching and batch consumption leave registered recipes unchanged");

        Field loaded = ModList.class.getDeclaredField("modLoaded");
        loaded.setAccessible(true);
        Object previous = loaded.get(ModList.GTNotLeisure);
        try {
            loaded.set(ModList.GTNotLeisure, true);
            WirelessEnergyCoverRecipes.loadRecipes();
            requireUnchanged(snapshot, "GTNL-present branch adds no recipe and changes no existing recipe");
        } finally {
            loaded.set(ModList.GTNotLeisure, previous);
        }
        require(ModList.GTNotLeisure.isModLoaded() == gtnl, "optional-mod detection restored after the guard check");
        require(failures.isEmpty(), "all fourteen tiers pass: " + String.join("; ", failures));
    }

    private void verifyTier(int index) {
        String tier = TIERS[index];
        ItemStack output = ItemList.WIRELESS_ENERGY_COVERS[index].get(1);
        List<GTRecipe> recipes = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
            .filter(recipe -> Arrays.stream(recipe.mOutputs).anyMatch(stack -> GTUtility.areStacksEqual(stack, output)))
            .collect(Collectors.toList());
        List<GTRecipe> imported = recipes.stream().filter(recipe -> recipe.mInputs.length == 8)
            .collect(Collectors.toList());
        require(imported.size() == 1, tier + " has exactly one GTNL-style assembler recipe");
        GTRecipe recipe = imported.get(0);
        require(recipe.mOutputs.length == 1 && recipe.mOutputs[0].stackSize == 1 && recipe.mFluidOutputs.length == 0,
            tier + " outputs one native 1A cover without byproducts");
        require(recipe.mEUt == GTValues.VP[index + 1] && recipe.mDuration == 200,
            tier + " uses its native practical voltage and 200 ticks");
        OrePrefixes wire = index >= 11 ? OrePrefixes.wireGt01 : OrePrefixes.cableGt01;
        ItemStack[] expected = { ItemList.valueOf("Sensor_" + tier).get(1),
            GTOreDictUnificator.get(OrePrefixes.plate, Materials.EnderPearl, 12),
            GTOreDictUnificator.get(OrePrefixes.circuit, material(tier), 4),
            ItemList.valueOf((index < 8 ? tier : "UHV") + "_Coil").get(COIL_COUNTS[index]),
            index == 0 ? GTOreDictUnificator.get(OrePrefixes.spring, Materials.Tin, 16)
                : ItemList.valueOf("Circuit_Chip_" + CHIPS[index]).get(CHIP_COUNTS[index]),
            GTOreDictUnificator.get(wire, material(CABLES[index]), 8),
            GTOreDictUnificator.get(wire, Materials.RedAlloy, 32),
            GTOreDictUnificator.get(OrePrefixes.plate, material(PLATES[index]), 12) };
        requireItems(recipe, expected, tier + " exact sensor, plates, circuits, coils, chips and wire quantities");
        require(
            recipe.mFluidInputs.length == 1 && recipe.mAltFluidInputs != null
                && recipe.mAltFluidInputs.length == 1
                && recipe.mAltFluidInputs[0].length == 3,
            tier + " retains all three native solder alternatives in one recipe");
        FluidStack[] solders = { Materials.SolderingAlloy.getMolten(144), Materials.Tin.getMolten(288),
            Materials.Lead.getMolten(576) };
        for (FluidStack solder : solders) verifySolder(recipe, expected, solder, index + 1);
        for (int slot = 0; slot < expected.length; slot++) {
            ItemStack[] shortItems = copy(expected);
            shortItems[slot].stackSize--;
            if (shortItems[slot].stackSize == 0) shortItems[slot] = null;
            require(!recipe.isRecipeInputEqual(false, false, new FluidStack[] { solders[0].copy() }, shortItems),
                tier + " rejects a shortage in ingredient " + slot);
        }

        List<GTRecipe> originals = recipes.stream().filter(candidate -> candidate.mInputs.length == 4)
            .filter(candidate -> candidate.mFluidInputs.length == 1
                && candidate.mFluidInputs[0].isFluidEqual(Materials.ExcitedDTEC.getFluid(32000)))
            .collect(Collectors.toList());
        require(originals.size() == 1, tier + " keeps the original GregTech assembler recipe");
        GTRecipe original = originals.get(0);
        report.add(tier + " original recipe: " + signature(original));
    }

    private void verifySolder(GTRecipe recipe, ItemStack[] expected, FluidStack solder, int tier) {
        String label = TIERS[tier - 1] + " " + solder.getFluid().getName() + " " + solder.amount + " mB";
        require(
            Arrays.stream(recipe.mAltFluidInputs[0])
                .anyMatch(actual -> actual.isFluidEqual(solder) && actual.amount == solder.amount),
            label + " is registered with its exact quantity");
        require(RecipeMaps.assemblerRecipes.findRecipeQuery().items(copy(expected)).fluids(solder.copy())
            .voltage(GTValues.V[tier]).find() == recipe, label + " resolves through the real assembler backend");
        require(RecipeMaps.assemblerRecipes.findRecipeQuery().items(copy(expected)).fluids(solder.copy())
            .voltage(GTValues.V[tier - 1]).find() != recipe, label + " rejects the lower voltage tier");
        FluidStack shortFluid = solder.copy();
        shortFluid.amount--;
        require(!recipe.isRecipeInputEqual(false, false, new FluidStack[] { shortFluid }, copy(expected)),
            label + " rejects a one-millibucket shortage");
        ItemStack[] consumed = copy(expected);
        FluidStack[] consumedFluid = { solder.copy() };
        require(recipe.isRecipeInputEqual(true, false, consumedFluid, consumed), label + " accepts one complete batch");
        require(Arrays.stream(consumed).allMatch(stack -> stack == null || stack.stackSize == 0)
            && consumedFluid[0].amount == 0, label + " consumes every item and the selected solder exactly once");
    }

    private void requireItems(GTRecipe recipe, ItemStack[] expected, String message) {
        boolean matches = recipe.mInputs.length == expected.length && Arrays.stream(expected)
            .allMatch(wanted -> wanted != null && Arrays.stream(recipe.mInputs)
                .filter(actual -> actual != null && GTUtility.areStacksEqual(actual, wanted)
                    && actual.stackSize == wanted.stackSize)
                .count() == 1);
        if (!matches) {
            for (int index = 0; index < Math.max(expected.length, recipe.mInputs.length); index++) {
                String diagnostic = message + "; input="
                    + index
                    + "; expected="
                    + diagnostic(index < expected.length ? expected[index] : null)
                    + "; actual="
                    + diagnostic(index < recipe.mInputs.length ? recipe.mInputs[index] : null);
                report.add(diagnostic);
                System.out.println("WIRELESS_COVER_QA: " + diagnostic);
            }
        }
        require(matches, message);
    }

    private static String diagnostic(ItemStack stack) {
        if (stack == null) return "null";
        ItemStack unified = GTOreDictUnificator.get(true, stack.copy());
        return stack.getDisplayName() + " "
            + stack.writeToNBT(new NBTTagCompound())
            + "; unified="
            + (unified == null ? "null" : unified.writeToNBT(new NBTTagCompound()));
    }

    private static ItemStack[] copy(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(ItemStack::copy).toArray(ItemStack[]::new);
    }

    private static Materials material(String field) {
        try {
            return (Materials) Materials.class.getField(field).get(null);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Unknown GregTech material field: " + field, error);
        }
    }

    private static Map<GTRecipe, String> snapshot() {
        Map<GTRecipe, String> result = new IdentityHashMap<>();
        for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
            result.put(recipe, signature(recipe));
        }
        return result;
    }

    private static String signature(GTRecipe recipe) {
        return "items=" + itemSignature(recipe.mInputs)
            + ";outputs="
            + itemSignature(recipe.mOutputs)
            + ";fluids="
            + fluidSignature(recipe.mFluidInputs)
            + ";fluidOutputs="
            + fluidSignature(recipe.mFluidOutputs)
            + ";alternatives="
            + (recipe.mAltFluidInputs == null ? "null"
                : Arrays.stream(recipe.mAltFluidInputs).map(WirelessCoverRecipeChecks::fluidSignature)
                    .collect(Collectors.joining(";")))
            + ";duration="
            + recipe.mDuration
            + ";eut="
            + recipe.mEUt
            + ";enabled="
            + recipe.mEnabled
            + ";fake="
            + recipe.mFakeRecipe;
    }

    private static String itemSignature(ItemStack[] stacks) {
        if (stacks == null) return "null";
        return Arrays.stream(stacks)
            .map(stack -> stack == null ? "null" : stack.writeToNBT(new NBTTagCompound()).toString())
            .collect(Collectors.joining(";"));
    }

    private static String fluidSignature(FluidStack[] stacks) {
        if (stacks == null) return "null";
        return Arrays.stream(stacks)
            .map(stack -> stack == null ? "null" : stack.writeToNBT(new NBTTagCompound()).toString())
            .collect(Collectors.joining(";"));
    }

    private void requireUnchanged(Map<GTRecipe, String> before, String message) {
        Map<GTRecipe, String> after = snapshot();
        require(
            before.size() == after.size()
                && before.entrySet().stream().allMatch(entry -> entry.getValue().equals(after.get(entry.getKey()))),
            message);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        report.add(message);
        System.out.println("WIRELESS_COVER_QA: " + message);
    }

    private void finish(String result) {
        finished = true;
        try {
            File output = new File(System.getProperty("gtng.wirelessCover.output", "."));
            Files.write(new File(output, "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
            Files.write(new File(output, "checks.txt").toPath(), report, StandardCharsets.UTF_8);
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("WIRELESS_COVER_QA: " + result);
        FMLCommonHandler.instance().exitJava(result.startsWith("PASS") ? 0 : 1, false);
    }
}
