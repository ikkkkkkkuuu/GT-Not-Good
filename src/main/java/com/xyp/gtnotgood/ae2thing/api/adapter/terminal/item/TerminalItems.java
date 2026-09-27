package com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import appeng.util.Platform;
import lombok.Getter;
import lombok.Setter;

public class TerminalItems {

    private ItemStack raw;
    private ItemStack target;
    @Getter
    @Setter
    private String displayName;
    @Getter
    @Setter
    private NBTTagCompound data;

    public TerminalItems(ItemStack raw, ItemStack target) {
        this(raw, target, Platform.getItemDisplayName(target), new NBTTagCompound());
    }

    public TerminalItems(ItemStack raw, ItemStack target, NBTTagCompound data) {
        this(raw, target, Platform.getItemDisplayName(target), data);
    }

    public TerminalItems(ItemStack raw, ItemStack target, String displayName, NBTTagCompound data) {
        this.raw = raw;
        this.target = target;
        this.displayName = displayName;
        this.data = data;
    }

    public ItemStack getRawItem() {
        return raw;
    }

    public ItemStack getTargetItem() {
        return target;
    }

    public void setRawItem(ItemStack raw) {
        this.raw = raw;
    }

    public void setTargetItem(ItemStack target) {
        this.target = target;
    }

    public void writeNBT(NBTTagCompound tag) {
        NBTTagCompound raw = new NBTTagCompound();
        NBTTagCompound target = new NBTTagCompound();
        getRawItem().writeToNBT(raw);
        getTargetItem().writeToNBT(target);
        tag.setTag("#0", raw);
        tag.setTag("#1", target);
        tag.setString("displayName", displayName);
        tag.setTag("data", data);
    }

    public static TerminalItems readFromNBT(NBTTagCompound tag) {
        ItemStack raw = ItemStack.loadItemStackFromNBT((NBTTagCompound) tag.getTag("#0"));
        ItemStack target = ItemStack.loadItemStackFromNBT((NBTTagCompound) tag.getTag("#1"));
        NBTTagCompound data = tag.getCompoundTag("data");
        return new TerminalItems(raw, target, tag.getString("displayName"), data);
    }
}
