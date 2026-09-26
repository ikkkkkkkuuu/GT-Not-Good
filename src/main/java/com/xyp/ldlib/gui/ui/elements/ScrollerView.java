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
public class ScrollerView extends UIElement {

    private int scroll;
    protected int contentHeight;
    private IGuiTexture thumbTexture;
    private boolean draggingThumb;
    private int grabOffset;

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
        addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button != 0 || event.target != this || getMaxScroll() == 0 || event.x < getScreenX() + width - 6)
                return;
            int thumb = thumbSize();
            int offset = (int) ((long) scroll * (height - thumb) / getMaxScroll());
            int localY = event.y - getScreenY();
            grabOffset = localY >= offset && localY < offset + thumb ? localY - offset : thumb / 2;
            draggingThumb = true;
            dragThumb(localY);
            event.preventDefault();
            event.stopPropagation();
        });
        addEventListener(UIEvents.MOUSE_MOVE, event -> {
            if (!draggingThumb || event.button != 0) return;
            dragThumb(event.y - getScreenY());
            event.preventDefault();
            event.stopPropagation();
        });
        addEventListener(UIEvents.MOUSE_UP, event -> draggingThumb = false);
    }

    private int thumbSize() {
        return contentHeight == 0 ? height
            : (int) Math.min(height, Math.max(8, (long) height * height / contentHeight));
    }

    private void dragThumb(int localY) {
        int travel = height - thumbSize();
        if (travel > 0)
            setScroll((int) ((long) Math.max(0, Math.min(travel, localY - grabOffset)) * getMaxScroll() / travel));
    }

    @Override
    public UIElement hitTest(int mx, int my) {
        UIElement hit = super.hitTest(mx, my);
        if (hit != null && getMaxScroll() > 0 && mx >= getScreenX() + width - 6) return this;
        return hit;
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
        updateContentHeight();
        setScroll(scroll);
    }

    /** Virtual lists override this using model extent rather than instantiated children. */
    protected void updateContentHeight() {
        contentHeight = 0;
        for (UIElement child : getChildren()) {
            if (child.isVisible()) contentHeight = Math.max(contentHeight, child.getY() + child.getHeight());
        }
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
        int thumb = thumbSize();
        int offset = (int) ((long) scroll * (height - thumb) / getMaxScroll());
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
