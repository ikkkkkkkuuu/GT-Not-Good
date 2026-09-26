package com.xyp.ldlib.gui.ui.elements;

import org.lwjgl.input.Keyboard;

import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/**
 * Root-mounted popup host. Its owner controls lifetime; hiding/removing the owner closes the popup.
 * UIInput confines focus traversal and event propagation to the topmost modal layer.
 * 
 * @see com.xyp.ldlib.gui.ui.UIInput
 */
public class ModalLayer extends UIElement {

    private final UIElement owner;
    private final boolean outsideCloses;

    protected ModalLayer(UIElement owner, boolean outsideCloses) {
        super(0, 0, 0, 0);
        this.owner = owner;
        this.outsideCloses = outsideCloses;
        addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == Keyboard.KEY_ESCAPE) {
                close();
                e.preventDefault();
                e.stopPropagation();
            }
        }, true);
        addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.target == this) {
                if (this.outsideCloses) close();
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }

    public void open() {
        if (getParent() != null) return;
        UIElement root = owner;
        while (root.getParent() != null) root = root.getParent();
        setSize(root.getWidth(), root.getHeight());
        root.addChild(this);
    }

    public void close() {
        if (getParent() != null) getParent().removeChild(this);
    }

    @Override
    public boolean isModal() {
        return true;
    }

    @Override
    public void layout() {
        UIElement root = getParent();
        if (root == null) return;
        if (!owner.belongsTo(root) || !owner.isInteractive()) {
            close();
            return;
        }
        setSize(root.getWidth(), root.getHeight());
        super.layout();
    }
}
