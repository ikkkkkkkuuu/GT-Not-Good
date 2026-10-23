package com.xyp.gtnotgood.loader;

import com.xyp.gtnotgood.common.machines.hatch.parallel.ParallelControlHatch;
import com.xyp.gtnotgood.common.machines.multiblock.parallelmachine.LargeFurnace;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;

public final class ParallelMachineLoader {

    public static final GTNGItemList[] HATCHES = { GTNGItemList.ParallelControlHatchIV,
        GTNGItemList.ParallelControlHatchLuV, GTNGItemList.ParallelControlHatchZPM, GTNGItemList.ParallelControlHatchUV,
        GTNGItemList.ParallelControlHatchUHV, GTNGItemList.ParallelControlHatchUEV,
        GTNGItemList.ParallelControlHatchUIV, GTNGItemList.ParallelControlHatchUMV,
        GTNGItemList.ParallelControlHatchUXV, GTNGItemList.ParallelControlHatchMAX };

    private ParallelMachineLoader() {}

    public static void register() {
        GTNGItemList.LargeFurnace.set(new LargeFurnace(GTNGMachineID.LargeFurnace.id, "LargeFurnace", ""));
        for (int i = 0; i < HATCHES.length; i++)
            HATCHES[i].set(new ParallelControlHatch(GTNGMachineID.ParallelControlHatchIV.id + i,
                "ParallelControlHatch" + (i + 5), i + 5));
    }
}
