package com.xyp.gtnotgood.client.particlecollider;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.xyp.gtnotgood.common.blocks.particlecollider.ParticleColliderCasing;
import com.xyp.gtnotgood.common.blocks.stellar.StellarConnections;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.TextureFactory;

@SideOnly(Side.CLIENT)
public final class ParticleColliderConnectedIcons {

    private static final Map<ParticleColliderCasing, IIcon> baseIcons = new EnumMap<>(ParticleColliderCasing.class);
    private static final Map<ParticleColliderCasing, IIcon[]> connectedIcons = new EnumMap<>(
        ParticleColliderCasing.class);
    private static final ITexture[] glowTextures = new ITexture[256];

    private ParticleColliderConnectedIcons() {}

    public static void register(IIconRegister register) {
        for (ParticleColliderCasing variant : ParticleColliderCasing.values()) {
            baseIcons.put(variant, register
                .registerIcon(ModList.GTNotGood.getResourcePath("particlecollider/" + variant.getTextureName())));
            if (variant.isConnected())
                connectedIcons.put(variant, registerConnected(register, variant.getTextureName()));
        }
        IIcon[] glow = registerConnected(register, "superconducting_coil_bloom");
        for (int mask = 0; mask < glowTextures.length; mask++) {
            if (StellarConnections.normalizeMask(mask) == mask)
                glowTextures[mask] = TextureFactory.builder().addIcon(new FixedIcon(glow[mask])).glow().build();
        }
        for (int mask = 0; mask < glowTextures.length; mask++)
            glowTextures[mask] = glowTextures[StellarConnections.normalizeMask(mask)];
    }

    private static IIcon[] registerConnected(IIconRegister register, String texture) {
        IIcon[] icons = new IIcon[256];
        for (int mask = 0; mask < icons.length; mask++) {
            if (StellarConnections.normalizeMask(mask) == mask) icons[mask] = register
                .registerIcon(ModList.GTNotGood.getResourcePath("particlecollider/ctm/" + texture + "/" + mask));
        }
        for (int mask = 0; mask < icons.length; mask++) icons[mask] = icons[StellarConnections.normalizeMask(mask)];
        return icons;
    }

    public static IIcon getBaseIcon(ParticleColliderCasing variant) {
        return baseIcons.get(variant);
    }

    public static IIcon getIcon(ParticleColliderCasing variant, int mask) {
        IIcon[] connected = connectedIcons.get(variant);
        return connected == null ? getBaseIcon(variant) : connected[mask & 255];
    }

    public static ITexture getGlowTexture(int mask) {
        return glowTextures[mask & 255];
    }

    private static final class FixedIcon implements IIconContainer {

        private final IIcon icon;

        private FixedIcon(IIcon icon) {
            this.icon = icon;
        }

        @Override
        public IIcon getIcon() {
            return icon;
        }

        @Override
        public IIcon getOverlayIcon() {
            return null;
        }

        @Override
        public ResourceLocation getTextureFile() {
            return TextureMap.locationBlocksTexture;
        }
    }
}
