package com.xyp.gtnotgood.common.mebridge;

import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Forge/FML lifecycle hooks for persistent wireless ME links. */
public final class MEWirelessLinkEventHandler {

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote) MEWirelessLinkManager.loadInstance(event.world);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) return;
        int dimension = event.world.provider.dimensionId;
        MEWirelessLinkManager.onDimensionUnload(dimension);
        if (dimension == 0) MEWirelessLinkManager.reset();
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.world.isRemote) {
            MEWirelessLinkManager.onChunkUnload(
                event.world.provider.dimensionId,
                event.getChunk().xPosition,
                event.getChunk().zPosition);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.world.isRemote) {
            MEWirelessLinkManager.removeAt(event.world.provider.dimensionId, event.x, event.y, event.z);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) MEWirelessLinkManager.onServerTick();
    }
}
