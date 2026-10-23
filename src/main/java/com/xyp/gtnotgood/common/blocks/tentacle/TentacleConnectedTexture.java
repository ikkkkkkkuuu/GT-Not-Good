package com.xyp.gtnotgood.common.blocks.tentacle;

import javax.annotation.Nullable;

import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Dyes;
import gregtech.api.render.ISBRContext;
import gregtech.api.render.ISBRWorldContext;
import gregtech.common.render.GTCopiedBlockTextureRender;

public final class TentacleConnectedTexture extends GTCopiedBlockTextureRender {

    private final TentacleCasing variant;
    private final boolean active;

    public TentacleConnectedTexture(TentacleCasing variant) {
        this(variant, false);
    }

    public TentacleConnectedTexture(TentacleCasing variant, boolean active) {
        super(variant.getBlock(), ForgeDirection.UNKNOWN.ordinal(),
            active ? variant.getActiveMetadata() : variant.getMetadata(), Dyes._NULL.getRGBA());
        this.variant = variant;
        this.active = active;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, @Nullable ISBRContext context) {
        if (variant.isConnected(side) && context instanceof ISBRWorldContext world) {
            return variant.getBlock().getIcon(world.getBlockAccess(), world.getX(), world.getY(), world.getZ(), side);
        }
        return variant.getBlock().getIcon(side, active ? variant.getActiveMetadata() : variant.getMetadata());
    }
}
