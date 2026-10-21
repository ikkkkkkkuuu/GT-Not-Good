package com.xyp.gtnotgood.ae2thing.nei;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.ItemFluidContainer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.ae2thing.nei.recipes.extractor.GT5RecipeExtractor;
import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.Loader;
import sun.misc.Unsafe;

public class QuickTerminalRecipeTransferHandlerTest {

    @BeforeClass
    public static void initialize() throws Exception {
        Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        if (loader.get(null) == null) loader.set(null, ((Unsafe) unsafe.get(null)).allocateInstance(Loader.class));
        HeadlessBlockRegistry.bootstrap();
        if (ItemFluidDisplay.INSTANCE == null) new ItemFluidDisplay();
    }

    @Test
    public void gtExtractorKeepsFilledContainersAsItemIngredients() {
        ItemFluidContainer container = new ItemFluidContainer(0).setCapacity(1000);
        ItemStack filled = new ItemStack(container, 3);
        assertEquals(1000, container.fill(filled, new FluidStack(FluidRegistry.WATER, 1000), true));
        NBTTagCompound original = (NBTTagCompound) filled.getTagCompound().copy();
        PositionedStack positioned = new PositionedStack(filled, 0, 0, false);
        Object extracted = GT5RecipeExtractor.getFluidFromDisplay(positioned);
        assertTrue(extracted instanceof ItemStack);
        ItemStack item = (ItemStack) extracted;
        assertSame(container, item.getItem());
        assertEquals(3, item.stackSize);
        assertNotSame(filled, item);
        item.stackSize = 1;
        item.getTagCompound().setString("test", "changed");
        assertEquals(3, filled.stackSize);
        assertEquals(original, filled.getTagCompound());
        assertEquals(1000, container.getFluid(filled).amount);
    }

    @Test
    public void gtExtractorUnwrapsNewNeiDisplaysAndCopiesCachedFluid() {
        PositionedStack display = new PositionedStack(new FluidStack(FluidRegistry.LAVA, 144), 0, 0, false);
        NBTTagCompound original = (NBTTagCompound) display.items[0].getTagCompound().copy();
        FluidStack cached = StackInfo.getFluid(display.items[0]);
        var extracted = GT5RecipeExtractor.packOrderStacks(Collections.singletonList(display));
        assertEquals(1, extracted.size());
        assertEquals(0, extracted.get(0).getIndex());
        assertTrue(extracted.get(0).getStack() instanceof FluidStack);
        FluidStack fluid = (FluidStack) extracted.get(0).getStack();
        assertSame(FluidRegistry.LAVA, fluid.getFluid());
        assertEquals(144, fluid.amount);
        assertNotSame(cached, fluid);
        fluid.amount = 1;
        assertEquals(144, cached.amount);
        assertEquals(original, display.items[0].getTagCompound());
        assertEquals(144, ItemFluidDisplay.INSTANCE.getAmountLong(display.items[0]));
    }
}
