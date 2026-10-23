package com.xyp.gtnotgood.common.blocks.dimensionalfocus;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.util.GTUtility;

public final class DimensionalFocusBlocks {

    private static final int TEXTURE_PAGE = 125;
    private static BlockDimensionalFocusCasing casing;
    private static BlockDimensionalFocusCasing glass;
    private static final ITexture[] textures = new ITexture[DimensionalFocusCasing.values().length];
    private static int textureStart = -1;

    private DimensionalFocusBlocks() {}

    public static void register() {
        casing = new BlockDimensionalFocusCasing(false);
        glass = new BlockDimensionalFocusCasing(true);
        GameRegistry.registerBlock(casing, ItemBlockDimensionalFocusCasing.class, "dimensional_focus_casing");
        GameRegistry.registerBlock(glass, ItemBlockDimensionalFocusCasing.class, "dimensional_focus_glass");
        if (Textures.BlockIcons.casingTexturePages[TEXTURE_PAGE] != null)
            throw new IllegalStateException("GregTech casing texture page 125 is already occupied");
        GTUtility.addTexturePage((byte) TEXTURE_PAGE);
        textureStart = TEXTURE_PAGE << 7;
        for (DimensionalFocusCasing variant : DimensionalFocusCasing.values()) {
            variant.getItemContainer().set(variant.getLegacyStack(1));
            ITexture texture = new DimensionalFocusConnectedTexture(variant);
            textures[variant.ordinal()] = texture;
            Textures.BlockIcons.setCasingTextureForId(getTextureId(variant), texture);
        }
    }

    public static BlockDimensionalFocusCasing getBlock(boolean isGlass) {
        BlockDimensionalFocusCasing block = isGlass ? glass : casing;
        if (block == null) throw new IllegalStateException("Dimensional focus casings have not been registered");
        return block;
    }

    public static int getTextureId(DimensionalFocusCasing variant) {
        if (textureStart < 0)
            throw new IllegalStateException("Dimensional focus casing textures have not been registered");
        return textureStart + variant.ordinal();
    }

    public static ITexture getTexture(DimensionalFocusCasing variant) {
        return textures[variant.ordinal()];
    }
}
