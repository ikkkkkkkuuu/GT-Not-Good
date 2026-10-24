package com.xyp.gtnotgood.common.machines.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.common.recipe.machine.LightHunterRecipeRules;
import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;
import com.xyp.gtnotgood.utils.HeadlessItemRegistry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.GTMod;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.common.GTProxy;
import sun.misc.Unsafe;

public class LightHunterBatchProcessingTest {

    private static Item shared;
    private static Item left;
    private static Item right;
    private static Item firstOutput;
    private static Item secondOutput;
    private static File originalHome;
    private static Side originalSide;
    private static GTProxy originalProxy;

    @BeforeClass
    public static void initializeNativeRecipeFixtures() throws Exception {
        Unsafe unsafe = (Unsafe) field(Unsafe.class, "theUnsafe").get(null);
        if (field(Loader.class, "instance").get(null) == null)
            field(Loader.class, "instance").set(null, unsafe.allocateInstance(Loader.class));
        if (field(Loader.class, "namedMods").get(Loader.instance()) == null)
            field(Loader.class, "namedMods").set(Loader.instance(), new HashMap<>());
        originalHome = (File) field(FMLInjectionData.class, "minecraftHome").get(null);
        if (originalHome == null) {
            File home = new File("build/test-runtime/light-hunter-batches").getAbsoluteFile();
            new File(home, "config").mkdirs();
            field(FMLInjectionData.class, "minecraftHome").set(null, home);
        }
        originalSide = (Side) field(FMLRelaunchLog.class, "side").get(null);
        if (originalSide == null) field(FMLRelaunchLog.class, "side").set(null, Side.SERVER);
        HeadlessBlockRegistry.bootstrap();
        HeadlessItemRegistry.bootstrap();
        originalProxy = GTMod.proxy;
        if (originalProxy == null) GTMod.proxy = (GTProxy) unsafe.allocateInstance(GTProxy.class);
        shared = registerItem(31300, "test:light_hunter_shared");
        left = registerItem(31301, "test:light_hunter_left");
        right = registerItem(31302, "test:light_hunter_right");
        firstOutput = registerItem(31303, "test:light_hunter_first_output");
        secondOutput = registerItem(31304, "test:light_hunter_second_output");
        String debug = System.getProperty("gt.recipebuilder.debug");
        System.setProperty("gt.recipebuilder.debug", "false");
        try {
            Class.forName(GTRecipeBuilder.class.getName(), true, GTRecipeBuilder.class.getClassLoader());
        } finally {
            if (debug == null) System.clearProperty("gt.recipebuilder.debug");
            else System.setProperty("gt.recipebuilder.debug", debug);
        }
    }

    @AfterClass
    public static void restoreEnvironment() throws Exception {
        GTMod.proxy = originalProxy;
        field(FMLInjectionData.class, "minecraftHome").set(null, originalHome);
        field(FMLRelaunchLog.class, "side").set(null, originalSide);
    }

    @Test
    public void mergesTwoRecipesWithoutConsumingSharedInputsTwice() {
        GTRecipe first = recipe(new ItemStack[] { stack(shared, 1), stack(left, 1) }, firstOutput, 10, 10);
        GTRecipe second = recipe(new ItemStack[] { stack(shared, 1), stack(right, 1) }, secondOutput, 20, 20);
        ItemStack sharedStock = stack(shared, 12), leftStock = stack(left, 4), rightStock = stack(right, 6);
        TestLogic logic = logic(false, 100, first, second);
        logic.setInputItems(sharedStock, leftStock, rightStock);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);

