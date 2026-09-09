package com.xyp.gtnotgood.common.network;

import java.io.IOException;
import java.util.function.BiConsumer;

import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.value.sync.SyncHandler;

/**
 * One eagerly registered selection channel for all matrix cells. Dynamic rows never register sync handlers.
 * Each click carries the displayed endpoint key, so sorting or paging cannot redirect it to a different device.
 */
final class NetworkMatrixSelectionSync extends SyncHandler<NetworkMatrixSelectionSync> {

    private final BiConsumer<String, Integer> selection;

    NetworkMatrixSelectionSync(BiConsumer<String, Integer> selection) {
        this.selection = selection;
        allowC2S();
    }

    void select(String key, int channel) {
        syncToServer(0, buffer -> {
            buffer.writeStringToBuffer(key);
            buffer.writeByte(channel);
        });
    }

    @Override
    public void readOnServer(int id, PacketBuffer buffer) throws IOException {
        if (id != 0) return;
        String key = buffer.readStringFromBuffer(64);
        int channel = buffer.readUnsignedByte();
        if (channel < 8) selection.accept(key, channel);
    }

    @Override
    public void readOnClient(int id, PacketBuffer buffer) {}
}
