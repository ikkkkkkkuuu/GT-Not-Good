package com.xyp.ldlib.gui.ui.elements;

import java.util.Objects;
import java.util.function.Consumer;

import com.xyp.ldlib.gui.texture.ColorRectTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/** LDLib2 modal dialog host with arbitrary content and a reusable confirmation factory. */
public final class Dialog extends ModalLayer {

    public final UIElement content;

    public Dialog(UIElement owner, int width, int height, ModernTheme theme) {
        super(owner, false);
        setBackground(new ColorRectTexture(0x88000000));
        content = new UIElement(0, 0, width, height).setBackground(theme.panel);
        addChild(content);
    }

    @Override
    public void layout() {
        super.layout();
        content.setPosition(
            Math.max(0, (getWidth() - content.getWidth()) / 2),
            Math.max(0, (getHeight() - content.getHeight()) / 2));
    }

    /** Labels are supplied by the host for localization. Escape cancels without invoking the callback. */
    public static Dialog confirm(UIElement owner, ModernTheme theme, String message, String accept, String cancel,
        Consumer<Boolean> result) {
        Objects.requireNonNull(result);
        Dialog dialog = new Dialog(owner, 210, 88, theme);
        dialog.content.addChild(new Label(8, 8, 194, 32, message));
        dialog.content.addChild(theme.button(10, 52, 90, 22, () -> accept, () -> {
            dialog.close();
            result.accept(true);
        }));
        dialog.content.addChild(theme.button(110, 52, 90, 22, () -> cancel, () -> {
            dialog.close();
            result.accept(false);
        }));
        dialog.open();
        return dialog;
    }
}
