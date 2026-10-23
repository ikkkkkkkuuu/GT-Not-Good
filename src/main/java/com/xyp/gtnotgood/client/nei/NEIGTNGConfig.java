package com.xyp.gtnotgood.client.nei;

import com.xyp.gtnotgood.Tags;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.FurnaceRecipeHandler;

@SuppressWarnings("unused")
public final class NEIGTNGConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        API.addRecipeCatalyst(GTNGItemList.LargeFurnace.get(1), new FurnaceRecipeHandler());
    }

    @Override
    public String getName() {
        return ModList.Names.GT_NOT_GOOD;
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
