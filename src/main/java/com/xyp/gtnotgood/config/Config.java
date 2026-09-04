package com.xyp.gtnotgood.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.xyp.gtnotgood.common.items.toolbelt.ConfigData;
import com.xyp.gtnotgood.utils.enums.ModList;

/**
 * Loads and saves GT Not Good's Forge configuration values.
 */
public class Config {

    private static final String CATEGORY_CUT_CORNERS = "CutCorners";
    private static final String CATEGORY_CROPSNH = "CropsNH";
    private static final String CATEGORY_FORESTRY = "Forestry";
    private static final String CATEGORY_GREGTECH = "GregTech";
    private static final String CATEGORY_THAUMCRAFT = "Thaumcraft";
    private static final String CATEGORY_TOOL_BELT = "Tool_Belt";
    private static final String CATEGORY_TORCHERINO = "Torcherino";
    private static final String CATEGORY_WIRELESS_MULTIBLOCK = "Wireless_Multiblock";

    private static final File DEFAULT_CONFIG_DIRECTORY = new File(
        System.getProperty("user.dir"),
        "config/" + "GTNOTGOOD");
    private static final File DEFAULT_CONFIG_FILE = new File(
        DEFAULT_CONFIG_DIRECTORY,
        ModList.ModIds.GT_NOT_GOOD + ".cfg");
    private static boolean configLoaded = false;
    private static Configuration configuration;
    private static File configDirectory;

    public static String greeting = "Hello from GT-Not-Good";
    public static boolean enableAlwaysDisplayRecipeOwner = true;
    public static boolean enableAlwaysDisplayWailaAverageNS = true;
    public static boolean enableAlwaysDisplayNEIOriginalVoltage = true;
    public static int recipeSpeedMode = 1;
    public static int recipeSpeedFixedDuration = 1;
    public static float recipeSpeedMultiplier = 0.1F;
    public static boolean recipeSpeedFullFluidOutput = true;
    public static boolean enableCropInstantGrowth = true;
    public static boolean enableCropMaxStats = true;
    public static boolean enableCropGuaranteedSeedDrop = true;
    public static boolean enableBeeAlwaysJubilant = true;
    public static boolean enableBeeHomozygousOffspring = true;
    public static boolean enableBeeMaxGenomeOnBreed = true;
    public static boolean enableBeeIgnoreDimensionMutation = true;
    public static boolean enableBeeIgnoreResourceMutation = true;
    /** Disables Thaumcraft and WarpTheory warp events without changing any warp values. */
    public static boolean disableWarpEvents = true;
    /** Treats every Thaumcraft research key as completed when enabled. */
    public static boolean tcUnlockAllResearch = false;
    /** Prevents research-point deductions while allowing scan rewards to be added normally. */
    public static boolean tcFreeResearchAspects = true;
    /** Allows scanning compound aspects without discovering their parent aspects first. */
    public static boolean tcScanIgnoreParentAspects = true;
    /** Prevents infusion instability events while allowing the infusion cycle to continue normally. */
    public static boolean tcInfusionNoInstability = true;
    /** Makes vis drainage succeed without consuming vis from nearby nodes. */
    public static boolean tcInfiniteVis = true;
    public static boolean releaseToSwap = true;
    public static boolean allowClickOutsideBounds = false;
    public static boolean displayEmptySlots = true;
    public static boolean minecraftHasNoCircles = false;
    public static float radialDeadzoneOffset = 8.0f;
    /** Fixed duration of one wireless cross-recipe processing batch, in ticks. */
    public static int wirelessCrossRecipeDurationTicks = 128;
    /** Maximum number of recipe matches processed in one wireless cross-recipe batch. */
    public static int wirelessCrossRecipeParallelLimit = 200;
    /** Multiplier applied to Torcherino extra ticks when the target is a safely accelerated GregTech machine. */
    public static float torcherinoGregTechAccelerationDiscount = 0.8F;
    /** Maximum configurable X radius for ordinary area Torcherinos. */
    public static int torcherinoMaxXRadius = 4;
    /** Maximum configurable Y radius for ordinary area Torcherinos. */
    public static int torcherinoMaxYRadius = 1;
    /** Maximum configurable Z radius for ordinary area Torcherinos. */
    public static int torcherinoMaxZRadius = 4;
    /** Maximum GUI speed level before the block tier multiplier is applied. */
    public static int torcherinoMaxSpeedLevel = 4;
    /** Enables a per-position nanosecond budget while a Torcherino applies acceleration. */
    public static boolean torcherinoEnableTickBudget = true;
    /** Nanosecond budget used when Torcherino tick budgeting is enabled. */
    public static long torcherinoTickBudgetNanos = 1000000L;
    /** Allows multiple Torcherinos to accelerate the same position in the same world tick when enabled. */
    public static boolean torcherinoEnableStackingAcceleration = false;
    /** Prevents overlapping Torcherinos from accelerating a position more than once per world tick. */
    public static boolean torcherinoEnableOverlapDetection = true;
    /** Registers wireless Torcherinos and their Data Stick binding support when enabled. */
    public static boolean enableWirelessTorcherino = true;
    /** X/Z binding radius for wireless Torcherinos; the Y axis covers world height. */
    public static int wirelessTorcherinoRadius = 8;
    /** Maximum number of machines bound to one wireless Torcherino. */
    public static int wirelessTorcherinoMaxBoundMachines = 64;

