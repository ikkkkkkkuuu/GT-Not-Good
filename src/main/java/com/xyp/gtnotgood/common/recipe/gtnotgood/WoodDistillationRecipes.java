// SPDX-License-Identifier: LGPL-3.0-only
// GT Not Leisure recipe port; see META-INF/chemical-recipes-port/NOTICE.md.
package com.xyp.gtnotgood.common.recipe.gtnotgood;

import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.objects.OreDictItemStack;
import gtPlusPlus.core.fluids.GTPPFluids;

public final class WoodDistillationRecipes {

    private WoodDistillationRecipes() {}

    public static void loadRecipes() {
        GTValues.RA.stdBuilder().itemInputs(new OreDictItemStack("logWood", 16))
            .itemOutputs(Materials.AshDark.getDust(4)).fluidInputs(Materials.Nitrogen.getGas(1000))
            .fluidOutputs(Materials.Water.getFluid(400), Materials.Methanol.getFluid(240),
                Materials.Benzene.getFluid(175), Materials.CarbonMonoxide.getGas(170), Materials.Creosote.getFluid(150),
                Materials.Dimethylbenzene.getFluid(120), Materials.AceticAcid.getFluid(80),
                Materials.Methane.getGas(60), Materials.Acetone.getFluid(40), Materials.Phenol.getFluid(35),
                Materials.Toluene.getFluid(35), Materials.Ethylene.getGas(10), Materials.Hydrogen.getGas(10),
                Materials.MethylAcetate.getFluid(8), new FluidStack(GTPPFluids.CoalGas, 8),
                Materials.Ethanol.getFluid(8))
            .duration(200).eut(120).addTo(GTNGRecipeMaps.WoodDistillationRecipes);
    }
}
