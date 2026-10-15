package com.xyp.gtnotgood.client.radialmenu.client.gui.ui;

import net.minecraft.client.gui.Gui;

public class UiList {

    public final int left;
    public final int top;
    public final int right;
    public final int bottom;
    public final int rowHeight;

    private int scrollRow;

    public UiList(int left, int top, int right, int bottom, int rowHeight) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.rowHeight = rowHeight;
    }

    public int rowsVisible() {
        return Math.max(1, (bottom - top - 2) / rowHeight);
    }

    public int firstRow() {
        return scrollRow;
    }

    public void clampScroll(int itemCount) {
        scrollRow = Math.max(0, Math.min(Math.max(0, itemCount - rowsVisible()), scrollRow));
    }

    public void scroll(int wheelDelta, int itemCount) {
        if (wheelDelta == 0) {
            return;
        }
        scrollRow += wheelDelta > 0 ? -1 : 1;
        clampScroll(itemCount);
    }

    public void scrollTo(int row, int itemCount) {
        scrollRow = row;
        clampScroll(itemCount);
    }

    public int rowTop(int visibleIndex) {
        return top + 1 + visibleIndex * rowHeight;
    }

    public int itemAt(int mouseX, int mouseY, int itemCount) {
        if (mouseX < left || mouseX >= right) {
            return -1;
        }
        for (int row = 0; row < rowsVisible() && row + scrollRow < itemCount; row++) {
            int y = rowTop(row);
            if (mouseY >= y && mouseY < y + rowHeight) {
                return row + scrollRow;
            }
        }
        return -1;
    }

    public void drawFrame() {
        Ui.list(left, top, right, bottom);
    }

    /** Row background: a hover tint, a stronger one when selected. */
    public void drawRowBackground(int visibleIndex, boolean selected, boolean hovered) {
        if (!selected && !hovered) {
            return;
        }
        int y = rowTop(visibleIndex);
        Gui.drawRect(left + 1, y, right - 1, y + rowHeight, selected ? Ui.ROW_SELECTED : Ui.ROW_HOVER);
    }

    /** A scrollbar, drawn only when there is something to scroll. */
    public void drawScrollbar(int itemCount) {
        int rows = rowsVisible();
        if (itemCount <= rows) {
            return;
        }
        int trackHeight = bottom - top - 2;
        int thumbHeight = Math.max(12, trackHeight * rows / itemCount);
        int maxScroll = itemCount - rows;
        int thumbTop = top + 1 + (trackHeight - thumbHeight) * scrollRow / Math.max(1, maxScroll);

        Gui.drawRect(right - 4, top + 1, right - 2, bottom - 1, 0x30FFFFFF);
        Gui.drawRect(right - 4, thumbTop, right - 2, thumbTop + thumbHeight, 0xC0FFFFFF);
    }

    public int textLeft() {
        return left + 5;
    }

    public int textWidth() {
        return right - left - 12;
    }
}
