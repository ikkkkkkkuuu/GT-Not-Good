package com.xyp.ldlib.gui.ui;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.xyp.ldlib.gui.ui.elements.TextField;

/** Right-click input routing must clear only the targeted enabled field and notify once. */
public class TextFieldClearTest {

    @Test
    public void clearsAndFocusesOnlyTheTargetOnce() {
        UIElement root = new UIElement(0, 0, 200, 60);
        List<String> edits = new ArrayList<>();
        TextField first = new TextField(0, 0, 80, 20).setText("123")
            .setOnChange(edits::add);
        TextField second = new TextField(90, 0, 80, 20).setText("keep");
        root.addChild(first);
        root.addChild(second);
        UIInput input = new UIInput(root);
        input.mouseDown(5, 5, 1);
        input.mouseUp(5, 5, 1);
        assertEquals("", first.getText());
        assertEquals("keep", second.getText());
        assertSame(first, input.getFocused());
        assertEquals(1, edits.size());
        input.mouseDown(5, 5, 1);
        input.mouseUp(5, 5, 1);
        assertEquals(1, edits.size());
    }

    @Test
    public void disabledFieldAndOutsideClickDoNotClear() {
        UIElement root = new UIElement(0, 0, 200, 60);
        TextField field = new TextField(0, 0, 80, 20).setText("123");
        root.addChild(field);
        UIInput input = new UIInput(root);
        input.mouseDown(150, 30, 1);
        assertEquals("123", field.getText());
        field.setEnabled(false);
        input.mouseDown(5, 5, 1);
        assertEquals("123", field.getText());
    }
}
