// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import appeng.api.networking.crafting.ICraftingPatternDetails;

/** Registry separating common provider scheduling from each target mod's real crafting implementation. */
public final class PackagedCoreRegistry {

    private static final Map<String, Adapter> ADAPTERS = new LinkedHashMap<>();

    private PackagedCoreRegistry() {}

    public static void register(String id, Adapter adapter) {
        if (id == null || id.isEmpty() || adapter == null || ADAPTERS.containsKey(id)) {
            throw new IllegalArgumentException("Invalid or duplicate packaged core: " + id);
        }
        ADAPTERS.put(id, adapter);
    }

    public static Map<String, Adapter> entries() {
        return Collections.unmodifiableMap(ADAPTERS);
    }

    public static Adapter get(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemPackagedCore core ? ADAPTERS.get(core.adapterId) : null;
    }

    /**
     * Server-only adapter contract. A failed dispatch must leave all inventories unchanged. A successful dispatch
     * transfers ownership of the supplied ingredients to the real target and returns its expected central output.
     * Implementations must not synthesize finished products or force-load chunks.
     */
    public interface Adapter {

        boolean accepts(TileEntity target);

        ItemStack dispatch(TilePackagedProvider provider, PackagedTarget target, ICraftingPatternDetails pattern,
            InventoryCrafting ingredients);

        /** Returns the real completed output inventory, or null while crafting/unavailable. */
        net.minecraft.inventory.IInventory output(TileEntity target);

        /** Maximum accepted receipts per target; adapters must also enforce physical input capacity. */
        default int maxInFlight() {
            return 1;
        }

        /** Whether releasing an interrupted receipt is safe; unknown adapters conservatively refuse. */
        default boolean canRelease(TileEntity target, ItemStack expected) {
            return false;
        }

        /** Explicit owner-requested interruption; ordinary adapters retain their stopped-job recovery rules. */
        default boolean interrupt(TilePackagedProvider provider, TileEntity target, ItemStack expected) {
            return canRelease(target, expected);
        }
    }
}
