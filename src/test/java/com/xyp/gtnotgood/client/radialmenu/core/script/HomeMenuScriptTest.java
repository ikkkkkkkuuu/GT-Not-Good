package com.xyp.gtnotgood.client.radialmenu.core.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;

/**
 * The scenario the script action was designed for, end to end: ask the server, read its answer, offer what it said,
 * send
 * a command built from what the player chose.
 */
public class HomeMenuScriptTest {

    private static final String HOME_SCRIPT = "local me = player.name\n" + "chat.send('/home list ' .. me)\n"
        + "local list = chat.await('^' .. me .. ': %d+ / %d+: (.+)$', 60)\n"
        + "if not list then notify('no answer from /home list') return end\n"
        + "local homes = {}\n"
        + "for name in list:gmatch('[^,%s]+') do homes[#homes + 1] = name end\n"
        + "local pick = menu.open(homes, { title = 'Homes', slots = 8 })\n"
        + "if pick then chat.send('/home ' .. pick .. ' ' .. me) end\n";

    private static FakeScriptHost hostWithHomeList() {
        FakeScriptHost host = new FakeScriptHost();
        host.incoming.add("Nortcast: 5 / 10: dungeon, gradka, home, mine, sand");
        return host;
    }

    @Test
    public void parsesTheReplyAndSendsTheChosenHome() {
        FakeScriptHost host = hostWithHomeList();
        host.choices.add(2);

        ScriptTask task = host.run(HOME_SCRIPT);

        assertNull(task.error());
        assertTrue(task.isFinished());
        assertEquals(2, host.sent.size());
        assertEquals("/home list Nortcast", host.sent.get(0));
        assertEquals("/home gradka Nortcast", host.sent.get(1));
    }

    @Test
    public void buildsOneEntryPerHome() {
        FakeScriptHost host = hostWithHomeList();
        host.choices.add(1);

        host.run(HOME_SCRIPT);

        MenuNode menu = host.firstMenu();
        assertEquals("Homes", menu.title);
        assertEquals(5, menu.childrenOrEmpty().size());
        assertEquals("dungeon", menu.childrenOrEmpty().get(0).title);
        assertEquals("sand", menu.childrenOrEmpty().get(4).title);
    }

    @Test
    public void everyEntryCarriesTheRunAndItsOwnPosition() {
        FakeScriptHost host = hostWithHomeList();
        host.choices.add(3);

        host.run(HOME_SCRIPT);

        ActionSpec third = host.firstMenu().childrenOrEmpty().get(2).action;
        assertEquals(ActionTypes.SCRIPT_RESUME, third.type);
        assertEquals("token-1", third.getString(ActionTypes.PARAM_TOKEN, null));
        assertEquals("3", third.getString(ActionTypes.PARAM_CHOICE, null));
    }

    @Test
    public void slotsIsAMinimumRatherThanACap() {
        // Eight sectors for five homes keeps the ring's shape, which is what a fixed wheel is for.
        FakeScriptHost host = hostWithHomeList();
        host.choices.add(1);

        host.run(HOME_SCRIPT);

        SlotLayout layout = host.firstMenu().layoutOrDefault();
        assertEquals(SlotLayout.Mode.Fixed, layout.mode);
        assertEquals(8, layout.slots);
    }

    @Test
    public void aWheelStretchesRatherThanLosingEntriesItWasGiven() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run("menu.open({ 'a', 'b', 'c', 'd', 'e', 'f' }, { slots = 4 })");

