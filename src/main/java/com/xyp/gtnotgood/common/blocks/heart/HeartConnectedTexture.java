package com.xyp.gtnotgood.common.blocks.heart;

import javax.annotation.Nullable;

import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Dyes;
import gregtech.api.render.ISBRContext;
import gregtech.api.render.ISBRWorldContext;
import gregtech.common.render.GTCopiedBlockTextureRender;

public final class HeartConnectedTexture extends GTCopiedBlockTextureRender {

    private final HeartCasing variant;

    public HeartConnectedTexture(HeartCasing variant) {
        super(variant.getBlock(), ForgeDirection.UNKNOWN.ordinal(), variant.getMetadata(), Dyes._NULL.getRGBA());
        this.variant = variant;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, @Nullable ISBRContext context) {
        if (variant.isConnected() && context instanceof ISBRWorldContext world) {
            return variant.getBlock().getIcon(world.getBlockAccess(), world.getX(), world.getY(), world.getZ(), side);
        }
        return variant.getBlock().getIcon(side, variant.getMetadata());
    }
}
