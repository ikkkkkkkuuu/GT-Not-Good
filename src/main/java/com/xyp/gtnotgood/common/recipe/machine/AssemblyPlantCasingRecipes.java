// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.recipe.machine;

import java.util.Objects;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

public final class AssemblyPlantCasingRecipes {

    private AssemblyPlantCasingRecipes() {}

    public static void loadRecipes() {

        GTRecipeBuilder.builder()
            .itemInputs(stack(OrePrefixes.wireGt02, Materials.Platinum, 4),
                stack(OrePrefixes.cableGt01, Materials.Tungsten, 1), Casings.ChemicallyInertMachineCasing.toStack(1),
                new Object[] { OrePrefixes.circuit.get(Materials.IV), 1 })
            .itemOutputs(AssemblyPlantCasing.ElectrolyticCell.getStack(2)).duration(50).eut(16)
            .addTo(RecipeMaps.assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(stack(OrePrefixes.ring, Materials.Molybdenum, 32), Materials.Silicon.getDust(16),
                Materials.Graphene.getDust(4))
            .fluidInputs(Objects.requireNonNull(Materials.TungstenSteel.getMolten(144), "Missing tungstensteel fluid"))
            .itemOutputs(AssemblyPlantCasing.MolybdenumDisilicideCoil.getStack(1)).duration(500).eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static ItemStack stack(OrePrefixes prefix, Materials material, long amount) {
        return Objects.requireNonNull(GTOreDictUnificator.get(prefix, material, amount),
            "Missing native casing material: " + prefix + material);
    }
}
