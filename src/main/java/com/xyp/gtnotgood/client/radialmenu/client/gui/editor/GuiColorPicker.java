package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.Ui;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiScreen;
import com.xyp.gtnotgood.client.radialmenu.core.Colors;

public class GuiColorPicker extends UiScreen {

    public interface Result {

        void onColorPicked(String hex);
    }

    private static final int ID_CLEAR = 1;

    private static final int[] PRESETS = { 0xFFFFFF, 0xC0C0C0, 0x808080, 0x404040, 0x000000, 0xFF5555, 0xFF8800,
        0xFFD700, 0x7FFF00, 0x2ECC71, 0x1ABC9C, 0x4A90D9, 0x3355FF, 0x8E44AD, 0xFF69B4, 0x8B4513 };

    private static final int SQUARE = 120;
    private static final int HUE_WIDTH = 14;

    private static final int SIDE = 68;
    private static final int SWATCH = 16;
    private static final int STEPS = 30;

    private final Result result;

    private final boolean withAlpha;

    private float hue;
    private float saturation = 1f;
    private float value = 1f;

    /** Opacity, 0 to 1. Ring colours are translucent by nature, so it is picked here rather than inherited. */
    private float alpha = 1f;

    private GuiTextField hexField;

    private int squareLeft;
    private int squareTop;
    private int hueLeft;
    private int alphaLeft;
    private int presetsTop;

    /** Which control the drag started on, so dragging off one does not hand over to the other. */
    private int dragging;

    public GuiColorPicker(String initialHex, Result result) {
        this(initialHex, false, result);
    }

    public GuiColorPicker(String initialHex, boolean withAlpha, Result result) {
        this.result = result;
        this.withAlpha = withAlpha;
        int argb = Colors.parseArgb(initialHex, 0xFFFFFFFF);
        alpha = withAlpha && Colors.hasAlpha(initialHex) ? ((argb >>> 24) & 0xFF) / 255f : 1f;
        setRgb(argb & 0x00FFFFFF);
    }

    @Override
    protected String titleKey() {
        // #tr radialmenu.color.title
        // # Pick a color
        // # zh_CN 选择颜色
        return "radialmenu.color.title";
    }

    /** Derived from the controls rather than picked by eye, so the panel cannot end up wider than its contents. */
    @Override
    protected int panelWidth() {
        int strips = withAlpha ? 2 : 1;
        return SQUARE + strips * (Ui.GAP + HUE_WIDTH) + Ui.GAP + 2 + SIDE + Ui.PAD * 2;
    }

    @Override
    protected int panelHeightHint() {
        return SQUARE + Ui.GAP + 2 * (SWATCH + 2);
    }

    @Override
    protected void buildControls() {
        squareLeft = contentLeft();
        squareTop = contentTop();
        hueLeft = squareLeft + SQUARE + Ui.GAP;
        alphaLeft = hueLeft + HUE_WIDTH + Ui.GAP;
        presetsTop = squareTop + SQUARE + Ui.GAP;

        int sideLeft = sideLeft();
        hexField = new GuiTextField(this.fontRendererObj, sideLeft + 1, squareTop + 44, SIDE - 2, 14);
        hexField.setMaxStringLength(withAlpha ? 9 : 7);
        hexField.setText(toHex());
        hexField.setCursorPositionZero();

        this.buttonList.add(
            // #tr radialmenu.color.inherit
            // # Inherit
            // # zh_CN 继承
            new GuiButton(ID_CLEAR, sideLeft, squareTop + 64, SIDE, Ui.ROW, I18n.format("radialmenu.color.inherit")));
        // #tr radialmenu.color.inherit.tip
        // # Clear this color to inherit the profile or mod defaults.
        // # zh_CN 清除此颜色，继承配置方案或模组默认值。
        tooltip(ID_CLEAR, I18n.format("radialmenu.color.inherit.tip"));

        setContentHeight(SQUARE + Ui.GAP + 2 * (SWATCH + 2));
        addBottomBar("gui.done", null, "gui.cancel");
    }

    // -- colour conversion -------------------------------------------------------------------------------------

    /** Hand-rolled rather than via AWT, so nothing here depends on a graphics environment being available. */
    private static int hsvToRgb(float h, float s, float v) {
        float r;
        float g;
        float b;
        int sector = (int) Math.floor(h * 6f) % 6;
        float f = h * 6f - (float) Math.floor(h * 6f);
        float p = v * (1f - s);
        float q = v * (1f - f * s);
        float t = v * (1f - (1f - f) * s);

        switch (sector) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
                break;
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }

