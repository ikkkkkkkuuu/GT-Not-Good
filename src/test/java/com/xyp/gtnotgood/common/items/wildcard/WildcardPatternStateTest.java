package com.xyp.gtnotgood.common.items.wildcard;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.xyp.gtnotgood.common.items.wildcard.model.WildcardModelState;
import com.xyp.gtnotgood.common.items.wildcard.model.io.FluidIOComponent;

import gregtech.api.enums.FluidState;

/** Covers the shared quantity changes used by the pattern optimization matrix. */
public class WildcardPatternStateTest {

    @Test
    public void scalesEveryModelInputAndOutputTogether() {
        ItemStack pattern = new ItemStack(new Item());
        WildcardModelState.ensureInitialized(pattern);
        WildcardModelState.setInputs(pattern, Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, 144)));
        WildcardModelState.setOutputs(pattern, Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, 288)));

        WildcardPatternState.applyBitModification(pattern, 2);
        assertEquals(576, ((FluidIOComponent) WildcardModelState.getInputs(pattern).get(0)).getAmount());
        assertEquals(1152, ((FluidIOComponent) WildcardModelState.getOutputs(pattern).get(0)).getAmount());

        WildcardPatternState.applyBitModification(pattern, -2);
        assertEquals(144, ((FluidIOComponent) WildcardModelState.getInputs(pattern).get(0)).getAmount());
        assertEquals(288, ((FluidIOComponent) WildcardModelState.getOutputs(pattern).get(0)).getAmount());
    }

    @Test
    public void refusesMultiplierWhenAnyComponentWouldOverflow() {
        ItemStack pattern = new ItemStack(new Item());
        WildcardModelState.ensureInitialized(pattern);
        WildcardModelState.setInputs(pattern,
            Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, WildcardPatternEntry.MAX_AMOUNT)));
        WildcardModelState.setOutputs(pattern, Collections.singletonList(new FluidIOComponent(FluidState.MOLTEN, 1)));

        assertEquals(0, WildcardPatternState.getMaxBitMultiplier(pattern));
        WildcardPatternState.applyBitModification(pattern, 1);
        assertEquals(WildcardPatternEntry.MAX_AMOUNT,
            ((FluidIOComponent) WildcardModelState.getInputs(pattern).get(0)).getAmount());
        assertEquals(1, ((FluidIOComponent) WildcardModelState.getOutputs(pattern).get(0)).getAmount());
    }
}
