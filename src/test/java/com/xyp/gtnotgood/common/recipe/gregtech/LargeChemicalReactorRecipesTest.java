package com.xyp.gtnotgood.common.recipe.gregtech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.GTMod;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.common.GTProxy;
import gtPlusPlus.core.fluids.GTPPFluids;
import sun.misc.Unsafe;

public class LargeChemicalReactorRecipesTest {

    private static final Item PLATINUM_DUST = new Item();
    private static final Item CIRCUIT = new Item();
    private static final Fluid HYDROGEN = new Fluid("large_chemical_reactor_test_hydrogen");
    private static final Fluid OXYGEN = new Fluid("large_chemical_reactor_test_oxygen");
    private static final Fluid HYDROGEN_PEROXIDE = new Fluid("large_chemical_reactor_test_hydrogen_peroxide");
    private static Unsafe unsafe;
    private static Item originalFeather;
    private static File originalMinecraftHome;
    private static Side originalLogSide;
    private static GTProxy originalProxy;

    @BeforeClass
    public static void registerSamples() throws Exception {
        unsafe = (Unsafe) field(Unsafe.class, "theUnsafe").get(null);
        Field loader = field(Loader.class, "instance");
        if (loader.get(null) == null) loader.set(null, unsafe.allocateInstance(Loader.class));
        Field namedMods = field(Loader.class, "namedMods");
        if (namedMods.get(Loader.instance()) == null) namedMods.set(Loader.instance(), new HashMap<>());
        Field minecraftHome = field(FMLInjectionData.class, "minecraftHome");
        originalMinecraftHome = (File) minecraftHome.get(null);
        if (originalMinecraftHome == null) {
            File fixtureHome = new File("build/test-runtime/large-chemical-reactor").getAbsoluteFile();
            new File(fixtureHome, "config").mkdirs();
            minecraftHome.set(null, fixtureHome);
        }
        Field logSide = field(FMLRelaunchLog.class, "side");
        originalLogSide = (Side) logSide.get(null);
        if (originalLogSide == null) logSide.set(null, Side.SERVER);
        HeadlessBlockRegistry.bootstrap();
        originalFeather = Items.feather;
        if (originalFeather == null) setFeather(new Item());
        Method register = Item.itemRegistry.getClass().getDeclaredMethod("addObjectRaw", int.class, String.class,
            Object.class);
        register.setAccessible(true);
        register.invoke(Item.itemRegistry, 31070, "test:large_chemical_reactor_platinum", PLATINUM_DUST);
        register.invoke(Item.itemRegistry, 31071, "test:large_chemical_reactor_circuit", CIRCUIT);
        field(PLATINUM_DUST.delegate.getClass(), "name").set(PLATINUM_DUST.delegate,
            "test:large_chemical_reactor_platinum");
        field(CIRCUIT.delegate.getClass(), "name").set(CIRCUIT.delegate, "test:large_chemical_reactor_circuit");
        FluidRegistry.registerFluid(HYDROGEN);
        FluidRegistry.registerFluid(OXYGEN);
        FluidRegistry.registerFluid(HYDROGEN_PEROXIDE);
        String debug = System.getProperty("gt.recipebuilder.debug");
        System.setProperty("gt.recipebuilder.debug", "false");
        try {
            Class.forName(GTRecipeBuilder.class.getName(), true, GTRecipeBuilder.class.getClassLoader());
        } finally {
            if (debug == null) System.clearProperty("gt.recipebuilder.debug");
            else System.setProperty("gt.recipebuilder.debug", debug);
        }
        originalProxy = GTMod.proxy;
        if (originalProxy == null) GTMod.proxy = (GTProxy) unsafe.allocateInstance(GTProxy.class);
    }

    @AfterClass
    public static void restoreVanillaItem() throws Exception {
        if (originalFeather == null) setFeather(null);
        GTMod.proxy = originalProxy;
        field(FMLInjectionData.class, "minecraftHome").set(null, originalMinecraftHome);
        field(FMLRelaunchLog.class, "side").set(null, originalLogSide);
    }

    private static void setFeather(Item value) throws Exception {
        Field feather = field(Items.class, "feather");
        unsafe.putObject(unsafe.staticFieldBase(feather), unsafe.staticFieldOffset(feather), value);
    }

