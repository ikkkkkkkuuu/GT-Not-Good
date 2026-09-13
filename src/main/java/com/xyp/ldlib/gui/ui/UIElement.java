package com.xyp.ldlib.gui.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.ui.event.UIEvent;

/**
 * LDLib2 element-tree adaptation for 1.7.10. Bounds are parent-relative; paint and hit-test
 * order agree. Child overflow is permitted for scroll content, while hit testing clips to ancestors.
 * Fixed bounds and simple layouts replace upstream's native layout engine.
 */
public class UIElement {

    protected int x, y, width, height;
    private IGuiTexture background;
    private UIElement parent;
    private boolean visible = true, enabled = true, focusable;
    private final List<UIElement> children = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private UIElement[] childSnapshot = new UIElement[0];

    public UIElement(int x, int y, int width, int height) {
        setPosition(x, y);
        setSize(width, height);
    }

    public UIElement setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public UIElement setSize(int width, int height) {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Negative element size");
        this.width = width;
        this.height = height;
        return this;
    }

    public UIElement setBackground(IGuiTexture texture) {
        background = texture;
        return this;
    }

    public UIElement setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public UIElement setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public UIElement setFocusable(boolean focusable) {
        this.focusable = focusable;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFocusable() {
        return focusable;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public UIElement getParent() {
        return parent;
    }

    public List<UIElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public int getScreenX() {
        return x + (parent == null ? 0 : parent.getScreenX() + parent.childOffsetX());
    }

    public int getScreenY() {
        return y + (parent == null ? 0 : parent.getScreenY() + parent.childOffsetY());
    }

    protected int childOffsetX() {
        return 0;
    }

    protected int childOffsetY() {
        return 0;
    }

    /** Scroll containers override this to reveal a descendant that gains keyboard focus. */
    public void reveal(UIElement descendant) {}

    /** Checks the whole ancestry so hidden or disabled pages cannot retain interactive descendants. */
    public boolean isInteractive() {
        for (UIElement node = this; node != null; node = node.parent) {
            if (!node.visible || !node.enabled) return false;
        }
        return true;
    }

    public boolean belongsTo(UIElement root) {
        for (UIElement node = this; node != null; node = node.parent) if (node == root) return true;
        return false;
    }

    /** Adds an exclusively owned child and rejects cycles before changing the tree. */
    public UIElement addChild(UIElement child) {
        Objects.requireNonNull(child, "child");
        if (child.parent != null) throw new IllegalArgumentException("Element already has a parent");
        if (belongsTo(child)) throw new IllegalArgumentException("Cyclic element tree");
        child.parent = this;
        children.add(child);
        refreshChildren();
        return this;
    }

    public boolean removeChild(UIElement child) {
        if (!children.remove(child)) return false;
        child.parent = null;
        refreshChildren();
        return true;
    }

    public void clearAllChildren() {
        for (UIElement child : children) child.parent = null;
        children.clear();
        refreshChildren();
    }

    private void refreshChildren() {
        childSnapshot = children.toArray(new UIElement[0]);
    }

    /** Registration returns an idempotent unsubscribe action suitable for screen teardown. */
    public Runnable addEventListener(String type, Consumer<UIEvent> callback, boolean capture) {
        Listener listener = new Listener(type, Objects.requireNonNull(callback), capture);
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public Runnable addEventListener(String type, Consumer<UIEvent> callback) {
        return addEventListener(type, callback, false);
    }

    public void notifyListeners(UIEvent event, boolean capture) {
        for (Listener listener : new ArrayList<>(listeners)) {
            if (listener.capture == capture && listener.type.equals(event.type)) listener.callback.accept(event);
            if (event.isImmediatePropagationStopped()) break;
        }
    }

    /** Layout runs before screen input and paint; containers override to arrange their children. */
    public void layout() {
        for (UIElement child : childSnapshot) child.layout();
    }

    public void tick() {
        for (UIElement child : childSnapshot) if (child.visible) child.tick();
    }

    public boolean contains(int mouseX, int mouseY, int parentX, int parentY) {
        return mouseX >= parentX + x && mouseY >= parentY + y
            && mouseX < parentX + x + width
            && mouseY < parentY + y + height;
    }

    public UIElement hitTest(int mouseX, int mouseY) {
        if (!visible || !enabled || !contains(mouseX, mouseY, getScreenX() - x, getScreenY() - y)) return null;
        UIElement[] snapshot = childSnapshot;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            UIElement hit = snapshot[i].hitTest(mouseX, mouseY);
            if (hit != null) return hit;
        }
        return this;
    }

    public void draw(int mouseX, int mouseY, int parentX, int parentY) {
        if (!visible) return;
        drawBackground(mouseX, mouseY, parentX, parentY);
        drawChildren(mouseX, mouseY, parentX, parentY);
        drawForeground(mouseX, mouseY, parentX, parentY);
    }

    protected void drawChildren(int mouseX, int mouseY, int parentX, int parentY) {
        for (UIElement child : childSnapshot) {
            child.draw(mouseX, mouseY, parentX + x + childOffsetX(), parentY + y + childOffsetY());
        }
    }

    protected void drawBackground(int mouseX, int mouseY, int parentX, int parentY) {
        if (background != null) background.draw(mouseX, mouseY, parentX + x, parentY + y, width, height);
    }

    protected void drawForeground(int mouseX, int mouseY, int parentX, int parentY) {}

    /** Legacy direct-click bridge retained for the first wildcard prototype; new screens use UIInput. */
    public boolean mouseClicked(int mouseX, int mouseY, int button, int parentX, int parentY) {
        if (!isInteractive() || !contains(mouseX, mouseY, parentX, parentY)) return false;
        UIElement[] snapshot = childSnapshot;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            if (snapshot[i].parent == this && snapshot[i]
                .mouseClicked(mouseX, mouseY, button, parentX + x + childOffsetX(), parentY + y + childOffsetY()))
                return true;
        }
        return false;
    }

    /** Listener identity is preserved even when the same callback is registered more than once. */
    private static final class Listener {

        final String type;
        final Consumer<UIEvent> callback;
        final boolean capture;

        Listener(String type, Consumer<UIEvent> callback, boolean capture) {
            this.type = type;
            this.callback = callback;
            this.capture = capture;
        }
    }
}
