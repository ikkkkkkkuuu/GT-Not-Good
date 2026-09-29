package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/** Exercises the transformed AE2 tree, including rule mutation and reload, in the opt-in client. */
final class InvTweaksOrderCacheClientChecks {

    static void run() throws Exception {
        Class<?> loader = Class.forName("appeng.util.InvTweakSortingModule$InvTweaksItemTreeLoader");
        Method load = loader.getDeclaredMethod("load", File.class);
        load.setAccessible(true);
        StringBuilder xml = new StringBuilder("<root><variants>");
        for (int i = 0; i < 4096; i++) {
            xml.append("<v")
                .append(i)
                .append(" id=\"qa:variants\" damage=\"")
                .append(i)
                .append("\"/>");
        }
        xml.append("</variants><range id=\"qa:range\" dmin=\"10\" dmax=\"20\"/>");
        xml.append("<other><wild id=\"qa:wild\"/></other></root>");
        File fixture = new File(System.getProperty("gtng.terminalScroll.qa.output"), "sort-tree.xml");
        Files.write(
            fixture.toPath(),
            xml.toString()
                .getBytes(StandardCharsets.UTF_8));
        Object tree = load.invoke(null, fixture);
        Method order = tree.getClass()
            .getDeclaredMethod("getItemOrder", String.class, int.class);
        order.setAccessible(true);
        Field cache = null;
        for (Field field : tree.getClass()
            .getDeclaredFields()) {
            if (field.getName()
                .contains("itemOrderCache")) cache = field;
        }
        require(cache != null, "cache mixin applied");
        cache.setAccessible(true);
        for (int damage = 0; damage < 4096; damage++) {
            require((int) order.invoke(tree, "qa:variants", damage) == damage, "exact variant order");
            require((int) order.invoke(tree, "qa:variants", damage) == damage, "cached variant order");
        }
        require(
            order.invoke(tree, "qa:range", 10)
                .equals(order.invoke(tree, "qa:range", 20)),
            "range order");
        require(
            order.invoke(tree, "qa:wild", 0)
                .equals(order.invoke(tree, "qa:wild", 32000)),
            "wildcard order");
        require((int) order.invoke(tree, null, 0) == 0, "null registry id");
        require(
            order.invoke(tree, "qa:unknown", 7)
                .equals(order.invoke(tree, "qa:unknown", 7)),
            "unknown stable");

        // Compare repeated late-variant lookups with the original scan forced by clearing only our cache.
        long uncached = measure(tree, order, cache, true);
        long cached = measure(tree, order, cache, false);
        System.out.println("TERMINAL_SORT_QA: 20000 lookups uncached=" + uncached + "ns cached=" + cached + "ns");

        Class<?> entryType = Class.forName("appeng.util.InvTweakSortingModule$InvTweaksItemTreeItem");
        Constructor<?> entry = entryType.getDeclaredConstructor(String.class, String.class, int.class, int.class);
        entry.setAccessible(true);
        Method add = tree.getClass()
            .getDeclaredMethod("addItem", String.class, entryType);
        add.setAccessible(true);
        add.invoke(tree, "root", entry.newInstance("added", "qa:added", 5, 123));
        require(((Map<?, ?>) cache.get(tree)).isEmpty(), "rule addition invalidates cache");
        require((int) order.invoke(tree, "qa:added", 5) == 123, "added rule visible");
        Method reset = tree.getClass()
            .getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(tree);
        require(((Map<?, ?>) cache.get(tree)).isEmpty(), "reset invalidates cache");
        Object reloaded = load.invoke(null, fixture);
        require((int) order.invoke(reloaded, "qa:variants", 4095) == 4095, "reload uses new tree");
        System.out.println("TERMINAL_SORT_QA: exact, range, wildcard, unknown, mutation and reload PASS");
    }

    private static long measure(Object tree, Method order, Field cache, boolean clear) throws Exception {
        long start = System.nanoTime();
        for (int i = 0; i < 20000; i++) {
            if (clear) ((Map<?, ?>) cache.get(tree)).clear();
            require((int) order.invoke(tree, "qa:variants", 4095) == 4095, "benchmark rank unchanged");
        }
        return System.nanoTime() - start;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
