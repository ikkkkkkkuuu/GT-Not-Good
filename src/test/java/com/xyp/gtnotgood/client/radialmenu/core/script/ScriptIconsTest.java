package com.xyp.gtnotgood.client.radialmenu.core.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;

/**
 * Holds what the editor's icon button writes against what a script's icon is read as.
 *
 * <p>
 * The two are separate code, and the failure is quiet: a spelling the reader takes for a different icon draws that
 * icon, with nothing to say the picker was asked for another.
 */
public class ScriptIconsTest {

    private static IconSpec roundTrip(IconSpec icon) {
        String spelled = ScriptIcons.spell(icon);
        assertEquals("expected the one-string form: " + spelled, '"', spelled.charAt(0));
        return IconSpec.parse(spelled.substring(1, spelled.length() - 1));
    }

    private static void assertSame(IconSpec expected, IconSpec actual) {
        assertNotNull(actual);
        assertEquals(expected.kind, actual.kind);
        assertEquals(expected.id, actual.id);
        assertEquals(expected.meta, actual.meta);
    }

    @Test
    public void everyKindReadsBackAsItself() {
        IconSpec[] icons = { IconSpec.item("minecraft:stone", 0), IconSpec.item("minecraft:stained_hardened_clay", 5),
            IconSpec.item("gregtech:gt.metaitem.01", 32000), IconSpec.sprite("phosphor:house", null),
            IconSpec.sprite("phosphor:house", ""), IconSpec.file("backpack.png"), IconSpec.effect("potion.moveSpeed"),
            IconSpec.player("Nortcast") };
        for (IconSpec icon : icons) {
            assertSame(icon, roundTrip(icon));
        }
    }

    @Test
    public void spellsTheShortFormsTheGuideUses() {
        assertEquals("\"minecraft:stone\"", ScriptIcons.spell(IconSpec.item("minecraft:stone", 0)));
        assertEquals("\"minecraft:wool:5\"", ScriptIcons.spell(IconSpec.item("minecraft:wool", 5)));
        assertEquals("\"sprite:phosphor:sword\"", ScriptIcons.spell(IconSpec.sprite("phosphor:sword", null)));
        assertEquals("\"player:Nortcast\"", ScriptIcons.spell(IconSpec.player("Nortcast")));
    }

    @Test
    public void aTintIsTheOnlyThingThatNeedsATable() {
        assertEquals("{ kind = \"sprite\", id = \"phosphor:house\", color = \"#7FD4FF\" }",
            ScriptIcons.spell(IconSpec.sprite("phosphor:house", "#7FD4FF")));

        IconSpec file = IconSpec.file("backpack.png");
        file.color = "#FF0000";
        assertEquals("{ kind = \"file\", id = \"backpack.png\", color = \"#FF0000\" }", ScriptIcons.spell(file));
    }

    @Test
    public void anItemFromAModNamedLikeAKindKeepsBeingAnItem() {
        IconSpec icon = IconSpec.item("player:trophy", 2);
        assertEquals("\"item:player:trophy:2\"", ScriptIcons.spell(icon));
        assertSame(icon, roundTrip(icon));
    }

    @Test
    public void quotesAndBackslashesAreEscaped() {
        assertEquals("\"file:a\\\\b\\\"c.png\"", ScriptIcons.spell(IconSpec.file("a\\b\"c.png")));
    }

    private static LuaSyntax.Token at(String line, int column) {
        List<LuaSyntax.Token> tokens = LuaSyntax.tokenize(Collections.singletonList(line)).get(0);
        return ScriptIcons.stringAt(tokens, line, column);
    }

    @Test
    public void theCaretInsideAStringFindsIt() {
        String line = "icon = \"minecraft:stone\",";
        int open = line.indexOf('"');
        int close = line.lastIndexOf('"');

        LuaSyntax.Token token = at(line, open + 4);
        assertNotNull(token);
        assertEquals(open, token.start);
        assertEquals(close + 1, token.end);

        // Between the quotes of an empty string counts; that is where the "menu entry" snippet leaves a blank.
        assertNotNull(at("icon = \"\"", 8));
        assertNotNull(at(line, open + 1));
        assertNotNull(at(line, close));
    }

    @Test
    public void theCaretBesideAStringDoesNot() {
        String line = "icon = \"minecraft:stone\",";
        assertNull(at(line, line.indexOf('"')));
        assertNull(at(line, line.lastIndexOf('"') + 1));
        assertNull(at(line, 2));
    }

    @Test
    public void aStringStillBeingTypedRunsToTheEndOfTheLine() {
        String line = "icon = \"minecraft:st";
        assertNotNull(at(line, line.length()));
    }

    @Test
    public void picksTheRightOneOfTwo() {
        String line = "{ key = \"home\", icon = \"minecraft:bed\" }";
        LuaSyntax.Token token = at(line, line.indexOf("bed"));
        assertNotNull(token);
        assertEquals("\"minecraft:bed\"", line.substring(token.start, token.end));
    }
}
