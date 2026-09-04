package com.xyp.gtnotgood.common.mebridge;

import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.DynamicLinkedSyncHandler;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;

final class MEBridgeReceiverGui {

    private static final String CHANNEL_SYNC_KEY = "channel";
    private static final String CONNECTED_SYNC_KEY = "connected";
    private static final String CHANNEL_LIST_SYNC_KEY = "channelList_data";
    private static final String DIRECTORY_DATA_SYNC_KEY = "channelDirectory_data";
    private static final String DIRECTORY_SYNC_KEY = "channelDirectory";
    private static final String DIRECTORY_DATA_SEPARATOR = "\u0002";

    private MEBridgeReceiverGui() {}

    static ModularPanel build(TileMEBridgeReceiver receiver, PanelSyncManager syncManager) {
        StringSyncValue channelSync = MEBridgeGuiSync
            .editableChannel(receiver::getChannelName, receiver::setChannelName);
        syncManager.syncValue(CHANNEL_SYNC_KEY, channelSync);

        StringSyncValue connectedSync = MEBridgeGuiSync.readOnly(() -> receiver.isConnected() ? "1" : "0");
        syncManager.syncValue(CONNECTED_SYNC_KEY, connectedSync);

        StringSyncValue channelListSync = MEBridgeGuiSync
            .readOnly(() -> MEBridgeChannelManager.browserSnapshot(receiver.getWorldTime()));
        syncManager.syncValue(CHANNEL_LIST_SYNC_KEY, channelListSync);

        String[] directoryFilter = { "" };
        StringSyncValue filterSync = new StringSyncValue(() -> directoryFilter[0], value -> directoryFilter[0] = value)
            .allowC2S();
        StringSyncValue directoryDataSync = MEBridgeGuiSync
            .readOnly(() -> channelListSync.getValue() + DIRECTORY_DATA_SEPARATOR + directoryFilter[0]);
        syncManager.syncValue(DIRECTORY_DATA_SYNC_KEY, directoryDataSync);

        DynamicLinkedSyncHandler<StringSyncValue> directorySync = new DynamicLinkedSyncHandler<>(directoryDataSync)
            .widgetProvider((manager, value) -> createDirectory(manager, receiver, value.getValue()));
        syncManager.syncValue(DIRECTORY_SYNC_KEY, directorySync);

        ParentWidget<?> content = new ParentWidget<>().size(412, 232);
        // #tr gui.mebridge.receiver.title
        // # ME Bridge Receiver
        // # zh_CN ME 网桥 - 接收端
        content.child(
            MEBridgeGuiTheme.text(
                IKey.lang("gui.mebridge.receiver.title"),
                8,
                6,
                200,
                MEBridgeGuiTheme.TEXT,
                Alignment.CenterLeft));
        // #tr gui.mebridge.receiver.connected
        // # Connected
        // # zh_CN 已连接
        // #tr gui.mebridge.receiver.disconnected
        // # Disconnected
        // # zh_CN 未连接
        content.child(
            MEBridgeGuiTheme.dynamic(
                () -> StatCollector.translateToLocal(
                    "1".equals(connectedSync.getValue()) ? "gui.mebridge.receiver.connected"
                        : "gui.mebridge.receiver.disconnected"),
                300,
                6,
                104,
                "1".equals(connectedSync.getValue()) ? MEBridgeGuiTheme.SUCCESS : MEBridgeGuiTheme.MUTED,
                Alignment.CenterRight));

        ParentWidget<?> directory = MEBridgeGuiTheme.section(0, 24, 238, 200);
        // #tr gui.mebridge.receiver.directory
        // # Channel Directory
        // # zh_CN 频道目录
        directory.child(
            MEBridgeGuiTheme.text(
                IKey.lang("gui.mebridge.receiver.directory"),
                8,
                8,
                222,
                MEBridgeGuiTheme.ACCENT,
                Alignment.CenterLeft));
        // #tr gui.mebridge.receiver.directory_hint
        // # Grouped by sender dimension
        // # zh_CN 按发起端维度分组
        directory.child(
            // #tr gui.mebridge.receiver.search
            // # Search sender channels
            // # zh_CN 搜索发起端频道
            new TextFieldWidget().value(filterSync)
                .hintText(StatCollector.translateToLocal("gui.mebridge.receiver.search"))
                .autoUpdateOnChange(true)
                .pos(8, 22)
                .size(222, 14));
        directory.child(
            new DynamicSyncedWidget<>().syncHandler(directorySync)
                .initialChild(createDirectory(syncManager, receiver, DIRECTORY_DATA_SEPARATOR))
                .pos(4, 42)
                .size(230, 152));
        content.child(directory);

        ParentWidget<?> details = MEBridgeGuiTheme.section(244, 24, 168, 200);
        // #tr gui.mebridge.receiver.details
        // # Link Details
        // # zh_CN 连接详情
        details.child(
            MEBridgeGuiTheme
                .text(IKey.lang("gui.mebridge.receiver.details"), 8, 8, 152, MEBridgeGuiTheme.TEXT, Alignment.Center));
        // #tr gui.mebridge.receiver.channel
        // # Current Channel
        // # zh_CN 当前频道
        details.child(
            MEBridgeGuiTheme.text(
                IKey.lang("gui.mebridge.receiver.channel"),
                8,
                26,
                152,
                MEBridgeGuiTheme.ACCENT,
                Alignment.CenterLeft));
        details.child(
            new TextFieldWidget().value(channelSync)
                .autoUpdateOnChange(false)
                .setMaxLength(MEBridgeChannelName.MAX_LENGTH)
                .pos(8, 40)
                .size(116, 16));
        // #tr gui.mebridge.receiver.clear
        // # Clear
        // # zh_CN 断开
        details.child(
            new ButtonWidget<>().pos(128, 40)
                .size(32, 16)
                .background(GTNGGuiTextures.MODERN_BUTTON_COMPACT)
                .overlay(IKey.lang("gui.mebridge.receiver.clear"))
                .syncHandler(
                    new InteractionSyncHandler()
                        .setOnMousePressed(mouseData -> { if (!mouseData.isClient()) receiver.setChannelName(""); })));
        // #tr gui.mebridge.receiver.local_dimension
        // # Receiver Dimension
        // # zh_CN 接收端维度
        details.child(
            MEBridgeGuiTheme.dynamic(
                () -> StatCollector.translateToLocal("gui.mebridge.receiver.local_dimension") + ": "
                    + MEBridgeGuiTheme.dimensionLabel(receiver.getDimensionId()),
                8,
                66,
                152,
                MEBridgeGuiTheme.MUTED,
                Alignment.CenterLeft));
        // #tr gui.mebridge.receiver.sender_dimension
        // # Sender Dimension
        // # zh_CN 发起端维度
        details.child(
            detail(channelListSync, receiver, "gui.mebridge.receiver.sender_dimension", 92, EntryField.DIMENSION));
        // #tr gui.mebridge.receiver.sender_coordinates
        // # Sender Coordinates
        // # zh_CN 发起端坐标
        details.child(
            detail(channelListSync, receiver, "gui.mebridge.receiver.sender_coordinates", 116, EntryField.COORDINATES));
        // #tr gui.mebridge.receiver.linked_receivers
        // # Linked Receivers
        // # zh_CN 已连接接收端
        details.child(
            detail(
                channelListSync,
                receiver,
                "gui.mebridge.receiver.linked_receivers",
                140,
                EntryField.RECEIVER_COUNT));
        // #tr gui.mebridge.receiver.connection
        // # Connection
        // # zh_CN 连接状态
        details.child(
            MEBridgeGuiTheme.dynamic(
                () -> StatCollector.translateToLocal("gui.mebridge.receiver.connection") + ": "
                    + StatCollector.translateToLocal(
                        "1".equals(connectedSync.getValue()) ? "gui.mebridge.receiver.connected"
                            : "gui.mebridge.receiver.disconnected"),
                8,
                166,
                152,
                "1".equals(connectedSync.getValue()) ? MEBridgeGuiTheme.SUCCESS : MEBridgeGuiTheme.MUTED,
                Alignment.CenterLeft));
        content.child(details);

        return ModularPanel.defaultPanel("mebridge_receiver", 420, 240)
            .background(GTNGGuiTextures.MODERN_BACKGROUND)
            .child(content.pos(4, 4));
    }

