// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.api.networking.storage;

import java.util.Collection;
import java.util.Set;

import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.AEKey;
import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.KeyCounter;

public interface IStorageService {

    int getInventorySize();

    KeyCounter copyInventory();

    KeyCounter copyInventory(Collection<AEKey> keys, Set<AEKey> fuzzyKeys);
}
