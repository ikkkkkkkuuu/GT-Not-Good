package com.xyp.gtnotgood.common.network;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.cleanroommc.modularui.utils.fluid.FluidInteractions;

/** Per-channel device-face rule. Ghost samples support whitelist/blacklist and optional matching constraints. */
public final class NetworkRule {

    public int mode;
    public int interval = 20;
    public int facing = -1;
    public int priority;
    public int rate = 64;
    public boolean blacklist;
    public boolean matchMeta = true;
    public boolean matchNbt = true;
    public boolean matchOre;
    public final ItemStack[] filters = new ItemStack[18];
    public final FluidStack[] fluidFilters = new FluidStack[18];

    /** Reads native fluid samples, with compatibility for older saves that stored filled containers. */
    public FluidStack fluidFilter(int slot) {
        if (fluidFilters[slot] != null) return fluidFilters[slot];
        ItemStack sample = filters[slot];
        if (sample == null) return null;
        ItemStack copy = sample.copy();
        copy.stackSize = 1;
        return FluidInteractions.getFluidForItem(copy);
    }

    /** Resolves the configured device access face; -1 follows the connector's physical contact face. */
    public net.minecraftforge.common.util.ForgeDirection face(NetworkTopology.Endpoint endpoint) {
        return facing >= 0 && facing < 6 ? net.minecraftforge.common.util.ForgeDirection.getOrientation(facing)
            : endpoint.direction.getOpposite();
    }

    /** Each endpoint schedules independently, including when the opposite end uses a different interval. */
    public boolean due(long tick) {
        return tick % Math.max(1, interval) == 0;
    }

    public boolean accepts(ItemStack stack) {
        if (stack == null) return false;
        boolean any = false;
        for (ItemStack sample : filters) {
            if (sample == null) continue;
            any = true;
            boolean itemMatches = sample.getItem() == stack.getItem();
            if (matchOre) {
                for (int left : OreDictionary.getOreIDs(sample)) {
                    for (int right : OreDictionary.getOreIDs(stack)) if (left == right) itemMatches = true;
                }
            }
            if (itemMatches && (!matchMeta || sample.getItemDamage() == stack.getItemDamage())
                && (!matchNbt || ItemStack.areItemStackTagsEqual(sample, stack))) return !blacklist;
        }
        return !any || blacklist;
    }

    public boolean accepts(FluidStack stack) {
        if (stack == null) return false;
        boolean any = false;
        for (int i = 0; i < fluidFilters.length; i++) {
            FluidStack fluid = fluidFilter(i);
            if (fluid == null) continue;
            any = true;
            if (fluid.getFluid() == stack.getFluid() && (!matchNbt || fluid.isFluidEqual(stack))) return !blacklist;
        }
        return !any || blacklist;
    }

    public void read(NBTTagCompound tag) {
        facing = tag.hasKey("facing") ? Math.max(-1, Math.min(5, tag.getInteger("facing"))) : -1;
        interval = tag.hasKey("interval") ? Math.max(1, Math.min(1200, tag.getInteger("interval"))) : 20;
        mode = Math.max(0, Math.min(2, tag.getInteger("mode")));
        priority = Math.max(-99, Math.min(99, tag.getInteger("priority")));
        rate = Math.max(1, tag.getInteger("rate"));
        blacklist = tag.getBoolean("blacklist");
        matchMeta = !tag.hasKey("matchMeta") || tag.getBoolean("matchMeta");
        matchNbt = !tag.hasKey("matchNbt") || tag.getBoolean("matchNbt");
        matchOre = tag.getBoolean("matchOre");
        for (int i = 0; i < filters.length; i++) {
            String key = i == 0 && tag.hasKey("filter") ? "filter" : "filter" + i;
            filters[i] = tag.hasKey(key) ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag(key)) : null;
            if (filters[i] != null) filters[i].stackSize = 1;
            fluidFilters[i] = tag.hasKey("fluidFilter" + i)
                ? FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("fluidFilter" + i))
                : null;
            if (fluidFilters[i] != null) fluidFilters[i].amount = 1;
        }
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("mode", mode);
        tag.setInteger("interval", interval);
        tag.setInteger("facing", facing);
        tag.setInteger("priority", priority);
        tag.setInteger("rate", rate);
        tag.setBoolean("blacklist", blacklist);
        tag.setBoolean("matchMeta", matchMeta);
        tag.setBoolean("matchNbt", matchNbt);
        tag.setBoolean("matchOre", matchOre);
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] != null) tag.setTag("filter" + i, filters[i].writeToNBT(new NBTTagCompound()));
            if (fluidFilters[i] != null)
                tag.setTag("fluidFilter" + i, fluidFilters[i].writeToNBT(new NBTTagCompound()));
        }
        return tag;
    }
}