    private static IWidget createDirectory(PanelSyncManager syncManager, TileMEBridgeReceiver receiver,
        String encodedDirectoryData) {
        String[] directoryData = encodedDirectoryData.split(DIRECTORY_DATA_SEPARATOR, 2);
        String encodedEntries = directoryData.length > 0 ? directoryData[0] : "";
        String filter = directoryData.length > 1 ? directoryData[1].trim()
            .toLowerCase(Locale.ROOT) : "";
        List<MEBridgeChannelListCodec.Entry> entries = MEBridgeChannelListCodec.decode(encodedEntries);
        if (!filter.isEmpty()) entries.removeIf(
            entry -> !entry.name.toLowerCase(Locale.ROOT)
                .contains(filter));
        if (entries.isEmpty()) return emptyDirectory(!filter.isEmpty());

        Flow column = Flow.column()
            .coverChildrenHeight()
            .width(222);
        Integer currentDimension = null;
        for (MEBridgeChannelListCodec.Entry entry : entries) {
            if (!Integer.valueOf(entry.dimension)
                .equals(currentDimension)) {
                currentDimension = entry.dimension;
                column.child(dimensionHeader(currentDimension));
            }
            column.child(channelRow(syncManager, receiver, entry));
        }
        return new ListWidget<>().size(230, 152)
            .child(column);
    }

