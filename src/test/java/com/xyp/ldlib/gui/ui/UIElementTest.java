package com.xyp.ldlib.gui.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** Regression coverage for nested coordinates, overlap ordering and page replacement. */
public class UIElementTest {

    @Test
    public void topmostChildConsumesClickWithNestedScreenOffsets() {
        List<String> events = new ArrayList<>();
        UIElement root = new UIElement(5, 7, 100, 100);
        UIElement group = new UIElement(10, 12, 60, 60);
        group.addChild(new Probe("bottom", events, null));
        group.addChild(new Probe("top", events, null));
        root.addChild(group);
        root.draw(0, 0, 100, 200);
        assertEquals(Arrays.asList("bottom@118,223", "top@118,223"), events);
        events.clear();
        assertTrue(root.mouseClicked(118, 223, 0, 100, 200));
        assertEquals(Arrays.asList("top"), events);
        assertFalse(root.mouseClicked(138, 223, 0, 100, 200));
    }

    @Test
    public void clickCanReplaceChildrenWithoutDispatchingToRemovedSiblings() {
        List<String> events = new ArrayList<>();
        UIElement root = new UIElement(0, 0, 80, 80);
        root.addChild(new Probe("old", events, null));
        root.addChild(new Probe("replace", events, root::clearAllChildren));
        assertTrue(root.mouseClicked(3, 4, 0, 0, 0));
        assertEquals(Arrays.asList("replace"), events);
        assertFalse(root.mouseClicked(3, 4, 0, 0, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAncestorAsChild() {
        UIElement root = new UIElement(0, 0, 40, 40);
        UIElement child = new UIElement(0, 0, 40, 40);
        root.addChild(child);
        child.addChild(root);
    }

    @Test
    public void detachedChildrenCanBeReused() {
        UIElement root = new UIElement(0, 0, 40, 40);
        UIElement child = new UIElement(0, 0, 20, 20);
        root.addChild(child);
        root.clearAllChildren();
        new UIElement(0, 0, 30, 30).addChild(child);
    }

    /** Records rendering and consumes clicks without loading Minecraft or OpenGL. */
    private static final class Probe extends UIElement {

        private final String name;
        private final List<String> events;
        private final Runnable action;

        private Probe(String name, List<String> events, Runnable action) {
            super(3, 4, 20, 20);
            this.name = name;
            this.events = events;
            this.action = action;
            setBackground((mx, my, x, y, w, h) -> events.add(name + "@" + x + "," + y));
        }

        @Override
        public boolean mouseClicked(int mouseX, int mouseY, int button, int parentX, int parentY) {
            if (!contains(mouseX, mouseY, parentX, parentY)) return false;
            events.add(name);
            if (action != null) action.run();
            return true;
        }
    }
}
