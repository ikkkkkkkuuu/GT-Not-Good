package com.xyp.gtnotgood.common.network;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;

/** XNet-style beveled gray surfaces; original licensed artwork is registered centrally in GTNGGuiTextures. */
final class NetworkGuiStyle {

    static final IDrawable FRAME = com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures.NETWORK_FRAME;
    static final IDrawable PANEL = surface(0xFF8B8B8B, 0xFFFFFFFF, 0xFF373737);
    static final IDrawable DIVIDER = new Rectangle().color(0xFF686868);
    static final IDrawable COLUMN = new Rectangle().color(0xFFA1B7B7);
    static final IDrawable BUTTON = surface(0xFFC6C6C6, 0xFF373737, 0xFFFFFFFF);
    static final IDrawable SELECTED = surface(0xFFA1B7B7, 0xFF373737, 0xFFE8FFFF);
    static final IDrawable EXTRACT = BUTTON;
    static final IDrawable INSERT = BUTTON;
    static final IDrawable EMPTY = surface(0xFF8B8B8B, 0xFF373737, 0xFFFFFFFF);
    static final IDrawable INPUT = surface(0xFFC6C6C6, 0xFFFFFFFF, 0xFF373737);
    static final IDrawable SLOT = surface(0xFF8B8B8B, 0xFFFFFFFF, 0xFF373737);

    private NetworkGuiStyle() {}

    /** Draws the raised top/left and shaded bottom/right edges without allocating objects per frame. */
    private static IDrawable surface(int fill, int border, int accent) {
        Rectangle edge = new Rectangle().color(border);
        Rectangle body = new Rectangle().color(fill);
        Rectangle highlight = new Rectangle().color(accent);
        Rectangle shade = new Rectangle().color(0xFF707070);
        return (context, x, y, width, height, theme) -> {
            edge.draw(context, x, y, width, height, theme);
            body.draw(context, x + 1, y + 1, Math.max(0, width - 2), Math.max(0, height - 2), theme);
            highlight.draw(context, x + 1, y, Math.max(0, width - 2), 1, theme);
            highlight.draw(context, x, y, 1, Math.max(0, height - 1), theme);
            if (fill == 0xFFC6C6C6 && border != 0xFFFFFFFF) {
                highlight.draw(context, x + 1, y + 1, Math.max(0, width - 3), 1, theme);
                highlight.draw(context, x + 1, y + 1, 1, Math.max(0, height - 3), theme);
                shade.draw(context, x + 2, y + height - 2, Math.max(0, width - 3), 1, theme);
                shade.draw(context, x + width - 2, y + 2, 1, Math.max(0, height - 3), theme);
            }
        };
    }
}
