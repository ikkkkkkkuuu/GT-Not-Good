package com.xyp.gtnotgood.loader;

import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.addItemTooltip;

import com.xyp.gtnotgood.common.machines.hatch.WirelessLaserDynamoHatch;
import com.xyp.gtnotgood.common.machines.hatch.WirelessLaserEnergyHatch;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.AnimatedText;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.VoltageIndex;
import tectech.thing.CustomItemList;

/**
 * Owns the stable LV-MAX laser catalog. Eight ID slots are reserved per tier, independent of recipe configuration.
 * Native UXV inputs and the UMV dynamo are reused, leaving their reserved slots unused.
 */
public final class WirelessLaserLoader {

    public static final int INPUT_VARIANTS = 7;
    public static final int DYNAMO_AMPERES = 65_536;

    /**
     * Direct enum references indexed by tier minus LV, then by ascending amperage variant.
     * Keep row and column order stable: registration, recipes and saved machine IDs share this ordering.
     */
    private static final GTNGItemList[][] ENERGY_HATCHES = {
        { GTNGItemList.WirelessLaserEnergyLV256A, GTNGItemList.WirelessLaserEnergyLV1024A,
            GTNGItemList.WirelessLaserEnergyLV4096A, GTNGItemList.WirelessLaserEnergyLV16384A,
            GTNGItemList.WirelessLaserEnergyLV65536A, GTNGItemList.WirelessLaserEnergyLV262144A,
            GTNGItemList.WirelessLaserEnergyLV1048576A },
        { GTNGItemList.WirelessLaserEnergyMV256A, GTNGItemList.WirelessLaserEnergyMV1024A,
            GTNGItemList.WirelessLaserEnergyMV4096A, GTNGItemList.WirelessLaserEnergyMV16384A,
            GTNGItemList.WirelessLaserEnergyMV65536A, GTNGItemList.WirelessLaserEnergyMV262144A,
            GTNGItemList.WirelessLaserEnergyMV1048576A },
        { GTNGItemList.WirelessLaserEnergyHV256A, GTNGItemList.WirelessLaserEnergyHV1024A,
            GTNGItemList.WirelessLaserEnergyHV4096A, GTNGItemList.WirelessLaserEnergyHV16384A,
            GTNGItemList.WirelessLaserEnergyHV65536A, GTNGItemList.WirelessLaserEnergyHV262144A,
            GTNGItemList.WirelessLaserEnergyHV1048576A },
        { GTNGItemList.WirelessLaserEnergyEV256A, GTNGItemList.WirelessLaserEnergyEV1024A,
            GTNGItemList.WirelessLaserEnergyEV4096A, GTNGItemList.WirelessLaserEnergyEV16384A,
            GTNGItemList.WirelessLaserEnergyEV65536A, GTNGItemList.WirelessLaserEnergyEV262144A,
            GTNGItemList.WirelessLaserEnergyEV1048576A },
        { GTNGItemList.WirelessLaserEnergyIV256A, GTNGItemList.WirelessLaserEnergyIV1024A,
            GTNGItemList.WirelessLaserEnergyIV4096A, GTNGItemList.WirelessLaserEnergyIV16384A,
            GTNGItemList.WirelessLaserEnergyIV65536A, GTNGItemList.WirelessLaserEnergyIV262144A,
            GTNGItemList.WirelessLaserEnergyIV1048576A },
        { GTNGItemList.WirelessLaserEnergyLuV256A, GTNGItemList.WirelessLaserEnergyLuV1024A,
            GTNGItemList.WirelessLaserEnergyLuV4096A, GTNGItemList.WirelessLaserEnergyLuV16384A,
            GTNGItemList.WirelessLaserEnergyLuV65536A, GTNGItemList.WirelessLaserEnergyLuV262144A,
            GTNGItemList.WirelessLaserEnergyLuV1048576A },
        { GTNGItemList.WirelessLaserEnergyZPM256A, GTNGItemList.WirelessLaserEnergyZPM1024A,
            GTNGItemList.WirelessLaserEnergyZPM4096A, GTNGItemList.WirelessLaserEnergyZPM16384A,
            GTNGItemList.WirelessLaserEnergyZPM65536A, GTNGItemList.WirelessLaserEnergyZPM262144A,
            GTNGItemList.WirelessLaserEnergyZPM1048576A },
        { GTNGItemList.WirelessLaserEnergyUV256A, GTNGItemList.WirelessLaserEnergyUV1024A,
            GTNGItemList.WirelessLaserEnergyUV4096A, GTNGItemList.WirelessLaserEnergyUV16384A,
            GTNGItemList.WirelessLaserEnergyUV65536A, GTNGItemList.WirelessLaserEnergyUV262144A,
            GTNGItemList.WirelessLaserEnergyUV1048576A },
        { GTNGItemList.WirelessLaserEnergyUHV256A, GTNGItemList.WirelessLaserEnergyUHV1024A,
            GTNGItemList.WirelessLaserEnergyUHV4096A, GTNGItemList.WirelessLaserEnergyUHV16384A,
            GTNGItemList.WirelessLaserEnergyUHV65536A, GTNGItemList.WirelessLaserEnergyUHV262144A,
            GTNGItemList.WirelessLaserEnergyUHV1048576A },
        { GTNGItemList.WirelessLaserEnergyUEV256A, GTNGItemList.WirelessLaserEnergyUEV1024A,
            GTNGItemList.WirelessLaserEnergyUEV4096A, GTNGItemList.WirelessLaserEnergyUEV16384A,
            GTNGItemList.WirelessLaserEnergyUEV65536A, GTNGItemList.WirelessLaserEnergyUEV262144A,
            GTNGItemList.WirelessLaserEnergyUEV1048576A },
        { GTNGItemList.WirelessLaserEnergyUIV256A, GTNGItemList.WirelessLaserEnergyUIV1024A,
            GTNGItemList.WirelessLaserEnergyUIV4096A, GTNGItemList.WirelessLaserEnergyUIV16384A,
            GTNGItemList.WirelessLaserEnergyUIV65536A, GTNGItemList.WirelessLaserEnergyUIV262144A,
            GTNGItemList.WirelessLaserEnergyUIV1048576A },
        { GTNGItemList.WirelessLaserEnergyUMV256A, GTNGItemList.WirelessLaserEnergyUMV1024A,
            GTNGItemList.WirelessLaserEnergyUMV4096A, GTNGItemList.WirelessLaserEnergyUMV16384A,
            GTNGItemList.WirelessLaserEnergyUMV65536A, GTNGItemList.WirelessLaserEnergyUMV262144A,
            GTNGItemList.WirelessLaserEnergyUMV1048576A },
        { GTNGItemList.WirelessLaserEnergyUXV256A, GTNGItemList.WirelessLaserEnergyUXV1024A,
            GTNGItemList.WirelessLaserEnergyUXV4096A, GTNGItemList.WirelessLaserEnergyUXV16384A,
            GTNGItemList.WirelessLaserEnergyUXV65536A, GTNGItemList.WirelessLaserEnergyUXV262144A,
            GTNGItemList.WirelessLaserEnergyUXV1048576A },
        { GTNGItemList.WirelessLaserEnergyMAX256A, GTNGItemList.WirelessLaserEnergyMAX1024A,
            GTNGItemList.WirelessLaserEnergyMAX4096A, GTNGItemList.WirelessLaserEnergyMAX16384A,
            GTNGItemList.WirelessLaserEnergyMAX65536A, GTNGItemList.WirelessLaserEnergyMAX262144A,
            GTNGItemList.WirelessLaserEnergyMAX1048576A } };

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

