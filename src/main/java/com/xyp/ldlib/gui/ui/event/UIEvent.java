package com.xyp.ldlib.gui.ui.event;

import com.xyp.ldlib.gui.ui.UIElement;

/** Reduced LDLib2 event payload and propagation controls, for local UI dispatch only. */
public final class UIEvent {

    /** Event path phase, in the same order as upstream LDLib2. */
    public enum EventPhase {
        CAPTURE,
        AT_TARGET,
        BUBBLE
    }

    public final String type;
    public final long timeStamp = System.currentTimeMillis();
    public int x, y, deltaY, button, keyCode;
    public char codePoint;
    public boolean shift, control;
    public UIElement target, currentElement;
    public EventPhase phase;
    private boolean propagationStopped;
    private boolean immediatePropagationStopped;
    private boolean defaultPrevented;

    public UIEvent(String type) {
        this.type = type;
    }

    public void stopPropagation() {
        propagationStopped = true;
    }

    public void stopImmediatePropagation() {
        immediatePropagationStopped = true;
        propagationStopped = true;
    }

    public void preventDefault() {
        defaultPrevented = true;
    }

    public boolean isPropagationStopped() {
        return propagationStopped;
    }

    public boolean isImmediatePropagationStopped() {
        return immediatePropagationStopped;
    }

    public boolean isDefaultPrevented() {
        return defaultPrevented;
    }
}
