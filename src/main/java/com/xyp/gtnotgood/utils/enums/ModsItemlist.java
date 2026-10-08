package com.xyp.gtnotgood.utils.enums;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Mods;
import gregtech.api.util.GTModHandler;
import ic2.api.item.IC2Items;

public enum ModsItemlist {

    // 量子桥接卡
    QuantumBridgeCard(Mods.AE2FluidCraft.ID, "quantum_bridge_card", true),
    // 能量卡
    EnergyCard(Mods.AE2FluidCraft.ID, "energy_card", true),
    // ME二合一接口（方块）
    FluidInterfaceBlock(Mods.AE2FluidCraft.ID, "fluid_interface", 0),
    // 256k-ME流体存储组件
    FluidStorageComponent256k(Mods.AE2FluidCraft.ID, "fluid_part", 4),
    // 4096k-ME流体存储组件
    FluidStorageComponent4096k(Mods.AE2FluidCraft.ID, "fluid_part", 6),
    // 16384k-ME流体存储组件
    FluidStorageComponent16384k(Mods.AE2FluidCraft.ID, "fluid_part", 7),
    // ME人造宇宙流体存储元件
    UniverseFluidStorageCell(Mods.AE2FluidCraft.ID, "fluid_storage.Universe", 0),
    // ME高级多流体存储外壳
    AdvancedFluidStorageHousing(Mods.AE2FluidCraft.ID, "fluid_storage_housing", 3),
    // ME二合一接口（线缆部件）
    FluidInterfacePart(Mods.AE2FluidCraft.ID, "part_fluid_interface", 0),
    // ME流体存储总线
    FluidStorageBus(Mods.AE2FluidCraft.ID, "part_fluid_storage_bus", 0),

    // 无限增幅卡
    InfinityBoosterCard(Mods.AE2WCT.ID, "infinityBoosterCard", true),

    // 运算压印模板
    CalculationPress(Mods.AppliedEnergistics2.ID, "item.ItemMultiMaterial", 13),
    // 工程压印模板
    EngineeringPress(Mods.AppliedEnergistics2.ID, "item.ItemMultiMaterial", 14),
    // 逻辑压印模板
    LogicPress(Mods.AppliedEnergistics2.ID, "item.ItemMultiMaterial", 15),
    // 硅压印模板
    SiliconPress(Mods.AppliedEnergistics2.ID, "item.ItemMultiMaterial", 19),
    // ME 接口
    MEInterface(Mods.AppliedEnergistics2.ID, "tile.BlockInterface"),

    // 阿卡西记录
    AkashicRecord(Mods.Avaritia.ID, "Akashic_Record", 0),
    // 寰宇肉丸
    CosmicMeatballs(Mods.Avaritia.ID, "Cosmic_Meatballs", 0),
    // 水晶矩阵
    CrystalMatrix(Mods.Avaritia.ID, "Crystal_Matrix", 0),
    // 终望珍珠
    EndestPearl(Mods.Avaritia.ID, "Endest_Pearl", 0),
    // 无尽胸甲
    InfinityChestplate(Mods.Avaritia.ID, "Infinity_Chest", 0),
    // 无尽头盔
    InfinityHelmet(Mods.Avaritia.ID, "Infinity_Helm", 0),
    // 无尽护腿
    InfinityLeggings(Mods.Avaritia.ID, "Infinity_Pants", 0),
    // 世界崩解之镐
    InfinityPickaxe(Mods.Avaritia.ID, "Infinity_Pickaxe", 0),
    // 无尽靴子
    InfinityBoots(Mods.Avaritia.ID, "Infinity_Shoes", 0),
    // 寰宇支配之剑
    InfinitySword(Mods.Avaritia.ID, "Infinity_Sword", 0),
    // 阿蒙克气血宝珠
    OrbArmok(Mods.Avaritia.ID, "Orb_Armok", 0),
    // 钻石晶格
    DiamondLattice(Mods.Avaritia.ID, "Resource", 0),
    // 水晶矩阵锭
    CrystalMatrixIngot(Mods.Avaritia.ID, "Resource", 1),
    // 无尽催化剂
    InfinityCatalyst(Mods.Avaritia.ID, "Resource", 5),
    // 无尽之锭
    InfinityIngot(Mods.Avaritia.ID, "Resource", 6),
    // 唱片碎片
    RecordFragment(Mods.Avaritia.ID, "Resource", 7),
    // 恒星燃料
    StellarFuel(Mods.Avaritia.ID, "Resource", 8),
    // 铁奇点
    IronSingularity(Mods.Avaritia.ID, "Singularity", 0),
    // 金奇点
    GoldSingularity(Mods.Avaritia.ID, "Singularity", 1),
    // 末影锭奇点
    EnderiumSingularity(Mods.Avaritia.ID, "Singularity", 10),
    // 黏土奇点
    ClaySingularity(Mods.Avaritia.ID, "Singularity", 11),
    // 青金石奇点
    LapisSingularity(Mods.Avaritia.ID, "Singularity", 2),
    // 红石奇点
    RedstoneSingularity(Mods.Avaritia.ID, "Singularity", 3),
    // 下界石英奇点
    QuartzSingularity(Mods.Avaritia.ID, "Singularity", 4),
    // 铜奇点
    CopperSingularity(Mods.Avaritia.ID, "Singularity", 5),
    // 锡奇点
    TinSingularity(Mods.Avaritia.ID, "Singularity", 6),
    // 铅奇点
    LeadSingularity(Mods.Avaritia.ID, "Singularity", 7),
    // 银奇点
    SilverSingularity(Mods.Avaritia.ID, "Singularity", 8),
    // 镍奇点
    NickelSingularity(Mods.Avaritia.ID, "Singularity", 9),
    // 超级煲
    UltimateStew(Mods.Avaritia.ID, "Ultimate_Stew", 0),
    // 物质团解压器
    ClusterOpener(Mods.Avaritia.ID, "cluster_opener", 0),

    // 压缩箱子
    CompressedChest(Mods.AvaritiaAddons.ID, "CompressedChest", 0),
    // 梦魇工作台
    ExtremeAutoCrafter(Mods.AvaritiaAddons.ID, "ExtremeAutoCrafter", 0),
    // 无尽箱子
    InfinityChest(Mods.AvaritiaAddons.ID, "InfinityChest", 0),

    // 松树树叶
    PineLeaves(Mods.BiomesOPlenty.ID, "colorizedLeaves2", 1),
    // 松树树苗
    PineSapling(Mods.BiomesOPlenty.ID, "colorizedSaplings", 5),
    // 粉珊瑚
    PinkCoral(Mods.BiomesOPlenty.ID, "coral1", 12),
    // 橙珊瑚
    OrangeCoral(Mods.BiomesOPlenty.ID, "coral1", 13),
    // 蓝珊瑚
    BlueCoral(Mods.BiomesOPlenty.ID, "coral1", 14),
    // 夜光珊瑚
    GlowingCoral(Mods.BiomesOPlenty.ID, "coral1", 15),
    // 大型睡莲
    LargeLilyPad(Mods.BiomesOPlenty.ID, "lilyBop", 0),
    // 中型睡莲
    MediumLilyPad(Mods.BiomesOPlenty.ID, "lilyBop", 1),
    // 小型睡莲
    SmallLilyPad(Mods.BiomesOPlenty.ID, "lilyBop", 2),
    // 松树原木
    BiomesOPlentyPineLog(Mods.BiomesOPlenty.ID, "logs4", 0),
    // 松果
    PineCone(Mods.BiomesOPlenty.ID, "misc", 13),

    // 血之TNT
    BloodTnt(Mods.BloodArsenal.ID, "blood_tnt", 0),
    // 生命注入器
    LifeInfuser(Mods.BloodArsenal.ID, "life_infuser", 0),
    // 生命能量具现器
    LpMaterializer(Mods.BloodArsenal.ID, "lp_materializer", 0),

