package com.xyp.gtnotgood.loader;

import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.addItemTooltip;

import com.xyp.gtnotgood.common.machines.hatch.WirelessLaserDynamoHatch;
import com.xyp.gtnotgood.common.machines.hatch.WirelessMultiAmpEnergyHatch;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.AnimatedText;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.VoltageIndex;
import tectech.thing.CustomItemList;

/** Eight stable ID slots per tier; native UXV inputs and the UMV dynamo keep their existing IDs. */
public final class WirelessHatchLoader {

    public static final int INPUT_VARIANTS = 7;
    public static final int DYNAMO_AMPERES = 65_536;

    /**
     * Direct enum references indexed by tier minus LV, then by ascending amperage variant.
     * Keep row and column order stable: registration, recipes and saved machine IDs share this ordering.
     */
    private static final GTNGItemList[][] ENERGY_HATCHES = {
        { GTNGItemList.WirelessMultiAmpEnergyLV256A, GTNGItemList.WirelessMultiAmpEnergyLV1024A,
            GTNGItemList.WirelessMultiAmpEnergyLV4096A, GTNGItemList.WirelessMultiAmpEnergyLV16384A,
            GTNGItemList.WirelessMultiAmpEnergyLV65536A, GTNGItemList.WirelessMultiAmpEnergyLV262144A,
            GTNGItemList.WirelessMultiAmpEnergyLV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyMV256A, GTNGItemList.WirelessMultiAmpEnergyMV1024A,
            GTNGItemList.WirelessMultiAmpEnergyMV4096A, GTNGItemList.WirelessMultiAmpEnergyMV16384A,
            GTNGItemList.WirelessMultiAmpEnergyMV65536A, GTNGItemList.WirelessMultiAmpEnergyMV262144A,
            GTNGItemList.WirelessMultiAmpEnergyMV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyHV256A, GTNGItemList.WirelessMultiAmpEnergyHV1024A,
            GTNGItemList.WirelessMultiAmpEnergyHV4096A, GTNGItemList.WirelessMultiAmpEnergyHV16384A,
            GTNGItemList.WirelessMultiAmpEnergyHV65536A, GTNGItemList.WirelessMultiAmpEnergyHV262144A,
            GTNGItemList.WirelessMultiAmpEnergyHV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyEV256A, GTNGItemList.WirelessMultiAmpEnergyEV1024A,
            GTNGItemList.WirelessMultiAmpEnergyEV4096A, GTNGItemList.WirelessMultiAmpEnergyEV16384A,
            GTNGItemList.WirelessMultiAmpEnergyEV65536A, GTNGItemList.WirelessMultiAmpEnergyEV262144A,
            GTNGItemList.WirelessMultiAmpEnergyEV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyIV256A, GTNGItemList.WirelessMultiAmpEnergyIV1024A,
            GTNGItemList.WirelessMultiAmpEnergyIV4096A, GTNGItemList.WirelessMultiAmpEnergyIV16384A,
            GTNGItemList.WirelessMultiAmpEnergyIV65536A, GTNGItemList.WirelessMultiAmpEnergyIV262144A,
            GTNGItemList.WirelessMultiAmpEnergyIV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyLuV256A, GTNGItemList.WirelessMultiAmpEnergyLuV1024A,
            GTNGItemList.WirelessMultiAmpEnergyLuV4096A, GTNGItemList.WirelessMultiAmpEnergyLuV16384A,
            GTNGItemList.WirelessMultiAmpEnergyLuV65536A, GTNGItemList.WirelessMultiAmpEnergyLuV262144A,
            GTNGItemList.WirelessMultiAmpEnergyLuV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyZPM256A, GTNGItemList.WirelessMultiAmpEnergyZPM1024A,
            GTNGItemList.WirelessMultiAmpEnergyZPM4096A, GTNGItemList.WirelessMultiAmpEnergyZPM16384A,
            GTNGItemList.WirelessMultiAmpEnergyZPM65536A, GTNGItemList.WirelessMultiAmpEnergyZPM262144A,
            GTNGItemList.WirelessMultiAmpEnergyZPM1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyUV256A, GTNGItemList.WirelessMultiAmpEnergyUV1024A,
            GTNGItemList.WirelessMultiAmpEnergyUV4096A, GTNGItemList.WirelessMultiAmpEnergyUV16384A,
            GTNGItemList.WirelessMultiAmpEnergyUV65536A, GTNGItemList.WirelessMultiAmpEnergyUV262144A,
            GTNGItemList.WirelessMultiAmpEnergyUV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyUHV256A, GTNGItemList.WirelessMultiAmpEnergyUHV1024A,
            GTNGItemList.WirelessMultiAmpEnergyUHV4096A, GTNGItemList.WirelessMultiAmpEnergyUHV16384A,
            GTNGItemList.WirelessMultiAmpEnergyUHV65536A, GTNGItemList.WirelessMultiAmpEnergyUHV262144A,
            GTNGItemList.WirelessMultiAmpEnergyUHV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyUEV256A, GTNGItemList.WirelessMultiAmpEnergyUEV1024A,
            GTNGItemList.WirelessMultiAmpEnergyUEV4096A, GTNGItemList.WirelessMultiAmpEnergyUEV16384A,
            GTNGItemList.WirelessMultiAmpEnergyUEV65536A, GTNGItemList.WirelessMultiAmpEnergyUEV262144A,
            GTNGItemList.WirelessMultiAmpEnergyUEV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyUIV256A, GTNGItemList.WirelessMultiAmpEnergyUIV1024A,
            GTNGItemList.WirelessMultiAmpEnergyUIV4096A, GTNGItemList.WirelessMultiAmpEnergyUIV16384A,
            GTNGItemList.WirelessMultiAmpEnergyUIV65536A, GTNGItemList.WirelessMultiAmpEnergyUIV262144A,
            GTNGItemList.WirelessMultiAmpEnergyUIV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyUMV256A, GTNGItemList.WirelessMultiAmpEnergyUMV1024A,
            GTNGItemList.WirelessMultiAmpEnergyUMV4096A, GTNGItemList.WirelessMultiAmpEnergyUMV16384A,
            GTNGItemList.WirelessMultiAmpEnergyUMV65536A, GTNGItemList.WirelessMultiAmpEnergyUMV262144A,
            GTNGItemList.WirelessMultiAmpEnergyUMV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyUXV256A, GTNGItemList.WirelessMultiAmpEnergyUXV1024A,
            GTNGItemList.WirelessMultiAmpEnergyUXV4096A, GTNGItemList.WirelessMultiAmpEnergyUXV16384A,
            GTNGItemList.WirelessMultiAmpEnergyUXV65536A, GTNGItemList.WirelessMultiAmpEnergyUXV262144A,
            GTNGItemList.WirelessMultiAmpEnergyUXV1048576A },
        { GTNGItemList.WirelessMultiAmpEnergyMAX256A, GTNGItemList.WirelessMultiAmpEnergyMAX1024A,
            GTNGItemList.WirelessMultiAmpEnergyMAX4096A, GTNGItemList.WirelessMultiAmpEnergyMAX16384A,
            GTNGItemList.WirelessMultiAmpEnergyMAX65536A, GTNGItemList.WirelessMultiAmpEnergyMAX262144A,
            GTNGItemList.WirelessMultiAmpEnergyMAX1048576A } };

