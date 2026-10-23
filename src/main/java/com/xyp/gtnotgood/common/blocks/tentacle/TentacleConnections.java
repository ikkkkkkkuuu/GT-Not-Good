package com.xyp.gtnotgood.common.blocks.tentacle;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.stellar.StellarConnections;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TentacleConnections {

    private static final ForgeDirection[] RIGHT = { ForgeDirection.EAST, ForgeDirection.EAST, ForgeDirection.WEST,
        ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.NORTH };
    private static final ForgeDirection[] DOWN = { ForgeDirection.SOUTH, ForgeDirection.SOUTH, ForgeDirection.DOWN,
        ForgeDirection.DOWN, ForgeDirection.DOWN, ForgeDirection.DOWN };

    private TentacleConnections() {}

    public static int getMask(IBlockAccess world, int x, int y, int z, int side, TentacleCasing variant) {
        if (!variant.isConnected(side) || world == null || side < 0 || side >= ForgeDirection.VALID_DIRECTIONS.length)
            return 0;
        ForgeDirection right = RIGHT[side];
        ForgeDirection down = DOWN[side];
        ForgeDirection face = ForgeDirection.VALID_DIRECTIONS[side];
        int mask = 0;
        if (connects(world, x - right.offsetX, y - right.offsetY, z - right.offsetZ, face, variant))
            mask |= StellarConnections.LEFT;
        if (connects(world, x + right.offsetX, y + right.offsetY, z + right.offsetZ, face, variant))
            mask |= StellarConnections.RIGHT;
        if (connects(world, x + down.offsetX, y + down.offsetY, z + down.offsetZ, face, variant))
            mask |= StellarConnections.DOWN;
        if (connects(world, x - down.offsetX, y - down.offsetY, z - down.offsetZ, face, variant))
            mask |= StellarConnections.UP;
        if (
            connects(world, x - right.offsetX + down.offsetX, y - right.offsetY + down.offsetY,
                z - right.offsetZ + down.offsetZ, face, variant)
        ) mask |= StellarConnections.DOWN_LEFT;
        if (
            connects(world, x + right.offsetX + down.offsetX, y + right.offsetY + down.offsetY,
                z + right.offsetZ + down.offsetZ, face, variant)
        ) mask |= StellarConnections.DOWN_RIGHT;
        if (
            connects(world, x - right.offsetX - down.offsetX, y - right.offsetY - down.offsetY,
                z - right.offsetZ - down.offsetZ, face, variant)
        ) mask |= StellarConnections.UP_LEFT;
        if (
            connects(world, x + right.offsetX - down.offsetX, y + right.offsetY - down.offsetY,
                z + right.offsetZ - down.offsetZ, face, variant)
        ) mask |= StellarConnections.UP_RIGHT;
        return StellarConnections.normalizeMask(mask);
    }

    private static boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection face,
        TentacleCasing variant) {
        if (!sameMaterial(world, x, y, z, variant)) return false;
        int frontX = x + face.offsetX;
        int frontY = y + face.offsetY;
        int frontZ = z + face.offsetZ;
        Block cover = world.getBlock(frontX, frontY, frontZ);
        return !cover.isOpaqueCube() && !world.isSideSolid(frontX, frontY, frontZ, face.getOpposite(), false)
            && !(variant.isGlass() && sameMaterial(world, frontX, frontY, frontZ, variant));
    }

    private static boolean sameMaterial(IBlockAccess world, int x, int y, int z, TentacleCasing variant) {
        return world.getBlock(x, y, z) == variant.getBlock()
            && variant.getBlock().getVariant(world.getBlockMetadata(x, y, z)) == variant;
    }
}
