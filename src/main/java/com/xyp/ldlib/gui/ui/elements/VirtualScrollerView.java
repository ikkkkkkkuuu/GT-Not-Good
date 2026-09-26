package com.xyp.ldlib.gui.ui.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.lwjgl.input.Keyboard;

import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/**
 * Fixed-height LDLib2 virtual scroller. Only visible rows and one overscan row on each side exist.
 * Models are snapshotted on setItems; call it again after a model change. Row factories run on the client thread.
 * Keyboard navigation of a data selection belongs to the owning search/tree component.
 */
public final class VirtualScrollerView<T> extends ScrollerView {

    private List<T> items = Collections.emptyList();
    private final int rowHeight;
    private final BiFunction<T, Integer, UIElement> factory;
    private final Map<Integer, UIElement> rows = new LinkedHashMap<>();

    public VirtualScrollerView(int x, int y, int width, int height, int rowHeight,
        BiFunction<T, Integer, UIElement> factory) {
        super(x, y, width, height);
        if (rowHeight <= 0) throw new IllegalArgumentException("Invalid row height");
        this.rowHeight = rowHeight;
        this.factory = Objects.requireNonNull(factory);
        setFocusable(true);
        addEventListener(UIEvents.KEY_DOWN, event -> {
            int next;
            if (event.keyCode == Keyboard.KEY_PRIOR) next = Math.max(0, getScroll() - height);
            else if (event.keyCode == Keyboard.KEY_NEXT)
                next = (int) Math.min(getMaxScroll(), (long) getScroll() + height);
            else return;
            setScroll(next);
            event.preventDefault();
            event.stopPropagation();
        });
    }

    public VirtualScrollerView<T> setItems(List<T> items) {
        Objects.requireNonNull(items);
        if ((long) items.size() * rowHeight > Integer.MAX_VALUE)
            throw new IllegalArgumentException("List extent overflow");
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        clearAllChildren();
        rows.clear();
        updateContentHeight();
        setScroll(getScroll());
        return this;
    }

    public List<T> getItems() {
        return items;
    }

    public int getInstantiatedRowCount() {
        return rows.size();
    }

    /** Reveals a model index without instantiating all preceding rows. */
    public void revealIndex(int index) {
        if (index < 0 || index >= items.size()) return;
        int top = index * rowHeight;
        if (top < getScroll()) setScroll(top);
        else if (top + rowHeight > getScroll() + height) setScroll(top + rowHeight - height);
    }

    @Override
    protected void updateContentHeight() {
        contentHeight = items.size() * rowHeight;
    }

    @Override
    public void layout() {
        updateContentHeight();
        setScroll(getScroll());
        int first = Math.max(0, getScroll() / rowHeight - 1);
        int end = Math.min(
            items.size(),
            (int) Math.min(Integer.MAX_VALUE, ((long) getScroll() + height + rowHeight - 1) / rowHeight + 1));
        rows.entrySet()
            .removeIf(entry -> {
                if (entry.getKey() >= first && entry.getKey() < end) return false;
                removeChild(entry.getValue());
                return true;
            });
        for (int i = first; i < end; i++) {
            UIElement row = rows.get(i);
            if (row == null) {
                row = Objects.requireNonNull(factory.apply(items.get(i), i));
                addChild(row);
                rows.put(i, row);
            }
            row.setPosition(0, i * rowHeight);
            row.setSize(Math.max(0, width - 6), rowHeight);
        }
        super.layout();
    }
}