    @Test
    public void registersNonConsumedPlatinumUnderStrictNullInputValidation() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, ItemStack> unification = (Map<String, ItemStack>) field(GTOreDictUnificator.class, "sName2StackMap")
            .get(null);
        ItemStack savedPlatinum = unification.get("dustPlatinum");
        ItemStack savedCircuit = (ItemStack) field(ItemList.class, "mStack").get(ItemList.Circuit_Integrated);
        boolean circuitUnset = field(ItemList.class, "mHasNotBeenSet").getBoolean(ItemList.Circuit_Integrated);
        Fluid savedHydrogen = Materials.Hydrogen.mGas;
        Fluid savedOxygen = Materials.Oxygen.mGas;
        Fluid savedPeroxide = GTPPFluids.HydrogenPeroxide;
        boolean savedStrictNull = GTRecipeBuilder.PANIC_MODE_NULL;
        Set<GTRecipe> original = Collections.newSetFromMap(new IdentityHashMap<>());
        original.addAll(RecipeMaps.multiblockChemicalReactorRecipes.getAllRecipes());
        try {
            // Seed the real GT dust lookup without bootstrapping unrelated vanilla crafting recipes.
            unification.put("dustPlatinum", new ItemStack(PLATINUM_DUST));
            ItemList.Circuit_Integrated.set(CIRCUIT);
            Materials.Hydrogen.mGas = HYDROGEN;
            Materials.Oxygen.mGas = OXYGEN;
            GTPPFluids.HydrogenPeroxide = HYDROGEN_PEROXIDE;
            GTRecipeBuilder.PANIC_MODE_NULL = true;
            assertSame(PLATINUM_DUST, Materials.Platinum.getDust(1).getItem());
            assertNull(Materials.Platinum.getDust(0));
            assertThrows(IllegalArgumentException.class,
                () -> GTRecipeBuilder.builder().itemInputs(Materials.Platinum.getDust(0)));

            LargeChemicalReactorRecipes.loadRecipes();

            List<GTRecipe> added = addedRecipes(original);
            assertEquals(1, added.size());
            GTRecipe recipe = added.get(0);
            assertEquals(2, recipe.mInputs.length);
            for (ItemStack input : recipe.mInputs) assertNotNull(input);
            ItemStack catalyst = Arrays.stream(recipe.mInputs).filter(input -> input.getItem() == PLATINUM_DUST)
                .findFirst().get();
            ItemStack circuit = Arrays.stream(recipe.mInputs).filter(input -> input.getItem() == CIRCUIT).findFirst()
                .get();
            assertEquals(0, catalyst.stackSize);
            assertEquals(0, circuit.stackSize);
            assertEquals(24, circuit.getItemDamage());
            assertEquals(0, recipe.mOutputs.length);
            assertEquals(2, recipe.mFluidInputs.length);
            assertEquals(1, recipe.mFluidOutputs.length);
            assertFluid(recipe.mFluidInputs, HYDROGEN, 2000);
            assertFluid(recipe.mFluidInputs, OXYGEN, 2000);
            assertFluid(recipe.mFluidOutputs, HYDROGEN_PEROXIDE, 1000);
            assertEquals(400, recipe.mDuration);
            assertEquals(TierEU.RECIPE_EV, recipe.mEUt);

            ItemStack[] items = { new ItemStack(PLATINUM_DUST), new ItemStack(CIRCUIT, 1, 24) };
            FluidStack[] fluids = { new FluidStack(HYDROGEN, 2000), new FluidStack(OXYGEN, 2000) };
            assertTrue(recipe.isRecipeInputEqual(true, false, fluids, items));
            assertEquals(1, items[0].stackSize);
            assertEquals(1, items[1].stackSize);
            assertEquals(0, fluids[0].amount);
            assertEquals(0, fluids[1].amount);
            assertFalse(recipe.isRecipeInputEqual(false, false,
                new FluidStack[] { new FluidStack(HYDROGEN, 2000), new FluidStack(OXYGEN, 2000) },
                new ItemStack(CIRCUIT, 1, 24)));
        } finally {
            RecipeMaps.multiblockChemicalReactorRecipes.getBackend().removeRecipes(addedRecipes(original));
            if (savedPlatinum == null) unification.remove("dustPlatinum");
            else unification.put("dustPlatinum", savedPlatinum);
            field(ItemList.class, "mStack").set(ItemList.Circuit_Integrated, savedCircuit);
            field(ItemList.class, "mHasNotBeenSet").setBoolean(ItemList.Circuit_Integrated, circuitUnset);
            Materials.Hydrogen.mGas = savedHydrogen;
            Materials.Oxygen.mGas = savedOxygen;
            GTPPFluids.HydrogenPeroxide = savedPeroxide;
            GTRecipeBuilder.PANIC_MODE_NULL = savedStrictNull;
        }
    }

    private static void assertFluid(FluidStack[] fluids, Fluid expected, int amount) {
        for (FluidStack fluid : fluids) assertNotNull(fluid);
        FluidStack actual = Arrays.stream(fluids).filter(fluid -> fluid.getFluid() == expected).findFirst().get();
        assertEquals(amount, actual.amount);
    }

    private static List<GTRecipe> addedRecipes(Set<GTRecipe> original) {
        List<GTRecipe> added = new ArrayList<>();
        for (GTRecipe recipe : RecipeMaps.multiblockChemicalReactorRecipes.getAllRecipes()) {
            if (!original.contains(recipe)) added.add(recipe);
        }
        return added;
    }

    private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
