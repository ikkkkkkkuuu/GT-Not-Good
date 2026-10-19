package com.xyp.gtnotgood.common.api.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraftforge.fluids.FluidStack;

public final class FluidEntryInfo {

    public final FluidStack fluid;
    public final double weight;
    public final List<String> dimAbbrs;
    public final int minAmount;
    public final int maxAmount;
    public final boolean filtered;
    public final boolean aimed;

    public FluidEntryInfo(FluidStack fluid, double weight, List<String> dimAbbrs, int minAmount, int maxAmount,
        boolean filtered, boolean aimed) {
        this.fluid = fluid == null ? null : fluid.copy();
        this.weight = weight;
        this.dimAbbrs = Collections.unmodifiableList(new ArrayList<>(dimAbbrs));
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.filtered = filtered;
        this.aimed = aimed;
    }
}
