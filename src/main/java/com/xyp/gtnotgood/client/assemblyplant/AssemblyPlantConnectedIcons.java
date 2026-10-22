package com.xyp.gtnotgood.client.assemblyplant;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.blocks.stellar.StellarConnections;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.TextureFactory;

@SideOnly(Side.CLIENT)
public final class AssemblyPlantConnectedIcons {

    private static final Map<AssemblyPlantCasing, IIcon> baseIcons = new EnumMap<>(AssemblyPlantCasing.class);
    private static final Map<AssemblyPlantCasing, IIcon[]> connectedIcons = new EnumMap<>(AssemblyPlantCasing.class);
    private static final Map<AssemblyPlantCasing, ITexture[]> glowTextures = new EnumMap<>(AssemblyPlantCasing.class);
    private static IIcon electrolyticCellActive;

    private AssemblyPlantConnectedIcons() {}

    public static void register(IIconRegister register, boolean glass) {
        for (AssemblyPlantCasing variant : AssemblyPlantCasing.values()) {
            if (variant.isGlass() != glass) continue;
            baseIcons.put(variant, icon(register, variant.getTextureName()));
            if (variant.isConnected())
                connectedIcons.put(variant, registerConnected(register, variant.getTextureName()));
        }
        if (glass) return;
        electrolyticCellActive = icon(register, "electrolytic_cell_active");
        registerGlow(AssemblyPlantCasing.AdvancedAssemblyLineUnit,
            new IIcon[] { icon(register, "advanced_assembly_line_unit_bloom") });
        registerGlow(AssemblyPlantCasing.MolybdenumDisilicideCoil,
            registerConnected(register, "molybdenum_disilicide_coil_bloom"));
    }

    private static IIcon icon(IIconRegister register, String name) {
        return register.registerIcon(ModList.GTNotGood.getResourcePath("assemblyplant/" + name));
    }

    private static IIcon[] registerConnected(IIconRegister register, String texture) {
        IIcon[] icons = new IIcon[256];
        for (int mask = 0; mask < icons.length; mask++) {
            if (StellarConnections.normalizeMask(mask) == mask)
                icons[mask] = icon(register, "ctm/" + texture + "/" + mask);
        }
        for (int mask = 0; mask < icons.length; mask++) icons[mask] = icons[StellarConnections.normalizeMask(mask)];
        return icons;
    }

    private static void registerGlow(AssemblyPlantCasing variant, IIcon[] icons) {
        ITexture[] textures = new ITexture[256];
        if (icons.length == 1) {
            ITexture texture = TextureFactory.builder().addIcon(new FixedIcon(icons[0])).glow().build();
            for (int mask = 0; mask < textures.length; mask++) textures[mask] = texture;
        } else {
            for (int mask = 0; mask < textures.length; mask++) {
                if (StellarConnections.normalizeMask(mask) == mask)
                    textures[mask] = TextureFactory.builder().addIcon(new FixedIcon(icons[mask])).glow().build();
            }
            for (int mask = 0; mask < textures.length; mask++)
                textures[mask] = textures[StellarConnections.normalizeMask(mask)];
        }
        glowTextures.put(variant, textures);
    }

    public static IIcon getIcon(AssemblyPlantCasing variant, int mask, boolean active) {
        if (active && variant == AssemblyPlantCasing.ElectrolyticCell) return electrolyticCellActive;
        IIcon[] connected = connectedIcons.get(variant);
        return connected == null ? baseIcons.get(variant) : connected[mask & 255];
    }

    public static ITexture getGlowTexture(AssemblyPlantCasing variant, int mask) {
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
