// Native render layout adapted from AE2FluidCraft 1.5.110-gtnh, LGPL-3.0-or-later; see META-INF/large-interface-port.
package com.xyp.gtnotgood.client.largeinterface;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.largeinterface.BlockLargeInterface;
import com.xyp.gtnotgood.common.blocks.largeinterface.TileLargeInterface;

import appeng.api.util.AEColor;
import appeng.client.render.BaseBlockRender;
import appeng.client.render.BlockRenderInfo;

/** Uses native orientation and runtime AE2FC textures, with orange as the unpainted color. */
public final class LargeInterfaceRenderer extends BaseBlockRender<BlockLargeInterface, TileLargeInterface> {

    public LargeInterfaceRenderer() {
        super(false, 20);
    }

    @Override
    public boolean renderInWorld(BlockLargeInterface block, IBlockAccess world, int x, int y, int z,
        RenderBlocks renderer) {
        TileLargeInterface tile = block.getTileEntity(world, x, y, z);
        BlockRenderInfo info = block.getRendererInstance();
        if (tile != null && tile.getForward() != ForgeDirection.UNKNOWN) {
            AEColor color = tile.getColor();
            IIcon face = block.getInterfaceIcon(0, color);
            IIcon back = block.getInterfaceIcon(1, color);
            IIcon edge = block.getInterfaceIcon(2, color);
            info.setTemporaryRenderIcons(back, face, edge, edge, edge, edge);
        }
        try {
            return super.renderInWorld(block, world, x, y, z, renderer);
        } finally {
            info.setTemporaryRenderIcon(null);
        }
    }
}
