package com.xyp.gtnotgood.client.particlecollider;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.particlecollider.ParticleColliderCasing;
import com.xyp.gtnotgood.common.blocks.stellar.StellarConnections;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.SuperParticleCollider;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.SuperconductingElectromagneticFactory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;

@SideOnly(Side.CLIENT)
public final class ParticleColliderConnections {

    private static final ForgeDirection[] RIGHT = { ForgeDirection.EAST, ForgeDirection.EAST, ForgeDirection.WEST,
        ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.NORTH };
    private static final ForgeDirection[] DOWN = { ForgeDirection.SOUTH, ForgeDirection.SOUTH, ForgeDirection.DOWN,
        ForgeDirection.DOWN, ForgeDirection.DOWN, ForgeDirection.DOWN };
    private static final int[] DU = { -1, 1, 0, 0, -1, 1, -1, 1 };
    private static final int[] DV = { 0, 0, 1, -1, 1, 1, -1, -1 };

    private ParticleColliderConnections() {}

    public static int getMask(IBlockAccess world, int x, int y, int z, int side, ParticleColliderCasing variant) {
        if (!variant.isConnected() || world == null || side < 0 || side >= ForgeDirection.VALID_DIRECTIONS.length)
            return 0;
        ForgeDirection right = RIGHT[side];
        ForgeDirection down = DOWN[side];
        ForgeDirection face = ForgeDirection.VALID_DIRECTIONS[side];
        int mask = 0;
        for (int i = 0; i < DU.length; i++) {
            int nextX = x + right.offsetX * DU[i] + down.offsetX * DV[i];
            int nextY = y + right.offsetY * DU[i] + down.offsetY * DV[i];
            int nextZ = z + right.offsetZ * DU[i] + down.offsetZ * DV[i];
            if (connects(world, nextX, nextY, nextZ, face, variant)) mask |= 1 << i;
        }
        return StellarConnections.normalizeMask(mask);
    }

    private static boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection face,
        ParticleColliderCasing variant) {
        if (!sameMaterial(world, x, y, z, variant)) return false;
        int frontX = x + face.offsetX;
        int frontY = y + face.offsetY;
        int frontZ = z + face.offsetZ;
        Block cover = world.getBlock(frontX, frontY, frontZ);
        return !cover.isOpaqueCube() && !world.isSideSolid(frontX, frontY, frontZ, face.getOpposite(), false);
    }

    private static boolean sameMaterial(IBlockAccess world, int x, int y, int z, ParticleColliderCasing variant) {
        if (world.getBlock(x, y, z) == variant.getBlock())
            return ParticleColliderCasing.fromMetadata(world.getBlockMetadata(x, y, z)) == variant;
        if (variant != ParticleColliderCasing.Lafium) return false;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGregTechTileEntity base)) return false;
        IMetaTileEntity machine = base.getMetaTileEntity();
        if (machine instanceof SuperParticleCollider || machine instanceof SuperconductingElectromagneticFactory)
            return true;
        return machine instanceof MTEHatch hatch && hatch.getCasingTexture() != null
            && Textures.BlockIcons.getTextureIndex(hatch.getCasingTexture()) == variant.getTextureId();
    }
}
