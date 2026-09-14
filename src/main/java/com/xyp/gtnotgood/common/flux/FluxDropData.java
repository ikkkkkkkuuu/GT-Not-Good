package com.xyp.gtnotgood.common.flux;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Omits redundant defaults from portable nodes while preserving custom configuration and owned cargo. */
public final class FluxDropData {

    private FluxDropData() {}

    /** Also migrates older inventory stacks; unknown tags, display names and pending resources are untouched. */
    public static void normalize(ItemStack stack, boolean logistics) {
        if (stack == null || !stack.hasTagCompound()) return;
        NBTTagCompound original = stack.getTagCompound();
        NBTTagCompound tag = (NBTTagCompound) original.copy();
        if (logistics) {
            if (tag.getString("logisticsChannel")
                .isEmpty()) tag.removeTag("logisticsChannel");
            removeNumber(tag, "logisticsImport", 0);
            for (int i = 0; i < TileFluxLogistics.SLOTS; i++) {
                removeNumber(tag, "items" + i, 0);
                removeNumber(tag, "fluids" + i, 0);
            }
        } else {
            removeNumber(tag, "fluxVoltage", 32);
            removeNumber(tag, "fluxAmperage", 1);
            removeNumber(tag, "fluxEnabled", 1);
            removeNumber(tag, "fluxConnected", 1);
            removeNumber(tag, "fluxRedstone", 0);
            removeNumber(tag, "fluxPriority", 0);
            removeNumber(tag, "fluxSurge", 0);
            if (tag.getString("fluxName")
                .isEmpty()) tag.removeTag("fluxName");
            // These retired settings no longer affect transmission.
            tag.removeTag("fluxLimit");
            tag.removeTag("fluxDisableLimit");
        }
        if (tag.hasNoTags()) stack.setTagCompound(null);
        else if (!tag.equals(original)) stack.setTagCompound(tag);
    }

    private static void removeNumber(NBTTagCompound tag, String key, long defaultValue) {
        if (tag.hasKey(key, 99) && tag.getLong(key) == defaultValue) tag.removeTag(key);
    }
}
