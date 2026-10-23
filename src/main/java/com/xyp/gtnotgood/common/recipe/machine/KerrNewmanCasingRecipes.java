// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.recipe.machine;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.blocks.kerrnewman.KerrNewmanCasing;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import bartworks.system.material.WerkstoffLoader;
import cpw.mods.fml.common.registry.GameRegistry;
import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.core.material.MaterialsElements;

public final class KerrNewmanCasingRecipes {

    private KerrNewmanCasingRecipes() {}

    public static void loadPostInitRecipes() {
        GameRegistry.addRecipe(KerrNewmanCasing.AmprosiumPipeCasing.getStack(1), "ABA", "BCB", "ABA", 'A',
            Materials.Neutronium.getPlates(1), 'B', stack(OrePrefixes.pipeMedium, Materials.Neutronium, 1), 'C',
            stack(OrePrefixes.frameGt, Materials.Neutronium, 1));
        assemble(KerrNewmanCasing.EnhancedHyperCasing, 50, 16, Materials.Naquadria.getMolten(576),
            stack(OrePrefixes.frameGt, Materials.Naquadria, 1), Materials.NaquadahAlloy.getPlates(4),
            Materials.Naquadah.getPlates(4), Materials.NaquadahEnriched.getPlates(4),
            GTUtility.getIntegratedCircuit(6));
        assemble(KerrNewmanCasing.ZirconiaCeramicCasing, 200, 30,
            stack(OrePrefixes.frameGt, Materials.TungstenSteel, 1), Materials.RedSteel.getPlates(2),
            WerkstoffLoader.CubicZirconia.get(OrePrefixes.dust, 4));
        assemble(KerrNewmanCasing.AmprosiumGearbox, 50, 16, Materials.Neutronium.getPlates(4),
            stack(OrePrefixes.gearGt, Materials.Neutronium, 2), stack(OrePrefixes.frameGt, Materials.Neutronium, 1),
            GTUtility.getIntegratedCircuit(4));
        assemble(KerrNewmanCasing.OilGasPipeCasing, 200, 480, MaterialsAlloy.MARAGING350.getFluidStack(576),
            stack(OrePrefixes.frameGt, Materials.HSSG, 1), stack(OrePrefixes.pipeMedium, Materials.NiobiumTitanium, 4),
            WerkstoffLoader.RhodiumPlatedPalladium.get(OrePrefixes.plate, 4), MaterialsAlloy.HASTELLOY_N.getPlate(4));
        assemble(KerrNewmanCasing.HighPressurePipeCasing, 200, 480, Materials.Nitrogen.getGas(576),
            stack(OrePrefixes.frameGt, Materials.Titanium, 1), stack(OrePrefixes.pipeMedium, Materials.Titanium, 4),
            Materials.TungstenSteel.getPlates(4));
        assemble(KerrNewmanCasing.GravityStabilizationCasing, 400, 480, Materials.Neutronium.getMolten(576),
            stack(OrePrefixes.frameGt, Materials.Neutronium, 1), ItemList.Gravistar.get(1),
            ItemList.Neutron_Reflector.get(2), MaterialsAlloy.TRINIUM_NAQUADAH_CARBON.getPlate(10));
        assemble(KerrNewmanCasing.AmprosiumActiveCasing, 200, 480, Materials.Neutronium.getMolten(576),
            stack(OrePrefixes.frameGt, Materials.Neutronium, 1), stack(OrePrefixes.pipeMedium, Materials.Europium, 4),
            MaterialsAlloy.LAFIUM.getPlate(8), Materials.Neutronium.getPlates(8));
        assemble(KerrNewmanCasing.HighStrengthSupportSpindle, 200, 480, Materials.Concrete.getMolten(2304),
            stack(OrePrefixes.frameGt, Materials.BlackSteel, 1),
            WerkstoffLoader.RhodiumPlatedPalladium.get(OrePrefixes.plate, 16),
            stack(OrePrefixes.stickLong, Materials.Naquadah, 8));
        assemble(KerrNewmanCasing.ExtremeDensityCasing, 200, 120, Materials.Naquadria.getMolten(576),
            MaterialsAlloy.TRINIUM_NAQUADAH_CARBON.getFrameBox(1),
            stack(OrePrefixes.plateDense, Materials.NaquadahAlloy, 1),
            MaterialsAlloy.TRINIUM_NAQUADAH_CARBON.getPlate(6));
        assemble(KerrNewmanCasing.PpsCorrosionResistantCasing, 50, 16,
            stack(OrePrefixes.frameGt, Materials.Polytetrafluoroethylene, 1),
            Materials.PolyphenyleneSulfide.getPlates(6), GTUtility.getIntegratedCircuit(6));
        assemble(KerrNewmanCasing.PlasmaHeaterCasing, 200, 120, Materials.Osmiridium.getMolten(288),
            stack(OrePrefixes.frameGt, Materials.Trinium, 1), ItemList.LuV_Coil.get(2),
            MaterialsElements.getInstance().RHENIUM.getPlate(2), stack(OrePrefixes.screw, Materials.Tungsten, 8),
            Materials.Trinium.getPlates(4));
        assemble(KerrNewmanCasing.RadiationAbsorbentCasing, 200, 120, Materials.Ultimet.getMolten(288),
            stack(OrePrefixes.frameGt, Materials.Uranium, 1), MaterialsAlloy.BABBIT_ALLOY.getPlate(4),
            WerkstoffLoader.RhodiumPlatedPalladium.get(OrePrefixes.plate, 8));
        assemble(KerrNewmanCasing.AtomicCasing, 50, 16, stack(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1),
            MaterialsAlloy.ABYSSAL.getPlate(2), Materials.NaquadahEnriched.getPlates(4),
            GTUtility.getIntegratedCircuit(6));
        assemble(KerrNewmanCasing.AcceleratorCoilUhv, 200, 960, Materials.NiobiumTitanium.getMolten(13824),
            stack(OrePrefixes.frameGt, Materials.NaquadahEnriched, 1), GTNGItemList.ImprovedSuperconductorCoil.get(1),
            stack(OrePrefixes.wireGt16, Materials.SuperconductorUHVBase, 8), ItemList.LuV_Coil.get(4));

        GTRecipeBuilder.builder().itemInputs(GTNGItemList.TemperedGlass.get(4))
            .fluidInputs(Objects.requireNonNull(Materials.Silicon.getMolten(144), "Missing molten silicon"))
            .itemOutputs(KerrNewmanCasing.IndustrialFramelessGlass.getStack(4)).duration(200).eut(120)
            .addTo(RecipeMaps.arcFurnaceRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(Casings.SlickSterileFlocculationCasing.toStack(1),
                WerkstoffLoader.CubicZirconia.get(OrePrefixes.dust, 2), ItemList.Circuit_Chip_Biocell.get(64),
                ItemList.FluidRegulator_UHV.get(2))
            .fluidInputs(Objects.requireNonNull(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(576)),
                Objects.requireNonNull(Materials.BioMediumSterilized.getFluid(576)),
                Objects.requireNonNull(Materials.Tritanium.getMolten(288)),
                Objects.requireNonNull(Materials.Glass.getMolten(576)))
            .itemOutputs(KerrNewmanCasing.BioactiveCasing.getStack(1)).duration(200).eut(TierEU.RECIPE_UV)
            .specialValue(0).addTo(GoodGeneratorRecipeMaps.preciseAssemblerRecipes);

    }

    private static void assemble(KerrNewmanCasing output, int duration, int eut, ItemStack... inputs) {
        recipe(output, duration, eut, inputs).addTo(RecipeMaps.assemblerRecipes);
    }

    private static void assemble(KerrNewmanCasing output, int duration, int eut, FluidStack fluid,
        ItemStack... inputs) {
        recipe(output, duration, eut, inputs).fluidInputs(Objects.requireNonNull(fluid, "Missing fluid for " + output))
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static GTRecipeBuilder recipe(KerrNewmanCasing output, int duration, int eut, ItemStack... inputs) {
        for (int index = 0; index < inputs.length; index++)
            Objects.requireNonNull(inputs[index], "Missing ingredient " + index + " for " + output);
        return GTRecipeBuilder.builder().itemInputs(inputs).itemOutputs(output.getStack(1)).duration(duration).eut(eut);
    }

    private static ItemStack stack(OrePrefixes prefix, Materials material, long amount) {
        return Objects.requireNonNull(GTOreDictUnificator.get(prefix, material, amount),
            "Missing Kerr-Newman casing ingredient: " + prefix + material);
    }
}
