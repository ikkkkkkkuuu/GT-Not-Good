package com.xyp.gtnotgood.common.blocks.flux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;

import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;

/** Runs the same unregistered-handler guard used by MUI2 when constructing dynamic channel rows. */
public class FluxLogisticsDirectoryTest {

    @Test
    public void dynamicRowsRegisterAndReuseHandlersAcrossDirectoryChanges() throws Exception {
        PanelSyncManager sync = new PanelSyncManager(new ModularSyncManager(false), true);
        sync.initialize("test");
        allowRegistration(sync, true);
        var first = FluxLogisticsGui.directory(null, sync, encode("生产频道") + "\n" + encode("Storage"));
        allowRegistration(sync, false);
        assertEquals(0, WidgetTree.countUnregisteredSyncHandlers(sync, first));
        var saved = sync.getOrCreateSyncHandler("logistics_channel_Storage", 0, InteractionSyncHandler.class,
            () -> { throw new AssertionError("Expected an explicitly registered channel handler"); });
        allowRegistration(sync, true);
        var refreshed = FluxLogisticsGui.directory(null, sync, encode("Storage") + "\n" + encode("New channel"));
        allowRegistration(sync, false);
        assertEquals(0, WidgetTree.countUnregisteredSyncHandlers(sync, refreshed));
        assertSame(saved, sync.getOrCreateSyncHandler("logistics_channel_Storage", 0, InteractionSyncHandler.class,
            () -> { throw new AssertionError("Existing channel handler must survive refresh"); }));
    }

    private static void allowRegistration(PanelSyncManager sync, boolean allowed) throws Exception {
        var method = PanelSyncManager.class.getDeclaredMethod("allowTemporarySyncHandlerRegistration", boolean.class);
        method.setAccessible(true);
        method.invoke(sync, allowed);
    }

    private static String encode(String name) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }
}
