package com.rtsbuilding.rtsbuilding.common;

import net.minecraft.creativetab.CreativeTabs;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;

/** Maps the upstream item catalog onto the host creative tab. */
public final class RtsCreativeTabs {
    public static final CreativeTabs RTSBUILDING_TAB = GTNGCreativeTabs.GTNGItem;
    /** Forces tab initialization at the upstream lifecycle boundary. */
    public static void register() {}
    private RtsCreativeTabs() {}
}