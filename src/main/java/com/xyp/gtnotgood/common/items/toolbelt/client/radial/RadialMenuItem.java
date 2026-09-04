package com.xyp.gtnotgood.common.items.toolbelt.client.radial;

public abstract class RadialMenuItem {

    private final GenericRadialMenu menu;

    protected RadialMenuItem(GenericRadialMenu menu) {
        this.menu = menu;
    }

    public GenericRadialMenu getMenu() {
        return menu;
    }

    public abstract void draw(DrawingContext context);

    public abstract void drawTooltip(DrawingContext context);

    public abstract boolean onClick();

    public boolean isVisible() {
        return true;
    }
}
