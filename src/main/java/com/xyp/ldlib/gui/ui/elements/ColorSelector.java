package com.xyp.ldlib.gui.ui.elements;

import java.awt.Color;
import java.util.Locale;
import java.util.Objects;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvent;
import com.xyp.ldlib.gui.ui.event.UIEvents;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/** LDLib2 HSV/alpha picker with a saturation/value plane and editable ARGB hexadecimal text. */
public final class ColorSelector extends UIElement {

    private float hue, saturation, brightness = 1;
    private int alpha = 255;
    private final Slider hueSlider, alphaSlider;
    private final TextField hex;
    private final UIElement plane;
    private IntConsumer onChange = ignored -> {};

    public ColorSelector(int x, int y, int width, int height, ModernTheme theme) {
        super(x, y, width, height);
        plane = new UIElement(0, 0, width, Math.max(0, height - 64));
        plane.setBackground(this::drawPlane);
        plane.addEventListener(UIEvents.MOUSE_DOWN, this::pick);
        plane.addEventListener(UIEvents.MOUSE_MOVE, this::pick);
        hueSlider = new Slider(
            0,
            height - 60,
            width,
            12,
            0,
            1,
            1d / 360,
            false,
            (mx, my, left, top, w, h) -> drawHue(left, top, w, h),
            theme.accent);
        alphaSlider = new Slider(0, height - 44, width, 12, 0, 255, 1, false, theme.input, theme.accent);
        hex = theme.textField(0, height - 26, Math.max(0, width - 30), 22)
            .setMaxLength(8)
            .setValidator(text -> text.matches("[0-9a-fA-F]{0,8}"));
        hueSlider.setOnChange(value -> {
            hue = (float) value;
            changed();
        });
        alphaSlider.setOnChange(value -> {
            alpha = (int) value;
            changed();
        });
        hex.setOnChange(text -> {
            if (text.length() == 8) {
                int before = getColor();
                setColor((int) Long.parseLong(text, 16));
                if (getColor() != before) onChange.accept(getColor());
            }
        });
        addChild(plane);
        addChild(hueSlider);
        addChild(alphaSlider);
        addChild(hex);
        addChild(new UIElement(width - 26, height - 26, 26, 22).setBackground((mx, my, left, top, w, h) -> {
            for (int row = 0; row < h; row += 4) for (int col = 0; col < w; col += 4) {
                Gui.drawRect(
                    left + col,
                    top + row,
                    left + Math.min(w, col + 4),
                    top + Math.min(h, row + 4),
                    ((row + col) / 4 % 2 == 0) ? 0xFFEEEEEE : 0xFF888888);
            }
            Gui.drawRect(left, top, left + w, top + h, getColor());
        }));
        setColor(0xFFFFFFFF);
    }

    public int getColor() {
        return alpha << 24 | Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
    }

    /** Silent ARGB update; controls always reflect the same underlying color. */
    public ColorSelector setColor(int argb) {
        float[] hsv = Color.RGBtoHSB(argb >> 16 & 255, argb >> 8 & 255, argb & 255, null);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
        alpha = argb >>> 24;
        hueSlider.setValue(hue);
        alphaSlider.setValue(alpha);
        hex.setText(String.format(Locale.ROOT, "%08X", argb));
        return this;
    }

    public ColorSelector setOnChange(IntConsumer callback) {
        onChange = Objects.requireNonNull(callback);
        return this;
    }

    private void changed() {
        hex.setText(String.format(Locale.ROOT, "%08X", getColor()));
        onChange.accept(getColor());
    }

    private void pick(UIEvent e) {
        if (e.button != 0) return;
        int before = getColor();
        saturation = Math.max(0, Math.min(1, (float) (e.x - plane.getScreenX()) / Math.max(1, plane.getWidth() - 1)));
        brightness = 1
            - Math.max(0, Math.min(1, (float) (e.y - plane.getScreenY()) / Math.max(1, plane.getHeight() - 1)));
        if (before != getColor()) changed();
        e.preventDefault();
        e.stopPropagation();
    }

    private void drawHue(int left, int top, int w, int h) {
        for (int col = 0; col < w; col++) Gui
            .drawRect(left + col, top, left + col + 1, top + h, Color.HSBtoRGB((float) col / Math.max(1, w - 1), 1, 1));
    }

    /** Batches the HSV plane into one tessellator draw, restoring fixed-function state afterwards. */
    private void drawPlane(int mx, int my, int left, int top, int w, int h) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            for (int row = 0; row < h; row += 2) for (int col = 0; col < w; col += 2) {
                tess.setColorOpaque_I(
                    Color.HSBtoRGB(hue, (float) col / Math.max(1, w - 1), 1 - (float) row / Math.max(1, h - 1)));
                int right = left + Math.min(w, col + 2), bottom = top + Math.min(h, row + 2);
                tess.addVertex(left + col, bottom, 0);
                tess.addVertex(right, bottom, 0);
                tess.addVertex(right, top + row, 0);
                tess.addVertex(left + col, top + row, 0);
            }
            tess.draw();
        } finally {
            GL11.glPopAttrib();
        }
        int cx = left + Math.round(saturation * Math.max(0, w - 1));
        int cy = top + Math.round((1 - brightness) * Math.max(0, h - 1));
        Gui.drawRect(Math.max(left, cx - 2), cy, Math.min(left + w, cx + 3), cy + 1, 0xFFFFFFFF);
        Gui.drawRect(cx, Math.max(top, cy - 2), cx + 1, Math.min(top + h, cy + 3), 0xFFFFFFFF);
    }

    @Override
    public void layout() {
        plane.setSize(width, Math.max(0, height - 64));
        hueSlider.setPosition(0, Math.max(0, height - 60))
            .setSize(width, 12);
        alphaSlider.setPosition(0, Math.max(0, height - 44))
            .setSize(width, 12);
        hex.setPosition(0, Math.max(0, height - 26))
            .setSize(Math.max(0, width - 30), 22);
        getChildren().get(4)
            .setPosition(Math.max(0, width - 26), Math.max(0, height - 26));
        super.layout();
    }
}
