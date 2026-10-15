package com.xyp.gtnotgood.client.radialmenu.core.script;

import static org.junit.Assert.fail;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.AccentCoefficients;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;

/**
 * Stands in for the client while a script runs.
 *
 * <p>
 * This is what the split between a script and its host buys: the whole of a script's logic - a command sent, a reply
 * parsed, a wheel built, an entry chosen, a second command sent - is exercised here with no Minecraft anywhere near it.
 * Queue up what the server will say and what the player will choose, run the script, and read back what it did.
 */
class FakeScriptHost implements ScriptContext {

    /** Lines the "server" will answer with, in order. */
    final Deque<String> incoming = new ArrayDeque<>();

    /** Entries the "player" will choose, one per menu, counted from 1. 0 dismisses the menu. */
    final Deque<Integer> choices = new ArrayDeque<>();

    final List<String> sent = new ArrayList<>();
    final List<String> notices = new ArrayList<>();
    final List<String> logs = new ArrayList<>();
    final List<ActionSpec> actions = new ArrayList<>();

    /** Every wheel the script opened or updated, in order. */
    final List<MenuNode> menus = new ArrayList<>();

    boolean closedMenu;

    /** The run {@link #run} drove last, so a failed assertion can say what stopped it. */
    private ScriptTask lastTask;

    String playerName = "Nortcast";
    int dimension = 0;
    int x = 100;
    int y = 64;
    int z = -200;

    /** Facing west and level, so a test reading either one gets something it can tell apart from nothing. */
    double yaw = 90.0;
    double pitch = 0.0;

    double health = 20.0;
    int food = 20;
    int air = 300;

    /** Mid-morning of the fourth day, which is a time no arithmetic mistake would land on by accident. */
    long worldTime = 24000L * 3 + 1000L;

    String worldName = "testserver.example";

    /** What the crosshair is on, or null for thin air. */
    LookTarget lookingAt;

    ScriptItem held;

    final List<ScriptItem> carried = new ArrayList<>();

    /** What a script has remembered, and what it asked to remember. */
    final Map<String, String> store = new LinkedHashMap<>();

    /** Answers the prompts, in order. A missing one is the player cancelling. */
    final Deque<String> typed = new ArrayDeque<>();

    /** Every prompt the script put up, so a test can check what it asked. */
    final List<String> prompts = new ArrayList<>();

    /**
     * The real instruction budget and entry cap, but not the real wall clock. Twenty milliseconds is a stutter in a
     * warmed-up client; on a cold test JVM on a shared CI runner it is class loading, and a script that did nothing
     * wrong was stopped before it opened its menu. The watchdog itself is proven by its own test with its own limits.
     */
    static final ScriptLimits TEST_LIMITS = new ScriptLimits(ScriptLimits.DEFAULT.instructionsPerResume,
        10_000_000_000L, ScriptLimits.DEFAULT.maxMenuItems);

    ScriptTask run(String source) {
        return run(source, TEST_LIMITS);
    }

    ScriptTask run(String source, ScriptLimits limits) {
        ScriptTask task = ScriptTask.start(source, "test", "token-1", this, limits);
        lastTask = task;
        drive(task);
        return task;
    }

    private void drive(ScriptTask task) {
        // A script that cannot be finished in this many exchanges is a test that has hung, and a hung test says far
        // less than a failed one.
        for (int step = 0; step < 2000; step++) {
            ScriptRequest request = task.pending();
            if (request == null) {
                return;
            }
            perform(task, request);
        }
        fail("the script was still asking for things after 2000 exchanges");
    }

    private void perform(ScriptTask task, ScriptRequest request) {
        switch (request.kind) {
            case Send:
                sent.add(request.text);
                task.resumeVoid();
                break;
            case Notify:
                notices.add(request.text);
                task.resumeVoid();
                break;
            case Log:
                logs.add(request.text);
                task.resumeVoid();
                break;
            case Sleep:
                task.resumeVoid();
                break;
            case StoreSet:
                if (request.text == null) {
                    store.remove(request.key);
                } else {
                    store.put(request.key, request.text);
                }
                task.resumeVoid();
                break;
            case Prompt:
                prompts.add(request.text);
                task.resumeText(typed.isEmpty() ? null : typed.poll());
                break;
            case Action:
                actions.add(request.action);
                task.resumeVoid();
                break;
            case AwaitLine:
                // An empty queue is a server that never answered: the wait runs out rather than blocking a test.
                if (incoming.isEmpty()) {
                    task.resumeLine(null, request.ticks);
                } else {
                    task.resumeLine(incoming.poll(), 1);
                }
                break;
            case Menu:
                menus.add(request.menu);
                task.resumeChoice(choices.isEmpty() ? 0 : choices.poll());
                break;
            case MenuUpdate:
                menus.add(request.menu);
                task.resumeVoid();
                break;
            case MenuClose:
                closedMenu = true;
                task.resumeVoid();
                break;
            default:
                fail("unhandled request kind " + request.kind);
        }
    }

    /** The wheel the script opened first, which is the one most tests are about. */
    MenuNode firstMenu() {
        if (menus.isEmpty()) {
            String error = lastTask == null ? null : lastTask.error();
            fail("the script opened no menu" + (error == null ? "" : ": " + error));
        }
        return menus.get(0);
    }

    @Override
    public String playerName() {
        return playerName;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public int blockX() {
        return x;
    }

    @Override
    public int blockY() {
        return y;
    }

    @Override
    public int blockZ() {
        return z;
    }

    @Override
    public double yaw() {
        return yaw;
    }

    @Override
    public double health() {
        return health;
    }

    @Override
    public int food() {
        return food;
    }

    @Override
    public int air() {
        return air;
    }

    @Override
    public ScriptItem heldItem() {
        return held;
    }

    @Override
    public List<ScriptItem> inventory() {
        return carried;
    }

    @Override
    public LookTarget lookingAt() {
        return lookingAt;
    }

    @Override
    public long worldTime() {
        return worldTime;
    }

    @Override
    public String worldName() {
        return worldName;
    }

    @Override
    public String storeGet(String key) {
        return store.get(key);
    }

    @Override
    public double pitch() {
        return pitch;
    }

    @Override
    public AccentCoefficients accentCoefficients() {
        return new AccentCoefficients(new AccentCoefficients.Tone(160, 40), new AccentCoefficients.Tone(220, 100),
            new AccentCoefficients.Tone(90, 130), new AccentCoefficients.Tone(230, 160),
            new AccentCoefficients.Tone(128, 20));
    }
}