        SlotLayout layout = host.firstMenu().layoutOrDefault();
        assertEquals(SlotLayout.Mode.Fixed, layout.mode);
        assertEquals(6, layout.slots);
    }

    @Test
    public void noAnswerLeavesTheMenuUnopenedAndSaysSo() {
        FakeScriptHost host = new FakeScriptHost();

        ScriptTask task = host.run(HOME_SCRIPT);

        assertNull(task.error());
        assertEquals(1, host.sent.size());
        assertEquals(1, host.notices.size());
        assertEquals("no answer from /home list", host.notices.get(0));
        assertTrue(host.menus.isEmpty());
    }

    @Test
    public void aDismissedMenuSendsNothing() {
        FakeScriptHost host = hostWithHomeList();
        // No choice queued: the player let go without picking anything.

        host.run(HOME_SCRIPT);

        assertEquals(1, host.sent.size());
        assertEquals(1, host.menus.size());
    }

    @Test
    public void anEmptyListOpensNoWheel() {
        FakeScriptHost host = new FakeScriptHost();

        host.run("local pick = menu.open({})\n" + "notify(tostring(pick))\n");

        assertTrue(host.menus.isEmpty());
        assertEquals("nil", host.notices.get(0));
    }

    @Test
    public void readsTheKeyAndTheEntryBack() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(2);

        host.run("local key, item = menu.open({\n" + "  { key = 'a', label = 'First' },\n"
            + "  { key = 'b', label = 'Second', command = '/warp b' },\n"
            + "})\n"
            + "notify(key .. ' ' .. item.label .. ' ' .. item.command)\n");

        assertEquals("b Second /warp b", host.notices.get(0));
    }

    @Test
    public void aKeylessEntryFallsBackToItsLabelAndThenToItsPosition() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);
        host.run("notify(tostring(menu.open({ { label = 'Mine' } })))");
        assertEquals("Mine", host.notices.get(0));

        FakeScriptHost unlabelled = new FakeScriptHost();
        unlabelled.choices.add(2);
        unlabelled.run("notify(tostring(menu.open({ { icon = 'minecraft:stone' }, { icon = 'minecraft:dirt' } })))");
        assertEquals("2", unlabelled.notices.get(0));
    }

    @Test
    public void onPickRunsBeforeTheKeyComesBack() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run("local key = menu.open({\n"
            + "  { key = 'home', label = 'Home', onPick = function(k, item) chat.send('/home ' .. k) end },\n"
            + "})\n"
            + "chat.send('after ' .. key)\n");

        assertEquals("/home home", host.sent.get(0));
        assertEquals("after home", host.sent.get(1));
    }

    @Test
    public void aCallbackMayItselfWaitAndOpenAnotherMenu() {
        // The reason onPick lives in the prelude rather than on the host: it runs inside the script, so waiting in it
        // is
        // ordinary code.
        FakeScriptHost host = new FakeScriptHost();
        host.incoming.add("Warps: spawn, shop, mine");
        host.choices.add(1); // the outer menu's "warps" entry
        host.choices.add(2); // "shop" in the menu the callback opens

        host.run("menu.open({\n" + "  { key = 'warps', label = 'Warps', onPick = function()\n"
            + "      chat.send('/warp list')\n"
            + "      local line = chat.await('^Warps: (.+)$')\n"
            + "      local warps = {}\n"
            + "      for w in line:gmatch('[^,%s]+') do warps[#warps + 1] = w end\n"
            + "      local pick = menu.open(warps, { title = 'Warps' })\n"
            + "      if pick then chat.send('/warp ' .. pick) end\n"
            + "  end },\n"
            + "})\n");

        assertEquals(2, host.menus.size());
        assertEquals("Warps", host.menus.get(1).title);
        assertEquals("/warp list", host.sent.get(0));
        assertEquals("/warp shop", host.sent.get(1));
    }

    @Test
    public void anOnPickThatIsNotAFunctionIsReportedRatherThanCalled() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        ScriptTask task = host.run("notify(tostring(menu.open({ { key = 'a', onPick = 'oops' } })))");

        assertNull(task.error());
        assertEquals("a", host.notices.get(0));
        assertEquals(1, host.logs.size());
        assertTrue(host.logs.get(0), host.logs.get(0).contains("not a function"));
    }

    @Test
    public void duplicateKeysAreReported() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run("menu.open({ 'home', 'home' })");

        assertEquals(1, host.logs.size());
        assertTrue(host.logs.get(0), host.logs.get(0).contains("home"));
    }

    @Test
    public void moreEntriesThanTheWheelCanDrawAreCut() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run("local items = {}\n" + "for i = 1, 40 do items[i] = 'entry' .. i end\n" + "menu.open(items)\n");

        assertEquals(SlotLayout.MAX_SLOTS, host.firstMenu().childrenOrEmpty().size());
        // And said so: a script whose parsing found forty homes should not have to wonder where sixteen of them went.
        assertEquals(1, host.logs.size());
        assertTrue(host.logs.get(0), host.logs.get(0).contains("40"));
    }

    @Test
    public void readsEveryIconForm() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run("menu.open({\n" + "  { key = 'a', icon = 'minecraft:stone' },\n"
            + "  { key = 'b', icon = 'minecraft:stained_hardened_clay:5' },\n"
            + "  { key = 'c', icon = 'sprite:phosphor:house', color = '#7FD4FF' },\n"
            + "  { key = 'd', icon = 'file:sand.png' },\n"
            + "  { key = 'e', icon = 'effect:potion.moveSpeed' },\n"
            + "  { key = 'f', icon = { kind = 'item', id = 'minecraft:wool', meta = 14 } },\n"
            + "  { key = 'g', icon = 'player:Nortcast' },\n"
            + "  { key = 'h', icon = { kind = 'player', id = 'Nortcast' } },\n"
            + "  { key = 'i' },\n"
            + "})\n");

        MenuNode menu = host.firstMenu();
        IconSpec stone = menu.childrenOrEmpty().get(0).icon;
        assertEquals(IconSpec.Kind.Item, stone.kind);
        assertEquals("minecraft:stone", stone.id);
        assertEquals(0, stone.meta);

        IconSpec clay = menu.childrenOrEmpty().get(1).icon;
        assertEquals("minecraft:stained_hardened_clay", clay.id);
        assertEquals(5, clay.meta);

        IconSpec house = menu.childrenOrEmpty().get(2).icon;
        assertEquals(IconSpec.Kind.Sprite, house.kind);
        assertEquals("phosphor:house", house.id);
        assertEquals("#7FD4FF", house.color);

        assertEquals(IconSpec.Kind.File, menu.childrenOrEmpty().get(3).icon.kind);
        assertEquals(IconSpec.Kind.Effect, menu.childrenOrEmpty().get(4).icon.kind);

        IconSpec wool = menu.childrenOrEmpty().get(5).icon;
        assertEquals("minecraft:wool", wool.id);
        assertEquals(14, wool.meta);

        // A face is spelled either way round, because a script building one out of a name it read has no business
        // formatting a prefix by hand.
        IconSpec spelled = menu.childrenOrEmpty().get(6).icon;
        assertEquals(IconSpec.Kind.Player, spelled.kind);
        assertEquals("Nortcast", spelled.id);

        IconSpec tabled = menu.childrenOrEmpty().get(7).icon;
        assertEquals(IconSpec.Kind.Player, tabled.kind);
        assertEquals("Nortcast", tabled.id);

        assertNull(menu.childrenOrEmpty().get(8).icon);
    }

    @Test
    public void anAccentIsWrittenOutIntoTheColoursItDerives() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run("menu.open({ 'a' }, { accent = '#4A90D9', highlight = '0xFF123456' })");

        MenuNode menu = host.firstMenu();
        assertNotNull(menu.style);
        assertNotNull(menu.style.ringColor);
        assertNotNull(menu.style.borderColor);
        // An explicitly named colour wins over the one the accent would have derived.
        assertEquals("0xFF123456", menu.style.highlightColor);
        // The icon tint is never derived from an accent, so it stays unset and inherits.
        assertNull(menu.style.iconColor);
    }

    @Test
    public void aMenuThatNamesNoColoursInheritsRatherThanInventingThem() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run("menu.open({ 'a' })");

        assertNull(host.firstMenu().style);
    }

    @Test
    public void keepOpenRidesOnEveryEntryOfTheWheel() {
        FakeScriptHost host = new FakeScriptHost();
        host.choices.add(1);

        host.run("menu.open({ 'a', 'b' }, { keepOpen = true })\n" + "menu.close()\n");

        for (MenuNode child : host.firstMenu().childrenOrEmpty()) {
            assertTrue(child.keepOpen);
        }
        assertTrue(host.closedMenu);
    }

    @Test
    public void readsThePlayerThroughTheHostRatherThanACopy() {
        FakeScriptHost host = new FakeScriptHost();

        host.run("notify(player.name .. ' ' .. player.dim .. ' ' .. player.x .. ',' .. player.y .. ',' .. player.z)");

        assertEquals("Nortcast 0 100,64,-200", host.notices.get(0));
    }

    @Test
    public void reachesTheModsOwnActions() {
        FakeScriptHost host = new FakeScriptHost();

        host.run(
            "action.run{ type = 'keybind', params = { binding = 'key.inventory', mode = 'tap', holdTicks = 20 } }");

        assertEquals(1, host.actions.size());
        ActionSpec spec = host.actions.get(0);
        assertEquals(ActionTypes.KEYBIND, spec.type);
        assertEquals("key.inventory", spec.getString(ActionTypes.PARAM_BINDING, null));
        // Numbers are converted rather than refused: the file format stores strings, a script writes 20.
        assertEquals("20", spec.getString(ActionTypes.PARAM_HOLD_TICKS, null));
    }

    @Test
    public void collectsSeveralLinesWhenAskedTo() {
        FakeScriptHost host = new FakeScriptHost();
        host.incoming.add("- spawn (3)");
        host.incoming.add("something else entirely");
        host.incoming.add("- shop (7)");

        host.run("local rows = chat.awaitAll('^%- (%S+) %((%d+)%)$', 10)\n" + "local out = {}\n"
            + "for i = 1, #rows do out[i] = rows[i][1] .. '=' .. rows[i][2] end\n"
            + "notify(table.concat(out, ' '))\n");

        assertEquals("spawn=3 shop=7", host.notices.get(0));
    }
}
