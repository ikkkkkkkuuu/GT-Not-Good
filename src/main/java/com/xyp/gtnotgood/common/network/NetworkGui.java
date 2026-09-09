package com.xyp.gtnotgood.common.network;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.DynamicLinkedSyncHandler;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;
import com.xyp.gtnotgood.common.network.NetworkTopology.Endpoint;
import com.xyp.gtnotgood.common.network.TileNetworkController.Channel;

/** MUI2-only controller and connector panels. Every edit runs on the server and addresses a stable endpoint key. */
public final class NetworkGui {

    private static final int MATRIX_X = 12;
    private static final int CHANNEL_OFFSET = 36;
    private static final int CHANNEL_PITCH = 21;
    private static final int CHANNEL_WIDTH = 20;

    private NetworkGui() {}

    public static ModularPanel controller(TileNetworkController controller, PanelSyncManager sync) {
        int[] selected = { 0 };
        int[] page = { 0 };
        String[] selectedKey = { "" };
        String[] search = { "" };
        ModularPanel panel = ModularPanel.defaultPanel("programmable_network", 470, 330)
            .background(GTNGGuiTextures.MODERN_BACKGROUND);
        panel.child(
            new ParentWidget<>().pos(4, 4)
                .size(462, 322)
                .background(new Rectangle().color(0xFF202632)));
        panel.child(
            new ParentWidget<>().pos(8, 8)
                .size(224, 305)
                .background(new Rectangle().color(0xFF303746)));
        panel.child(
            new ParentWidget<>().pos(238, 8)
                .size(224, 56)
                .background(new Rectangle().color(0xFF303746)));
        panel.child(
            new ParentWidget<>().pos(238, 70)
                .size(224, 146)
                .background(new Rectangle().color(0xFF303746)));
        StringSyncValue selection = read(sync, "selection", () -> selected[0] + ";" + selectedKey[0]);
        NetworkMatrixSelectionSync matrixSelection = new NetworkMatrixSelectionSync((key, channel) -> {
            if (controller.endpoint(key) == null) return;
            selected[0] = channel;
            selectedKey[0] = key;
        });
        sync.syncValue("matrix_selection", matrixSelection);
        StringSyncValue query = new StringSyncValue(() -> search[0], value -> {
            search[0] = value.length() > 32 ? value.substring(0, 32) : value;
            page[0] = 0;
        }).allowC2S();
        sync.syncValue("search", query);
        panel.child(
            new TextFieldWidget().value(query)
                .setMaxLength(32)
                .pos(12, 12)
                .size(158, 18));
        panel.child(button(sync, "previous", () -> "<", () -> page[0] = Math.max(0, page[0] - 1), 174, 11, 22));
        panel.child(button(sync, "next", () -> ">", () -> page[0] = Math.min(191, page[0] + 1), 200, 11, 22));
        for (int i = 0; i < 8; i++) {
            final int index = i;
            panel.child(
                button(
                    sync,
                    "channel_" + i,
                    () -> integer(selection, 0) == index ? "[" + (index + 1) + "]" : "" + (index + 1),
                    () -> {
                        selected[0] = index;
                        selectedKey[0] = "";
                    },
                    MATRIX_X + CHANNEL_OFFSET + i * CHANNEL_PITCH,
                    36,
                    CHANNEL_WIDTH));
        }
        StringSyncValue topologyState = read(sync, "topology_state", () -> {
            NetworkTopology topology = controller.topology();
            return topology.status + ";" + topology.nodes + ";" + topology.endpoints.size();
        });
        panel.child(
            text(
                () -> status(integer(topologyState, 0)) + "  "
                    + field(topologyState, 1)
                    + " / "
                    + field(topologyState, 2),
                12,
                316,
                218));
        StringSyncValue list = new StringSyncValue(() -> {
            java.util.List<Endpoint> endpoints = new java.util.ArrayList<>();
            String queryText = search[0].toLowerCase(java.util.Locale.ROOT);
            for (Endpoint endpoint : controller.topology().endpoints) {
                if (deviceLabel(endpoint).toLowerCase(java.util.Locale.ROOT)
                    .contains(queryText)) endpoints.add(endpoint);
            }
            page[0] = Math.min(page[0], Math.max(0, (endpoints.size() - 1) / 32));
            StringBuilder snapshot = new StringBuilder();
            for (int i = page[0] * 32; i < Math.min(endpoints.size(), page[0] * 32 + 32); i++) {
                Endpoint endpoint = endpoints.get(i);
                TileEntity target = endpoint.target();
                if (target == null) continue;
                snapshot.append(endpoint.key)
                    .append(',')
                    .append(NetworkDeviceDisplay.encode(endpoint))
                    .append(',');
                for (Channel channel : controller.channels) {
                    NetworkRule rule = channel.rules.get(endpoint.key);
                    snapshot.append(rule == null ? 0 : rule.mode == 0 ? 3 : rule.mode);
                }
                snapshot.append(',')
                    .append(
                        java.util.Base64.getEncoder()
                            .encodeToString(deviceLabel(endpoint).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .append(';');
            }
            return snapshot.toString();
        }, ignored -> {});
        sync.syncValue("matrix_data", list);
        DynamicLinkedSyncHandler<StringSyncValue> matrix = new DynamicLinkedSyncHandler<>(list)
            .widgetProvider((manager, value) -> matrix(value.getValue(), matrixSelection, selection));
        sync.syncValue("matrix_widgets", matrix);
        panel.child(
            new DynamicSyncedWidget<>().syncHandler(matrix)
                .pos(MATRIX_X, 62)
                .size(216, 246));

        StringSyncValue channelState = read(sync, "channel_state", () -> {
            Channel channel = controller.channels[selected[0]];
            return channel.type + ";"
                + (channel.enabled ? 1 : 0)
                + ";"
                + channel.interval
                + ";"
                + (channel.hasCargo() ? 1 : 0);
        });
        panel.child(button(sync, "type", () -> type(integer(channelState, 0)), () -> {
            Channel channel = controller.channels[selected[0]];
            if (channel.hasCargo()) return;
            channel.type = (channel.type + 1) % 3;
            channel.enabled = false;
            channel.rules.clear();
            controller.markDirty();
        }, 244, 12, 54).tooltip(t -> {
            // #tr gui.network.type_hint
            // # Switching type clears rules. Drain buffered cargo first.
            // # zh_CN 切换类型会清空规则；须先传完缓冲内容。
            t.addLine(IKey.lang("gui.network.type_hint"));
        }));
        StringSyncValue name = new StringSyncValue(() -> controller.channels[selected[0]].name, value -> {
            if (controller.getWorldObj().isRemote) return;
            String clean = value.replaceAll("[\\p{Cntrl}§]", "");
            controller.channels[selected[0]].name = clean.substring(0, Math.min(24, clean.length()));
            controller.markDirty();
        }).allowC2S();
        sync.syncValue("channel_name", name);
        panel.child(
            new TextFieldWidget().value(name)
                .setMaxLength(24)
                .pos(304, 13)
                .size(150, 18));
        com.cleanroommc.modularui.value.sync.IntSyncValue channelPriority = new com.cleanroommc.modularui.value.sync.IntSyncValue(
            () -> controller.channels[selected[0]].priority,
            value -> {
                if (controller.getWorldObj().isRemote) return;
                controller.channels[selected[0]].priority = Math.max(-99, Math.min(99, value));
                controller.markDirty();
            }).allowC2S();
        sync.syncValue("channel_priority", channelPriority);
        // #tr gui.network.channel_priority
        // # Channel priority
        // # zh_CN 频道优先级
        panel.child(text(() -> tr("gui.network.channel_priority"), 244, 43, 90));
        panel.child(
            new TextFieldWidget().value(channelPriority)
                .formatAsInteger(true)
                .numbersInt(-99, 99)
                .autoUpdateOnChange(false)
                .pos(340, 39)
                .size(114, 18));
        ParentWidget<?> rulePanel = new ParentWidget<>().size(470, 330)
            .setEnabledIf(w -> !field(selection, 1).isEmpty());
        panel.child(rulePanel);
        Supplier<NetworkRule> rule = () -> controller.rule(selected[0], selectedKey[0], false);
        Consumer<Consumer<NetworkRule>> edit = action -> {
            NetworkRule value = rule.get();
            if (value != null) {
                action.accept(value);
                controller.channels[selected[0]].enabled = true;
                controller.markDirty();
            }
        };
        StringSyncValue ruleState = read(sync, "rule_state", () -> {
            NetworkRule value = rule.get();
            Endpoint endpoint = controller.endpoint(selectedKey[0]);
            return (value == null ? 0 : value.mode) + ";"
                + (value == null ? 64 : value.rate)
                + ";"
                + (value == null ? 0 : value.priority)
                + ";"
                + (value != null && value.blacklist ? 1 : 0)
                + ";"
                + (value == null || value.matchMeta ? 1 : 0)
                + ";"
                + (value == null || value.matchNbt ? 1 : 0)
                + ";"
                + (value != null && value.matchOre ? 1 : 0)
                + ";"
                + (endpoint == null ? ""
                    : NetworkDeviceDisplay.name(endpoint)
                        .replace(';', ' '))
                + ";"
                + (value == null ? 0 : 1)
                + ";"
                + (value == null ? -1 : value.facing);
        });
        // #tr gui.network.select_cell
        // # Select a device/channel cell
        // # zh_CN 请选择设备与通道的交叉格
        rulePanel.child(
            text(
                () -> field(ruleState, 7).isEmpty() ? tr("gui.network.select_cell") : field(ruleState, 7),
                318,
                77,
                112));
        // #tr gui.network.create_connection
        // # Create connection
        // # zh_CN 创建连接
        rulePanel.child(
            button(
                sync,
                "create_rule",
                () -> tr("gui.network.create_connection"),
                () -> { if (controller.rule(selected[0], selectedKey[0], true) != null) controller.markDirty(); },
                244,
                99,
                100).setEnabledIf(w -> integer(ruleState, 8) == 0));
        ParentWidget<?> existingRulePanel = new ParentWidget<>().size(470, 330)
            .setEnabledIf(w -> integer(ruleState, 8) == 1);
        rulePanel.child(existingRulePanel);
        existingRulePanel.child(
            button(
                sync,
                "access_face",
                () -> accessFace(integer(ruleState, 9)),
                () -> edit.accept(value -> value.facing = value.facing >= 5 ? -1 : value.facing + 1),
                244,
                73,
                70));
        existingRulePanel.child(button(sync, "remove_rule", () -> "X", () -> {
            controller.channels[selected[0]].rules.remove(selectedKey[0]);
            controller.markDirty();
        }, 436, 73, 18).tooltip(t -> {
            // #tr gui.network.remove_rule
            // # Remove this channel connection. The physical device remains in the matrix.
            // # zh_CN 删除当前通道连接；物理设备仍保留在矩阵中。
            t.addLine(IKey.lang("gui.network.remove_rule"));
        }));
        existingRulePanel.child(
            button(
                sync,
                "mode",
                () -> mode(integer(ruleState, 0)),
                () -> edit.accept(value -> value.mode = (value.mode + 1) % 3),
                244,
                99,
                58));
        com.cleanroommc.modularui.value.sync.IntSyncValue amount = new com.cleanroommc.modularui.value.sync.IntSyncValue(
            () -> {
                NetworkRule current = rule.get();
                return current == null ? controller.channels[selected[0]].type == 0 ? 64 : 1000 : current.rate;
            },
            value -> {
                if (!controller.getWorldObj().isRemote) edit.accept(current -> current.rate = Math.max(1, value));
            }).allowC2S();
        sync.syncValue("transfer_amount", amount);
        existingRulePanel.child(
            new TextFieldWidget().value(amount)
                .formatAsInteger(true)
                .numbersInt(1, Integer.MAX_VALUE)
                .autoUpdateOnChange(false)
                .pos(310, 100)
                .size(114, 18));
        // #tr gui.network.item_unit
        // # items
        // # zh_CN 个
        existingRulePanel.child(
            text(
                () -> integer(channelState, 0) == 0 ? tr("gui.network.item_unit")
                    : integer(channelState, 0) == 1 ? "L" : "EU",
                430,
                103,
                24));
        // #tr gui.network.priority
        // # Priority %s
        // # zh_CN 优先级 %s
        existingRulePanel.child(text(() -> tr("gui.network.priority", field(ruleState, 2)), 244, 127, 116));
        existingRulePanel.child(
            button(
                sync,
                "priority_less",
                () -> "-",
                () -> edit.accept(value -> value.priority = Math.max(-99, value.priority - 1)),
                388,
                123,
                30));
        existingRulePanel.child(
            button(
                sync,
                "priority_more",
                () -> "+",
                () -> edit.accept(value -> value.priority = Math.min(99, value.priority + 1)),
                424,
                123,
                30));
        ParentWidget<?> filterPanel = new ParentWidget<>().size(470, 330)
            .setEnabledIf(w -> integer(channelState, 0) != 2);
        existingRulePanel.child(filterPanel);
        // #tr gui.network.blacklist
        // # Blacklist
        // # zh_CN 黑名单
        filterPanel.child(
            button(
                sync,
                "blacklist",
                () -> filterMode(integer(ruleState, 3)),
                () -> edit.accept(value -> value.blacklist = !value.blacklist),
                244,
                148,
                54));
        // #tr gui.network.ore
        // # OreDict
        // # zh_CN 矿辞
        filterPanel.child(
            button(
                sync,
                "ore",
                () -> flag(tr("gui.network.ore"), integer(ruleState, 6)),
                () -> edit.accept(value -> value.matchOre = !value.matchOre),
                302,
                148,
                46));
        // #tr gui.network.meta
        // # Meta
        // # zh_CN 元数据
        filterPanel.child(
            button(
                sync,
                "meta",
                () -> flag(tr("gui.network.meta"), integer(ruleState, 4)),
                () -> edit.accept(value -> value.matchMeta = !value.matchMeta),
                352,
                148,
                54));
        filterPanel.child(
            button(
                sync,
                "nbt",
                () -> flag("NBT", integer(ruleState, 5)),
                () -> edit.accept(value -> value.matchNbt = !value.matchNbt),
                410,
                148,
                44));
        NetworkFilterHandler filters = new NetworkFilterHandler(
            controller,
            () -> controller.channels[selected[0]].type == 0 ? rule.get() : null,
            () -> controller.channels[selected[0]].type == 0 ? rule.get() : null);
        for (int i = 0; i < 18; i++) {
            filterPanel.child(
                new com.cleanroommc.modularui.widgets.slot.PhantomItemSlot()
                    .slot(new NetworkFilterSlot(filters, i).singletonSlotGroup())
                    .setEnabledIf(w -> integer(channelState, 0) == 0)
                    .pos(260 + i % 9 * 18, 174 + i / 9 * 18));
            com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler fluidSync = new com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler(
                new NetworkFluidFilterTank(
                    controller,
                    () -> controller.channels[selected[0]].type == 1 ? rule.get() : null,
                    i)).phantom(true)
                        .controlsAmount(false);
            sync.syncValue("fluid_filter_" + i, fluidSync);
            filterPanel.child(
                new com.cleanroommc.modularui.widgets.slot.FluidSlot().syncHandler(fluidSync)
                    .setEnabledIf(w -> integer(channelState, 0) == 1)
                    .pos(260 + i % 9 * 18, 174 + i / 9 * 18)
                    .size(18, 18));
        }
        String[] clipboardStatus = { "" };
        String clipboardKey = com.xyp.gtnotgood.utils.enums.ModList.GTNotGood.getID() + ".networkRuleClipboard";
        // #tr gui.network.copy_rule
        // # Copy
        // # zh_CN 复制配置
        existingRulePanel.child(button(sync, "copy_rule", () -> tr("gui.network.copy_rule"), () -> {
            NetworkRule current = rule.get();
            if (current == null) return;
            net.minecraft.nbt.NBTTagCompound copy = current.write();
            copy.setInteger("channelType", controller.channels[selected[0]].type);
            sync.getPlayer()
                .getEntityData()
                .setTag(clipboardKey, copy);
            clipboardStatus[0] = "copied";
        }, 244, 216, 62));
        // #tr gui.network.paste_rule
        // # Paste
        // # zh_CN 粘贴配置
        existingRulePanel.child(button(sync, "paste_rule", () -> tr("gui.network.paste_rule"), () -> {
            NetworkRule current = rule.get();
            if (current == null) return;
            net.minecraft.nbt.NBTTagCompound playerData = sync.getPlayer()
                .getEntityData();
            if (!playerData.hasKey(clipboardKey)) {
                clipboardStatus[0] = "empty";
                return;
            }
            net.minecraft.nbt.NBTTagCompound copy = playerData.getCompoundTag(clipboardKey);
            if (copy.getInteger("channelType") != controller.channels[selected[0]].type) {
                clipboardStatus[0] = "type";
                return;
            }
            current.read((net.minecraft.nbt.NBTTagCompound) copy.copy());
            controller.channels[selected[0]].enabled = true;
            controller.markDirty();
            clipboardStatus[0] = "pasted";
        }, 310, 216, 62));
        StringSyncValue clipboardMessage = read(sync, "clipboard_status", () -> clipboardStatus[0]);
        existingRulePanel.child(text(() -> clipboardMessage(clipboardMessage.getValue()), 378, 221, 78));
        panel.child(
            SlotGroupWidget.playerInventory(true)
                .pos(260, 239));
        return panel;
    }

    static IWidget matrix(String snapshot, NetworkMatrixSelectionSync matrixSelection, StringSyncValue selection) {
        Flow rows = Flow.column()
            .width(208)
            .padding(0)
            .crossAxisAlignment(com.cleanroommc.modularui.utils.Alignment.CrossAxis.START)
            .coverChildrenHeight();
        if (snapshot == null || snapshot.isEmpty()) {
            // #tr gui.network.empty
            // # Attach connectors to inventories or tanks.
            // # zh_CN 请将连接器贴在物品容器或储罐上。
            rows.child(text(() -> tr("gui.network.empty"), 2, 2, 202));
        } else for (String entry : snapshot.split(";")) {
            String[] fields = entry.split(",", -1);
            String key = fields[0];
            String modes = fields[2];
            String label = new String(
                java.util.Base64.getDecoder()
                    .decode(fields[3]),
                java.nio.charset.StandardCharsets.UTF_8);
            ParentWidget<?> row = new ParentWidget<>().size(208, 24)
                .marginBottom(1)
                .background(new Rectangle().color(0xFF383F4B));
            ItemStack device = NetworkDeviceDisplay.decode(fields[1]);
            if (device != null) {
                row.child(
                    new com.cleanroommc.modularui.drawable.ItemDrawable(device).asWidget()
                        .pos(1, 4)
                        .size(16, 16)
                        .tooltip(t -> t.addLine(IKey.str(label))));
            }
            int face = Integer.parseInt(key.substring(key.lastIndexOf(':') + 1));
            row.child(text(() -> side(face), 19, 7, 16));
            for (int i = 0; i < 8; i++) {
                final int channel = i;
                row.child(
                    new ButtonWidget<>().pos(CHANNEL_OFFSET + i * CHANNEL_PITCH, 2)
                        .size(CHANNEL_WIDTH, 20)
                        .background(GTNGGuiTextures.MODERN_BUTTON_COMPACT)
                        .overlay(IKey.dynamic(() -> {
                            String value = modes.charAt(channel) == '1' ? "E"
                                : modes.charAt(channel) == '2' ? "I" : modes.charAt(channel) == '3' ? "-" : "";
                            return integer(selection, 0) == channel && field(selection, 1).equals(key)
                                ? "[" + value + "]"
                                : value;
                        })
                            .color(0xFF151B25))
                        .onMousePressed(mouseButton -> {
                            matrixSelection.select(key, channel);
                            return true;
                        })
                        .tooltip(t -> t.addLine(IKey.str(label))));
            }
            rows.child(row);
        }
        return new ListWidget<>().size(216, 246)
            .padding(0)
            .crossAxisAlignment(com.cleanroommc.modularui.utils.Alignment.CrossAxis.START)
            .child(rows);
    }

    private static String deviceLabel(Endpoint endpoint) {
        if (endpoint == null || endpoint.target() == null) return "";
        return endpoint.connector.getName() + " " + NetworkDeviceDisplay.name(endpoint) + " " + endpoint.key;
    }

    private static String filterMode(int state) {
        // #tr gui.network.whitelist
        // # Whitelist
        // # zh_CN 白名单
        if (state == 0) return tr("gui.network.whitelist");
        return tr("gui.network.blacklist");
    }

    private static String accessFace(int facing) {
        // #tr gui.network.auto_face
        // # Auto face
        // # zh_CN 自动方向
        return facing < 0 ? tr("gui.network.auto_face") : side(facing);
    }

    private static String clipboardMessage(String status) {
        // #tr gui.network.copied
        // # Copied
        // # zh_CN 已复制
        if ("copied".equals(status)) return tr("gui.network.copied");
        // #tr gui.network.pasted
        // # Pasted
        // # zh_CN 已粘贴
        if ("pasted".equals(status)) return tr("gui.network.pasted");
        // #tr gui.network.clipboard_empty
        // # Copy first
        // # zh_CN 请先复制
        if ("empty".equals(status)) return tr("gui.network.clipboard_empty");
        // #tr gui.network.clipboard_type
        // # Type mismatch
        // # zh_CN 频道类型不同
        if ("type".equals(status)) return tr("gui.network.clipboard_type");
        return "";
    }

    private static String flag(String label, int state) {
        return label + (state == 1 ? " ✓" : " ×");
    }

    public static ModularPanel connector(TileNetworkNode connector, PanelSyncManager sync) {
        ModularPanel panel = ModularPanel.defaultPanel("network_connector", 250, 200)
            .background(GTNGGuiTextures.MODERN_BACKGROUND);
        panel.child(
            new ParentWidget<>().pos(4, 4)
                .size(242, 192)
                .background(new Rectangle().color(0xFF202632)));
        // #tr gui.network.connector
        // # Network Connector
        // # zh_CN 网络连接器
        panel.child(text(() -> tr("gui.network.connector"), 10, 10, 230));
        StringSyncValue name = new StringSyncValue(connector::getName, connector::setName).allowC2S();
        sync.syncValue("name", name);
        panel.child(
            new TextFieldWidget().value(name)
                .setMaxLength(32)
                .pos(10, 30)
                .size(230, 18));
        for (int i = 0; i < 6; i++) {
            final int face = i;
            StringSyncValue state = read(sync, "face_" + i, () -> connector.faceEnabled(face) ? "1" : "0");
            panel.child(
                button(
                    sync,
                    "toggle_" + i,
                    () -> side(face) + ": " + enabled("1".equals(state.getValue())),
                    () -> connector.toggleFace(face),
                    10 + (i % 2) * 118,
                    60 + (i / 2) * 28,
                    112));
        }
        // #tr gui.network.connector_hint
        // # Configure routing channels at the controller.
        // # zh_CN 通道传输规则请在控制器中设置。
        panel.child(text(() -> tr("gui.network.connector_hint"), 10, 155, 230));
        return panel;
    }

    private static StringSyncValue read(PanelSyncManager sync, String key, Supplier<String> getter) {
        StringSyncValue value = new StringSyncValue(getter, ignored -> {});
        sync.syncValue(key, value);
        return value;
    }

    private static ButtonWidget<?> button(PanelSyncManager sync, String key, Supplier<String> label, Runnable action,
        int x, int y, int width) {
        InteractionSyncHandler handler = sync.getOrCreateSyncHandler(
            key,
            InteractionSyncHandler.class,
            () -> new InteractionSyncHandler().setOnMousePressed(mouse -> { if (!mouse.isClient()) action.run(); }));
        return new ButtonWidget<>().pos(x, y)
            .size(width, 20)
            .background(GTNGGuiTextures.MODERN_BUTTON_COMPACT)
            .overlay(
                IKey.dynamic(label)
                    .color(0xFF151B25))
            .syncHandler(handler);
    }

    private static IWidget text(Supplier<String> label, int x, int y, int width) {
        return IKey.dynamic(label)
            .asWidget()
            .pos(x, y)
            .size(width, 12)
            .color(0xFFE7E9F2);
    }

    private static String field(StringSyncValue value, int index) {
        String data = value.getValue();
        String[] fields = data == null ? new String[0] : data.split(";", -1);
        return index < fields.length ? fields[index] : "";
    }

    private static int integer(StringSyncValue value, int index) {
        try {
            return Integer.parseInt(field(value, index));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String tr(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }

    private static String type(int type) {
        // #tr gui.network.items
        // # Items
        // # zh_CN 物品
        if (type == 0) return tr("gui.network.items");
        // #tr gui.network.fluids
        // # Fluids
        // # zh_CN 流体
        if (type == 1) return tr("gui.network.fluids");
        return "GT EU";
    }

    private static String enabled(boolean enabled) {
        // #tr gui.network.enabled
        // # Enabled
        // # zh_CN 已启用
        if (enabled) return tr("gui.network.enabled");
        // #tr gui.network.disabled
        // # Disabled
        // # zh_CN 已禁用
        return tr("gui.network.disabled");
    }

    private static String mode(int mode) {
        // #tr gui.network.extract
        // # Extract
        // # zh_CN 提取
        if (mode == 1) return tr("gui.network.extract");
        // #tr gui.network.insert
        // # Insert
        // # zh_CN 插入
        if (mode == 2) return tr("gui.network.insert");
        // #tr gui.network.off
        // # Off
        // # zh_CN 关闭
        return tr("gui.network.off");
    }

    private static String status(int status) {
        // #tr gui.network.conflict
        // # Controller conflict
        // # zh_CN 控制器冲突
        if (status == 1) return tr("gui.network.conflict");
        // #tr gui.network.limit
        // # Network too large
        // # zh_CN 网络超出上限
        if (status == 2) return tr("gui.network.limit");
        // #tr gui.network.unloaded
        // # Unloaded boundary
        // # zh_CN 边界区块未加载
        if (status == 3) return tr("gui.network.unloaded");
        // #tr gui.network.ready
        // # Ready
        // # zh_CN 就绪
        return tr("gui.network.ready");
    }

    private static String side(int side) {
        switch (side) {
            // #tr gui.network.down
            // # Down
            // # zh_CN 下
            case 0:
                return tr("gui.network.down");
            // #tr gui.network.up
            // # Up
            // # zh_CN 上
            case 1:
                return tr("gui.network.up");
            // #tr gui.network.north
            // # North
            // # zh_CN 北
            case 2:
                return tr("gui.network.north");
            // #tr gui.network.south
            // # South
            // # zh_CN 南
            case 3:
                return tr("gui.network.south");
            // #tr gui.network.west
            // # West
            // # zh_CN 西
            case 4:
                return tr("gui.network.west");
            // #tr gui.network.east
            // # East
            // # zh_CN 东
            default:
                return tr("gui.network.east");
        }
    }
}
