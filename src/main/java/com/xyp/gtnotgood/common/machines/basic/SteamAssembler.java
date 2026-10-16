package com.xyp.gtnotgood.common.machines.basic;

import net.minecraft.util.StatCollector;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;

public final class SteamAssembler extends SteamProcessingMachine {

    public SteamAssembler(int id, String name, String localizedName) {
        super(id, name, localizedName, description(), 9, 1);
    }

    private SteamAssembler(String name, String[] description, ITexture[][][] textures) {
        super(name, description, textures, 9, 1);
    }

    private static String description() {
        // #tr tooltip.steam_assembler.description
        // # Assembles items using steam
        // # zh_CN 使用蒸汽组装物品
        return StatCollector.translateToLocal("tooltip.steam_assembler.description");
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SteamAssembler(mName, mDescriptionArray, mTextures);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.assemblerRecipes;
    }

    @Override
    protected String getMachineOverlay() {
        return "assembler";
    }
}