    // 血之祭坛
    BloodAltar(Mods.BloodMagic.ID, "Altar", 0),
    // [觉醒]激活水晶
    AwakenedActivationCrystal(Mods.BloodMagic.ID, "activationCrystal", 1),
    // 学徒气血宝珠
    ApprenticeBloodOrb(Mods.BloodMagic.ID, "apprenticeBloodOrb", 0),
    // 贤者气血宝珠
    ArchmageBloodOrb(Mods.BloodMagic.ID, "archmageBloodOrb", 0),
    // 炼金术台
    AlchemyTable(Mods.BloodMagic.ID, "blockWritingTable", 0),
    // 测试宝珠
    CreativeBloodOrb(Mods.BloodMagic.ID, "creativeFiller", 0),
    // 献祭刀
    DaggerOfSacrifice(Mods.BloodMagic.ID, "daggerOfSacrifice", 0),
    // 仪式推测杖
    RitualDiviner(Mods.BloodMagic.ID, "itemRitualDiviner", 2),
    // 法师气血宝珠
    MagicianBloodOrb(Mods.BloodMagic.ID, "magicianBloodOrb", 0),
    // 导师气血宝珠
    MasterBloodOrb(Mods.BloodMagic.ID, "masterBloodOrb", 0),
    // 主仪式石
    MasterRitualStone(Mods.BloodMagic.ID, "masterStone", 0),
    // 卓越气血宝珠
    TranscendentBloodOrb(Mods.BloodMagic.ID, "transcendentBloodOrb", 0),
    // 虚弱气血宝珠
    WeakBloodOrb(Mods.BloodMagic.ID, "weakBloodOrb", 0),

    // 魔力钢锭
    ManasteelIngot(Mods.Botania.ID, "manaResource", 0),
    // 魔力珍珠
    ManaPearl(Mods.Botania.ID, "manaResource", 1),
    // 魔力钻石
    ManaDiamond(Mods.Botania.ID, "manaResource", 2),
    // 炼金催化器
    AlchemyCatalyst(Mods.Botania.ID, "alchemyCatalyst", 0),
    // 精灵门核心
    AlfheimPortal(Mods.Botania.ID, "alfheimPortal", 0),
    // 艾琳的意志
    WillOfAhrim(Mods.Botania.ID, "ancientWill", 0),
    // 达洛克的意志
    WillOfDharok(Mods.Botania.ID, "ancientWill", 1),
    // 古赞的意志
    WillOfGuthan(Mods.Botania.ID, "ancientWill", 2),
    // 托拉格的意志
    WillOfTorag(Mods.Botania.ID, "ancientWill", 3),
    // 威拉克的意志
    WillOfVerac(Mods.Botania.ID, "ancientWill", 4),
    // 卡瑞的意志
    WillOfKaril(Mods.Botania.ID, "ancientWill", 5),
    // 彩虹桥方块
    BifrostBlock(Mods.Botania.ID, "bifrostPerm", 0),
    // 彩虹玻璃板
    BifrostPane(Mods.Botania.ID, "bifrostPermPane", 0),
    // 黑莲花
    BlackLotus(Mods.Botania.ID, "blackLotus", 0),
    // 暗黑莲花
    BlackestLotus(Mods.Botania.ID, "blackLotus", 1),
    // 炼造催化器
    ConjurationCatalyst(Mods.Botania.ID, "conjurationCatalyst", 0),
    // 多媒体火花
    CorporeaSpark(Mods.Botania.ID, "corporeaSpark", 0),
    // 主媒体火花
    MasterCorporeaSpark(Mods.Botania.ID, "corporeaSpark", 1),
    // 命运骰子
    Dice(Mods.Botania.ID, "dice", 0),
    // 精灵玻璃
    ElfGlass(Mods.Botania.ID, "elfGlass", 0),
    // 盖亚守护者的头
    GaiaHead(Mods.Botania.ID, "gaiaHead", 0),
    // 魔力透镜：传送
    WarpLens(Mods.Botania.ID, "lens", 18),
    // 魔力透镜：反射
    BounceLens(Mods.Botania.ID, "lens", 5),
    // 植物魔法辞典
    LexicaBotania(Mods.Botania.ID, "lexicon", 0),
    // 磁化指环
    MagnetRing(Mods.Botania.ID, "magnetRing", 0),
    // 不稳定信标
    ManaBeacon(Mods.Botania.ID, "manaBeacon", 0),
    // 盖亚魂锭
    GaiaSpiritIngot(Mods.Botania.ID, "manaResource", 14),
    // 瓶装末地空气
    EnderAirBottle(Mods.Botania.ID, "manaResource", 15),
    // 泰拉钢锭
    TerrasteelIngot(Mods.Botania.ID, "manaResource", 4),
    // 盖亚之魂
    GaiaSpirit(Mods.Botania.ID, "manaResource", 5),
    // 源质钢锭
    ElementiumIngot(Mods.Botania.ID, "manaResource", 7),
    // 精灵尘
    PixieDust(Mods.Botania.ID, "manaResource", 8),
    // 龙石
    Dragonstone(Mods.Botania.ID, "manaResource", 9),
    // 增生之种
    OvergrowthSeed(Mods.Botania.ID, "overgrowthSeed", 0),
    // 粉色手炮
    Pinkinator(Mods.Botania.ID, "pinkinator", 0),
    // 力量传递器
    PistonRelay(Mods.Botania.ID, "pistonRelay", 0),
    // 永恒魔力池
    EverlastingManaPool(Mods.Botania.ID, "pool", 1),
    // 神话魔力池
    FabulousManaPool(Mods.Botania.ID, "pool", 3),
    // 魔力泵
    Pump(Mods.Botania.ID, "pump", 0),
    // 魔法水晶
    ManaPylon(Mods.Botania.ID, "pylon", 0),
    // 自然水晶
    NaturaPylon(Mods.Botania.ID, "pylon", 1),
    // 盖亚水晶
    GaiaPylon(Mods.Botania.ID, "pylon", 2),
    // 触物指环
    ReachRing(Mods.Botania.ID, "reachRing", 0),
    // 破损的唱片
    ScathedMusicDisc(Mods.Botania.ID, "recordGaia2", 0),
    // 魔力转换器
    RfGenerator(Mods.Botania.ID, "rfGenerator", 0),
    // 水之符文
    WaterRune(Mods.Botania.ID, "rune", 0),
    // 火之符文
    FireRune(Mods.Botania.ID, "rune", 1),
    // 暴食符文
    GluttonyRune(Mods.Botania.ID, "rune", 10),
    // 贪婪符文
    GreedRune(Mods.Botania.ID, "rune", 11),
    // 懒惰符文
    SlothRune(Mods.Botania.ID, "rune", 12),
    // 暴怒符文
    WrathRune(Mods.Botania.ID, "rune", 13),
    // 嫉妒符文
    EnvyRune(Mods.Botania.ID, "rune", 14),
    // 傲慢符文
    PrideRune(Mods.Botania.ID, "rune", 15),
    // 地之符文
    EarthRune(Mods.Botania.ID, "rune", 2),
    // 风之符文
    AirRune(Mods.Botania.ID, "rune", 3),
    // 春之符文
    SpringRune(Mods.Botania.ID, "rune", 4),
    // 夏之符文
    SummerRune(Mods.Botania.ID, "rune", 5),
    // 秋之符文
    AutumnRune(Mods.Botania.ID, "rune", 6),
    // 冬之符文
    WinterRune(Mods.Botania.ID, "rune", 7),
    // 魔力符文
    ManaRune(Mods.Botania.ID, "rune", 8),
    // 欲望符文
    LustRune(Mods.Botania.ID, "rune", 9),
    // 符文祭坛
    RunicAltar(Mods.Botania.ID, "runeAltar", 0),
    // 火花
    Spark(Mods.Botania.ID, "spark", 0),
    // 植物魔法花
    SpecialFlower(Mods.Botania.ID, "specialFlower", 0),
    // 盖亚魔力发射器
    GaiaManaSpreader(Mods.Botania.ID, "spreader", 3),
    // 安山岩
    Andesite(Mods.Botania.ID, "stone", 0),
    // 玄武岩
    Basalt(Mods.Botania.ID, "stone", 1),
    // 闪长岩
    Diorite(Mods.Botania.ID, "stone", 2),
    // 花岗岩
    Granite(Mods.Botania.ID, "stone", 3),
    // 魔力钢块
    ManasteelBlock(Mods.Botania.ID, "storage", 0),
    // 泰拉粉碎者
    TerraShatterer(Mods.Botania.ID, "terraPick", 0),
    // 泰拉凝聚板
    TerrestrialAgglomerationPlate(Mods.Botania.ID, "terraPlate", 0),

    // 储罐
    TankBlock(Mods.BuildCraftFactory.ID, "tankBlock", 0),

