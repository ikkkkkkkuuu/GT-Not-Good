package com.xyp.gtnotgood.common.flux;

import java.util.List;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import com.xyp.gtnotgood.GTNotGood;

/**
 * Forge 1.7.10 adapter for Flux Networks' opt-in chunk loading switch. Each ticket forces only
 * the connector's own chunk. Forge's configured ticket quotas apply; failure leaves the switch off.
 */
public final class FluxChunkLoading implements ForgeChunkManager.LoadingCallback {

    public static ForgeChunkManager.Ticket request(TileFluxConnector tile) {
        ForgeChunkManager.Ticket ticket = ForgeChunkManager
            .requestTicket(GTNotGood.instance, tile.getWorldObj(), ForgeChunkManager.Type.NORMAL);
        if (ticket != null) {
            ticket.getModData()
                .setInteger("fluxX", tile.xCoord);
            ticket.getModData()
                .setInteger("fluxY", tile.yCoord);
            ticket.getModData()
                .setInteger("fluxZ", tile.zCoord);
            ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(tile.xCoord >> 4, tile.zCoord >> 4));
        }
        return ticket;
    }

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        for (ForgeChunkManager.Ticket ticket : tickets) {
            int x = ticket.getModData()
                .getInteger("fluxX");
            int y = ticket.getModData()
                .getInteger("fluxY");
            int z = ticket.getModData()
                .getInteger("fluxZ");
            if (ticket.getModData()
                .hasKey("fluxX") && world.getTileEntity(x, y, z) instanceof TileFluxConnector tile
                && tile.restoreTicket(ticket)) {
                ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(x >> 4, z >> 4));
            } else {
                ForgeChunkManager.releaseTicket(ticket);
            }
        }
    }
}
