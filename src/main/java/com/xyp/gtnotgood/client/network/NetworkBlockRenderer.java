package com.xyp.gtnotgood.client.network;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.common.network.BlockNetwork;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

/** Minecraft 1.7.10 cuboid renderer for the XNet-textured cable core and six connected arms. */
public final class NetworkBlockRenderer implements ISimpleBlockRenderingHandler {

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        renderer.setRenderBounds(0.3125, 0.3125, 0, 0.6875, 0.6875, 1);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setNormal(0, -1, 0);
        renderer.renderFaceYNeg(block, 0, 0, 0, block.getIcon(0, 0));
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0, 1, 0);
        renderer.renderFaceYPos(block, 0, 0, 0, block.getIcon(1, 0));
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0, 0, -1);
        renderer.renderFaceZNeg(block, 0, 0, 0, block.getIcon(2, 0));
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0, 0, 1);
        renderer.renderFaceZPos(block, 0, 0, 0, block.getIcon(3, 0));
        t.draw();
        t.startDrawingQuads();
        t.setNormal(-1, 0, 0);
        renderer.renderFaceXNeg(block, 0, 0, 0, block.getIcon(4, 0));
        t.draw();
        t.startDrawingQuads();
        t.setNormal(1, 0, 0);
        renderer.renderFaceXPos(block, 0, 0, 0, block.getIcon(5, 0));
        t.draw();
        renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        BlockNetwork network = (BlockNetwork) block;
        double low = network.kind == BlockNetwork.CONNECTOR ? 0.25 : 0.3125;
        double high = 1 - low;
        renderer.setRenderBounds(low, low, low, high, high, high);
        renderer.renderStandardBlock(block, x, y, z);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (!network.connects(world, x, y, z, direction)) continue;
            renderer.setRenderBounds(
                direction.offsetX < 0 ? 0 : direction.offsetX > 0 ? high : 0.3125,
                direction.offsetY < 0 ? 0 : direction.offsetY > 0 ? high : 0.3125,
                direction.offsetZ < 0 ? 0 : direction.offsetZ > 0 ? high : 0.3125,
                direction.offsetX > 0 ? 1 : direction.offsetX < 0 ? low : 0.6875,
                direction.offsetY > 0 ? 1 : direction.offsetY < 0 ? low : 0.6875,
                direction.offsetZ > 0 ? 1 : direction.offsetZ < 0 ? low : 0.6875);
            renderer.renderStandardBlock(block, x, y, z);
        }
        renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return BlockNetwork.cableRenderId;
    }
}
