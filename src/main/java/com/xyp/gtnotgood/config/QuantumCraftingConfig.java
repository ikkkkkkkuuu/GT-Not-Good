package com.xyp.gtnotgood.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;

@Config(modid = ModList.ModIds.GT_NOT_GOOD, configSubDirectory = "GTNOTGOOD", filename = "QuantumCrafting")
public final class QuantumCraftingConfig {

    @Config.Comment("记录每次量子合成计算的步骤")
    @Config.DefaultBoolean(false)
    public static boolean enableCalculationLog;

    @Config.Comment("记录量子合成计算的阶段耗时与计数")
    @Config.DefaultBoolean(false)
    public static boolean enableCalculationPerformanceLog;

    @Config.Comment("缺料时尝试替代样板的时间上限（秒），0 表示不尝试")
    @Config.RangeInt(min = 0)
    @Config.DefaultInt(5)
    public static int calculationFallbackSeconds;

    @Config.Comment("独立服务器单次量子计算的时间上限（秒），0 表示不限")
    @Config.RangeInt(min = 0)
    @Config.DefaultInt(30)
    public static int calculationLimitSeconds;
}
