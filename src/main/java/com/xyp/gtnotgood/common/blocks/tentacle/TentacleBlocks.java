package com.xyp.gtnotgood.common.blocks.tentacle;

import java.util.EnumMap;
import java.util.Map;

import com.xyp.gtnotgood.common.blocks.tentacle.TentacleCasing.Group;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.util.GTUtility;

public final class TentacleBlocks {

    private static final int TEXTURE_PAGE = 120;
    private static final Map<Group, BlockTentacleCasing> blocks = new EnumMap<>(Group.class);
    private static final ITexture[] textures = new ITexture[TentacleCasing.values().length];
    private static int textureStart = -1;

    private TentacleBlocks() {}

    public static void register() {
        for (Group group : Group.values()) {
            BlockTentacleCasing block = new BlockTentacleCasing(group);
            blocks.put(group, block);
            GameRegistry.registerBlock(block, ItemBlockTentacleCasing.class, block.getRegistryName());
        }
        if (Textures.BlockIcons.casingTexturePages[TEXTURE_PAGE] != null)
            throw new IllegalStateException("GregTech casing texture page " + TEXTURE_PAGE + " is already occupied");
        GTUtility.addTexturePage((byte) TEXTURE_PAGE);
        textureStart = TEXTURE_PAGE << 7;
        for (TentacleCasing variant : TentacleCasing.values()) {
            variant.getItemContainer().set(variant.getLegacyStack(1));
            ITexture texture = new TentacleConnectedTexture(variant);
            textures[variant.ordinal()] = texture;
            Textures.BlockIcons.setCasingTextureForId(getTextureId(variant), texture);
        }
    }

    public static BlockTentacleCasing getBlock(Group group) {
        BlockTentacleCasing block = blocks.get(group);
        if (block == null) throw new IllegalStateException("Light Hunter casings have not been registered");
        return block;
    }

    public static int getTextureId(TentacleCasing variant) {
        if (textureStart < 0) throw new IllegalStateException("Light Hunter casing textures have not been registered");
        return textureStart + variant.ordinal();
    }

    public static ITexture getTexture(TentacleCasing variant) {
        return textures[variant.ordinal()];
    }
}
