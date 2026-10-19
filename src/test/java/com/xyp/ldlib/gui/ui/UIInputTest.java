package com.xyp.ldlib.gui.ui;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.xyp.ldlib.gui.ui.elements.Flow;
import com.xyp.ldlib.gui.ui.elements.ScrollerView;
import com.xyp.ldlib.gui.ui.event.UIEvent;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/** Behavioral regressions for event paths, focus and clipped pointer capture. */
public class UIInputTest {

    @Test
    public void textInputDeliversOneUnicodeCommitToTheFocusedElement() {
        UIElement root = new UIElement(0, 0, 100, 100);
        UIElement first = new UIElement(0, 0, 40, 20).setFocusable(true);
        UIElement second = new UIElement(0, 30, 40, 20).setFocusable(true);
        root.addChild(first);
        root.addChild(second);
        List<String> commits = new ArrayList<>();
        first.addEventListener(UIEvents.TEXT_INPUT, event -> {
            commits.add(event.text);
            event.preventDefault();
            event.stopPropagation();
        });
        second.addEventListener(UIEvents.TEXT_INPUT, event -> fail("Text reached the unfocused element"));
        root.addEventListener(UIEvents.TEXT_INPUT, event -> fail("Consumed text must not bubble"));
        UIInput input = new UIInput(root);
        assertFalse(input.textInput("岩浆"));
        input.focus(first);
        assertTrue(input.textInput("岩浆"));
        assertFalse(input.textInput(""));
        assertEquals(Arrays.asList("岩浆"), commits);
    }

    @Test
    public void textInputDropsStaleFocusAndPreservesKeyModifiers() {
        UIElement root = new UIElement(0, 0, 100, 100);
        UIElement field = new UIElement(0, 0, 40, 20).setFocusable(true);
        root.addChild(field);
        int[] commits = { 0 };
        field.addEventListener(UIEvents.TEXT_INPUT, event -> {
            commits[0]++;
            event.preventDefault();
        });
        field.addEventListener(UIEvents.KEY_DOWN, event -> {
            assertTrue(event.shift);
            assertTrue(event.control);
            assertEquals(203, event.keyCode);
            assertEquals('\0', event.codePoint);
            event.preventDefault();
        });
        UIInput input = new UIInput(root);
        input.focus(field);
        assertTrue(input.keyTyped('\0', 203, true, true, false));
        field.setEnabled(false);
        assertFalse(input.textInput("water"));
        field.setEnabled(true);
        input.focus(field);
        root.removeChild(field);
        assertFalse(input.textInput("water"));
        assertEquals(0, commits[0]);
    }

    @Test
    public void dispatchesCaptureTargetAndBubbleInOrder() {
        UIElement root = new UIElement(0, 0, 100, 100);
        UIElement child = new UIElement(0, 0, 40, 40);
        root.addChild(child);
        List<String> calls = new ArrayList<>();
        root.addEventListener(UIEvents.CLICK, e -> calls.add("capture"), true);
        child.addEventListener(UIEvents.CLICK, e -> calls.add("target-capture"), true);
        child.addEventListener(UIEvents.CLICK, e -> calls.add("target"));
        root.addEventListener(UIEvents.CLICK, e -> calls.add("bubble"));
        new UIInput(root).dispatch(child, new UIEvent(UIEvents.CLICK));
        assertEquals(Arrays.asList("capture", "target-capture", "target", "bubble"), calls);
    }

    @Test
    public void stoppingPropagationPreservesOtherListenersOnSameNode() {
        UIElement root = new UIElement(0, 0, 100, 100);
        UIElement child = new UIElement(0, 0, 40, 40);
        root.addChild(child);
        List<String> calls = new ArrayList<>();
        child.addEventListener(UIEvents.CLICK, e -> {
            calls.add("one");
            e.stopPropagation();
        });
        child.addEventListener(UIEvents.CLICK, e -> calls.add("two"));
        root.addEventListener(UIEvents.CLICK, e -> fail("Must not bubble"));
        new UIInput(root).dispatch(child, new UIEvent(UIEvents.CLICK));
        assertEquals(Arrays.asList("one", "two"), calls);
    }

    @Test
    public void rejectsClicksReleasedOutsideAndAfterDetachment() {
        UIElement root = new UIElement(0, 0, 100, 100);
        UIElement child = new UIElement(0, 0, 40, 40);
        root.addChild(child);
        int[] clicks = { 0 };
        child.addEventListener(UIEvents.CLICK, e -> clicks[0]++);
        UIInput input = new UIInput(root);
        input.mouseDown(10, 10, 0);
        input.mouseUp(90, 90, 0);
        assertEquals(0, clicks[0]);
        input.mouseDown(10, 10, 0);
        root.clearAllChildren();
        input.mouseUp(10, 10, 0);
        assertEquals(0, clicks[0]);
    }

    @Test
    public void disabledAncestryClearsFocusAndSkipsTabTargets() {
        UIElement root = new UIElement(0, 0, 100, 100);
        UIElement page = new UIElement(0, 0, 80, 80);
        UIElement first = new UIElement(0, 0, 20, 20).setFocusable(true);
        UIElement second = new UIElement(0, 30, 20, 20).setFocusable(true);
        page.addChild(first);
        root.addChild(page);
        root.addChild(second);
        UIInput input = new UIInput(root);
        input.cycleFocus(false);
        assertSame(first, input.getFocused());
        page.setEnabled(false);
        assertNull(input.getFocused());
        input.cycleFocus(true);
        assertSame(second, input.getFocused());
        second.setVisible(false);
        assertNull(input.getFocused());
    }

    @Test
    public void scrollerClampsAndRevealsFocusedDescendants() {
        UIElement root = new UIElement(10, 20, 120, 100);
        ScrollerView viewport = new ScrollerView(5, 5, 80, 40);
        UIElement child = new UIElement(0, 80, 60, 20).setFocusable(true);
        viewport.addChild(child);
        root.addChild(viewport);
        root.layout();
        assertEquals(60, viewport.getMaxScroll());
        assertNotSame(child, root.hitTest(16, 30));
        UIInput input = new UIInput(root);
        input.focus(child);
        assertEquals(60, viewport.getScroll());
        assertSame(child, root.hitTest(16, 50));
        assertNull(root.hitTest(16, 130));
        viewport.removeChild(child);
        root.layout();
        assertEquals(0, viewport.getScroll());
    }

    @Test
    public void nestedWheelBubblesWhenInnerViewportReachesEnd() {
        UIElement root = new UIElement(0, 0, 100, 100);
        ScrollerView outer = new ScrollerView(0, 0, 100, 100);
        ScrollerView inner = new ScrollerView(0, 0, 80, 40);
        inner.addChild(new UIElement(0, 0, 70, 100));
        outer.addChild(inner);
        outer.addChild(new UIElement(0, 120, 10, 10));
        root.addChild(outer);
        root.layout();
        inner.setScroll(999);
        new UIInput(root).mouseWheel(10, 10, -120);
        assertEquals(60, inner.getScroll());
        assertEquals(20, outer.getScroll());
    }

    @Test
    public void hiddenFlowChildrenDoNotLeaveGaps() {
        Flow row = new Flow(0, 0, 100, 20, false).setPadding(2).setGap(3);
        UIElement a = new UIElement(0, 0, 10, 10), b = new UIElement(0, 0, 10, 10);
        row.addChild(a);
        row.addChild(b);
        row.layout();
        assertEquals(15, b.getX());
        a.setVisible(false);
        row.layout();
        assertEquals(2, b.getX());
    }
}
