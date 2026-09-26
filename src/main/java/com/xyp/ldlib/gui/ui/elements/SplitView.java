package com.xyp.ldlib.gui.ui.elements;

import java.util.Objects;
import java.util.function.DoubleConsumer;

import org.lwjgl.input.Keyboard;

import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvent;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/** Two clipped panes with a captured draggable divider, adapted from LDLib2 SplitView. */
public final class SplitView extends UIElement {

    public final ScrollerView first = new ScrollerView(0, 0, 0, 0);
    public final ScrollerView second = new ScrollerView(0, 0, 0, 0);
    private final UIElement divider;
    private final boolean vertical;
    private double percentage = .5;
    private int minimum = 24;
    private DoubleConsumer onChange = ignored -> {};

    public SplitView(int x, int y, int width, int height, boolean vertical, IGuiTexture dividerTexture) {
        super(x, y, width, height);
        this.vertical = vertical;
        divider = new UIElement(0, 0, 4, 4).setBackground(dividerTexture)
            .setFocusable(true);
        divider.addEventListener(UIEvents.MOUSE_DOWN, this::drag);
        divider.addEventListener(UIEvents.MOUSE_MOVE, this::drag);
        divider.addEventListener(UIEvents.KEY_DOWN, e -> {
            int direction = e.keyCode == Keyboard.KEY_LEFT || e.keyCode == Keyboard.KEY_UP ? -1
                : e.keyCode == Keyboard.KEY_RIGHT || e.keyCode == Keyboard.KEY_DOWN ? 1 : 0;
            if (direction == 0) return;
            change(percentage + direction * .05);
            e.preventDefault();
            e.stopPropagation();
        });
        addChild(first);
        addChild(second);
        addChild(divider);
    }

    public SplitView setMinimumPaneSize(int pixels) {
        if (pixels < 0) throw new IllegalArgumentException("Negative minimum");
        minimum = pixels;
        setPercentage(percentage);
        return this;
    }

    /** Sets the first pane's fraction in [0,1], respecting both panes' minimum when space permits. */
    public SplitView setPercentage(double fraction) {
        if (!Double.isFinite(fraction)) throw new IllegalArgumentException("Non-finite split");
        int available = Math.max(0, (vertical ? height : width) - 4);
        double lower = available == 0 ? .5 : Math.min(.5, (double) minimum / available);
        percentage = Math.max(lower, Math.min(1 - lower, fraction));
        return this;
    }

    public double getPercentage() {
        return percentage;
    }

    public SplitView setOnChange(DoubleConsumer callback) {
        onChange = Objects.requireNonNull(callback);
        return this;
    }

    private void change(double fraction) {
        double old = percentage;
        setPercentage(fraction);
        if (old != percentage) onChange.accept(percentage);
    }

    private void drag(UIEvent e) {
        if (e.button != 0) return;
        change(
            (double) (vertical ? e.y - getScreenY() - 2 : e.x - getScreenX() - 2)
                / Math.max(1, (vertical ? height : width) - 4));
        e.preventDefault();
        e.stopPropagation();
    }

    @Override
    public void layout() {
        setPercentage(percentage);
        int size = Math.min(4, vertical ? height : width);
        int available = (vertical ? height : width) - size;
        int split = (int) Math.round(available * percentage);
        first.setSize(vertical ? width : split, vertical ? split : height);
        divider.setPosition(vertical ? 0 : split, vertical ? split : 0);
        divider.setSize(vertical ? width : size, vertical ? size : height);
        second.setPosition(vertical ? 0 : split + size, vertical ? split + size : 0);
        second.setSize(vertical ? width : available - split, vertical ? available - split : height);
        super.layout();
    }
}