    /** One 65,536 A dynamo per voltage tier, ordered LV through MAX. */
    private static final GTNGItemList[] DYNAMO_HATCHES = { GTNGItemList.WirelessLaserDynamoLV,
        GTNGItemList.WirelessLaserDynamoMV, GTNGItemList.WirelessLaserDynamoHV, GTNGItemList.WirelessLaserDynamoEV,
        GTNGItemList.WirelessLaserDynamoIV, GTNGItemList.WirelessLaserDynamoLuV, GTNGItemList.WirelessLaserDynamoZPM,
        GTNGItemList.WirelessLaserDynamoUV, GTNGItemList.WirelessLaserDynamoUHV, GTNGItemList.WirelessLaserDynamoUEV,
        GTNGItemList.WirelessLaserDynamoUIV, GTNGItemList.WirelessLaserDynamoUMV, GTNGItemList.WirelessLaserDynamoUXV,
        GTNGItemList.WirelessLaserDynamoMAX };

    /** Native UXV inputs in the same ascending amperage order as the catalog columns. */
    private static final CustomItemList[] NATIVE_UXV_INPUTS = { CustomItemList.eM_energyWirelessTunnel1_UXV,
        CustomItemList.eM_energyWirelessTunnel2_UXV, CustomItemList.eM_energyWirelessTunnel3_UXV,
        CustomItemList.eM_energyWirelessTunnel4_UXV, CustomItemList.eM_energyWirelessTunnel5_UXV,
        CustomItemList.eM_energyWirelessTunnel6_UXV, CustomItemList.eM_energyWirelessTunnel7_UXV };

