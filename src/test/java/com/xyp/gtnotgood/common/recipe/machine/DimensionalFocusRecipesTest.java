package com.xyp.gtnotgood.common.recipe.machine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;
import com.xyp.gtnotgood.utils.HeadlessItemRegistry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.GTMod;
import gregtech.api.enums.ItemList;
import gregtech.api.objects.SubstituteFluidStack;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.recipe.metadata.SimpleRecipeMetadataKey;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.common.GTProxy;
import sun.misc.Unsafe;

public class DimensionalFocusRecipesTest {

    private static final Item WAFER = new Item().setUnlocalizedName("focus_test_wafer");
    private static final Item LENS = new Item().setUnlocalizedName("focus_test_lens");
    private static final Item PRODUCT = new Item().setUnlocalizedName("focus_test_product");
    private static final Item DATA_STICK = new Item().setUnlocalizedName("focus_test_data_stick");
    private static final Fluid WATER = new Fluid("dimensional_focus_test_water");
    private static final RecipeMetadataKey<String> SAMPLE_METADATA = SimpleRecipeMetadataKey.create(String.class,
        "dimensional_focus_test_metadata");
    private static Unsafe unsafe;
    private static File originalMinecraftHome;
    private static Side originalLogSide;
    private static GTProxy originalProxy;
    private static ItemStack originalDataStick;
    private static boolean originalDataStickUnset;

    @BeforeClass
    public static void initializeFixtures() throws Exception {
        unsafe = (Unsafe) field(Unsafe.class, "theUnsafe").get(null);
        Field loader = field(Loader.class, "instance");
        if (loader.get(null) == null) loader.set(null, unsafe.allocateInstance(Loader.class));
        Field namedMods = field(Loader.class, "namedMods");
        if (namedMods.get(Loader.instance()) == null) namedMods.set(Loader.instance(), new HashMap<>());
        Field minecraftHome = field(FMLInjectionData.class, "minecraftHome");
        originalMinecraftHome = (File) minecraftHome.get(null);
        if (originalMinecraftHome == null) {
            File fixtureHome = new File("build/test-runtime/dimensional-focus").getAbsoluteFile();
            new File(fixtureHome, "config").mkdirs();
            minecraftHome.set(null, fixtureHome);
        }
        Field logSide = field(FMLRelaunchLog.class, "side");
        originalLogSide = (Side) logSide.get(null);
        if (originalLogSide == null) logSide.set(null, Side.SERVER);
        HeadlessBlockRegistry.bootstrap();
        HeadlessItemRegistry.bootstrap();
        Method register = Item.itemRegistry.getClass().getDeclaredMethod("addObjectRaw", int.class, String.class,
            Object.class);
        register.setAccessible(true);
        Item[] items = { WAFER, LENS, PRODUCT, DATA_STICK };
        for (int index = 0; index < items.length; index++) {
            String name = "test:dimensional_focus_" + index;
            register.invoke(Item.itemRegistry, 31200 + index, name, items[index]);
            field(items[index].delegate.getClass(), "name").set(items[index].delegate, name);
        }
        originalProxy = GTMod.proxy;
        if (originalProxy == null) GTMod.proxy = (GTProxy) unsafe.allocateInstance(GTProxy.class);
        originalDataStick = (ItemStack) field(ItemList.class, "mStack").get(ItemList.Tool_DataStick);
        originalDataStickUnset = field(ItemList.class, "mHasNotBeenSet").getBoolean(ItemList.Tool_DataStick);
        ItemList.Tool_DataStick.set(DATA_STICK);
        FluidRegistry.registerFluid(WATER);
        DimensionalFocusFluids.init();
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
        field(FMLInjectionData.class, "minecraftHome").set(null, originalMinecraftHome);
        field(FMLRelaunchLog.class, "side").set(null, originalLogSide);
        field(ItemList.class, "mStack").set(ItemList.Tool_DataStick, originalDataStick);
        field(ItemList.class, "mHasNotBeenSet").setBoolean(ItemList.Tool_DataStick, originalDataStickUnset);
    }