        assertTrue(logic.process().wasSuccessful());
        assertEquals(12, sharedStock.stackSize);
        assertEquals(4, leftStock.stackSize);
        assertEquals(6, rightStock.stackSize);
        assertEquals(10, logic.getBatchParallelCount());
        assertEquals(2, logic.getBatchRecipeCount());
        assertEquals(100, logic.getCalculatedEut());
        assertEquals(28, logic.getDuration());
        assertOutputCount(logic, 0, BigInteger.valueOf(4));
        assertOutputCount(logic, 1, BigInteger.valueOf(6));
        assertTrue(logic.commitInputs());
        assertEquals(2, sharedStock.stackSize);
        assertEquals(0, leftStock.stackSize);
        assertEquals(0, rightStock.stackSize);
        assertFalse(logic.commitInputs());
    }

    @Test
    public void secondRecipeRechecksSharedStockAfterTheFirstRecipeConsumesItsSnapshot() {
        GTRecipe first = recipe(new ItemStack[] { stack(shared, 1), stack(left, 1) }, firstOutput, 10, 10);
        GTRecipe second = recipe(new ItemStack[] { stack(shared, 1), stack(right, 1) }, secondOutput, 10, 10);
        ItemStack stock = stack(shared, 12), leftStock = stack(left, 8), rightStock = stack(right, 8);
        TestLogic logic = logic(false, 100, first, second);
        logic.setInputItems(stock, leftStock, rightStock);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);

        assertTrue(logic.process().wasSuccessful());
        assertEquals(12, logic.getBatchParallelCount());
        assertOutputCount(logic, 0, BigInteger.valueOf(8));
        assertOutputCount(logic, 1, BigInteger.valueOf(4));
        assertTrue(logic.commitInputs());
        assertEquals(0, stock.stackSize);
        assertEquals(4, rightStock.stackSize);
    }

    @Test
    public void itemCountsAboveIntRemainExactAndRequireAnExplicitCommit() {
        GTRecipe recipe = recipe(new ItemStack[] { stack(shared, 1) }, firstOutput, 1, 1);
        ItemStack first = stack(shared, Integer.MAX_VALUE), second = stack(shared, Integer.MAX_VALUE);
        TestLogic logic = logic(false, Long.MAX_VALUE, recipe);
        logic.setInputItems(first, second);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);

        assertTrue(logic.process().wasSuccessful());
        long expected = 2L * Integer.MAX_VALUE;
        assertEquals(expected, logic.getBatchParallelCount());
        assertOutputCount(logic, 0, BigInteger.valueOf(expected));
        assertEquals(Integer.MAX_VALUE, first.stackSize);
        assertTrue(logic.commitInputs());
        assertEquals(0, first.stackSize);
        assertEquals(0, second.stackSize);
    }

    @Test
    public void fullAstralArrayStationRemovesOnlyTheNormalParallelQuota() {
        GTRecipe recipe = recipe(new ItemStack[] { stack(shared, 1) }, firstOutput, 1, 1);
        for (boolean unrestricted : new boolean[] { false, true }) {
            ItemStack[] stock = new ItemStack[129];
            Arrays.setAll(stock, index -> stack(shared, Integer.MAX_VALUE));
            TestLogic logic = logic(unrestricted, 1000000, recipe);
            logic.setInputItems(stock);
            logic.prepareModuleRecipe(Long.MAX_VALUE, true);
            assertTrue(logic.process().wasSuccessful());
            long expected = unrestricted ? 129L * Integer.MAX_VALUE : LightHunterRecipeRules.NORMAL_PARALLEL_QUOTA;
            assertEquals(expected, logic.getBatchParallelCount());
            assertOutputCount(logic, 0, BigInteger.valueOf(expected));
            assertEquals(1000000, logic.getCalculatedEut());
            assertTrue(logic.commitInputs());
            long remaining = 0;
            for (ItemStack item : stock) remaining += item.stackSize;
            assertEquals(129L * Integer.MAX_VALUE - expected, remaining);
        }
    }

    @Test
    public void aliasedInputSlotsDoNotMultiplyTheAvailableStock() {
        GTRecipe recipe = recipe(new ItemStack[] { stack(shared, 1) }, firstOutput, 1, 1);
        ItemStack stock = stack(shared, 10);
        TestLogic logic = logic(false, 100, recipe);
        logic.setInputItems(stock, stock);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);
        assertTrue(logic.process().wasSuccessful());
        assertEquals(10, logic.getBatchParallelCount());
        assertTrue(logic.commitInputs());
        assertEquals(0, stock.stackSize);
    }

    @Test
    public void repeatedFluidRequirementsShareTheSameStock() {
        GTRecipe recipe = GTRecipeBuilder.builder()
            .fluidInputs(new FluidStack(FluidRegistry.WATER, 1), new FluidStack(FluidRegistry.WATER, 1))
            .itemOutputs(stack(firstOutput, 1)).duration(20).eut(10).build().get();
        FluidStack stock = new FluidStack(FluidRegistry.WATER, 10);
        assertEquals(5,
            LightHunterBatchProcessingLogic.maximumParallel(recipe, new ItemStack[0], new FluidStack[] { stock }));
        TestLogic logic = logic(false, 100, recipe);
        logic.setInputFluids(stock);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);
        assertTrue(logic.process().wasSuccessful());
        assertEquals(5, logic.getBatchParallelCount());
        assertEquals(10, stock.amount);
        assertTrue(logic.commitInputs());
        assertEquals(0, stock.amount);
    }

    @Test
    public void abandoningOrRejectingAStaleBatchDoesNotConsumeLiveInputs() {
        GTRecipe recipe = recipe(new ItemStack[] { stack(shared, 1) }, firstOutput, 20, 10);
        ItemStack stock = stack(shared, 10);
        TestLogic logic = logic(false, 100, recipe);
        logic.setInputItems(stock);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);
        assertTrue(logic.process().wasSuccessful());
        logic.finishModuleRecipe();
        assertFalse(logic.commitInputs());
        assertEquals(10, stock.stackSize);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);
        assertTrue(logic.process().wasSuccessful());
        stock.stackSize = 9;
        assertFalse(logic.commitInputs());
        assertEquals(9, stock.stackSize);
    }

    @Test
    public void batchesIndependentPatternInventoriesInOneAtomicCommit() {
        GTRecipe first = recipe(new ItemStack[] { stack(left, 1) }, firstOutput, 10, 10);
        GTRecipe second = GTRecipeBuilder.builder().itemInputs(stack(right, 1))
            .fluidInputs(new FluidStack(FluidRegistry.WATER, 100)).itemOutputs(stack(secondOutput, 1)).duration(20)
            .eut(20).build().get();
        ItemStack a = stack(left, 3), b = stack(right, 5);
        FluidStack water = new FluidStack(FluidRegistry.WATER, 500);
        TestLogic logic = logic(false, 100, first, second);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);
        assertTrue(logic
            .processPatternInputs(
                List.of(new ModularProcessingLogic.RecipeInputs(new ItemStack[] { a }, new FluidStack[0]),
                    new ModularProcessingLogic.RecipeInputs(new ItemStack[] { b }, new FluidStack[] { water })))
            .wasSuccessful());
        assertEquals(2, logic.getBatchRecipeCount());
        assertEquals(8, logic.getBatchParallelCount());
        assertEquals(3, a.stackSize);
        assertEquals(5, b.stackSize);
        assertEquals(500, water.amount);
        water.amount--;
        assertFalse(logic.commitInputs());
        assertEquals(3, a.stackSize);
        assertEquals(5, b.stackSize);
        water.amount++;
        assertTrue(logic.commitInputs());
        assertEquals(0, a.stackSize);
        assertEquals(0, b.stackSize);
        assertEquals(0, water.amount);
    }

    @Test
    public void neverCombinesIngredientsFromDifferentPatterns() {
        GTRecipe recipe = recipe(new ItemStack[] { stack(left, 1), stack(right, 1) }, firstOutput, 10, 10);
        ItemStack a = stack(left, 3), b = stack(right, 5);
        TestLogic logic = logic(false, 100, recipe);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);
        assertFalse(logic
            .processPatternInputs(
                List.of(new ModularProcessingLogic.RecipeInputs(new ItemStack[] { a }, new FluidStack[0]),
                    new ModularProcessingLogic.RecipeInputs(new ItemStack[] { b }, new FluidStack[0])))
            .wasSuccessful());
        logic.finishModuleRecipe();
        assertFalse(logic.commitInputs());
        assertEquals(3, a.stackSize);
        assertEquals(5, b.stackSize);
    }

    @Test
    public void sharedHatchStockIsConsumedOnlyOnceAcrossPatternGroups() {
        GTRecipe first = recipe(new ItemStack[] { stack(shared, 1), stack(left, 1) }, firstOutput, 10, 10);
        GTRecipe second = recipe(new ItemStack[] { stack(shared, 1), stack(right, 1) }, secondOutput, 10, 10);
        ItemStack common = stack(shared, 5), a = stack(left, 3), b = stack(right, 3);
        TestLogic logic = logic(false, 100, first, second);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);
        assertTrue(logic
            .processPatternInputs(
                List.of(new ModularProcessingLogic.RecipeInputs(new ItemStack[] { common, a }, new FluidStack[0]),
                    new ModularProcessingLogic.RecipeInputs(new ItemStack[] { common, b }, new FluidStack[0])))
            .wasSuccessful());
        assertEquals(5, logic.getBatchParallelCount());
        assertTrue(logic.commitInputs());
        assertEquals(0, common.stackSize);
        assertEquals(1, a.stackSize + b.stackSize);
    }

    @Test
    public void refillingTheFirstLongPatternCannotStarveTheFollowingPattern() {
        GTRecipe first = recipe(new ItemStack[] { stack(left, 1) }, firstOutput, 20000, 8);
        GTRecipe second = recipe(new ItemStack[] { stack(right, 1) }, secondOutput, 200, 8);
        ItemStack a = stack(left, 64), b = stack(right, 4);
        TestLogic logic = logic(false, 32, first, second);
        List<ModularProcessingLogic.RecipeInputs> groups = List.of(
            new ModularProcessingLogic.RecipeInputs(new ItemStack[] { a }, new FluidStack[0]),
            new ModularProcessingLogic.RecipeInputs(new ItemStack[] { b }, new FluidStack[0]));
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);
        assertTrue(logic.processPatternInputs(groups).wasSuccessful());
        assertEquals(1, logic.getBatchRecipeCount());
        assertTrue(logic.commitInputs());
        assertEquals(0, a.stackSize);
        assertEquals(4, b.stackSize);

        a.stackSize = 64;
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);
        assertTrue(logic.processPatternInputs(groups).wasSuccessful());
        assertTrue(logic.commitInputs());
        assertEquals(0, b.stackSize);
    }

    private static GTRecipe recipe(ItemStack[] inputs, Item output, int duration, int eut) {
        return GTRecipeBuilder.builder().itemInputs(inputs).itemOutputs(stack(output, 1)).duration(duration).eut(eut)
            .build().get();
    }

    private static TestLogic logic(boolean unrestricted, long voltage, GTRecipe... recipes) {
        TestLogic logic = new TestLogic(unrestricted, Arrays.asList(recipes));
        logic.setAvailableVoltage(voltage);
        logic.setInputItems(new ItemStack[0]);
        logic.setInputFluids(new FluidStack[0]);
        return logic;
    }

    private static ItemStack stack(Item item, int amount) {
        return new ItemStack(item, amount);
    }

    private static void assertOutputCount(TestLogic logic, int index, BigInteger expected) {
        NBTTagList items = logic.getBatchOutputs().save().getTagList("items", 10);
        assertEquals(expected.toString(), items.getCompoundTagAt(index).getString("amount"));
    }

    private static Item registerItem(int id, String name) throws Exception {
        Item item = new Item();
        Method register = Item.itemRegistry.getClass().getDeclaredMethod("addObjectRaw", int.class, String.class,
            Object.class);
        register.setAccessible(true);
        register.invoke(Item.itemRegistry, id, name, item);
        field(item.delegate.getClass(), "name").set(item.delegate, name);
        return item;
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class TestLogic extends LightHunterBatchProcessingLogic {

        private final List<GTRecipe> recipes;

        private TestLogic(boolean unrestricted, List<GTRecipe> recipes) {
            super(() -> unrestricted, () -> 20);
            this.recipes = recipes;
        }

        @Override
        protected Stream<GTRecipe> findRecipeMatches(RecipeMap<?> map) {
            return recipes.stream();
        }

        @Override
        protected CheckRecipeResult validateRecipe(GTRecipe recipe) {
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }
    }
}
