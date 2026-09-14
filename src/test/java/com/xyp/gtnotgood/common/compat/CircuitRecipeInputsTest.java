package com.xyp.gtnotgood.common.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import org.junit.Test;

/** Tests delegated alternative-input decisions and copy isolation; GT ore registration requires a running Forge. */
public class CircuitRecipeInputsTest {

    private static final Item RUBBER = new Item();
    private static final Item SILICONE = new Item();
    private static final Item WRONG = new Item();
    private static final FluidStack[] NO_FLUIDS = new FluidStack[0];

    @Test
    public void acceptedAlternativeIsConsumedOnlyInTheSimulationCopy() {
        ItemStack original = new ItemStack(SILICONE, 8);
        original.setTagCompound(new NBTTagCompound());
        original.getTagCompound()
            .setString("owner", "unchanged");
        assertEquals(
            4,
            CircuitRecipeInputs
                .simulate(4, null, new ItemStack[] { original }, NO_FLUIDS, (operations, copies, fluids) -> {
                    assertNotSame(original, copies[0]);
                    assertNotSame(original.getTagCompound(), copies[0].getTagCompound());
                    copies[0].getTagCompound()
                        .setString("owner", "simulation");
                    return consumeAllowedRings(operations, copies);
                }));
        assertEquals(8, original.stackSize);
        assertEquals(
            "unchanged",
            original.getTagCompound()
                .getString("owner"));
    }

    @Test
    public void mixedAlternativesFollowTheDelegatedMatcher() {
        ItemStack[] input = { new ItemStack(RUBBER), new ItemStack(SILICONE) };
        assertEquals(
            1,
            CircuitRecipeInputs.simulate(
                1,
                null,
                input,
                NO_FLUIDS,
                (operations, copies, fluids) -> consumeAllowedRings(operations, copies)));
        assertEquals(1, input[0].stackSize);
        assertEquals(1, input[1].stackSize);
    }

    @Test
    public void rejectedMaterialOrSurplusCannotPassValidation() {
        assertEquals(
            0,
            CircuitRecipeInputs.simulate(
                1,
                null,
                new ItemStack[] { new ItemStack(WRONG, 2) },
                NO_FLUIDS,
                (operations, copies, fluids) -> consumeAllowedRings(operations, copies)));
        ItemStack surplus = new ItemStack(SILICONE, 3);
        assertEquals(
            0,
            CircuitRecipeInputs.simulate(
                1,
                null,
                new ItemStack[] { surplus },
                NO_FLUIDS,
                (operations, copies, fluids) -> consumeAllowedRings(operations, copies)));
        assertEquals(3, surplus.stackSize);
    }

    @Test
    public void missingOrOversizedBatchIsRejectedBeforeCallingTheMatcher() {
        assertEquals(
            0,
            CircuitRecipeInputs.simulate(
                0,
                null,
                new ItemStack[0],
                NO_FLUIDS,
                (operations, copies, fluids) -> { throw new AssertionError("must not run"); }));
        assertEquals(
            0,
            CircuitRecipeInputs.simulate(
                (long) Integer.MAX_VALUE + 1,
                null,
                new ItemStack[0],
                NO_FLUIDS,
                (operations, copies, fluids) -> { throw new AssertionError("must not run"); }));
    }

    /** A deterministic test double; production delegates this decision to GTRecipe.isRecipeInputEqual. */
    private static boolean consumeAllowedRings(int operations, ItemStack[] items) {
        long needed = operations * 2L;
        for (ItemStack stack : items) {
            if (stack.getItem() != RUBBER && stack.getItem() != SILICONE) continue;
            int take = (int) Math.min(needed, stack.stackSize);
            stack.stackSize -= take;
            needed -= take;
        }
        return needed == 0;
    }
}
