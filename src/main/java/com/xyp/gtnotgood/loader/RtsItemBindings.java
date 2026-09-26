package com.xyp.gtnotgood.loader;

import com.rtsbuilding.rtsbuilding.common.RtsItems;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

/**
 * Binds the embedded RTS plugin items to the host item catalog using compile-time enum references.
 * Called only after the RTS items are registered; this class does not enable the optional RTS integration.
 */
public final class RtsItemBindings {

    private RtsItemBindings() {}

    /** Assigns each registered RTS handle without depending on enum naming conventions or reflection. */
    public static void bind() {
        GTNGItemList.RTS_RTS_CONTROL_CORE.set(RtsItems.RTS_CONTROL_CORE.get());
        GTNGItemList.RTS_REMOTE_CONTROL_PLUGIN.set(RtsItems.REMOTE_CONTROL_PLUGIN.get());
        GTNGItemList.RTS_STORAGE_INTEGRATION_PLUGIN.set(RtsItems.STORAGE_INTEGRATION_PLUGIN.get());
        GTNGItemList.RTS_CRAFT_TERMINAL_PLUGIN.set(RtsItems.CRAFT_TERMINAL_PLUGIN.get());
        GTNGItemList.RTS_CHAIN_BREAK_PLUGIN.set(RtsItems.CHAIN_BREAK_PLUGIN.get());
        GTNGItemList.RTS_AREA_DESTROY_PLUGIN.set(RtsItems.AREA_DESTROY_PLUGIN.get());
        GTNGItemList.RTS_BLUEPRINT_PLUGIN.set(RtsItems.BLUEPRINT_PLUGIN.get());
        GTNGItemList.RTS_RANGE_CULLING_PLUGIN.set(RtsItems.RANGE_CULLING_PLUGIN.get());
        GTNGItemList.RTS_FIELD_DEPLOYMENT_PLUGIN.set(RtsItems.FIELD_DEPLOYMENT_PLUGIN.get());
        GTNGItemList.RTS_RANGE_EXTENSION_I.set(RtsItems.RANGE_EXTENSION_I.get());
        GTNGItemList.RTS_RANGE_EXTENSION_II.set(RtsItems.RANGE_EXTENSION_II.get());
        GTNGItemList.RTS_RANGE_EXTENSION_III.set(RtsItems.RANGE_EXTENSION_III.get());
        GTNGItemList.RTS_RANGE_EXTENSION_MAX.set(RtsItems.RANGE_EXTENSION_MAX.get());
        GTNGItemList.RTS_HARVEST_TIER_STONE.set(RtsItems.HARVEST_TIER_STONE.get());
        GTNGItemList.RTS_HARVEST_TIER_IRON.set(RtsItems.HARVEST_TIER_IRON.get());
        GTNGItemList.RTS_HARVEST_TIER_DIAMOND.set(RtsItems.HARVEST_TIER_DIAMOND.get());
        GTNGItemList.RTS_HARVEST_TIER_UNLIMITED.set(RtsItems.HARVEST_TIER_UNLIMITED.get());
    }
}
