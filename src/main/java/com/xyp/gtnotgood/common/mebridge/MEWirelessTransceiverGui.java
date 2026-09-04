package com.xyp.gtnotgood.common.mebridge;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
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

/** Channel browser stored on the handheld transceiver's ItemStack. */
final class MEWirelessTransceiverGui {

    private static final String SELECTED_SYNC_KEY = "wireless_selected";
    private static final String CHANNEL_LIST_SYNC_KEY = "wireless_channels";
    private static final String FILTER_SYNC_KEY = "wireless_filter";
    private static final String DIRECTORY_DATA_SYNC_KEY = "wireless_directory_data";
    private static final String DIRECTORY_WIDGET_SYNC_KEY = "wireless_directory_widget";
    private static final String DIRECTORY_SEPARATOR = "\u0002";

    private MEWirelessTransceiverGui() {}

    static ModularPanel build(PlayerInventoryGuiData data, PanelSyncManager syncManager) {
        StringSyncValue selectedSync = new StringSyncValue(
            () -> ItemMEWirelessTransceiver.getSelectedChannel(data.getUsedItemStack()),
            value -> setSelectedChannel(data, value)).allowC2S();
        syncManager.syncValue(SELECTED_SYNC_KEY, selectedSync);

        StringSyncValue channelListSync = MEBridgeGuiSync
            .readOnly(() -> MEBridgeChannelManager.browserSnapshot(data.getPlayer().worldObj.getTotalWorldTime()));
        syncManager.syncValue(CHANNEL_LIST_SYNC_KEY, channelListSync);

        String[] filter = { "" };
        StringSyncValue filterSync = new StringSyncValue(() -> filter[0], value -> filter[0] = value).allowC2S();
        syncManager.syncValue(FILTER_SYNC_KEY, filterSync);

        StringSyncValue directoryDataSync = MEBridgeGuiSync
            .readOnly(() -> channelListSync.getValue() + DIRECTORY_SEPARATOR + filter[0]);
        syncManager.syncValue(DIRECTORY_DATA_SYNC_KEY, directoryDataSync);
        DynamicLinkedSyncHandler<StringSyncValue> directoryWidgetSync = new DynamicLinkedSyncHandler<>(
            directoryDataSync).widgetProvider((manager, value) -> createDirectory(manager, data, value.getValue()));
        syncManager.syncValue(DIRECTORY_WIDGET_SYNC_KEY, directoryWidgetSync);

        ParentWidget<?> content = new ParentWidget<>().size(412, 212);
        // #tr gui.me_wireless_transceiver.title
        // # ME Wireless Transceiver
        // # zh_CN ME 无线收发器
        content.child(
            MEBridgeGuiTheme.text(
                IKey.lang("gui.me_wireless_transceiver.title"),
                8,
                6,
                396,
                MEBridgeGuiTheme.TEXT,
                Alignment.CenterLeft));

        ParentWidget<?> directory = MEBridgeGuiTheme.section(0, 24, 264, 180);
        // #tr gui.me_wireless_transceiver.directory
        // # Sender Channel Directory
        // # zh_CN 发起端频道目录
        directory.child(
            MEBridgeGuiTheme.text(
                IKey.lang("gui.me_wireless_transceiver.directory"),
                8,
                7,
                248,
                MEBridgeGuiTheme.ACCENT,
                Alignment.CenterLeft));
        // #tr gui.me_wireless_transceiver.search
        // # Search channels
        // # zh_CN 搜索频道
        directory.child(
            new TextFieldWidget().value(filterSync)
                .hintText(StatCollector.translateToLocal("gui.me_wireless_transceiver.search"))
                .autoUpdateOnChange(true)
                .pos(8, 22)
                .size(248, 14));
        directory.child(
            new DynamicSyncedWidget<>().syncHandler(directoryWidgetSync)
                .initialChild(createDirectory(syncManager, data, DIRECTORY_SEPARATOR))
                .pos(4, 42)
                .size(256, 132));
        content.child(directory);

        ParentWidget<?> details = MEBridgeGuiTheme.section(270, 24, 142, 180);
        // #tr gui.me_wireless_transceiver.selected
        // # Selected Channel
        // # zh_CN 已选择频道
        details.child(
            MEBridgeGuiTheme.text(
                IKey.lang("gui.me_wireless_transceiver.selected"),
                8,
                8,
                126,
                MEBridgeGuiTheme.ACCENT,
                Alignment.CenterLeft));
        details.child(
            MEBridgeGuiTheme.dynamic(
                () -> selectedSync.getValue()
                    .isEmpty() ? "-" : selectedSync.getValue(),
                8,
                28,
                126,
                MEBridgeGuiTheme.SUCCESS,
                Alignment.CenterLeft));
        // #tr gui.me_wireless_transceiver.instructions.0
        // # Close this screen, then
        // # zh_CN 关闭界面，然后
        details.child(
            MEBridgeGuiTheme.text(
                IKey.lang("gui.me_wireless_transceiver.instructions.0"),
                8,
                58,
                126,
                MEBridgeGuiTheme.MUTED,
                Alignment.CenterLeft));
        // #tr gui.me_wireless_transceiver.instructions.1
        // # Shift + right-click an ME node.
        // # zh_CN Shift + 右键 ME 节点。
        details.child(
            MEBridgeGuiTheme.text(
                IKey.lang("gui.me_wireless_transceiver.instructions.1"),
                8,
                72,
                126,
                MEBridgeGuiTheme.MUTED,
                Alignment.CenterLeft));
        // #tr gui.me_wireless_transceiver.instructions.2
        // # Repeat to disconnect.
        // # zh_CN 再次操作即可断开。
        details.child(
            MEBridgeGuiTheme.text(
                IKey.lang("gui.me_wireless_transceiver.instructions.2"),
                8,
                86,
                126,
                MEBridgeGuiTheme.MUTED,
                Alignment.CenterLeft));
        content.child(details);

        return ModularPanel.defaultPanel("me_wireless_transceiver", 420, 220)
            .background(GTNGGuiTextures.MODERN_BACKGROUND)
            .child(content.pos(4, 4));
    }

