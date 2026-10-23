package com.xyp.gtnotgood.common.blocks.particlecollider;

import com.xyp.gtnotgood.client.particlecollider.ParticleColliderConnectedTexture;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.util.GTUtility;

public final class ParticleColliderBlocks {

    private static final int TEXTURE_PAGE = 124;
    private static final ITexture[] textures = new ITexture[ParticleColliderCasing.values().length];
    private static BlockParticleColliderCasing block;

    private ParticleColliderBlocks() {}

    public static void register() {
        if (Textures.BlockIcons.casingTexturePages[TEXTURE_PAGE] != null)
            throw new IllegalStateException("GregTech casing texture page 124 is already occupied");
        GTUtility.addTexturePage((byte) TEXTURE_PAGE);
        block = new BlockParticleColliderCasing();
        GameRegistry.registerBlock(block, ItemBlockParticleColliderCasing.class, "particle_collider_casing");
        for (ParticleColliderCasing variant : ParticleColliderCasing.values()) {
            variant.getItemContainer().set(variant.getLegacyStack(1));
            ITexture texture = new ParticleColliderConnectedTexture(variant);
            textures[variant.ordinal()] = texture;
            Textures.BlockIcons.setCasingTextureForId(variant.getTextureId(), texture);
        }
    }

    public static BlockParticleColliderCasing getBlock() {
        if (block == null) throw new IllegalStateException("Particle collider casings have not been registered");
        return block;
    }

    public static int getTextureId(ParticleColliderCasing variant) {
        return (TEXTURE_PAGE << 7) + variant.ordinal();
    }

    public static ITexture getTexture(ParticleColliderCasing variant) {
        return textures[variant.ordinal()];
    }
}
