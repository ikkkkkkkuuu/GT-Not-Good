package com.xyp.gtnotgood.client.radialmenu.client.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;

public final class DelayedActions {

    private static final class Entry {

        Object owner;
        ActionSpec spec;
        String rawCommand;

        long dueTick;
    }

    private static long tick;

    private static final List<Entry> PENDING = new ArrayList<>();

    private DelayedActions() {}

    public static void scheduleAction(Object owner, ActionSpec spec, int delayTicks) {
        Entry entry = new Entry();
        entry.owner = owner;
        entry.spec = spec;
        entry.dueTick = tick + Math.max(0, delayTicks);
        PENDING.add(entry);
    }

    public static void scheduleCommand(Object owner, String command, int delayTicks) {
        Entry entry = new Entry();
        entry.owner = owner;
        entry.rawCommand = command;
        entry.dueTick = tick + Math.max(0, delayTicks);
        PENDING.add(entry);
    }

    public static void cancel(Object owner) {
        Iterator<Entry> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().owner == owner) {
                iterator.remove();
            }
        }
    }

    public static void cancelAll() {
        PENDING.clear();
    }

    public static boolean hasPending() {
        return !PENDING.isEmpty();
    }

    public static void beginTick() {
        tick++;
    }

    public static boolean onClientTick() {
        if (PENDING.isEmpty()) {
            return false;
        }

        List<Entry> due = new ArrayList<>();
        Iterator<Entry> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (tick >= entry.dueTick) {
                due.add(entry);
                iterator.remove();
            }
        }

        for (Entry entry : due) {
            if (entry.rawCommand != null) {
                ChatSender.send(entry.rawCommand);
            } else if (entry.spec != null) {
                ActionExecutors.runNow(entry.spec);
            }
        }
        return !due.isEmpty();
    }
}
