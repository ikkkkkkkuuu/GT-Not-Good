/*
 * GUI adapted from Flux Networks 1.20 GuiFluxDeviceHome / GuiTabCore / FluxEditBox / SwitchButton.
 * Copyright (c) 2019-2021 BloCamLimb. GUI design/resources: CC BY-NC-SA 4.0.
 * See META-INF/flux-port/NOTICE.md for exact upstream revisions and adaptation scope.
 */
package com.xyp.gtnotgood.common.flux;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.gui.texture.SpriteTexture;
import com.xyp.ldlib.integration.modularui.ModernThemeAdapter;

/**
 * Modern Flux Networks GUI port using the project's LDLib texture bridge and MUI2 synchronization.
 * Original 176x166 content coordinates, 172px artwork, 16px navigation icons, outlined editors and
 * animated 16x8 switches are retained. GT-specific voltage/amperage live on the settings tab;
 * network pages show the existing GTNH account rather than creating a second energy ledger.
 */
public final class FluxConnectorGui {

    private static final int COLOR = 0xFF295E8A;

    private FluxConnectorGui() {}

    public static ModularPanel build(TileFluxConnector tile, PanelSyncManager sync) {
        ModularPanel panel = ModularPanel.defaultPanel("flux_connector", 176, 202)
            .background();
        panel.child(
            new ParentWidget<>().pos(2, 20)
                .size(172, 172)
                .background(
                    texture("gui_background", 0, 0, 512, 512),
                    ModernThemeAdapter.drawable(sprite("gui_frame", 0, 0, 512, 512).setColor(COLOR))));
        StringSyncValue name = edit(sync, "name", tile::customName, tile::setCustomName, tile);
        StringSyncValue volts = edit(sync, "voltage", () -> Long.toString(tile.voltage()), value -> {
            try {
                tile.configure(Long.parseLong(value), tile.amperage());
            } catch (NumberFormatException ignored) {}
        }, tile);
        StringSyncValue priority = edit(sync, "priority", () -> Integer.toString(tile.priority()), value -> {
            try {
                tile.setPriority(Long.parseLong(value));
            } catch (NumberFormatException ignored) {}
        }, tile);
        StringSyncValue amps = edit(sync, "amperage", () -> Long.toString(tile.amperage()), value -> {
            try {
                tile.configure(tile.voltage(), Long.parseLong(value));
            } catch (NumberFormatException ignored) {}
        }, tile);
        StringSyncValue status = new StringSyncValue(
            () -> tile.ownerName() + "\n"
                + tile.balance()
                + "\n"
                + tile.stored()
                + "\n"
                + tile.lastTransfer()
                + "\n"
                + tile.enabled()
                + "\n"
                + tile.connected()
                + "\n"
                + tile.redstoneRequired()
                + "\n"
                + tile.surgeMode()
                + "\n"
                + tile.chunkLoading());
        sync.syncValue("status", status);
        PagedWidget<?> pages = new PagedWidget<>().pos(0, 18)
            .size(176, 166);
        ParentWidget<?> home = new ParentWidget<>().size(176, 166);
        ParentWidget<?> account = new ParentWidget<>().size(176, 166);
        ParentWidget<?> neighbours = new ParentWidget<>().size(176, 166);
        ParentWidget<?> statistics = new ParentWidget<>().size(176, 166);
        ParentWidget<?> settings = new ParentWidget<>().size(176, 166);
        pages.addPage(home)
            .addPage(account)
            .addPage(neighbours)
            .addPage(statistics)
            .addPage(settings);
        panel.child(pages);
        int[] upstreamTabs = { 0, 1, 3, 4, 6 };
        String[] tabNames = {
            // #tr flux.gui.home
            // # Home
            // # zh_CN 主页
            tr("flux.gui.home"),
            // #tr flux.gui.selection
            // # Network Selection
            // # zh_CN 网络选择
            tr("flux.gui.selection"),
            // #tr flux.gui.connections
            // # Connections
            // # zh_CN 连接
            tr("flux.gui.connections"),
            // #tr flux.gui.statistics
            // # Network Statistics
            // # zh_CN 网络统计
            tr("flux.gui.statistics"),
            // #tr flux.gui.settings
            // # Settings
            // # zh_CN 设置
            tr("flux.gui.settings") };
        for (int i = 0; i < upstreamTabs.length; i++) {
            int page = i;
            IDrawable icon = texture("gui_icon", 64 * upstreamTabs[i], 192, 64, 64);
            panel.child(
                new ButtonWidget<>().pos(12 + 18 * upstreamTabs[i], 2)
                    .size(16, 16)
                    .background(icon)
                    .hoverBackground(icon)
                    .addTooltipLine(tabNames[i])
                    .onMousePressed(button -> {
                        pages.setPage(page);
                        return true;
                    }));
        }
        home.child(
            new ButtonWidget<>().pos(20, 8)
                .size(135, 12)
                .background(texture("gui_icon", 0, 320, 270, 24))
                .overlay(
                    IKey.dynamic(() -> accountName(status))
                        .color(0xFFFFFFFF))
                .onMousePressed(button -> {
                    pages.setPage(1);
                    return true;
                }));
        // #tr flux.gui.name
        // # Name:
        // # zh_CN 名称：
        field(home, tr("flux.gui.name"), name, 28, false);
        // #tr flux.gui.priority
        // # Priority:
        // # zh_CN 优先级：
        field(home, tr("flux.gui.priority"), priority, 45, true).setPattern(Pattern.compile("-?[0-9]*"));
        home.child(
            new com.cleanroommc.modularui.drawable.ItemDrawable(
                tile.isPlug() ? com.xyp.gtnotgood.utils.enums.GTNGItemList.FluxPlug.get(1)
                    : com.xyp.gtnotgood.utils.enums.GTNGItemList.FluxPoint.get(1)).asWidget()
                        .pos(10, 91)
                        .size(16, 16));
        home.child(text(() -> part(status, 3) + " EU/t", 30, 90, 130));
        // #tr flux.gui.buffer
        // # Buffer:
        // # zh_CN 缓存：
        home.child(text(() -> tr("flux.gui.buffer") + " " + part(status, 2) + " EU", 30, 100, 130));
        // #tr flux.gui.surge
        // # Surge Mode
        // # zh_CN 浪涌模式
        toggle(home, sync, tile, "surge", tr("flux.gui.surge"), () -> bool(status, 7), tile::toggleSurgeMode, 120);
        // #tr flux.gui.chunk_loading
        // # Chunk Loading
        // # zh_CN 区块加载
        toggle(
            home,
            sync,
            tile,
            "chunk_loading",
            tr("flux.gui.chunk_loading"),
            () -> bool(status, 8),
            tile::toggleChunkLoading,
            144);
        // #tr flux.gui.voltage
        // # Voltage:
        // # zh_CN 电压：
        field(settings, tr("flux.gui.voltage"), volts, 28, true);
        // #tr flux.gui.amperage
        // # Amperage:
        // # zh_CN 安培数：
        field(settings, tr("flux.gui.amperage"), amps, 45, true);
        // #tr flux.gui.enabled
        // # Enable Transfer
        // # zh_CN 启用传输
        toggle(
            settings,
            sync,
            tile,
            "enabled",
            tr("flux.gui.enabled"),
            () -> bool(status, 4),
            tile::toggleEnabled,
            120);
        // #tr flux.gui.connected
        // # Connect Network
        // # zh_CN 连接网络
        toggle(
            settings,
            sync,
            tile,
            "connected",
            tr("flux.gui.connected"),
            () -> bool(status, 5),
            tile::toggleConnected,
            132);
        // #tr flux.gui.redstone
        // # Require Redstone
        // # zh_CN 需要红石信号
        toggle(
            settings,
            sync,
            tile,
            "redstone",
            tr("flux.gui.redstone"),
            () -> bool(status, 6),
            tile::toggleRedstone,
            144);

        account.child(text(() -> accountName(status), 16, 12, 144));
        // #tr flux.gui.account
        // # GTNH wireless account
        // # zh_CN GTNH 无线电网账户
        account.child(text(() -> tr("flux.gui.account"), 16, 32, 144));
        account.child(text(() -> part(status, 0), 16, 48, 144));
        // #tr flux.gui.team
        // # Uses the owner's GT team balance
        // # zh_CN 共用所有者的 GT 团队余额
        account.child(text(() -> tr("flux.gui.team"), 16, 68, 144));
        toggle(
            account,
            sync,
            tile,
            "account_connected",
            tr("flux.gui.connected"),
            () -> bool(status, 5),
            tile::toggleConnected,
            132);
        String[] directions = { "↓", "↑", "N", "S", "W", "E" };
        for (int i = 0; i < 6; i++) {
            int side = i;
            StringSyncValue target = new StringSyncValue(() -> tile.neighbourName(side));
            sync.syncValue("neighbour_" + i, target);
            neighbours.child(text(() -> directions[side] + "  " + target.getStringValue(), 16, 12 + 22 * i, 144));
        }
        // #tr flux.gui.balance
        // # Wireless balance (EU)
        // # zh_CN 无线电网余额（EU）
        statistics.child(text(() -> tr("flux.gui.balance"), 16, 12, 144));
        statistics.child(text(() -> part(status, 1), 16, 30, 144));
        statistics.child(text(() -> part(status, 3) + " EU/t", 16, 60, 144));
        // #tr flux.gui.owner_edit
        // # Only the owner can edit settings
        // # zh_CN 仅所有者可修改设置
        statistics.child(text(() -> tr("flux.gui.owner_edit"), 16, 110, 144));
        // #tr flux.gui.voltage_note
        // # Output voltage must match the receiver
        // # zh_CN 输出电压须匹配接收设备
        statistics.child(text(() -> tr("flux.gui.voltage_note"), 16, 130, 144));
        return panel;
    }

