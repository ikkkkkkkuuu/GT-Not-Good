package com.xyp.gtnotgood.common.recipe.machine;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.machines.hatch.WirelessLaserDynamoHatch;
import com.xyp.gtnotgood.common.machines.hatch.WirelessLaserEnergyHatch;
import com.xyp.gtnotgood.config.Config;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEWirelessEnergy;
import gregtech.api.recipe.RecipeMaps;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessDynamoMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

/**
 * Adds optional assembler alternatives for every registered native wireless energy hatch and this mod's lasers.
 * Runs after all init handlers, when TecTech's item containers are populated. No original recipe is removed.
 * A regular same-tier hatch keeps LV-EV recipes usable without nonexistent wired laser hatches.
 */
public final class EasyWirelessRecipes {

    private EasyWirelessRecipes() {}

    /**
     * Distinct non-consumed circuit settings distinguish outputs sharing the same basic hatch and components.
     *
     * @param amperes native power-of-four rating, or 2 A for a regular wireless input
     * @return circuit 19-21 for multi-amp, 22 for regular, or 1-9 for laser
     */
    public static int circuitSetting(int amperes) {
        if (amperes == 2) return 22;
        if (amperes == 4) return 19;
        if (amperes == 16) return 20;
        if (amperes == 64) return 21;
        for (int variant = 0; variant < 9; variant++) {
            if (amperes == (256 << (2 * variant))) return 1 + variant;
        }
        throw new IllegalArgumentException("Unsupported wireless amperage: " + amperes);
    }

    /** Only native classes and our two adapters are included; other addons retain control of their own recipes. */
    public static boolean supports(IMetaTileEntity machine) {
        if (machine == null) return false;
        Class<?> type = machine.getClass();
        return type == MTEWirelessEnergy.class || type == MTEHatchWirelessMulti.class
            || type == MTEHatchWirelessDynamoMulti.class
            || type == WirelessLaserEnergyHatch.class
            || type == WirelessLaserDynamoHatch.class;
    }

    public static void loadRecipes() {
        if (!Config.enableEasyWirelessRecipes) return;
        int count = 0;
        for (IMetaTileEntity machine : GregTechAPI.METATILEENTITIES) {
            if (!supports(machine)) continue;
            MTEHatch hatch = (MTEHatch) machine;
            boolean dynamo = hatch instanceof MTEHatchWirelessDynamoMulti;
            int amps = dynamo ? ((MTEHatchWirelessDynamoMulti) hatch).maxAmperes
                : hatch instanceof MTEHatchWirelessMulti ? ((MTEHatchWirelessMulti) hatch).maxAmperes : 2;
            addRecipe(hatch, dynamo, amps);
            count++;
        }
        GTNotGood.LOG.info("Registered {} easy wireless energy recipes", count);
    }

    private static void addRecipe(MTEHatch hatch, boolean dynamo, int amperes) {
        int tier = hatch.mTier;
        // ULV has no emitter/sensor; use LV parts. MAX has no regular energy hatch; use a UXV hatch.
        int componentTier = Math.max(VoltageIndex.LV, tier);
        int baseTier = Math.min(VoltageIndex.UXV, tier);
        ItemStack base = (dynamo ? ItemList.HATCHES_DYNAMO : ItemList.HATCHES_ENERGY)[baseTier].get(1);
        boolean laser = amperes > 64;
        int circuits = laser ? 3 : amperes > 2 ? 2 : 1;
        int parts = laser ? 2 : 1;
        int solderIngots = laser ? 16 : amperes == 64 ? 8 : amperes == 16 ? 4 : amperes == 4 ? 2 : 1;
        Materials[] circuitMaterials = { Materials.ULV, Materials.LV, Materials.MV, Materials.HV, Materials.EV,
            Materials.IV, Materials.LuV, Materials.ZPM, Materials.UV, Materials.UHV, Materials.UEV, Materials.UIV,
            Materials.UMV, Materials.UXV, Materials.MAX };
        GTValues.RA.stdBuilder()
            .itemInputs(
                base,
                new Object[] { OrePrefixes.circuit.get(circuitMaterials[tier]), circuits },
                ItemList.valueOf("Emitter_" + GTValues.VN[componentTier])
                    .get(parts),
                ItemList.valueOf("Sensor_" + GTValues.VN[componentTier])
                    .get(parts))
            .circuit(dynamo ? 22 : circuitSetting(amperes))
            .fluidInputs(Materials.SolderingAlloy.getMolten(144L * solderIngots))
            .itemOutputs(hatch.getStackForm(1))
            .duration(80)
            .eut(GTValues.VP[tier])
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