    @Test
    public void retainsNativeInputsFluidsLensesAndMetadata() {
        GTRecipe source = source(120, 49);
        GTRecipe converted = DimensionalFocusRecipes.convert(source);
        assertNotNull(converted);
        assertEquals(480, converted.mEUt);
        assertEquals(9, converted.mDuration);
        assertEquals(2, converted.mInputs[0].stackSize);
        assertEquals(0, converted.mInputs[1].stackSize);
        assertEquals(3, converted.mOutputs[0].stackSize);
        assertEquals("retained", converted.getMetadata(SAMPLE_METADATA));
        assertEquals(2, converted.mFluidInputs.length);
        assertEquals(100, converted.mFluidInputs[0].amount);
        assertEquals(WATER, converted.mFluidInputs[0].getFluid());
        assertEquals(DimensionalFocusFluids.photoresist(1).getFluid(), converted.mFluidInputs[1].getFluid());
        assertEquals(3, converted.mFluidInputs[1].amount);
        assertEquals(120, source.mEUt);
        assertEquals(49, source.mDuration);
        assertEquals(1, source.mFluidInputs.length);
        assertFalse(DimensionalFocusResearch.requiresResearch(converted));
    }

    @Test
    public void rejectsUnsupportedFluidOutputsAndEnergyOverflow() {
        GTRecipe source = source(Integer.MAX_VALUE / 4 + 1, 100);
        assertNull(DimensionalFocusRecipes.convert(source));
        source = source(120, 100);
        source.mFluidOutputs = new FluidStack[] { new FluidStack(WATER, 10) };
        assertNull(DimensionalFocusRecipes.convert(source));
    }

    @Test
    public void retainsAlternativeLensesFluidsAndChances() {
        GTRecipe source = GTRecipeBuilder.builder()
            .itemInputs(new Object[] { new ItemStack(WAFER, 2),
                new ItemStack[] { new ItemStack(LENS, 0), new ItemStack(DATA_STICK, 0) } })
            .itemOutputs(new ItemStack(PRODUCT, 3), new ItemStack(WAFER, 1)).outputChances(10000, 5000)
            .fluidInputs(new SubstituteFluidStack(new FluidStack(WATER, 100), DimensionalFocusFluids.photoresist(200)))
            .fluidInputChances(7500).duration(100).eut(120).buildWithAlt().get();
        GTRecipe converted = DimensionalFocusRecipes.convert(source);
        assertTrue(converted instanceof GTRecipe.GTRecipe_WithAlt);
        GTRecipe.GTRecipe_WithAlt alternatives = (GTRecipe.GTRecipe_WithAlt) converted;
        assertEquals(2, alternatives.mOreDictAlt[1].length);
        assertEquals(0, alternatives.mOreDictAlt[1][1].stackSize);
        assertEquals(2, converted.mAltFluidInputs[0].length);
        assertEquals(200, converted.mAltFluidInputs[0][1].amount);
        assertEquals(2, converted.mOutputs.length);
        assertEquals(5000, converted.mOutputChances[1]);
        assertEquals(7500, converted.mFluidInputChances[0]);
        assertEquals(10000, converted.mFluidInputChances[1]);
    }

    @Test
    public void requiresOutputSpecificResearchAtUhv() {
        GTRecipe converted = DimensionalFocusRecipes.convert(source(7_864_320, 100));
        assertTrue(DimensionalFocusResearch.requiresResearch(converted));
        assertEquals(1, DimensionalFocusResearch.requiredComputation(converted));
        assertEquals(DimensionalFocusResearch.fingerprint(converted.mOutputs[0]),
            converted.getMetadata(DimensionalFocusResearch.RESEARCH));
        ItemStack research = DimensionalFocusResearch.createResearchStick(converted.mOutputs[0]);
        assertTrue(
            DimensionalFocusResearch.hasToken(research, converted.getMetadata(DimensionalFocusResearch.RESEARCH)));
        assertFalse(DimensionalFocusResearch.hasToken(new ItemStack(DATA_STICK),
            converted.getMetadata(DimensionalFocusResearch.RESEARCH)));
        assertFalse(DimensionalFocusResearch.hasToken(research,
            DimensionalFocusResearch.fingerprint(new ItemStack(PRODUCT, 1, 1))));
    }

