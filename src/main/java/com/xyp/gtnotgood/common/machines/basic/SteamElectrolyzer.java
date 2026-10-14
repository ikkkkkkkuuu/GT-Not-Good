package com.xyp.gtnotgood.common.machines.basic;

import net.minecraft.util.StatCollector;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;

public final class SteamElectrolyzer extends SteamProcessingMachine {

    public SteamElectrolyzer(int id, String name, String localizedName) {
        super(id, name, localizedName, description());
    }

    private SteamElectrolyzer(String name, String[] description, ITexture[][][] textures) {
        super(name, description, textures);
    }

    private static String description() {
        // #tr tooltip.steam_electrolyzer.description
        // # Cell-free electrolysis powered by steam
        // # zh_CN 无需流体单元的蒸汽电解处理
        return StatCollector.translateToLocal("tooltip.steam_electrolyzer.description");
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SteamElectrolyzer(mName, mDescriptionArray, mTextures);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.electrolyzerNonCellRecipes;
    }

    @Override
    protected String getMachineOverlay() {
        return "electrolyzer";
    }
}
