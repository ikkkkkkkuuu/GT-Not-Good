package com.xyp.gtnotgood.utils.machine;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class ApiaryDropScan {

    private static final ThreadLocal<Scope> currentScope = new ThreadLocal<>();

    private ApiaryDropScan() {}

    public static Scope begin(Map<?, ?> progress) {
        Scope scope = new Scope(progress, currentScope.get());
        currentScope.set(scope);
        return scope;
    }

    public static <K, V> Set<Map.Entry<K, V>> entries(Map<K, V> progress, Set<K> changedKeys,
        Set<Map.Entry<K, V>> allEntries) {
        Scope scope = currentScope.get();
        if (scope == null || scope.progress != progress) return allEntries;
        // The first queen also drains any outstanding progress from earlier processing calls.
        if (!scope.scanned) {
            scope.scanned = true;
            return allEntries;
        }
        if (progress.size() <= changedKeys.size()) return allEntries;
        return new AbstractSet<Map.Entry<K, V>>() {

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                Iterator<K> keys = changedKeys.iterator();
                return new Iterator<Map.Entry<K, V>>() {

                    @Override
                    public boolean hasNext() {
                        return keys.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        K key = keys.next();
                        return new AbstractMap.SimpleEntry<K, V>(key, progress.get(key)) {

                            @Override
                            public V setValue(V value) {
                                V previous = progress.put(key, value);
                                super.setValue(value);
                                return previous;
                            }
                        };
                    }
                };
            }

            @Override
            public int size() {
                return changedKeys.size();
            }
        };
    }

    public static final class Scope implements AutoCloseable {

        private final Map<?, ?> progress;
        private final Scope previous;
        private boolean scanned;

        private Scope(Map<?, ?> progress, Scope previous) {
            this.progress = progress;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) currentScope.remove();
            else currentScope.set(previous);
        }
    }
}
