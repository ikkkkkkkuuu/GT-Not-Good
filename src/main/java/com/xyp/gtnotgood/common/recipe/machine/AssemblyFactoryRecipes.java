package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.enums.TierEU.RECIPE_UHV;
import static gregtech.api.util.GTRecipeBuilder.HOURS;
import static gregtech.api.util.GTRecipeBuilder.INGOTS;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.STACKS;
import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import goodgenerator.items.GGMaterial;
import goodgenerator.loader.Loaders;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import tectech.recipe.TTRecipeAdder;

/**
 * Registers the Assembly Factory controller and both structural matrix tiers.
 */
public final class AssemblyFactoryRecipes {

    private AssemblyFactoryRecipes() {}

    /** Adds all recipes required to build and upgrade the Assembly Factory. */
    public static void loadRecipes() {
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, new ItemStack(Loaders.CompAssline.getItem(), 1, 32026))
            .metadata(SCANNING, new Scanning(6 * HOURS, RECIPE_UHV))
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.nanite, Materials.Neutronium, 8),
                new ItemStack(Casings.AssemblerMachineCasing.getItem(), 64, 9),
                new ItemStack(Casings.AssemblyLineCasing.getItem(), 64, 5),
                ItemList.Robot_Arm_UHV.get(64),
                ItemList.Conveyor_Module_UHV.get(64),
                ItemList.Electric_Pump_UHV.get(64),
                ItemList.Sensor_UHV.get(64),
                ItemList.Emitter_UHV.get(64),
                ItemList.Field_Generator_UHV.get(4),
                GTOreDictUnificator.get(OrePrefixes.pipeMedium, Materials.Infinity, 8),
                GTOreDictUnificator.get(OrePrefixes.gearGt, Materials.Infinity, 8),
                GTOreDictUnificator.get(OrePrefixes.screw, Materials.Infinity, 8),
                new Object[] { OrePrefixes.circuit.get(Materials.UEV), 16 },
                new Object[] { OrePrefixes.circuit.get(Materials.UHV), 32 },
                new Object[] { OrePrefixes.circuit.get(Materials.UV), 64 },
                GregtechItemList.Laser_Lens_Special.get(1))
            .fluidInputs(
                MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(STACKS * INGOTS),
                MaterialsAlloy.INDALLOY_140.getFluidStack(144 * 1024),
                MaterialsAlloy.PIKYONIUM.getFluidStack(144 * 512))
            .itemOutputs(GTNGItemList.AssemblyFactory.get(1))
            .eut(RECIPE_UHV)
            .duration(20 * 600)
            .addTo(AssemblyLine);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Casing_SolidSteel.get(1),
                ItemList.Electric_Motor_UHV.get(16),
                ItemList.Electric_Piston_UHV.get(8),
                ItemList.Conveyor_Module_UHV.get(8),
                ItemList.Robot_Arm_UHV.get(4),
                ItemList.Electric_Pump_UHV.get(4),
                ItemList.Field_Generator_UV.get(8),
                GTOreDictUnificator.get(OrePrefixes.gearGt, GGMaterial.marCeM200.getGTMaterial(), 64),
                new Object[] { OrePrefixes.circuit.get(Materials.UHV), 8 })
            .fluidInputs(MaterialsAlloy.INDALLOY_140.getFluidStack(STACKS))
            .itemOutputs(GTNGItemList.AssemblyMatrixBlock.get(1))
            .eut(RECIPE_UHV)
            .duration(MINUTES * 4)
            .addTo(RecipeMaps.assemblerRecipes);

        TTRecipeAdder.addResearchableAssemblylineRecipe(
            GTNGItemList.AssemblyMatrixBlock.get(1),
            16_777_216 * 2,
            16384,
            (int) TierEU.RECIPE_UMV,
            1,
            new Object[] { GTNGItemList.AssemblyMatrixBlock.get(1), ItemList.Electric_Motor_UMV.get(4),
                ItemList.Electric_Piston_UMV.get(4), ItemList.Robot_Arm_UMV.get(4), ItemList.Electric_Pump_UMV.get(4),
                ItemList.Conveyor_Module_UMV.get(4), ItemList.Field_Generator_UMV.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.UMV), 4 },
                GTOreDictUnificator.get(OrePrefixes.spring, Materials.SpaceTime, 4),
                GTOreDictUnificator.get(OrePrefixes.gearGt, Materials.SpaceTime, 2),
                GTOreDictUnificator.get(OrePrefixes.gearGtSmall, Materials.SpaceTime, 1) },
            new FluidStack[] { MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(STACKS * INGOTS),
                MaterialsAlloy.INDALLOY_140.getFluidStack(144 * 1024),
                MaterialsAlloy.PIKYONIUM.getFluidStack(144 * 512) },
            GTNGItemList.AdvancedAssemblyMatrixBlock.get(1),
            20 * 600,
            (int) TierEU.RECIPE_UMV);
    }
}