    // region VeinMiningPickaxe 配置
    public static class VeinMinerPickaxe {

        public static int maxAmount = 327670;
        public static int maxRange = 32;
    }

    /**
     * Ensures config values are available to code that can run before this mod's Forge preInit event.
     */
    public static synchronized void ensureLoaded() {
        if (!configLoaded) {
            loadConfiguration(DEFAULT_CONFIG_FILE);
        }
    }

    public static synchronized void synchronizeConfiguration(File configFile) {
        loadConfiguration(configFile);
    }

    private static void loadConfiguration(File configFile) {
        // 始终使用 config/GTNOTGOOD 作为本模组配置目录
        File configDirectory = DEFAULT_CONFIG_DIRECTORY;

        if (!configDirectory.exists()) {
            if (!configDirectory.mkdirs() && !configDirectory.isDirectory()) {
                throw new IllegalStateException("无法创建配置目录: " + configDirectory.getAbsolutePath());
            }
        }

        // 不使用 Forge 传入的 configFile 路径，
        // 而是强制将 GT-Not-Good.cfg 放到 config/GTNOTGOOD 下
        File actualConfigFile = new File(configDirectory, ModList.ModIds.GT_NOT_GOOD + ".cfg");

        Configuration configuration = new Configuration(configFile);
        Config.configuration = configuration;
        Config.configDirectory = configFile.getParentFile();

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");
        configuration.addCustomCategoryComment(CATEGORY_CUT_CORNERS, "配方提速配置");
        configuration.addCustomCategoryComment(CATEGORY_CROPSNH, "CropsNH 作物配置");
        configuration.addCustomCategoryComment(CATEGORY_FORESTRY, "Forestry 蜜蜂杂交配置");
        configuration.addCustomCategoryComment(CATEGORY_GREGTECH, "GregTech 客户端显示配置");
        configuration.addCustomCategoryComment(CATEGORY_THAUMCRAFT, "Thaumcraft 扭曲与研究配置");
        configuration.addCustomCategoryComment(CATEGORY_TOOL_BELT, "工具腰带的配置设置");
        configuration.addCustomCategoryComment(CATEGORY_TORCHERINO, "加速火把与无线加速火把配置");
        configuration.addCustomCategoryComment(CATEGORY_WIRELESS_MULTIBLOCK, "无线多方块跨配方并行配置");

        wirelessCrossRecipeDurationTicks = configuration.getInt(
            "crossRecipeDurationTicks",
            CATEGORY_WIRELESS_MULTIBLOCK,
            wirelessCrossRecipeDurationTicks,
            1,
            Integer.MAX_VALUE,
            "无线跨配方每批固定工作时间,单位 tick。修改后重启游戏生效。");

        wirelessCrossRecipeParallelLimit = configuration.getInt(
            "crossRecipeParallelLimit",
            CATEGORY_WIRELESS_MULTIBLOCK,
            wirelessCrossRecipeParallelLimit,
            1,
            Integer.MAX_VALUE,
            "无线跨配方每批最多处理的配方数量。修改后重启游戏生效。");

        torcherinoGregTechAccelerationDiscount = configuration.getFloat(
            "gregTechAccelerationDiscount",
            CATEGORY_TORCHERINO,
            torcherinoGregTechAccelerationDiscount,
            0.0F,
            1.0F,
            "加速 GregTech 机器进度时的折扣系数。0.8 表示请求 100 个额外 tick 时实际推进 80 tick。");

        torcherinoMaxXRadius = configuration
            .getInt("maxXRadius", CATEGORY_TORCHERINO, torcherinoMaxXRadius, 0, 16, "普通加速火把 X 轴最大半径。4 表示最大宽度 9 格。");

        torcherinoMaxYRadius = configuration
            .getInt("maxYRadius", CATEGORY_TORCHERINO, torcherinoMaxYRadius, 0, 8, "普通加速火把 Y 轴最大半径。1 表示最大高度 3 格。");

        torcherinoMaxZRadius = configuration
            .getInt("maxZRadius", CATEGORY_TORCHERINO, torcherinoMaxZRadius, 0, 16, "普通加速火把 Z 轴最大半径。4 表示最大长度 9 格。");

        torcherinoMaxSpeedLevel = configuration.getInt(
            "maxSpeedLevel",
            CATEGORY_TORCHERINO,
            torcherinoMaxSpeedLevel,
            0,
            32,
            "火把界面的最大速度档位。实际额外 tick = 档位 x 方块等级倍率。");

        torcherinoEnableTickBudget = configuration.getBoolean(
            "enableTickBudget",
            CATEGORY_TORCHERINO,
            torcherinoEnableTickBudget,
            "开启后,每个目标位置的加速会受到纳秒预算限制,避免极端设置卡死服务器。");

        torcherinoTickBudgetNanos = configuration.getInt(
            "tickBudgetNanos",
            CATEGORY_TORCHERINO,
            (int) torcherinoTickBudgetNanos,
            100000,
            100000000,
            "加速单个目标位置时允许消耗的纳秒数。默认 1000000 = 1ms。");

        torcherinoEnableStackingAcceleration = configuration.getBoolean(
            "enableStackingAcceleration",
            CATEGORY_TORCHERINO,
            torcherinoEnableStackingAcceleration,
            "开启后,多个火把可在同一 tick 叠加加速同一个目标。关闭时通常只保留覆盖目标的最高速度。");

        torcherinoEnableOverlapDetection = configuration.getBoolean(
            "enableOverlapDetection",
            CATEGORY_TORCHERINO,
            torcherinoEnableOverlapDetection,
            "开启后,当叠加加速关闭时,同一世界 tick 内已被更高或相同速度加速的位置不会再次被低速火把处理。");

        enableWirelessTorcherino = configuration.getBoolean(
            "enableWirelessTorcherino",
            CATEGORY_TORCHERINO,
            enableWirelessTorcherino,
            "开启后注册无线加速火把及其 GT 数据棒绑定功能。关闭后只保留普通加速火把。");

        wirelessTorcherinoRadius = configuration.getInt(
            "wirelessTorcherinoRadius",
            CATEGORY_TORCHERINO,
            wirelessTorcherinoRadius,
            0,
            16,
            "无线加速火把 X/Z 绑定半径。Y 轴固定覆盖世界高度。8 表示 17x世界高度x17。");

        wirelessTorcherinoMaxBoundMachines = configuration.getInt(
            "wirelessTorcherinoMaxBoundMachines",
            CATEGORY_TORCHERINO,
            wirelessTorcherinoMaxBoundMachines,
            1,
            256,
            "单个无线加速火把最多可绑定的机器数量。");

        recipeSpeedMode = configuration
            .getInt("mode", CATEGORY_CUT_CORNERS, recipeSpeedMode, 0, 2, "配方时长修改模式。0=不修改; 1=固定时长; 2=倍率缩短。");

        recipeSpeedFixedDuration = configuration.getInt(
            "fixedDuration",
            CATEGORY_CUT_CORNERS,
            recipeSpeedFixedDuration,
            1,
            Integer.MAX_VALUE,
            "mode=1 时生效,所有配方时长固定为此 tick 数。");

        recipeSpeedMultiplier = configuration.getFloat(
            "multiplier",
            CATEGORY_CUT_CORNERS,
            recipeSpeedMultiplier,
            0.0001F,
            Float.MAX_VALUE,
            "mode=2 时生效,配方时长乘以此倍率,结果至少为 1 tick。");

        recipeSpeedFullFluidOutput = configuration.getBoolean(
            "fullFluidOutput",
            CATEGORY_CUT_CORNERS,
            recipeSpeedFullFluidOutput,
            "开启后,单方块机器自动输出流体时一次抽干内部储罐,避免高速配方导致流体堵塞。");

        enableCropInstantGrowth = configuration
            .getBoolean("enableInstantGrowth", CATEGORY_CROPSNH, enableCropInstantGrowth, "开启后,CropsNH 作物棒在生长判定时直接成熟。");

        enableCropMaxStats = configuration.getBoolean(
            "enableMaxStats",
            CATEGORY_CROPSNH,
            enableCropMaxStats,
            "开启后,所有新生成的 CropsNH 种子生长/产量/抗性三项属性都拉满到 31。");

        enableCropGuaranteedSeedDrop = configuration.getBoolean(
            "enableGuaranteedSeedDrop",
            CATEGORY_CROPSNH,
            enableCropGuaranteedSeedDrop,
            "开启后,左键收获成熟 CropsNH 作物必定掉落种子,绕过抗性概率判定。");

        enableBeeAlwaysJubilant = configuration.getBoolean(
            "enableBeeAlwaysJubilant",
            CATEGORY_FORESTRY,
            enableBeeAlwaysJubilant,
            "开启后,所有蜜蜂产出特殊产物时都视为气候满足,普通蜂箱也能在任意气候下产出特产。");

        enableBeeHomozygousOffspring = configuration.getBoolean(
            "enableHomozygousOffspring",
            CATEGORY_FORESTRY,
            enableBeeHomozygousOffspring,
            "开启后,普通蜂箱/蜂房杂交产出的后代只保留纯合基因,不产出杂合子。");

        enableBeeMaxGenomeOnBreed = configuration.getBoolean(
            "enableMaxGenomeOnBreed",
            CATEGORY_FORESTRY,
            enableBeeMaxGenomeOnBreed,
            "开启后,普通蜂箱/蜂房杂交产出的后代会被真正写成满基因,物种保留不变。");

        enableBeeIgnoreDimensionMutation = configuration.getBoolean(
            "enableBeeIgnoreDimensionMutation",
            CATEGORY_FORESTRY,
            enableBeeIgnoreDimensionMutation,
            "开启后,蜜蜂杂交忽略维度限制,原本要求特定维度的蜂可在任意维度杂交。");

        enableBeeIgnoreResourceMutation = configuration.getBoolean(
            "enableBeeIgnoreResourceMutation",
            CATEGORY_FORESTRY,
            enableBeeIgnoreResourceMutation,
            "开启后,蜜蜂杂交忽略蜂箱下方指定方块或运行中 GT 机器之类的硬性条件。");

        disableWarpEvents = configuration.getBoolean(
            "disableWarpEvents",
            CATEGORY_THAUMCRAFT,
            disableWarpEvents,
            "开启后,神秘时代及 WarpTheory 的扭曲事件永不触发,但永久/临时/黏滞扭曲值及临时扭曲的正常衰减不受影响。");

        tcUnlockAllResearch = configuration.getBoolean(
            "unlockAllResearch",
            CATEGORY_THAUMCRAFT,
            tcUnlockAllResearch,
            "开启后,所有研究都视为已完成。注意:研究笔记本 GUI 可能显示异常。");

        tcFreeResearchAspects = configuration.getBoolean(
            "freeResearchAspects",
            CATEGORY_THAUMCRAFT,
            tcFreeResearchAspects,
            "开启后,研究点数不会因研究消耗而减少,但扫描获得的研究点照常增加。");

        tcScanIgnoreParentAspects = configuration.getBoolean(
            "scanIgnoreParentAspects",
            CATEGORY_THAUMCRAFT,
            tcScanIgnoreParentAspects,
            "开启后,扫描源质时无视必须先发现父源质的顺序要求,可直接发现复合源质。");

        tcInfusionNoInstability = configuration.getBoolean(
            "infusionNoInstability",
            CATEGORY_THAUMCRAFT,
            tcInfusionNoInstability,
            "开启后,注魔祭坛注魔时不会产生失稳负面事件,合成进度照常进行。");

        tcInfiniteVis = configuration
            .getBoolean("infiniteVis", CATEGORY_THAUMCRAFT, tcInfiniteVis, "开启后,从 vis 网络抽取魔力永远成功且不消耗节点存量。");

        enableAlwaysDisplayRecipeOwner = configuration.getBoolean(
            "enableAlwaysDisplayRecipeOwner",
            CATEGORY_GREGTECH,
            enableAlwaysDisplayRecipeOwner,
            "开启后,强制 GregTech 的 NEI 配方页面显示配方所属模组。");

        enableAlwaysDisplayWailaAverageNS = configuration.getBoolean(
            "enableAlwaysDisplayWailaAverageNS",
            CATEGORY_GREGTECH,
            enableAlwaysDisplayWailaAverageNS,
            "开启后,强制 GregTech 的 Waila 信息显示平均耗时(ns)。");

        enableAlwaysDisplayNEIOriginalVoltage = configuration.getBoolean(
            "enableAlwaysDisplayNEIOriginalVoltage",
            CATEGORY_GREGTECH,
            enableAlwaysDisplayNEIOriginalVoltage,
            "开启后,强制 GregTech 的 NEI 配方页面显示原始电压等级。");

        releaseToSwap = configuration
            .getBoolean("config.toolbelt.releaseToSwap", CATEGORY_TOOL_BELT, releaseToSwap, "开启后,松开工具腰带按键时与高亮物品交换。");

        allowClickOutsideBounds = configuration.getBoolean(
            "config.toolbelt.allowClickOutsideBounds",
            CATEGORY_TOOL_BELT,
            allowClickOutsideBounds,
            "开启后,允许点击径向菜单死区来关闭工具腰带。");

        displayEmptySlots = configuration
            .getBoolean("config.toolbelt.displayEmptySlots", CATEGORY_TOOL_BELT, displayEmptySlots, "开启后,显示径向菜单中的空槽位。");

        minecraftHasNoCircles = configuration.getBoolean(
            "config.toolbelt.minecraftHasNoCircles",
            CATEGORY_TOOL_BELT,
            minecraftHasNoCircles,
            "开启后,使用方形风格绘制径向菜单。");

        radialDeadzoneOffset = configuration.getFloat(
            "config.toolbelt.radialDeadzoneOffset",
            CATEGORY_TOOL_BELT,
            radialDeadzoneOffset,
            0.0f,
            64.0f,
            "工具腰带径向菜单中心死区额外偏移像素数。");

        ConfigData.syncFromMainConfig();

        if (configuration.hasChanged()) {
            configuration.save();
        }
        configLoaded = true;
    }

    /**
     * Returns the main Forge configuration shared with client-only integrations.
     * The lazy load keeps mixin and pre-initialization callers safe when they run
     * before the normal Forge configuration event.
     *
     * @return loaded main configuration
     */
    public static synchronized Configuration getConfiguration() {
        ensureLoaded();
        return configuration;
    }

    /**
     * Returns the directory containing this mod's configuration file.
     *
     * @return configuration directory
     */
    public static synchronized File getConfigDirectory() {
        ensureLoaded();
        return configDirectory == null ? DEFAULT_CONFIG_FILE.getParentFile() : configDirectory;
    }

    public static int getModifiedRecipeDuration(int original) {
        switch (recipeSpeedMode) {
            case 1:
                return Math.max(1, recipeSpeedFixedDuration);
            case 2:
                return Math.max(1, (int) (original * recipeSpeedMultiplier));
            case 0:
            default:
                return original;
        }
    }
}
