package com.xyp.ldlib.gui.ui.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/** LDLib2 hierarchical popup menu. Opens at screen coordinates, clamps panels and closes before actions. */
public final class Menu extends ModalLayer {

    /** Immutable leaf action or submenu, with an explicit enabled state. */
    public static final class Entry {

        public final String label;
        public final boolean enabled;
        private final Runnable action;
        private final List<Entry> children;

        public Entry(String label, boolean enabled, Runnable action) {
            this.label = Objects.requireNonNull(label);
            this.enabled = enabled;
            this.action = Objects.requireNonNull(action);
            children = Collections.emptyList();
        }

        public Entry(String label, Entry... children) {
            this.label = Objects.requireNonNull(label);
            this.children = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(children)));
            enabled = children.length > 0;
            action = null;
        }
    }

    private final ModernTheme theme;
    private final List<UIElement> panels = new ArrayList<>();

    public Menu(UIElement owner, ModernTheme theme) {
        super(owner, true);
        this.theme = Objects.requireNonNull(theme);
    }

    public void openAt(int screenX, int screenY, List<Entry> entries) {
        open();
        showPanel(0, screenX - getScreenX(), screenY - getScreenY(), entries);
    }

    private void showPanel(int depth, int left, int top, List<Entry> entries) {
        while (panels.size() > depth) removeChild(panels.remove(panels.size() - 1));
        int panelWidth = Math.min(140, getWidth());
        int panelHeight = Math.min(getHeight(), entries.size() * 22 + 4);
        left = Math.max(0, Math.min(left, getWidth() - panelWidth));
        top = Math.max(0, Math.min(top, getHeight() - panelHeight));
        ScrollerView panel = theme.scroller(left, top, panelWidth, panelHeight);
        panel.setBackground(theme.panel);
        panels.add(panel);
        addChild(panel);
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            final int row = i;
            panel.addChild(
                theme.button(
                    2,
                    2 + i * 22,
                    Math.max(0, panelWidth - 8),
                    22,
                    () -> entry.label + (entry.children.isEmpty() ? "" : " >"),
                    () -> {
                        if (!entry.children.isEmpty()) {
                            int nextX = panel.getX() + panel.getWidth();
                            if (nextX + panelWidth > getWidth()) nextX = panel.getX() - panelWidth;
                            showPanel(
                                depth + 1,
                                nextX,
                                panel.getY() + 2 + row * 22 - panel.getScroll(),
                                entry.children);
                        } else {
                            close();
                            entry.action.run();
                        }
                    })
                    .setEnabled(entry.enabled));
        }
    }
}
