// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.ae2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.Item;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.AEKeyType;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.util.item.AEItemStackType;

public final class StackKeyFactory {

    private static final Map<IAEStackType<?>, AEKeyType> TYPES = new ConcurrentHashMap<>();

    private final Map<IAEStack<?>, StackKey> keys = new ConcurrentHashMap<>();
    private final Map<ItemMeta, ItemMeta> itemMetas = new ConcurrentHashMap<>();

    @Nullable
    public StackKey of(@Nullable IAEStack<?> stack) {
        if (stack == null || stack instanceof IAEItemStack itemStack && itemStack.getItem() == null) {
            return null;
        }
        final StackKey existing = this.keys.get(stack);
        if (existing != null) {
            return existing;
        }

        final IAEStack<?> copy = stack.copy();
        copy.reset();
        copy.setStackSize(1);
        final StackKey created = new StackKey(copy, typeOf(copy.getStackType()),
            StackKey.computePrimaryKey(copy, this));
        final StackKey raced = this.keys.putIfAbsent(copy, created);
        return raced == null ? created : raced;
    }

    public int size() {
        return this.keys.size();
    }

    Object itemMeta(Item item, int meta) {
        final ItemMeta probe = new ItemMeta(item, meta);
        final ItemMeta existing = this.itemMetas.putIfAbsent(probe, probe);
        return existing == null ? probe : existing;
    }

    private static AEKeyType typeOf(IAEStackType<?> stackType) {
        return TYPES.computeIfAbsent(stackType, t -> new AEKeyType(t.getId(), t.getAmountPerUnit(),
            t.getAmountPerByte(), t == AEItemStackType.ITEM_STACK_TYPE));
    }

    @Desugar
    private record ItemMeta(Item item, int meta) {}
}
