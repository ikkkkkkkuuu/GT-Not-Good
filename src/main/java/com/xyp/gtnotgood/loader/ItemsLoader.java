package com.xyp.gtnotgood.loader;

import com.xyp.gtnotgood.common.items.VeinMiningPickaxe.VeinMiningPickaxe;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternItem;
import com.xyp.gtnotgood.common.mebridge.ItemMEWirelessTransceiver;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers ordinary Forge items owned by GT Not Good.
 */
public final class ItemsLoader {

    public static ItemMEWirelessTransceiver meWirelessTransceiver;
    public static WildcardPatternItem wildcardPattern;
    public static VeinMiningPickaxe veinMiningPickaxe;

    private ItemsLoader() {}

    /**
     * Registers all non-GregTech items and stores their stacks for recipes and tooltips.
     */
    public static void registry() {
        var connector = new com.xyp.gtnotgood.common.packaged.ItemWirelessConnector();
        GameRegistry.registerItem(connector, "packaged_wireless_connector");
        GTNGItemList.ItemWirelessConnector.set(connector);
        var basicCore = new com.xyp.gtnotgood.common.packaged.ItemPackagedCore(false);
        GameRegistry.registerItem(basicCore, "basic_packaged_core");
        GTNGItemList.BasicPackagedCore.set(basicCore);
        if (com.xyp.gtnotgood.utils.enums.ModList.BloodMagic.isModLoaded()) {
            // #tr item.blood_altar_packaged_core.name
            // # Blood Altar Packaged Core
            // # zh_CN 血魔法祭坛封包核心
            var bloodCore = new com.xyp.gtnotgood.common.packaged.ItemPackagedCore(
                "blood_altar",
                "blood_altar_packaged_core");
            GameRegistry.registerItem(bloodCore, "blood_altar_packaged_core");
            GTNGItemList.BloodAltarCore.set(bloodCore);
            com.xyp.gtnotgood.common.packaged.PackagedCoreRegistry
                .register("blood_altar", new com.xyp.gtnotgood.common.packaged.BloodAltarAdapter());
        }
        // #tr item.assembly_line_packaged_core.name
        // # Assembly Line Packaged Core
        // # zh_CN 装配线封包核心
        var assemblyCore = new com.xyp.gtnotgood.common.packaged.ItemPackagedCore(
            "assembly_line",
            "assembly_line_packaged_core");
        GameRegistry.registerItem(assemblyCore, "assembly_line_packaged_core");
        GTNGItemList.AssemblyLineCore.set(assemblyCore);
        // #tr item.advanced_assembly_line_packaged_core.name
        // # Advanced Assembly Line Packaged Core
        // # zh_CN 进阶装配线封包核心
        var advancedCore = new com.xyp.gtnotgood.common.packaged.ItemPackagedCore(
            "advanced_assembly_line",
            "advanced_assembly_line_packaged_core");
        GameRegistry.registerItem(advancedCore, "advanced_assembly_line_packaged_core");
        GTNGItemList.AdvancedAssemblyLineCore.set(advancedCore);
        com.xyp.gtnotgood.common.packaged.PackagedCoreRegistry
            .register("assembly_line", new com.xyp.gtnotgood.common.packaged.AssemblyLineAdapter(false));
        com.xyp.gtnotgood.common.packaged.PackagedCoreRegistry
            .register("advanced_assembly_line", new com.xyp.gtnotgood.common.packaged.AssemblyLineAdapter(true));
        if (com.xyp.gtnotgood.utils.enums.ModList.Thaumcraft.isModLoaded()) {
            var infusionCore = new com.xyp.gtnotgood.common.packaged.ItemPackagedCore(true);
            GameRegistry.registerItem(infusionCore, "tc4_infusion_packaged_core");
            GTNGItemList.ThaumcraftInfusionCore.set(infusionCore);
            com.xyp.gtnotgood.common.packaged.PackagedCoreRegistry
                .register("thaumcraft_infusion", new com.xyp.gtnotgood.common.packaged.ThaumcraftInfusionAdapter());
        }
        com.xyp.gtnotgood.common.user.ItemUserSpeedUpgrade userSpeed = new com.xyp.gtnotgood.common.user.ItemUserSpeedUpgrade();
        GameRegistry.registerItem(userSpeed, "mechanical_user_speed");
        GTNGItemList.MechanicalUserSpeedUpgrade.set(userSpeed);
        meWirelessTransceiver = new ItemMEWirelessTransceiver();
        GameRegistry.registerItem(meWirelessTransceiver, ItemMEWirelessTransceiver.ITEM_NAME);
        GTNGItemList.MEWirelessTransceiver.set(meWirelessTransceiver);

        wildcardPattern = new WildcardPatternItem();
        GameRegistry.registerItem(wildcardPattern, WildcardPatternItem.ITEM_NAME);
        GTNGItemList.WildcardPattern.set(wildcardPattern);
        // 注册矿脉挖掘镐
        veinMiningPickaxe = new VeinMiningPickaxe();
    }
}
