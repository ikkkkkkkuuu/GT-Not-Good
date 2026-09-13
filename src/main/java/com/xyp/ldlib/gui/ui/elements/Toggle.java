package com.xyp.ldlib.gui.ui.elements;

import java.util.function.Consumer;

import org.lwjgl.input.Keyboard;

import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/** LDLib2-style two-state control; setValue is silent and user activation notifies once. */
public final class Toggle extends UIElement {

    private boolean value;
    private final IGuiTexture off, on;
    private Consumer<Boolean> onChange = state -> {};

    public Toggle(int x, int y, int width, int height, IGuiTexture off, IGuiTexture on) {
        super(x, y, width, height);
        this.off = off;
        this.on = on;
        setFocusable(true);
        addEventListener(UIEvents.CLICK, e -> {
            if (e.button == 0) {
                toggle();
                e.preventDefault();
                e.stopPropagation();
            }
        });
        addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == Keyboard.KEY_SPACE || e.keyCode == Keyboard.KEY_RETURN) {
                toggle();
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }

    private void toggle() {
        value = !value;
        onChange.accept(value);
    }

    public boolean getValue() {
        return value;
    }

    public Toggle setValue(boolean value) {
        this.value = value;
        return this;
    }

    public Toggle setOnChange(Consumer<Boolean> callback) {
        onChange = java.util.Objects.requireNonNull(callback);
        return this;
    }

    @Override
    protected void drawBackground(int mx, int my, int px, int py) {
        (value ? on : off).draw(mx, my, px + x, py + y, width, height);
    }
}
