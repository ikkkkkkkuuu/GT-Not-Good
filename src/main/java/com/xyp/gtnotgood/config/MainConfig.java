package com.xyp.gtnotgood.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class MainConfig {

    private static boolean configLoaded = false;
    private static Configuration configuration;

    public static boolean someOption = true;

    // EOH
    public static boolean gasInput = true;
    public static boolean eohSuccessRateControls = true;
    public static double recipeChance = 100;

    // FOG
    public static boolean fogUpdate = true;

    // 黑洞压缩机
    public static boolean blackHoleCompressorStabilityLock = true;

    // WATER
    public static boolean water = true;
    public static boolean grade8WaterPurificationEnabled = true;
    public static boolean grade7WaterPurificationEnabled = true;
    public static boolean grade6WaterPurificationEnabled = true;
    public static boolean grade5WaterPurificationEnabled = true;
    public static boolean grade4WaterPurificationEnabled = true;
    public static boolean grade3WaterPurificationEnabled = true;
    public static boolean grade2WaterPurificationEnabled = true;
    public static boolean grade1WaterPurificationEnabled = true;

    private static final String CATEGORY_PURIFIED_WATER = "净化水";
    private static final String CATEGORY_PURIFIED_COM = "黑洞压缩机";
    private static final String CATEGORY_PURIFIED_FOG = "诸神之锻炉";
    private static final String CATEGORY_PURIFIED_EOH = "鸿蒙之眼";

    public static synchronized void ensureLoaded() {
        if (!configLoaded) {
            loadConfiguration();
        }
    }

    private static void loadConfiguration() {

        // 确保主配置已经确定了 GTNOTGOOD 目录
        Config.ensureLoaded();

        File configFile = new File(Config.getConfigDirectory(), "main.cfg");

        configuration = new Configuration(configFile);
        configuration.addCustomCategoryComment(CATEGORY_PURIFIED_EOH, "调整鸿蒙之眼的流体消耗和配方成功率。");
        configuration.addCustomCategoryComment(CATEGORY_PURIFIED_FOG, "调整诸神之锻炉升级的材料和解锁条件。");
        configuration.addCustomCategoryComment(CATEGORY_PURIFIED_COM, "控制黑洞压缩机是否锁定稳定性。");
        configuration.addCustomCategoryComment(CATEGORY_PURIFIED_WATER, "调整各等级净化水机器的处理规则；仍需搭建正确的多方块结构。");

        gasInput = configuration.getBoolean("GasInPut", CATEGORY_PURIFIED_EOH, gasInput,
            "鸿蒙之眼配方流体输入控制，控制是否需要输入流体才会工作，开启后鸿蒙不需要流体输入即可工作");

        eohSuccessRateControls = configuration.getBoolean("EOHSuccessRateControls", CATEGORY_PURIFIED_EOH,
            eohSuccessRateControls, "鸿蒙之眼配方成功率控制，控制是否使用自定义成功率，开启后将使用自定义成功率");

        recipeChance = configuration.getFloat("RecipeChance", CATEGORY_PURIFIED_EOH, (float) recipeChance, 0.0f, 100.0f,
            "鸿蒙之眼配方成功率，控制鸿蒙之眼配方的成功率，范围为0-100，默认100");

        fogUpdate = configuration.getBoolean("FOGUpDate", CATEGORY_PURIFIED_FOG, fogUpdate,
            "诸神之锻炉升级模块随便点，无视材料，分支，引力子碎片");

        blackHoleCompressorStabilityLock = configuration.getBoolean("BlackHoleCompressorStabilityLock",
            CATEGORY_PURIFIED_COM, blackHoleCompressorStabilityLock, "黑洞压缩机稳定性锁定，开启后黑洞压缩机的稳定性将被锁定");

        water = configuration.getBoolean("Water", CATEGORY_PURIFIED_WATER, water, "净化水机器修改仅改动了内部处理逻辑，机器多方块结构依然要保证正确！");

        grade1WaterPurificationEnabled = configuration.getBoolean("Grade1WaterPurificationEnabled",
            CATEGORY_PURIFIED_WATER, grade1WaterPurificationEnabled, "开启后1级水机器的过滤器永不损坏");

        grade2WaterPurificationEnabled = configuration.getBoolean("Grade2WaterPurificationEnabled",
            CATEGORY_PURIFIED_WATER, grade2WaterPurificationEnabled,
            "开启后2级水机器百分百成功并且输入臭氧过量也不会爆炸(注：配方百分百成功是利用ASM字节码进行修改,如果您在游玩中使用热重载关闭了这个功能,对配方的修改也不会失效!必须重启游戏才可以");

        grade3WaterPurificationEnabled = configuration.getBoolean("Grade3WaterPurificationEnabled",
            CATEGORY_PURIFIED_WATER, grade3WaterPurificationEnabled, "开启后3级水机器输入2级水即可工作并且百分百成功，不需要任何额外自动化");

        grade4WaterPurificationEnabled = configuration.getBoolean("Grade4WaterPurificationEnabled",
            CATEGORY_PURIFIED_WATER, grade4WaterPurificationEnabled, "开启后4级水机器输入3级水即可工作并且百分百成功，不需要任何额外自动化");

        grade5WaterPurificationEnabled = configuration.getBoolean("Grade5WaterPurificationEnabled",
            CATEGORY_PURIFIED_WATER, grade5WaterPurificationEnabled, "开启后5级水机器输入4级水即可工作并且百分百成功，不需要任何额外自动化");

        grade6WaterPurificationEnabled = configuration.getBoolean("Grade6WaterPurificationEnabled",
            CATEGORY_PURIFIED_WATER, grade6WaterPurificationEnabled, "开启后6级水机器输入5级水即可工作并且百分百成功，不需要任何额外自动化");

        grade7WaterPurificationEnabled = configuration.getBoolean("Grade7WaterPurificationEnabled",
            CATEGORY_PURIFIED_WATER, grade7WaterPurificationEnabled, "开启后7级水机器输入6级水即可工作并且百分百成功，不需要任何额外自动化");

        grade8WaterPurificationEnabled = configuration.getBoolean("Grade8WaterPurificationEnabled",
            CATEGORY_PURIFIED_WATER, grade8WaterPurificationEnabled, "开启后8级水机器输入7级水即可工作并且百分百成功，不需要任何额外自动化");

        if (configuration.hasChanged()) {
            configuration.save();
        }

        configLoaded = true;
    }

    public static synchronized Configuration getConfiguration() {
        ensureLoaded();
        return configuration;
    }

    public static boolean isSomeOption() {
        ensureLoaded();
        return someOption;
    }
}
