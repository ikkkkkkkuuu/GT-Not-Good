package com.xyp.gtnotgood.common.machines.hatch.me;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.Test;

import appeng.api.storage.data.IAEStack;
import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;

/** Exercises native cache capacity and staged transactions without bootstrapping Forge's item/fluid registries. */
public class PatternMEOutputTest {

    @Test
    public void transactionsAreIsolatedAndCommitLongAmountsOnce() throws Exception {
        int[] dirty = { 0 };
        PatternMEOutput output = new PatternMEOutput(null, () -> dirty[0]++);
        Field itemField = PatternMEOutput.class.getDeclaredField("items");
        itemField.setAccessible(true);
        MTEHatchOutputMEBase<?> items = (MTEHatchOutputMEBase<?>) itemField.get(output);
        Field fluidField = PatternMEOutput.class.getDeclaredField("fluids");
        fluidField.setAccessible(true);
        MTEHatchOutputMEBase<?> fluids = (MTEHatchOutputMEBase<?>) fluidField.get(output);
        assertEquals(Long.MAX_VALUE, items.getCacheCapacity());
        assertEquals(Long.MAX_VALUE, fluids.getCacheCapacity());

        var transaction = output.itemOutput.createTransaction();
        Field pendingField = transaction.getClass()
            .getDeclaredField("pending");
        pendingField.setAccessible(true);
        Object pending = pendingField.get(transaction);
        Method insert = pending.getClass()
            .getDeclaredMethod("insert", IAEStack.class);
        insert.setAccessible(true);
        assertEquals(true, insert.invoke(pending, stack(Integer.MAX_VALUE)));
        assertEquals(true, insert.invoke(pending, stack(Integer.MAX_VALUE)));
        assertEquals(0, items.getCachedAmount());
        assertEquals(0, dirty[0]);
        transaction.commit();
        assertEquals(2L * Integer.MAX_VALUE, items.getCachedAmount());
        assertEquals(0, fluids.getCachedAmount());
        assertEquals(1, dirty[0]);
        try {
            transaction.commit();
            fail("A transaction must not commit twice");
        } catch (IllegalStateException expected) {
            assertEquals(2L * Integer.MAX_VALUE, items.getCachedAmount());
        }
    }

    /** Minimal immutable-key AE stack double; no game registry is needed for cache arithmetic. */
    private static IAEStack stack(long initial) {
        long[] amount = { initial };
        return (IAEStack) Proxy.newProxyInstance(
            IAEStack.class.getClassLoader(),
            new Class<?>[] { IAEStack.class },
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "copy":
                        return stack(amount[0]);
                    case "getStackSize":
                        return amount[0];
                    case "setStackSize":
                        amount[0] = (Long) args[0];
                        return proxy;
                    case "hashCode":
                        return 1;
                    case "equals":
                        return args[0] instanceof IAEStack;
                    default:
                        throw new AssertionError("Unexpected stack method: " + method.getName());
                }
            });
    }
}
