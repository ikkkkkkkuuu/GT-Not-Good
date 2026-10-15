package com.xyp.gtnotgood.client.radialmenu.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.opengl.GL11;

public final class Ui {

    public static final int ROW = 20;

    public static final int GAP = 4;

    public static final int STEP = ROW + GAP;

    public static final int GROUP_STEP = ROW + 1;

    public static final int PAD = 8;

    public static final int BOTTOM_MARGIN = 8;

    public static final int LABEL_WIDTH = 104;

    public static final int PANEL_BG = 0xE8101010;
    public static final int PANEL_BORDER = 0xFF4A4A4A;
    public static final int LIST_BG = 0xC0000000;
    public static final int ROW_HOVER = 0x30FFFFFF;
    public static final int ROW_SELECTED = 0x50FFFFFF;

    public static final int CODE_PLAIN = 0xFFC9D1D9;
    public static final int CODE_KEYWORD = 0xFFC586C0;
    public static final int CODE_DECLARATION = 0xFFFF7B72;
    public static final int CODE_CONSTANT = 0xFF569CD6;
    public static final int CODE_SELF = 0xFF79C0FF;
    public static final int CODE_API = 0xFFFFA657;
    public static final int CODE_FUNCTION = 0xFFD2A8FF;
    public static final int CODE_FIELD = 0xFF4EC9B0;
    public static final int CODE_STRING = 0xFFA5D6FF;
    public static final int CODE_NUMBER = 0xFFB5CEA8;
    public static final int CODE_COMMENT = 0xFF8B949E;
    public static final int CODE_OPERATOR = 0xFFD4D4D4;
    public static final int CODE_GUTTER = 0xFF6A6A6A;
    public static final int CODE_CURRENT_LINE = 0x18FFFFFF;
    public static final int CODE_INDENT_GUIDE = 0x38FFFFFF;
    public static final int CODE_FOLDED = 0x20FFFFFF;
    public static final int CODE_SELECTION = 0x604A90D9;
    public static final int CODE_ERROR_LINE = 0x40FF5555;

    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_MUTED = 0xFF9A9A9A;
    public static final int TEXT_HEADER = 0xFFFFAA00;
    public static final int TEXT_ACTIVE = 0xFF55FF55;
    public static final int TEXT_ERROR = 0xFFFF5555;

    private Ui() {}

    public static FontRenderer font() {
        return Minecraft.getMinecraft().fontRenderer;
    }

    public static int bottomBarY(int screenHeight) {
        return screenHeight - BOTTOM_MARGIN - ROW;
    }

    public static void frame(int left, int top, int right, int bottom, int fill, int border) {
        Gui.drawRect(left, top, right, top + 1, border);
        Gui.drawRect(left, bottom - 1, right, bottom, border);
        Gui.drawRect(left, top + 1, left + 1, bottom - 1, border);
        Gui.drawRect(right - 1, top + 1, right, bottom - 1, border);
        Gui.drawRect(left + 1, top + 1, right - 1, bottom - 1, fill);
    }

    public static void panel(int left, int top, int right, int bottom) {
        frame(left, top, right, bottom, PANEL_BG, PANEL_BORDER);
    }

    public static void list(int left, int top, int right, int bottom) {
        frame(left, top, right, bottom, LIST_BG, PANEL_BORDER);
    }

    public static void sectionHeader(String text, int left, int top, int right) {
        font().drawString(text, left, top, TEXT_HEADER);
        Gui.drawRect(left, top + 10, right, top + 11, 0x40FFFFFF);
    }

    public static void rowLabel(String text, int left, int rowTop) {
        FontRenderer font = font();
        font.drawString(text, left + LABEL_WIDTH - font.getStringWidth(text), rowTop + (ROW - 8) / 2, TEXT_MUTED);
    }

    public static void beginClip(int left, int top, int right, int bottom) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scale = resolution.getScaleFactor();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left * scale, (resolution.getScaledHeight() - bottom) * scale, Math.max(0, right - left) * scale,
            Math.max(0, bottom - top) * scale);
    }

    public static void endClip() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** Shortens text with an ellipsis so it cannot run past the width it was given. */
    public static String fit(String text, int width) {
        FontRenderer font = font();
        if (text == null || font.getStringWidth(text) <= width) {
            return text;
        }
        String trimmed = font.trimStringToWidth(text, width - font.getStringWidth("..."));
        return trimmed + "...";
    }
}
