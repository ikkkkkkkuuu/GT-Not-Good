package com.xyp.gtnotgood.loader;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.common.items.VeinMiningPickaxe.VeinMiningPickaxe;
import com.xyp.gtnotgood.common.items.fuel.IronFuelRod;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternItem;
import com.xyp.gtnotgood.common.mebridge.ItemMEWirelessTransceiver;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.items.ItemRadioactiveCellIC;

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
        var advancedIO = new com.xyp.gtnotgood.common.advancedio.ItemAdvancedIOBus();
        GameRegistry.registerItem(advancedIO, "advanced_io_bus");
        GTNGItemList.AdvancedIOBus.set(advancedIO);
        appeng.api.config.Upgrades.SPEED.registerItem(GTNGItemList.AdvancedIOBus.get(1), 4);
        appeng.api.config.Upgrades.SUPERSPEED.registerItem(GTNGItemList.AdvancedIOBus.get(1), 4);
        appeng.api.config.Upgrades.SUPERLUMINALSPEED.registerItem(GTNGItemList.AdvancedIOBus.get(1), 4);
        appeng.api.config.Upgrades.CAPACITY.registerItem(GTNGItemList.AdvancedIOBus.get(1), 5);
        appeng.api.config.Upgrades.REDSTONE.registerItem(GTNGItemList.AdvancedIOBus.get(1), 1);
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
            // #tr item.tc4_crucible_packaged_core.name
            // # Thaumcraft Crucible Packaged Core
            // # zh_CN 神秘时代坩埚封包核心
            var crucibleCore = new com.xyp.gtnotgood.common.packaged.ItemPackagedCore(
                "thaumcraft_crucible",
                "tc4_crucible_packaged_core");
            GameRegistry.registerItem(crucibleCore, "tc4_crucible_packaged_core");
            GTNGItemList.ThaumcraftCrucibleCore.set(crucibleCore);
            com.xyp.gtnotgood.common.packaged.PackagedCoreRegistry
                .register("thaumcraft_crucible", new com.xyp.gtnotgood.common.packaged.ThaumcraftCrucibleAdapter());
            // #tr item.arcane_workbench_packaged_core.name
            // # Arcane Workbench Packaged Core
            // # zh_CN 奥术工作台封包核心
            var arcaneCore = new com.xyp.gtnotgood.common.packaged.ItemPackagedCore(
                "arcane_workbench",
                "arcane_workbench_packaged_core");
            GameRegistry.registerItem(arcaneCore, "arcane_workbench_packaged_core");
            GTNGItemList.ArcaneWorkbenchCore.set(arcaneCore);
            com.xyp.gtnotgood.common.packaged.PackagedCoreRegistry
                .register("arcane_workbench", new com.xyp.gtnotgood.common.packaged.ArcaneWorkbenchAdapter());
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

        ItemStack baseFuel = ItemList.RodUranium4.get(1);
        if (baseFuel == null || !(baseFuel.getItem() instanceof ItemRadioactiveCellIC)) {
            throw new IllegalStateException("GregTech four-cell uranium fuel rod is unavailable");
        }
        // #tr item.gtnotgood.depleted_iron_fuel_rod.name
        // # Depleted Iron Fuel Rod
        // # zh_CN 枯竭的铁燃料棒
        GTNGItem depletedFuel = new GTNGItem("depleted_iron_fuel_rod");
        GameRegistry.registerItem(depletedFuel, "depleted_iron_fuel_rod");
        GTNGItemList.DepletedIronFuelRod.set(depletedFuel);
        // #tr gt.gtnotgood.iron_fuel_rod.name
        // # Iron Fuel Rod
        // # zh_CN 铁燃料棒
        GTNGItemList.IronFuelRod
            .set(new IronFuelRod((ItemRadioactiveCellIC) baseFuel.getItem(), new ItemStack(depletedFuel)));

        // 注册矿脉挖掘镐
        veinMiningPickaxe = new VeinMiningPickaxe();
    }
}
