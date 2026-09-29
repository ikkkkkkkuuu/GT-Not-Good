package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import appeng.api.config.SortDir;
import appeng.api.storage.data.IAEStack;
import appeng.util.ItemSorters;

/** Uses AE2's transformed native comparators to verify ordering and display-name evaluation counts. */
final class ItemSortNameCacheClientChecks {

    static void run() {
        AtomicInteger lookups = new AtomicInteger();
        List<IAEStack<?>> source = new ArrayList<>();
        String[] mutableNames = new String[512];
        for (int i = 0; i < mutableNames.length; i++) {
            final int index = i;
            mutableNames[i] = (i % 2 == 0 ? "\u00a7c" : "&a") + "Item " + (i % 79);
            source.add(
                (IAEStack<?>) Proxy.newProxyInstance(
                    IAEStack.class.getClassLoader(),
                    new Class<?>[] { IAEStack.class },
                    (proxy, method, args) -> {
                        if (method.getName()
                            .equals("getDisplayName")) {
                            lookups.incrementAndGet();
                            return mutableNames[index];
                        }
                        if (method.getName()
                            .equals("getModId")) return "mod" + index % 3;
                        if (method.getName()
                            .equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName()
                            .equals("equals")) return proxy == args[0];
                        throw new AssertionError("Unexpected stack method: " + method);
                    }));
        }
        Collections.shuffle(source, new Random(729));
        try {
            for (SortDir direction : SortDir.values()) {
                ItemSorters.setDirection(direction);
                check(source, ItemSorters.CONFIG_BASED_SORT_BY_NAME, lookups);
                check(source, ItemSorters.CONFIG_BASED_SORT_BY_MOD, lookups);
            }
            mutableNames[0] = "Renamed after previous refresh";
            check(source, ItemSorters.CONFIG_BASED_SORT_BY_NAME, lookups);
            IAEStack<?> first = source.get(0);
            ItemSortNameCache.duringSort(() -> {
                ItemSortNameCache.put(first, "outer");
                try {
                    ItemSortNameCache.duringSort(() -> {
                        require(ItemSortNameCache.get(first) == null, "nested cache independent");
                        throw new IllegalStateException("test comparator failure");
                    });
                } catch (IllegalStateException expected) {}
                require("outer".equals(ItemSortNameCache.get(first)), "outer scope restored");
            });
            require(ItemSortNameCache.get(first) == null, "cache released after sort");
        } finally {
            ItemSorters.setDirection(SortDir.ASCENDING);
        }
        System.out.println("TERMINAL_NAME_QA: native order, formatting, ties, rename, direction and scope PASS");
    }

    private static void check(List<IAEStack<?>> source, Comparator<IAEStack<?>> comparator, AtomicInteger lookups) {
        List<IAEStack<?>> expected = new ArrayList<>(source);
        lookups.set(0);
        expected.sort(comparator);
        int uncached = lookups.get();
        List<IAEStack<?>> actual = new ArrayList<>(source);
        lookups.set(0);
        ItemSortNameCache.duringSort(() -> actual.sort(comparator));
        require(actual.equals(expected), "identical stable native ordering");
        require(lookups.get() <= source.size(), "at most one name evaluation per stack per sort");
        require(uncached > lookups.get(), "name processing reduced");
        System.out.println("TERMINAL_NAME_QA: uncached=" + uncached + " cached=" + lookups.get());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
