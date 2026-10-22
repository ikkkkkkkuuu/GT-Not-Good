package com.xyp.gtnotgood.common.blocks.lighthunter;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.xyp.gtnotgood.common.blocks.stellar.StellarConnections;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.TextureFactory;

@SideOnly(Side.CLIENT)
public final class LightHunterConnectedIcons {

    private static final Map<String, IIcon[]> icons = new HashMap<>();
    private static final Map<String, ITexture[]> glowTextures = new HashMap<>();

    private LightHunterConnectedIcons() {}

    public static void register(IIconRegister register, String texture) {
        IIcon[] connected = new IIcon[256];
        for (int mask = 0; mask < connected.length; mask++) {
            if (StellarConnections.normalizeMask(mask) == mask) connected[mask] = register
                .registerIcon(ModList.GTNotGood.getResourcePath("lighthunter/ctm/" + texture + "/" + mask));
        }
        for (int mask = 0; mask < connected.length; mask++)
            connected[mask] = connected[StellarConnections.normalizeMask(mask)];
        icons.put(texture, connected);
    }

    public static IIcon getIcon(String texture, int mask) {
        IIcon[] connected = icons.get(texture);
        return connected == null ? null : connected[mask & 255];
    }

    public static void registerGlow(IIconRegister register, String name, boolean connected) {
        if (connected) register(register, name);
        ITexture[] textures = new ITexture[256];
        IIcon fallback = connected ? null
            : register.registerIcon(ModList.GTNotGood.getResourcePath("lighthunter/" + name));
        for (int mask = 0; mask < textures.length; mask++) {
            if (StellarConnections.normalizeMask(mask) == mask) {
                IIcon icon = connected ? getIcon(name, mask) : fallback;
                textures[mask] = TextureFactory.builder().addIcon(new FixedIcon(icon)).glow().build();
            }
        }
        for (int mask = 0; mask < textures.length; mask++)
            textures[mask] = textures[StellarConnections.normalizeMask(mask)];
        glowTextures.put(name, textures);
    }

    public static ITexture getGlowTexture(String name, int mask) {
        ITexture[] textures = glowTextures.get(name);
        return textures == null ? null : textures[mask & 255];
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
