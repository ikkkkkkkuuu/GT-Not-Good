package com.xyp.gtnotgood.client.radialmenu.core.script;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/**
 * What a script actually reads off {@code player}.
 *
 * <p>
 * Through a real run rather than by calling the maths directly: the fields arrive through a metatable keyed on
 * strings, so a name spelled one way in the API and another in the documentation is a field that silently reads nil.
 * Only running a script that asks for them by name catches that.
 */
public class PlayerFieldsTest {

    private final FakeScriptHost host = new FakeScriptHost();

    @Test
    public void theBlockPositionAndDimensionAreWhatTheHostSays() {
        host.x = 100;
        host.y = 64;
        host.z = -200;
        host.dimension = -1;

        assertEquals(Arrays.asList("Nortcast", "-1", "100", "64", "-200"),
            notices("notify(player.name) notify(player.dim) notify(player.x) notify(player.y) notify(player.z)"));
    }

    @Test
    public void theChunkIsTheOneThatBlockBelongsTo() {
        host.x = 100;
        host.z = -200;

        // 100 is chunk 6, offset 4. Negative coordinates are the half worth testing: -200 belongs to chunk -13 and
        // sits 8 blocks into it, which an integer division towards zero would get wrong in both halves.
        assertEquals(Arrays.asList("6", "-13", "4", "8"),
            notices("notify(player.chunkX) notify(player.chunkZ) notify(player.xInChunk) notify(player.zInChunk)"));
    }

    @Test
    public void theLookIsNormalisedAndNamed() {
        host.yaw = -90.0;
        host.pitch = -31.5;

        assertEquals(Arrays.asList("east", "270", "-31.5"),
            notices("notify(player.facing) notify(player.yaw) notify(player.pitch)"));
    }

    @Test
    public void aChunkCoordinateAndAnOffsetMakeAWorldCoordinateAgain() {
        host.x = 100;
        host.z = -200;

        // The round trip is the property worth holding: whatever the chunk maths says about a block, putting the two
        // halves back together has to return the block. Negative coordinates are where that stops being obvious.
        assertEquals(Arrays.asList("100", "-200", "-208", "-193"),
            notices("notify(chunk.toWorld(player.chunkX, player.xInChunk)) "
                + "notify(chunk.toWorld(player.chunkZ, player.zInChunk)) "
                + "notify(chunk.toWorld(chunk.of(-200))) "
                + "notify(chunk.toWorld(chunk.of(-200), chunk.size - 1))"));
    }

    @Test
    public void theChunkTableAnswersForAnyCoordinate() {
        // Not only the player's: a script that read a position out of chat has to be able to ask about that one.
        assertEquals(Arrays.asList("-1", "15", "0", "0"),
            notices("notify(chunk.of(-1)) notify(chunk.offset(-1)) notify(chunk.of(0)) notify(chunk.offset(0))"));
    }

    @Test
    public void somethingThePlayerTableDoesNotHaveIsNilRatherThanAnError() {
        // A script asking for a field that does not exist should be able to test for it, which it cannot do if the
        // read throws.
        assertEquals(Arrays.asList("nothing"), notices("if player.elevation == nil then notify(\"nothing\") end"));
    }

    /** Runs a script and hands back what it said, so each test reads as the line it is about. */
    private java.util.List<String> notices(String source) {
        host.notices.clear();
        host.run(source);
        return host.notices;
    }
}
