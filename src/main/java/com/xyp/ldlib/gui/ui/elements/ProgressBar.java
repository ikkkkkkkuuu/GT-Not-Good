package com.xyp.ldlib.gui.ui.elements;

import java.util.Objects;
import java.util.function.DoubleSupplier;

import com.xyp.ldlib.gui.render.ScissorScope;
import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.ui.UIElement;

/** LDLib2 directional progress fill, clipped without stretching its texture; values are client display data. */
public final class ProgressBar extends UIElement {

    /** Direction in which the foreground reveals its full-size texture. */
    public enum FillDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        BOTTOM_TO_TOP,
        TOP_TO_BOTTOM
    }

    private final IGuiTexture fill;
    private DoubleSupplier supplier;
    private double value, displayed;
    private double interpolation = 1;
    private FillDirection direction = FillDirection.LEFT_TO_RIGHT;

    public ProgressBar(int x, int y, int width, int height, IGuiTexture background, IGuiTexture fill) {
        super(x, y, width, height);
        setBackground(background);
        this.fill = Objects.requireNonNull(fill);
    }

    public ProgressBar setValue(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite progress");
        this.value = Math.max(0, Math.min(1, value));
        if (interpolation == 1) displayed = this.value;
        return this;
    }

    public double getValue() {
        return value;
    }

    public ProgressBar setSupplier(DoubleSupplier supplier) {
        this.supplier = Objects.requireNonNull(supplier);
        return this;
    }

    /** Sets the fraction of remaining distance applied per client tick, in (0, 1]. */
    public ProgressBar setInterpolation(double fraction) {
        if (!Double.isFinite(fraction) || fraction <= 0 || fraction > 1)
            throw new IllegalArgumentException("Invalid interpolation");
        interpolation = fraction;
        return this;
    }

    public ProgressBar setDirection(FillDirection direction) {
        this.direction = Objects.requireNonNull(direction);
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        if (supplier != null) setValue(supplier.getAsDouble());
        displayed += (value - displayed) * interpolation;
        if (Math.abs(value - displayed) < .0001) displayed = value;
    }

    @Override
    protected void drawForeground(int mx, int my, int px, int py) {
        int left = px + x, top = py + y, w = width, h = height;
        switch (direction) {
            case LEFT_TO_RIGHT:
                w = (int) Math.round(width * displayed);
                break;
            case RIGHT_TO_LEFT:
                w = (int) Math.round(width * displayed);
                left += width - w;
                break;
            case TOP_TO_BOTTOM:
                h = (int) Math.round(height * displayed);
                break;
            case BOTTOM_TO_TOP:
                h = (int) Math.round(height * displayed);
                top += height - h;
                break;
            default:
                break;
        }
        if (w <= 0 || h <= 0) return;
        try (ScissorScope ignored = new ScissorScope(left, top, w, h)) {
            fill.draw(mx, my, px + x, py + y, width, height);
        }
    }
}
