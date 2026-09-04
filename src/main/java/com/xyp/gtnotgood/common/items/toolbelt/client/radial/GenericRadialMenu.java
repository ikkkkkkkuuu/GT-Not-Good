package com.xyp.gtnotgood.common.items.toolbelt.client.radial;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.common.items.toolbelt.ConfigData;

public class GenericRadialMenu extends Gui {

    private final Minecraft mc;
    private final IRadialMenuHost host;
    private final List<RadialMenuItem> menuItems = new ArrayList<>();
    private RadialMenuItem itemHovering;
    private float animProgress;
    private float animTarget;
    private boolean closing;
    private boolean ready;

    public GenericRadialMenu(Minecraft mc, IRadialMenuHost host) {
        this.mc = mc;
        this.host = host;
        this.itemHovering = null;
    }

    public void clear() {
        menuItems.clear();
        itemHovering = null;
    }

    public void add(RadialMenuItem item) {
        menuItems.add(item);
    }

    public void tick() {
        if (closing) {
            animTarget = 0.0f;
            if (animProgress <= 0.01f) {
                animProgress = 0.0f;
                ready = false;
            }
        } else {
            animTarget = 1.0f;
            if (animProgress >= 0.99f) {
                animProgress = 1.0f;
                ready = true;
            }
        }

        float diff = animTarget - animProgress;
        animProgress += diff * 0.4f; // Smooth animation

        if (Math.abs(diff) < 0.001f) {
            animProgress = animTarget;
        }
    }

    public void close() {
        closing = true;
    }

    public boolean isClosed() {
        return closing && animProgress <= 0.001f;
    }

    public boolean isReady() {
        return ready;
    }

    public void draw(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        if (animProgress <= 0.0f) return;

        // Count visible items -- all visible items dynamically share the circle equally
        int visibleCount = 0;
        for (RadialMenuItem item : menuItems) {
            if (item.isVisible()) visibleCount++;
        }

        if (visibleCount == 0) return;

        float radiusIn = 20;
        float radiusOut = 60;
        float radiusMid = (radiusIn + radiusOut) / 2;

        float startAngle = -90.0f; // Start from top
        float anglePerItem = 360.0f / visibleCount;

        // Draw full-screen dark background overlay behind the radial menu
        drawDarkOverlay(sr, centerX, centerY, radiusOut);

        // === PHASE 1: Detect hover BEFORE drawing wedges ===
        // Angle-only selection: any mouse position past the deadzone selects the
        // wedge in that direction. No upper-radius limit — the cursor never needs
        // to be inside the ring.
        itemHovering = null;
        int visibleIndex = 0;
        for (int i = 0; i < menuItems.size(); i++) {
            RadialMenuItem item = menuItems.get(i);
            if (!item.isVisible()) continue;

            float angle = startAngle + anglePerItem * visibleIndex;

            if (isPointInWedge(
                mouseX,
                mouseY,
                centerX,
                centerY,
                radiusIn * animProgress,
                angle - anglePerItem / 2,
                angle + anglePerItem / 2)) {
                itemHovering = item;
            }

            visibleIndex++;
        }

        // === PHASE 2: Draw wedges with proper GL state ===
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);

        // Draw wedges: visible items packed consecutively, all same angular size
        visibleIndex = 0;
        for (int i = 0; i < menuItems.size(); i++) {
            RadialMenuItem item = menuItems.get(i);
            if (!item.isVisible()) continue;

            float angle = startAngle + anglePerItem * visibleIndex;

            // Draw background wedge: highlight (0x80FFFFFF) when hovered, normal (0x60000000) otherwise
            drawWedge(
                centerX,
                centerY,
                radiusIn * animProgress,
                radiusOut * animProgress,
                angle - anglePerItem / 2,
                angle + anglePerItem / 2,
                i == getItemIndex(itemHovering) ? 0x80FFFFFF : 0x60000000);

            visibleIndex++;
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();

        // === PHASE 3: Draw items and tooltips ===
        visibleIndex = 0;
        for (int i = 0; i < menuItems.size(); i++) {
            RadialMenuItem item = menuItems.get(i);
            if (!item.isVisible()) continue;

            float angle = startAngle + anglePerItem * visibleIndex;
            float rad = (float) Math.toRadians(angle);

            float cx = centerX + MathHelper.cos(rad) * radiusMid * animProgress;
            float cy = centerY + MathHelper.sin(rad) * radiusMid * animProgress;

            DrawingContext context = new DrawingContext(cx, cy, zLevel, host.getFontRenderer(), this);

            GL11.glPushMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, (animProgress < 0.5f ? animProgress * 2 : 1.0f));
            item.draw(context);
            GL11.glPopMatrix();

            // Draw tooltip for hovered item (hover was already detected in Phase 1)
            if (item == itemHovering) {
                item.drawTooltip(context);
            }

            visibleIndex++;
        }

