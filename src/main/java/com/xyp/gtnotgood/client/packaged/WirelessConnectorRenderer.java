// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from AE2 Lightning Tech's WirelessConnectorRenderer (upstream authors retained in NOTICE.md).
// GTNG port modifications (c) 2026 GTNG contributors.
package com.xyp.gtnotgood.client.packaged;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.common.packaged.ItemWirelessConnector;
import com.xyp.gtnotgood.common.packaged.PackagedTarget;
import com.xyp.gtnotgood.common.packaged.TilePackagedProvider;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Ports LT's held-connector overlay to Forge 1.7.10: blue bindings, yellow selection/preview,
 * an occluded inner host cube and lines ending at bound face centers. Only loaded hosts are inspected.
 * Source revision and geometry/render-state adaptations are recorded in CODE_PORT_NOTES.md.
 */
@SideOnly(Side.CLIENT)
public final class WirelessConnectorRenderer {

    private static final int BLUE_FACE = 0x600080FF;
    private static final int BLUE_LINE = 0xC00080FF;
    private static final int YELLOW_FACE = 0x60FFFF00;
    private static final int YELLOW_LINE = 0xC0FFFF00;
    private final List<PackagedTarget> hosts = new ArrayList<>();
    private final List<TilePackagedProvider> visible = new ArrayList<>();
    private World cachedWorld;
    private long nextScan;

    /** Releases cached client-world references immediately on disconnect, even without another rendered frame. */
    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.world == cachedWorld) {
            cachedWorld = null;
            hosts.clear();
            visible.clear();
            nextScan = 0;
        }
    }

    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world != cachedWorld) {
            cachedWorld = world;
            hosts.clear();
            nextScan = 0;
        }
        visible.clear();
        if (world == null || mc.thePlayer == null || mc.renderViewEntity == null) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemWirelessConnector)) return;
        PackagedTarget selected = ItemWirelessConnector.selection(held);
        if (selected != null) {
            if (selected.resolve(world) instanceof TilePackagedProvider provider) visible.add(provider);
        } else {
            if (world.getTotalWorldTime() >= nextScan) {
                nextScan = world.getTotalWorldTime() + 4;
                hosts.clear();
                int minX = ((int) Math.floor(mc.thePlayer.posX) - 64) >> 4;
                int maxX = ((int) Math.floor(mc.thePlayer.posX) + 64) >> 4;
                int minZ = ((int) Math.floor(mc.thePlayer.posZ) - 64) >> 4;
                int maxZ = ((int) Math.floor(mc.thePlayer.posZ) + 64) >> 4;
                for (Object tile : world.loadedTileEntityList) {
                    if (tile instanceof TilePackagedProvider provider && !provider.isInvalid()
                        && (provider.xCoord >> 4) >= minX
                        && (provider.xCoord >> 4) <= maxX
                        && (provider.zCoord >> 4) >= minZ
                        && (provider.zCoord >> 4) <= maxZ) {
                        hosts.add(
                            new PackagedTarget(
                                world.provider.dimensionId,
                                provider.xCoord,
                                provider.yCoord,
                                provider.zCoord,
                                0));
                    }
                }
            }
            for (PackagedTarget host : hosts) {
                if (host.resolve(world) instanceof TilePackagedProvider provider && !provider.isInvalid())
                    visible.add(provider);
            }
        }
        if (visible.isEmpty()) return;
        PackagedTarget preview = selected == null ? null : preview(mc, visible.get(0));
        Entity camera = mc.renderViewEntity;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(
                -(camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks),
                -(camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks),
                -(camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks));
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDepthFunc(GL11.GL_GREATER);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            color(t, selected == null ? 0x800080FF : 0x80FFFF00);
            for (TilePackagedProvider provider : visible) {
                for (int face = 0; face < 6; face++) {
                    quad(t, provider.xCoord + .25, provider.yCoord + .25, provider.zCoord + .25, .5, face, 0);
                }
            }
            t.draw();
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            // AE OverlayRenderType's crumbling blend for highlighted faces.
            GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
            t.startDrawingQuads();
            color(t, BLUE_FACE);
            for (TilePackagedProvider provider : visible) {
                for (PackagedTarget target : provider.connections()) {
                    if (target.dimension == world.provider.dimensionId)
                        quad(t, target.x, target.y, target.z, 1, target.face, .001);
                }
            }
            if (preview != null) {
                color(t, YELLOW_FACE);
                quad(t, preview.x, preview.y, preview.z, 1, preview.face, .001);
            }
            t.draw();
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            GL11.glDepthMask(true);
            GL11.glLineWidth(3);
            t.startDrawing(GL11.GL_LINES);
            color(t, BLUE_LINE);
            for (TilePackagedProvider provider : visible) {
                for (PackagedTarget target : provider.connections()) {
                    if (target.dimension == world.provider.dimensionId) line(t, provider, target);
                }
            }
            if (preview != null) {
                color(t, YELLOW_LINE);
                line(t, visible.get(0), preview);
            }
            t.draw();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    /** Matches direct binding: an already-bound block cannot acquire another lane through a different face. */
    private static PackagedTarget preview(Minecraft mc, TilePackagedProvider provider) {
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
            || hit.sideHit < 0
            || hit.sideHit > 5) return null;
        PackagedTarget target = new PackagedTarget(
            mc.theWorld.provider.dimensionId,
            hit.blockX,
            hit.blockY,
            hit.blockZ,
            hit.sideHit);
        if (target.resolve(mc.theWorld) == null || target.resolve(mc.theWorld) == provider
            || provider.connections()
                .size() >= TilePackagedProvider.MAX_TARGETS)
            return null;
        for (PackagedTarget bound : provider.connections()) if (bound.sameBlock(target)) return null;
        return target;
    }

    private static void color(Tessellator t, int argb) {
        t.setColorRGBA(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24);
    }

    private static void line(Tessellator t, TilePackagedProvider provider, PackagedTarget target) {
        ForgeDirection direction = ForgeDirection.getOrientation(target.face);
        t.addVertex(provider.xCoord + .5, provider.yCoord + .5, provider.zCoord + .5);
        t.addVertex(
            target.x + .5 + direction.offsetX * .501,
            target.y + .5 + direction.offsetY * .501,
            target.z + .5 + direction.offsetZ * .501);
    }

    /** Shared axis-aligned face geometry preserves upstream's 0.001-block surface offset. Culling is disabled. */
    private static void quad(Tessellator t, double x, double y, double z, double size, int face, double epsilon) {
        double X = x + size, Y = y + size, Z = z + size;
        switch (face) {
            case 0, 1 -> {
                double plane = face == 0 ? y - epsilon : Y + epsilon;
                t.addVertex(x, plane, z);
                t.addVertex(X, plane, z);
                t.addVertex(X, plane, Z);
                t.addVertex(x, plane, Z);
            }
            case 2, 3 -> {
                double plane = face == 2 ? z - epsilon : Z + epsilon;
                t.addVertex(x, y, plane);
                t.addVertex(x, Y, plane);
                t.addVertex(X, Y, plane);
                t.addVertex(X, y, plane);
            }
            case 4, 5 -> {
                double plane = face == 4 ? x - epsilon : X + epsilon;
                t.addVertex(plane, y, z);
                t.addVertex(plane, Y, z);
                t.addVertex(plane, Y, Z);
                t.addVertex(plane, y, Z);
            }
        }
    }
}