    private void setRgb(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        value = max;
        saturation = max == 0f ? 0f : delta / max;

        if (delta == 0f) {
            hue = 0f;
        } else if (max == r) {
            hue = ((g - b) / delta % 6f) / 6f;
        } else if (max == g) {
            hue = ((b - r) / delta + 2f) / 6f;
        } else {
            hue = ((r - g) / delta + 4f) / 6f;
        }
        if (hue < 0f) {
            hue += 1f;
        }
    }

    private int currentRgb() {
        return hsvToRgb(hue, saturation, value);
    }

    private int sideLeft() {
        int lastStripRight = (withAlpha ? alphaLeft : hueLeft) + HUE_WIDTH;
        return lastStripRight + Ui.GAP + 2;
    }

    /** Eight digits when opacity is part of the choice, six otherwise. */
    private String toHex() {
        if (!withAlpha) {
            return String.format("#%06X", Integer.valueOf(currentRgb()));
        }
        return String.format("#%02X%06X", Integer.valueOf(Math.round(alpha * 255f)), Integer.valueOf(currentRgb()));
    }

    // -- drawing -----------------------------------------------------------------------------------------------

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        drawSaturationValueSquare();
        drawHueStrip();
        if (withAlpha) {
            drawAlphaStrip();
        }
        drawPresets();

        int sideLeft = sideLeft();
        if (withAlpha) {
            drawChequerboard(sideLeft, squareTop, sideLeft + SIDE, squareTop + 38);
        }
        Ui.frame(sideLeft, squareTop, sideLeft + SIDE, squareTop + 38, currentArgb(), 0xFF000000);

