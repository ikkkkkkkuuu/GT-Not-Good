package com.xyp.gtnotgood.common.recipe.machine;

import com.xyp.gtnotgood.common.blocks.NativeStructureCasings;
import com.xyp.gtnotgood.common.blocks.StructureCasing;
import com.xyp.gtnotgood.common.recipe.gtnotgood.TransmutationRecipes;

import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;

public final class NativeCasingRecipes {

    private NativeCasingRecipes() {}

    public static void loadRecipes() {
        for (StructureCasing casing : NativeStructureCasings.variants()) {
            TransmutationRecipes.excludeRecoveryOutput(casing.getLegacyStack(1));
            GTRecipeBuilder.builder().itemInputs(casing.getLegacyStack(1)).circuit(25).itemOutputs(casing.getStack(1))
                .duration(50).eut(16).addTo(RecipeMaps.assemblerRecipes);
        }
    }
}