    private WirelessHatchLoader() {}

    public static int amperes(int variant) {
        if (variant < 0 || variant >= INPUT_VARIANTS) throw new IllegalArgumentException("Invalid wireless variant");
        return 256 << (2 * variant);
    }

    /** Stable saved-world ID; slot seven is the tier's dynamo and must never be reassigned. */
    public static int machineId(int tier, int slot) {
        checkTier(tier);
        if (slot < 0 || slot > INPUT_VARIANTS) throw new IllegalArgumentException("Invalid wireless slot");
        return GTNGMachineID.WirelessHatches.id + (tier - VoltageIndex.LV) * 8 + slot;
    }

    public static GTNGItemList energy(int tier, int variant) {
        checkTier(tier);
        if (variant < 0 || variant >= INPUT_VARIANTS) throw new IllegalArgumentException("Invalid wireless variant");
        return ENERGY_HATCHES[tier - VoltageIndex.LV][variant];
    }

    public static GTNGItemList dynamo(int tier) {
        checkTier(tier);
        return DYNAMO_HATCHES[tier - VoltageIndex.LV];
    }

    private static void checkTier(int tier) {
        if (tier < VoltageIndex.LV || tier > VoltageIndex.MAX)
            throw new IllegalArgumentException("Invalid wireless tier");
    }

    /**
     * Validates the entire owned range before registering any machine, so an ID conflict cannot partially overwrite it.
     */
    public static void register() {
        for (int tier = VoltageIndex.LV; tier <= VoltageIndex.MAX; tier++) {
            for (int slot = 0; slot <= INPUT_VARIANTS; slot++) {
                int id = machineId(tier, slot);
                if (GregTechAPI.METATILEENTITIES[id] != null) {
                    throw new IllegalStateException(
                        ModList.GTNotGood.getDisplayName() + " wireless hatch ID already occupied: " + id);
                }
            }
        }
        for (int tier = VoltageIndex.LV; tier <= VoltageIndex.MAX; tier++) {
            for (int variant = 0; variant < INPUT_VARIANTS; variant++) {
                if (tier == VoltageIndex.UXV) continue;
                energy(tier, variant).set(new WirelessMultiAmpEnergyHatch(machineId(tier, variant),
                    ModList.GTNotGood.getID() + ".wireless.laser.input." + tier + "." + amperes(variant), tier,
                    amperes(variant)));
                addItemTooltip(energy(tier, variant).get(1), AnimatedText.GT_NOT_GOOD);
            }
            if (tier != VoltageIndex.UMV) {
                dynamo(tier).set(new WirelessLaserDynamoHatch(machineId(tier, INPUT_VARIANTS),
                    ModList.GTNotGood.getID() + ".wireless.laser.output." + tier, tier, DYNAMO_AMPERES));
                addItemTooltip(dynamo(tier).get(1), AnimatedText.GT_NOT_GOOD);
            }
        }
    }

    /** Called during postInit, after TecTech has registered its native lasers and dynamo during init. */
    public static void bindNativeHatches() {
        dynamo(VoltageIndex.UMV).set(CustomItemList.eM_dynamoWirelessMulti.get(1));
        addItemTooltip(dynamo(VoltageIndex.UMV).get(1), AnimatedText.GT_NOT_GOOD);
        for (int variant = 0; variant < INPUT_VARIANTS; variant++) {
            energy(VoltageIndex.UXV, variant).set(NATIVE_UXV_INPUTS[variant].get(1));
            addItemTooltip(energy(VoltageIndex.UXV, variant).get(1), AnimatedText.GT_NOT_GOOD);
        }
    }
}
