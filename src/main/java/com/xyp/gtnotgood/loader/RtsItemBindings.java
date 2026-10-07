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
        GTNGItemList.RtsControlCore.set(RtsItems.RTS_CONTROL_CORE.get());
        GTNGItemList.RemoteControlCore.set(RtsItems.REMOTE_CONTROL_PLUGIN.get());
        GTNGItemList.StorageIntegrationPlugin.set(RtsItems.STORAGE_INTEGRATION_PLUGIN.get());
        GTNGItemList.CraftTerminalPlugin.set(RtsItems.CRAFT_TERMINAL_PLUGIN.get());
        GTNGItemList.ChainBreakPlugin.set(RtsItems.CHAIN_BREAK_PLUGIN.get());
        GTNGItemList.AreaDestroyPlugin.set(RtsItems.AREA_DESTROY_PLUGIN.get());
        GTNGItemList.BlueprintPlugin.set(RtsItems.BLUEPRINT_PLUGIN.get());
        GTNGItemList.RangeHidingPlugin.set(RtsItems.RANGE_CULLING_PLUGIN.get());
        GTNGItemList.FieldDeploymentPlugin.set(RtsItems.FIELD_DEPLOYMENT_PLUGIN.get());
        GTNGItemList.RangeExtensionI.set(RtsItems.RANGE_EXTENSION_I.get());
        GTNGItemList.RangeExtensionII.set(RtsItems.RANGE_EXTENSION_II.get());
        GTNGItemList.RangeExtensionIII.set(RtsItems.RANGE_EXTENSION_III.get());
        GTNGItemList.RangeExtensionMax.set(RtsItems.RANGE_EXTENSION_MAX.get());
        GTNGItemList.StoneHarvestPlugin.set(RtsItems.HARVEST_TIER_STONE.get());
        GTNGItemList.IronHarvestPlugin.set(RtsItems.HARVEST_TIER_IRON.get());
        GTNGItemList.DiamondHarvestPlugin.set(RtsItems.HARVEST_TIER_DIAMOND.get());
        GTNGItemList.UnlimitedHarvestPlugin.set(RtsItems.HARVEST_TIER_UNLIMITED.get());
    }
}
