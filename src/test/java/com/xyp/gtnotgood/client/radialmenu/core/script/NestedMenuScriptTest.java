package com.xyp.gtnotgood.client.radialmenu.core.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;

/**
 * A script that builds a wheel with branches in it.
 *
 * <p>
 * An entry carrying an {@code items} list of its own is a submenu, and the choice that comes back is still one number -
 * which with a tree can no longer be a position in any one list. Both halves of that are exercised here: the shape the
 * host is handed, and whether the number it answers with lands on the entry the player actually pointed at.
 */
public class NestedMenuScriptTest {

    /** Three entries at the top, two of them inside the middle one. Numbered 1, 2, (3, 4), 5 in that order. */
    private static final String BRANCHED = "local pick, item = menu.open({\n" + "  { key = 'home', label = 'Home' },\n"
        + "  { label = 'Warps', icon = 'minecraft:compass', opening = 'inline', slots = 6, ring = '#101820',\n"
        + "    items = { 'spawn', 'mine' } },\n"
        + "  { key = 'tp', label = 'Tp' },\n"
        + "}, { title = 'Root' })\n"
        + "if pick then notify('picked ' .. pick) end\n";

    private static MenuNode child(MenuNode menu, int index) {
        List<MenuNode> children = menu.childrenOrEmpty();
        assertTrue("menu has only " + children.size() + " entries", index < children.size());
        return children.get(index);
    }

    @Test
    public void anEntryWithItsOwnListBecomesASubmenu() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run(BRANCHED);

        MenuNode root = host.firstMenu();
        assertEquals(3, root.childrenOrEmpty().size());

