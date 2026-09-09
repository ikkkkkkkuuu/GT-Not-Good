package com.xyp.gtnotgood.common.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.value.sync.DynamicLinkedSyncHandler;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widget.WidgetTree;

import io.netty.buffer.Unpooled;

/** Exercises the real matrix widget provider across first-open initialization and repeated topology refreshes. */
public final class NetworkGuiSyncChecks {

    private NetworkGuiSyncChecks() {}

    public static void run() throws Exception {
        for (int rows : new int[] { 0, 1, 8, 32 }) {
            PanelSyncManager manager = new PanelSyncManager(new ModularSyncManager(false), true);
            List<SyncHandler<?>> handlers = new ArrayList<>();
            String[] clicked = { "" };
            NetworkMatrixSelectionSync action = new NetworkMatrixSelectionSync(
                (key, channel) -> clicked[0] = key + "/" + channel);
            manager.syncValue("select", action);
            handlers.add(action);
            StringSyncValue selected = new StringSyncValue(() -> "0;");
            manager.syncValue("selected", selected);
            handlers.add(selected);
            StringSyncValue data = new StringSyncValue(() -> snapshot(rows));
            manager.syncValue("data", data);
            handlers.add(data);
            DynamicLinkedSyncHandler<StringSyncValue> dynamic = new DynamicLinkedSyncHandler<>(data)
                .widgetProvider((sync, value) -> NetworkGui.matrix(value.getValue(), action, selected));
            manager.syncValue("matrix", dynamic);
            handlers.add(dynamic);
            // Handlers following the dynamic matrix must not be skipped when the panel is initialized.
            for (int i = 0; i < 64; i++) {
                StringSyncValue value = new StringSyncValue(() -> "value");
                manager.syncValue("following_" + i, value);
                handlers.add(value);
            }
            // Exercise a populated matrix during initialize(), not only a later empty-to-populated refresh.
            data.setValue(snapshot(rows), false, false);
            manager.initialize("network_sync_test");
            for (SyncHandler<?> handler : handlers) {
                if (!handler.isValid() || handler.getSyncManager() != manager)
                    throw new AssertionError("Uninitialized handler");
            }
            for (int count : new int[] { rows, 32, 1, 0, 8 }) {
                data.setValue(snapshot(count), false, false);
                if (WidgetTree
                    .countUnregisteredSyncHandlers(manager, NetworkGui.matrix(snapshot(count), action, selected))
                    != 0) {
                    throw new AssertionError("Matrix refresh introduced a sync handler");
                }
            }
            PacketBuffer packet = new PacketBuffer(Unpooled.buffer());
            try {
                packet.writeStringToBuffer("10:20:30:2");
                packet.writeByte(7);
                action.readOnServer(0, packet);
                if (!clicked[0].equals("10:20:30:2/7")) throw new AssertionError("Stable selection key lost");
                packet.clear();
                packet.writeStringToBuffer("wrong");
                packet.writeByte(8);
                action.readOnServer(0, packet);
                if (!clicked[0].equals("10:20:30:2/7")) throw new AssertionError("Invalid channel accepted");
            } finally {
                packet.release();
            }
        }
        System.out.println("Network GUI sync checks passed: initial open and refresh with 0/1/8/32 rows");
    }

    private static String snapshot(int rows) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < rows; i++) value.append(i)
            .append(":20:30:2,,12000000,ZGV2aWNl;");
        return value.toString();
    }
}
