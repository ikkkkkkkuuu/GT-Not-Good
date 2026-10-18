package com.xyp.gtnotgood.utils.machine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

public class ApiaryDropScanTest {

    @Test
    public void perQueenRoundingAndStackBoundariesMatchNativeConsumption() {
        List<Map<Integer, Double>> queens = Arrays.asList(queen(0, 1d), queen(0, 64d), queen(0, Math.ulp(65d)),
            queen(1, 65d), queen(1, 0.5d), queen(0, 0.75d));
        assertEquivalent(queens, new HashMap<>());

        Map<Integer, Double> progress = new HashMap<>();
        List<Integer> chunks = new ArrayList<>();
        try (ApiaryDropScan.Scope scope = ApiaryDropScan.begin(progress)) {
            for (Map<Integer, Double> queen : queens) {
                merge(progress, queen);
                drain(ApiaryDropScan.entries(progress, queen.keySet(), progress.entrySet()), chunks);
            }
        }
        assertEquals(Arrays.asList(64, 1, 64, 1), chunks);
        assertEquals(0.5d, progress.get(1), 0d);
    }

    @Test
    public void outstandingProductsAreConsumedOnceAtTheStartOfEveryCycle() {
        Map<Integer, Double> progress = new HashMap<>();
        progress.put(99, 65.5d);
        List<Map<Integer, Double>> queens = Arrays.asList(queen(0, 0.25d), queen(1, 0.5d));
        assertEquivalent(queens, progress);
        try (ApiaryDropScan.Scope scope = ApiaryDropScan.begin(progress)) {
            List<Integer> chunks = new ArrayList<>();
            merge(progress, queens.get(0));
            drain(ApiaryDropScan.entries(progress, queens.get(0).keySet(), progress.entrySet()), chunks);
            assertEquals(Arrays.asList(64, 1), chunks);
            assertEquals(0.5d, progress.get(99), 0d);
        }
        progress.put(98, 2.25d);
        try (ApiaryDropScan.Scope scope = ApiaryDropScan.begin(progress)) {
            assertSame(progress.entrySet(),
                ApiaryDropScan.entries(progress, Collections.emptySet(), progress.entrySet()));
        }
    }

    @Test
    public void wholeStacksKeepTheNativeStrictlyGreaterThanOneThreshold() {
        List<Map<Integer, Double>> queens = Arrays.asList(queen(0, 64d), queen(1, 65d), queen(2, 129d),
            queen(3, 129.5d));
        Map<Integer, Double> progress = new HashMap<>();
        Map<Integer, List<Integer>> chunks = new HashMap<>();
        try (ApiaryDropScan.Scope scope = ApiaryDropScan.begin(progress)) {
            for (Map<Integer, Double> queen : queens) {
                merge(progress, queen);
                drainByProduct(ApiaryDropScan.entries(progress, queen.keySet(), progress.entrySet()), chunks);
            }
        }
        assertEquals(Collections.singletonList(64), chunks.get(0));
        assertEquals(Collections.singletonList(64), chunks.get(1));
        assertEquals(Arrays.asList(64, 64), chunks.get(2));
        assertEquals(Arrays.asList(64, 64, 1), chunks.get(3));
        assertEquals(0d, progress.get(0), 0d);
        assertEquals(1d, progress.get(1), 0d);
        assertEquals(1d, progress.get(2), 0d);
        assertEquals(0.5d, progress.get(3), 0d);
        assertEquivalent(queens, new HashMap<>());
    }

