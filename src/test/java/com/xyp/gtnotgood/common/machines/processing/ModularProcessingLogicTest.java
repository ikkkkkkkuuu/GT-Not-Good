package com.xyp.gtnotgood.common.machines.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import cpw.mods.fml.common.Loader;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import sun.misc.Unsafe;

public class ModularProcessingLogicTest {

    @BeforeClass
    public static void bootstrapNativeFluids() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        if (loader.get(null) == null) loader.set(null, unsafe.allocateInstance(Loader.class));
        Field namedMods = Loader.class.getDeclaredField("namedMods");
        namedMods.setAccessible(true);
        if (namedMods.get(Loader.instance()) == null) namedMods.set(Loader.instance(), new HashMap<>());
        HeadlessBlockRegistry.bootstrap();
    }

    @Test
    public void preparesItemAndFluidConsumptionWithoutTouchingTheLiveInventory() {
        ItemStack stock = new ItemStack(new Item(), 8);
        FluidStack tank = new FluidStack(FluidRegistry.WATER, 1000);
        TestLogic logic = logic(stock, tank);
        logic.prepareModuleRecipe(128, true);

        assertTrue(logic.process().wasSuccessful());
        assertEquals(8, stock.stackSize);
        assertEquals(1000, tank.amount);
        assertTrue(logic.isModuleOverclockDisabled());
        assertTrue(logic.canCommitInputs());
        assertTrue(logic.commitInputs());
        assertEquals(6, stock.stackSize);
        assertEquals(750, tank.amount);
        assertFalse(logic.isPreparingModuleRecipe());
        assertFalse(logic.commitInputs());
    }

    @Test
    public void discardingAnUnfundedExecutionLeavesBothInputsUntouched() {
        ItemStack stock = new ItemStack(new Item(), 8);
        FluidStack tank = new FluidStack(FluidRegistry.WATER, 1000);
        TestLogic logic = logic(stock, tank);
        logic.prepareModuleRecipe(128, true);
        assertTrue(logic.process().wasSuccessful());

        logic.finishModuleRecipe();

        assertFalse(logic.commitInputs());
        assertEquals(8, stock.stackSize);
        assertEquals(1000, tank.amount);
    }

    @Test
    public void refusesAStaleInventoryWithoutPartiallyConsumingTheOtherInput() {
        ItemStack stock = new ItemStack(new Item(), 8);
        FluidStack tank = new FluidStack(FluidRegistry.WATER, 1000);
        TestLogic logic = logic(stock, tank);
        logic.prepareModuleRecipe(128, false);
        assertTrue(logic.process().wasSuccessful());
        tank.amount = 900;

        assertFalse(logic.canCommitInputs());
        assertFalse(logic.commitInputs());
        assertEquals(8, stock.stackSize);
        assertEquals(900, tank.amount);
    }

    @Test
    public void refusesChangedInputNbtAfterPreparation() {
        ItemStack stock = new ItemStack(new Item(), 8);
        stock.setTagCompound(new NBTTagCompound());
        stock.getTagCompound().setString("recipe", "original");
        TestLogic logic = logic(stock, new FluidStack(FluidRegistry.WATER, 1000));
        logic.prepareModuleRecipe(128, false);
        assertTrue(logic.process().wasSuccessful());
        stock.getTagCompound().setString("recipe", "changed");

        assertFalse(logic.commitInputs());
        assertEquals(8, stock.stackSize);
    }

    @Test
    public void preservesAliasedSlotsAndPreparesFreshCopiesForEachAttempt() {
        ItemStack stock = new ItemStack(new Item(), 8);
        FluidStack tank = new FluidStack(FluidRegistry.WATER, 1000);
        TestLogic logic = logic(stock, tank);
        logic.setInputItems(stock, stock);
        logic.setInputFluids(tank, tank);
        logic.expectAliases = true;
        logic.prepareModuleRecipe(128, false);

        assertTrue(logic.process().wasSuccessful());
        assertTrue(logic.process().wasSuccessful());
        assertTrue(logic.commitInputs());
        assertEquals(6, stock.stackSize);
        assertEquals(750, tank.amount);
    }

    @Test
    public void restoresLiveInputReferencesWhenRecipePreparationThrows() {
        ItemStack stock = new ItemStack(new Item(), 8);
        FluidStack tank = new FluidStack(FluidRegistry.WATER, 1000);
        TestLogic logic = logic(stock, tank);
        logic.fail = true;
        logic.prepareModuleRecipe(128, false);
        try {
            logic.process();
            throw new AssertionError("Recipe preparation must fail");
        } catch (IllegalStateException expected) {
            assertSame(stock, logic.liveItem());
            assertSame(tank, logic.liveFluid());
            assertEquals(8, stock.stackSize);
            assertEquals(1000, tank.amount);
        }
    }

    private static TestLogic logic(ItemStack stock, FluidStack tank) {
        TestLogic logic = new TestLogic();
        logic.setInputItems(stock);
        logic.setInputFluids(tank);
        return logic;
    }

    private static final class TestLogic extends ModularProcessingLogic {

        private boolean expectAliases;
        private boolean fail;

        @Override
        protected CheckRecipeResult processRecipe() {
            if (expectAliases) {
                assertSame(inputItems[0], inputItems[1]);
                assertSame(inputFluids[0], inputFluids[1]);
            }
            inputItems[0].stackSize -= 2;
            inputFluids[0].amount -= 250;
            if (fail) throw new IllegalStateException("Fixture recipe failure");
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }

        private ItemStack liveItem() {
            return inputItems[0];
        }

        private FluidStack liveFluid() {
            return inputFluids[0];
        }
    }
}
