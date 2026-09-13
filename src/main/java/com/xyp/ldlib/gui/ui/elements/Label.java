package com.xyp.ldlib.gui.ui.elements;

import java.util.function.Supplier;

import com.xyp.ldlib.gui.texture.TextTexture;
import com.xyp.ldlib.gui.ui.UIElement;

/** Supplied text label corresponding to LDLib2 Label, without server or editor bindings. */
public final class Label extends UIElement {

    public Label(int x, int y, int width, int height, Supplier<String> text) {
        super(x, y, width, height);
        setBackground(new TextTexture(text, 0xFF202830));
    }

    public Label(int x, int y, int width, int height, String text) {
        this(x, y, width, height, () -> text);
    }
}
