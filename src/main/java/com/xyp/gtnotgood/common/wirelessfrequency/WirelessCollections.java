// SPDX-License-Identifier: LGPL-3.0-only

package com.xyp.gtnotgood.common.wirelessfrequency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

final class WirelessCollections {

    private WirelessCollections() {}

    @SafeVarargs
    static <T> List<T> listOf(T... values) {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(values)));
    }

    static <T> List<T> copyList(Collection<? extends T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    @SafeVarargs
    static <T> Set<T> setOf(T... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(values)));
    }

    static <T> Set<T> copySet(Collection<? extends T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    static <K, V> Map<K, V> copyMap(Map<? extends K, ? extends V> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    static <K, V> Map<K, V> emptyMap() {
        return Collections.emptyMap();
    }

    static <T> Collector<T, ?, Set<T>> toImmutableSet() {
        return Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet);
    }
}
