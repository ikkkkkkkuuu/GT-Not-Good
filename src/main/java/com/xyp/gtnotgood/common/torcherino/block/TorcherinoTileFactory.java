package com.xyp.gtnotgood.common.torcherino.block;

import net.minecraft.tileentity.TileEntity;

/**
 * Creates the tile entity backing one Torcherino block tier.
 * <p>
 * This tiny project-local factory keeps block registration compatible with the Minecraft 1.7.10 code style while still
 * avoiding reflection in {@link BlockTorcherino} and {@link BlockWirelessTorcherino}.
 */
public interface TorcherinoTileFactory {

    /**
     * Creates a fresh tile entity for a newly placed Torcherino block.
     *
     * @return new tile entity instance
     */
    TileEntity create();
}