    private WirelessLaserLoader() {}

    /** Returns one of the seven power-of-four laser ratings, from 256 A to 1,048,576 A. */
    public static int amperes(int variant) {
        if (variant < 0 || variant >= INPUT_VARIANTS) throw new IllegalArgumentException("Invalid laser variant");
        return 256 << (2 * variant);
    }

    /** Stable saved-world ID; slot seven is the tier's dynamo and must never be reassigned. */
    public static int machineId(int tier, int slot) {
        checkTier(tier);
        if (slot < 0 || slot > INPUT_VARIANTS) throw new IllegalArgumentException("Invalid laser slot");
        return GTNGMachineID.WirelessLaser.id + (tier - VoltageIndex.LV) * 8 + slot;
    }

    public static GTNGItemList energy(int tier, int variant) {
        checkTier(tier);
        if (variant < 0 || variant >= INPUT_VARIANTS) throw new IllegalArgumentException("Invalid laser variant");
        return ENERGY_HATCHES[tier - VoltageIndex.LV][variant];
    }

    public static GTNGItemList dynamo(int tier) {
        checkTier(tier);
        return DYNAMO_HATCHES[tier - VoltageIndex.LV];
    }

    private static void checkTier(int tier) {
        if (tier < VoltageIndex.LV || tier > VoltageIndex.MAX) throw new IllegalArgumentException("Invalid laser tier");
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
                        ModList.GTNotGood.getDisplayName() + " wireless laser ID already occupied: " + id);
                }
            }
        }
        for (int tier = VoltageIndex.LV; tier <= VoltageIndex.MAX; tier++) {
            for (int variant = 0; variant < INPUT_VARIANTS; variant++) {
                if (tier == VoltageIndex.UXV) continue;
                energy(tier, variant).set(new WirelessLaserEnergyHatch(machineId(tier, variant),
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
