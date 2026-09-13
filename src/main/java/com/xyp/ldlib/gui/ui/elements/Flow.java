package com.xyp.ldlib.gui.ui.elements;

import com.xyp.ldlib.gui.ui.UIElement;

/** Java-only row/column layout for fixed-size LDLib2 children, with padding and gaps. */
public final class Flow extends UIElement {

    private final boolean vertical;
    private int gap, padding;

    public Flow(int x, int y, int width, int height, boolean vertical) {
        super(x, y, width, height);
        this.vertical = vertical;
    }

    public Flow setGap(int gap) {
        if (gap < 0) throw new IllegalArgumentException("Negative gap");
        this.gap = gap;
        return this;
    }

    public Flow setPadding(int padding) {
        if (padding < 0) throw new IllegalArgumentException("Negative padding");
        this.padding = padding;
        return this;
    }

    @Override
    public void layout() {
        int cursor = padding;
        for (UIElement child : getChildren()) {
            if (!child.isVisible()) continue;
            child.layout();
            child.setPosition(vertical ? padding : cursor, vertical ? cursor : padding);
            cursor += (vertical ? child.getHeight() : child.getWidth()) + gap;
        }
    }
}
