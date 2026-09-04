package com.xyp.gtnotgood.common.items.toolbelt.client.radial;

import net.minecraft.client.gui.FontRenderer;

public class DrawingContext {

    public final float x;
    public final float y;
    public final float z;
    public final FontRenderer fontRenderer;
    public final net.minecraft.client.gui.Gui drawingHelper;

    public DrawingContext(float x, float y, float z, FontRenderer fontRenderer,
        net.minecraft.client.gui.Gui drawingHelper) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.fontRenderer = fontRenderer;
        this.drawingHelper = drawingHelper;
    }
}