    // 钻石凿子
    DiamondChisel(Mods.Chisel.ID, "diamondChisel", 0),

    // 创造模式内存
    CreativeMemory(Mods.Computronics.ID, "computronics.ocSpecialParts", 0),

    // 觉醒核心
    AwakenedCore(Mods.DraconicEvolution.ID, "awakenedCore", 0),
    // 小的混沌残片
    SmallChaosFragment(Mods.DraconicEvolution.ID, "chaosFragment", 1),
    // 混沌碎片
    ChaosShard(Mods.DraconicEvolution.ID, "chaosShard", 0),
    // 混沌核心
    ChaoticCore(Mods.DraconicEvolution.ID, "chaoticCore", 0),
    // Dezil的棉花糖
    DezilsMarshmallow(Mods.DraconicEvolution.ID, "dezilsMarshmallow", 0),
    // 龙芯
    DraconicCore(Mods.DraconicEvolution.ID, "draconicCore", 0),
    // 龙块
    DraconiumBlock(Mods.DraconicEvolution.ID, "draconium", 0),
    // 充能龙块
    ChargedDraconiumBlock(Mods.DraconicEvolution.ID, "draconium", 2),
    // 龙之心
    DragonHeart(Mods.DraconicEvolution.ID, "dragonHeart", 0),
    // 物品错位器
    WyvernItemDislocator(Mods.DraconicEvolution.ID, "magnet", 0),
    // 觉醒物品错位器
    AwakenedItemDislocator(Mods.DraconicEvolution.ID, "magnet", 1),
    // 反应堆核心
    ReactorCore(Mods.DraconicEvolution.ID, "reactorCore", 0),
    // 高级错位宝石
    TeleporterMKII(Mods.DraconicEvolution.ID, "teleporterMKII", 0),

