package com.xyp.gtnotgood.client.radialmenu.client.gui.ui;

import net.minecraft.client.gui.Gui;

public final class UiDragReorder {

    /** Width of the grip column. Narrow enough not to crowd the row, wide enough to aim at. */
    public static final int GRIP = 8;

    /** How far the pointer moves before a press becomes a drag, so a shaky click stays a click. */
    private static final int THRESHOLD = 3;

    public interface Rows {

        int count();

        int top(int row);

        int height();
    }

    private int grabbed = -1;
    private int startY;
    private int pointerY;
    private boolean dragging;

    public boolean isDragging() {
        return dragging;
    }

    public int grabbedRow() {
        return grabbed;
    }

    public boolean press(int mouseX, int mouseY, int gripLeft, Rows rows) {
        if (mouseX < gripLeft || mouseX >= gripLeft + GRIP) {
            return false;
        }
        for (int row = 0; row < rows.count(); row++) {
            int top = rows.top(row);
            if (mouseY >= top && mouseY < top + rows.height()) {
                grabbed = row;
                startY = mouseY;
                pointerY = mouseY;
                dragging = false;
                return true;
            }
        }
        return false;
    }

    public void moveTo(int mouseY) {
        if (grabbed < 0) {
            return;
        }
        pointerY = mouseY;
        if (Math.abs(mouseY - startY) >= THRESHOLD) {
            dragging = true;
        }
    }

    public int overshoot(Rows rows) {
        if (!dragging || rows.count() == 0) {
            return 0;
        }
        int top = rows.top(0);
        int bottom = rows.top(rows.count() - 1) + rows.height();
        if (pointerY < top) {
            return pointerY - top;
        }
        return pointerY > bottom ? pointerY - bottom : 0;
    }

    /** Ends the gesture. @return the row the dragged one should become, or -1 if this was a click rather than a drag */
    public int release(Rows rows) {
        int target = -1;
        if (dragging && grabbed >= 0) {
            int insertion = insertionIndex(rows);
            // Removing the row first shifts everything after it up, so an insertion below its old place lands one
            // short. This is the one place that correction belongs; the screens just move a row to an index.
            target = insertion > grabbed ? insertion - 1 : insertion;
        }
        grabbed = -1;
        dragging = false;
        return target;
    }

    public void cancel() {
        grabbed = -1;
        dragging = false;
    }

    public int insertionIndex(Rows rows) {
        int index = 0;
        while (index < rows.count() && pointerY > rows.top(index) + rows.height() / 2) {
            index++;
        }
        return index;
    }

    public void drawInsertion(Rows rows, int left, int right) {
        if (!dragging || rows.count() == 0) {
            return;
        }
        int index = insertionIndex(rows);
        int y = index < rows.count() ? rows.top(index) - 2 : rows.top(rows.count() - 1) + rows.height();
        Gui.drawRect(left, y, right, y + 1, Ui.TEXT_HEADER);
    }

    public static void drawGrip(int left, int top, int height, boolean hot) {
        int colour = hot ? Ui.TEXT : Ui.CODE_GUTTER;
        for (int row = 0; row < 3; row++) {
            int y = top + height / 2 - 4 + row * 3;
            Gui.drawRect(left + 1, y, left + 3, y + 2, colour);
            Gui.drawRect(left + 5, y, left + 7, y + 2, colour);
        }
    }
}
