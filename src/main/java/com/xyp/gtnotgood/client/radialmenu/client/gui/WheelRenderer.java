package com.xyp.gtnotgood.client.radialmenu.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.client.radialmenu.client.icon.IconRenderer;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.config.AnimationConfig;
import com.xyp.gtnotgood.client.radialmenu.config.ColorConfig;
import com.xyp.gtnotgood.client.radialmenu.config.SlotPlate;
import com.xyp.gtnotgood.client.radialmenu.config.WheelConfig;
import com.xyp.gtnotgood.client.radialmenu.core.animation.RevealAnimation;
import com.xyp.gtnotgood.client.radialmenu.core.geometry.RadialGeometry;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.StyleResolver;
import com.xyp.gtnotgood.client.radialmenu.core.model.WheelColors;

public final class WheelRenderer {

    private static final int MIN_LABEL_WIDTH = 48;

    private static final double FEATHER = 1.0;

    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");

    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    private WheelRenderer() {}

    public static WheelColors colorsFor(MenuNode menu, boolean editMode) {
        WheelColors colors = StyleResolver.resolve(ColorConfig.defaultColors(), ProfileManager.active().style,
            menu.style);

        if (!editMode) {
            return colors;
        }

        return new WheelColors(blend(colors.ring, 0xFFAA00, 0.25F), blend(colors.highlight, 0xFFAA00, 0.45F),
            colors.border, colors.highlightBorder, colors.background, colors.icon);
    }

    public static void drawRing(WheelRing ring, WheelColors colors, int centerX, int centerY, int hoveredSlot,
        WheelAnimator animator) {
        MenuNode menu = ring.menu;
        int slotCount = ring.slotCount;
        if (slotCount <= 0) {
            return;
        }

        double outer = ring.outer;
        double inner = ring.inner;

        boolean grows = AnimationConfig.revealAnimation != RevealAnimation.FADE;
        double gap = Math.max(0.0, WheelConfig.sectorGap);
        double width = Math.max(1, WheelConfig.borderWidth);

        boolean outline = (colors.border >>> 24) != 0 && WheelConfig.drawOutline;
        boolean smooth = WheelConfig.smoothEdges;

        double sideWidth = gap > 0.0 ? width : width / 2.0;

        beginShapes();

        double span = ring.slotSpan();
        for (int slot = 0; slot < slotCount; slot++) {
            float reveal = animator.reveal(slot, slotCount);
            if (reveal <= 0.01f) {
                continue;
            }
            double sectorInner = sectorRadius(inner, reveal, grows, animator.push(slot));
            double sectorOuter = sectorRadius(outer, reveal, grows, animator.push(slot));

            boolean filled = menu.childAt(slot) != null;
            int color = slot == hoveredSlot ? colors.highlight : colors.ring;
            if (!filled) {

                color = fade(color, 0.35F);
            }
            color = fade(color, reveal);
            int lineColor = fade(colors.border, reveal);
            double start = ring.slotStart(slot);

            fillSector(centerX, centerY, sectorInner, sectorOuter, start, span, gap, color);

            if (outline) {
                outlineSector(centerX, centerY, sectorInner, sectorOuter, start, span, gap, width, sideWidth,
                    lineColor);
            }

            if (smooth) {

                featherArcs(centerX, centerY, sectorInner, sectorOuter, start, span, gap, outline ? lineColor : color);

                if (gap > 0.0) {
                    featherSides(centerX, centerY, sectorInner, sectorOuter, start, span, gap,
                        outline ? lineColor : color);
                }
            }
        }

        if (
            WheelConfig.drawHighlightOutline && (colors.highlightBorder >>> 24) != 0
                && hoveredSlot >= 0
                && hoveredSlot < slotCount
        ) {
            float reveal = animator.reveal(hoveredSlot, slotCount);
            double start = ring.slotStart(hoveredSlot);
            outlineSector(centerX, centerY, sectorRadius(inner, reveal, grows, animator.push(hoveredSlot)),
                sectorRadius(outer, reveal, grows, animator.push(hoveredSlot)), start, span, gap, width, width,
                fade(colors.highlightBorder, reveal));
        }

        endShapes();

        drawPlates(ring, centerX, centerY, hoveredSlot, grows, animator);
        drawIcons(ring, centerX, centerY, grows, animator);
    }

