package com.xyp.gtnotgood.client.radialmenu.core.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** What a script cannot reach, and what happens when one misbehaves. */
public class ScriptSandboxTest {

    @Test
    public void nothingThatReachesAFileAProcessOrAnotherChunkIsInTheEnvironment() {
        FakeScriptHost host = new FakeScriptHost();

        host.run("notify(type(io) .. ' ' .. type(os) .. ' ' .. type(require) .. ' ' .. type(package)"
            + " .. ' ' .. type(debug) .. ' ' .. type(loadfile) .. ' ' .. type(dofile))");

        assertEquals("nil nil nil nil nil nil nil", host.notices.get(0));
    }

    @Test
    public void theLanguageItselfIsStillThere() {
        FakeScriptHost host = new FakeScriptHost();

        host.run("notify(type(string.format) .. ' ' .. type(table.concat) .. ' ' .. type(math.floor)"
            + " .. ' ' .. type(pcall) .. ' ' .. type(coroutine.create))");

        assertEquals("function function function function function", host.notices.get(0));
    }

    @Test
    public void aScriptThatDoesNotCompileComesBackAsAnErrorRatherThanAnException() {
        FakeScriptHost host = new FakeScriptHost();

        ScriptTask task = host.run("this is not lua at all");

        assertTrue(task.isFinished());
        assertNotNull(task.error());
        assertTrue(host.sent.isEmpty());
    }

    @Test
    public void anErrorHalfwayStopsTheScriptAndKeepsWhatItAlreadyDid() {
        FakeScriptHost host = new FakeScriptHost();

        ScriptTask task = host.run("chat.send('/first')\n" + "local nothing = nil\n" + "notify(nothing.field)\n");

        assertTrue(task.isFinished());
        assertNotNull(task.error());
        assertEquals(1, host.sent.size());
        assertEquals("/first", host.sent.get(0));
    }

    @Test
    public void aLoopThatNeverWaitsIsStoppedRatherThanFreezingTheGame() {
        FakeScriptHost host = new FakeScriptHost();
        // Deliberately tiny, so the test proves the watchdog rather than waiting for the real budget.
        ScriptLimits tight = new ScriptLimits(5_000, 2_000_000_000L, 24);

        ScriptTask task = host.run("while true do end", tight);

        assertTrue(task.isFinished());
        assertNotNull(task.error());
        assertTrue(task.error(), task.error().contains("without waiting"));
    }

    @Test
    public void waitingIsNotSpending() {
        // The same budget, but the script yields: a run that waits for a hundred ticks costs nothing at all.
        FakeScriptHost host = new FakeScriptHost();
        ScriptLimits tight = new ScriptLimits(5_000, 2_000_000_000L, 24);

        ScriptTask task = host.run("for i = 1, 50 do sleep(1) end\n" + "chat.send('/done')\n", tight);

        assertNull(task.error());
        assertEquals("/done", host.sent.get(0));
    }

    @Test
    public void aScriptCannotTakeTheHostHalfOfMenuOpenForItself() {
        // The prelude hides it after wrapping it; reaching past the wrapper would skip key resolution and onPick.
        FakeScriptHost host = new FakeScriptHost();

        host.run("notify(type(chat.line) .. ' ' .. type(menu.update) .. ' ' .. type(menu.open))");

        assertEquals("nil function function", host.notices.get(0));
    }
}
