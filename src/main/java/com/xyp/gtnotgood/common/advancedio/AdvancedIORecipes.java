package com.xyp.gtnotgood.common.advancedio;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;

/** Combines existing AE buses/processors, retaining the pack's AE acquisition requirements. */
public final class AdvancedIORecipes {

    private AdvancedIORecipes() {}

    public static void register() {
        var definitions = AEApi.instance()
            .definitions();
        GameRegistry.addShapedRecipe(
            GTNGItemList.AdvancedIOBus.get(1),
            "ICI",
            "EPE",
            "ICI",
            'I',
            definitions.parts()
                .importBus()
                .maybeStack(1)
                .get(),
            'E',
            definitions.parts()
                .exportBus()
                .maybeStack(1)
                .get(),
            'C',
            definitions.materials()
                .calcProcessor()
                .maybeStack(1)
                .get(),
            'P',
            definitions.materials()
                .engProcessor()
                .maybeStack(1)
                .get());
    }
}
