package com.xyp.gtnotgood.common.machines.basic;

import net.minecraft.util.StatCollector;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;

public final class SteamCentrifuge extends SteamProcessingMachine {

    public SteamCentrifuge(int id, String name, String localizedName) {
        super(id, name, localizedName, description());
    }

    private SteamCentrifuge(String name, String[] description, ITexture[][][] textures) {
        super(name, description, textures);
    }

    private static String description() {
        // #tr tooltip.steam_centrifuge.description
        // # Cell-free centrifuging powered by steam
        // # zh_CN 无需流体单元的蒸汽离心处理
        return StatCollector.translateToLocal("tooltip.steam_centrifuge.description");
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SteamCentrifuge(mName, mDescriptionArray, mTextures);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.centrifugeNonCellRecipes;
    }

    @Override
    protected String getMachineOverlay() {
        return "centrifuge";
    }

    @Override
    protected boolean hasActiveGlow() {
        return false;
    }
}
