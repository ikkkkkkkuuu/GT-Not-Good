// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Modified for compact, fixed-maximum, energy-free GT Not Good machines.
package com.xyp.gtnotgood.utils;

/**
 * Carries an exact long-sized dispatch multiplier alongside Minecraft's int-sized crafting inventory stacks.
 */
public interface LargeInventoryCrafting {

    /**
     * Stores the multiplier selected before a compatible medium receives the crafting inventory.
     *
     * @param value positive dispatch craft count
     */
    void setAssemblerSize(long value);

    /**
     * Returns the exact multiplier that medium output logic must use instead of ItemStack.stackSize.
     *
     * @return positive dispatch craft count; ordinary crafting inventories default to one
     */
    long getAssemblerSize();
}
