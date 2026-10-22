package com.xyp.gtnotgood.loader;

import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.machines.hatch.modular.ModuleType;
import com.xyp.gtnotgood.common.machines.hatch.modular.ParameterModuleHatch;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;

public final class ModularHatchLoader {

    private ModularHatchLoader() {}

    public static final GTNGItemList[] STATIC_PARALLEL = { GTNGItemList.StaticParallelControllerT1,
        GTNGItemList.StaticParallelControllerT2, GTNGItemList.StaticParallelControllerT3,
        GTNGItemList.StaticParallelControllerT4, GTNGItemList.StaticParallelControllerT5,
        GTNGItemList.StaticParallelControllerT6, GTNGItemList.StaticParallelControllerT7,
        GTNGItemList.StaticParallelControllerT8 };

    public static final GTNGItemList[] DYNAMIC_PARALLEL = { GTNGItemList.DynamicParallelControllerT1,
        GTNGItemList.DynamicParallelControllerT2, GTNGItemList.DynamicParallelControllerT3,
        GTNGItemList.DynamicParallelControllerT4, GTNGItemList.DynamicParallelControllerT5,
        GTNGItemList.DynamicParallelControllerT6, GTNGItemList.DynamicParallelControllerT7,
        GTNGItemList.DynamicParallelControllerT8 };

    public static final GTNGItemList[] STATIC_SPEED = { GTNGItemList.StaticSpeedControllerT1,
        GTNGItemList.StaticSpeedControllerT2, GTNGItemList.StaticSpeedControllerT3,
        GTNGItemList.StaticSpeedControllerT4, GTNGItemList.StaticSpeedControllerT5,
        GTNGItemList.StaticSpeedControllerT6, GTNGItemList.StaticSpeedControllerT7,
        GTNGItemList.StaticSpeedControllerT8 };

    public static final GTNGItemList[] DYNAMIC_SPEED = { GTNGItemList.DynamicSpeedControllerT1,
        GTNGItemList.DynamicSpeedControllerT2, GTNGItemList.DynamicSpeedControllerT3,
        GTNGItemList.DynamicSpeedControllerT4, GTNGItemList.DynamicSpeedControllerT5,
        GTNGItemList.DynamicSpeedControllerT6, GTNGItemList.DynamicSpeedControllerT7,
        GTNGItemList.DynamicSpeedControllerT8 };

    public static final GTNGItemList[] POWER = { GTNGItemList.PowerConsumptionControllerT1,
        GTNGItemList.PowerConsumptionControllerT2, GTNGItemList.PowerConsumptionControllerT3,
        GTNGItemList.PowerConsumptionControllerT4, GTNGItemList.PowerConsumptionControllerT5,
        GTNGItemList.PowerConsumptionControllerT6, GTNGItemList.PowerConsumptionControllerT7,
        GTNGItemList.PowerConsumptionControllerT8 };

    public static void register() {
        registerParameters(STATIC_PARALLEL, ModuleType.Parallel, false);
        registerParameters(DYNAMIC_PARALLEL, ModuleType.Parallel, true);
        registerParameters(STATIC_SPEED, ModuleType.Speed, false);
        registerParameters(DYNAMIC_SPEED, ModuleType.Speed, true);
        registerParameters(POWER, ModuleType.PowerConsumption, false);
        registerOverclock(GTNGItemList.LowSpeedPerfectOverclockController, 1);
        registerOverclock(GTNGItemList.PerfectOverclockController, 2);
        registerOverclock(GTNGItemList.SingularityPerfectOverclockController, 3);
        registerCore(GTNGItemList.ExecutionCore, CoreType.Normal);
        registerCore(GTNGItemList.AdvancedExecutionCore, CoreType.Advanced);
        registerCore(GTNGItemList.PerfectExecutionCore, CoreType.Perfect);
    }

    private static void registerParameters(GTNGItemList[] entries, ModuleType type, boolean dynamic) {
        for (int i = 0; i < entries.length; i++) {
            GTNGItemList entry = entries[i];
            entry.set(new ParameterModuleHatch(id(entry), entry.name(), type, i + 1, dynamic));
        }
    }

    private static void registerOverclock(GTNGItemList entry, int tier) {
        entry.set(new ParameterModuleHatch(id(entry), entry.name(), ModuleType.Overclock, tier, false));
    }

    private static void registerCore(GTNGItemList entry, CoreType type) {
        entry.set(new ExecutionCoreHatch(id(entry), entry.name(), type));
    }

    private static int id(GTNGItemList entry) {
        return GTNGMachineID.valueOf(entry.name()).id;
    }
}
