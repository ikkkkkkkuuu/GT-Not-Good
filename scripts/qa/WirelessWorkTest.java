package com.xyp.gtnotgood.common.wireless;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.google.common.io.ByteStreams;

public class WirelessWorkTest {

    public static class MissingSignatureType {
    }

    public static class SignatureFixture {

        public void process() {}

        public MissingSignatureType unrelated(MissingSignatureType value) {
            return value;
        }
    }

    @Test
    public void absentOptionalMethodSignatureDoesNotRejectEntryPoint() throws Exception {
        String fixtureName = SignatureFixture.class.getName();
        String missingName = MissingSignatureType.class.getName();
        ClassLoader isolated = new ClassLoader(getClass().getClassLoader()) {

            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals(missingName)) throw new ClassNotFoundException(name);
                if (!name.equals(fixtureName)) return super.loadClass(name, resolve);
                Class<?> loaded = findLoadedClass(name);
                if (loaded != null) return loaded;
                try (InputStream bytes = getResourceAsStream(name.replace('.', '/') + ".class")) {
                    byte[] data = ByteStreams.toByteArray(bytes);
                    return defineClass(name, data, 0, data.length);
                } catch (Exception failure) {
                    throw new ClassNotFoundException(name, failure);
                }
            }
        };
        Class<?> fixture = isolated.loadClass(fixtureName);
        try {
            fixture.getDeclaredMethod("process");
            fail("Fixture must reproduce optional signature linkage failure");
        } catch (NoClassDefFoundError expected) {}
        assertEquals(fixtureName, WirelessCompatibility.declaring(fixture, "process"));
        assertEquals("", WirelessCompatibility.declaring(fixture, "absentEntry"));
    }

    @Test
    public void exactCostAcrossAllDurations() {
        for (int duration : new int[] { 1, 2, 7, 128, 1000 }) {
            for (BigInteger cost : new BigInteger[] { BigInteger.ZERO, BigInteger.ONE, BigInteger.valueOf(131),
                BigInteger.TEN.pow(45)
                    .add(BigInteger.valueOf(71)) }) {
                WirelessWork work = work(cost, duration);
                AtomicReference<BigInteger> paid = new AtomicReference<>(BigInteger.ZERO);
                for (int tick = 0; tick < duration; tick++) {
                    assertTrue(work.tick(eu -> {
                        paid.set(
                            paid.get()
                                .add(eu));
                        return true;
                    }));
                }
                assertTrue(work.finished());
                assertEquals(cost, paid.get());
                assertEquals(BigInteger.ZERO, work.remainingCost());
                assertFalse(work.tick(eu -> {
                    fail("Completed task debited again");
                    return true;
                }));
            }
        }
    }

    @Test
    public void powerLossAndReloadPreservePaidProgress() {
        WirelessWork work = work(BigInteger.valueOf(1001), 7);
        BigInteger paid = BigInteger.ZERO;
        for (int i = 0; i < 3; i++) {
            paid = paid.add(work.nextCost());
            assertTrue(work.tick(eu -> true));
        }
        BigInteger next = work.nextCost();
        for (int i = 0; i < 20; i++) assertFalse(work.tick(eu -> false));
        assertEquals(3, work.progress());
        assertEquals(next, work.nextCost());
        WirelessWork loaded = WirelessWork.load(work.save());
        assertEquals(work.owner, loaded.owner);
        assertEquals(3, loaded.progress());
        assertEquals(next, loaded.nextCost());
        assertEquals(work.totalEU, paid.add(loaded.remainingCost()));
    }

    @Test
    public void independentRecipesCanFinishAtDifferentTimes() {
        WirelessWork first = work(BigInteger.valueOf(40), 4);
        WirelessWork second = work(BigInteger.valueOf(21), 3);
        first.tick(eu -> true);
        first.tick(eu -> true);
        second.tick(eu -> true);
        first.tick(eu -> false);
        second.tick(eu -> true);
        second.tick(eu -> true);
        assertTrue(second.finished());
        assertFalse(first.finished());
        assertEquals(2, first.progress());
    }

    private static WirelessWork work(BigInteger eu, int duration) {
        return new WirelessWork(UUID.randomUUID(), "test", eu, duration, 1, null, null);
    }

    @Test
    public void bigParallelSurvivesSaveAndExactAccounting() {
        BigInteger parallels = BigInteger.TEN.pow(60)
            .add(BigInteger.valueOf(123));
        WirelessWork task = new WirelessWork(
            UUID.randomUUID(),
            "big",
            parallels.multiply(BigInteger.valueOf(320)),
            128,
            parallels,
            new WirelessOutputs(null, null));
        task.tick(eu -> true);
        WirelessWork loaded = WirelessWork.load(task.save());
        assertEquals(parallels, loaded.parallels);
        assertEquals(task.remainingCost(), loaded.remainingCost());
        assertEquals(1, loaded.progress());
    }

    @Test
    public void readsPreviousIntParallelSave() {
        var tag = work(BigInteger.valueOf(512), 128).save();
        tag.setInteger("parallels", Integer.MAX_VALUE);
        tag.removeTag("outputs");
        assertEquals(BigInteger.valueOf(Integer.MAX_VALUE), WirelessWork.load(tag).parallels);
    }
}
