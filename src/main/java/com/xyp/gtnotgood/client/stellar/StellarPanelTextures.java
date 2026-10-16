package com.xyp.gtnotgood.client.stellar;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.xyp.gtnotgood.common.blocks.stellar.StellarPanelVariant;
import com.xyp.gtnotgood.common.blocks.stellar.StellarPanelVariant.Base;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.TextureFactory;

@SideOnly(Side.CLIENT)
public final class StellarPanelTextures implements IIconContainer {

    private final IIcon icon;

    private StellarPanelTextures(IIconRegister register, String name) {
        icon = register.registerIcon(ModList.GTNotGood.getResourcePath("stellar/iconsets/" + name));
    }

    public static ITexture[][] create(StellarPanelVariant variant, IIconRegister register) {
        Base casing = variant.getBase();
        StellarConnectedIcons.register(register, casing.getConnectedTexture());
        ITexture base = new StellarConnectedTexture(casing.getBlock(), casing.getMetadata(),
            casing.getConnectedTexture());
        ITexture overlay = TextureFactory.of(new StellarPanelTextures(register, variant.getOverlayTexture()));
        ITexture glow = TextureFactory.builder()
            .addIcon(new StellarPanelTextures(register, variant.getOverlayTexture() + "_GLOW")).glow().build();
        ITexture[] sides = { base };
        ITexture[] front = { base, overlay, glow };
        return new ITexture[][] { sides, sides, front, sides, sides, sides };
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
