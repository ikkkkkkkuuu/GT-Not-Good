package com.xyp.gtnotgood.client.radialmenu.client.action;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;

public final class ActionExecutors {

    private static final Map<String, IActionExecutor> EXECUTORS = new HashMap<>();

    private static final Deque<ActionSpec> PENDING = new ArrayDeque<>();

    private ActionExecutors() {}

    public static void register(IActionExecutor executor) {
        EXECUTORS.put(executor.typeId(), executor);
    }

    public static IActionExecutor get(String typeId) {
        return typeId == null ? null : EXECUTORS.get(typeId);
    }

    public static void enqueue(ActionSpec spec) {
        if (spec != null && spec.type != null) {
            PENDING.add(spec);
        }
    }

    public static boolean runPending() {
        DelayedActions.beginTick();

        boolean ran = false;
        while (!PENDING.isEmpty()) {
            runNow(PENDING.poll());
            ran = true;
        }
        ran |= DelayedActions.onClientTick();
        return ran;
    }

    public static void runNow(ActionSpec spec) {
        if (WithoutScreen.wheelIsUp()) {
            WithoutScreen.runAction(spec);
            return;
        }
        execute(spec);
    }

    static void execute(ActionSpec spec) {
        IActionExecutor executor = get(spec.type);
        if (executor == null) {
            RadialMenuMod.LOG.warn("No executor registered for action type '" + spec.type + "'");
            return;
        }
        try {
            if (!executor.execute(spec)) {
                RadialMenuMod.LOG.warn("Action '" + spec.type + "' did not run");
            }
        } catch (Exception e) {
            RadialMenuMod.LOG.error("Action '" + spec.type + "' failed", e);
        }
    }
}
