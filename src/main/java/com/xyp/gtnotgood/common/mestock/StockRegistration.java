package com.xyp.gtnotgood.common.mestock;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.factory.GuiManager;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Upgrades;
import cpw.mods.fml.common.registry.GameRegistry;

public final class StockRegistration {

    private StockRegistration() {}

    public static void preInit() {
        register(ItemStockPart.Kind.ThresholdExportBus, "threshold_export_bus", GTNGItemList.ThresholdExportBus);
        register(
            ItemStockPart.Kind.ThresholdLevelEmitter,
            "threshold_level_emitter",
            GTNGItemList.ThresholdLevelEmitter);
        register(ItemStockPart.Kind.RequesterTerminal, "requester_terminal", GTNGItemList.MERequesterTerminal);
        ItemStack bus = GTNGItemList.ThresholdExportBus.get(1);
        Upgrades.CAPACITY.registerItem(bus, 3);
        Upgrades.SPEED.registerItem(bus, 4);
        Upgrades.SUPERSPEED.registerItem(bus, 4);
        Upgrades.SUPERLUMINALSPEED.registerItem(bus, 4);
        Upgrades.REDSTONE.registerItem(bus, 1);
        BlockMERequester requester = new BlockMERequester();
        GameRegistry.registerBlock(requester, ItemRequesterBlock.class, "me_requester");
        GameRegistry.registerTileEntity(TileMERequester.class, ModList.GTNotGood.getResourcePath("me_requester"));
        GTNGItemList.MERequester.set(new ItemStack(requester));
        AEApi.instance()
            .registries()
            .gridCache()
            .registerGridCache(StockGridCache.class, StockGridCache.class);
    }

    private static void register(ItemStockPart.Kind kind, String name, GTNGItemList container) {
        ItemStockPart item = new ItemStockPart(kind);
        GameRegistry.registerItem(item, name);
        container.set(new ItemStack(item));
    }

    public static void init() {
        GuiManager.registerFactory(StockGuiFactory.instance);
        var definitions = AEApi.instance()
            .definitions();
        ItemStack calc = definitions.materials()
            .calcProcessor()
            .maybeStack(1)
            .get();
        ItemStack logic = definitions.materials()
            .logicProcessor()
            .maybeStack(1)
            .get();
        ItemStack export = definitions.parts()
            .exportBus()
            .maybeStack(1)
            .get();
        ItemStack emitter = definitions.parts()
            .levelEmitter()
            .maybeStack(1)
            .get();
        GameRegistry.addShapelessRecipe(GTNGItemList.ThresholdExportBus.get(1), export, emitter, calc);
        GameRegistry.addShapelessRecipe(GTNGItemList.ThresholdLevelEmitter.get(1), emitter, emitter, calc);
        GameRegistry.addShapedRecipe(
            GTNGItemList.MERequester.get(1),
            "CLC",
            "EIE",
            "CLC",
            'C',
            calc,
            'L',
            logic,
            'E',
            emitter,
            'I',
            definitions.blocks()
                .iface()
                .maybeStack(1)
                .get());
        GameRegistry.addShapelessRecipe(
            GTNGItemList.MERequesterTerminal.get(1),
            definitions.parts()
                .interfaceTerminal()
                .maybeStack(1)
                .get(),
            calc,
            logic);
    }
}
