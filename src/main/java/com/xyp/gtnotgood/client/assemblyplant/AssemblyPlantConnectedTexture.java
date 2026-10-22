package com.xyp.gtnotgood.client.assemblyplant;

import javax.annotation.Nullable;

import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Dyes;
import gregtech.api.render.ISBRContext;
import gregtech.api.render.ISBRWorldContext;
import gregtech.common.render.GTCopiedBlockTextureRender;

public final class AssemblyPlantConnectedTexture extends GTCopiedBlockTextureRender {

    private final AssemblyPlantCasing variant;

    private final boolean active;

    public AssemblyPlantConnectedTexture(AssemblyPlantCasing variant, boolean active) {
        super(variant.getBlock(), ForgeDirection.UNKNOWN.ordinal(), variant.getMetadata(), Dyes._NULL.getRGBA());
        this.variant = variant;
        this.active = active;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, @Nullable ISBRContext context) {
        int mask = context instanceof ISBRWorldContext world
            ? AssemblyPlantConnections.getMask(world.getBlockAccess(), world.getX(), world.getY(), world.getZ(), side,
                variant)
            : 0;
        return AssemblyPlantConnectedIcons.getIcon(variant, mask, active);
    }
}
