package com.xyp.gtnotgood.common.parts.mestock;

import java.util.Arrays;

import net.minecraft.nbt.NBTTagCompound;

import appeng.api.storage.data.IAEStack;
import appeng.util.Platform;

/** Ghost identities and long quantities; zero is a valid reserve, never a real inventory stack. */
public final class StockConfig {

    private final IAEStack<?>[] keys;
    private final long[] amounts;
    private final long[] batches;
    private final boolean[] enabled;
    private final Runnable changed;
    private long revision;

    public StockConfig(int size, Runnable changed) {
        keys = new IAEStack<?>[size];
        amounts = new long[size];
        batches = new long[size];
        enabled = new boolean[size];
        Arrays.fill(enabled, true);
        this.changed = changed;
    }

    public int size() {
        return keys.length;
    }

    public IAEStack<?> key(int slot) {
        return keys[slot];
    }

    public long amount(int slot) {
        return amounts[slot];
    }

    public long batch(int slot) {
        return batches[slot];
    }

    public boolean enabled(int slot) {
        return enabled[slot];
    }

    public long revision() {
        return revision;
    }

    public void setKey(int slot, IAEStack<?> key) {
        if (slot < 0 || slot >= size()) return;
        if (key != null && !(key.isItem() || key.isFluid())) return;
        if (keys[slot] != null && key != null && keys[slot].isSameType(key)) return;
        keys[slot] = key == null ? null : key.copy().setStackSize(1);
        amounts[slot] = key == null ? 0 : Math.max(1, key.getStackSize());
        batches[slot] = 0;
        if (key != null) {
            for (int i = 0; i < size(); i++) {
                if (i != slot && keys[i] != null && keys[i].isSameType(key)) {
                    keys[i] = null;
                    amounts[i] = 0;
                }
            }
        }
        touch();
    }

    public void setAmount(int slot, long value) {
        value = Math.max(0, value);
        if (amounts[slot] == value) return;
        amounts[slot] = value;
        touch();
    }

    public void setBatch(int slot, long value) {
        value = Math.max(0, value);
        if (batches[slot] == value) return;
        batches[slot] = value;
        touch();
    }

    /** Applies the two fields of an upstream request submission with one scheduler notification. */
    public void setAmounts(int slot, long amount, long batch) {
        if (slot < 0 || slot >= size()) return;
        amount = Math.max(0, amount);
        batch = Math.max(0, batch);
        if (amounts[slot] == amount && batches[slot] == batch) return;
        amounts[slot] = amount;
        batches[slot] = batch;
        touch();
    }

    public void setEnabled(int slot, boolean value) {
        if (enabled[slot] == value) return;
        enabled[slot] = value;
        touch();
    }

    private void touch() {
        revision++;
        changed.run();
    }

    public void write(NBTTagCompound tag) {
        for (int i = 0; i < size(); i++) {
            NBTTagCompound row = new NBTTagCompound();
            if (keys[i] != null) Platform.writeStackNBT(keys[i], row, true);
            row.setLong("target", amounts[i]);
            row.setLong("batch", batches[i]);
            row.setBoolean("enabled", enabled[i]);
            tag.setTag("row" + i, row);
        }
    }

    public void read(NBTTagCompound tag) {
        Arrays.fill(keys, null);
        for (int i = 0; i < size(); i++) {
            NBTTagCompound row = tag.getCompoundTag("row" + i);
            IAEStack<?> key = Platform.readStackNBT(row, false);
            if (key != null && (key.isItem() || key.isFluid())) {
                keys[i] = key.copy().setStackSize(1);
                for (int previous = 0; previous < i; previous++) {
                    if (keys[previous] != null && keys[previous].isSameType(key)) keys[i] = null;
                }
            }
            amounts[i] = Math.max(0, row.getLong("target"));
            batches[i] = Math.max(0, row.getLong("batch"));
            enabled[i] = !row.hasKey("enabled") || row.getBoolean("enabled");
        }
        revision++;
    }
}
