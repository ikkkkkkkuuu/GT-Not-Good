package com.xyp.gtnotgood.client.render;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.xyp.gtnotgood.common.blocks.kerrnewman.KerrNewmanCasing;
import com.xyp.gtnotgood.common.blocks.stellar.StellarConnections;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.TextureFactory;

@SideOnly(Side.CLIENT)
public final class KerrNewmanConnectedIcons {

    private static final Map<KerrNewmanCasing, IIcon> baseIcons = new EnumMap<>(KerrNewmanCasing.class);
    private static final Map<KerrNewmanCasing, IIcon[]> connectedIcons = new EnumMap<>(KerrNewmanCasing.class);
    private static final Map<KerrNewmanCasing, ITexture[]> glowTextures = new EnumMap<>(KerrNewmanCasing.class);

    private KerrNewmanConnectedIcons() {}

    public static void register(IIconRegister register, KerrNewmanCasing.Group group) {
        for (KerrNewmanCasing variant : KerrNewmanCasing.values()) {
            if (variant.getGroup() != group) continue;
            baseIcons.put(variant, register.registerIcon(
                ModList.GTNotGood.getResourcePath(texturePath(variant.getTextureName(), variant.getTextureName()))));
            if (variant.isConnected())
                connectedIcons.put(variant, registerConnected(register, variant.getTextureName()));
            if (variant.hasActiveAppearance()) {
                String name = variant.getTextureName() + "_bloom";
                IIcon[] glow = variant.isConnected() ? registerConnected(register, name) : null;
                IIcon fixed = glow == null
                    ? register.registerIcon(ModList.GTNotGood.getResourcePath("kerrnewman/" + name))
                    : null;
                ITexture[] textures = new ITexture[256];
                for (int mask = 0; mask < textures.length; mask++) {
                    if (StellarConnections.normalizeMask(mask) == mask) textures[mask] = TextureFactory.builder()
                        .addIcon(new FixedIcon(glow == null ? fixed : glow[mask])).glow().build();
                }
                for (int mask = 0; mask < textures.length; mask++)
                    textures[mask] = textures[StellarConnections.normalizeMask(mask)];
                glowTextures.put(variant, textures);
            }
        }
    }

    private static IIcon[] registerConnected(IIconRegister register, String name) {
        IIcon[] icons = new IIcon[256];
        for (int mask = 0; mask < icons.length; mask++) {
            if (StellarConnections.normalizeMask(mask) == mask) icons[mask] = register
                .registerIcon(ModList.GTNotGood.getResourcePath(texturePath(name, "ctm/" + name + "/" + mask)));
        }
        for (int mask = 0; mask < icons.length; mask++) icons[mask] = icons[StellarConnections.normalizeMask(mask)];
        return icons;
    }

    private static String texturePath(String name, String path) {
        return "kerrnewman/" + (name.equals("dimensional_bridge_casing") ? "animated/" : "") + path;
    }

    public static IIcon getIcon(KerrNewmanCasing variant, int mask) {
        IIcon[] connected = connectedIcons.get(variant);
        return connected == null ? baseIcons.get(variant) : connected[mask & 255];
    }

    public static ITexture getGlowTexture(KerrNewmanCasing variant, int mask) {
        ITexture[] textures = glowTextures.get(variant);
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