    private static double sectorRadius(double radius, float reveal, boolean grows, double push) {
        return (grows ? radius * reveal : radius) + push;
    }

    private static void beginShapes() {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void endShapes() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private static void fillSector(int centerX, int centerY, double inner, double outer, double startAngle,
        double spanDegrees, double gap, int argb) {
        double insetInner = inset(inner, gap, spanDegrees);
        double insetOuter = inset(outer, gap, spanDegrees);
        int segments = RadialGeometry.arcSegments(spanDegrees);

        glColor(argb);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();

        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / segments;
            double t1 = (double) (i + 1) / segments;

            double innerFrom = lerpAngle(startAngle, spanDegrees, insetInner, t0);
            double innerTo = lerpAngle(startAngle, spanDegrees, insetInner, t1);
            double outerFrom = lerpAngle(startAngle, spanDegrees, insetOuter, t0);
            double outerTo = lerpAngle(startAngle, spanDegrees, insetOuter, t1);

            addVertex(tessellator, centerX, centerY, innerFrom, inner);
            addVertex(tessellator, centerX, centerY, outerFrom, outer);
            addVertex(tessellator, centerX, centerY, outerTo, outer);
            addVertex(tessellator, centerX, centerY, innerTo, inner);
        }
        tessellator.draw();
    }

    private static double inset(double radius, double gap, double spanDegrees) {
        return Math.min(RadialGeometry.gapInsetDegrees(radius, gap), spanDegrees / 2.0);
    }

    private static double lerpAngle(double startAngle, double spanDegrees, double inset, double fraction) {
        double from = startAngle + inset;
        double to = startAngle + spanDegrees - inset;
        return from + (to - from) * fraction;
    }

    private static void featherArcs(int centerX, int centerY, double inner, double outer, double startAngle,
        double spanDegrees, double gap, int argb) {
        double insetInner = inset(inner, gap, spanDegrees);
        double insetOuter = inset(outer, gap, spanDegrees);

        arcBand(centerX, centerY, outer, outer + FEATHER, startAngle + insetOuter,
            startAngle + spanDegrees - insetOuter, argb, transparent(argb));
        arcBand(centerX, centerY, inner - FEATHER, inner, startAngle + insetInner,
            startAngle + spanDegrees - insetInner, transparent(argb), argb);
    }

    private static void featherSides(int centerX, int centerY, double inner, double outer, double startAngle,
        double spanDegrees, double gap, int argb) {
        sideBand(centerX, centerY, inner, outer, startAngle, spanDegrees, gap, FEATHER, true, argb);
        sideBand(centerX, centerY, inner, outer, startAngle, spanDegrees, gap, FEATHER, false, argb);
    }

    private static void outlineSector(int centerX, int centerY, double inner, double outer, double startAngle,
        double spanDegrees, double gap, double width, double sideWidth, int argb) {
        double insetInner = inset(inner, gap, spanDegrees);
        double insetOuter = inset(outer, gap, spanDegrees);

        arcBand(centerX, centerY, outer - width, outer, startAngle + insetOuter, startAngle + spanDegrees - insetOuter,
            argb, argb);
        arcBand(centerX, centerY, inner, inner + width, startAngle + insetInner, startAngle + spanDegrees - insetInner,
            argb, argb);
        sideBand(centerX, centerY, inner, outer, startAngle, spanDegrees, gap, -sideWidth, true, argb);
        sideBand(centerX, centerY, inner, outer, startAngle, spanDegrees, gap, -sideWidth, false, argb);
    }

