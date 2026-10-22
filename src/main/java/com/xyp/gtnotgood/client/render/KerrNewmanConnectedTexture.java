package com.xyp.gtnotgood.client.render;

import javax.annotation.Nullable;

import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.kerrnewman.KerrNewmanCasing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Dyes;
import gregtech.api.render.ISBRContext;
import gregtech.api.render.ISBRWorldContext;
import gregtech.common.render.GTCopiedBlockTextureRender;

public final class KerrNewmanConnectedTexture extends GTCopiedBlockTextureRender {

    private final KerrNewmanCasing variant;

    public KerrNewmanConnectedTexture(KerrNewmanCasing variant) {
        super(variant.getBlock(), ForgeDirection.UNKNOWN.ordinal(), variant.getMetadata(), Dyes._NULL.getRGBA());
        this.variant = variant;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, @Nullable ISBRContext context) {
        int mask = context instanceof ISBRWorldContext world
            ? KerrNewmanConnections.getMask(world.getBlockAccess(), world.getX(), world.getY(), world.getZ(), side,
                variant)
            : 0;
        return KerrNewmanConnectedIcons.getIcon(variant, mask);
    }
}
