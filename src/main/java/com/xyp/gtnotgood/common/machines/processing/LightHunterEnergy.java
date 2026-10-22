package com.xyp.gtnotgood.common.machines.processing;

import java.util.Collection;

import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.util.ExoticEnergyInputHelper;
import gregtech.api.util.GTUtility;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;

public final class LightHunterEnergy {

    private LightHunterEnergy() {}

    public static boolean isLaserHatch(MTEHatch hatch) {
        return hatch instanceof MTEHatchEnergyMulti multi && multi.getHatchType() == 2;
    }

    public static int countEnergyHatches(Collection<? extends MTEHatch> normal, Collection<? extends MTEHatch> exotic) {
        return normal.size() + exotic.size() - countLaserHatches(exotic);
    }

    public static int countLaserHatches(Collection<? extends MTEHatch> exotic) {
        int count = 0;
        for (MTEHatch hatch : exotic) if (isLaserHatch(hatch)) count++;
        return count;
    }

    public static long totalInputPower(Collection<? extends MTEHatch> normal, Collection<? extends MTEHatch> exotic) {
        return GTUtility.addSafe(ExoticEnergyInputHelper.getTotalEuMulti(normal),
            ExoticEnergyInputHelper.getTotalEuMulti(exotic));
    }
}
