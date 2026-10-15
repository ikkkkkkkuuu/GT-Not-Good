package com.xyp.gtnotgood.client.radialmenu.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;

/**
 * Reordering entries.
 *
 * <p>
 * The two layouts move differently on purpose, because a move has to match what the player sees: a fixed wheel draws
 * its empty positions, a dynamic one does not.
 */
public class MenuNodeMoveTest {

    private static MenuNode leaf(String title) {
        return MenuNode.leaf(title, null, new ActionSpec("keybind"));
    }

    private static MenuNode fixedMenu() {
        MenuNode menu = MenuNode.category("root", null, SlotLayout.fixed(6));
        menu.setChildAt(0, leaf("a"));
        menu.setChildAt(2, leaf("b"));
        menu.setChildAt(4, leaf("c"));
        menu.ensureSlotCapacity();
        return menu;
    }

    private static List<String> titles(MenuNode menu) {
        List<String> result = new ArrayList<>();
        for (MenuNode child : menu.childrenOrEmpty()) {
            result.add(child == null ? "-" : child.title);
        }
        return result;
    }

    @Test
    public void onAFixedWheelAnEntryStepsIntoTheNeighbouringGap() {
        MenuNode menu = fixedMenu();

        assertTrue(menu.moveChild(2, -1));

        // b moved from position 2 into the empty position 1, rather than jumping past a.
        assertEquals("[a, b, -, -, c, -]", titles(menu).toString());
    }

    @Test
    public void onADynamicWheelAnEntrySwapsWithTheNextVisibleOne() {
        MenuNode menu = fixedMenu();
        menu.layout = SlotLayout.dynamic();

        assertTrue(menu.moveChild(2, -1));

        // Empty positions are not drawn, so stepping into one would have looked like nothing happened.
        assertEquals("[b, -, a, -, c, -]", titles(menu).toString());
    }

    @Test
    public void movingPastTheEndDoesNothing() {
        MenuNode menu = fixedMenu();

        assertFalse(menu.moveChild(0, -1));
        assertFalse(menu.moveChild(4, 1) && titles(menu).get(4).equals("c"));
    }

    @Test
    public void movingAnEmptyPositionIsRefused() {
        MenuNode menu = fixedMenu();
        assertFalse(menu.moveChild(1, 1));
    }

    @Test
    public void movingOutsideTheListIsRefused() {
        MenuNode menu = fixedMenu();
        assertFalse(menu.moveChild(-1, 1));
        assertFalse(menu.moveChild(99, -1));
        assertFalse(menu.moveChild(0, 0));
    }

    @Test
    public void aLeafHasNothingToReorder() {
        assertFalse(leaf("a").moveChild(0, 1));
    }

    @Test
    public void movingInACallersOwnListLeavesTheMenuAlone() {
        // How the editor works: it reorders a copy so that cancelling really cancels.
        MenuNode menu = fixedMenu();
        List<MenuNode> working = new ArrayList<>(menu.childrenOrEmpty());

        assertTrue(MenuNode.moveInList(working, 2, -1, true));

        assertEquals("b", working.get(1).title);
        assertEquals("the menu itself is untouched until save", "b", menu.childAt(2).title);
        assertNull(menu.childAt(1));
    }

    @Test
    public void aDynamicMoveAtTheEdgeIsRefused() {
        MenuNode menu = fixedMenu();
        menu.layout = SlotLayout.dynamic();

        // a is the first drawn entry, so there is no visible entry before it.
        assertFalse(menu.moveChild(0, -1));
    }
}
