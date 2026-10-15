package com.xyp.gtnotgood.common.machines.crop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gtnewhorizon.cropsnh.farming.registries.FertilizerRegistry;
import com.gtnewhorizon.cropsnh.farming.registries.HydrationRegistry;
import com.xyp.gtnotgood.utils.HeadlessBlockRegistry;

import cpw.mods.fml.common.Loader;
import sun.misc.Unsafe;

public class IndustrialFarmFluidsTest {

    private static Fluid water;
    private static Fluid fertilizer;
    private static Fluid dualPurpose;
    private static Fluid potentWater;

    @BeforeClass
    public static void registerFluids() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Field loader = Loader.class.getDeclaredField("instance");
        loader.setAccessible(true);
        if (loader.get(null) == null) loader.set(null, ((Unsafe) field.get(null)).allocateInstance(Loader.class));
        HeadlessBlockRegistry.bootstrap();
        water = register("industrial_farm_test_water");
        fertilizer = register("industrial_farm_test_fertilizer");
        dualPurpose = register("industrial_farm_test_dual");
        potentWater = register("industrial_farm_test_potent");
        HydrationRegistry.instance.register(water, 1);
        FertilizerRegistry.instance.register(fertilizer, 1);
        HydrationRegistry.instance.register(dualPurpose, 2);
        FertilizerRegistry.instance.register(dualPurpose, 3);
        HydrationRegistry.instance.register(potentWater, Integer.MAX_VALUE);
    }

    private static Fluid register(String name) {
        Fluid fluid = new Fluid(name);
        FluidRegistry.registerFluid(fluid);
        return fluid;
    }

    @Test
    public void duplicateReferencesCannotFundMoreThanTheirActualVolume() {
        FluidStack shared = new FluidStack(dualPurpose, 8);
        IndustrialFarmFluids.Plan plan = IndustrialFarmFluids.plan(Arrays.asList(shared, shared), 20, 0, false);
        assertFalse(plan.hasWater());
        assertFalse(plan.commit(false));
        assertEquals(8, shared.amount);
    }

    @Test
    public void waterAndFertilizerShareOneSourceWithoutDoubleConsumption() {
        FluidStack shared = new FluidStack(dualPurpose, 8);
        IndustrialFarmFluids.Plan plan = IndustrialFarmFluids.plan(Arrays.asList(shared, shared), 8, 12, false);
        assertTrue(plan.hasWater());
        assertTrue(plan.hasFertilizer());
        assertEquals(8, shared.amount);
        assertTrue(plan.commit(true));
        assertEquals(0, shared.amount);
        assertFalse(plan.commit(true));
    }

    @Test
    public void missingFertilizerAllowsOnlyTheWaterTransaction() {
        FluidStack shared = new FluidStack(dualPurpose, 8);
        IndustrialFarmFluids.Plan plan = IndustrialFarmFluids.plan(Collections.singletonList(shared), 8, 13, false);
        assertTrue(plan.hasWater());
        assertFalse(plan.hasFertilizer());
        assertFalse(plan.commit(true));
        assertEquals(8, shared.amount);
        assertTrue(plan.commit(false));
        assertEquals(4, shared.amount);
    }

    @Test
    public void changedSourceRejectsTheWholeTransactionBeforeDrainingWater() {
        FluidStack waterStack = new FluidStack(water, 10);
        FluidStack fertilizerStack = new FluidStack(fertilizer, 5);
        IndustrialFarmFluids.Plan plan = IndustrialFarmFluids.plan(Arrays.asList(waterStack, fertilizerStack), 5, 4,
            false);
        fertilizerStack.amount = 3;
        assertFalse(plan.commit(true));
        assertEquals(10, waterStack.amount);
        assertEquals(3, fertilizerStack.amount);
        fertilizerStack.amount = 5;
        assertTrue(plan.commit(true));
        assertEquals(5, waterStack.amount);
        assertEquals(1, fertilizerStack.amount);
    }

    @Test
    public void largeGoalsUseLongArithmeticAndRoundUpTheLastMillibucket() {
        FluidStack first = new FluidStack(potentWater, Integer.MAX_VALUE);
        FluidStack last = new FluidStack(potentWater, 1);
        long needed = (long) Integer.MAX_VALUE * Integer.MAX_VALUE + 1;
        IndustrialFarmFluids.Plan plan = IndustrialFarmFluids.plan(Arrays.asList(first, last), needed, 0, false);
        assertTrue(plan.hasWater());
        assertTrue(plan.commit(false));
        assertEquals(0, first.amount);
        assertEquals(0, last.amount);

        FluidStack insufficient = new FluidStack(potentWater, Integer.MAX_VALUE);
        IndustrialFarmFluids.Plan unmet = IndustrialFarmFluids.plan(Collections.singletonList(insufficient),
            Long.MAX_VALUE, 0, false);
        assertFalse(unmet.hasWater());
        assertFalse(unmet.commit(false));
        assertEquals(Integer.MAX_VALUE, insufficient.amount);
    }

    @Test
    public void enrichedModeRejectsOrdinaryRegisteredFertilizer() {
        FluidStack stack = new FluidStack(fertilizer, 10);
        IndustrialFarmFluids.Plan plan = IndustrialFarmFluids.plan(Collections.singletonList(stack), 0, 1, true);
        assertTrue(plan.hasWater());
        assertFalse(plan.hasFertilizer());
        assertTrue(plan.commit(false));
        assertEquals(10, stack.amount);
    }
}