    private static void arcBand(int centerX, int centerY, double from, double to, double fromAngle, double toAngle,
        int fromArgb, int toArgb) {
        double sweep = toAngle - fromAngle;
        int segments = RadialGeometry.arcSegments(Math.abs(sweep));

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();

        for (int i = 0; i < segments; i++) {
            double a = fromAngle + sweep * i / segments;
            double b = fromAngle + sweep * (i + 1) / segments;

            vertexColored(tessellator, centerX, centerY, a, from, fromArgb);
            vertexColored(tessellator, centerX, centerY, a, to, toArgb);
            vertexColored(tessellator, centerX, centerY, b, to, toArgb);
            vertexColored(tessellator, centerX, centerY, b, from, fromArgb);
        }
        tessellator.draw();
    }

    private static void sideBand(int centerX, int centerY, double inner, double outer, double startAngle,
        double spanDegrees, double gap, double width, boolean leading, int argb) {
        double insetInner = inset(inner, gap, spanDegrees);
        double insetOuter = inset(outer, gap, spanDegrees);

        double innerAngle = leading ? startAngle + insetInner : startAngle + spanDegrees - insetInner;
        double outerAngle = leading ? startAngle + insetOuter : startAngle + spanDegrees - insetOuter;

        double x1 = centerX + RadialGeometry.offsetX(innerAngle, inner);
        double y1 = centerY + RadialGeometry.offsetY(innerAngle, inner);
        double x2 = centerX + RadialGeometry.offsetX(outerAngle, outer);
        double y2 = centerY + RadialGeometry.offsetY(outerAngle, outer);

        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0) {
            return;
        }

        double nx = dy / length;
        double ny = -dx / length;
        double midAngle = startAngle + spanDegrees / 2.0;
        double midX = centerX + RadialGeometry.offsetX(midAngle, (inner + outer) / 2.0);
        double midY = centerY + RadialGeometry.offsetY(midAngle, (inner + outer) / 2.0);
        if (nx * ((x1 + x2) / 2.0 - midX) + ny * ((y1 + y2) / 2.0 - midY) < 0.0) {
            nx = -nx;
            ny = -ny;
        }

