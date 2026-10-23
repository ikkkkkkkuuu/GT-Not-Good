// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.crafting;

import com.xyp.gtnotgood.common.crafting.quantum.core.localization.PlayerMessages;

public class CraftingCalculationFailure extends RuntimeException {

    private final PlayerMessages messageKey;

    public CraftingCalculationFailure(PlayerMessages messageKey) {
        super(messageKey.getTranslationKey());
        this.messageKey = messageKey;
    }

    public PlayerMessages getMessageKey() {
        return this.messageKey;
    }

}
