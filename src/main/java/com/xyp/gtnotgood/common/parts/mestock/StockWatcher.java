package com.xyp.gtnotgood.common.parts.mestock;

import appeng.api.networking.storage.IStackWatcher;
import appeng.api.storage.data.IAEStack;

/** Registers only configured, enabled identities. No wildcard inventory listener or full storage scan. */
public final class StockWatcher {

    private final StockHost host;
    private IStackWatcher watcher;

    public StockWatcher(StockHost host) {
        this.host = host;
    }

    public void bind(IStackWatcher watcher) {
        this.watcher = watcher;
        rebuild();
    }

    public void rebuild() {
        if (watcher == null) return;
        watcher.clear();
        StockConfig config = host.stockConfig();
        for (int i = 0; i < host.stockSlots(); i++) {
            IAEStack<?> key = config.key(i);
            if (key != null && config.enabled(i)) watcher.add(key);
        }
    }
}
