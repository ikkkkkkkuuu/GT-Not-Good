package com.xyp.gtnotgood.client.radialmenu.core.action;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PlaceholdersTest {

    private static Map<String, String> context() {
        Map<String, String> values = new HashMap<>();
        values.put(Placeholders.PLAYER, "Navatusein");
        values.put(Placeholders.DIMENSION, "-1");
        values.put(Placeholders.X, "602");
        values.put(Placeholders.Y, "4");
        values.put(Placeholders.Z, "-751");
        return values;
    }

    @Test
    public void substitutesKnownPlaceholders() {
        assertEquals("/tp Navatusein 602 4 -751", Placeholders.apply("/tp {player} {x} {y} {z}", context()));
    }

    @Test
    public void leavesTextWithoutPlaceholdersAlone() {
        assertEquals("/home", Placeholders.apply("/home", context()));
    }

    @Test
    public void unknownPlaceholderIsLeftVisibleRatherThanBlanked() {
        // A silently emptied argument can turn a harmless command into a destructive one, and it is far more likely
        // to be a typo the player needs to see.
        assertEquals("/give {playerName} stone", Placeholders.apply("/give {playerName} stone", context()));
    }

    @Test
    public void unbalancedBraceIsTreatedAsLiteralText() {
        assertEquals("/say {player", Placeholders.apply("/say {player", context()));
    }

    @Test
    public void repeatedPlaceholdersAreAllReplaced() {
        assertEquals("Navatusein Navatusein", Placeholders.apply("{player} {player}", context()));
    }

    @Test
    public void nullTextSurvives() {
        assertNull(Placeholders.apply(null, context()));
    }

    @Test
    public void nullContextLeavesTextUntouched() {
        assertEquals("{player}", Placeholders.apply("{player}", null));
    }

    @Test
    public void emptyPlaceholderNameIsNotAMatch() {
        assertEquals("{}", Placeholders.apply("{}", context()));
    }

    @Test
    public void splitDropsBlankLinesAndTrims() {
        assertArrayEquals(new String[] { "/home", "/say hi" }, Placeholders.splitLines("  /home  \n\n   \n/say hi\n"));
    }

    @Test
    public void splitHandlesWindowsLineEndings() {
        assertArrayEquals(new String[] { "/a", "/b" }, Placeholders.splitLines("/a\r\n/b"));
    }

    @Test
    public void splitOfNothingIsEmpty() {
        assertEquals(0, Placeholders.splitLines(null).length);
        assertEquals(0, Placeholders.splitLines("   \n  ").length);
    }
}