        // Draw center circle with more visible background
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_TRIANGLE_FAN);
        tessellator.setColorRGBA_F(0.0f, 0.0f, 0.0f, 0.7f * animProgress);
        tessellator.addVertex(centerX, centerY, zLevel);
        float innerRadius = ConfigData.minecraftHasNoCircles ? radiusIn * 1.3f : radiusIn;
        int segments = ConfigData.minecraftHasNoCircles ? 4 : 36;
        for (int i = 0; i <= segments; i++) {
            double angle = i * 2.0 * Math.PI / segments;
            float x = centerX + (float) Math.cos(angle) * innerRadius * animProgress;
            float y = centerY + (float) Math.sin(angle) * innerRadius * animProgress;
            tessellator.addVertex(x, y, zLevel);
        }
        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    /**
     * Draw a semi-transparent dark overlay behind the radial menu area.
     * This creates the "GUI background" effect that darkens the game world.
     */
    private void drawDarkOverlay(ScaledResolution sr, int centerX, int centerY, float radiusOut) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        Tessellator tessellator = Tessellator.instance;
        float alpha = 0.4f * animProgress;
        float expandedRadius = radiusOut * animProgress + 30;

        // Draw a large filled circle as background
        tessellator.startDrawing(GL11.GL_TRIANGLE_FAN);
        tessellator.setColorRGBA_F(0.0f, 0.0f, 0.0f, alpha);
        tessellator.addVertex(centerX, centerY, zLevel);
        int segments = 48;
        for (int i = 0; i <= segments; i++) {
            double angle = i * 2.0 * Math.PI / segments;
            float x = centerX + (float) Math.cos(angle) * expandedRadius;
            float y = centerY + (float) Math.sin(angle) * expandedRadius;
            tessellator.addVertex(x, y, zLevel);
        }
        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void drawWedge(float centerX, float centerY, float radiusIn, float radiusOut, float startAngle,
        float endAngle, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.instance;
        int segments = ConfigData.minecraftHasNoCircles ? 4 : 12;
        float angleRange = endAngle - startAngle;

        // Use GL_QUADS: each segment draws a single quad (outer1鈫抜nner1鈫抜nner2鈫抩uter2).
        // This matches BlitPieArc from ToolBelt-master and avoids the alternating
        // winding-order problem of GL_TRIANGLE_STRIP which makes half the triangles
        // invisible when face culling is enabled.
        tessellator.startDrawing(GL11.GL_QUADS);
        tessellator.setColorRGBA_F(r, g, b, a);

        for (int i = 0; i < segments; i++) {
            float angle1 = startAngle + angleRange * i / segments;
            float angle2 = startAngle + angleRange * (i + 1) / segments;

            float rad1 = (float) Math.toRadians(angle1);
            float rad2 = (float) Math.toRadians(angle2);

            float cos1 = MathHelper.cos(rad1);
            float sin1 = MathHelper.sin(rad1);
            float cos2 = MathHelper.cos(rad2);
            float sin2 = MathHelper.sin(rad2);

            // Quad vertices: outer1, inner1, inner2, outer2
            tessellator.addVertex(centerX + cos1 * radiusOut, centerY + sin1 * radiusOut, zLevel);
            tessellator.addVertex(centerX + cos1 * radiusIn, centerY + sin1 * radiusIn, zLevel);
            tessellator.addVertex(centerX + cos2 * radiusIn, centerY + sin2 * radiusIn, zLevel);
            tessellator.addVertex(centerX + cos2 * radiusOut, centerY + sin2 * radiusOut, zLevel);
        }
        tessellator.draw();
    }

    private boolean isPointInWedge(int px, int py, float cx, float cy, float rIn, float startAngle, float endAngle) {
        float dx = px - cx;
        float dy = py - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // Only deadzone check — no upper limit. Any direction past the inner radius
        // selects the wedge at that angle (angle-only selection).
        float effectiveRIn = rIn + ConfigData.radialDeadzoneOffset;
        if (dist < effectiveRIn) return false;

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        startAngle = normalizeAngle(startAngle);
        endAngle = normalizeAngle(endAngle);
        angle = normalizeAngle(angle);

        if (startAngle < endAngle) {
            return angle >= startAngle && angle <= endAngle;
        } else {
            return angle >= startAngle || angle <= endAngle;
        }
    }

    private float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;
        return angle;
    }

    private int getItemIndex(RadialMenuItem item) {
        return menuItems.indexOf(item);
    }

    public void clickItem() {
        if (itemHovering != null) {
            itemHovering.onClick();
        } else {
            onClickOutside();
        }
    }

    public void onClickOutside() {
        close();
    }
}
