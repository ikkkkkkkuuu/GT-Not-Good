package com.xyp.gtnotgood.common.blocks.lighthunter;

import java.util.EnumMap;
import java.util.Map;

import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing.Group;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.util.GTUtility;

public final class LightHunterBlocks {

    private static final int TEXTURE_PAGE = 127;
    private static final Map<Group, BlockLightHunterCasing> blocks = new EnumMap<>(Group.class);
    private static final ITexture[] textures = new ITexture[LightHunterCasing.values().length];
    private static int textureStart = -1;

    private LightHunterBlocks() {}

    public static void register() {
        for (Group group : Group.values()) {
            BlockLightHunterCasing block = new BlockLightHunterCasing(group);
            blocks.put(group, block);
            GameRegistry.registerBlock(block, ItemBlockLightHunterCasing.class, block.getRegistryName());
        }
        if (Textures.BlockIcons.casingTexturePages[TEXTURE_PAGE] != null)
            throw new IllegalStateException("GregTech casing texture page " + TEXTURE_PAGE + " is already occupied");
        GTUtility.addTexturePage((byte) TEXTURE_PAGE);
        textureStart = TEXTURE_PAGE << 7;
        for (LightHunterCasing variant : LightHunterCasing.values()) {
            variant.getItemContainer().set(variant.getStack(1));
            ITexture texture = new LightHunterConnectedTexture(variant);
            textures[variant.ordinal()] = texture;
            Textures.BlockIcons.setCasingTextureForId(getTextureId(variant), texture);
        }
    }

    public static BlockLightHunterCasing getBlock(Group group) {
        BlockLightHunterCasing block = blocks.get(group);
        if (block == null) throw new IllegalStateException("Light Hunter casings have not been registered");
        return block;
    }

    public static int getTextureId(LightHunterCasing variant) {
        if (textureStart < 0) throw new IllegalStateException("Light Hunter casing textures have not been registered");
        return textureStart + variant.ordinal();
    }

    public static ITexture getTexture(LightHunterCasing variant) {
        return textures[variant.ordinal()];
    }
}
