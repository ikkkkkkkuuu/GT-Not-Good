package com.xyp.ldlib.gui.ui.elements;

import net.minecraft.client.gui.Gui;

import com.xyp.ldlib.gui.render.ScissorScope;
import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/**
 * Vertical LDLib2 scroller adaptation. Content coordinates stay unchanged; the viewport
 * applies the same offset to rendering and hit testing. Wheel events bubble at either limit.
 */
public final class ScrollerView extends UIElement {

    private int scroll, contentHeight;
    private IGuiTexture thumbTexture;

    public ScrollerView setThumbTexture(IGuiTexture texture) {
        thumbTexture = texture;
        return this;
    }

    public ScrollerView(int x, int y, int width, int height) {
        super(x, y, width, height);
        addEventListener(UIEvents.MOUSE_WHEEL, event -> {
            int before = scroll;
            setScroll(scroll - Integer.signum(event.deltaY) * 20);
            if (before != scroll) {
                event.preventDefault();
                event.stopPropagation();
            }
        });
    }

    public int getScroll() {
        return scroll;
    }

    public int getMaxScroll() {
        return Math.max(0, contentHeight - height);
    }

    public void setScroll(int pixels) {
        scroll = Math.max(0, Math.min(pixels, getMaxScroll()));
    }

    @Override
    protected int childOffsetY() {
        return -scroll;
    }

    @Override
    public void reveal(UIElement descendant) {
        int relativeTop = descendant.getScreenY() - getScreenY();
        if (relativeTop < 0) setScroll(scroll + relativeTop);
        else if (relativeTop + descendant.getHeight() > height) {
            setScroll(scroll + relativeTop + descendant.getHeight() - height);
        }
    }

    @Override
    public void layout() {
        super.layout();
        contentHeight = 0;
        for (UIElement child : getChildren()) {
            if (child.isVisible()) contentHeight = Math.max(contentHeight, child.getY() + child.getHeight());
        }
        setScroll(scroll);
    }

    @Override
    protected void drawChildren(int mouseX, int mouseY, int parentX, int parentY) {
        try (ScissorScope ignored = new ScissorScope(parentX + x, parentY + y, width, height)) {
            super.drawChildren(mouseX, mouseY, parentX, parentY);
        }
    }

    @Override
    protected void drawForeground(int mouseX, int mouseY, int parentX, int parentY) {
        if (getMaxScroll() == 0 || height == 0) return;
        int thumb = Math.min(height, Math.max(8, height * height / contentHeight));
        int offset = scroll * (height - thumb) / getMaxScroll();
        if (thumbTexture != null) {
            thumbTexture.draw(mouseX, mouseY, parentX + x + width - 6, parentY + y + offset, 6, thumb);
            return;
        }
        Gui.drawRect(
            parentX + x + width - 3,
            parentY + y + offset,
            parentX + x + width,
            parentY + y + offset + thumb,
            0xFF5C839B);
    }
}
