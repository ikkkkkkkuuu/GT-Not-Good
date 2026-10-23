// SPDX-License-Identifier: CC-BY-NC-SA-4.0
package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.Objects;

import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.blocks.particlecollider.ParticleColliderCasing;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.core.material.MaterialsElements;

public final class SuperParticleColliderRecipes {

    private SuperParticleColliderRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahEnriched, 1),
                MaterialsAlloy.LAFIUM.getPlate(6), GTUtility.getIntegratedCircuit(6))
            .itemOutputs(ParticleColliderCasing.Lafium.getStack(2)).duration(50).eut(16)
            .addTo(RecipeMaps.assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.pipeQuadruple, Materials.NiobiumTitanium, 1),
                ItemList.LuV_Coil.get(2), new Object[] { OrePrefixes.circuit.get(Materials.LuV), 1 },
                GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.NiobiumTitanium, 1),
                Materials.Copper.getDust(1), GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Neodymium, 1))
            .fluidInputs(Materials.SolderingAlloy.getMolten(288))
            .itemOutputs(ParticleColliderCasing.AcceleratedPipeline.getStack(1)).duration(400).eut(TierEU.RECIPE_IV)
            .requiresCleanRoom().addTo(RecipeMaps.assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(ParticleColliderCasing.SuperconductingCoil.getLegacyStack(1), GTUtility.getIntegratedCircuit(6))
            .itemOutputs(ItemList.Casing_Coil_Superconductor.get(1)).duration(50).eut(16)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    public static void loadPostInitRecipes() {
        MaterialsElements elements = MaterialsElements.getInstance();
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, ParticleColliderCasing.AcceleratedPipeline.getStack(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_ZPM))
            .itemInputs(ItemList.Field_Generator_ZPM.get(16), ItemList.Electric_Pump_ZPM.get(16),
                ItemList.Circuit_Chip_UHPIC.get(64), ItemList.Circuit_Chip_UHPIC.get(64),
                ItemList.Circuit_Chip_UHPIC.get(64), ItemList.Circuit_Chip_UHPIC.get(64),
                GTOreDictUnificator.get(OrePrefixes.wireGt16, Materials.SuperconductorZPM, 16),
                new Object[] { OrePrefixes.circuit.get(Materials.UV), 32 }, ItemList.Neutron_Reflector.get(16),
                MaterialsAlloy.LAFIUM.getPlateDouble(8))
            .fluidInputs(Materials.SolderingAlloy.getMolten(1152), elements.PROTACTINIUM.getFluidStack(1152),
                Materials.Uranium.getMolten(1152), elements.NEPTUNIUM.getFluidStack(1152))
            .itemOutputs(GTNGItemList.SuperParticleCollider.get(1)).duration(800).eut(TierEU.RECIPE_ZPM)
            .addTo(AssemblyLine);

        collision(Materials.Plutonium.getMolten(4096), elements.CURIUM.getFluidStack(4000));
        collision(Materials.Thorium.getMolten(4096), Materials.Uranium.getMolten(4000));
        collision(Materials.Uranium.getMolten(4096), Materials.Plutonium.getMolten(4000));
        collision(elements.PROTACTINIUM.getFluidStack(4096), elements.NEPTUNIUM.getFluidStack(4000));
        collision(elements.CALIFORNIUM.getFluidStack(4096), elements.FERMIUM.getFluidStack(4000));

        collision(Materials.Molybdenum.getMolten(4096), elements.RUTHENIUM.getFluidStack(4000));
        collision(elements.RUTHENIUM.getFluidStack(4096), Materials.Palladium.getMolten(4000));
        collision(Materials.Tantalum.getMolten(4096), elements.RHENIUM.getFluidStack(4000));
        collision(elements.RHENIUM.getFluidStack(4096), Materials.Iridium.getMolten(4000));
        collision(Materials.Tungsten.getMolten(4096), Materials.Osmium.getMolten(4000));
        collision(Materials.Osmium.getMolten(4096), Materials.Platinum.getMolten(4000));

        collision(Materials.Praseodymium.getMolten(4096), Materials.Promethium.getMolten(4000));
        collision(Materials.Promethium.getMolten(4096), Materials.Europium.getMolten(4000));
        collision(Materials.Neodymium.getMolten(4096), Materials.Samarium.getMolten(4000));
        collision(Materials.Samarium.getMolten(4096), Materials.Gadolinium.getMolten(4000));
        collision(Materials.Thulium.getMolten(4096), Materials.Lutetium.getMolten(4000));

        collision(elements.NEPTUNIUM.getFluidStack(4096), Materials.Americium.getMolten(4000));
        collision(elements.CURIUM.getFluidStack(4096), elements.CALIFORNIUM.getFluidStack(4000), TierEU.RECIPE_UHV);
    }

    private static void collision(FluidStack input, FluidStack output) {
        collision(input, output, TierEU.RECIPE_UV);
    }

    private static void collision(FluidStack input, FluidStack output, long eut) {
        GTRecipeBuilder.builder()
            .fluidInputs(Objects.requireNonNull(input, "Missing native collision input"),
                Objects.requireNonNull(Materials.Helium.getPlasma(4096), "Missing helium plasma"))
            .fluidOutputs(Objects.requireNonNull(output, "Missing native collision output")).duration(200).eut(eut)
            .addTo(GTNGRecipeMaps.SuperParticleColliderRecipes);
    }
}