    private static void setSelectedChannel(PlayerInventoryGuiData data, String value) {
        ItemStack stack = data.getUsedItemStack();
        if (stack == null || !(stack.getItem() instanceof ItemMEWirelessTransceiver)) return;
        ItemMEWirelessTransceiver.setSelectedChannel(stack, value);
        data.getPlayer().inventory.markDirty();
    }

    private static IWidget createDirectory(PanelSyncManager syncManager, PlayerInventoryGuiData data,
        String encodedDirectoryData) {
        String[] directoryData = encodedDirectoryData.split(DIRECTORY_SEPARATOR, 2);
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
            .width(248);
        Integer currentDimension = null;
        for (MEBridgeChannelListCodec.Entry entry : entries) {
            if (!Integer.valueOf(entry.dimension)
                .equals(currentDimension)) {
                currentDimension = entry.dimension;
                column.child(dimensionHeader(currentDimension));
            }
            column.child(channelRow(syncManager, data, entry));
        }
        return new ListWidget<>().size(256, 132)
            .child(column);
    }

    private static IWidget emptyDirectory(boolean filtered) {
        ParentWidget<?> empty = new ParentWidget<>().size(256, 132);
        // #tr gui.me_wireless_transceiver.empty
        // # No sender channels are available.
        // # zh_CN 当前没有可用的发起端频道。
        // #tr gui.me_wireless_transceiver.no_results
        // # No channels match this search.
        // # zh_CN 没有匹配的频道。
        empty.child(
            MEBridgeGuiTheme.text(
                IKey.lang(filtered ? "gui.me_wireless_transceiver.no_results" : "gui.me_wireless_transceiver.empty"),
                4,
                58,
                248,
                MEBridgeGuiTheme.MUTED,
                Alignment.Center));
        return empty;
    }

    private static ParentWidget<?> dimensionHeader(int dimension) {
        ParentWidget<?> header = new ParentWidget<>().width(248)
            .height(16)
            .background(new com.cleanroommc.modularui.drawable.Rectangle().color(MEBridgeGuiTheme.PANEL_ROW));
        header.child(
            MEBridgeGuiTheme.text(
                IKey.str(MEBridgeGuiTheme.dimensionLabel(dimension)),
                6,
                2,
                236,
                MEBridgeGuiTheme.ACCENT,
                Alignment.CenterLeft));
        return header;
    }

    private static ParentWidget<?> channelRow(PanelSyncManager syncManager, PlayerInventoryGuiData data,
        MEBridgeChannelListCodec.Entry entry) {
        String label = (entry.online ? "\u00A7a\u25CF " : "\u00A77\u25CB ") + entry.name
            + "\u00A7r  "
            + entry.receiverCount;
        ParentWidget<?> row = new ParentWidget<>().width(248)
            .height(18)
            .marginBottom(1);
        row.child(
            new ButtonWidget<>().pos(0, 0)
                .size(248, 18)
                .background(GTNGGuiTextures.MODERN_BUTTON_COMPACT)
                .overlay(IKey.str(label))
                .syncHandler(selectAction(syncManager, data, entry))
                .tooltip(
                    tooltip -> tooltip.addLine(
                        IKey.str(
                            entry.x + ", "
                                + entry.y
                                + ", "
                                + entry.z
                                + "  |  "
                                + MEBridgeGuiTheme.dimensionLabel(entry.dimension)))));
        return row;
    }

    private static InteractionSyncHandler selectAction(PanelSyncManager syncManager, PlayerInventoryGuiData data,
        MEBridgeChannelListCodec.Entry entry) {
        String key = "me_wireless_transceiver.select." + entry.dimension
            + "."
            + entry.x
            + "."
            + entry.y
            + "."
            + entry.z
            + "."
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(entry.name.getBytes(StandardCharsets.UTF_8));
        return syncManager.getOrCreateSyncHandler(
            key,
            InteractionSyncHandler.class,
            () -> new InteractionSyncHandler()
                .setOnMousePressed(mouseData -> { if (!mouseData.isClient()) setSelectedChannel(data, entry.name); }));
    }
}
