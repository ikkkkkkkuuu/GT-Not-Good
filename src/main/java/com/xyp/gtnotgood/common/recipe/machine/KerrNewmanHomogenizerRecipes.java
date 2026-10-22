// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;

public final class KerrNewmanHomogenizerRecipes {

    private KerrNewmanHomogenizerRecipes() {}

    public static void loadPostInitRecipes() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, GregtechItemList.Industrial_Mixer.get(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UEV))
            .itemInputs(ItemList.MixerUHV.get(16), ItemList.Electric_Pump_UEV.get(16),
                ItemList.Electric_Motor_UEV.get(16), ItemList.Robot_Arm_UEV.get(16), ItemList.Sensor_UEV.get(8),
                new Object[] { OrePrefixes.circuit.get(Materials.UIV), 8 },
                stack(OrePrefixes.stickLong, Materials.Osmium, 32),
                stack(OrePrefixes.stickLong, Materials.Europium, 32),
                Objects.requireNonNull(MaterialsAlloy.QUANTUM.getPlate(32), "Missing Quantum plates"))
            .fluidInputs(fluid(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(1000), "Mutated Living Solder"),
                fluid(MaterialsAlloy.TITANSTEEL.getFluidStack(1000), "Titansteel"),
                fluid(MaterialsAlloy.ABYSSAL.getFluidStack(1000), "Abyssal Alloy"))
            .itemOutputs(GTNGItemList.KerrNewmanHomogenizer.get(1)).duration(800).eut(2097152).addTo(AssemblyLine);
    }

    private static ItemStack stack(OrePrefixes prefix, Materials material, long amount) {
        return Objects.requireNonNull(GTOreDictUnificator.get(prefix, material, amount),
            "Missing Kerr-Newman ingredient: " + prefix + material);
    }

    private static FluidStack fluid(FluidStack stack, String material) {
        return Objects.requireNonNull(stack, "Missing Kerr-Newman fluid: " + material);
    }
}
