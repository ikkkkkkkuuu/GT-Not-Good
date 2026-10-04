package com.xyp.gtnotgood.common.wireless;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.xyp.gtnotgood.common.recipe.machine.EasyWirelessRecipes;

import gregtech.api.enums.VoltageIndex;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessDynamoMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

public class EasyWirelessRecipesTest {

    @Test
    public void nativeClassesWithAddonRatingsAreSkipped() {
        for (int amps : new int[] { 0, -1, 3, 128, 214748364, Integer.MAX_VALUE }) {
            assertFalse(EasyWirelessRecipes.supports(input(VoltageIndex.UXV, amps)));
            assertFalse(
                EasyWirelessRecipes.supports(
                    new MTEHatchWirelessDynamoMulti("addonDynamo", VoltageIndex.UXV, amps, new String[0], null)));
        }
    }

    @Test
    public void nativeRecipeRatingsRemainSupported() {
        for (int amps : new int[] { 4, 16, 64, 256, 1024, 4096, 16384, 65536, 262144, 1048576, 4194304, 16777216 }) {
            assertTrue(EasyWirelessRecipes.supports(input(VoltageIndex.UXV, amps)));
        }
    }

    @Test
    public void ratingsWithoutVoltageComponentsAreSkipped() {
        assertFalse(EasyWirelessRecipes.supports(input(VoltageIndex.ULV, 256)));
        assertFalse(EasyWirelessRecipes.supports(input(VoltageIndex.MAX + 1, 4)));
        assertTrue(EasyWirelessRecipes.supports(input(VoltageIndex.ULV, 4)));
        assertTrue(EasyWirelessRecipes.supports(input(VoltageIndex.MAX, 256)));
    }

    private static MTEHatchWirelessMulti input(int tier, int amps) {
        return new MTEHatchWirelessMulti("addonInput", tier, amps, new String[0], null);
    }
}
