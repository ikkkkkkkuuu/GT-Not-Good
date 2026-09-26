package com.xyp.ldlib.gui.ui.elements;

import java.util.Objects;
import java.util.function.DoubleConsumer;

import org.lwjgl.input.Keyboard;

import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvent;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/** LDLib2 range slider with capture-based dragging, discrete steps and keyboard navigation. */
public final class Slider extends UIElement {

    private final double min, max, step;
    private final boolean vertical;
    private final IGuiTexture handle;
    private double value;
    private DoubleConsumer onChange = ignored -> {};

    public Slider(int x, int y, int width, int height, double min, double max, double step, boolean vertical,
        IGuiTexture track, IGuiTexture handle) {
        super(x, y, width, height);
        if (!Double.isFinite(min) || !Double.isFinite(max)
            || !Double.isFinite(max - min)
            || max <= min
            || !Double.isFinite(step)
            || step <= 0) throw new IllegalArgumentException("Invalid slider range");
        this.min = min;
        this.max = max;
        this.step = step;
        this.vertical = vertical;
        this.handle = Objects.requireNonNull(handle);
        value = min;
        setBackground(track);
        setFocusable(true);
        addEventListener(UIEvents.MOUSE_DOWN, this::drag);
        addEventListener(UIEvents.MOUSE_MOVE, this::drag);
        addEventListener(UIEvents.KEY_DOWN, e -> {
            double next;
            switch (e.keyCode) {
                case Keyboard.KEY_LEFT:
                case Keyboard.KEY_DOWN:
                    next = value - step;
                    break;
                case Keyboard.KEY_RIGHT:
                case Keyboard.KEY_UP:
                    next = value + step;
                    break;
                case Keyboard.KEY_HOME:
                    next = min;
                    break;
                case Keyboard.KEY_END:
                    next = max;
                    break;
                default:
                    return;
            }
            change(next);
            e.preventDefault();
            e.stopPropagation();
        });
    }

    private void drag(UIEvent e) {
        if (e.button != 0) return;
        int length = vertical ? height : width;
        double fraction = ((vertical ? e.y - getScreenY() : e.x - getScreenX()) - handleSize() / 2d)
            / Math.max(1, length - handleSize());
        change(min + Math.max(0, Math.min(1, vertical ? 1 - fraction : fraction)) * (max - min));
        e.preventDefault();
        e.stopPropagation();
    }

    private int handleSize() {
        return Math.min(8, vertical ? height : width);
    }

    /** Programmatic updates are silent, clamp to the range and snap relative to its minimum. */
    public Slider setValue(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite slider value");
        double clamped = Math.max(min, Math.min(max, value));
        this.value = clamped == max ? max
            : Math.max(min, Math.min(max, min + Math.round((clamped - min) / step) * step));
        return this;
    }

    private void change(double next) {
        double old = value;
        setValue(next);
        if (old != value) onChange.accept(value);
    }

    public double getValue() {
        return value;
    }

    public Slider setOnChange(DoubleConsumer callback) {
        onChange = Objects.requireNonNull(callback);
        return this;
    }

    @Override
    protected void drawForeground(int mx, int my, int px, int py) {
        double fraction = (value - min) / (max - min);
        int size = handleSize();
        int offset = (int) Math.round((vertical ? 1 - fraction : fraction) * ((vertical ? height : width) - size));
        handle.draw(
            mx,
            my,
            px + x + (vertical ? 0 : offset),
            py + y + (vertical ? offset : 0),
            vertical ? width : size,
            vertical ? size : height);
    }
}
