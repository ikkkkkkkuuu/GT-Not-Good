package com.xyp.gtnotgood.client.stellar;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.stellar.BlockStellar;
import com.xyp.gtnotgood.common.blocks.stellar.StellarBlockVariant;
import com.xyp.gtnotgood.common.blocks.stellar.StellarConnections;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Dyes;
import gregtech.api.render.ISBRContext;
import gregtech.api.render.ISBRWorldContext;
import gregtech.common.render.GTCopiedBlockTextureRender;

public final class StellarConnectedTexture extends GTCopiedBlockTextureRender {

    private final Block block;
    private final int metadata;
    private final String texture;

    public StellarConnectedTexture(Block block, int metadata) {
        this(block, metadata, null);
    }

    public StellarConnectedTexture(Block block, int metadata, String texture) {
        super(block, ForgeDirection.UNKNOWN.ordinal(), metadata, Dyes._NULL.getRGBA());
        this.block = block;
        this.metadata = metadata;
        this.texture = texture;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int ordinalSide, @Nullable ISBRContext ctx) {
        if (ctx instanceof ISBRWorldContext world) {
            String key = texture;
            if (key == null && block instanceof BlockStellar stellar) {
                StellarBlockVariant variant = stellar.getVariant(metadata);
                if (variant != null) {
                    key = ordinalSide == 0 || ordinalSide == 1 ? variant.getEndTexture() : variant.getSideTexture();
                }
            }
            IIcon icon = StellarConnectedIcons.getIcon(key, StellarConnections.getMask(world.getBlockAccess(),
                world.getX(), world.getY(), world.getZ(), ordinalSide));
            if (icon != null) return icon;
        }
        return block.getIcon(ordinalSide, metadata);
    }
}
