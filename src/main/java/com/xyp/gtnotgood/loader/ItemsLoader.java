package com.xyp.gtnotgood.loader;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.blocks.packaged.ArcaneWorkbenchAdapter;
import com.xyp.gtnotgood.common.blocks.packaged.AssemblyLineAdapter;
import com.xyp.gtnotgood.common.blocks.packaged.BloodAltarAdapter;
import com.xyp.gtnotgood.common.blocks.packaged.PackagedCoreRegistry;
import com.xyp.gtnotgood.common.blocks.packaged.ThaumcraftCrucibleAdapter;
import com.xyp.gtnotgood.common.blocks.packaged.ThaumcraftInfusionAdapter;
import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.common.items.advancedio.ItemAdvancedIOBus;
import com.xyp.gtnotgood.common.items.compass.StructureCompassItem;
import com.xyp.gtnotgood.common.items.fuel.IronFuelRod;
import com.xyp.gtnotgood.common.items.largeinterface.ItemLargeInterface;
import com.xyp.gtnotgood.common.items.mebridge.ItemMEWirelessTransceiver;
import com.xyp.gtnotgood.common.items.mechanicaluser.ItemUserSpeedUpgrade;
import com.xyp.gtnotgood.common.items.packaged.ItemPackagedCore;
import com.xyp.gtnotgood.common.items.packaged.ItemWirelessConnector;
import com.xyp.gtnotgood.common.items.patternsorter.PatternSorterItem;
import com.xyp.gtnotgood.common.items.stockio.ItemStockIOInterface;
import com.xyp.gtnotgood.common.items.veinmining.VeinMiningPickaxe;
import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternItem;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.config.Upgrades;
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
        registerItem(new StructureCompassItem(), "structure_compass", GTNGItemList.StructureCompass);
        registerPatternSorter();
        registerAdvancedIOBus();
        registerItem(new ItemStockIOInterface(), "stock_io_interface_part", GTNGItemList.StockIOInterfacePart);
        registerItem(new ItemLargeInterface(), "large_interface_part", GTNGItemList.LargeInterfacePart);
        registerPackagedItems();
        registerMechanicalUserUpgrades();
        registerWirelessTransceiver();
        registerWildcardPattern();
        registerFuelRods();
        registerTools();
    }

    private static void registerPatternSorter() {
        registerItem(new PatternSorterItem(), "pattern_sorter", GTNGItemList.PatternSorter);
    }

    private static void registerAdvancedIOBus() {
        registerItem(new ItemAdvancedIOBus(), "advanced_io_bus", GTNGItemList.AdvancedIOBus);
        Upgrades.SPEED.registerItem(GTNGItemList.AdvancedIOBus.get(1), 4);
        Upgrades.SUPERSPEED.registerItem(GTNGItemList.AdvancedIOBus.get(1), 4);
        Upgrades.SUPERLUMINALSPEED.registerItem(GTNGItemList.AdvancedIOBus.get(1), 4);
        Upgrades.CAPACITY.registerItem(GTNGItemList.AdvancedIOBus.get(1), 5);
        Upgrades.REDSTONE.registerItem(GTNGItemList.AdvancedIOBus.get(1), 1);
    }

    /** Registers shared packaged items, then the cores whose backing mods are available. */
    private static void registerPackagedItems() {
        registerItem(new ItemWirelessConnector(), "packaged_wireless_connector", GTNGItemList.ItemWirelessConnector);
        registerItem(new ItemPackagedCore(false), "basic_packaged_core", GTNGItemList.BasicPackagedCore);
        if (ModList.BloodMagic.isModLoaded()) {
            registerBloodAltarCore();
        }
        registerAssemblyLineCores();
        if (ModList.Thaumcraft.isModLoaded()) {
            registerThaumcraftCores();
        }
    }

    private static void registerBloodAltarCore() {
        // #tr item.blood_altar_packaged_core.name
        // # Blood Altar Packaged Core
        // # zh_CN 血魔法祭坛封包核心
        var bloodCore = new ItemPackagedCore("blood_altar", "blood_altar_packaged_core");
        registerItem(bloodCore, "blood_altar_packaged_core", GTNGItemList.BloodAltarCore);
        PackagedCoreRegistry.register("blood_altar", new BloodAltarAdapter());
    }

    private static void registerAssemblyLineCores() {
        // #tr item.assembly_line_packaged_core.name
        // # Assembly Line Packaged Core
        // # zh_CN 装配线封包核心
        var assemblyCore = new ItemPackagedCore("assembly_line", "assembly_line_packaged_core");
        registerItem(assemblyCore, "assembly_line_packaged_core", GTNGItemList.AssemblyLineCore);
        // #tr item.advanced_assembly_line_packaged_core.name
        // # Advanced Assembly Line Packaged Core
        // # zh_CN 进阶装配线封包核心
        var advancedCore = new ItemPackagedCore("advanced_assembly_line", "advanced_assembly_line_packaged_core");
        registerItem(advancedCore, "advanced_assembly_line_packaged_core", GTNGItemList.AdvancedAssemblyLineCore);
        PackagedCoreRegistry.register("assembly_line", new AssemblyLineAdapter(false));
        PackagedCoreRegistry.register("advanced_assembly_line", new AssemblyLineAdapter(true));
    }

    private static void registerThaumcraftCores() {
        // #tr item.tc4_crucible_packaged_core.name
        // # Thaumcraft Crucible Packaged Core
        // # zh_CN 神秘时代坩埚封包核心
        var crucibleCore = new ItemPackagedCore("thaumcraft_crucible", "tc4_crucible_packaged_core");
        registerItem(crucibleCore, "tc4_crucible_packaged_core", GTNGItemList.ThaumcraftCrucibleCore);
        PackagedCoreRegistry.register("thaumcraft_crucible", new ThaumcraftCrucibleAdapter());
        // #tr item.arcane_workbench_packaged_core.name
        // # Arcane Workbench Packaged Core
        // # zh_CN 奥术工作台封包核心
        var arcaneCore = new ItemPackagedCore("arcane_workbench", "arcane_workbench_packaged_core");
        registerItem(arcaneCore, "arcane_workbench_packaged_core", GTNGItemList.ArcaneWorkbenchCore);
        PackagedCoreRegistry.register("arcane_workbench", new ArcaneWorkbenchAdapter());
        registerItem(new ItemPackagedCore(true), "tc4_infusion_packaged_core", GTNGItemList.ThaumcraftInfusionCore);
        PackagedCoreRegistry.register("thaumcraft_infusion", new ThaumcraftInfusionAdapter());
    }

    private static void registerMechanicalUserUpgrades() {
        registerItem(new ItemUserSpeedUpgrade(), "mechanical_user_speed", GTNGItemList.MechanicalUserSpeedUpgrade);
    }

    private static void registerWirelessTransceiver() {
        meWirelessTransceiver = registerItem(new ItemMEWirelessTransceiver(), ItemMEWirelessTransceiver.ITEM_NAME,
            GTNGItemList.MEWirelessTransceiver);
    }

    private static void registerWildcardPattern() {
        wildcardPattern = registerItem(new WildcardPatternItem(), WildcardPatternItem.ITEM_NAME,
            GTNGItemList.WildcardPattern);
    }

    /** Validates the GregTech fuel template before registering the depleted and active iron fuel rods. */
    private static void registerFuelRods() {
        ItemStack baseFuel = ItemList.RodUranium4.get(1);
        if (baseFuel == null || !(baseFuel.getItem() instanceof ItemRadioactiveCellIC)) {
            throw new IllegalStateException("GregTech four-cell uranium fuel rod is unavailable");
        }
        // #tr item.gtnotgood.depleted_iron_fuel_rod.name
        // # Depleted Iron Fuel Rod
        // # zh_CN 枯竭的铁燃料棒
        GTNGItem depletedFuel = new GTNGItem("depleted_iron_fuel_rod");
        registerItem(depletedFuel, "depleted_iron_fuel_rod", GTNGItemList.DepletedIronFuelRod);
        // #tr gt.gtnotgood.iron_fuel_rod.name
        // # Iron Fuel Rod
        // # zh_CN 铁燃料棒
        GTNGItemList.IronFuelRod
            .set(new IronFuelRod((ItemRadioactiveCellIC) baseFuel.getItem(), new ItemStack(depletedFuel)));
    }

    private static void registerTools() {
        veinMiningPickaxe = new VeinMiningPickaxe();
    }

    /**
     * Registers a new Forge item before exposing it through the shared item list.
     * Items that register themselves during construction must not use this helper.
     *
     * @param item  item instance to register
     * @param name  stable Forge registry name
     * @param entry shared item-list entry used by recipes and tooltips
     * @param <T>   concrete item type retained for typed loader fields
     * @return the registered item instance
     */
    private static <T extends Item> T registerItem(T item, String name, GTNGItemList entry) {
        GameRegistry.registerItem(item, name);
        entry.set(item);
        return item;
    }
}
