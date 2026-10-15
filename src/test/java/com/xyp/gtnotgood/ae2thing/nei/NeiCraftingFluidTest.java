package com.xyp.gtnotgood.ae2thing.nei;

import static org.junit.Assert.*;

import java.lang.reflect.Field;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.ItemFluidContainer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.Loader;
import sun.misc.Unsafe;

public class NeiCraftingFluidTest {

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
    public void zeroVolumeNeiDisplayResolvesWithoutChangingDisplayOrCache() {
        ItemStack display = ItemFluidDisplay.createStack(FluidRegistry.WATER, 0);
        FluidStack cached = StackInfo.getFluid(display);
        FluidStack target = NeiCraftingFluid.getFluid(display);
        assertNotNull(target);
        assertSame(FluidRegistry.WATER, target.getFluid());
        assertEquals(1, target.amount);
        assertNotSame(cached, target);
        assertEquals(0, cached.amount);
        assertEquals(0, ItemFluidDisplay.INSTANCE.getAmountLong(display));
    }

    @Test
    public void largeBookmarkAmountDoesNotChangeCraftIdentity() {
        ItemStack display = ItemFluidDisplay.createStack(FluidRegistry.LAVA, Long.MAX_VALUE);
        FluidStack target = NeiCraftingFluid.getFluid(display);
        assertSame(FluidRegistry.LAVA, target.getFluid());
        assertEquals(1, target.amount);
        assertEquals(Long.MAX_VALUE, ItemFluidDisplay.INSTANCE.getAmountLong(display));
    }

    @Test
    public void ordinaryItemsAndFilledContainersRemainItemRequests() {
        assertNull(NeiCraftingFluid.getFluid(null));
        assertNull(NeiCraftingFluid.getFluid(new ItemStack(new Item())));
        ItemFluidContainer container = new ItemFluidContainer(1000);
        ItemStack filled = new ItemStack(container);
        container.fill(filled, new FluidStack(FluidRegistry.WATER, 1000), true);
        assertNull(NeiCraftingFluid.getFluid(filled));
    }
}
