package com.xyp.gtnotgood.common.blocks.largeinterface;

import net.minecraft.block.Block;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;

import appeng.block.AEBaseItemBlock;

/** Keeps native placement, wrench and memory-card behavior for this mod's AE block. */
public final class ItemBlockLargeInterface extends AEBaseItemBlock {

    public ItemBlockLargeInterface(Block block) {
        super(block);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
    }
}
