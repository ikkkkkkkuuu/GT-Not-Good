package com.xyp.gtnotgood.client.radialmenu.core.script;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

public final class ScriptTask {

    private static final int HOOK_INTERVAL = 500;

    private final String token;
    private final ScriptContext context;
    private final ScriptLimits limits;
    private final Globals globals;

    private LuaThread thread;

    private ScriptRequest pending;
    private boolean finished;
    private String error;

    private int instructionsThisResume;
    private long resumeDeadlineNanos;

    private ScriptTask(String token, ScriptContext context, ScriptLimits limits) {
        this.token = token;
        this.context = context;
        this.limits = limits;
        this.globals = ScriptSandbox.newGlobals();
    }

    public static ScriptTask start(String source, String name, String token, ScriptContext context,
        ScriptLimits limits) {
        ScriptTask task = new ScriptTask(token, context, limits);
        task.launch(source == null ? "" : source, name == null ? "script" : name);
        return task;
    }

    private void launch(String source, String name) {
        LuaValue chunk;
        try {
            chunk = globals.load(source, name);
        } catch (LuaError e) {
            fail(e);
            return;
        }

        try {
            ScriptApi.install(this, globals);
        } catch (LuaError e) {
            fail(e);
            return;
        }

        thread = new LuaThread(globals, chunk);

        thread.state.hookfunc = new Watchdog();
        thread.state.hookcount = HOOK_INTERVAL;

        resume(LuaValue.NONE);
    }

    public ScriptRequest pending() {
        return pending;
    }

    public boolean isFinished() {
        return finished;
    }

    public String error() {
        return error;
    }

    public String token() {
        return token;
    }

    public ScriptContext context() {
        return context;
    }

    public ScriptLimits limits() {
        return limits;
    }

    public void resumeVoid() {
        resume(LuaValue.NONE);
    }

    public void resumeLine(String line, int ticksWaited) {
        expect(ScriptRequest.Kind.AwaitLine);
        resume(LuaValue.varargsOf(line == null ? LuaValue.NIL : LuaValue.valueOf(line),
            LuaValue.valueOf(Math.max(0, ticksWaited))));
    }

    public void resumeChoice(int choice) {
        expect(ScriptRequest.Kind.Menu);
        resume(choice <= 0 ? LuaValue.NIL : LuaValue.valueOf(choice));
    }

    public void resumeText(String text) {
        expect(ScriptRequest.Kind.Prompt);
        resume(text == null ? LuaValue.NIL : LuaValue.valueOf(text));
    }

    public void cancel(String reason) {
        pending = null;
        thread = null;
        finished = true;
        if (error == null) {
            error = reason;
        }
    }

    Varargs yieldRequest(ScriptRequest request) {
        this.pending = request;
        return globals.yield(LuaValue.NONE);
    }

    private void resume(Varargs args) {
        if (finished || thread == null) {
            return;
        }

        pending = null;
        instructionsThisResume = 0;
        resumeDeadlineNanos = System.nanoTime() + limits.nanosPerResume;

        Varargs result;
        try {
            result = thread.resume(args);
        } catch (LuaError e) {
            fail(e);
            return;
        }

        if (!result.arg1().toboolean()) {
            finished = true;
            pending = null;
            error = result.arg(2).tojstring();
            return;
        }

        if (pending == null) {
            finished = true;
        }
    }

    private void fail(LuaError e) {
        finished = true;
        pending = null;
        error = e.getMessage() == null ? e.toString() : e.getMessage();
    }

    private void expect(ScriptRequest.Kind kind) {
        if (pending == null || pending.kind != kind) {
            throw new IllegalStateException(
                "script is waiting for " + (pending == null ? "nothing" : pending.kind) + ", not " + kind);
        }
    }

    private final class Watchdog extends VarArgFunction {

        @Override
        public Varargs invoke(Varargs args) {
            instructionsThisResume += HOOK_INTERVAL;
            if (instructionsThisResume > limits.instructionsPerResume) {
                throw new LuaError(
                    "script ran " + limits.instructionsPerResume + " instructions without waiting for anything");
            }
            if (System.nanoTime() > resumeDeadlineNanos) {
                throw new LuaError(
                    "script ran for more than " + limits.nanosPerResume / 1_000_000L + "ms without waiting");
            }
            return LuaValue.NONE;
        }
    }
}
