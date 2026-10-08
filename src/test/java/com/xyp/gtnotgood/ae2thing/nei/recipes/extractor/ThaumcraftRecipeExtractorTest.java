package com.xyp.gtnotgood.ae2thing.nei.recipes.extractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import com.xyp.gtnotgood.ae2thing.nei.recipes.FluidRecipe;

import codechicken.nei.PositionedStack;
import sun.reflect.ReflectionFactory;

/** Regression coverage for missing Thaumcraft overlays and excluding magical resource display stacks. */
public class ThaumcraftRecipeExtractorTest {

    private final ThaumcraftRecipeExtractor extractor = new ThaumcraftRecipeExtractor();

    @Test
    public void registersEveryArcaneAlchemyAndInfusionOverlay() {
        ThaumcraftRecipeExtractor.register();
        assertTrue(FluidRecipe.getSupportRecipes().containsAll(Arrays.asList("thaumcraft.arcane.shaped",
            "thaumcraft.arcane.shapeless", "thaumcraft.wands", "thaumcraft.alchemy", "thaumcraft.infusion")));
    }

    @Test
    public void importsOnlyTheResultAndNeverWandVisAsAnOutput() {
        PositionedStack output = stack(3, 75, 5);
        PositionedStack vis = stack(40, 10, 110);
        var result = extractor.getOutputIngredients(Arrays.asList(output, vis));
        assertEquals(1, result.size());
        assertEquals(3, ((ItemStack) result.get(0).getStack()).stackSize);
        assertNotSame(output.item, result.get(0).getStack());
        assertTrue(extractor.getOutputIngredients(Arrays.asList(null, vis)).isEmpty());
        assertTrue(extractor.getOutputIngredients(Collections.emptyList()).isEmpty());
    }

    @Test
    public void preservesPhysicalItemsAndRemovesVisAndEssentia() throws Exception {
        PositionedStack central = stack(1, 47, 38);
        ItemStack alternative = new ItemStack(new Item());
        central.items = new ItemStack[] { alternative, central.item };
        // A physical jar/crystal carrying aspect NBT must survive; filtering by NBT alone would remove it.
        PositionedStack physical = stack(4, 10, 150);
        physical.item.setTagCompound(new NBTTagCompound());
        physical.item.getTagCompound().setString("Aspect", "aer");
        PositionedStack essentia = aspect(80, 1);
        PositionedStack vis = aspect(40, 0);
        var result = extractor.getInputIngredients(Arrays.asList(central, essentia, null, vis, physical));
        assertEquals(2, result.size());
        assertSame(central.item.getItem(), ((ItemStack) result.get(0).getStack()).getItem());
        assertEquals(1, result.get(1).getIndex());
        ItemStack copied = (ItemStack) result.get(1).getStack();
        assertEquals(4, copied.stackSize);
        assertEquals("aer", copied.getTagCompound().getString("Aspect"));
        assertNotSame(physical.item.getTagCompound(), copied.getTagCompound());
        copied.stackSize = 1;
        assertEquals(4, physical.item.stackSize);
        assertTrue(extractor.getInputIngredients(Arrays.asList(vis, essentia)).isEmpty());
        assertTrue(extractor.getOutputIngredients(Collections.singletonList(essentia)).isEmpty());
    }

    /** Uses the real optional plugin item type while avoiding its Forge-only creative tab initialization. */
    private static PositionedStack aspect(int count, int metadata) throws Exception {
        var constructor = ReflectionFactory.getReflectionFactory().newConstructorForSerialization(
            Class.forName("com.gtnewhorizons.aspectrecipeindex.common.items.ItemAspect"), Item.class.getConstructor());
        Item item = (Item) constructor.newInstance();
        return new PositionedStack(new ItemStack(item, count, metadata), 10, 150, false);
    }

    private static PositionedStack stack(int count, int x, int y) {
        return new PositionedStack(new ItemStack(new Item(), count), x, y, false);
    }
}
