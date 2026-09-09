package com.xyp.gtnotgood.common.network;

import java.util.function.Supplier;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

/**
 * One fluid identity sample for MUI2 phantom slots. It never stores or consumes actual container contents.
 * The supplier only returns existing fluid rules, so stale packets cannot create connections or edit other channel
 * types.
 */
final class NetworkFluidFilterTank implements IFluidTank {

    private final TileNetworkController controller;
    private final Supplier<NetworkRule> selected;
    private final int slot;
    private FluidStack client;

    NetworkFluidFilterTank(TileNetworkController controller, Supplier<NetworkRule> selected, int slot) {
        this.controller = controller;
        this.selected = selected;
        this.slot = slot;
    }

    @Override
    public FluidStack getFluid() {
        if (controller.getWorldObj().isRemote) return client;
        NetworkRule rule = selected.get();
        return rule == null ? null : rule.fluidFilter(slot);
    }

    private void sample(FluidStack fluid) {
        FluidStack copy = fluid == null ? null : fluid.copy();
        if (copy != null) copy.amount = 1;
        if (controller.getWorldObj().isRemote) client = copy;
        else {
            NetworkRule rule = selected.get();
            if (rule == null) return;
            rule.fluidFilters[slot] = copy;
            rule.filters[slot] = null;
            controller.markDirty();
        }
    }

    @Override
    public int getFluidAmount() {
        return getFluid() == null ? 0 : 1;
    }

    @Override
    public int getCapacity() {
        return 1;
    }

    @Override
    public FluidTankInfo getInfo() {
        return new FluidTankInfo(getFluid(), 1);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        if (doFill) sample(resource);
        return 1;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        FluidStack fluid = getFluid();
        if (fluid == null || maxDrain <= 0) return null;
        FluidStack copy = fluid.copy();
        copy.amount = 1;
        if (doDrain) sample(null);
        return copy;
    }
}
