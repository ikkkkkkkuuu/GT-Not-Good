package com.xyp.gtnotgood.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.common.items.compass.HiveMarkers;
import com.xyp.gtnotgood.common.items.compass.StructureCompassItem;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class StructureCompassRenderer implements IItemRenderer {

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return type != ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return helper == ItemRendererHelper.ENTITY_BOBBING || helper == ItemRendererHelper.ENTITY_ROTATION;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            if (type != ItemRenderType.INVENTORY) {
                if (type == ItemRenderType.ENTITY) GL11.glTranslatef(-.5f, .5f, 0);
                else GL11.glTranslatef(0, 1, 0);
                GL11.glScalef(1f / 16, -1f / 16, 1f / 16);
            }
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            // The face is visible from both sides in first person. Do not let its base occlude its own needle.
            GL11.glDepthMask(false);
            Tessellator face = Tessellator.instance;
            face.startDrawingQuads();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    double radius = Math.hypot(x - 7.5, y - 7.5);
                    if (radius > 7.8) continue;
                    if (radius > 6.5) face.setColorOpaque_F(.72f, .55f, .22f);
                    else face.setColorOpaque_F(.12f, .17f, .20f);
                    face.addVertex(x, y, 0);
                    face.addVertex(x + 1, y, 0);
                    face.addVertex(x + 1, y + 1, 0);
                    face.addVertex(x, y + 1, 0);
                }
            }
            face.draw();
            GL11.glColor3f(.87f, .85f, .68f);
            quad(7, 2, 9, 3, .01);
            quad(7, 13, 9, 14, .01);
            quad(2, 7, 3, 9, .01);
            quad(13, 7, 14, 9, .01);
            Minecraft mc = Minecraft.getMinecraft();
            NBTTagCompound tag = stack.getTagCompound();
            boolean active = mc.thePlayer != null && mc.theWorld != null
                && tag != null
                && tag.getBoolean("Found")
                && mc.thePlayer.dimension == tag.getInteger("Dimension")
                && (StructureCompassItem.mode(stack) != 0 || isHiveTarget(mc, tag));
            double angle = 0;
            if (active) {
                double dx = tag.getInteger("X") + .5 - mc.thePlayer.posX;
                double dz = tag.getInteger("Z") + .5 - mc.thePlayer.posZ;
                angle = Math.toDegrees(Math.atan2(-dx, dz)) - mc.thePlayer.rotationYaw;
            }
            GL11.glTranslatef(8, 8, .02f);
            GL11.glRotatef((float) angle, 0, 0, 1);
            GL11.glColor3f(.64f, .69f, .72f);
            triangle(0, 5, -1.5, 0, 1.5, 0);
            if (!active) GL11.glColor3f(.4f, .4f, .4f);
            else if (!tag.getBoolean("Confirmed")) GL11.glColor3f(1, .7f, .15f);
            else if (StructureCompassItem.mode(stack) == 0) GL11.glColor3f(1, .8f, .2f);
            else GL11.glColor3f(.2f, .9f, .95f);
            triangle(0, -5, 1.5, 0, -1.5, 0);
            GL11.glColor3f(1, .92f, .64f);
            quad(-.7, -.7, .7, .7, .01);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null || mc.renderViewEntity == null) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof StructureCompassItem) || StructureCompassItem.mode(held) != 0)
            return;
        NBTTagCompound tag = held.getTagCompound();
        if (
            tag == null || !tag.getBoolean("Found")
                || !tag.getBoolean("Confirmed")
                || tag.getInteger("Dimension") != mc.thePlayer.dimension
                || !isHiveTarget(mc, tag)
        ) return;

        int x = tag.getInteger("X"), y = tag.getInteger("Y"), z = tag.getInteger("Z");
        Entity camera = mc.renderViewEntity;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(-(camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks),
                -(camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks),
                -(camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks));
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(2.5F);
            GL11.glColor4f(1, .8f, .2f, .95f);
            RenderGlobal.drawOutlinedBoundingBox(
                AxisAlignedBB.getBoundingBox(x - .005, y - .005, z - .005, x + 1.005, y + 1.005, z + 1.005), -1);
            Tessellator marker = Tessellator.instance;
            marker.startDrawing(GL11.GL_LINES);
            marker.setColorRGBA_F(1, .8f, .2f, .7f);
            marker.addVertex(x + .5, y + 1.05, z + .5);
            marker.addVertex(x + .5, mc.theWorld.getActualHeight() + 16, z + .5);
            marker.draw();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static boolean isHiveTarget(Minecraft mc, NBTTagCompound tag) {
        if (!tag.getBoolean("HiveTarget")) return false;
        int x = tag.getInteger("X"), y = tag.getInteger("Y"), z = tag.getInteger("Z");
        Chunk chunk = mc.theWorld.getChunkFromChunkCoords(x >> 4, z >> 4);
        return y >= 0 && y < mc.theWorld.getActualHeight()
            && (chunk instanceof EmptyChunk || HiveMarkers.isHive(chunk.getBlock(x & 15, y, z & 15)));
    }

    private static void quad(double x, double y, double right, double bottom, double z) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertex(x, y, z);
        tessellator.addVertex(right, y, z);
        tessellator.addVertex(right, bottom, z);
        tessellator.addVertex(x, bottom, z);
        tessellator.draw();
    }

    private static void triangle(double x, double y, double x2, double y2, double x3, double y3) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_TRIANGLES);
        tessellator.addVertex(x, y, 0);
        tessellator.addVertex(x2, y2, 0);
        tessellator.addVertex(x3, y3, 0);
        tessellator.draw();
    }
}
