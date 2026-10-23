// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.client.quantum;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.network.quantum.QuantumCraftingModePacket;

@Config(modid = GTNotGood.MODID, configSubDirectory = "GTNOTGOOD", filename = "QuantumCraftingClient")
@Config.ExcludeFromAutoGui
public final class QuantumCraftingClientConfig {

    @Config.Comment("网络中有量子计算机时，使用量子模式规划玩家发起的合成请求。")
    @Config.DefaultBoolean(false)
    public static boolean useQuantumCraftingMode;

    @Config.Ignore
    private static boolean registered;

    @Config.Ignore
    private static boolean loadFailed;

    @Config.Ignore
    private static volatile boolean syncScheduled;

    public static boolean isQuantumCraftingMode() {
        ensureLoaded();
        return useQuantumCraftingMode;
    }

    public static void setQuantumCraftingMode(boolean enabled) {
        ensureLoaded();
        if (useQuantumCraftingMode == enabled) return;
        useQuantumCraftingMode = enabled;
        if (registered) ConfigurationManager.save(QuantumCraftingClientConfig.class);
    }

    public static void scheduleSync() {
        syncScheduled = true;
    }

    public static void syncIfScheduled() {
        if (!syncScheduled) return;
        syncScheduled = false;
        // The server starts every session with the mode off
        if (isQuantumCraftingMode()) {
            GTNotGood.channel.sendToServer(new QuantumCraftingModePacket(true));
        }
    }

    private static void ensureLoaded() {
        if (registered || loadFailed) return;
        try {
            ConfigurationManager.registerConfig(QuantumCraftingClientConfig.class);
            registered = true;
        } catch (ConfigException e) {
            loadFailed = true;
            GTNotGood.LOG.error("Failed to load quantum crafting client preferences", e);
        }
    }
}
