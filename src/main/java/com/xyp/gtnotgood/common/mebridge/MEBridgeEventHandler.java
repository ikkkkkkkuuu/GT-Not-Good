package com.xyp.gtnotgood.common.mebridge;

import net.minecraftforge.event.world.WorldEvent;

import com.xyp.gtnotgood.GTNotGood;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * 跨维度 ME 网桥频道注册表的世界事件挂载。
 * <p>
 * 仿照 {@code SteamNetworkEventHandler}:{@link WorldEvent.Load} 时加载持久化;
 * 主世界({@code dim 0})卸载时复位,防止单机切换存档残留(现有蒸汽网缺此清理钩子,这里补上)。
 */
public class MEBridgeEventHandler {

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world.isRemote) return;
        try {
            MEBridgeWorldSavedData.loadInstance(event.world);
        } catch (Exception e) {
            GTNotGood.LOG.error("[MEBridge] Failed to load channel WorldSavedData", e);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) return;
        // 主世界卸载 = 存档关闭,复位以防单机切换存档残留
        if (event.world.provider.dimensionId == 0) {
            MEBridgeWorldSavedData.reset();
        }
    }
}
