package com.xyp.gtnotgood.common.recipe.machine;

import com.gtnewhorizon.cropsnh.api.CropsNHItemList;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.registry.GameRegistry;

public final class LargeIndustrialFarmRecipes {

    private LargeIndustrialFarmRecipes() {}

    public static void loadRecipes() {
        GameRegistry.addShapelessRecipe(GTNGItemList.LargeIndustrialFarm.get(1),
            CropsNHItemList.IndustrialFarmController.get(1));
    }
}