    private static IWidget emptyDirectory(boolean filtered) {
        ParentWidget<?> empty = new ParentWidget<>().size(230, 152);
        // #tr gui.mebridge.receiver.empty
        // # No sender channels are currently available.
        // # zh_CN 当前没有可用的发起端频道。
        empty.child(
            // #tr gui.mebridge.receiver.no_search_results
            // # No channels match this search.
            // # zh_CN 没有匹配的频道。
            MEBridgeGuiTheme.text(
                IKey.lang(filtered ? "gui.mebridge.receiver.no_search_results" : "gui.mebridge.receiver.empty"),
                4,
                67,
                222,
                MEBridgeGuiTheme.MUTED,
                Alignment.Center));
        return empty;
    }

    private static IWidget detail(StringSyncValue channelListSync, TileMEBridgeReceiver receiver, String key, int y,
        EntryField field) {
        return MEBridgeGuiTheme.dynamic(
            () -> StatCollector.translateToLocal(key) + ": " + field.value(selectedEntry(channelListSync, receiver)),
            8,
            y,
            152,
            MEBridgeGuiTheme.TEXT,
            Alignment.CenterLeft);
    }

    private static MEBridgeChannelListCodec.Entry selectedEntry(StringSyncValue channelListSync,
        TileMEBridgeReceiver receiver) {
        String selectedName = receiver.getChannelName();
        for (MEBridgeChannelListCodec.Entry entry : MEBridgeChannelListCodec.decode(channelListSync.getValue())) {
            if (entry.name.equals(selectedName)) return entry;
        }
        return null;
    }

    private static ParentWidget<?> dimensionHeader(int dimension) {
        ParentWidget<?> header = new ParentWidget<>().width(222)
            .height(16)
            .background(new com.cleanroommc.modularui.drawable.Rectangle().color(MEBridgeGuiTheme.PANEL_ROW));
        header.child(
            MEBridgeGuiTheme.text(
                IKey.str(MEBridgeGuiTheme.dimensionLabel(dimension)),
                6,
                2,
                210,
                MEBridgeGuiTheme.ACCENT,
                Alignment.CenterLeft));
        return header;
    }

