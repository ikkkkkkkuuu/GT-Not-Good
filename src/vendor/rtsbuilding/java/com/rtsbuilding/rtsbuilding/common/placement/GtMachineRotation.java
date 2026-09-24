package com.rtsbuilding.rtsbuilding.common.placement;

import com.rtsbuilding.rtsbuilding.platform.math.BlockPos;
import com.rtsbuilding.rtsbuilding.platform.math.EnumFacing;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** GT rotation uses the existing machine entity and its legal facings, never block metadata. */
public final class GtMachineRotation {
    private GtMachineRotation() {}

    /** Resolves a legal quarter turn without mutating client or server state. */
    public static ForgeDirection target(World world, BlockPos pos, EnumFacing axis, int turns) {
        if (world == null || pos == null || axis == null || Math.abs(turns) != 1) return ForgeDirection.UNKNOWN;
        TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        if (!(tile instanceof IGregTechTileEntity)) return ForgeDirection.UNKNOWN;
        IGregTechTileEntity machine = (IGregTechTileEntity) tile;
        if (machine.getMetaTileEntity() == null) return ForgeDirection.UNKNOWN;
        ForgeDirection current = machine.getFrontFacing();
        if (current == ForgeDirection.UNKNOWN) return ForgeDirection.UNKNOWN;
        EnumFacing rotated = PlacedBlockRotationStep.rotateDirection(EnumFacing.byIndex(current.ordinal()), axis, turns);
        ForgeDirection next = ForgeDirection.getOrientation(rotated.getIndex());
        return next != current && machine.isValidFacing(next) ? next : ForgeDirection.UNKNOWN;
    }

    /** Applies only a legal server-side turn through GT's notification-aware facing setter. */
    public static boolean apply(World world, BlockPos pos, EnumFacing axis, int turns) {
        if (world == null || world.isRemote) return false;
        ForgeDirection next = target(world, pos, axis, turns);
        if (next == ForgeDirection.UNKNOWN) return false;
        IGregTechTileEntity machine = (IGregTechTileEntity) world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        machine.setFrontFacing(next);
        ((TileEntity) machine).markDirty();
        return machine.getFrontFacing() == next;
    }
}
