package com.xyp.ldlib.gui.ui.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.lwjgl.input.Keyboard;

import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvents;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/**
 * LDLib2 searchable selector adapted to a root popup and a virtual result list.
 * Search runs against a client-side snapshot; applications supply localized names and synchronize selections
 * separately.
 */
public final class SearchComponent<T> extends UIElement {

    private final ModernTheme theme;
    private final Function<T, String> names;
    private List<T> candidates = Collections.emptyList();
    private Consumer<T> onChange = ignored -> {};
    private T value;
    private Popup popup;

    public SearchComponent(int x, int y, int width, int height, ModernTheme theme, Function<T, String> names) {
        super(x, y, width, height);
        this.theme = Objects.requireNonNull(theme);
        this.names = Objects.requireNonNull(names);
        addChild(theme.button(0, 0, width, height, () -> value == null ? "..." : names.apply(value), this::show));
    }

    public SearchComponent<T> setCandidates(List<T> candidates) {
        this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
        if (popup != null) popup.close();
        return this;
    }

    public SearchComponent<T> setValue(T value) {
        this.value = value;
        return this;
    }

    public T getValue() {
        return value;
    }

    public SearchComponent<T> setOnChange(Consumer<T> callback) {
        onChange = Objects.requireNonNull(callback);
        return this;
    }

    /** Locale-independent matching; an empty query returns every candidate in source order. */
    public List<T> search(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<T> result = new ArrayList<>();
        for (T item : candidates) if (names.apply(item)
            .toLowerCase(Locale.ROOT)
            .contains(needle)) result.add(item);
        return result;
    }

    public void show() {
        if (popup != null) popup.close();
        popup = new Popup();
        popup.open();
        popup.layout();
    }

    @Override
    public void layout() {
        getChildren().get(0)
            .setSize(width, height);
        super.layout();
    }

    /** Popup owns its editor and result snapshot, so dismissing it never leaves detached keyboard focus. */
    private final class Popup extends ModalLayer {

        private final UIElement panel;
        private final VirtualScrollerView<T> results;
        private int selected;

        Popup() {
            super(SearchComponent.this, true);
            panel = new UIElement(0, 0, Math.max(120, SearchComponent.this.width), 132).setBackground(theme.panel);
            TextField field = theme.textField(3, 3, panel.getWidth() - 6, 20);
            results = new VirtualScrollerView<>(3, 26, panel.getWidth() - 6, 102, 20, (item, index) -> {
                UIElement row = new UIElement(0, 0, 0, 20);
                row.setBackground(
                    (mx, my, x, y, w, h) -> theme
                        .text(index == selected ? theme.accent : theme.button, () -> names.apply(item))
                        .draw(mx, my, x, y, w, h));
                row.addEventListener(UIEvents.CLICK, e -> { if (e.button == 0) choose(item); });
                return row;
            });
            results.setItems(search(""));
            results.setThumbTexture(theme.scrollThumb);
            field.setOnChange(query -> {
                selected = 0;
                results.setItems(search(query));
                results.setScroll(0);
            });
            panel.addChild(field);
            panel.addChild(results);
            addChild(panel);
            addEventListener(UIEvents.KEY_DOWN, e -> {
                int count = results.getItems()
                    .size();
                if (e.keyCode == Keyboard.KEY_UP || e.keyCode == Keyboard.KEY_DOWN) {
                    selected = Math.max(0, Math.min(count - 1, selected + (e.keyCode == Keyboard.KEY_UP ? -1 : 1)));
                    results.revealIndex(selected);
                } else if (e.keyCode == Keyboard.KEY_RETURN || e.keyCode == Keyboard.KEY_NUMPADENTER) {
                    if (count > 0) choose(
                        results.getItems()
                            .get(selected));
                } else return;
                e.preventDefault();
                e.stopPropagation();
            }, true);
        }

        private void choose(T item) {
            close();
            if (!Objects.equals(value, item)) {
                value = item;
                onChange.accept(item);
            }
        }

        @Override
        public void layout() {
            super.layout();
            int w = Math.min(getWidth(), Math.max(120, SearchComponent.this.width));
            int h = Math.min(getHeight(), 132);
            int left = SearchComponent.this.getScreenX() - getScreenX();
            int top = SearchComponent.this.getScreenY() - getScreenY() + SearchComponent.this.height;
            if (top + h > getHeight()) top -= h + SearchComponent.this.height;
            panel.setPosition(Math.max(0, Math.min(left, getWidth() - w)), Math.max(0, top));
            panel.setSize(w, h);
            panel.getChildren()
                .get(0)
                .setSize(Math.max(0, w - 6), 20);
            results.setSize(Math.max(0, w - 6), Math.max(0, h - 30));
            results.layout();
        }
    }
}
