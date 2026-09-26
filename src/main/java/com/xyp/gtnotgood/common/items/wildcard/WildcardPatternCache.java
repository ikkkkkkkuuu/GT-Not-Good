package com.xyp.gtnotgood.common.items.wildcard;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

/**
 * One owner-local result and a detached NBT snapshot. Hits compare tags without serializing them.
 * Mutable pattern details must never be shared across slots: AE2 assigns each slot its own priority.
 * Access is confined to the owning client or server thread.
 *
 * @param <T> cached result type, including empty results
 */
public final class WildcardPatternCache<T> {

    private ItemStack snapshot;
    private Object context;
    private T result;
    private boolean computed;

    /**
     * Reuses a result only while item, metadata, NBT and context identity remain unchanged.
     * Captures the snapshot after building because generation may initialize NBT or write a count.
     * Failed builds leave the previous entry untouched and are retried on the next call.
     *
     * @param stack   pattern configuration; stack quantity does not affect expansion
     * @param context owner context (the world for pattern details, null for tooltip counts)
     * @param build   computation to run on a miss
     * @return the cached or newly computed result
     */
    public T get(ItemStack stack, Object context, Supplier<T> build) {
        if (!computed || this.context != context || !matches(stack)) {
            T next = build.get();
            snapshot = stack == null ? null : stack.copy();
            this.context = context;
            result = next;
            computed = true;
        }
        return result;
    }

    private boolean matches(ItemStack stack) {
        if (snapshot == null || stack == null) return snapshot == stack;
        return snapshot.getItem() == stack.getItem() && snapshot.getItemDamage() == stack.getItemDamage()
            && ItemStack.areItemStackTagsEqual(snapshot, stack);
    }
}