    private static String accountName(StringSyncValue value) {
        return part(value, 0) + " / GT EU";
    }

    private static String part(StringSyncValue value, int i) {
        String[] parts = value.getStringValue()
            .split("\n", -1);
        return i < parts.length ? parts[i] : "";
    }

    private static boolean bool(StringSyncValue value, int i) {
        return Boolean.parseBoolean(part(value, i));
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }

    private static com.cleanroommc.modularui.api.widget.IWidget text(Supplier<String> value, int x, int y, int width) {
        return IKey.dynamic(value)
            .color(0xFFFFFFFF)
            .asWidget()
            .pos(x, y)
            .size(width, 12)
            .textAlign(com.cleanroommc.modularui.utils.Alignment.TopLeft);
    }

    private static StringSyncValue edit(PanelSyncManager sync, String key, Supplier<String> getter,
        Consumer<String> setter, TileFluxConnector tile) {
        StringSyncValue value = new StringSyncValue(getter, incoming -> {
            // The callback executes after the GUI has bound its container/player.
            if (tile.getWorldObj() != null && !tile.getWorldObj().isRemote && tile.canConfigure(sync.getPlayer())) {
                setter.accept(incoming);
            }
        }).allowC2S();
        sync.syncValue(key, value);
        return value;
    }

    /** Ports FluxEditBox's 144x12 bordered row, prefix label and live editing interaction. */
    private static TextFieldWidget field(ParentWidget<?> parent, String label, StringSyncValue value, int y,
        boolean numeric) {
        ParentWidget<?> row = new ParentWidget<>().pos(15, y - 1)
            .size(146, 14)
            .background(new Rectangle().color(COLOR));
        row.child(
            new ParentWidget<>().pos(1, 1)
                .size(144, 12)
                .background(new Rectangle().color(0xE0101114)));
        int header = cpw.mods.fml.common.FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient() ? headerWidth(label) : 50;
        row.child(
            IKey.str(label)
                .color(COLOR)
                .asWidget()
                .pos(5, 3)
                .size(header, 9)
                .textAlign(com.cleanroommc.modularui.utils.Alignment.TopLeft));
        TextFieldWidget field = new TextFieldWidget().value(value)
            .autoUpdateOnChange(true)
            .setMaxLength(numeric ? 16 : 24)
            .setTextColor(0xFFFFFFFF)
            .background()
            .pos(header + 5, 3)
            .size(138 - header, 9)
            .setTextAlignment(com.cleanroommc.modularui.utils.Alignment.TopLeft);
        if (numeric) field.setPattern(Pattern.compile("[0-9]*"));
        row.child(field);
        parent.child(row);
        return field;
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private static int headerWidth(String label) {
        return Math.min(110, net.minecraft.client.Minecraft.getMinecraft().fontRenderer.getStringWidth(label));
    }

    private static void toggle(ParentWidget<?> parent, PanelSyncManager sync, TileFluxConnector tile, String id,
        String label, BooleanSupplier checked, Runnable action, int y) {
        parent.child(
            IKey.str(label)
                .color(COLOR)
                .asWidget()
                .pos(20, y)
                .size(116, 9)
                .textAlign(com.cleanroommc.modularui.utils.Alignment.TopLeft));
        InteractionSyncHandler handler = new InteractionSyncHandler().setOnMousePressed(
            mouse -> { if (!mouse.isClient() && tile.canConfigure(sync.getPlayer())) action.run(); });
        sync.syncValue(id, handler);
        float[] offset = { checked.getAsBoolean() ? 1 : 0 };
        long[] previous = { 0 };
        IDrawable drawable = (context, x, drawY, width, height, theme) -> {
            long now = System.nanoTime();
            float step = previous[0] == 0 ? 1 : Math.min(1, (now - previous[0]) / 200_000_000F);
            previous[0] = now;
            offset[0] = Math.max(0, Math.min(1, offset[0] + (checked.getAsBoolean() ? step : -step)));
            int thumb = Math.round(offset[0] * 8);
            if (thumb > 0) sprite("gui_icon", 320, 256, thumb * 8, 32).setColor(COLOR)
                .draw(0, 0, x, drawY, thumb * 2, 8);
            sprite("gui_icon", 256, 256, 64, 32).draw(0, 0, x, drawY, 16, 8);
            sprite("gui_icon", 256, 288, 32, 32).draw(0, 0, x + thumb, drawY, 8, 8);
        };
        parent.child(
            new ButtonWidget<>().pos(140, y)
                .size(16, 8)
                .background(drawable)
                .hoverBackground(drawable)
                .syncHandler(handler));
    }

    private static SpriteTexture sprite(String name, int x, int y, int width, int height) {
        return new SpriteTexture(
            ModList.GTNotGood.getResourceLocation("textures/gui/flux/" + name + ".png"),
            512,
            512,
            0).setSprite(x, y, width, height);
    }

    private static IDrawable texture(String name, int x, int y, int width, int height) {
        return ModernThemeAdapter.drawable(sprite(name, x, y, width, height));
    }
}