        boolean fade = width > 0.0;
        double offset = Math.abs(width) * (fade ? 1.0 : -1.0);
        int nearArgb = argb;
        int farArgb = fade ? transparent(argb) : argb;

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        vertexColored(tessellator, x1, y1, nearArgb);
        vertexColored(tessellator, x2, y2, nearArgb);
        vertexColored(tessellator, x2 + nx * offset, y2 + ny * offset, farArgb);
        vertexColored(tessellator, x1 + nx * offset, y1 + ny * offset, farArgb);
        tessellator.draw();
    }

    private static void addVertex(Tessellator tessellator, int centerX, int centerY, double angle, double radius) {
        tessellator.addVertex(centerX + RadialGeometry.offsetX(angle, radius),
            centerY + RadialGeometry.offsetY(angle, radius), 0.0);
    }

    private static void vertexColored(Tessellator tessellator, int centerX, int centerY, double angle, double radius,
        int argb) {
        vertexColored(tessellator, centerX + RadialGeometry.offsetX(angle, radius),
            centerY + RadialGeometry.offsetY(angle, radius), argb);
    }

    private static void vertexColored(Tessellator tessellator, double x, double y, int argb) {
        tessellator.setColorRGBA((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
        tessellator.addVertex(x, y, 0.0);
    }

    private static int transparent(int argb) {
        return argb & 0x00FFFFFF;
    }

    private static void glColor(int argb) {
        GL11.glColor4f(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F,
            ((argb >>> 24) & 0xFF) / 255.0F);
    }

    private static void drawPlates(WheelRing ring, int centerX, int centerY, int hoveredSlot, boolean grows,
        WheelAnimator animator) {
        SlotPlate plate = WheelConfig.slotPlate;
        if (plate == null || plate == SlotPlate.NONE) {
            return;
        }

        int slotCount = ring.slotCount;
        for (int slot = 0; slot < slotCount; slot++) {
            if (ring.childAt(slot) == null || !arrived(animator, slot, slotCount)) {
                continue;
            }
            double iconRadius = iconRadius(ring, slot, grows, animator);
            int x = iconLeft(centerX, ring, slot, iconRadius);
            int y = iconTop(centerY, ring, slot, iconRadius);
            drawTexture(INVENTORY, x - scaled(1), y - scaled(1), 7, 141, scaled(18), scaled(18));
        }

        if (plate == SlotPlate.SELECTED) {
            for (int slot = 0; slot < slotCount; slot++) {
                if (ring.childAt(slot) == null || !arrived(animator, slot, slotCount)) {
                    continue;
                }
                double iconRadius = iconRadius(ring, slot, grows, animator);
                drawSelection(iconLeft(centerX, ring, slot, iconRadius), iconTop(centerY, ring, slot, iconRadius));
            }
            return;
        }

        if (
            plate != SlotPlate.HOTBAR || hoveredSlot < 0
                || ring.childAt(hoveredSlot) == null
                || !arrived(animator, hoveredSlot, slotCount)
        ) {
            return;
        }
        double hoveredRadius = iconRadius(ring, hoveredSlot, grows, animator);
        int x = iconLeft(centerX, ring, hoveredSlot, hoveredRadius);
        int y = iconTop(centerY, ring, hoveredSlot, hoveredRadius);
        drawSelection(x, y);
    }

    private static void drawSelection(int x, int y) {
        drawTexture(WIDGETS, x - scaled(4), y - scaled(4), 0, 22, scaled(24), scaled(24));
    }

    private static int scaled(int atSixteen) {
        return Math.max(1, Math.round(atSixteen * WheelConfig.effectiveIconSize() / 16.0F));
    }

    private static void drawTexture(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        float scale = 1.0F / 256.0F;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0.0, u * scale, (v + height) * scale);
        tessellator.addVertexWithUV(x + width, y + height, 0.0, (u + width) * scale, (v + height) * scale);
        tessellator.addVertexWithUV(x + width, y, 0.0, (u + width) * scale, v * scale);
        tessellator.addVertexWithUV(x, y, 0.0, u * scale, v * scale);
        tessellator.draw();

        GL11.glDisable(GL11.GL_BLEND);
    }

    private static int iconLeft(int centerX, WheelRing ring, int slot, double iconRadius) {
        double angle = ring.slotCenter(slot);
        return (int) Math.round(centerX + RadialGeometry.offsetX(angle, iconRadius))
            - WheelConfig.effectiveIconSize() / 2;
    }

    private static int iconTop(int centerY, WheelRing ring, int slot, double iconRadius) {
        double angle = ring.slotCenter(slot);
        return (int) Math.round(centerY + RadialGeometry.offsetY(angle, iconRadius))
            - WheelConfig.effectiveIconSize() / 2;
    }

    private static double iconRadius(WheelRing ring, int slot, boolean grows, WheelAnimator animator) {
        float reveal = animator.reveal(slot, ring.slotCount);
        return (grows ? ring.midRadius() * reveal : ring.midRadius()) + animator.push(slot);
    }

    private static boolean arrived(WheelAnimator animator, int slot, int slotCount) {
        return animator.reveal(slot, slotCount) > 0.5f;
    }

    private static void drawIcons(WheelRing ring, int centerX, int centerY, boolean grows, WheelAnimator animator) {
        for (int slot = 0; slot < ring.slotCount; slot++) {
            MenuNode child = ring.childAt(slot);
            if (child == null || child.icon == null || !arrived(animator, slot, ring.slotCount)) {
                continue;
            }
            double iconRadius = iconRadius(ring, slot, grows, animator);
            IconRenderer.draw(child.icon, iconLeft(centerX, ring, slot, iconRadius),
                iconTop(centerY, ring, slot, iconRadius), WheelConfig.effectiveIconSize());
        }
    }

    private static List<String> labelLines(MenuNode menu, int hoveredSlot) {
        MenuNode hovered = menu.childAt(hoveredSlot);
        String text = hovered == null ? null : hovered.title;
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;

        int wrapWidth = Math.max(MIN_LABEL_WIDTH, WheelConfig.effectiveInnerRadius() * 2 - 8);
        return wrapWords(font, text, wrapWidth);
    }

    public static void drawLabel(MenuNode menu, int centerX, int centerY, int hoveredSlot) {
        MenuNode hovered = menu.childAt(hoveredSlot);
        String text = hovered == null ? null : hovered.title;
        if (text == null || text.isEmpty()) {
            return;
        }

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        List<String> lines = labelLines(menu, hoveredSlot);
        int top = centerY - lines.size() * font.FONT_HEIGHT / 2;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            font.drawStringWithShadow(line, centerX - font.getStringWidth(line) / 2, top + i * font.FONT_HEIGHT,
                0xFFFFFFFF);
        }
    }

    private static List<String> wrapWords(FontRenderer font, String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (line.length() == 0) {
                line.append(word);
                continue;
            }
            if (font.getStringWidth(line + " " + word) <= width) {
                line.append(' ').append(word);
                continue;
            }
            lines.add(line.toString());
            line.setLength(0);
            line.append(word);
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    public static void drawHeader(int screenWidth, String profileName, String breadcrumb, boolean editMode,
        boolean scripted) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;

        String left = EnumChatFormatting.GRAY + profileName;
        String path = breadcrumb == null || breadcrumb.isEmpty() ? "" : EnumChatFormatting.WHITE + breadcrumb;

        StringBuilder line = new StringBuilder(left);
        if (!path.isEmpty()) {
            line.append(EnumChatFormatting.DARK_GRAY).append("  >  ").append(path);
        }
        if (editMode) {
            line.append("   ").append(EnumChatFormatting.GOLD).append(EnumChatFormatting.BOLD)
                // #tr radialmenu.wheel.editing
                // # EDIT
                // # zh_CN 编辑中
                .append(I18n.format("radialmenu.wheel.editing"));
        } else if (scripted) {
            line.append("   ").append(EnumChatFormatting.AQUA).append(EnumChatFormatting.BOLD)
                // #tr radialmenu.wheel.scripted
                // # SCRIPT
                // # zh_CN 脚本
                .append(I18n.format("radialmenu.wheel.scripted"));
        }

        String text = line.toString();
        int width = font.getStringWidth(text);
        int x = screenWidth / 2 - width / 2;

        Gui.drawRect(x - 6, 4, x + width + 6, 20, 0x80000000);
        font.drawStringWithShadow(text, x, 8, 0xFFFFFFFF);
    }

    public static void drawCenterHint(MenuNode menu, int centerX, int centerY, int hoveredSlot) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        // #tr radialmenu.wheel.menuSettings
        // # Menu settings
        // # zh_CN 菜单设置
        String text = I18n.format("radialmenu.wheel.menuSettings");

        int lines = labelLines(menu, hoveredSlot).size();
        int y = lines == 0 ? centerY - font.FONT_HEIGHT / 2 : centerY + lines * font.FONT_HEIGHT / 2 + 1;

        font.drawStringWithShadow(text, centerX - font.getStringWidth(text) / 2, y, 0xFFFFAA00);
    }

    private static int blend(int argb, int rgb, float amount) {
        int alpha = argb >>> 24;
        int r = Math.round(((argb >> 16) & 0xFF) * (1 - amount) + ((rgb >> 16) & 0xFF) * amount);
        int g = Math.round(((argb >> 8) & 0xFF) * (1 - amount) + ((rgb >> 8) & 0xFF) * amount);
        int b = Math.round((argb & 0xFF) * (1 - amount) + (rgb & 0xFF) * amount);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private static int fade(int argb, float factor) {
        int alpha = (int) (((argb >>> 24) & 0xFF) * factor);
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

}
