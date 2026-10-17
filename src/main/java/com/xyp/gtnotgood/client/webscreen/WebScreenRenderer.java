package com.xyp.gtnotgood.client.webscreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.common.blocks.webscreen.TileWebScreen;
import com.xyp.gtnotgood.common.blocks.webscreen.WebScreenShape;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WebScreenRenderer extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity entity, double x, double y, double z, float partialTick) {
        TileWebScreen tile = (TileWebScreen) entity;
        if (!ClientWebScreens.isRenderAnchor(tile)) return;
        ResourceLocation texture = ClientWebScreens.getTexture(tile);
        if (texture == null) return;
        WebScreenShape shape = ClientWebScreens.getShape(tile);
        float brightnessX = OpenGlHelper.lastBrightnessX;
        float brightnessY = OpenGlHelper.lastBrightnessY;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
        try {
            GL11.glTranslated(x + (shape.facing <= 3 ? shape.width / 2.0 : 0.5), y + shape.height / 2.0,
                z + (shape.facing >= 4 ? shape.width / 2.0 : 0.5));
            int facing = tile.getFacing();
            GL11.glRotatef(facing == 2 ? 180 : facing == 4 ? -90 : facing == 5 ? 90 : 0, 0, 1, 0);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            double panelWidth = shape.width - 0.125;
            double panelHeight = shape.height - 0.125;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(0, 0, 0, 1);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertex(-panelWidth / 2, -panelHeight / 2, 0.501);
            tessellator.addVertex(panelWidth / 2, -panelHeight / 2, 0.501);
            tessellator.addVertex(panelWidth / 2, panelHeight / 2, 0.501);
            tessellator.addVertex(-panelWidth / 2, panelHeight / 2, 0.501);
            tessellator.draw();
            double viewWidth = Math.min(panelWidth, panelHeight * 16 / 9);
            double viewHeight = viewWidth * 9 / 16;
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1, 1, 1, 1);
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(-viewWidth / 2, -viewHeight / 2, 0.5012, 0, 1);
            tessellator.addVertexWithUV(viewWidth / 2, -viewHeight / 2, 0.5012, 1, 1);
            tessellator.addVertexWithUV(viewWidth / 2, viewHeight / 2, 0.5012, 1, 0);
            tessellator.addVertexWithUV(-viewWidth / 2, viewHeight / 2, 0.5012, 0, 0);
            tessellator.draw();
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }
}