        MenuNode warps = child(root, 1);
        assertTrue(warps.isCategory());
        assertEquals("Warps", warps.title);
        // A shape rather than something to run: the wheel opens it, so there is nothing for an executor to be handed.
        assertNull(warps.action);
        assertEquals(2, warps.childrenOrEmpty().size());
        assertEquals("spawn", child(warps, 0).title);
        assertEquals("mine", child(warps, 1).title);
    }

    @Test
    public void aSubmenuEntryCarriesItsOwnLayoutAndColours() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run(BRANCHED);

        MenuNode warps = child(host.firstMenu(), 1);
        SlotLayout layout = warps.layoutOrDefault();
        assertEquals(SlotLayout.Opening.Inline, layout.opening);
        assertEquals(SlotLayout.Mode.Fixed, layout.mode);
        assertEquals(6, layout.slots);
        assertNotNull(warps.style);
        assertEquals("#101820", warps.style.ringColor);
        // "icon" on an entry is its own picture, not the colour its children's icons are drawn in - read as a colour it
        // would hand the parser a block name.
        assertNull(warps.style.iconColor);
        assertNotNull(warps.icon);
        assertEquals("minecraft:compass", warps.icon.id);
    }

    @Test
    public void theMenuAtTheTopIsStillAWheelOfItsOwn() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run(BRANCHED);

        assertEquals("Root", host.firstMenu().title);
        assertEquals(SlotLayout.Opening.Replace, host.firstMenu().layoutOrDefault().opening);
    }

    @Test
    public void everyEntryIsNumberedDepthFirstAndParentsFirst() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run(BRANCHED);

        MenuNode root = host.firstMenu();
        assertEquals("1", choiceOf(child(root, 0)));
        // 2 is the submenu itself, which has no resume to carry it: its entries follow it, and the entry after it
        // picks up where they left off.
        assertEquals("3", choiceOf(child(child(root, 1), 0)));
        assertEquals("4", choiceOf(child(child(root, 1), 1)));
        assertEquals("5", choiceOf(child(root, 2)));
    }

    @Test
    public void aNestedEntryIsWhatComesBack() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(4);

        host.run(BRANCHED);

        assertEquals(1, host.notices.size());
        assertEquals("picked mine", host.notices.get(0));
    }

    @Test
    public void anEntryAfterASubmenuIsNotShiftedByIt() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(5);

        host.run(BRANCHED);

        assertEquals("picked tp", host.notices.get(0));
    }

    @Test
    public void aNestedOnPickRunsLikeAnyOther() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(3);

        ScriptTask task = host.run("local pick = menu.open({\n" + "  { key = 'home', label = 'Home' },\n"
            + "  { label = 'Warps', items = {\n"
            + "      { key = 'spawn', label = 'Spawn', onPick = function(key) chat.send('/warp ' .. key) end },\n"
            + "  } },\n"
            + "})\n"
            + "notify(tostring(pick))\n");

        assertNull(task.error());
        assertEquals(1, host.sent.size());
        assertEquals("/warp spawn", host.sent.get(0));
        assertEquals("spawn", host.notices.get(0));
    }

    @Test
    public void anEmptyListIsAnEntryRatherThanASubmenu() {
        // Nothing to unfold: a submenu with no entries is a sector that opens onto nothing, and the entry's own onPick
        // is the only thing it could have meant.
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run("local pick = menu.open({ { key = 'solo', label = 'Solo', items = {} } })\n" + "notify(pick)\n");

        MenuNode entry = child(host.firstMenu(), 0);
        assertTrue(!entry.isCategory());
        assertEquals("1", choiceOf(entry));
        assertEquals("solo", host.notices.get(0));
    }

    @Test
    public void aBranchDeeperThanOneStillNumbersInOrder() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(4);

        host.run("local pick = menu.open({\n" + "  { label = 'A', items = {\n"
            + "      { label = 'B', items = {\n"
            + "          { key = 'c', label = 'C' },\n"
            + "      } },\n"
            + "      { key = 'd', label = 'D' },\n"
            + "  } },\n"
            + "})\n"
            + "notify(pick)\n");

        MenuNode a = child(host.firstMenu(), 0);
        MenuNode b = child(a, 0);
        assertTrue(b.isCategory());
        assertEquals("3", choiceOf(child(b, 0)));
        assertEquals("4", choiceOf(child(a, 1)));
        assertEquals("d", host.notices.get(0));
    }

    @Test
    public void keepOpenRidesOnANestedEntryToo() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(2);

        host.run("menu.open({ { label = 'A', items = { 'b' } } }, { keepOpen = true })\n" + "menu.close()\n");

        MenuNode nested = child(child(host.firstMenu(), 0), 0);
        assertTrue(nested.keepOpen);
    }

    @Test
    public void anOverLongNestedListIsCutAndSaidSo() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run("local many = {}\n" + "for i = 1, 30 do many[i] = 'entry' .. i end\n"
            + "menu.open({ { label = 'Many', items = many } })\n");

        assertEquals(SlotLayout.MAX_SLOTS, child(host.firstMenu(), 0).childrenOrEmpty().size());
        assertEquals(1, host.logs.size());
        assertTrue(host.logs.get(0), host.logs.get(0).contains("30"));
    }

    @Test
    public void whatIsCutDoesNotMoveTheNumbersOfWhatIsNot() {
        // The cut falls inside the first branch, so a positional numbering would have everything after it naming the
        // wrong entry. The number each entry carries is the one the prelude gave it, which the cut cannot touch.
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(32);

        host.run("local many = {}\n" + "for i = 1, 30 do many[i] = 'entry' .. i end\n"
            + "local pick = menu.open({ { label = 'Many', items = many }, { key = 'last', label = 'Last' } })\n"
            + "notify(tostring(pick))\n");

        // One for the branch, thirty inside it, and the entry after them is the thirty-second.
        assertEquals("32", choiceOf(child(host.firstMenu(), 1)));
        assertEquals("last", host.notices.get(0));
    }

    private static String choiceOf(MenuNode leaf) {
        assertNotNull("entry has no action", leaf.action);
        assertEquals(ActionTypes.SCRIPT_RESUME, leaf.action.type);
        return leaf.action.getString(ActionTypes.PARAM_CHOICE, null);
    }
}
