package com.xyp.gtnotgood.client.radialmenu.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;

public class MenuNodeTest {

    private static MenuNode fixedCategory(int slots) {
        return MenuNode.category("root", null, SlotLayout.fixed(slots));
    }

    private static MenuNode leaf(String title) {
        return MenuNode.leaf(title, null, new ActionSpec("keybind"));
    }

    @Test
    public void everyEntryStaysVisibleAfterPadding() {
        // Regression: padding a fixed wheel to its slot count and then appending entries pushed them all past the
        // last visible sector, so a freshly generated profile rendered as an empty ring.
        MenuNode root = fixedCategory(8);
        root.ensureSlotCapacity();

        root.setChildAt(0, leaf("first"));
        root.setChildAt(2, leaf("second"));

        assertEquals(8, root.slotCount());
        assertNotNull(root.childAt(0));
        assertNotNull(root.childAt(2));
        assertNull(root.childAt(1));
    }

    @Test
    public void paddingFillsUpToTheSlotCount() {
        MenuNode root = fixedCategory(6);
        root.ensureSlotCapacity();
        assertEquals(6, root.children.size());
        for (int slot = 0; slot < 6; slot++) {
            assertNull(root.childAt(slot));
        }
    }

    @Test
    public void overflowWidensTheWheelInsteadOfHidingEntries() {
        MenuNode root = fixedCategory(2);
        root.setChildAt(0, leaf("a"));
        root.setChildAt(1, leaf("b"));
        root.setChildAt(2, leaf("c"));

        root.ensureSlotCapacity();

        assertEquals(3, root.slotCount());
        assertNotNull(root.childAt(2));
    }

    @Test
    public void dynamicWheelsFollowTheirChildCount() {
        MenuNode root = MenuNode.category("root", null, SlotLayout.dynamic());
        root.setChildAt(0, leaf("a"));
        root.setChildAt(1, leaf("b"));

        root.ensureSlotCapacity();

        assertEquals(2, root.slotCount());
        assertEquals(2, root.children.size());
    }

    @Test
    public void normalizeTreatsAConflictedNodeAsACategory() {
        MenuNode node = fixedCategory(4);
        node.action = new ActionSpec("keybind");

        node.normalize();

        assertTrue(node.isCategory());
        assertNull("children are worth more than a single action", node.action);
    }

    @Test
    public void normalizeStripsLayoutFromLeaves() {
        MenuNode node = leaf("a");
        node.layout = SlotLayout.fixed(8);

        node.normalize();

        assertNull(node.layout);
    }

    @Test
    public void copyIsDeepEnoughToEditSafely() {
        MenuNode root = fixedCategory(4);
        root.setChildAt(0, leaf("child"));

        MenuNode copy = root.copy();
        copy.childAt(0).title = "renamed";
        copy.childAt(0).action.set("binding", "key.jump");

        assertEquals("child", root.childAt(0).title);
        assertNull(root.childAt(0).action.getString("binding", null));
    }

    @Test
    public void countingDeeplyLooksPastTheFirstWheel() {
        MenuNode root = MenuNode.category("root", null, SlotLayout.dynamic());
        MenuNode sub = MenuNode.category("sub", null, SlotLayout.dynamic());
        sub.children.add(MenuNode.leaf("a", null, null));
        sub.children.add(MenuNode.leaf("b", null, null));
        root.children.add(sub);
        root.children.add(MenuNode.leaf("c", null, null));
        root.children.add(null);

        // One wheel has two things on it; the profile has four entries in it.
        assertEquals(2, root.filledCount());
        assertEquals(4, root.deepCount());

        // Emptying the submenu is invisible to the wheel above it, and that is the difference worth having.
        sub.children.clear();
        assertEquals(2, root.filledCount());
        assertEquals(2, root.deepCount());
    }
}
