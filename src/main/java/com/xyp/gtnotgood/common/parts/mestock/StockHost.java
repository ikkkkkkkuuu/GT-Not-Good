package com.xyp.gtnotgood.common.parts.mestock;

import appeng.me.helpers.AENetworkProxy;

/** Shared server configuration contract for the stock GUIs and exact resource watchers. */
public interface StockHost {

    StockConfig stockConfig();

    AENetworkProxy getProxy();

    void stockChanged();

    String stockTitle();

    default int stockSlots() {
        return stockConfig().size();
    }
}
