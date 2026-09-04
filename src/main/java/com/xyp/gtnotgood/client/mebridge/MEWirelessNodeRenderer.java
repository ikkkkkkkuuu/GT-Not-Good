package com.xyp.gtnotgood.client.mebridge;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.common.mebridge.ItemMEWirelessTransceiver;
import com.xyp.gtnotgood.common.mebridge.MessageMEWirelessVisualization;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** AE2 Network Visualiser-style overlay restricted to transceiver-linked target nodes. */
@SideOnly(Side.CLIENT)
public final class MEWirelessNodeRenderer {

    private static final long SNAPSHOT_EXPIRY_MS = 5_000L;
    private static final double NODE_HALF_SIZE = 0.22D;
    private static final double BLOCK_INSET = 0.035D;

    private static int dimension = Integer.MIN_VALUE;
    private static int[] positions = new int[0];
    private static long expiresAt;
    private static boolean displayListDirty = true;

    private final int displayList = GL11.glGenLists(1);

    public static void update(int newDimension, int[] newPositions) {
        int[] safePositions = newPositions == null ? new int[0] : newPositions;
        if (dimension != newDimension || !Arrays.equals(positions, safePositions)) displayListDirty = true;
        dimension = newDimension;
        positions = safePositions;
        expiresAt = System.currentTimeMillis() + SNAPSHOT_EXPIRY_MS;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || minecraft.thePlayer == null
            || positions.length == 0
            || dimension != minecraft.theWorld.provider.dimensionId
            || System.currentTimeMillis() > expiresAt) {
            return;
        }

        ItemStack held = minecraft.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemMEWirelessTransceiver)) return;

        EntityPlayerSP player = minecraft.thePlayer;
        double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glTranslated(-cameraX, -cameraY, -cameraZ);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        if (displayListDirty) {
            displayListDirty = false;
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            drawNodeCubes(0.32F);
            GL11.glLineWidth(3.0F);
            drawBlockOutlines(0.95F);
            GL11.glEndList();
        }
        GL11.glCallList(displayList);

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private static void drawNodeCubes(float alpha) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (int index = 0; index + 3 < positions.length; index += MessageMEWirelessVisualization.VALUES_PER_NODE) {
            setColor(tessellator, positions[index + 3], alpha);
            double centerX = positions[index] + 0.5D;
            double centerY = positions[index + 1] + 0.5D;
            double centerZ = positions[index + 2] + 0.5D;
            addCube(
                tessellator,
                centerX - NODE_HALF_SIZE,
                centerY - NODE_HALF_SIZE,
                centerZ - NODE_HALF_SIZE,
                centerX + NODE_HALF_SIZE,
                centerY + NODE_HALF_SIZE,
                centerZ + NODE_HALF_SIZE);
        }
        tessellator.draw();
    }

    private static void drawBlockOutlines(float alpha) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        for (int index = 0; index + 3 < positions.length; index += MessageMEWirelessVisualization.VALUES_PER_NODE) {
            setColor(tessellator, positions[index + 3], alpha);
            double x0 = positions[index] + BLOCK_INSET;
            double y0 = positions[index + 1] + BLOCK_INSET;
            double z0 = positions[index + 2] + BLOCK_INSET;
            addOutline(
                tessellator,
                x0,
                y0,
                z0,
                x0 + 1.0D - BLOCK_INSET * 2.0D,
                y0 + 1.0D - BLOCK_INSET * 2.0D,
                z0 + 1.0D - BLOCK_INSET * 2.0D);
        }
        tessellator.draw();
    }

    private static void setColor(Tessellator tessellator, int rgb, float alpha) {
        tessellator
            .setColorRGBA_F(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, alpha);
    }

    private static void addCube(Tessellator tessellator, double x0, double y0, double z0, double x1, double y1,
        double z1) {
        quad(tessellator, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(tessellator, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        quad(tessellator, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(tessellator, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        quad(tessellator, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        quad(tessellator, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
    }

    private static void quad(Tessellator tessellator, double ax, double ay, double az, double bx, double by, double bz,
        double cx, double cy, double cz, double dx, double dy, double dz) {
        tessellator.addVertex(ax, ay, az);
        tessellator.addVertex(bx, by, bz);
        tessellator.addVertex(cx, cy, cz);
        tessellator.addVertex(dx, dy, dz);
    }

    private static void addOutline(Tessellator tessellator, double x0, double y0, double z0, double x1, double y1,
        double z1) {
        edge(tessellator, x0, y0, z0, x1, y0, z0);
        edge(tessellator, x1, y0, z0, x1, y0, z1);
        edge(tessellator, x1, y0, z1, x0, y0, z1);
        edge(tessellator, x0, y0, z1, x0, y0, z0);
        edge(tessellator, x0, y1, z0, x1, y1, z0);
        edge(tessellator, x1, y1, z0, x1, y1, z1);
        edge(tessellator, x1, y1, z1, x0, y1, z1);
        edge(tessellator, x0, y1, z1, x0, y1, z0);
        edge(tessellator, x0, y0, z0, x0, y1, z0);
        edge(tessellator, x1, y0, z0, x1, y1, z0);
        edge(tessellator, x1, y0, z1, x1, y1, z1);
        edge(tessellator, x0, y0, z1, x0, y1, z1);
    }

    private static void edge(Tessellator tessellator, double ax, double ay, double az, double bx, double by,
        double bz) {
        tessellator.addVertex(ax, ay, az);
        tessellator.addVertex(bx, by, bz);
    }
}
