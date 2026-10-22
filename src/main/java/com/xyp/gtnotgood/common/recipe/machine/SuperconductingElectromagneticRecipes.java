// SPDX-License-Identifier: CC-BY-NC-SA-4.0
package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.blocks.particlecollider.ParticleColliderCasing;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import bartworks.system.material.WerkstoffLoader;
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

public final class SuperconductingElectromagneticRecipes {

    private SuperconductingElectromagneticRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder().fluidInputs(fluid(Materials.Air.getGas(10000)))
            .fluidOutputs(fluid(Materials.NitricOxide.getGas(1000))).duration(200).eut(TierEU.RECIPE_MV)
            .addTo(GTNGRecipeMaps.LightningProcessorRecipes);
    }

    public static void loadPostInitRecipes() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, ParticleColliderCasing.SuperconductingCoil.getStack(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UEV))
            .itemInputs(GregtechItemList.Industrial_Electrolyzer.get(4),
                ItemList.Machine_Multi_IndustrialElectromagneticSeparator.get(4),
                GregtechItemList.Industrial_Arc_Furnace.get(4), ItemList.Emitter_UV.get(16),
                new Object[] { OrePrefixes.circuit.get(Materials.UEV), 8 },
                stack(OrePrefixes.wireFine, Materials.SuperconductorZPMBase, 64),
                stack(OrePrefixes.wireFine, Materials.SuperconductorZPMBase, 64),
                stack(OrePrefixes.wireFine, Materials.SuperconductorZPMBase, 64),
                stack(OrePrefixes.wireFine, Materials.SuperconductorZPMBase, 64),
                stack(OrePrefixes.plateDouble, Materials.Tantalum, 8),
                stack(OrePrefixes.plateDouble, Materials.Silver, 8),
                stack(OrePrefixes.plateDouble, Materials.Silicon, 8),
                Objects.requireNonNull(WerkstoffLoader.Ruthenium.get(OrePrefixes.plate, 16),
                    "Missing ruthenium plates"))
            .fluidInputs(fluid(MaterialsAlloy.INDALLOY_140.getFluidStack(2880)),
                fluid(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(2880)),
                fluid(Materials.SolderingAlloy.getMolten(5760)), fluid(Materials.SuperconductorUHVBase.getMolten(5760)))
            .itemOutputs(GTNGItemList.SuperconductingElectromagneticFactory.get(1)).duration(1200)
            .eut(TierEU.RECIPE_UEV).addTo(AssemblyLine);
    }

    private static ItemStack stack(OrePrefixes prefix, Materials material, long amount) {
        return Objects.requireNonNull(GTOreDictUnificator.get(prefix, material, amount),
            "Missing native electromagnetic material: " + prefix + material);
    }

    private static FluidStack fluid(FluidStack stack) {
        return Objects.requireNonNull(stack, "Missing native electromagnetic fluid");
    }
}
