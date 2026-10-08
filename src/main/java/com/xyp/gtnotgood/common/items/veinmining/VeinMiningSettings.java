package com.xyp.gtnotgood.common.items.veinmining;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.config.Config;

/** Limits scroll requests to the current pickaxe without replacing its other item data. */
public final class VeinMiningSettings {

    private VeinMiningSettings() {}

    public static boolean apply(InventoryPlayer inventory, int slot, Setting setting, int value) {
        if (setting == null || slot < 0 || slot >= InventoryPlayer.getHotbarSize() || slot != inventory.currentItem)
            return false;
        ItemStack stack = inventory.getCurrentItem();
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof VeinMiningPickaxe)) return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(setting.key, setting.clamp(value));
        inventory.markDirty();
        return true;
    }

    public enum Setting {

        Range("range", 3, 1),
        Amount("amount", 327670, 10000);

        private final String key;
        private final int defaultValue;
        private final int step;

        Setting(String key, int defaultValue, int step) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.step = step;
        }

        public int read(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            return clamp(tag != null && tag.hasKey(key) ? tag.getInteger(key) : defaultValue);
        }

        public int scroll(ItemStack stack, int wheel) {
            return clamp((long) read(stack) + (long) Integer.signum(wheel) * step);
        }

        private int clamp(long value) {
            int maximum = this == Range ? Config.VeinMinerPickaxe.maxRange : Config.VeinMinerPickaxe.maxAmount;
            return (int) Math.max(0, Math.min(Math.max(0, maximum), value));
        }
    }
}
