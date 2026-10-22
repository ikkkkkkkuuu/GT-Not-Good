package com.xyp.gtnotgood.common.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessItemRegistry;

import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.RecipeItemInput;
import sun.misc.Unsafe;

public class CircuitPatternMoldsMatchingTest {

    private static final Item REPRESENTATIVE = new Item();
    private static final Item ALTERNATIVE = new Item();
    private static final Item OTHER_COLOR = new Item();
    private static Unsafe unsafe;

    @BeforeClass
    public static void initializeNativeMatching() throws Exception {
        unsafe = (Unsafe) field(Unsafe.class, "theUnsafe").get(null);
        HeadlessItemRegistry.bootstrap();
    }

    @Test
    public void reusableAlternativeDoesNotHaveToBeTheRepresentativeItem() throws Exception {
        ItemStack representative = new ItemStack(REPRESENTATIVE, 0);
        ItemStack alternative = new ItemStack(ALTERNATIVE);
        assertNotSame(representative.getItem(), alternative.getItem());
        GTRecipe recipe = recipe(representative, input(0, false, representative, alternative));
        assertNotSame(recipe.mInputs[0].getItem(), alternative.getItem());
        assertTrue(CircuitPatternMolds.isNonConsumed(recipe, representative));
        assertTrue(CircuitPatternMolds.isNonConsumed(recipe, alternative));
    }

    @Test
    public void unrelatedLensColorIsNotAReusableToolForTheRecipe() throws Exception {
        GTRecipe recipe = recipe(new ItemStack(REPRESENTATIVE, 0),
            input(0, false, new ItemStack(REPRESENTATIVE), new ItemStack(ALTERNATIVE)));
        assertFalse(CircuitPatternMolds.isNonConsumed(recipe, new ItemStack(OTHER_COLOR)));
    }

    @Test
    public void positiveQuantityAlternativeRemainsARealInput() throws Exception {
        GTRecipe recipe = recipe(new ItemStack(REPRESENTATIVE, 2),
            input(2, false, new ItemStack(REPRESENTATIVE), new ItemStack(ALTERNATIVE)));
        assertFalse(CircuitPatternMolds.isNonConsumed(recipe, new ItemStack(ALTERNATIVE)));
    }

    @Test
    public void consumedSlotPreventsVirtualizingAnAlsoReusableAlternative() throws Exception {
        RecipeItemInput reusable = input(0, false, new ItemStack(REPRESENTATIVE), new ItemStack(ALTERNATIVE));
        RecipeItemInput consumed = input(1, false, new ItemStack(ALTERNATIVE));
        GTRecipe recipe = recipe(new ItemStack(REPRESENTATIVE, 0), reusable, consumed);
        assertFalse(CircuitPatternMolds.isNonConsumed(recipe, new ItemStack(ALTERNATIVE)));
    }

    @Test
    public void alternativeMatchingPreservesTheNativeNbtRule() throws Exception {
        ItemStack configured = new ItemStack(ALTERNATIVE);
        configured.setTagCompound(new NBTTagCompound());
        configured.getTagCompound().setString("configuration", "encoded");
        ItemStack different = configured.copy();
        different.getTagCompound().setString("configuration", "different");
        GTRecipe sensitive = recipe(new ItemStack(REPRESENTATIVE, 0), input(0, true, configured));
        assertTrue(CircuitPatternMolds.isNonConsumed(sensitive, configured.copy()));
        assertFalse(CircuitPatternMolds.isNonConsumed(sensitive, different));
        GTRecipe insensitive = recipe(new ItemStack(REPRESENTATIVE, 0), input(0, false, configured));
        assertTrue(CircuitPatternMolds.isNonConsumed(insensitive, different));
    }

    private static RecipeItemInput input(long amount, boolean nbtSensitive, ItemStack... alternatives) {
        // Native GT falls back to explicit alternatives when Forge's ore registry is unavailable.
        return new RecipeItemInput(Integer.MAX_VALUE, null, amount, nbtSensitive, alternatives);
    }

    private static GTRecipe recipe(ItemStack representative, RecipeItemInput... inputs) throws Exception {
        GTRecipe recipe = (GTRecipe) unsafe.allocateInstance(GTRecipe.class);
        recipe.mInputs = new ItemStack[] { representative };
        field(GTRecipe.class, "inputsAtCacheTime").set(recipe, recipe.mInputs);
        field(GTRecipe.class, "mergedInputCache").set(recipe, inputs);
        return recipe;
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
