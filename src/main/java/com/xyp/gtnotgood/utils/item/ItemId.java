package com.xyp.gtnotgood.utils.item;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Replacement for GTUtility.ItemId (@AutoValue-generated class missing from jar).
 */
public final class ItemId {

    private final Item item;
    private final int meta;
    @Nullable
    private final NBTTagCompound nbt;
    @Nullable
    private final Integer stackSize;

    private ItemId(Item item, int meta, @Nullable NBTTagCompound nbt, @Nullable Integer stackSize) {
        this.item = item;
        this.meta = meta;
        this.nbt = nbt;
        this.stackSize = stackSize;
    }

    public static ItemId createNoCopy(@Nonnull ItemStack stack) {
        return new ItemId(stack.getItem(), stack.getItemDamage(), stack.getTagCompound(), null);
    }

    public static ItemId createNoCopyWithStackSize(@Nonnull ItemStack stack) {
        return new ItemId(stack.getItem(), stack.getItemDamage(), stack.getTagCompound(), stack.stackSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemId other)) return false;
        return meta == other.meta && item == other.item && Objects.equals(nbt, other.nbt);
    }

    @Override
    public int hashCode() {
        int result = item.hashCode();
        result = 31 * result + meta;
        result = 31 * result + (nbt != null ? nbt.hashCode() : 0);
        return result;
    }
}
