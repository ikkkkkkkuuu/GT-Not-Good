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
        { GTNGItemList.WirelessLaserEnergyLV_256, GTNGItemList.WirelessLaserEnergyLV_1024,
            GTNGItemList.WirelessLaserEnergyLV_4096, GTNGItemList.WirelessLaserEnergyLV_16384,
            GTNGItemList.WirelessLaserEnergyLV_65536, GTNGItemList.WirelessLaserEnergyLV_262144,
            GTNGItemList.WirelessLaserEnergyLV_1048576 },
        { GTNGItemList.WirelessLaserEnergyMV_256, GTNGItemList.WirelessLaserEnergyMV_1024,
            GTNGItemList.WirelessLaserEnergyMV_4096, GTNGItemList.WirelessLaserEnergyMV_16384,
            GTNGItemList.WirelessLaserEnergyMV_65536, GTNGItemList.WirelessLaserEnergyMV_262144,
            GTNGItemList.WirelessLaserEnergyMV_1048576 },
        { GTNGItemList.WirelessLaserEnergyHV_256, GTNGItemList.WirelessLaserEnergyHV_1024,
            GTNGItemList.WirelessLaserEnergyHV_4096, GTNGItemList.WirelessLaserEnergyHV_16384,
            GTNGItemList.WirelessLaserEnergyHV_65536, GTNGItemList.WirelessLaserEnergyHV_262144,
            GTNGItemList.WirelessLaserEnergyHV_1048576 },
        { GTNGItemList.WirelessLaserEnergyEV_256, GTNGItemList.WirelessLaserEnergyEV_1024,
            GTNGItemList.WirelessLaserEnergyEV_4096, GTNGItemList.WirelessLaserEnergyEV_16384,
            GTNGItemList.WirelessLaserEnergyEV_65536, GTNGItemList.WirelessLaserEnergyEV_262144,
            GTNGItemList.WirelessLaserEnergyEV_1048576 },
        { GTNGItemList.WirelessLaserEnergyIV_256, GTNGItemList.WirelessLaserEnergyIV_1024,
            GTNGItemList.WirelessLaserEnergyIV_4096, GTNGItemList.WirelessLaserEnergyIV_16384,
            GTNGItemList.WirelessLaserEnergyIV_65536, GTNGItemList.WirelessLaserEnergyIV_262144,
            GTNGItemList.WirelessLaserEnergyIV_1048576 },
        { GTNGItemList.WirelessLaserEnergyLuV_256, GTNGItemList.WirelessLaserEnergyLuV_1024,
            GTNGItemList.WirelessLaserEnergyLuV_4096, GTNGItemList.WirelessLaserEnergyLuV_16384,
            GTNGItemList.WirelessLaserEnergyLuV_65536, GTNGItemList.WirelessLaserEnergyLuV_262144,
            GTNGItemList.WirelessLaserEnergyLuV_1048576 },
        { GTNGItemList.WirelessLaserEnergyZPM_256, GTNGItemList.WirelessLaserEnergyZPM_1024,
            GTNGItemList.WirelessLaserEnergyZPM_4096, GTNGItemList.WirelessLaserEnergyZPM_16384,
            GTNGItemList.WirelessLaserEnergyZPM_65536, GTNGItemList.WirelessLaserEnergyZPM_262144,
            GTNGItemList.WirelessLaserEnergyZPM_1048576 },
        { GTNGItemList.WirelessLaserEnergyUV_256, GTNGItemList.WirelessLaserEnergyUV_1024,
            GTNGItemList.WirelessLaserEnergyUV_4096, GTNGItemList.WirelessLaserEnergyUV_16384,
            GTNGItemList.WirelessLaserEnergyUV_65536, GTNGItemList.WirelessLaserEnergyUV_262144,
            GTNGItemList.WirelessLaserEnergyUV_1048576 },
        { GTNGItemList.WirelessLaserEnergyUHV_256, GTNGItemList.WirelessLaserEnergyUHV_1024,
            GTNGItemList.WirelessLaserEnergyUHV_4096, GTNGItemList.WirelessLaserEnergyUHV_16384,
            GTNGItemList.WirelessLaserEnergyUHV_65536, GTNGItemList.WirelessLaserEnergyUHV_262144,
            GTNGItemList.WirelessLaserEnergyUHV_1048576 },
        { GTNGItemList.WirelessLaserEnergyUEV_256, GTNGItemList.WirelessLaserEnergyUEV_1024,
            GTNGItemList.WirelessLaserEnergyUEV_4096, GTNGItemList.WirelessLaserEnergyUEV_16384,
            GTNGItemList.WirelessLaserEnergyUEV_65536, GTNGItemList.WirelessLaserEnergyUEV_262144,
            GTNGItemList.WirelessLaserEnergyUEV_1048576 },
        { GTNGItemList.WirelessLaserEnergyUIV_256, GTNGItemList.WirelessLaserEnergyUIV_1024,
            GTNGItemList.WirelessLaserEnergyUIV_4096, GTNGItemList.WirelessLaserEnergyUIV_16384,
            GTNGItemList.WirelessLaserEnergyUIV_65536, GTNGItemList.WirelessLaserEnergyUIV_262144,
            GTNGItemList.WirelessLaserEnergyUIV_1048576 },
        { GTNGItemList.WirelessLaserEnergyUMV_256, GTNGItemList.WirelessLaserEnergyUMV_1024,
            GTNGItemList.WirelessLaserEnergyUMV_4096, GTNGItemList.WirelessLaserEnergyUMV_16384,
            GTNGItemList.WirelessLaserEnergyUMV_65536, GTNGItemList.WirelessLaserEnergyUMV_262144,
            GTNGItemList.WirelessLaserEnergyUMV_1048576 },
        { GTNGItemList.WirelessLaserEnergyUXV_256, GTNGItemList.WirelessLaserEnergyUXV_1024,
            GTNGItemList.WirelessLaserEnergyUXV_4096, GTNGItemList.WirelessLaserEnergyUXV_16384,
            GTNGItemList.WirelessLaserEnergyUXV_65536, GTNGItemList.WirelessLaserEnergyUXV_262144,
            GTNGItemList.WirelessLaserEnergyUXV_1048576 },
        { GTNGItemList.WirelessLaserEnergyMAX_256, GTNGItemList.WirelessLaserEnergyMAX_1024,
            GTNGItemList.WirelessLaserEnergyMAX_4096, GTNGItemList.WirelessLaserEnergyMAX_16384,
            GTNGItemList.WirelessLaserEnergyMAX_65536, GTNGItemList.WirelessLaserEnergyMAX_262144,
            GTNGItemList.WirelessLaserEnergyMAX_1048576 } };

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
        return GTNGMachineID.WIRELESS_LASER.ID + (tier - VoltageIndex.LV) * 8 + slot;
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
                energy(tier, variant).set(
                    new WirelessLaserEnergyHatch(
                        machineId(tier, variant),
                        ModList.GTNotGood.getID() + ".wireless.laser.input." + tier + "." + amperes(variant),
                        tier,
                        amperes(variant)));
                addItemTooltip(energy(tier, variant).get(1), AnimatedText.GT_NOT_GOOD);
            }
            if (tier != VoltageIndex.UMV) {
                dynamo(tier).set(
                    new WirelessLaserDynamoHatch(
                        machineId(tier, INPUT_VARIANTS),
                        ModList.GTNotGood.getID() + ".wireless.laser.output." + tier,
                        tier,
                        DYNAMO_AMPERES));
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
