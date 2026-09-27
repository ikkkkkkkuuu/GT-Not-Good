package com.xyp.gtnotgood.common.machines.hatch.me;

import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.interfaces.IOutputBus;
import gregtech.api.interfaces.IOutputBusTransaction;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.IOutputTransaction;
import gregtech.api.util.GTUtility;

/**
 * Guards every transaction path, including GT's early recipe-check branches which bypass ordinary filtering.
 * Delegates capacity, void protection and commit semantics unchanged to the native ME transaction.
 */
abstract class FilteredMEOutputTransaction<ID, T>
    implements IOutputTransaction<ID, T>, IOutputTransaction.IRecipeCheckAware, IOutputTransaction.IProtectOutputAware {

    private final IOutputTransaction<ID, T> delegate;
    private final Predicate<T> accepts;

    FilteredMEOutputTransaction(IOutputTransaction<ID, T> delegate, Predicate<T> accepts) {
        this.delegate = delegate;
        this.accepts = accepts;
    }

    @Override
    public boolean isFiltered() {
        return delegate.isFiltered();
    }

    @Override
    public boolean isFilteredTo(ID id) {
        return delegate.isFilteredTo(id);
    }

    @Override
    public boolean hasAvailableSpace() {
        return delegate.hasAvailableSpace();
    }

    @Override
    public boolean storePartial(ID id, T stack, long totalPerParallel, long perParallel) {
        return accepts.test(stack) && delegate.storePartial(id, stack, totalPerParallel, perParallel);
    }

    @Override
    public void complete(ID id) {
        delegate.complete(id);
    }

    @Override
    public void commit() {
        delegate.commit();
    }

    @Override
    public boolean needsTotalParallelData() {
        return delegate.needsTotalParallelData();
    }

    @Override
    public void setRecipeCheck(boolean checking) {
        if (delegate instanceof IRecipeCheckAware aware) aware.setRecipeCheck(checking);
    }

    @Override
    public void setProtectOutput(boolean protect) {
        if (delegate instanceof IProtectOutputAware aware) aware.setProtectOutput(protect);
    }

    /** Preserves the owning bus for GregTech's output ordering and matching. */
    static final class Items extends FilteredMEOutputTransaction<GTUtility.ItemId, ItemStack>
        implements IOutputBusTransaction {

        private final IOutputBusTransaction bus;

        Items(IOutputBusTransaction bus, Predicate<ItemStack> accepts) {
            super(bus, accepts);
            this.bus = bus;
        }

        @Override
        public IOutputBus getBus() {
            return bus.getBus();
        }
    }

    /** Preserves the owning hatch for GregTech's output ordering and matching. */
    static final class Fluids extends FilteredMEOutputTransaction<GTUtility.FluidId, FluidStack>
        implements IOutputHatchTransaction {

        private final IOutputHatchTransaction hatch;

        Fluids(IOutputHatchTransaction hatch, Predicate<FluidStack> accepts) {
            super(hatch, accepts);
            this.hatch = hatch;
        }

        @Override
        public IOutputHatch getHatch() {
            return hatch.getHatch();
        }
    }
}
