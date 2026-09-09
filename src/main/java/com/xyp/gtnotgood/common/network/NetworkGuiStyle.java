package com.xyp.gtnotgood.common.network;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;

/** Shared pixel-aligned graphite surfaces and restrained cyan selection accents for network screens. */
final class NetworkGuiStyle {

    static final IDrawable FRAME = surface(0xFF101820, 0xFF536879, 0xFF60C9D5);
    static final IDrawable PANEL = surface(0xFF1A2530, 0xFF304352, 0xFF304352);
    static final IDrawable BUTTON = surface(0xFF293B49, 0xFF486170, 0xFF587383);
    static final IDrawable SELECTED = surface(0xFF205161, 0xFF6CD7DF, 0xFFB4F1EE);
    static final IDrawable EXTRACT = surface(0xFF24463C, 0xFF4B8870, 0xFF6BA78D);
    static final IDrawable INSERT = surface(0xFF283E58, 0xFF527CA2, 0xFF719ABF);
    static final IDrawable EMPTY = surface(0xFF18232D, 0xFF334653, 0xFF334653);

    private NetworkGuiStyle() {}

    /** Draws a one-pixel border and upper highlight without scaling raster corners or allocating per frame. */
    private static IDrawable surface(int fill, int border, int accent) {
        Rectangle edge = new Rectangle().color(border);
        Rectangle body = new Rectangle().color(fill);
        Rectangle highlight = new Rectangle().color(accent);
        return (context, x, y, width, height, theme) -> {
            edge.draw(context, x, y, width, height, theme);
            body.draw(context, x + 1, y + 1, Math.max(0, width - 2), Math.max(0, height - 2), theme);
            highlight.draw(context, x + 1, y, Math.max(0, width - 2), 1, theme);
        };
    }
}
