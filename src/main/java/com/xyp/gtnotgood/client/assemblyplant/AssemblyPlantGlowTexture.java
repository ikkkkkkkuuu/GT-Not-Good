package com.xyp.gtnotgood.client.assemblyplant;

import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.ITexture;
import gregtech.api.render.ISBRContext;
import gregtech.api.render.ISBRWorldContext;

@SideOnly(Side.CLIENT)
public final class AssemblyPlantGlowTexture implements ITexture {

    private final AssemblyPlantCasing variant;

    public AssemblyPlantGlowTexture(AssemblyPlantCasing variant) {
        this.variant = variant;
    }

    private ITexture texture(ISBRContext context, ForgeDirection side) {
        int mask = context instanceof ISBRWorldContext world
            ? AssemblyPlantConnections.getMask(world.getBlockAccess(), world.getX(), world.getY(), world.getZ(),
                side.ordinal(), variant)
            : 0;
        return AssemblyPlantConnectedIcons.getGlowTexture(variant, mask);
    }

    @Override
    public void renderXPos(ISBRContext context) {
        texture(context, ForgeDirection.EAST).renderXPos(context);
    }

    @Override
    public void renderXNeg(ISBRContext context) {
        texture(context, ForgeDirection.WEST).renderXNeg(context);
    }

    @Override
    public void renderYPos(ISBRContext context) {
        texture(context, ForgeDirection.UP).renderYPos(context);
    }

    @Override
    public void renderYNeg(ISBRContext context) {
        texture(context, ForgeDirection.DOWN).renderYNeg(context);
    }

    @Override
    public void renderZPos(ISBRContext context) {
        texture(context, ForgeDirection.SOUTH).renderZPos(context);
    }

    @Override
    public void renderZNeg(ISBRContext context) {
        texture(context, ForgeDirection.NORTH).renderZNeg(context);
    }

    @Override
    public boolean isValidTexture() {
        return AssemblyPlantConnectedIcons.getGlowTexture(variant, 0) != null;
    }
}