    // 混沌注魔八重压缩太阳能
    ChaosInfusedOctupleSolarPanel(Mods.ElectroMagicTools.ID, "EMTSolars4", 14),
    // 风注魔八重压缩太阳能
    AirInfusedOctupleSolarPanel(Mods.ElectroMagicTools.ID, "EMTSolars4", 15),
    // 水注魔八重压缩太阳能
    WaterInfusedOctupleSolarPanel(Mods.ElectroMagicTools.ID, "EMTSolars5", 1),
    // 火注魔八重压缩太阳能
    FireInfusedOctupleSolarPanel(Mods.ElectroMagicTools.ID, "EMTSolars5", 2),
    // 能量源质发电机
    PotentiaEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 0),
    // 火之源质发电机
    FireEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 1),
    // 灵气源质发电机
    VitalEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 2),
    // 木之源质发电机
    WoodEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 3),
    // 风之源质发电机
    AirEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 4),
    // 贪婪源质发电机
    GreedEssentiaGenerator(Mods.ElectroMagicTools.ID, "EssentiaGenerators", 5),

    // 电容库
    CapacitorBank(Mods.EnderIO.ID, "blockCapBank", 0),
    // 玄钢砧
    DarkSteelAnvil(Mods.EnderIO.ID, "blockDarkSteelAnvil", 0),
    // 末影人头
    EndermanSkull(Mods.EnderIO.ID, "blockEndermanSkull", 0),
    // 禁锢末影人头
    TormentedEndermanHead(Mods.EnderIO.ID, "blockEndermanSkull", 2),
    // 种植站
    FarmingStation(Mods.EnderIO.ID, "blockFarmStation", 0),
    // 红石合金块
    RedstoneAlloyBlock(Mods.EnderIO.ID, "blockIngotStorage", 3),
    // 玄钢块
    DarkSteelBlock(Mods.EnderIO.ID, "blockIngotStorage", 6),
    // 电动刷怪笼
    PoweredSpawner(Mods.EnderIO.ID, "blockPoweredSpawner", 0),
    // 光伏板
    PhotovoltaicCell(Mods.EnderIO.ID, "blockSolarPanel", 0),
    // 高级光伏板
    AdvancedPhotovoltaicCell(Mods.EnderIO.ID, "blockSolarPanel", 1),
    // 脉冲光伏板
    VibrantPhotovoltaicCell(Mods.EnderIO.ID, "blockSolarPanel", 2),
    // 电容
    BasicCapacitor(Mods.EnderIO.ID, "itemBasicCapacitor", 0),
    // 僵尸电极
    ZombieElectrode(Mods.EnderIO.ID, "itemFrankenSkull", 0),
    // Z-逻辑控制器
    ZLogicController(Mods.EnderIO.ID, "itemFrankenSkull", 1),
    // 人造僵尸
    ArtificialZombie(Mods.EnderIO.ID, "itemFrankenSkull", 2),
    // 末影谐振器
    EnderResonator(Mods.EnderIO.ID, "itemFrankenSkull", 3),
    // 意识末影谐振器
    SentientEnderResonator(Mods.EnderIO.ID, "itemFrankenSkull", 4),
    // 守卫者二极管
    GuardianDiode(Mods.EnderIO.ID, "itemFrankenSkull", 6),
    // 预知晶体
    PrescientCrystal(Mods.EnderIO.ID, "itemMaterial", 13),
    // 脉冲晶体粉
    VibrantCrystalPowder(Mods.EnderIO.ID, "itemMaterial", 14),
    // 末影晶体粉
    EnderCrystalPowder(Mods.EnderIO.ID, "itemMaterial", 16),
    // 预知晶体粉
    PrescientCrystalPowder(Mods.EnderIO.ID, "itemMaterial", 17),
    // 脉冲晶体
    VibrantCrystal(Mods.EnderIO.ID, "itemMaterial", 5),
    // 末影晶体
    EnderCrystal(Mods.EnderIO.ID, "itemMaterial", 8),
    // 诱引晶体
    AttractorCrystal(Mods.EnderIO.ID, "itemMaterial", 9),
    // 能量导管
    PowerConduit(Mods.EnderIO.ID, "itemPowerConduit", 0),

    // 末影箱子
    EnderChest(Mods.EnderStorage.ID, "enderChest", 0),
    // 末影蓄水槽（末影储罐）
    EnderTank(Mods.EnderStorage.ID, "enderChest", 1),

    // 闪瞬奇点
    CombinedSingularity(Mods.EternalSingularity.ID, "combined_singularity", 0),
    // 圣灵奇点
    HolySingularity(Mods.EternalSingularity.ID, "combined_singularity", 1),
    // 静空奇点
    VoidSingularity(Mods.EternalSingularity.ID, "combined_singularity", 15),
    // 意面奇点
    SpaghettiSingularity(Mods.EternalSingularity.ID, "combined_singularity", 2),
    // 大气奇点
    AtmosphericSingularity(Mods.EternalSingularity.ID, "combined_singularity", 3),
    // 神秘奇点
    MysticSingularity(Mods.EternalSingularity.ID, "combined_singularity", 4),
    // 史诗奇点
    EpicSingularity(Mods.EternalSingularity.ID, "combined_singularity", 5),
    // 星耀奇点
    AstralSingularity(Mods.EternalSingularity.ID, "combined_singularity", 6),
    // 永恒奇点
    EternalSingularity(Mods.EternalSingularity.ID, "eternal_singularity", 0),

    // 黑石
    Blackstone(Mods.EtFuturumRequiem.ID, "blackstone", 0),
    // 蓝冰
    BlueIce(Mods.EtFuturumRequiem.ID, "blue_ice", 0),
    // 深板岩圆石
    CobbledDeepslate(Mods.EtFuturumRequiem.ID, "cobbled_deepslate", 0),
    // 深板岩
    Deepslate(Mods.EtFuturumRequiem.ID, "deepslate", 0),
    // 鞘翅
    Elytra(Mods.EtFuturumRequiem.ID, "elytra", 0),
    // 岩浆块
    MagmaBlock(Mods.EtFuturumRequiem.ID, "magma", 0),
    // 下界合金斧
    NetheriteAxe(Mods.EtFuturumRequiem.ID, "netherite_axe", 0),
    // 下界合金靴子
    NetheriteBoots(Mods.EtFuturumRequiem.ID, "netherite_boots", 0),
    // 下界合金胸甲
    NetheriteChestplate(Mods.EtFuturumRequiem.ID, "netherite_chestplate", 0),
    // 下界合金头盔
    NetheriteHelmet(Mods.EtFuturumRequiem.ID, "netherite_helmet", 0),
    // 下界合金锄
    NetheriteHoe(Mods.EtFuturumRequiem.ID, "netherite_hoe", 0),
    // 下界合金护腿
    NetheriteLeggings(Mods.EtFuturumRequiem.ID, "netherite_leggings", 0),
    // 下界合金镐
    NetheritePickaxe(Mods.EtFuturumRequiem.ID, "netherite_pickaxe", 0),
    // 下界合金碎片
    NetheriteScrap(Mods.EtFuturumRequiem.ID, "netherite_scrap", 0),
    // 下界合金锹
    NetheriteSpade(Mods.EtFuturumRequiem.ID, "netherite_spade", 0),
    // 下界合金剑
    NetheriteSword(Mods.EtFuturumRequiem.ID, "netherite_sword", 0),
    // 潜影壳
    ShulkerShell(Mods.EtFuturumRequiem.ID, "shulker_shell", 0),
    // 黏液块
    Slime(Mods.EtFuturumRequiem.ID, "slime", 0),
    // 灵魂火把
    SoulTorch(Mods.EtFuturumRequiem.ID, "soul_torch", 0),
    // 海绵
    EtFuturumRequiemSponge(Mods.EtFuturumRequiem.ID, "sponge", 0),
    // 湿海绵
    WetSponge(Mods.EtFuturumRequiem.ID, "sponge", 1),
    // 不死图腾
    TotemOfUndying(Mods.EtFuturumRequiem.ID, "totem_of_undying", 0),

    // 七重压缩圆石
    CompressedCobbleSeven(Mods.ExtraUtilities.ID, "cobblestone_compressed", 6),
    // 八重压缩圆石
    CompressedCobbleEight(Mods.ExtraUtilities.ID, "cobblestone_compressed", 7),
    // 漆黑之门
    DarkPortal(Mods.ExtraUtilities.ID, "dark_portal", 0),
    // 不稳定金属方块
    UnstableIngotBlock(Mods.ExtraUtilities.ID, "decorativeBlock1", 5),
    // 荧石玻璃
    GlowstoneGlass(Mods.ExtraUtilities.ID, "decorativeBlock2", 7),
    // 钻石锥刺
    DiamondSpike(Mods.ExtraUtilities.ID, "spike_base_diamond", 0),
    // 垃圾桶（流体）
    FluidTrashCan(Mods.ExtraUtilities.ID, "trashcan", 1),
    // 不稳定金属锭
    UnstableIngot(Mods.ExtraUtilities.ID, "unstableingot", 0),

    // 邪术气血宝珠
    EldritchOrb(Mods.ForbiddenMagic.ID, "EldritchOrb", 0),

    // 蜂箱组组件
    Alveary(Mods.Forestry.ID, "alveary", 0),
    // 蜂箱组克隆盒
    AlvearySwarmer(Mods.Forestry.ID, "alveary", 2),
    // 蜂箱组稳定器
    AlvearyStabilizer(Mods.Forestry.ID, "alveary", 6),
    // 蜂蜡
    Beeswax(Mods.Forestry.ID, "beeswax", 0),
    // 农场齿轮箱
    FarmGearbox(Mods.Forestry.ID, "ffarm", 2),
    // 农场水阀
    FarmValve(Mods.Forestry.ID, "ffarm", 4),
    // 农场控制盒
    FarmControl(Mods.Forestry.ID, "ffarm", 5),
    // 树叶
    Leaves(Mods.Forestry.ID, "leaves", 0),
    // 松树原木
    ForestryPineLog(Mods.Forestry.ID, "logs", 20),
    // 花粉
    Pollen(Mods.Forestry.ID, "pollen", 0),
    // 蜂王浆
    ForestryRoyalJelly(Mods.Forestry.ID, "royalJelly", 0),

    // 天域使魔
    EtherealFamiliar(Mods.Gadomancy.ID, "ItemEtherealFamiliar", 0),

    // 轻质合金板
    LightweightAlloyPlate(Mods.GalacticraftAmunRa.ID, "item.baseItem", 15),
    // 暗物质碎片
    DarkMatterFragment(Mods.GalacticraftAmunRa.ID, "item.baseItem", 26),
    // 穿梭机图纸
    ShuttleSchematic(Mods.GalacticraftAmunRa.ID, "item.schematic", 0),
    // 暗物质
    DarkMatter(Mods.GalacticraftAmunRa.ID, "tile.baseBlockRock", 14),

    // 高级晶圆
    AdvancedWafer(Mods.GalacticraftCore.ID, "item.basicItem", 14),
    // 无限氧气罐
    InfiniteOxygenTank(Mods.GalacticraftCore.ID, "item.infiniteOxygen", 0),
    // 2阶火箭图纸
    Tier2RocketSchematic(Mods.GalacticraftCore.ID, "item.schematic", 1),
    // 1阶火箭
    Tier1Rocket(Mods.GalacticraftCore.ID, "item.spaceship", 0),
    // NASA工作台
    NasaWorkbench(Mods.GalacticraftCore.ID, "tile.rocketWorkbench", 0),

    // 3阶火箭
    Tier3Rocket(Mods.GalacticraftMars.ID, "item.itemTier3Rocket", 0),
    // 3阶火箭图纸
    Tier3RocketSchematic(Mods.GalacticraftMars.ID, "item.schematic", 0),
    // 2阶火箭
    Tier2Rocket(Mods.GalacticraftMars.ID, "item.spaceshipTier2", 0),

    // 巴纳德C树木原木
    BarnardaCLog(Mods.GalaxySpace.ID, "barnardaClog", 0),
    // 4阶火箭控制电脑
    RocketControlComputerTier4(Mods.GalaxySpace.ID, "item.RocketControlComputer", 4),
    // 7阶火箭控制电脑
    RocketControlComputerTier7(Mods.GalaxySpace.ID, "item.RocketControlComputer", 7),
    // 4阶火箭图纸
    Tier4RocketSchematic(Mods.GalaxySpace.ID, "item.SchematicTier4", 0),
    // 5阶火箭图纸
    Tier5RocketSchematic(Mods.GalaxySpace.ID, "item.SchematicTier5", 0),
    // 6阶火箭图纸
    Tier6RocketSchematic(Mods.GalaxySpace.ID, "item.SchematicTier6", 0),
    // 7阶火箭图纸
    Tier7RocketSchematic(Mods.GalaxySpace.ID, "item.SchematicTier7", 0),
    // 8阶火箭图纸
    Tier8RocketSchematic(Mods.GalaxySpace.ID, "item.SchematicTier8", 0),
    // 4阶火箭
    Tier4Rocket(Mods.GalaxySpace.ID, "item.Tier4Rocket", 0),
    // 5阶火箭
    Tier5Rocket(Mods.GalaxySpace.ID, "item.Tier5Rocket", 0),
    // 6阶火箭
    Tier6Rocket(Mods.GalaxySpace.ID, "item.Tier6Rocket", 0),
    // 7阶火箭
    Tier7Rocket(Mods.GalaxySpace.ID, "item.Tier7Rocket", 0),
    // 8阶火箭
    Tier8Rocket(Mods.GalaxySpace.ID, "item.Tier8Rocket", 0),
    // 鲸鱼座T星E藻类（形态一）
    CetiESeaweedFormI(Mods.GalaxySpace.ID, "tcetiedandelions", 0),
    // 鲸鱼座T星E藻类（形态二）
    CetiESeaweedFormII(Mods.GalaxySpace.ID, "tcetiedandelions", 1),
    // 鲸鱼座T星E藻类（形态三）
    CetiESeaweedFormIII(Mods.GalaxySpace.ID, "tcetiedandelions", 2),
    // 鲸鱼座T星E藻类（形态四）
    CetiESeaweedFormIV(Mods.GalaxySpace.ID, "tcetiedandelions", 3),
    // 鲸鱼座T星E藻类（形态五）
    CetiESeaweedFormV(Mods.GalaxySpace.ID, "tcetiedandelions", 4),
    // 鲸鱼座T星E藻类（形态六）
    CetiESeaweedFormVI(Mods.GalaxySpace.ID, "tcetiedandelions", 5),

    // 铸件（一次性锉）
    SingleUseFileMold(Mods.GGFab.ID, "gt.ggfab.d1", 30),
    // 铸件（一次性扳手）
    SingleUseWrenchMold(Mods.GGFab.ID, "gt.ggfab.d1", 31),
    // 铸件（一次性撬棍）
    SingleUseCrowbarMold(Mods.GGFab.ID, "gt.ggfab.d1", 32),
    // 铸件（一次性剪线钳）
    SingleUseWireCutterMold(Mods.GGFab.ID, "gt.ggfab.d1", 33),
    // 铸件（一次性锻造锤）
    SingleUseHardHammerMold(Mods.GGFab.ID, "gt.ggfab.d1", 34),
    // 铸件（一次性软锤）
    SingleUseSoftMalletMold(Mods.GGFab.ID, "gt.ggfab.d1", 35),
    // 铸件（一次性螺丝刀）
    SingleUseScrewdriverMold(Mods.GGFab.ID, "gt.ggfab.d1", 36),
    // 铸件（一次性锯子）
    SingleUseSawMold(Mods.GGFab.ID, "gt.ggfab.d1", 37),

    // 防辐射板
    RadiationProtectionPlate(Mods.GoodGenerator.ID, "radiationProtectionPlate"),

    // 喷射引擎
    JetEngine(Mods.GraviSuite.ID, "itemSimpleItem", 6),

    // 脱水线圈 []
    DehydratorCoil(Mods.GTPlusPlus.ID, "itemDehydratorCoil", 3),
    // 能量核心 [UHV]
    BufferCoreUHV(Mods.GTPlusPlus.ID, "item.itemBufferCore10", 0),

    // 末影粉末
    EndPowder(Mods.HardcoreEnderExpansion.ID, "end_powder", 0),
    // 末影粉末矿石
    EndPowderOre(Mods.HardcoreEnderExpansion.ID, "end_powder_ore", 0),

    // 低压变压器
    LVTransformer(Mods.IndustrialCraft2.ID, "blockElectric", 3),
    // 中压变压器
    MVTransformer(Mods.IndustrialCraft2.ID, "blockElectric", 4),
    // 高压变压器
    HVTransformer(Mods.IndustrialCraft2.ID, "blockElectric", 5),
    // 超高压变压器
    EVTransformer(Mods.IndustrialCraft2.ID, "blockElectric", 6),
    // 合金锭
    MixedMetalIngot(Mods.IndustrialCraft2.ID, "mixedMetalIngot", false, true),
    // 核反应堆
    NuclearReactor(Mods.IndustrialCraft2.ID, "blockGenerator", 5),
    // 工业TNT
    IndustrialTnt(Mods.IndustrialCraft2.ID, "blockITNT", 0),
    // 铁炉
    IronFurnace(Mods.IndustrialCraft2.ID, "blockMachine", 1),
    // 核弹
    Nuke(Mods.IndustrialCraft2.ID, "blockNuke", 0),
    // 核反应仓
    ReactorChamber(Mods.IndustrialCraft2.ID, "blockReactorChamber", OreDictionary.WILDCARD_VALUE),
    // 橡胶树原木
    RubberLog(Mods.IndustrialCraft2.ID, "blockRubWood", 0),
    // 能量水晶
    EnergyCrystal(Mods.IndustrialCraft2.ID, "itemBatCrystal", 26),
    // 兰波顿水晶
    LapotronCrystal(Mods.IndustrialCraft2.ID, "itemBatLamaCrystal", 26),
    // 空单元
    EmptyCell(Mods.IndustrialCraft2.ID, "itemCellEmpty", 13),
    // 粘性树脂
    StickyResin(Mods.IndustrialCraft2.ID, "itemHarz", 0),
    // 生碳纤维
    RawCarbonFibre(Mods.IndustrialCraft2.ID, "itemPartCarbonFibre", 0),
    // 电路板
    ElectronicCircuit(Mods.IndustrialCraft2.ID, "itemPartCircuit", 0),
    // 高级电路板
    AdvancedCircuit(Mods.IndustrialCraft2.ID, "itemPartCircuitAdv", 0),
    // 加厚中子反射板
    ThickNeutronReflector(Mods.IndustrialCraft2.ID, "reactorReflectorThick", 1),

    // 铁箱子
    IronChest(Mods.IronChests.ID, "BlockIronChest", 0),
    // 金箱子
    GoldChest(Mods.IronChests.ID, "BlockIronChest", 1),
    // 钻石箱子
    DiamondChest(Mods.IronChests.ID, "BlockIronChest", 2),
    // 铜箱子
    CopperChest(Mods.IronChests.ID, "BlockIronChest", 3),
    // 钢箱子
    SteelChest(Mods.IronChests.ID, "BlockIronChest", 4),
    // 水晶箱子
    CrystalChest(Mods.IronChests.ID, "BlockIronChest", 5),
    // 黑曜石箱子
    ObsidianChest(Mods.IronChests.ID, "BlockIronChest", 6),
    // 下界合金箱子
    NetheriteChest(Mods.IronChests.ID, "BlockIronChest", 8),
    // 玄钢箱子
    DarkSteelChest(Mods.IronChests.ID, "BlockIronChest", 9),

    // 钻石储罐
    DiamondTank(Mods.IronTanks.ID, "diamondTank", 0),

    // 寻矿魔杖
    IfuBuildingKit(Mods.IWillFindYou.ID, "ifu_buildingKit", 0),

    // 兰波顿机械方块/电容
    LapotronicEnergyUnitBlock(Mods.KekzTech.ID, "kekztech_lapotronicenergyunit_block", 0),

    // 穿刺箭
    PiercingArrow(Mods.MineAndBladeBattleGear2.ID, "mb.arrow", 3),

    // 黑云
    BlackCloud(Mods.Natura.ID, "Cloud", 1),
    // 灰云
    GrayCloud(Mods.Natura.ID, "Cloud", 2),
    // 硫云
    SulfurCloud(Mods.Natura.ID, "Cloud", 3),

    // 主世界
    DimensionOverworld(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ow"),
    // 下界
    DimensionNether(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ne"),
    // 暮色森林
    DimensionTwilight(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_TF"),
    // 末地
    DimensionEnd(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_ED"),
    // 末地小行星
    DimensionEndAsteroids(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_EA"),
    // 永恒湿地
    DimensionEverglades(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Eg"),
    // 月球
    DimensionMoon(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Mo"),
    // 火卫二
    DimensionDeimos(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_De"),
    // 火星
    DimensionMars(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ma"),
    // 火卫一
    DimensionPhobos(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ph"),
    // 小行星带
    DimensionAsteroids(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_As"),
    // 木卫四
    DimensionCallisto(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ca"),
    // 谷神星
    DimensionCeres(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ce"),
    // 木卫二
    DimensionEuropa(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Eu"),
    // 木卫三
    DimensionGanymede(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ga"),
    // 罗斯128b
    DimensionRoss128b(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Rb"),
    // 木卫一
    DimensionIo(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Io"),
    // 水星
    DimensionMercury(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Me"),
    // 金星
    DimensionVenus(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ve"),
    // 土卫二
    DimensionEnceladus(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_En"),
    // 天卫五
    DimensionMiranda(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Mi"),
    // 天卫四
    DimensionOberon(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ob"),
    // 土卫六
    DimensionTitan(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ti"),
    // 罗斯128ba
    DimensionRoss128ba(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ra"),
    // 海卫八
    DimensionProteus(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Pr"),
    // 海卫一
    DimensionTriton(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Tr"),
    // 妊神星
    DimensionHaumea(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ha"),
    // 柯伊伯带
    DimensionKuiperBelt(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_KB"),
    // 鸟神星
    DimensionMakeMake(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_MM"),
    // 冥王星
    DimensionPluto(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Pl"),
    // 巴纳德C
    DimensionBarnardC(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_BC"),
    // 巴纳德E
    DimensionBarnardE(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_BE"),
    // 巴纳德F
    DimensionBarnardF(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_BF"),
    // 半人马Bb
    DimensionCentauriBb(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_CB"),
    // 鲸鱼座T星E
    DimensionTauCetiE(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_TE"),
    // 织女B
    DimensionVegaB(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_VB"),
    // 阿努比斯
    DimensionAnubis(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_An"),
    // 荷鲁斯
    DimensionHorus(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Ho"),
    // 马赫斯
    DimensionMaahes(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Mh"),
    // 迈罕带
    DimensionMehenBelt(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_MB"),
    // 奈佩里
    DimensionNeper(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Np"),
    // 赛特
    DimensionSeth(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_Se"),
    // 漆黑世界
    DimensionDeepDark(Mods.NEIOrePlugin.ID, "blockDimensionDisplay_DD"),

    // 强化玻璃透镜
    ReinforcedGlassLens(Mods.NewHorizonsCoreMod.ID, "ReinforcedGlassLense"),
    // 神秘水晶透镜
    MysteriousCrystalLens(Mods.NewHorizonsCoreMod.ID, "MysteriousCrystalLens"),
    // 拉多克斯聚合物透镜
    RadoxPolymerLens(Mods.NewHorizonsCoreMod.ID, "RadoxPolymerLens"),
    // 彩色透镜
    ChromaticLens(Mods.NewHorizonsCoreMod.ID, "ChromaticLens"),
    // 琼脂
    Agar(Mods.NewHorizonsCoreMod.ID, "GTNHBioItems", 2),

    // 电梯
    Elevator(Mods.OpenBlocks.ID, "elevator", 0),
    // 海绵
    OpenBlocksSponge(Mods.OpenBlocks.ID, "sponge", 0),

    // T3加速处理器（APU）
    Tier3APU(Mods.OpenComputers.ID, "item", 103),
    // 创造模式组件总线
    CreativeComponentBus(Mods.OpenComputers.ID, "item", 114),
    // T4服务器
    Tier4Server(Mods.OpenComputers.ID, "item", 69),
    // T3微控制器外壳
    Tier3MicrocontrollerCase(Mods.OpenComputers.ID, "item", 90),
    // T3无人机外壳
    Tier3DroneCase(Mods.OpenComputers.ID, "item", 91),
    // T3平板电脑外壳
    Tier3TabletCase(Mods.OpenComputers.ID, "item", 93),

    // 生鳀鱼
    RawAnchovy(Mods.PamsHarvestCraft.ID, "anchovyrawItem", 0),
    // 生鲈鱼
    RawBass(Mods.PamsHarvestCraft.ID, "bassrawItem", 0),
    // 生鱿鱼
    RawCalamari(Mods.PamsHarvestCraft.ID, "calamarirawItem", 0),
    // 生鲤鱼
    RawCarp(Mods.PamsHarvestCraft.ID, "carprawItem", 0),
    // 生鲶鱼
    RawCatfish(Mods.PamsHarvestCraft.ID, "catfishrawItem", 0),
    // 生嘉鱼
    RawCharr(Mods.PamsHarvestCraft.ID, "charrrawItem", 0),
    // 生蛤蜊
    RawClam(Mods.PamsHarvestCraft.ID, "clamrawItem", 0),
    // 生螃蟹
    RawCrab(Mods.PamsHarvestCraft.ID, "crabrawItem", 0),
    // 蔓越莓
    Cranberry(Mods.PamsHarvestCraft.ID, "cranberryItem", 0),
    // 生龙虾
    RawCrayfish(Mods.PamsHarvestCraft.ID, "crayfishrawItem", 0),
    // 生鳗鱼
    RawEel(Mods.PamsHarvestCraft.ID, "eelrawItem", 0),
    // 生青蛙
    RawFrog(Mods.PamsHarvestCraft.ID, "frograwItem", 0),
    // 绿心鱼
    GreenHeartFish(Mods.PamsHarvestCraft.ID, "greenheartfishItem", 0),
    // 生石斑鱼
    RawGrouper(Mods.PamsHarvestCraft.ID, "grouperrawItem", 0),
    // 生青鱼
    RawHerring(Mods.PamsHarvestCraft.ID, "herringrawItem", 0),
    // 香辣鸡翅
    HotWings(Mods.PamsHarvestCraft.ID, "hotwingsItem", 0),
    // 生海蜇
    RawJellyfish(Mods.PamsHarvestCraft.ID, "jellyfishrawItem", 0),
    // 生泥鱼
    RawMudfish(Mods.PamsHarvestCraft.ID, "mudfishrawItem", 0),
    // 生章鱼
    RawOctopus(Mods.PamsHarvestCraft.ID, "octopusrawItem", 0),
    // 生鲈鱼
    RawPerch(Mods.PamsHarvestCraft.ID, "perchrawItem", 0),
    // 水稻
    Rice(Mods.PamsHarvestCraft.ID, "riceItem", 0),
    // 蜂王浆
    PamsHarvestCraftRoyalJelly(Mods.PamsHarvestCraft.ID, "royaljellyItem", 0),
    // 生扇贝
    RawScallop(Mods.PamsHarvestCraft.ID, "scalloprawItem", 0),
    // 海带
    Seaweed(Mods.PamsHarvestCraft.ID, "seaweedItem", 0),
    // 生虾
    RawShrimp(Mods.PamsHarvestCraft.ID, "shrimprawItem", 0),
    // 生蜗牛
    RawSnail(Mods.PamsHarvestCraft.ID, "snailrawItem", 0),
    // 生鲷鱼
    RawSnapper(Mods.PamsHarvestCraft.ID, "snapperrawItem", 0),
    // 生罗非鱼
    RawTilapia(Mods.PamsHarvestCraft.ID, "tilapiarawItem", 0),
    // 生鳟鱼
    RawTrout(Mods.PamsHarvestCraft.ID, "troutrawItem", 0),
    // 生金枪鱼
    RawTuna(Mods.PamsHarvestCraft.ID, "tunarawItem", 0),
    // 生海龟
    RawTurtle(Mods.PamsHarvestCraft.ID, "turtlerawItem", 0),
    // 生碧古鱼
    RawWalleye(Mods.PamsHarvestCraft.ID, "walleyerawItem", 0),
    // 荸荠
    WaterChestnut(Mods.PamsHarvestCraft.ID, "waterchestnutItem", 0),

    // 创造模式IC芯片
    CreativeICChip(Mods.ProjectRedFabrication.ID, "projectred.fabrication.icchip", 1),

    // 普通轨道
    Track(Mods.Railcraft.ID, "track", 0),
    TrackLegacyDamage736(Mods.Railcraft.ID, "track", 736),
    TrackLegacyDamage816(Mods.Railcraft.ID, "track", 816),
    // 集水器壁板
    WaterTankWall(Mods.Railcraft.ID, "machine.alpha", 14),
    // 高级焦炉砖块
    AdvancedCokeOvenBrick(Mods.Railcraft.ID, "machine.alpha", 12),
    // 民科蒸汽引擎
    HobbyistSteamEngine(Mods.Railcraft.ID, "machine.beta", 7),

    // 沃土
    FertilizedDirt(Mods.RandomThings.ID, "fertilizedDirt", 0),
    // 灵气
    Spirit(Mods.RandomThings.ID, "ingredient", 3),

    // 吊炸天电容
    Ic2Capacitor(Mods.SGCraft.ID, "ic2Capacitor", 0),
    // RF星门能量单元
    RfPowerUnit(Mods.SGCraft.ID, "rfPowerUnit", 0),
    // 星门导标升级
    StargateChevronUpgrade(Mods.SGCraft.ID, "sgChevronUpgrade", 0),
    // 星门控制水晶
    StargateControllerCrystal(Mods.SGCraft.ID, "sgControllerCrystal", 0),
    // 星门核心水晶
    StargateCoreCrystal(Mods.SGCraft.ID, "sgCoreCrystal", 0),
    // 星门虹膜升级
    StargateIrisUpgrade(Mods.SGCraft.ID, "sgIrisUpgrade", 0),
    // 星门底座方块
    StargateBase(Mods.SGCraft.ID, "stargateBase", 0),
    // 星门控制器
    StargateController(Mods.SGCraft.ID, "stargateController", 0),
    // 星门外环段
    StargateRing(Mods.SGCraft.ID, "stargateRing", 0),
    // 星门导标方块
    StargateChevronBlock(Mods.SGCraft.ID, "stargateRing", 1),

    // 标准车壳
    StandardHull(Mods.StevesCarts2.ID, "CartModule", 38),
    // 无尽引擎
    InfinityEngine(Mods.StevesCarts2.ID, "CartModule", 61),
    // 升级：创造模式
    CreativeUpgrade(Mods.StevesCarts2.ID, "upgrade", 14),

    // 抽屉管理器
    DrawerController(Mods.StorageDrawers.ID, "controller", 0),
    // 抽屉容量升级（II）
    CapacityUpgradeII(Mods.StorageDrawers.ID, "upgrade", 2),
    // 抽屉容量升级（III）
    CapacityUpgradeIII(Mods.StorageDrawers.ID, "upgrade", 3),
    // 抽屉容量升级（IV）
    CapacityUpgradeIV(Mods.StorageDrawers.ID, "upgrade", 4),
    // 抽屉容量升级（V）
    CapacityUpgradeV(Mods.StorageDrawers.ID, "upgrade", 5),
    // 抽屉容量升级（VI）
    CapacityUpgradeVI(Mods.StorageDrawers.ID, "upgrade", 6),
    // 抽屉容量升级（VII）
    CapacityUpgradeVII(Mods.StorageDrawers.ID, "upgrade", 7),
    // 抽屉容量升级（VIII）
    CapacityUpgradeVIII(Mods.StorageDrawers.ID, "upgrade", 8),
    // 升级模板
    UpgradeTemplate(Mods.StorageDrawers.ID, "upgradeTemplate", 0),

    // 多方块机器全息投影仪
    StructureHologram(Mods.StructureLib.ID, "item.structurelib.constructableTrigger", 0),

    // 法杖核心:时间
    FocusTime(Mods.TaintedMagic.ID, "ItemFocusTime", 0),

    // 法杖核心:元始
    FocusPrimal(Mods.Thaumcraft.ID, "FocusPrimal", 0),
    // 法杖核心:守护
    FocusWarding(Mods.Thaumcraft.ID, "FocusWarding", 0),
    // 凡人护身符
    MundaneAmulet(Mods.Thaumcraft.ID, "ItemBaubleBlanks", 0),
    // 凡人指环
    MundaneRing(Mods.Thaumcraft.ID, "ItemBaubleBlanks", 1),
    // 元始珍珠
    PrimordialPearl(Mods.Thaumcraft.ID, "ItemEldritchObject", 3),
    // 白色油脂蜡烛
    WhiteTallowCandle(Mods.Thaumcraft.ID, "blockCandle", 0),
    // 风之魔晶
    AirCrystal(Mods.Thaumcraft.ID, "blockCrystal", 0),
    // 火之魔晶
    FireCrystal(Mods.Thaumcraft.ID, "blockCrystal", 1),
    // 水之魔晶
    WaterCrystal(Mods.Thaumcraft.ID, "blockCrystal", 2),
    // 复相魔晶
    MixedCrystal(Mods.Thaumcraft.ID, "blockCrystal", 6),
    // 符文矩阵
    RunicMatrix(Mods.Thaumcraft.ID, "blockStoneDevice", 2),
    // 奥术工作台
    ArcaneWorkbench(Mods.Thaumcraft.ID, "blockTable", 15),

    // 镶金黑曜石
    GildedObsidian(Mods.ThaumicBases.ID, "eldritchArk", 0),
    // 彩虹仙人掌
    RainbowCactus(Mods.ThaumicBases.ID, "rainbowCactus", 0),
    // 奥术左轮枪
    Revolver(Mods.ThaumicBases.ID, "revolver", 0),

    // 魔导源质存储元件
    EssentiaStorageCell(Mods.ThaumicEnergistics.ID, "storage.essentia", 4),
    // 奥术装配室
    ArcaneAssembler(Mods.ThaumicEnergistics.ID, "thaumicenergistics.block.arcane.assembler", 0),

    // 炼狱之壶
    EverburnUrn(Mods.ThaumicExploration.ID, "everburnUrn", 0),

    // 觉醒灵宝镐
    IchorPickGem(Mods.ThaumicTinkerer.ID, "ichorPickGem", 0),

    // 钴矿石
    CobaltOre(Mods.TinkerConstruct.ID, "SearedBrick", 1),
    // 阿迪特矿石
    ArditeOre(Mods.TinkerConstruct.ID, "SearedBrick", 2),
    // 合成站
    CraftingStation(Mods.TinkerConstruct.ID, "CraftingStation", 0),
    // 史莱姆水晶
    SlimeCrystal(Mods.TinkerConstruct.ID, "materials", 1),
    // 蓝色史莱姆水晶
    BlueSlimeCrystal(Mods.TinkerConstruct.ID, "materials", 17),
    // 凝固史莱姆块
    SlimeGel(Mods.TinkerConstruct.ID, "slime.gel", 0),
    // 弹跳板
    SlimePad(Mods.TinkerConstruct.ID, "slime.pad", 0),

    // 雪人首领毛皮
    AlphaFur(Mods.TwilightForest.ID, "item.alphaFur", 0),
    // 极地毛皮
    ArcticFur(Mods.TwilightForest.ID, "item.arcticFur", 0),
    // 砷铅铁
    Carminite(Mods.TwilightForest.ID, "item.carminite", 0),
    // 保管符咒 III
    CharmOfKeepingIII(Mods.TwilightForest.ID, "item.charmOfKeeping3", 0),
    // 生命符咒 II
    CharmOfLifeII(Mods.TwilightForest.ID, "item.charmOfLife2", 0),
    // 粉碎号角
    CrumbleHorn(Mods.TwilightForest.ID, "item.crumbleHorn", 0),
    // 炽热的血液
    FieryBlood(Mods.TwilightForest.ID, "item.fieryBlood", 0),
    // 炽热的泪
    FieryTears(Mods.TwilightForest.ID, "item.fieryTears", 0),
    // 巨人的镐
    GiantPick(Mods.TwilightForest.ID, "item.giantPick", 0),
    // 巨人的剑
    GiantSword(Mods.TwilightForest.ID, "item.giantSword", 0),
    // 九头蛇肉排
    HydraChop(Mods.TwilightForest.ID, "item.hydraChop", 0),
    // 寒冰炸弹
    IceBomb(Mods.TwilightForest.ID, "item.iceBomb", 0),
    // 铁木锭
    IronwoodIngot(Mods.TwilightForest.ID, "item.ironwoodIngot", 0),
    // 骑士金属锭
    KnightmetalIngot(Mods.TwilightForest.ID, "item.knightMetal", 0),
    // 灰烬烧灯
    LampOfCinders(Mods.TwilightForest.ID, "item.lampOfCinders", 0),
    // 魔豆
    MagicBeans(Mods.TwilightForest.ID, "item.magicBeans", 0),
    // 魔法地图核心
    MagicMapFocus(Mods.TwilightForest.ID, "item.magicMapFocus", 0),
    // 迷宫地图核心
    MazeMapFocus(Mods.TwilightForest.ID, "item.mazeMapFocus", 0),
    // 迷宫破坏者
    MazebreakerPick(Mods.TwilightForest.ID, "item.mazebreakerPick", 0),
    // 牛头人肉排
    MeefSteak(Mods.TwilightForest.ID, "item.meefSteak", 0),
    // 牛头人沙拉酱肉
    MeefStroganoff(Mods.TwilightForest.ID, "item.meefStroganoff", 0),
    // 娜迦鳞片
    NagaScale(Mods.TwilightForest.ID, "item.nagaScale", 0),
    // 幻影头盔
    PhantomHelm(Mods.TwilightForest.ID, "item.phantomHelm", 0),
    // 幻影胸甲
    PhantomPlate(Mods.TwilightForest.ID, "item.phantomPlate", 0),
    // 吸血权杖
    ScepterLifeDrain(Mods.TwilightForest.ID, "item.scepterLifeDrain", 0),
    // 黄昏权杖
    ScepterTwilight(Mods.TwilightForest.ID, "item.scepterTwilight", 0),
    // 僵尸权杖
    ScepterZombie(Mods.TwilightForest.ID, "item.scepterZombie", 0),
    // 钢叶
    SteeleafIngot(Mods.TwilightForest.ID, "item.steeleafIngot", 0),
    // 三发弓
    TripleBow(Mods.TwilightForest.ID, "item.tripleBow", 0),
    // 九头蛇战利品
    HydraTrophy(Mods.TwilightForest.ID, "item.trophy", 0),
    // 娜迦战利品
    NagaTrophy(Mods.TwilightForest.ID, "item.trophy", 1),
    // 巫妖战利品
    LichTrophy(Mods.TwilightForest.ID, "item.trophy", 2),
    // 暮色恶魂战利品
    UrGhastTrophy(Mods.TwilightForest.ID, "item.trophy", 3),
    // 冰雪女王战利品
    SnowQueenTrophy(Mods.TwilightForest.ID, "item.trophy", 4),
    // 米诺陶战利品
    MinoshroomTrophy(Mods.TwilightForest.ID, "item.trophy", 5),
    // 幻影骑士战利品
    KnightPhantomTrophy(Mods.TwilightForest.ID, "item.trophy", 6),
    // 雪人首领战利品
    AlphaYetiTrophy(Mods.TwilightForest.ID, "item.trophy", 7),
    // 谜题羊战利品
    QuestingRamTrophy(Mods.TwilightForest.ID, "item.trophy", 8),
    // 极光柱
    AuroraPillar(Mods.TwilightForest.ID, "tile.AuroraPillar", 0),
    // 蓬松的云
    FluffyCloud(Mods.TwilightForest.ID, "tile.FluffyCloud", 0),
    // 巨型圆石
    GiantCobble(Mods.TwilightForest.ID, "tile.GiantCobble", 0),
    // 巨型原木
    GiantLog(Mods.TwilightForest.ID, "tile.GiantLog", 0),
    // 巨型黑曜石
    GiantObsidian(Mods.TwilightForest.ID, "tile.GiantObsidian", 0),
    // 巨型暮色森林蘑菇
    HugeGloomBlock(Mods.TwilightForest.ID, "tile.HugeGloomBlock", 0),
    // 巨型荷叶
    HugeLilyPad(Mods.TwilightForest.ID, "tile.HugeLilyPad", 0),
    // 巨大的茎
    HugeStalk(Mods.TwilightForest.ID, "tile.HugeStalk", 0),
    // 极光方块
    AuroraBrick(Mods.TwilightForest.ID, "tile.TFAuroraBrick", 0),
    // 暮色橡树原木
    TwilightOakLog(Mods.TwilightForest.ID, "tile.TFLog", 0),
    // 时光树的时钟
    TreeOfTimeClock(Mods.TwilightForest.ID, "tile.TFMagicLogSpecial", 0),
    // 时光树树苗
    TreeOfTimeSapling(Mods.TwilightForest.ID, "tile.TFSapling", 5),
    // 螺旋纹石砖
    SpiralBricks(Mods.TwilightForest.ID, "tile.TFSpiralBricks", 0),
    // 重现方块
    ReappearingBlock(Mods.TwilightForest.ID, "tile.TFTowerDevice", 0),
    // 消失方块
    VanishingBlock(Mods.TwilightForest.ID, "tile.TFTowerDevice", 2),
    // 飘渺的云
    WispyCloud(Mods.TwilightForest.ID, "tile.WispyCloud", 0),

    // 导电铁奇点
    USConductiveIronSingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 0),
    // 磁钢奇点
    USElectricalSteelSingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 1),
    // 充能合金奇点
    USEnergeticAlloySingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 2),
    // 玄钢奇点
    USDarkSteelSingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 3),
    // 脉动铁奇点
    USPulsatingIronSingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 4),
    // 红石合金奇点
    USRedstoneAlloySingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 5),
    // 魂金奇点
    USSoulariumSingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 6),
    // 脉冲合金奇点
    USVibrantAlloySingularity(Mods.UniversalSingularities.ID, "universal.enderIO.singularity", 7),
    // 不稳定金属奇点
    USUnstableSingularity(Mods.UniversalSingularities.ID, "universal.extraUtilities.singularity", 0),
    // 铝奇点
    USAluminumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 0),
    // 黄铜奇点
    USBrassSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 1),
    // 蓝宝石奇点
    USSapphireSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 10),
    // 钢奇点
    USSteelSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 11),
    // 钛奇点
    USTitaniumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 12),
    // 钨奇点
    USTungstenSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 13),
    // 铀奇点
    USUraniumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 14),
    // 锌奇点
    USZincSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 15),
    // 磷酸三钙奇点
    USTricalciumPhosphateSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 16),
    // 钯奇点
    USPalladiumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 17),
    // 大马士革钢奇点
    USDamascusSteelSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 18),
    // 黑钢奇点
    USBlackSteelSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 19),
    // 青铜奇点
    USBronzeSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 2),
    // 流体琥珀金奇点
    USElectrumFluxSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 20),
    // 水银奇点
    USQuicksilverSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 21),
    // 暗影钢奇点
    USShadowSteelSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 22),
    // 铱奇点
    USIridiumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 23),
    // 下界之星奇点
    USNetherStarSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 24),
    // 铂奇点
    USPlatinumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 25),
    // 超能硅岩奇点
    USNaquadriaSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 26),
    // 钚奇点
    USPlutoniumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 27),
    // 陨铁奇点
    USMeteoricIronSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 28),
    // 戴斯奇点
    USDeshSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 29),
    // 木炭奇点
    USCharcoalSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 3),
    // 铕奇点
    USEuropiumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 30),
    // 脉石奇点
    USGangueSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 31),
    // 琥珀金奇点
    USElectrumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 4),
    // 殷钢奇点
    USInvarSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 5),
    // 镁奇点
    USMagnesiumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 6),
    // 锇奇点
    USOsmiumSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 7),
    // 橄榄石奇点
    USPeridotSingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 8),
    // 红宝石奇点
    USRubySingularity(Mods.UniversalSingularities.ID, "universal.general.singularity", 9),
    // 蓝石奇点
    USElectrotineSingularity(Mods.UniversalSingularities.ID, "universal.projectRed.singularity", 0),
    // 耐酸铝奇点
    USAlumiteSingularity(Mods.UniversalSingularities.ID, "universal.tinkersConstruct.singularity", 1),
    // 阿迪特奇点
    USArditeSingularity(Mods.UniversalSingularities.ID, "universal.tinkersConstruct.singularity", 2),
    // 钴奇点
    USCobaltSingularity(Mods.UniversalSingularities.ID, "universal.tinkersConstruct.singularity", 3),
    // 末影奇点
    USEnderSingularity(Mods.UniversalSingularities.ID, "universal.tinkersConstruct.singularity", 4),
    // 玛玉灵奇点
    USManyullynSingularity(Mods.UniversalSingularities.ID, "universal.tinkersConstruct.singularity", 6),
    // 煤炭奇点
    USCoalSingularity(Mods.UniversalSingularities.ID, "universal.vanilla.singularity", 0),
    // 绿宝石奇点
    USEmeraldSingularity(Mods.UniversalSingularities.ID, "universal.vanilla.singularity", 1),
    // 钻石奇点
    USDiamondSingularity(Mods.UniversalSingularities.ID, "universal.vanilla.singularity", 2),

    // 无限之蛋
    InfinityEgg(Mods.Witchery.ID, "infinityegg", 0),
    // 木灰
    WoodAsh(Mods.Witchery.ID, "ingredient", 18),
    // 水心之酿
    WaterArtichokeBrew(Mods.Witchery.ID, "ingredient", 96),

    // 奥术计算器
    ArcaneCalculator(Mods.WitchingGadgets.ID, "item.WG_Material", 7);

    private final String modId;
    private final String registryName;
    private final int metadata;
    private final boolean forgeRegistry;
    private final boolean ic2NamedItem;

    ModsItemlist(String modId, String registryName) {
        this(modId, registryName, 0);
    }

    ModsItemlist(String modId, String registryName, int metadata) {
        this(modId, registryName, metadata, false, false);
    }

    ModsItemlist(String modId, String registryName, boolean forgeRegistry) {
        this(modId, registryName, 0, forgeRegistry, false);
    }

    ModsItemlist(String modId, String registryName, boolean forgeRegistry, boolean ic2NamedItem) {
        this(modId, registryName, 0, forgeRegistry, ic2NamedItem);
    }

    ModsItemlist(String modId, String registryName, int metadata, boolean forgeRegistry, boolean ic2NamedItem) {
        this.modId = modId;
        this.registryName = registryName;
        this.metadata = metadata;
        this.forgeRegistry = forgeRegistry;
        this.ic2NamedItem = ic2NamedItem;
    }

    public ItemStack get(long amount) {
        if (ic2NamedItem) {
            ItemStack stack = IC2Items.getItem(registryName);
            if (stack == null) return null;
            ItemStack copy = stack.copy();
            copy.stackSize = (int) amount;
            return copy;
        }
        if (forgeRegistry) return GameRegistry.findItemStack(modId, registryName, (int) amount);
        return GTModHandler.getModItem(modId, registryName, amount, metadata);
    }
}
