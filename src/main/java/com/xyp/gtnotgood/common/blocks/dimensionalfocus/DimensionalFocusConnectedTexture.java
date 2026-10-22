package com.xyp.gtnotgood.common.blocks.dimensionalfocus;

import javax.annotation.Nullable;

import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Dyes;
import gregtech.api.render.ISBRContext;
import gregtech.api.render.ISBRWorldContext;
import gregtech.common.render.GTCopiedBlockTextureRender;

public final class DimensionalFocusConnectedTexture extends GTCopiedBlockTextureRender {

    private final DimensionalFocusCasing variant;

    public DimensionalFocusConnectedTexture(DimensionalFocusCasing variant) {
        super(variant.getBlock(), ForgeDirection.UNKNOWN.ordinal(), variant.getMetadata(), Dyes._NULL.getRGBA());
        this.variant = variant;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, @Nullable ISBRContext context) {
        if (variant.isConnected() && context instanceof ISBRWorldContext world) {
            if (
                world.getBlockAccess().getBlock(world.getX(), world.getY(), world.getZ()) == variant.getBlock()
            ) return variant.getBlock().getIcon(world.getBlockAccess(), world.getX(), world.getY(), world.getZ(), side);
            IIcon icon = DimensionalFocusConnectedIcons.getIcon(variant, DimensionalFocusConnections
                .getMask(world.getBlockAccess(), world.getX(), world.getY(), world.getZ(), side, variant));
            if (icon != null) return icon;
        }
        return variant.getBlock().getIcon(side, variant.getMetadata());
    }
}
