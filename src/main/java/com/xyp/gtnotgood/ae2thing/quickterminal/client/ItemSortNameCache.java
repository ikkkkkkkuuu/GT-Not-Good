package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import java.util.IdentityHashMap;
import java.util.Map;

import appeng.api.storage.data.IAEStack;

/** Sort-local names cannot survive a language change, stack rename or later inventory refresh. */
public final class ItemSortNameCache {

    private static final ThreadLocal<Map<IAEStack<?>, String>> names = new ThreadLocal<>();

    private ItemSortNameCache() {}

    /** Restores an outer sort's cache on both normal return and comparator failure. */
    public static void duringSort(Runnable sort) {
        Map<IAEStack<?>, String> previous = names.get();
        names.set(new IdentityHashMap<>());
        try {
            sort.run();
        } finally {
            if (previous == null) names.remove();
            else names.set(previous);
        }
    }

    public static String get(IAEStack<?> stack) {
        Map<IAEStack<?>, String> current = names.get();
        return current == null ? null : current.get(stack);
    }

    public static void put(IAEStack<?> stack, String name) {
        Map<IAEStack<?>, String> current = names.get();
        if (current != null) current.put(stack, name);
    }
}
