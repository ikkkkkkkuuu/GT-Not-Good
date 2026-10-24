package com.xyp.gtnotgood.common.items.multiblockterminal;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalHintProbe.Hint;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalStructureCapture.Position;

final class TerminalFluidOperations {

    private TerminalFluidOperations() {}

    static boolean handles(Hint hint) {
        return hint != null && fluid(hint) != null;
    }

    static boolean place(Position position, Hint hint, ItemStack trigger, EntityPlayerMP player,
        TerminalItemSource source) {
        World world = position.world();
        if (
            !world.isAirBlock(position.x(), position.y(), position.z())
                || !world.canMineBlock(player, position.x(), position.y(), position.z())
                || !player.canPlayerEdit(position.x(), position.y(), position.z(), 1, trigger)
        ) return false;
        FluidStack reserved = player.capabilities.isCreativeMode ? null
            : source.takeFluid(new FluidStack(fluid(hint), FluidContainerRegistry.BUCKET_VOLUME), false);
        if (
            !player.capabilities.isCreativeMode
                && (reserved == null || reserved.amount != FluidContainerRegistry.BUCKET_VOLUME)
        ) {
            if (reserved != null) source.giveFluid(reserved, false);
            return false;
        }
        BlockSnapshot before = BlockSnapshot.getBlockSnapshot(world, position.x(), position.y(), position.z());
        boolean placed = false;
        try {
            boolean changed = world.setBlock(position.x(), position.y(), position.z(), hint.block(), hint.meta(), 3);
            if (changed)
                changed = !ForgeEventFactory.onPlayerBlockPlace(player, before, ForgeDirection.UP).isCanceled();
            placed = changed;
            return placed;
        } finally {
            if (!placed) {
                TerminalPlacement.restore(before, world);
                if (reserved != null) source.giveFluid(reserved, false);
            }
        }
    }

    static boolean dismantle(Position position, Hint hint, EntityPlayerMP player, TerminalItemSource source) {
        World world = position.world();
        if (
            world.getBlock(position.x(), position.y(), position.z()) != hint.block()
                || world.getBlockMetadata(position.x(), position.y(), position.z()) != hint.meta()
        ) return false;
        FluidStack contents = hint.block() instanceof IFluidBlock block
            ? block.drain(world, position.x(), position.y(), position.z(), false)
            : new FluidStack(fluid(hint), FluidContainerRegistry.BUCKET_VOLUME);
        if (contents == null || contents.amount <= 0) return false;
        boolean removed = TerminalBlockRecovery.harvest(player, world, position.x(), position.y(), position.z(),
            source);
        if (removed && !player.capabilities.isCreativeMode) source.giveFluid(contents, false);
        return removed;
    }

    private static Fluid fluid(Hint hint) {
        return hint.block() instanceof IFluidBlock block ? block.getFluid()
            : FluidRegistry.lookupFluidForBlock(hint.block());
    }
}
