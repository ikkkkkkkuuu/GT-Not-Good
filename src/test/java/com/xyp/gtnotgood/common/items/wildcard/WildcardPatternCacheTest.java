package com.xyp.gtnotgood.common.items.wildcard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

/** Regression coverage for mutable configurations, empty results and owner-local cache lifetimes. */
public class WildcardPatternCacheTest {

    @Test
    public void repeatedQueriesReuseZeroCountEvenAfterGeneratorWritesNbt() {
        WildcardPatternCache<Integer> cache = new WildcardPatternCache<>();
        ItemStack stack = pattern();
        AtomicInteger builds = new AtomicInteger();
        for (int i = 0; i < 1000; i++) {
            assertEquals(Integer.valueOf(0), cache.get(stack, null, () -> {
                builds.incrementAndGet();
                stack.getTagCompound()
                    .setInteger("WPExpandedCount", 0);
                return 0;
            }));
        }
        assertEquals(1, builds.get());
    }

    @Test
    public void nestedAmountAndFilterEditsInvalidateDetachedSnapshot() {
        WildcardPatternCache<Integer> cache = new WildcardPatternCache<>();
        ItemStack stack = pattern();
        AtomicInteger builds = new AtomicInteger();
        assertEquals(Integer.valueOf(1), cache.get(stack, null, builds::incrementAndGet));
        stack.getTagCompound()
            .getTagList("WPInputComponents", 10)
            .getCompoundTagAt(0)
            .setLong("Amount", 2000);
        assertEquals(Integer.valueOf(2), cache.get(stack, null, builds::incrementAndGet));
        stack.getTagCompound()
            .setString("WPFilterComponents", "Copper");
        assertEquals(Integer.valueOf(3), cache.get(stack, null, builds::incrementAndGet));
        stack.getTagCompound()
            .removeTag("WPFilterComponents");
        assertEquals(Integer.valueOf(4), cache.get(stack, null, builds::incrementAndGet));
        assertEquals(Integer.valueOf(4), cache.get(stack.copy(), null, builds::incrementAndGet));
    }

    @Test
    public void itemMetadataAndWorldChangesInvalidateButStackQuantityDoesNot() {
        WildcardPatternCache<Integer> cache = new WildcardPatternCache<>();
        AtomicInteger builds = new AtomicInteger();
        ItemStack stack = pattern();
        Object world = new Object();
        assertEquals(Integer.valueOf(1), cache.get(stack, world, builds::incrementAndGet));
        stack.stackSize = 64;
        assertEquals(Integer.valueOf(1), cache.get(stack, world, builds::incrementAndGet));
        stack.setItemDamage(1);
        assertEquals(Integer.valueOf(2), cache.get(stack, world, builds::incrementAndGet));
        Object nextWorld = new Object();
        assertEquals(Integer.valueOf(3), cache.get(stack, nextWorld, builds::incrementAndGet));
        ItemStack replacement = new ItemStack(new Item(), 64, 1);
        replacement.setTagCompound(
            (NBTTagCompound) stack.getTagCompound()
                .copy());
        assertEquals(Integer.valueOf(4), cache.get(replacement, nextWorld, builds::incrementAndGet));
    }

    @Test
    public void identicalSlotsOwnSeparateMutableResults() {
        WildcardPatternCache<List<Integer>> first = new WildcardPatternCache<>();
        WildcardPatternCache<List<Integer>> second = new WildcardPatternCache<>();
        ItemStack stack = pattern();
        List<Integer> firstResult = first.get(stack, null, ArrayList::new);
        List<Integer> secondResult = second.get(stack, null, ArrayList::new);
        assertNotSame(firstResult, secondResult);
        firstResult.add(7);
        assertEquals(0, secondResult.size());
        assertSame(firstResult, first.get(stack.copy(), null, ArrayList::new));
    }

    @Test
    public void failedBuildIsRetriedAndNullTransitionsInvalidate() {
        WildcardPatternCache<Integer> cache = new WildcardPatternCache<>();
        ItemStack stack = pattern();
        assertEquals(Integer.valueOf(1), cache.get(stack, null, () -> 1));
        stack.setTagCompound(null);
        try {
            cache.get(stack, null, () -> { throw new IllegalStateException("failed build"); });
            fail("Expected the build failure");
        } catch (IllegalStateException expected) {}
        assertEquals(Integer.valueOf(2), cache.get(stack, null, () -> 2));
        assertEquals(Integer.valueOf(3), cache.get(null, null, () -> 3));
        assertEquals(Integer.valueOf(3), cache.get(null, null, () -> 4));
        assertEquals(Integer.valueOf(5), cache.get(stack, null, () -> 5));
    }

    private static ItemStack pattern() {
        ItemStack stack = new ItemStack(new Item());
        NBTTagCompound config = new NBTTagCompound();
        NBTTagCompound input = new NBTTagCompound();
        input.setLong("Amount", 1000);
        NBTTagList inputs = new NBTTagList();
        inputs.appendTag(input);
        config.setTag("WPInputComponents", inputs);
        stack.setTagCompound(config);
        return stack;
    }
}
