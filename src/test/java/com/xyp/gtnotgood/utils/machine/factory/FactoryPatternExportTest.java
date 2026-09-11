package com.xyp.gtnotgood.utils.machine.factory;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;

/** Preview-to-pattern amounts and native-fluid writer selection, without bootstrapping a Minecraft world. */
public class FactoryPatternExportTest {

    @Test
    public void retainedSurplusAndChanceProductionCannotBecomeAePromises() {
        FactoryPreview.Snapshot snapshot = new FactoryPreview.Snapshot();
        assertNull(snapshot.exportIssue());
        FactoryPreview.Ingredient entry = new FactoryPreview.Ingredient();
        entry.internal = true;
        snapshot.outputs.add(entry);
        assertEquals(FactoryText.PATTERN_INTERNAL, snapshot.exportIssue());
        entry.internal = false;
        snapshot.hasChance = true;
        assertEquals(FactoryText.PATTERN_CHANCE, snapshot.exportIssue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void exportGuardRunsBeforeAeItemCreation() {
        FactoryPreview.Snapshot snapshot = new FactoryPreview.Snapshot();
        snapshot.hasChance = true;
        FactoryPatternExport.create(snapshot);
    }

    @Test
    public void integerPreviewQuantitiesStayUnchanged() {
        assertArrayEquals(
            new long[] { 9000000, 3500000, 300 },
            FactoryPatternExport.integerCounts(new double[] { 9000000, 3500000, 300 }));
    }

    @Test
    public void fractionalInputsAndOutputsScaleTogether() {
        assertArrayEquals(new long[] { 6, 1, 12 }, FactoryPatternExport.integerCounts(new double[] { 1.5, 0.25, 3 }));
    }

    @Test(expected = ArithmeticException.class)
    public void oversizedCountsRejectInsteadOfWrapping() {
        FactoryPatternExport.integerCounts(new double[] { 1e30 });
    }

    @Test
    public void eitherSideWithFluidRequiresUltimate() {
        IAEStack<?> item = stack(false), fluid = stack(true);
        assertFalse(FactoryPatternExport.hasFluids(Collections.singletonList(item), Collections.singletonList(item)));
        assertTrue(FactoryPatternExport.hasFluids(Collections.singletonList(fluid), Collections.singletonList(item)));
        assertTrue(FactoryPatternExport.hasFluids(Collections.singletonList(item), Collections.singletonList(fluid)));
    }

    @Test
    public void ordinaryAndUltimateUseTheirRespectiveNbtWriters() {
        IAEStack<?> item = stack(false), fluid = stack(true);
        NBTTagCompound ordinary = FactoryPatternExport
            .encode(Collections.singletonList(item), Collections.singletonList(item), false);
        assertFalse(ordinary.getBoolean("crafting"));
        assertFalse(
            ordinary.getTagList("in", 10)
                .getCompoundTagAt(0)
                .getBoolean("nativeWriter"));
        NBTTagCompound ultimate = FactoryPatternExport
            .encode(Arrays.asList(item, fluid), Collections.singletonList(fluid), true);
        assertEquals(
            2,
            ultimate.getTagList("in", 10)
                .tagCount());
        assertTrue(
            ultimate.getTagList("in", 10)
                .getCompoundTagAt(0)
                .getBoolean("nativeWriter"));
        assertTrue(
            ultimate.getTagList("out", 10)
                .getCompoundTagAt(0)
                .getBoolean("nativeWriter"));
    }

    private static IAEStack<?> stack(boolean fluid) {
        return (IAEStack<?>) Proxy.newProxyInstance(
            FactoryPatternExportTest.class.getClassLoader(),
            new Class<?>[] { fluid ? IAEFluidStack.class : IAEItemStack.class },
            (proxy, method, args) -> {
                if (method.getName()
                    .equals("getStackSize")) return 7L;
                if (method.getName()
                    .equals("writeToNBT")
                    || method.getName()
                        .equals("writeToNBTGeneric")) {
                    ((NBTTagCompound) args[0]).setBoolean(
                        "nativeWriter",
                        method.getName()
                            .equals("writeToNBTGeneric"));
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
    }
}
