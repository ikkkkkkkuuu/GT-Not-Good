// SPDX-License-Identifier: CC-BY-NC-SA-4.0
package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import bartworks.system.material.WerkstoffLoader;
import goodgenerator.items.GGMaterial;
import goodgenerator.util.ItemRefer;
import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.core.material.MaterialsElements;

public final class MageAssemblerRecipes {

    private MageAssemblerRecipes() {}

    public static void loadRecipes() {
        GTModHandler.addCraftingRecipe(AssemblyPlantCasing.AdvancedAssemblyLineUnit.getStack(1),
            new Object[] { "CEC", "BAB", "CEC", 'A', Casings.AssemblyLineCasing.toStack(1), 'B',
                OrePrefixes.circuit.get(Materials.UV), 'C', Materials.HSSG.getPlates(1), 'E',
                Objects.requireNonNull(WerkstoffLoader.Rhodium.get(OrePrefixes.gearGt, 1), "Missing rhodium gear") });
    }

    public static void loadPostInitRecipes() {
        controller();
        spacetimeAssemblyLineUnit();
    }

    private static void controller() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, GTNGItemList.PrecisionAssembler.get(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UHV))
            .itemInputs(ItemList.AssemblingMachineUV.get(16), ItemList.Machine_UV_CircuitAssembler.get(16),
                ItemList.Robot_Arm_UV.get(16), ItemList.FluidRegulator_UV.get(32), ItemList.Robot_Arm_UV.get(64),
                new Object[] { OrePrefixes.circuit.get(Materials.UHV), 32 },
                stack(OrePrefixes.screw, Materials.TungstenCarbide, 64),
                stack(OrePrefixes.screw, Materials.YttriumBariumCuprate, 64),
                Objects.requireNonNull(WerkstoffLoader.RhodiumPlatedPalladium.get(OrePrefixes.screw, 64),
                    "Missing rhodium-plated palladium screws"),
                stack(OrePrefixes.screw, Materials.NaquadahAlloy, 64),
                Objects.requireNonNull(MaterialsAlloy.INCOLOY_MA956.getLongRod(32), "Missing Incoloy MA956 long rods"),
                stack(OrePrefixes.stickLong, Materials.TungstenCarbide, 32),
                stack(OrePrefixes.stickLong, Materials.TungstenSteel, 32),
                stack(OrePrefixes.stickLong, Materials.Ultimet, 32),
                Objects.requireNonNull(MaterialsAlloy.LAFIUM.getPlate(16), "Missing Lafium plates"),
                stack(OrePrefixes.plateDouble, Materials.Iridium, 8))
            .fluidInputs(fluid(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(5760)),
                fluid(Materials.Kanthal.getMolten(5760)),
                fluid(MaterialsElements.getInstance().FERMIUM.getFluidStack(2880)),
                fluid(GGMaterial.metastableOganesson.getMolten(2880)))
            .itemOutputs(GTNGItemList.MageAssembler.get(1)).duration(800).eut(TierEU.RECIPE_UHV).addTo(AssemblyLine);
    }

    private static void spacetimeAssemblyLineUnit() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, AssemblyPlantCasing.AdvancedAssemblyLineUnit.getStack(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UHV))
            .itemInputs(stack(OrePrefixes.frameGt, Materials.Adamantium, 1),
                AssemblyPlantCasing.AdvancedAssemblyLineUnit.getStack(1), ItemRefer.Precise_Electronic_Unit_T4.get(1),
                ItemList.Robot_Arm_UHV.get(1), Materials.Neutronium.getNanite(1), ItemList.Circuit_Chip_Optical.get(6),
                new Object[] { OrePrefixes.circuit.get(Materials.UEV), 2 },
                stack(OrePrefixes.gearGt, Materials.BlueSteel, 4), stack(OrePrefixes.gearGt, Materials.RedSteel, 4),
                ItemList.Robot_Arm_ZPM.get(4),
                Objects.requireNonNull(MaterialsAlloy.PIKYONIUM.getPlateDouble(6), "Missing Pikyonium double plates"))
            .fluidInputs(fluid(Materials.LiquidCrystalKevlar.getFluid(5760)),
                fluid(MaterialsAlloy.ABYSSAL.getFluidStack(1296)),
                fluid(MaterialsAlloy.INCOLOY_MA956.getFluidStack(1296)),
                fluid(MaterialsAlloy.PIKYONIUM.getFluidStack(1296)))
            .itemOutputs(AssemblyPlantCasing.SpacetimeAssemblyLineUnit.getStack(1)).duration(400).eut(TierEU.RECIPE_UHV)
            .addTo(AssemblyLine);
    }

    private static ItemStack stack(OrePrefixes prefix, Materials material, long amount) {
        return Objects.requireNonNull(GTOreDictUnificator.get(prefix, material, amount),
            "Missing native assembly material: " + prefix + material);
    }

    private static FluidStack fluid(FluidStack stack) {
        return Objects.requireNonNull(stack, "Missing native assembly fluid");
    }
}
