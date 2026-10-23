// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.CLEANROOM;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.blocks.tentacle.TentacleCasing;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import goodgenerator.util.ItemRefer;
import gregtech.api.casing.Casings;
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

public final class NyarlathotepsTentacleRecipes {

    private NyarlathotepsTentacleRecipes() {}

    public static void loadPostInitRecipes() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, GTNGItemList.MageAssembler.get(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UEV))
            .itemInputs(ItemList.Energy_Module.get(64), ItemList.Machine_Multi_Assemblyline.get(64),
                ModsItemlist.CircuitAssemblyLine.get(64), TentacleCasing.SpacetimeAssemblyLineCasing.getStack(16),
                AssemblyPlantCasing.SpacetimeAssemblyLineUnit.getStack(16), GTNGItemList.MolecularCoil.get(32),
                ItemList.Emitter_UEV.get(32), ItemList.Sensor_UEV.get(32), ItemRefer.Precise_Electronic_Unit_T3.get(64),
                new Object[] { OrePrefixes.circuit.get(Materials.UIV), 64 }, ItemList.Energy_Cluster.get(1),
                Objects.requireNonNull(MaterialsAlloy.QUANTUM.getPlateDouble(32)))
            .fluidInputs(fluid(Materials.Glass.getMolten(3204)), fluid(Materials.RubberSilicone.getMolten(5760)),
                fluid(Materials.Platinum.getMolten(1296)), fluid(MaterialsAlloy.STELLITE.getFluidStack(5184)))
            .itemOutputs(GTNGItemList.NyarlathotepsTentacle.get(1)).duration(3200).eut(7864320).addTo(AssemblyLine);

        assemble(TentacleCasing.MolecularCasing, 50, 16, GTNGItemList.MolecularCasing.get(1),
            GTUtility.getIntegratedCircuit(24));
        assemble(TentacleCasing.SpacetimeAssemblyLineUnit, 50, 16, GTNGItemList.SpacetimeAssemblyLineUnit.get(1),
            GTUtility.getIntegratedCircuit(24));
        assemble(TentacleCasing.ContainmentFieldGenerator, 50, 16, GTNGItemList.ContainmentFieldGenerator.get(1),
            GTUtility.getIntegratedCircuit(24));
        assemble(TentacleCasing.HollowCasing, 50, 16, GTNGItemList.HollowCasing.get(1),
            GTUtility.getIntegratedCircuit(24));
        assemble(TentacleCasing.RestraintDevice, 50, 16, GTNGItemList.RestraintDevice.get(1),
            GTUtility.getIntegratedCircuit(24));
        assemble(TentacleCasing.NaquadahAlloyCasing, 50, 16, stack(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1),
            Materials.NaquadahAlloy.getPlates(6), GTUtility.getIntegratedCircuit(6));
        assemble(TentacleCasing.LaserCasing, 50, 16, stack(OrePrefixes.frameGt, Materials.Iridium, 1),
            stack(OrePrefixes.plateDouble, Materials.Osmiridium, 2),
            stack(OrePrefixes.plateDouble, Materials.Osmium, 4), GTUtility.getIntegratedCircuit(6));
        assemble(TentacleCasing.DegenerateRheniumConstrainedCasing, 1200, 491520,
            Objects.requireNonNull(MaterialsAlloy.QUANTUM.getFrameBox(1)),
            Objects.requireNonNull(MaterialsElements.getInstance().RHENIUM.getPlate(6)),
            GTUtility.getIntegratedCircuit(6));
        assemble(TentacleCasing.LaserCoolingCasing, 200, 1966080, Materials.SolderingAlloy.getMolten(576),
            stack(OrePrefixes.frameGt, Materials.HSSE, 1), ItemList.Coolant_Duct_Casing.get(1),
            ItemList.UHV_Coil.get(2), Materials.TungstenCarbide.getPlates(6),
            stack(OrePrefixes.plateDouble, Materials.ElectrumFlux, 2),
            Objects.requireNonNull(MaterialsAlloy.ZERON_100.getPlateDouble(4)));
        assemble(TentacleCasing.UruiumCoil, 600, 31457280, Materials.Molybdenum.getMolten(576),
            stack(OrePrefixes.wireGt02, Materials.SuperconductorUIVBase, 64),
            stack(OrePrefixes.wireGt02, Materials.SuperconductorUIVBase, 64),
            stack(OrePrefixes.ring, Materials.Neutronium, 16), stack(OrePrefixes.bolt, Materials.Neutronium, 32),
            stack(OrePrefixes.foil, Materials.BlackSteel, 64), stack(OrePrefixes.foil, Materials.Cupronickel, 64),
            stack(OrePrefixes.foil, Materials.Neutronium, 64), stack(OrePrefixes.foil, Materials.HSSG, 64),
            stack(OrePrefixes.foil, Materials.StyreneButadieneRubber, 64));

        GTRecipeBuilder.builder()
            .itemInputs(AssemblyPlantCasing.AdvancedAssemblyLineUnit.getStack(1), ItemList.Emitter_UHV.get(1),
                ItemList.Electric_Motor_UEV.get(4), ItemList.Conveyor_Module_UEV.get(2),
                ItemList.Electric_Piston_UEV.get(2), new Object[] { OrePrefixes.circuit.get(Materials.UEV), 2 },
                stack(OrePrefixes.plateDouble, Materials.Zinc, 8))
            .fluidInputs(fluid(MaterialsElements.getInstance().RHENIUM.getFluidStack(200)))
            .itemOutputs(TentacleCasing.SpacetimeAssemblyLineCasing.getStack(1)).duration(400).eut(7864320)
            .metadata(CLEANROOM, true).addTo(RecipeMaps.assemblerRecipes);
        assemble(TentacleCasing.SterilizingFilterCasing, 200, TierEU.RECIPE_UV,
            Materials.BioMediumSterilized.getFluid(576), Casings.FilterMachineCasing.toStack(1),
            ItemList.Circuit_Chip_Biocell.get(4), GTUtility.getIntegratedCircuit(24));
        assemble(TentacleCasing.ElectronPermeableGlass, 400, TierEU.RECIPE_UHV, Materials.Neutronium.getMolten(144),
            GTNGItemList.FusionGlass.get(1), stack(OrePrefixes.foil, Materials.Neutronium, 8),
            GTUtility.getIntegratedCircuit(24));
        assemble(TentacleCasing.QuarkPipe, 400, TierEU.RECIPE_UEV, Materials.SpaceTime.getMolten(144),
            stack(OrePrefixes.frameGt, Materials.Trinium, 1), stack(OrePrefixes.pipeMedium, Materials.Neutronium, 4),
            GTNGItemList.DimensionallyTranscendentCasing.get(1), GTUtility.getIntegratedCircuit(24));
    }

    private static void assemble(TentacleCasing output, int duration, long eut, ItemStack... inputs) {
        GTRecipeBuilder.builder().itemInputs(inputs).itemOutputs(output.getStack(1)).duration(duration).eut(eut)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static void assemble(TentacleCasing output, int duration, long eut, FluidStack fluid, ItemStack... inputs) {
        GTRecipeBuilder.builder().itemInputs(inputs).fluidInputs(fluid(fluid)).itemOutputs(output.getStack(1))
            .duration(duration).eut(eut).addTo(RecipeMaps.assemblerRecipes);
    }

    private static ItemStack stack(OrePrefixes prefix, Materials material, long amount) {
        return Objects.requireNonNull(GTOreDictUnificator.get(prefix, material, amount),
            "Missing tentacle ingredient: " + prefix + material);
    }

    private static FluidStack fluid(FluidStack stack) {
        return Objects.requireNonNull(stack, "Missing tentacle fluid");
    }
}
