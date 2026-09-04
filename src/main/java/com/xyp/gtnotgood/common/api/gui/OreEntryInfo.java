package com.xyp.gtnotgood.common.api.gui;

import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * Immutable display row for a void-miner ore entry.
 * <p>
 * The server builds this object from GalacticGreg drop-map data and ModularUI2 syncs it to the client for the
 * configuration browser. The item stack represents the ore, {@link #weight} is the merged cross-dimension weight, and
 * {@link #dimAbbrs} records the dimension abbreviations that contributed to the entry.
 */
public class OreEntryInfo {

    public final ItemStack ore;
    public float weight;
    public final List<String> dimAbbrs;
    public final boolean filtered;
    public final boolean aimed;

    public OreEntryInfo(ItemStack ore, float weight, List<String> dimAbbrs, boolean filtered, boolean aimed) {
        this.ore = ore;
        this.weight = weight;
        this.dimAbbrs = dimAbbrs;
        this.filtered = filtered;
        this.aimed = aimed;
    }
}
