package com.xyp.gtnotgood.common.blocks.kerrnewman;

import com.xyp.gtnotgood.client.render.KerrNewmanConnectedTexture;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.util.GTUtility;

public final class KerrNewmanBlocks {

    private static final int TEXTURE_PAGE = 122;
    private static final BlockKerrNewmanCasing[] blocks = new BlockKerrNewmanCasing[KerrNewmanCasing.Group
        .values().length];
    private static final ITexture[] textures = new ITexture[KerrNewmanCasing.values().length];
    private static int textureStart = -1;

    private KerrNewmanBlocks() {}

    public static void register() {
        String[] names = { "kerr_newman_casing", "kerr_newman_casing_2", "kerr_newman_glass" };
        for (KerrNewmanCasing.Group group : KerrNewmanCasing.Group.values()) {
            BlockKerrNewmanCasing block = new BlockKerrNewmanCasing(group, names[group.ordinal()]);
            blocks[group.ordinal()] = block;
            GameRegistry.registerBlock(block, ItemBlockKerrNewmanCasing.class, names[group.ordinal()]);
        }
        if (Textures.BlockIcons.casingTexturePages[TEXTURE_PAGE] != null)
            throw new IllegalStateException("GregTech casing texture page 122 is already occupied");
        GTUtility.addTexturePage((byte) TEXTURE_PAGE);
        textureStart = TEXTURE_PAGE << 7;
        for (KerrNewmanCasing variant : KerrNewmanCasing.values()) {
            variant.getItemContainer().set(variant.getStack(1));
            ITexture texture = new KerrNewmanConnectedTexture(variant);
            textures[variant.ordinal()] = texture;
            Textures.BlockIcons.setCasingTextureForId(getTextureId(variant), texture);
        }
    }

    public static BlockKerrNewmanCasing getBlock(KerrNewmanCasing.Group group) {
        BlockKerrNewmanCasing block = blocks[group.ordinal()];
        if (block == null) throw new IllegalStateException("Kerr-Newman casings have not been registered");
        return block;
    }

    public static int getTextureId(KerrNewmanCasing variant) {
        if (textureStart < 0) throw new IllegalStateException("Kerr-Newman casing textures have not been registered");
        return textureStart + variant.ordinal();
    }

    public static ITexture getTexture(KerrNewmanCasing variant) {
        return textures[variant.ordinal()];
    }
}
