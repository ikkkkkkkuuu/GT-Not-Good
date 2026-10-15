/* Adapted from GT-Not-Leisure, LGPL-3.0; see META-INF/chemical-recipes-port/NOTICE.md. */
package com.xyp.gtnotgood.common.recipe.metadata;

import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.nei.RecipeDisplayInfo;

public final class FuelRefiningTier extends RecipeMetadataKey<Integer> {

    public static final FuelRefiningTier INSTANCE = new FuelRefiningTier();

    private FuelRefiningTier() {
        super(Integer.class, ModList.GTNotGood.getID() + ".fuel_refining_tier");
    }

    @Override
    public void drawInfo(RecipeDisplayInfo info, Object value) {
        // #tr gtng.chemical.recipe.refining_tier
        // # Refining Core Tier: %s
        // # zh_CN 精炼核心等级：%s
        info.drawText(StatCollector.translateToLocalFormatted("gtng.chemical.recipe.refining_tier", cast(value, 0)));
    }
}
