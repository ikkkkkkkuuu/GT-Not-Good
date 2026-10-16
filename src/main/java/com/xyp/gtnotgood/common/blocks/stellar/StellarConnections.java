package com.xyp.gtnotgood.common.blocks.stellar;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.machines.hatch.VaultPortHatch;
import com.xyp.gtnotgood.common.machines.multiblock.SingularityDataHub;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public final class StellarConnections {

    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int DOWN = 4;
    public static final int UP = 8;
    public static final int DOWN_LEFT = 16;
    public static final int DOWN_RIGHT = 32;
    public static final int UP_LEFT = 64;
    public static final int UP_RIGHT = 128;

    // Increasing U/V directions in vanilla RenderBlocks' unrotated face UVs.
    private static final ForgeDirection[] RIGHT_DIRECTIONS = { ForgeDirection.EAST, ForgeDirection.EAST,
        ForgeDirection.WEST, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.NORTH };
    private static final ForgeDirection[] DOWN_DIRECTIONS = { ForgeDirection.SOUTH, ForgeDirection.SOUTH,
        ForgeDirection.DOWN, ForgeDirection.DOWN, ForgeDirection.DOWN, ForgeDirection.DOWN };

    private StellarConnections() {}

    public static int getMask(IBlockAccess world, int x, int y, int z, int side) {
        if (world == null || side < 0 || side >= ForgeDirection.VALID_DIRECTIONS.length) return 0;
        ForgeDirection face = ForgeDirection.VALID_DIRECTIONS[side];
        String material = getMaterial(world, x, y, z, side);
        if (material == null || !connects(world, x, y, z, face, material)) return 0;
        ForgeDirection right = RIGHT_DIRECTIONS[side];
        ForgeDirection down = DOWN_DIRECTIONS[side];
        int mask = 0;
        if (connects(world, x - right.offsetX, y - right.offsetY, z - right.offsetZ, face, material)) mask |= LEFT;
        if (connects(world, x + right.offsetX, y + right.offsetY, z + right.offsetZ, face, material)) mask |= RIGHT;
        if (connects(world, x + down.offsetX, y + down.offsetY, z + down.offsetZ, face, material)) mask |= DOWN;
        if (connects(world, x - down.offsetX, y - down.offsetY, z - down.offsetZ, face, material)) mask |= UP;
        if (
            (mask & (LEFT | DOWN)) == (LEFT | DOWN) && connects(world, x - right.offsetX + down.offsetX,
                y - right.offsetY + down.offsetY, z - right.offsetZ + down.offsetZ, face, material)
        ) mask |= DOWN_LEFT;
        if (
            (mask & (RIGHT | DOWN)) == (RIGHT | DOWN) && connects(world, x + right.offsetX + down.offsetX,
                y + right.offsetY + down.offsetY, z + right.offsetZ + down.offsetZ, face, material)
        ) mask |= DOWN_RIGHT;
        if (
            (mask & (LEFT | UP)) == (LEFT | UP) && connects(world, x - right.offsetX - down.offsetX,
                y - right.offsetY - down.offsetY, z - right.offsetZ - down.offsetZ, face, material)
        ) mask |= UP_LEFT;
        if (
            (mask & (RIGHT | UP)) == (RIGHT | UP) && connects(world, x + right.offsetX - down.offsetX,
                y + right.offsetY - down.offsetY, z + right.offsetZ - down.offsetZ, face, material)
        ) mask |= UP_RIGHT;
        return mask;
    }

    public static int normalizeMask(int mask) {
        int result = mask & 15;
        if ((result & (LEFT | DOWN)) == (LEFT | DOWN)) result |= mask & DOWN_LEFT;
        if ((result & (RIGHT | DOWN)) == (RIGHT | DOWN)) result |= mask & DOWN_RIGHT;
        if ((result & (LEFT | UP)) == (LEFT | UP)) result |= mask & UP_LEFT;
        if ((result & (RIGHT | UP)) == (RIGHT | UP)) result |= mask & UP_RIGHT;
        return result;
    }

    private static boolean connects(IBlockAccess world, int x, int y, int z, ForgeDirection face, String material) {
        if (!material.equals(getMaterial(world, x, y, z, face.ordinal()))) return false;
        int frontX = x + face.offsetX;
        int frontY = y + face.offsetY;
        int frontZ = z + face.offsetZ;
        Block cover = world.getBlock(frontX, frontY, frontZ);
        if (cover.isOpaqueCube() || world.isSideSolid(frontX, frontY, frontZ, face.getOpposite(), false)) return false;
        return !(world.getBlock(x, y, z) instanceof BlockStellar stellar && stellar.isGlass()
            && material.equals(getMaterial(world, frontX, frontY, frontZ, face.ordinal())));
    }

    private static String getMaterial(IBlockAccess world, int x, int y, int z, int side) {
        Block block = world.getBlock(x, y, z);
        if (block instanceof BlockStellar stellar) {
            StellarBlockVariant variant = stellar.getVariant(world.getBlockMetadata(x, y, z));
            return variant == null ? null : side == 0 || side == 1 ? variant.getEndTexture() : variant.getSideTexture();
        }
        if (block instanceof BlockStellarPanel panel) {
            StellarPanelVariant variant = panel.getVariant(world.getBlockMetadata(x, y, z));
            if (variant == null) return null;
            return variant.getBase().getConnectedTexture();
        }
        if (block != GregTechAPI.sBlockMachines) return null;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGregTechTileEntity base)) return null;
        IMetaTileEntity machine = base.getMetaTileEntity();
        return machine instanceof SingularityDataHub || machine instanceof VaultPortHatch
            ? StellarBlockVariant.EddingtonWallCasing.getSideTexture()
            : null;
    }
}
