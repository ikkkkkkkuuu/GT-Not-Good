package com.xyp.gtnotgood.client.particlecollider;

import javax.annotation.Nullable;

import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.particlecollider.ParticleColliderCasing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Dyes;
import gregtech.api.render.ISBRContext;
import gregtech.api.render.ISBRWorldContext;
import gregtech.common.render.GTCopiedBlockTextureRender;

public final class ParticleColliderConnectedTexture extends GTCopiedBlockTextureRender {

    private final ParticleColliderCasing variant;

    public ParticleColliderConnectedTexture(ParticleColliderCasing variant) {
        super(variant.getBlock(), ForgeDirection.UNKNOWN.ordinal(), variant.getMetadata(), Dyes._NULL.getRGBA());
        this.variant = variant;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, @Nullable ISBRContext context) {
        int mask = context instanceof ISBRWorldContext world
            ? ParticleColliderConnections.getMask(world.getBlockAccess(), world.getX(), world.getY(), world.getZ(),
                side, variant)
            : 0;
        return ParticleColliderConnectedIcons.getIcon(variant, mask);
    }
}
