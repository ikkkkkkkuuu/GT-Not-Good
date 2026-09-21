package com.xyp.gtnotgood.common.flux;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Resets portable connector settings without discarding owned cargo. */
public final class FluxDropData {

    private FluxDropData() {}

    /**
     * Clears connector configuration on drops and older inventory stacks so configured connectors can stack again.
     * Real undelivered cargo, unknown tags and item display names are preserved.
     *
     * @param stack     portable connector stack to normalize in place
     * @param logistics whether this stack is a logistics plug rather than an energy connector
     */
    public static void normalize(ItemStack stack, boolean logistics) {
        if (stack == null || !stack.hasTagCompound()) return;
        NBTTagCompound original = stack.getTagCompound();
        NBTTagCompound tag = (NBTTagCompound) original.copy();
        if (logistics) {
            tag.removeTag("logisticsChannel");
            tag.removeTag("logisticsImport");
            for (int i = 0; i < TileFluxLogistics.SLOTS; i++) {
                tag.removeTag("item" + i);
                tag.removeTag("fluid" + i);
                tag.removeTag("items" + i);
                tag.removeTag("fluids" + i);
            }
        } else {
            tag.removeTag("fluxVoltage");
            tag.removeTag("fluxAmperage");
            tag.removeTag("fluxEnabled");
            tag.removeTag("fluxConnected");
            tag.removeTag("fluxRedstone");
            tag.removeTag("fluxPriority");
            tag.removeTag("fluxSurge");
            tag.removeTag("fluxName");
            // These retired settings no longer affect transmission.
            tag.removeTag("fluxLimit");
            tag.removeTag("fluxDisableLimit");
        }
        if (tag.hasNoTags()) stack.setTagCompound(null);
        else if (!tag.equals(original)) stack.setTagCompound(tag);
    }
}
