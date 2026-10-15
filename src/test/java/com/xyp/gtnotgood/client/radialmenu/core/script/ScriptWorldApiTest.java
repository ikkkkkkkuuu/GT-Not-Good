package com.xyp.gtnotgood.client.radialmenu.core.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * The five things a script learned to ask about: what it remembers, what it is looking at, what it is carrying, what
 * the player types, and how the world and body are doing.
 *
 * <p>
 * Driven through real scripts rather than through the Java behind them, because every one of these arrives by a
 * string key - a field spelled one way in the API and another in the documentation reads as nil and nothing says so.
 */
public class ScriptWorldApiTest {

    private final FakeScriptHost host = new FakeScriptHost();

    @Test
    public void whatIsStoredSurvivesIntoTheNextRun() {
        host.run("store.set(\"home.last\", \"spawn\")");
        assertEquals("spawn", host.store.get("home.last"));

        // A second run is a fresh script with nothing in hand; the store is the only thing carried over.
        assertEquals(Arrays.asList("spawn"), notices("notify(store.get(\"home.last\"))"));
    }

    @Test
    public void aMissingKeyIsNilOrWhateverTheCallerWouldRatherHave() {
        assertEquals(Arrays.asList("none", "nothing stored"),
            notices("if store.get(\"never.set\") == nil then notify(\"none\") end "
                + "notify(store.get(\"never.set\", \"nothing stored\"))"));
    }

    @Test
    public void storingNothingForgetsTheKey() {
        host.store.put("tool", "pickaxe");
        host.run("store.set(\"tool\", nil)");
        assertTrue(host.store.isEmpty());
    }

    @Test
    public void theCrosshairIsABlockOrAnEntityOrNothingAtAll() {
        assertEquals(Arrays.asList("looking at nothing"),
            notices("if not world.lookingAt() then notify(\"looking at nothing\") end"));

        host.lookingAt = LookTarget.block("gregtech:machine", "Electric Blast Furnace", 7, 12, 64, -30);
        assertEquals(Arrays.asList("block", "gregtech:machine", "Electric Blast Furnace", "7", "12", "64", "-30"),
            notices("local t = world.lookingAt() " + "notify(t.kind) notify(t.id) notify(t.label) notify(t.meta) "
                + "notify(t.x) notify(t.y) notify(t.z)"));

        host.lookingAt = LookTarget.entity("Villager", "Farmer", 1, 70, 2);
        assertEquals(Arrays.asList("entity", "Farmer"),
            notices("local t = world.lookingAt() notify(t.kind) notify(t.label)"));
    }

    @Test
    public void theInventoryIsCountedByIdAndOptionallyByDamage() {
        host.carried.add(new ScriptItem("minecraft:wool", 0, 32, "White Wool", 1));
        host.carried.add(new ScriptItem("minecraft:wool", 5, 8, "Lime Wool", 2));
        host.carried.add(new ScriptItem("minecraft:coal", 0, 64, "Coal", 3));

        // Without a damage value every variant counts, which is what "do I have wool" means. With one, only that
        // variant does, which is what "do I have lime wool" means.
        assertEquals(Arrays.asList("40", "8", "64", "0", "true", "false"),
            notices("notify(inventory.count(\"minecraft:wool\")) " + "notify(inventory.count(\"minecraft:wool\", 5)) "
                + "notify(inventory.count(\"minecraft:coal\")) "
                + "notify(inventory.count(\"minecraft:diamond\")) "
                + "notify(tostring(inventory.has(\"minecraft:coal\"))) "
                + "notify(tostring(inventory.has(\"minecraft:diamond\")))"));
    }

    @Test
    public void anInventoryListingCanBeHandedStraightToAMenu() {
        host.carried.add(new ScriptItem("minecraft:wool", 5, 8, "Lime Wool", 2));

        assertEquals(Arrays.asList("Lime Wool", "minecraft:wool:5", "2", "8"),
            notices("local items = inventory.items() "
                + "notify(items[1].label) notify(items[1].icon) notify(items[1].slot) notify(items[1].count)"));
    }

    @Test
    public void whatIsHeldIsTheSameShapeAsAnInventoryEntry() {
        assertEquals(Arrays.asList("empty handed"), notices("if not player.held then notify(\"empty handed\") end"));

        host.held = new ScriptItem("minecraft:iron_pickaxe", 12, 1, "Iron Pickaxe", 0);
        assertEquals(Arrays.asList("Iron Pickaxe", "minecraft:iron_pickaxe"),
            notices("notify(player.held.label) notify(player.held.id)"));
    }

    @Test
    public void aPromptAnswersWithTheTextOrWithNothing() {
        host.typed.add("riverside");

        assertEquals(Arrays.asList("riverside"), notices("notify(prompt(\"Name this home\"))"));
        assertEquals(Arrays.asList("Name this home"), host.prompts);

        // Nothing queued is the player cancelling, which reads as nil - the same nothing a dismissed menu gives.
        assertEquals(Arrays.asList("cancelled"), notices("if prompt(\"Again\") == nil then notify(\"cancelled\") end"));
    }

    @Test
    public void theWorldAndTheBodyAnswerInTheUnitsAPersonWouldUse() {
        host.worldTime = 24000L * 3 + 13000L;
        host.health = 15.5;
        host.food = 7;
        host.air = 300;

        assertEquals(Arrays.asList("13000", "3", "false", "testserver.example", "15.5", "7", "300"),
            notices("notify(world.time) notify(world.day) notify(tostring(world.isDay)) notify(world.name) "
                + "notify(player.health) notify(player.food) notify(player.air)"));
    }

    @Test
    public void nothingTheApiDoesNotHaveThrows() {
        // A script poking at a field that is not there has to get nil, so it can test for one version's feature
        // while still running on another.
        assertNull(host.run("notify(tostring(world.weather)) notify(tostring(inventory.weight))").error());
        assertEquals(Arrays.asList("nil", "nil"), host.notices);
    }

    private List<String> notices(String source) {
        host.notices.clear();
        host.run(source);
        return host.notices;
    }
}
