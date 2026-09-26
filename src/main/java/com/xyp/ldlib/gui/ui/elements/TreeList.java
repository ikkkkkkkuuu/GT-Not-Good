package com.xyp.ldlib.gui.ui.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.lwjgl.input.Keyboard;

import com.xyp.ldlib.gui.texture.TextTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvents;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/** Expandable LDLib2 tree with identity-stable selection, keyboard navigation and virtualized visible rows. */
public final class TreeList<T> extends UIElement {

    /** Exclusively owned tree node. Mutations require TreeList.refresh(), and cycles are rejected. */
    public static final class Node<T> {

        public final T value;
        private Node<T> parent;
        private final List<Node<T>> children = new ArrayList<>();

        public Node(T value) {
            this.value = value;
        }

        public Node<T> getParent() {
            return parent;
        }

        public List<Node<T>> getChildren() {
            return Collections.unmodifiableList(children);
        }

        public Node<T> add(Node<T> child) {
            Objects.requireNonNull(child);
            for (Node<T> node = this; node != null; node = node.parent) {
                if (node == child) throw new IllegalArgumentException("Tree cycle");
            }
            if (child.parent != null) throw new IllegalArgumentException("Node already owned");
            child.parent = this;
            children.add(child);
            return this;
        }

        public boolean remove(Node<T> child) {
            if (!children.remove(child)) return false;
            child.parent = null;
            return true;
        }
    }

    private final Set<Node<T>> expanded = Collections.newSetFromMap(new IdentityHashMap<>());
    private final VirtualScrollerView<Node<T>> rows;
    private Node<T> root, selected;
    private Consumer<Node<T>> onChange = ignored -> {};

    public TreeList(int x, int y, int width, int height, ModernTheme theme, Function<T, String> names) {
        super(x, y, width, height);
        setFocusable(true);
        rows = new VirtualScrollerView<>(0, 0, width, height, 20, (node, index) -> {
            UIElement row = new UIElement(0, 0, 0, 20);
            row.setBackground((mx, my, left, top, w, h) -> {
                (node == selected ? theme.accent : theme.button).draw(mx, my, left, top, w, h);
                int indent = Math.min(Math.max(0, w - 20), depth(node) * 12);
                String arrow = node.children.isEmpty() ? "  " : expanded.contains(node) ? "- " : "+ ";
                new TextTexture(() -> arrow + names.apply(node.value), 0xFF202830)
                    .draw(mx, my, left + indent, top, w - indent, h);
            });
            row.addEventListener(UIEvents.CLICK, e -> {
                if (e.button != 0) return;
                select(node);
                if (!node.children.isEmpty()) setExpanded(node, !expanded.contains(node));
                e.preventDefault();
                e.stopPropagation();
            });
            return row;
        });
        addChild(rows);
        rows.setThumbTexture(theme.scrollThumb);
        addEventListener(UIEvents.KEY_DOWN, e -> {
            List<Node<T>> visible = rows.getItems();
            if (visible.isEmpty()) return;
            int index = visible.indexOf(selected);
            switch (e.keyCode) {
                case Keyboard.KEY_DOWN:
                    select(visible.get(Math.min(visible.size() - 1, index + 1)));
                    break;
                case Keyboard.KEY_UP:
                    select(visible.get(Math.max(0, index - 1)));
                    break;
                case Keyboard.KEY_HOME:
                    select(visible.get(0));
                    break;
                case Keyboard.KEY_END:
                    select(visible.get(visible.size() - 1));
                    break;
                case Keyboard.KEY_RIGHT:
                    if (selected != null) {
                        if (!expanded.contains(selected)) setExpanded(selected, true);
                        else if (!selected.children.isEmpty()) select(selected.children.get(0));
                    }
                    break;
                case Keyboard.KEY_LEFT:
                    if (selected != null) {
                        if (expanded.contains(selected)) setExpanded(selected, false);
                        else if (selected.parent != null) select(selected.parent);
                    }
                    break;
                default:
                    return;
            }
            rows.revealIndex(
                rows.getItems()
                    .indexOf(selected));
            e.preventDefault();
            e.stopPropagation();
        });
    }

    private int depth(Node<T> node) {
        int depth = 0;
        while (node != root && node.parent != null) {
            depth++;
            node = node.parent;
        }
        return depth;
    }

    public TreeList<T> setRoot(Node<T> root) {
        if (root != null && root.parent != null) throw new IllegalArgumentException("Root already owned");
        this.root = root;
        expanded.clear();
        selected = null;
        if (root != null) expanded.add(root);
        refresh();
        return this;
    }

    public Node<T> getSelected() {
        return selected;
    }

    public TreeList<T> setOnChange(Consumer<Node<T>> callback) {
        onChange = Objects.requireNonNull(callback);
        return this;
    }

    private void select(Node<T> node) {
        if (selected != node) {
            selected = node;
            onChange.accept(node);
        }
    }

    public void setExpanded(Node<T> node, boolean expand) {
        if (expand && !node.children.isEmpty()) expanded.add(node);
        else expanded.remove(node);
        refresh();
    }

    public boolean isExpanded(Node<T> node) {
        return expanded.contains(node);
    }

    private boolean attached(Node<T> node) {
        while (node != null) {
            if (node == root) return true;
            node = node.parent;
        }
        return false;
    }

    /** Rebuilds the flattened model after explicit tree mutations; detached selection and expansion are discarded. */
    public void refresh() {
        expanded.removeIf(node -> !attached(node));
        if (!attached(selected)) selected = null;
        List<Node<T>> visible = new ArrayList<>();
        if (root != null) {
            List<Node<T>> pending = new ArrayList<>();
            pending.add(root);
            while (!pending.isEmpty()) {
                Node<T> node = pending.remove(pending.size() - 1);
                visible.add(node);
                if (expanded.contains(node)) {
                    for (int i = node.children.size() - 1; i >= 0; i--) pending.add(node.children.get(i));
                }
            }
        }
        rows.setItems(visible);
    }

    @Override
    public void layout() {
        rows.setSize(width, height);
        super.layout();
    }
}