    @Test
    public void researchIdentityIgnoresCountAndCompoundInsertionOrder() {
        ItemStack first = new ItemStack(PRODUCT, 1);
        first.setTagCompound(new NBTTagCompound());
        first.getTagCompound().setString("a", "one");
        first.getTagCompound().setInteger("b", 2);
        ItemStack second = new ItemStack(PRODUCT, 64);
        second.setTagCompound(new NBTTagCompound());
        second.getTagCompound().setInteger("b", 2);
        second.getTagCompound().setString("a", "one");
        assertEquals(DimensionalFocusResearch.fingerprint(first), DimensionalFocusResearch.fingerprint(second));
        second.getTagCompound().setInteger("b", 3);
        assertNotEquals(DimensionalFocusResearch.fingerprint(first), DimensionalFocusResearch.fingerprint(second));
        second.getTagCompound().setInteger("b", 2);
        NBTTagCompound firstEntry = new NBTTagCompound();
        firstEntry.setString("label", "first");
        firstEntry.setInteger("value", 1);
        NBTTagCompound sameEntry = new NBTTagCompound();
        sameEntry.setInteger("value", 1);
        sameEntry.setString("label", "first");
        NBTTagCompound secondEntry = new NBTTagCompound();
        secondEntry.setString("label", "second");
        NBTTagList firstList = new NBTTagList();
        firstList.appendTag(firstEntry);
        firstList.appendTag(secondEntry);
        NBTTagList secondList = new NBTTagList();
        secondList.appendTag(sameEntry);
        secondList.appendTag(secondEntry.copy());
        first.getTagCompound().setTag("steps", firstList);
        second.getTagCompound().setTag("steps", secondList);
        NBTTagCompound before = (NBTTagCompound) first.getTagCompound().copy();
        String firstFingerprint = DimensionalFocusResearch.fingerprint(first);
        assertEquals(firstFingerprint, DimensionalFocusResearch.fingerprint(second));
        assertEquals(before, first.getTagCompound());
        assertEquals(2, firstList.tagCount());
        NBTTagList reordered = new NBTTagList();
        reordered.appendTag(secondEntry.copy());
        reordered.appendTag(firstEntry.copy());
        second.getTagCompound().setTag("steps", reordered);
        assertNotEquals(firstFingerprint, DimensionalFocusResearch.fingerprint(second));
        assertEquals(before, first.getTagCompound());
    }

    @Test
    public void tierAndPhotoresistBoundariesAreExact() {
        assertFalse(DimensionalFocusRecipeRules.usesEuv(1_048_576));
        assertTrue(DimensionalFocusRecipeRules.usesEuv(1_048_577));
        assertEquals(10, DimensionalFocusRecipeRules.photoresistAmount(1_048_576));
        assertEquals(5, DimensionalFocusRecipeRules.photoresistAmount(1_048_577));
        assertEquals(1, DimensionalFocusRecipeRules.photoresistAmount(1));
        assertEquals(1, DimensionalFocusRecipeRules.duration(4));
        assertEquals(0, DimensionalFocusRecipeRules.computation(7_864_319));
        assertEquals(1, DimensionalFocusRecipeRules.computation(8_388_608));
        assertEquals(2, DimensionalFocusRecipeRules.computation(8_388_609));
        assertEquals(3, DimensionalFocusRecipeRules.computation(125_829_120));
    }

    private static GTRecipe source(int eut, int duration) {
        return GTRecipeBuilder.builder().itemInputs(new Object[] { new ItemStack(WAFER, 2), new ItemStack(LENS, 0) })
            .itemOutputs(new ItemStack(PRODUCT, 3)).fluidInputs(new FluidStack(WATER, 100))
            .metadata(SAMPLE_METADATA, "retained").duration(duration).eut(eut).buildWithAlt().get();
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

}
