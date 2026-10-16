package com.xyp.gtnotgood.common.machines.basic;

import net.minecraft.util.StatCollector;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;

public final class SteamChemicalReactor extends SteamProcessingMachine {

    public SteamChemicalReactor(int id, String name, String localizedName) {
        super(id, name, localizedName, description());
    }

    private SteamChemicalReactor(String name, String[] description, ITexture[][][] textures) {
        super(name, description, textures);
    }

    private static String description() {
        // #tr tooltip.steam_chemical_reactor.description
        // # Runs chemical reactions using steam; no fluid cells needed
        // # zh_CN 使用蒸汽进行化学反应，无需流体单元
        return StatCollector.translateToLocal("tooltip.steam_chemical_reactor.description");
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SteamChemicalReactor(mName, mDescriptionArray, mTextures);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.multiblockChemicalReactorRecipes;
    }

    @Override
    protected String getMachineOverlay() {
        return "chemical_reactor";
    }
}
