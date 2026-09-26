package com.xyp.ldlib.gui.ui;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.xyp.ldlib.gui.ui.event.UIEvent;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/**
 * Owns focus and pointer capture for one screen. Dispatch follows LDLib2's
 * capture/target/bubble phases, using a stable path if listeners rebuild the tree.
 * Detached, disabled or hidden targets lose focus and capture before subsequent input.
 */
public final class UIInput {

    private final UIElement root;
    private UIElement focused, captured;
    private int capturedButton = -1;
    private UIElement scope;
    private final Map<UIElement, UIElement> savedFocus = new IdentityHashMap<>();

    public UIInput(UIElement root) {
        this.root = root;
        scope = root;
    }

    public UIElement getFocused() {
        validate();
        return focused;
    }

    private boolean valid(UIElement element) {
        return element != null && element.belongsTo(scope) && element.isInteractive();
    }

    public void validate() {
        UIElement next = root;
        for (UIElement child : root.getChildren()) {
            if (child.isModal() && child.isInteractive()) next = child;
        }
        if (next != scope) {
            if (scope.belongsTo(root)) savedFocus.put(scope, focused);
            scope = next;
            focus(savedFocus.remove(scope));
            if (focused == null && scope != root) cycleFocus(false);
        }
        savedFocus.keySet()
            .removeIf(element -> !element.belongsTo(root));
        if (focused != null && (!valid(focused) || !focused.isFocusable())) focus(null);
        if (!valid(captured)) {
            captured = null;
            capturedButton = -1;
        }
    }

    public void focus(UIElement element) {
        if (element != null && (!valid(element) || !element.isFocusable())) element = null;
        if (focused == element) return;
        UIElement old = focused;
        focused = element;
        if (old != null) dispatch(old, new UIEvent(UIEvents.BLUR));
        if (focused != null) {
            for (UIElement ancestor = focused.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
                ancestor.reveal(focused);
            }
            dispatch(focused, new UIEvent(UIEvents.FOCUS));
        }
    }

    public void clear() {
        focus(null);
        captured = null;
        capturedButton = -1;
        savedFocus.clear();
    }

    public boolean mouseDown(int x, int y, int button) {
        validate();
        UIElement hit = scope.hitTest(x, y);
        if (button == 0 || button == 1) {
            UIElement candidate = hit;
            while (candidate != null && !candidate.isFocusable()) candidate = candidate.getParent();
            focus(candidate);
        }
        captured = hit;
        capturedButton = button;
        if (hit == null) return false;
        dispatch(hit, mouse(UIEvents.MOUSE_DOWN, x, y, button));
        validate();
        return true;
    }

    public boolean mouseUp(int x, int y, int button) {
        validate();
        UIElement target = captured;
        if (target == null || capturedButton != button) return false;
        captured = null;
        capturedButton = -1;
        dispatch(target, mouse(UIEvents.MOUSE_UP, x, y, button));
        if (valid(target) && scope.hitTest(x, y) == target) {
            dispatch(target, mouse(UIEvents.CLICK, x, y, button));
        }
        validate();
        return true;
    }

    public void mouseMove(int x, int y) {
        validate();
        UIElement target = captured != null ? captured : scope.hitTest(x, y);
        if (target != null) dispatch(target, mouse(UIEvents.MOUSE_MOVE, x, y, capturedButton));
    }

    public boolean mouseWheel(int x, int y, int delta) {
        validate();
        UIElement target = scope.hitTest(x, y);
        if (target == null || delta == 0) return false;
        UIEvent event = mouse(UIEvents.MOUSE_WHEEL, x, y, -1);
        event.deltaY = delta;
        dispatch(target, event);
        return event.isDefaultPrevented();
    }

    /** Key codes are supplied by the host; Tab is passed separately to keep dispatch headless-testable. */
    public boolean keyTyped(char character, int keyCode, boolean shift, boolean control, boolean tab) {
        validate();
        if (tab) {
            cycleFocus(shift);
            return true;
        }
        if (focused == null && scope == root) return false;
        UIEvent event = new UIEvent(UIEvents.KEY_DOWN);
        event.codePoint = character;
        event.keyCode = keyCode;
        event.shift = shift;
        event.control = control;
        dispatch(focused == null ? scope : focused, event);
        validate();
        return event.isDefaultPrevented();
    }

    public void cycleFocus(boolean backwards) {
        List<UIElement> candidates = new ArrayList<>();
        collect(scope, candidates);
        if (candidates.isEmpty()) {
            focus(null);
            return;
        }
        int index = candidates.indexOf(focused);
        if (index < 0) index = backwards ? 0 : -1;
        focus(candidates.get(Math.floorMod(index + (backwards ? -1 : 1), candidates.size())));
    }

    private void collect(UIElement element, List<UIElement> result) {
        if (!element.isInteractive()) return;
        if (element.isFocusable()) result.add(element);
        for (UIElement child : element.getChildren()) collect(child, result);
    }

    private static UIEvent mouse(String type, int x, int y, int button) {
        UIEvent event = new UIEvent(type);
        event.x = x;
        event.y = y;
        event.button = button;
        return event;
    }

    /** Dispatches along a snapshot of ancestors; stopping propagation keeps remaining same-node listeners. */
    public void dispatch(UIElement target, UIEvent event) {
        List<UIElement> path = new ArrayList<>();
        for (UIElement node = target; node != null; node = node.getParent()) {
            path.add(node);
            if (node == scope) break;
        }
        event.target = target;
        for (int i = path.size() - 1; i > 0; i--) {
            notify(path.get(i), event, UIEvent.EventPhase.CAPTURE, true);
            if (event.isPropagationStopped()) return;
        }
        notify(target, event, UIEvent.EventPhase.AT_TARGET, true);
        if (!event.isImmediatePropagationStopped()) notify(target, event, UIEvent.EventPhase.AT_TARGET, false);
        if (event.isPropagationStopped()) return;
        for (int i = 1; i < path.size(); i++) {
            notify(path.get(i), event, UIEvent.EventPhase.BUBBLE, false);
            if (event.isPropagationStopped()) return;
        }
    }

    private void notify(UIElement node, UIEvent event, UIEvent.EventPhase phase, boolean capture) {
        event.currentElement = node;
        event.phase = phase;
        node.notifyListeners(event, capture);
    }
}