        hexField.drawTextBox();
    }

    private void drawSaturationValueSquare() {
        int step = SQUARE / STEPS;
        for (int sx = 0; sx < STEPS; sx++) {
            for (int sy = 0; sy < STEPS; sy++) {
                float s = sx / (float) (STEPS - 1);
                float v = 1f - sy / (float) (STEPS - 1);
                int x = squareLeft + sx * step;
                int y = squareTop + sy * step;
                drawRect(x, y, x + step, y + step, 0xFF000000 | hsvToRgb(hue, s, v));
            }
        }
        drawCrosshair(squareLeft + Math.round(saturation * (SQUARE - 1)),
            squareTop + Math.round((1f - value) * (SQUARE - 1)));
    }

    private void drawHueStrip() {
        int step = Math.max(1, SQUARE / 60);
        for (int i = 0; i * step < SQUARE; i++) {
            int y = squareTop + i * step;
            drawRect(hueLeft, y, hueLeft + HUE_WIDTH, y + step,
                0xFF000000 | hsvToRgb(i * step / (float) SQUARE, 1f, 1f));
        }
        int markerY = squareTop + Math.round(hue * (SQUARE - 1));
        drawRect(hueLeft - 2, markerY - 1, hueLeft + HUE_WIDTH + 2, markerY + 1, 0xFFFFFFFF);
    }

    private void drawAlphaStrip() {
        drawChequerboard(alphaLeft, squareTop, alphaLeft + HUE_WIDTH, squareTop + SQUARE);

        int rgb = currentRgb();
        int step = Math.max(1, SQUARE / 60);
        for (int i = 0; i * step < SQUARE; i++) {
            int y = squareTop + i * step;
            int a = Math.round((1f - i * step / (float) SQUARE) * 255f);
            drawRect(alphaLeft, y, alphaLeft + HUE_WIDTH, y + step, (a << 24) | rgb);
        }
        int markerY = squareTop + Math.round((1f - alpha) * (SQUARE - 1));
        drawRect(alphaLeft - 2, markerY - 1, alphaLeft + HUE_WIDTH + 2, markerY + 1, 0xFFFFFFFF);
    }

    private void drawChequerboard(int left, int top, int right, int bottom) {
        int cell = 4;
        for (int y = top; y < bottom; y += cell) {
            for (int x = left; x < right; x += cell) {
                boolean dark = ((x - left) / cell + (y - top) / cell) % 2 == 0;
                drawRect(x, y, Math.min(x + cell, right), Math.min(y + cell, bottom), dark ? 0xFF808080 : 0xFFC0C0C0);
            }
        }
    }

    private int currentArgb() {
        return (Math.round(alpha * 255f) << 24) | currentRgb();
    }

    private void drawPresets() {
        for (int i = 0; i < PRESETS.length; i++) {
            int x = squareLeft + (i % 8) * (SWATCH + 2);
            int y = presetsTop + (i / 8) * (SWATCH + 2);
            Ui.frame(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | PRESETS[i], 0xFF000000);
        }
    }

    /** Two thin bars rather than a filled square, so the colour underneath stays visible. */
    private void drawCrosshair(int x, int y) {
        int colour = value > 0.5f && saturation < 0.5f ? 0xFF000000 : 0xFFFFFFFF;
        drawRect(x - 4, y, x + 5, y + 1, colour);
        drawRect(x, y - 4, x + 1, y + 5, colour);
    }

    // -- input -------------------------------------------------------------------------------------------------

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_PRIMARY) {
            confirm(toHex());
        } else if (button.id == ID_SECONDARY) {
            onCancel();
        } else if (button.id == ID_CLEAR) {
            // An empty value is how a colour says "use the global setting".
            confirm("");
        }
    }

    private void confirm(String hex) {
        result.onColorPicked(hex);
        GuiStack.pop();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!isShowing()) {
            return;
        }
        hexField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }
        if (inSquare(mouseX, mouseY)) {
            dragging = 1;
            applySquare(mouseX, mouseY);
            return;
        }
        if (inHueStrip(mouseX, mouseY)) {
            dragging = 2;
            applyHue(mouseY);
            return;
        }
        if (withAlpha && inAlphaStrip(mouseX, mouseY)) {
            dragging = 3;
            applyAlpha(mouseY);
            return;
        }
        for (int i = 0; i < PRESETS.length; i++) {
            int x = squareLeft + (i % 8) * (SWATCH + 2);
            int y = presetsTop + (i / 8) * (SWATCH + 2);
            if (mouseX >= x && mouseX < x + SWATCH && mouseY >= y && mouseY < y + SWATCH) {
                setRgb(PRESETS[i]);
                syncHexField();
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long heldTime) {
        if (dragging == 1) {
            applySquare(mouseX, mouseY);
        } else if (dragging == 2) {
            applyHue(mouseY);
        } else if (dragging == 3) {
            applyAlpha(mouseY);
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        if (state != -1) {
            dragging = 0;
        }
    }

    private boolean inSquare(int x, int y) {
        return x >= squareLeft && x < squareLeft + SQUARE && y >= squareTop && y < squareTop + SQUARE;
    }

    private boolean inHueStrip(int x, int y) {
        return x >= hueLeft && x < hueLeft + HUE_WIDTH && y >= squareTop && y < squareTop + SQUARE;
    }

    private boolean inAlphaStrip(int x, int y) {
        return x >= alphaLeft && x < alphaLeft + HUE_WIDTH && y >= squareTop && y < squareTop + SQUARE;
    }

    private void applyAlpha(int mouseY) {
        alpha = 1f - clamp01((mouseY - squareTop) / (float) (SQUARE - 1));
        syncHexField();
    }

    private void applySquare(int mouseX, int mouseY) {
        saturation = clamp01((mouseX - squareLeft) / (float) (SQUARE - 1));
        value = 1f - clamp01((mouseY - squareTop) / (float) (SQUARE - 1));
        syncHexField();
    }

    private void applyHue(int mouseY) {
        hue = clamp01((mouseY - squareTop) / (float) (SQUARE - 1));
        syncHexField();
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void syncHexField() {
        hexField.setText(toHex());
        hexField.setCursorPositionZero();
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            confirm(toHex());
            return true;
        }
        if (hexField.textboxKeyTyped(typedChar, keyCode)) {
            // Only once the text is a complete colour: otherwise every keystroke of a half-typed value would drag
            // the crosshair somewhere meaningless.
            String text = hexField.getText().trim();
            if (withAlpha && Colors.hasAlpha(text)) {
                int argb = Colors.parseArgb(text, currentArgb());
                alpha = ((argb >>> 24) & 0xFF) / 255f;
                setRgb(argb & 0x00FFFFFF);
            } else if (text.length() == 7 || text.length() == 6) {
                // Six digits leave the opacity alone: the player is editing the colour, not clearing the alpha.
                setRgb(Colors.parseArgb(text, currentRgb()) & 0x00FFFFFF);
            }
            return true;
        }
        return false;
    }
}
