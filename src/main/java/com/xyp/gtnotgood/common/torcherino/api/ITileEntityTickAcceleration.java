package com.xyp.gtnotgood.common.torcherino.api;

/**
 * Opt-in hook for tile entities that can be accelerated more safely than by repeatedly calling
 * {@link net.minecraft.tileentity.TileEntity#updateEntity()}.
 */
public interface ITileEntityTickAcceleration {

    /**
     * Applies acceleration to the target tile entity.
     *
     * @param acceleratedTicks extra progress ticks requested by the Torcherino
     * @return {@code true} when the implementation handled the acceleration, or {@code false} to let the generic
     *         fallback tick the tile entity directly
     */
    boolean tickAcceleration(int acceleratedTicks);

}
