package com.xyp.ldlib.integration.modularui;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Test;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.widgets.menu.AbstractMenuButton;
import com.cleanroommc.modularui.widgets.menu.Menu;

/** Regression coverage for orphan menus after a wildcard filter row is rebuilt or removed. */
public class SelectorLifecycleTest {

    private static SelectorWidget selector() {
        return new SelectorWidget("wildcard_property_0", new Rectangle(), Arrays.asList("dust", "ingot"));
    }

    /** Replays the same Menu -> source.checkClose call chain present in the reported crash. */
    @Test
    public void lateCloseEventDoesNotReadRemovedParent() throws Exception {
        SelectorWidget selector = selector();
        Method getter = AbstractMenuButton.class.getDeclaredMethod("getMenu");
        getter.setAccessible(true);
        Menu<?> oldMenu = (Menu<?>) getter.invoke(selector);
        selector.dispose();
        oldMenu.checkClose(false, false);
        oldMenu.checkClose(true, true);
        assertFalse(selector.isOpen());
    }

    @Test
    public void removedRowClosesAndInvalidatesCachedPopup() throws Exception {
        SelectorWidget selector = selector();
        RecordingPanel handler = new RecordingPanel();
        field("panelHandler").set(selector, handler);
        field("open").setBoolean(selector, true);
        selector.dispose();
        assertEquals(1, handler.closes);
        assertEquals(1, handler.deletes);
        assertFalse(selector.isOpen());
        selector.dispose();
        assertEquals(1, handler.closes);
    }

    @Test
    public void rebuiltRowCannotReuseOldPanelHandlerName() throws Exception {
        assertNotEquals(field("panelName").get(selector()), field("panelName").get(selector()));
    }

    @Test
    public void detachedRowCannotOpenPopup() {
        SelectorWidget selector = selector();
        selector.openMenu(false);
        assertFalse(selector.isOpen());
    }

    /** Injects only MUI's private panel bookkeeping to exercise teardown without an OpenGL client. */
    private static Field field(String name) throws Exception {
        Field field = AbstractMenuButton.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    /** Minimal handler recording the real selector's cleanup calls. */
    private static final class RecordingPanel implements IPanelHandler {

        private boolean open = true;
        private int closes, deletes;

        @Override
        public boolean isPanelOpen() {
            return open;
        }

        @Override
        public void openPanel() {
            open = true;
        }

        @Override
        public void closePanel() {
            closes++;
            open = false;
        }

        @Override
        public void closeSubPanels() {}

        @Override
        public void closePanelInternal() {
            open = false;
        }

        @Override
        public void deleteCachedPanel() {
            deletes++;
        }

        @Override
        public boolean isSubPanel() {
            return true;
        }
    }
}