    @Test
    public void sharedAndDistinctProductsMatchNativeForRepeatedCycles() {
        Random random = new Random(0x415049415259L);
        List<Map<Integer, Double>> queens = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            Map<Integer, Double> products = new LinkedHashMap<>();
            for (int j = 0; j < 6; j++) {
                int id = j < 3 ? random.nextInt(16) : i * 3 + j + 16;
                products.merge(id, random.nextDouble() * 200d, Double::sum);
            }
            queens.add(products);
        }
        Map<Integer, Double> progress = new HashMap<>();
        progress.put(-1, 128.0000000001d);
        for (int cycle = 0; cycle < 8; cycle++) progress = assertEquivalent(queens, progress);
    }

    @Test
    public void traversalScalesWithProductsChangedByEachQueen() {
        Map<Integer, Double> progress = new HashMap<>();
        int visited = 0;
        try (ApiaryDropScan.Scope scope = ApiaryDropScan.begin(progress)) {
            for (int queen = 0; queen < 256; queen++) {
                Set<Integer> changed = new LinkedHashSet<>();
                for (int product = 0; product < 4; product++) {
                    int id = queen * 4 + product;
                    changed.add(id);
                    progress.put(id, 0.5d);
                }
                Set<Map.Entry<Integer, Double>> entries = ApiaryDropScan.entries(progress, changed,
                    progress.entrySet());
                for (Map.Entry<Integer, Double> ignored : entries) visited++;
            }
        }
        assertEquals(1024, visited);
        assertTrue(visited < 4 * 256 * 257 / 2);
    }

    @Test
    public void nestedAndExceptionalCallsRestoreThePreviousScope() {
        Map<Integer, Double> first = new HashMap<>();
        first.put(0, 0.5d);
        first.put(1, 0.5d);
        Map<Integer, Double> second = new HashMap<>(first);
        Set<Integer> changed = Collections.singleton(0);
        assertSame(first.entrySet(), ApiaryDropScan.entries(first, changed, first.entrySet()));
        try (ApiaryDropScan.Scope outer = ApiaryDropScan.begin(first)) {
            assertSame(first.entrySet(), ApiaryDropScan.entries(first, changed, first.entrySet()));
            try (ApiaryDropScan.Scope inner = ApiaryDropScan.begin(second)) {
                assertSame(second.entrySet(), ApiaryDropScan.entries(second, changed, second.entrySet()));
                throw new IllegalStateException("fixture");
            } catch (IllegalStateException expected) {
                assertEquals("fixture", expected.getMessage());
            }
            assertEquals(1, ApiaryDropScan.entries(first, changed, first.entrySet()).size());
            assertSame(second.entrySet(), ApiaryDropScan.entries(second, changed, second.entrySet()));
        }
        assertSame(first.entrySet(), ApiaryDropScan.entries(first, changed, first.entrySet()));
    }

    private static Map<Integer, Double> assertEquivalent(List<Map<Integer, Double>> queens,
        Map<Integer, Double> initialProgress) {
        Map<Integer, Double> expectedProgress = new HashMap<>(initialProgress);
        Map<Integer, Double> actualProgress = new HashMap<>(initialProgress);
        Map<Integer, List<Integer>> expectedChunks = new HashMap<>();
        Map<Integer, List<Integer>> actualChunks = new HashMap<>();
        try (ApiaryDropScan.Scope scope = ApiaryDropScan.begin(actualProgress)) {
            for (Map<Integer, Double> queen : queens) {
                merge(expectedProgress, queen);
                merge(actualProgress, queen);
                drainByProduct(expectedProgress.entrySet(), expectedChunks);
                drainByProduct(ApiaryDropScan.entries(actualProgress, queen.keySet(), actualProgress.entrySet()),
                    actualChunks);
                assertEquals(expectedProgress, actualProgress);
                assertEquals(expectedChunks, actualChunks);
            }
        }
        return actualProgress;
    }

    private static Map<Integer, Double> queen(int product, double amount) {
        return Collections.singletonMap(product, amount);
    }

    private static void merge(Map<Integer, Double> progress, Map<Integer, Double> products) {
        products.forEach((id, amount) -> progress.merge(id, amount, Double::sum));
    }

    private static void drainByProduct(Set<Map.Entry<Integer, Double>> entries, Map<Integer, List<Integer>> chunks) {
        for (Map.Entry<Integer, Double> entry : entries) {
            List<Integer> productChunks = chunks.computeIfAbsent(entry.getKey(), key -> new ArrayList<>());
            consume(entry, productChunks);
            if (productChunks.isEmpty()) chunks.remove(entry.getKey());
        }
    }

    private static void drain(Set<Map.Entry<Integer, Double>> entries, List<Integer> chunks) {
        entries.forEach(entry -> consume(entry, chunks));
    }

    private static void consume(Map.Entry<Integer, Double> entry, List<Integer> chunks) {
        double amount = entry.getValue();
        while (amount > 1f) {
            int size = Math.min((int) amount, 64);
            chunks.add(size);
            amount -= size;
            entry.setValue(amount);
        }
    }
}
