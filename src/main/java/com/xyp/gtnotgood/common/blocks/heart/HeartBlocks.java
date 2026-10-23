package com.xyp.gtnotgood.common.blocks.heart;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.util.GTUtility;

public final class HeartBlocks {

    private static final int TEXTURE_PAGE = 121;
    private static BlockHeartCasing casing;
    private static BlockHeartCasing glass;
    private static final ITexture[] textures = new ITexture[HeartCasing.values().length];
    private static int textureStart = -1;

    private HeartBlocks() {}

    public static void register() {
        casing = new BlockHeartCasing(false);
        glass = new BlockHeartCasing(true);
        GameRegistry.registerBlock(casing, ItemBlockHeartCasing.class, "heart_casing");
        GameRegistry.registerBlock(glass, ItemBlockHeartCasing.class, "heart_glass");
        if (Textures.BlockIcons.casingTexturePages[TEXTURE_PAGE] != null)
            throw new IllegalStateException("GregTech casing texture page 121 is already occupied");
        GTUtility.addTexturePage((byte) TEXTURE_PAGE);
        textureStart = TEXTURE_PAGE << 7;
        for (HeartCasing variant : HeartCasing.values()) {
            variant.getItemContainer().set(variant.getLegacyStack(1));
            ITexture texture = new HeartConnectedTexture(variant);
            textures[variant.ordinal()] = texture;
            Textures.BlockIcons.setCasingTextureForId(getTextureId(variant), texture);
        }
    }

    public static BlockHeartCasing getBlock(boolean isGlass) {
        BlockHeartCasing block = isGlass ? glass : casing;
        if (block == null) throw new IllegalStateException("Heart casings have not been registered");
        return block;
    }

    public static int getTextureId(HeartCasing variant) {
        if (textureStart < 0) throw new IllegalStateException("Heart casing textures have not been registered");
        return textureStart + variant.ordinal();
    }

    public static ITexture getTexture(HeartCasing variant) {
        return textures[variant.ordinal()];
    }
}
