package com.xyp.gtnotgood.common.recipe.machine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.imageio.ImageIO;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.xyp.gtnotgood.common.machines.processing.HeartGenerationRules;
import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;
import com.xyp.gtnotgood.utils.HeadlessItemRegistry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import gregtech.GTMod;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.common.GTProxy;
import sun.misc.Unsafe;

public class HeartOfTheUniverseRecipesTest {

    private static final Item FUEL = new Item().setUnlocalizedName("heart_recipe_test_fuel");
    private static final Item SEED = new Item().setUnlocalizedName("heart_recipe_test_seed");
    private static final Item CONSTRAINER = new Item().setUnlocalizedName("heart_recipe_test_constrainer");
    private static File originalMinecraftHome;
    private static Side originalLogSide;
    private static GTProxy originalProxy;

    @BeforeClass
    public static void initializeFixtures() throws Exception {
        Unsafe unsafe = (Unsafe) field(Unsafe.class, "theUnsafe").get(null);
        Field loader = field(Loader.class, "instance");
        if (loader.get(null) == null) loader.set(null, unsafe.allocateInstance(Loader.class));
        Field namedMods = field(Loader.class, "namedMods");
        if (namedMods.get(Loader.instance()) == null) namedMods.set(Loader.instance(), new HashMap<>());
        Field minecraftHome = field(FMLInjectionData.class, "minecraftHome");
        originalMinecraftHome = (File) minecraftHome.get(null);
        if (originalMinecraftHome == null) {
            File fixtureHome = new File("build/test-runtime/heart-recipes").getAbsoluteFile();
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
        Item[] items = { FUEL, SEED, CONSTRAINER };
        for (int index = 0; index < items.length; index++) {
            String name = "test:heart_recipe_" + index;
            register.invoke(Item.itemRegistry, 31960 + index, name, items[index]);
            field(items[index].delegate.getClass(), "name").set(items[index].delegate, name);
        }
        originalProxy = GTMod.proxy;
        if (originalProxy == null) GTMod.proxy = (GTProxy) unsafe.allocateInstance(GTProxy.class);
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
    }

    @Test
    public void generationRecipeConsumesBothItemsAndRetainsNativeByproductChance() {
        GTRecipe recipe = HeartOfTheUniverseRecipes
            .generationRecipe(new ItemStack(FUEL), new ItemStack(SEED), new ItemStack(CONSTRAINER, 64)).build().get();
        assertFalse(recipe.mFakeRecipe);
        assertTrue(recipe.mEnabled);
        assertEquals(0, recipe.mEUt);
        assertEquals(HeartGenerationRules.BASE_DURATION, recipe.mDuration);
        assertEquals(2, recipe.mInputs.length);
        assertEquals(1, recipe.mInputs[0].stackSize);
        assertEquals(1, recipe.mInputs[1].stackSize);
        assertEquals(64, recipe.mOutputs[0].stackSize);
        assertEquals(6000, recipe.getOutputChance(0));
        ItemStack[] inventory = { new ItemStack(FUEL, 2), new ItemStack(SEED, 2) };
        assertTrue(recipe.isRecipeInputEqual(false, false, new FluidStack[0], inventory));
        recipe.consumeInput(1, new FluidStack[0], inventory);
        assertEquals(1, inventory[0].stackSize);
        assertEquals(1, inventory[1].stackSize);
    }

    @Test
    public void blackHoleSeedRetainsAllFourAnimationFrames() throws Exception {
        String path = "/assets/gtnotgood/textures/items/heart/black_hole_seed.png";
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            BufferedImage image = ImageIO.read(stream);
            assertEquals(16, image.getWidth());
            assertEquals(64, image.getHeight());
        }
        try (InputStream stream = getClass().getResourceAsStream(path + ".mcmeta")) {
            assertNotNull(stream);
            String metadata = new String(ByteStreams.toByteArray(stream), StandardCharsets.UTF_8);
            assertTrue(metadata.replaceAll("\\s", "").contains("\"frametime\":4"));
        }
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

}
