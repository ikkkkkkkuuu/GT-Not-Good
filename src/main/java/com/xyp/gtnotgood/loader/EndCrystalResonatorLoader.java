package com.xyp.gtnotgood.loader;

import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.addItemTooltip;

import com.xyp.gtnotgood.common.machines.basic.EndCrystalResonator;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.text.AnimatedText;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;

public final class EndCrystalResonatorLoader {

    private static final GTNGItemList[] MACHINES = { GTNGItemList.EndCrystalResonatorLV,
        GTNGItemList.EndCrystalResonatorMV, GTNGItemList.EndCrystalResonatorHV, GTNGItemList.EndCrystalResonatorEV,
        GTNGItemList.EndCrystalResonatorIV, GTNGItemList.EndCrystalResonatorLuV, GTNGItemList.EndCrystalResonatorZPM,
        GTNGItemList.EndCrystalResonatorUV, GTNGItemList.EndCrystalResonatorUHV, GTNGItemList.EndCrystalResonatorUEV,
        GTNGItemList.EndCrystalResonatorUIV, GTNGItemList.EndCrystalResonatorUMV, GTNGItemList.EndCrystalResonatorUXV,
        GTNGItemList.EndCrystalResonatorMAX };

    private EndCrystalResonatorLoader() {}

    public static GTNGItemList machine(int tier) {
        return MACHINES[tier - 1];
    }

    public static void register() {
        for (int tier = 1; tier <= 14; tier++) {
            int id = GTNGMachineID.EndCrystalResonatorLV.id + tier - 1;
            if (GregTechAPI.METATILEENTITIES[id] != null)
                throw new IllegalStateException("End Crystal Resonator ID already occupied: " + id);
        }
        for (int tier = 1; tier <= 14; tier++) {
            machine(tier).set(new EndCrystalResonator(GTNGMachineID.EndCrystalResonatorLV.id + tier - 1,
                "EndCrystalResonator" + GTValues.VN[tier], tier));
            addItemTooltip(machine(tier).get(1), AnimatedText.GT_NOT_GOOD);
        }
    }
}
