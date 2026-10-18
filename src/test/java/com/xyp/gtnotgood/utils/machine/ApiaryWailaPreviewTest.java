package com.xyp.gtnotgood.utils.machine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class ApiaryWailaPreviewTest {

    @Test
    public void nullAndEmptyOutputsKeepTheirNativeMeaning() {
        ApiaryWailaPreview<Object> missing = ApiaryWailaPreview.of(null);
        assertNull(missing.outputs());
        assertEquals(0, missing.totalCount());
        Object[] empty = new Object[0];
        ApiaryWailaPreview<Object> present = ApiaryWailaPreview.of(empty);
        assertSame(empty, present.outputs());
        assertEquals(0, present.totalCount());
        ApiaryWailaPreview<Object> nullEntries = ApiaryWailaPreview.of(new Object[256]);
        assertEquals(0, nullEntries.outputs().length);
        assertEquals(0, nullEntries.totalCount());
    }

    @Test
    public void shortOutputArraysAreReusedWithoutChangingEntryOrder() {
        Object first = new Object();
        Object second = new Object();
        Object[] outputs = { null, first, second };
        ApiaryWailaPreview<Object> preview = ApiaryWailaPreview.of(outputs);
        assertSame(outputs, preview.outputs());
        assertEquals(2, preview.totalCount());
    }

    @Test
    public void previewSkipsNullEntriesAndPreservesTheFullOutputCount() {
        Integer[] outputs = { null, 11, null, 22, 33, null, 44, 55 };
        Integer[] original = outputs.clone();
        ApiaryWailaPreview<Integer> preview = ApiaryWailaPreview.of(outputs);
        assertArrayEquals(new Integer[] { 11, 22, 33 }, preview.outputs());
        assertEquals(5, preview.totalCount());
        assertArrayEquals(original, outputs);
    }

    @Test
    public void trailingNullEntriesCannotCreateAdditionalVisibleRows() {
        Object output = new Object();
        Object[] outputs = { null, output, null, null, null };
        ApiaryWailaPreview<Object> preview = ApiaryWailaPreview.of(outputs);
        assertArrayEquals(new Object[] { output }, preview.outputs());
        assertSame(output, preview.outputs()[0]);
        assertEquals(1, preview.totalCount());
    }

    @Test
    public void largeOutputTotalsAndStackAmountsRemainIntegers() {
        int[] largeStack = { Integer.MAX_VALUE };
        int[][] outputs = new int[1024][];
        for (int i = 0; i < outputs.length; i++) outputs[i] = largeStack;
        ApiaryWailaPreview<int[]> preview = ApiaryWailaPreview.of(outputs);
        assertEquals(3, preview.outputs().length);
        assertEquals(1024, preview.totalCount());
        assertSame(largeStack, preview.outputs()[0]);
        assertEquals(Integer.MAX_VALUE, preview.outputs()[0][0]);
    }
}
