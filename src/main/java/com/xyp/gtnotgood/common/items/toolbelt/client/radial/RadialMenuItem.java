package com.xyp.gtnotgood.common.items.toolbelt.client.radial;

import lombok.Getter;

public abstract class RadialMenuItem {

    @Getter
    private final GenericRadialMenu menu;

    protected RadialMenuItem(GenericRadialMenu menu) {
        this.menu = menu;
    }

    public abstract void draw(DrawingContext context);

    public abstract void drawTooltip(DrawingContext context);

    public abstract boolean onClick();

    public boolean isVisible() {
        return true;
    }
}
