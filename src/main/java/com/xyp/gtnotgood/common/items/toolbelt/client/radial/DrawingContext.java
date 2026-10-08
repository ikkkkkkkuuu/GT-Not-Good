package com.xyp.gtnotgood.common.items.toolbelt.client.radial;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

public class DrawingContext {

    public final float x;
    public final float y;
    public final float z;
    public final FontRenderer fontRenderer;
    public final Gui drawingHelper;

    public DrawingContext(float x, float y, float z, FontRenderer fontRenderer, Gui drawingHelper) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.fontRenderer = fontRenderer;
        this.drawingHelper = drawingHelper;
    }
}
