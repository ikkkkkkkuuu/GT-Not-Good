package com.xyp.gtnotgood.common.blocks.fieldextruder;

import com.xyp.gtnotgood.client.fieldextruder.FieldExtruderConnectedTexture;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.util.GTUtility;

public final class FieldExtruderBlocks {

    private static final int TEXTURE_PAGE = 126;
    private static BlockFieldExtruderCasing casing;
    private static BlockFieldExtruderCasing glass;
    private static final ITexture[] textures = new ITexture[FieldExtruderCasing.values().length];
    private static int textureStart = -1;

    private FieldExtruderBlocks() {}

    public static void register() {
        casing = new BlockFieldExtruderCasing(false);
        glass = new BlockFieldExtruderCasing(true);
        GameRegistry.registerBlock(casing, ItemBlockFieldExtruderCasing.class, "field_extruder_casing");
        GameRegistry.registerBlock(glass, ItemBlockFieldExtruderCasing.class, "field_extruder_glass");
        textureStart = reserveTexturePage();
        for (FieldExtruderCasing variant : FieldExtruderCasing.values()) {
            variant.getItemContainer().set(variant.getLegacyStack(1));
            ITexture texture = new FieldExtruderConnectedTexture(variant);
            textures[variant.ordinal()] = texture;
            Textures.BlockIcons.setCasingTextureForId(getTextureId(variant), texture);
        }
    }

    private static int reserveTexturePage() {
        // Hatch NBT stores texture IDs, so server and client must reserve the same page.
        if (Textures.BlockIcons.casingTexturePages[TEXTURE_PAGE] != null)
            throw new IllegalStateException("GregTech casing texture page 126 is already occupied");
        GTUtility.addTexturePage((byte) TEXTURE_PAGE);
        return TEXTURE_PAGE << 7;
    }

    public static BlockFieldExtruderCasing getBlock(boolean isGlass) {
        BlockFieldExtruderCasing block = isGlass ? glass : casing;
        if (block == null) throw new IllegalStateException("Field extruder casings have not been registered");
        return block;
    }

    public static int getTextureId(FieldExtruderCasing variant) {
        if (textureStart < 0)
            throw new IllegalStateException("Field extruder casing textures have not been registered");
        return textureStart + variant.ordinal();
    }

    public static ITexture getTexture(FieldExtruderCasing variant) {
        return textures[variant.ordinal()];
    }
}
