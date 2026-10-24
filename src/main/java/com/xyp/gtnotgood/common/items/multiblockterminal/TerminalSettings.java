package com.xyp.gtnotgood.common.items.multiblockterminal;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;

import gregtech.common.misc.GTStructureChannels;

public final class TerminalSettings {

    public static final int MAX_COUNT = 648;
    private static final String TAG = "MultiblockTerminal";

    private TerminalSettings() {}

    public static boolean dismantle(ItemStack stack) {
        return data(stack).getBoolean("Dismantle");
    }

    public static void setDismantle(ItemStack stack, boolean value) {
        writableData(stack).setBoolean("Dismantle", value);
    }

    public static boolean fullBuild(ItemStack stack) {
        NBTTagCompound data = data(stack);
        return !data.hasKey("FullBuild") || data.getBoolean("FullBuild");
    }

    public static void setFullBuild(ItemStack stack, boolean value) {
        writableData(stack).setBoolean("FullBuild", value);
    }

    public static boolean noHatches(ItemStack stack) {
        NBTTagCompound data = data(stack);
        return !data.hasKey("NoHatches") || data.getBoolean("NoHatches");
    }

    public static void setNoHatches(ItemStack stack, boolean value) {
        writableData(stack).setBoolean("NoHatches", value);
    }

    public static boolean replace(ItemStack stack) {
        return data(stack).getBoolean("Replace");
    }

    public static void setReplace(ItemStack stack, boolean value) {
        writableData(stack).setBoolean("Replace", value);
    }

    public static boolean mirror(ItemStack stack) {
        return data(stack).getBoolean("Mirror");
    }

    public static void setMirror(ItemStack stack, boolean value) {
        writableData(stack).setBoolean("Mirror", value);
    }

    public static boolean ae(ItemStack stack) {
        return data(stack).getBoolean("AE");
    }

    public static void setAe(ItemStack stack, boolean value) {
        writableData(stack).setBoolean("AE", value);
    }

    public static int count(ItemStack stack) {
        return Math.max(1, Math.min(MAX_COUNT, data(stack).getInteger("Count")));
    }

    public static void setCount(ItemStack stack, int value) {
        writableData(stack).setInteger("Count", Math.max(1, Math.min(MAX_COUNT, value)));
    }

    /** StructureLib uses the trigger quantity as its master channel; the actual terminal remains one item. */
    public static ItemStack projectorTrigger(ItemStack stack) {
        ItemStack trigger = stack.copy();
        trigger.stackSize = count(stack);
        return trigger;
    }

    public static ItemStack trigger(ItemStack stack) {
        ItemStack trigger = projectorTrigger(stack);
        String hatchChannel = GTStructureChannels.HATCH.get();
        if (noHatches(stack)) ChannelDataAccessor.unsetChannelData(trigger, hatchChannel);
        else if (!ChannelDataAccessor.hasSubChannel(trigger, hatchChannel))
            ChannelDataAccessor.setChannelData(trigger, hatchChannel, 1);
        return trigger;
    }

    private static NBTTagCompound data(ItemStack stack) {
        return stack != null && stack.hasTagCompound() ? stack.getTagCompound().getCompoundTag(TAG)
            : new NBTTagCompound();
    }

    private static NBTTagCompound writableData(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey(TAG, 10)) tag.setTag(TAG, new NBTTagCompound());
        return tag.getCompoundTag(TAG);
    }
}