    private static ParentWidget<?> channelRow(PanelSyncManager syncManager, TileMEBridgeReceiver receiver,
        MEBridgeChannelListCodec.Entry entry) {
        String label = (entry.online ? "\u00A7a\u25CF " : "\u00A77\u25CB ") + entry.name
            + "\u00A7r  "
            + entry.receiverCount;
        ParentWidget<?> row = new ParentWidget<>().width(222)
            .height(18)
            .marginBottom(1);
        row.child(
            new ButtonWidget<>().pos(0, 0)
                .size(178, 18)
                .background(GTNGGuiTextures.MODERN_BUTTON_COMPACT)
                .overlay(IKey.str(label))
                .tooltip(
                    tooltip -> tooltip.addLine(
                        IKey.str(
                            entry.x + ", "
                                + entry.y
                                + ", "
                                + entry.z
                                + "  |  "
                                + MEBridgeGuiTheme.dimensionLabel(entry.dimension)))));
        // #tr gui.mebridge.receiver.connect
        // # Connect
        // # zh_CN 连接
        row.child(
            new ButtonWidget<>().pos(182, 0)
                .size(18, 18)
                .background(GTNGGuiTextures.MODERN_BUTTON_COMPACT)
                .overlay(IKey.str("C"))
                .syncHandler(directoryAction(syncManager, receiver, entry, DirectoryAction.CONNECT))
                .tooltip(tooltip -> tooltip.addLine(IKey.lang("gui.mebridge.receiver.connect"))));
        // #tr gui.mebridge.receiver.teleport
        // # Teleport to sender
        // # zh_CN 传送至发起端
        row.child(
            new ButtonWidget<>().pos(204, 0)
                .size(18, 18)
                .background(GTNGGuiTextures.MODERN_BUTTON_COMPACT)
                .overlay(IKey.str("T"))
                .syncHandler(directoryAction(syncManager, receiver, entry, DirectoryAction.TELEPORT))
                .tooltip(tooltip -> tooltip.addLine(IKey.lang("gui.mebridge.receiver.teleport"))));
        return row;
    }

    private static InteractionSyncHandler directoryAction(PanelSyncManager syncManager, TileMEBridgeReceiver receiver,
        MEBridgeChannelListCodec.Entry entry, DirectoryAction action) {
        String key = "mebridge.directory." + action.key
            + "."
            + entry.dimension
            + "."
            + entry.x
            + "."
            + entry.y
            + "."
            + entry.z
            + "."
            + entry.name;
        return syncManager.getOrCreateSyncHandler(
            key,
            InteractionSyncHandler.class,
            () -> new InteractionSyncHandler().setOnMousePressed(mouseData -> {
                if (mouseData.isClient()) return;
                if (action == DirectoryAction.TELEPORT && syncManager.getPlayer() instanceof EntityPlayerMP) {
                    receiver.teleportPlayerToSender((EntityPlayerMP) syncManager.getPlayer(), entry.name);
                } else if (action == DirectoryAction.CONNECT) {
                    receiver.setChannelName(entry.name);
                }
            }));
    }

    private enum DirectoryAction {

        CONNECT("connect"),
        TELEPORT("teleport");

        private final String key;

        DirectoryAction(String key) {
            this.key = key;
        }
    }

    private enum EntryField {

        DIMENSION {

            @Override
            String value(MEBridgeChannelListCodec.Entry entry) {
                return entry == null ? "-" : MEBridgeGuiTheme.dimensionLabel(entry.dimension);
            }
        },
        COORDINATES {

            @Override
            String value(MEBridgeChannelListCodec.Entry entry) {
                return entry == null ? "-" : entry.x + ", " + entry.y + ", " + entry.z;
            }
        },
        RECEIVER_COUNT {

            @Override
            String value(MEBridgeChannelListCodec.Entry entry) {
                return entry == null ? "-" : String.valueOf(entry.receiverCount);
            }
        };

        abstract String value(MEBridgeChannelListCodec.Entry entry);
    }
}
