// SPDX-License-Identifier: LGPL-3.0-only
// Upstream screen/widget mappings and original artwork: META-INF/me-stock-port/NOTICE.md
package com.xyp.gtnotgood.common.mestock;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.storage.data.IAEStack;
import appeng.util.ReadableNumberConverter;
import cpw.mods.fml.relauncher.Side;

/** Original upstream pixel layouts and interactions, with bounded synchronization of visible terminal lines. */
final class StockGui {

    private static final int maximumTerminalRows = 64;
    private static int terminalStyle = 3;

    private StockGui() {}

    static ModularPanel build(StockHost host, StockGuiFactory.Data data, PanelSyncManager sync) {
        if (host instanceof TileMERequester tile) return requester(tile, data, sync);
        if (host instanceof PartThresholdLevelEmitter emitter) return emitter(emitter, data, sync);
        return threshold((PartThresholdExportBus) host, data, sync);
    }

    private static ModularPanel panel(String id, IKey title, int width, int height, IDrawable frame,
        boolean inventory) {
        var panel = ModularPanel.defaultPanel(id, width, height)
            .background(frame)
            .disableHoverBackground();
        panel.child(
            title.color(0xff404040)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(8, 6)
                .size(width - 16, 9));
        if (inventory) {
            playerInventory(panel);
        }
        return panel;
    }

    private static void playerInventory(ModularPanel panel) {
        panel.child(
            IKey.lang("container.inventory")
                .color(0xff404040)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .left(8)
                .bottom(86)
                .size(162, 9));
        panel.child(
            SlotGroupWidget.playerInventory((index, slot) -> slot.background(IDrawable.EMPTY))
                .left(7)
                .bottom(9));
    }

    private static ModularPanel threshold(PartThresholdExportBus bus, StockGuiFactory.Data data,
        PanelSyncManager sync) {
        var panel = panel("threshold_export", IKey.lang(bus.stockTitle()), 176, 253, StockGuiAssets.bus, true);
        panel.child(
            StockText.SetAmount.label()
                .color(0xff404040)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(10, 17)
                .size(150, 9)
                .scale(0.6f));
        var slots = new IntSyncValue(bus::stockSlots);
        sync.syncValue("slots", slots);
        for (int i = 0; i < bus.stockConfig()
            .size(); i++) {
            int row = i;
            var token = new DeviceToken(() -> "local:" + row);
            sync.syncValue("identity", row, token);
            var sample = sample(
                sync,
                row,
                token,
                data,
                () -> bus.stockConfig()
                    .key(row),
                () -> row < bus.stockSlots(),
                (mouse, cursor) -> {
                    if (mouse.mouseButton == 2) StockGuiFactory.instance.openAmount(sync.getPlayer(), bus, row);
                    else if (mouse.shift || cursor == null && mouse.mouseButton == 1) bus.stockConfig()
                        .setKey(row, null);
                    else if (cursor != null) bus.stockConfig()
                        .setKey(row, StockResources.sample(cursor));
                });
            var amount = new LongSyncValue(
                () -> bus.stockConfig()
                    .amount(row));
            sync.syncValue("reserve", row, amount);
            panel.child(
                new SampleSlot(sample).displayAmount(amount::getLongValue)
                    .pos(7 + row % 9 * 18, 28 + row / 9 * 18)
                    .background(row < 18 ? IDrawable.EMPTY : StockGuiAssets.slot)
                    .setEnabledIf(w -> row < slots.getIntValue())
                    .tooltip(
                        t -> t.addLine(StockText.SetAmount.label())
                            .addLine(IKey.dynamic(() -> StockText.Target.text() + ": " + amount.getLongValue()))));
        }
        panel.child(
            StockGuiAssets.upgrades(8)
                .asWidget()
                .pos(174, 0)
                .size(28, 156)
                .excludeAreaInRecipeViewer());
        var upgrades = new InvWrapper(bus.getInventoryByName("upgrades"));
        for (int i = 0; i < 8; i++) panel.child(
            new ItemSlot().slot(new ModularSlot(upgrades, i).singletonSlotGroup())
                .background(IDrawable.EMPTY)
                .pos(174, 5 + i * 18)
                .excludeAreaInRecipeViewer());
        var redstone = new IntSyncValue(
            () -> bus.getRSMode()
                .ordinal());
        var hasRedstone = new BooleanSyncValue(() -> bus.getInstalledUpgrades(Upgrades.REDSTONE) > 0);
        var scheduling = new IntSyncValue(
            () -> ((SchedulingMode) bus.getConfigManager()
                .getSetting(Settings.SCHEDULING_MODE)).ordinal());
        var above = new BooleanSyncValue(bus::above);
        sync.syncValue("redstone", redstone);
        sync.syncValue("hasRedstone", hasRedstone);
        sync.syncValue("scheduling", scheduling);
        sync.syncValue("above", above);
        panel.child(
            toolbar(
                sync,
                "redstoneMode",
                data,
                -18,
                1,
                () -> StockGuiAssets.icon(
                    redstone.getIntValue() == RedstoneMode.HIGH_SIGNAL.ordinal() ? 16
                        : redstone.getIntValue() == RedstoneMode.LOW_SIGNAL.ordinal() ? 0 : 48,
                    0),
                () -> redstoneText(redstone.getIntValue()),
                mouse -> {
                    RedstoneMode[] modes = { RedstoneMode.IGNORE, RedstoneMode.HIGH_SIGNAL, RedstoneMode.LOW_SIGNAL };
                    int old = bus.getRSMode() == modes[1] ? 1 : bus.getRSMode() == modes[2] ? 2 : 0;
                    bus.getConfigManager()
                        .putSetting(Settings.REDSTONE_CONTROLLED, modes[(old + 1) % 3]);
                    bus.stockChanged();
                }).setEnabledIf(w -> hasRedstone.getBoolValue()));
        panel.child(
            toolbarPosition(
                toolbar(
                    sync,
                    "schedule",
                    data,
                    -18,
                    23,
                    () -> StockGuiAssets.icon(scheduling.getIntValue() * 16, 240),
                    () -> new String[] { "Default", "Round robin", "Random" }[scheduling.getIntValue()],
                    mouse -> bus.getConfigManager()
                        .putSetting(
                            Settings.SCHEDULING_MODE,
                            SchedulingMode.values()[(scheduling.getIntValue() + 1) % 3])),
                hasRedstone,
                23,
                1));
        panel.child(
            toolbarPosition(
                toolbar(
                    sync,
                    "mode",
                    data,
                    -18,
                    45,
                    () -> StockGuiAssets.thresholdIcon(above.getBoolValue()),
                    () -> above.getBoolValue() ? StockText.Above.text() : StockText.Below.text(),
                    mouse -> bus.setAbove(!bus.above())),
                hasRedstone,
                45,
                23));
        return panel;
    }

    static ModularPanel amount(PartThresholdExportBus bus, int row, StockGuiFactory.Data data, PanelSyncManager sync) {
        var fluid = new BooleanSyncValue(
            () -> bus.stockConfig()
                .key(row) != null && bus.stockConfig()
                    .key(row)
                    .isFluid());
        sync.syncValue("fluid", fluid);
        String[] draft = { StockNumbers.format(
            bus.stockConfig()
                .amount(row)) };
        var value = new StringSyncValue(() -> draft[0], text -> { if (authorized(data, sync)) draft[0] = text; })
            .allowC2S();
        sync.syncValue("amount", value);
        var panel = panel("threshold_amount", StockText.SelectAmount.label(), 176, 107, StockGuiAssets.amount, false);
        Runnable save = () -> {
            try {
                long amount = StockNumbers.parse(draft[0]);
                if (amount == 0) bus.stockConfig()
                    .setKey(row, null);
                else bus.stockConfig()
                    .setAmount(row, amount);
                StockGuiFactory.instance.open(sync.getPlayer(), bus);
            } catch (ArithmeticException | NumberFormatException ignored) {}
        };
        sync.registerSyncedAction("saveAmount", false, true, buffer -> {
            if (authorized(data, sync)) {
                draft[0] = NetworkUtils.readStringSafe(buffer);
                save.run();
            }
        });
        var entry = field(value);
        ((ConfirmField) entry).fluid = fluid::getBoolValue;
        panel.child(
            entry.pos(48, 55)
                .size(66, 12));
        ((ConfirmField) entry).confirm = () -> sync
            .callSyncedAction("saveAmount", buffer -> NetworkUtils.writeStringSafe(buffer, entry.getText()));
        panel.child(
            IKey.str("L")
                .color(0xff545454)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(108, 57)
                .size(8, 9)
                .setEnabledIf(w -> fluid.getBoolValue()));
        panel.child(
            new ItemSlot().slot(
                new ModularSlot(
                    new SampleInventory(
                        () -> bus.stockConfig()
                            .key(row),
                        data),
                    0).singletonSlotGroup()
                        .canPut(false)
                        .canTake(false))
                .background(IDrawable.EMPTY)
                .pos(22, 52));
        int[] x = { 20, 48, 82, 120 }, widths = { 22, 28, 32, 38 };
        long[] steps = { 1, 10, 100, 1000 }, stacks = { 1, 16, 32, 64 };
        for (int i = 0; i < 4; i++) for (int direction = 0; direction < 2; direction++) {
            int index = i, sign = direction == 0 ? 1 : -1;
            panel.child(
                button(sync, "step" + i + direction, data, () -> (sign > 0 ? "+" : "−") + steps[index], mouse -> {
                    try {
                        long current = StockNumbers.parse(draft[0]);
                        long step = (mouse.shift || mouse.ctrl ? stacks[index] : steps[index]);
                        draft[0] = StockNumbers
                            .format(sign > 0 ? StockResources.add(current, step) : Math.max(0, current - step));
                    } catch (ArithmeticException | NumberFormatException ignored) {}
                }).pos(x[i], direction == 0 ? 30 : 72)
                    .size(widths[i], 20));
        }
        panel.child(
            clientButton(() -> ((ConfirmField) entry).confirm.run()).overlay(StockText.Set.label())
                .pos(120, 51)
                .size(38, 20));
        panel.child(
            button(sync, "back", data, () -> "", mouse -> StockGuiFactory.instance.open(sync.getPlayer(), bus))
                .pos(152, -5)
                .size(20, 20)
                .background(StockGuiAssets.texture("states", 256, 256, 160, 192, 20, 20))
                .overlay(new ItemDrawable(GTNGItemList.ThresholdExportBus.get(1)))
                .excludeAreaInRecipeViewer());
        return panel;
    }

    private static ModularPanel emitter(PartThresholdLevelEmitter emitter, StockGuiFactory.Data data,
        PanelSyncManager sync) {
        var panel = panel("threshold_emitter", IKey.lang(emitter.stockTitle()), 176, 186, StockGuiAssets.emitter, true);
        var token = new DeviceToken(() -> "emitter");
        var fluid = new BooleanSyncValue(
            () -> emitter.stockConfig()
                .key(0) != null && emitter.stockConfig()
                    .key(0)
                    .isFluid());
        sync.syncValue("identity", token);
        sync.syncValue("fluid", fluid);
        panel.child(
            StockText.Upper.label()
                .color(0xff404040)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(20, 23)
                .size(100, 9));
        panel.child(
            StockText.Lower.label()
                .color(0xff404040)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(20, 58)
                .size(100, 9));
        panel.child(
            quantity(sync, "upper", token, fluid::getBoolValue, emitter::upper, emitter::setUpper, data, () -> true)
                .pos(20, 34)
                .size(83, 12));
        panel.child(
            quantity(
                sync,
                "lower",
                token,
                fluid::getBoolValue,
                () -> emitter.stockConfig()
                    .amount(0),
                value -> emitter.stockConfig()
                    .setAmount(0, value),
                data,
                () -> true).pos(20, 69)
                    .size(83, 12));
        panel.child(
            IKey.str("L")
                .color(0xff545454)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(98, 36)
                .size(8, 9)
                .setEnabledIf(w -> fluid.getBoolValue()));
        panel.child(
            IKey.str("L")
                .color(0xff545454)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(98, 71)
                .size(8, 9)
                .setEnabledIf(w -> fluid.getBoolValue()));
        var sample = sample(
            sync,
            0,
            token,
            data,
            () -> emitter.stockConfig()
                .key(0),
            () -> true,
            (mouse, cursor) -> {
                if (mouse.shift || cursor == null && mouse.mouseButton == 1) emitter.stockConfig()
                    .setKey(0, null);
                else if (cursor != null) emitter.stockConfig()
                    .setKey(0, StockResources.sample(cursor));
            });
        panel.child(
            new SampleSlot(sample).background(IDrawable.EMPTY)
                .pos(136, 46));
        var low = new BooleanSyncValue(emitter::lowSignal);
        sync.syncValue("low", low);
        panel.child(
            toolbar(
                sync,
                "signalMode",
                data,
                -18,
                1,
                () -> StockGuiAssets.icon(low.getBoolValue() ? 208 : 192, 0),
                () -> low.getBoolValue() ? StockText.LowSignal.text() : StockText.HighSignal.text(),
                mouse -> emitter.setLowSignal(!emitter.lowSignal())));
        return panel;
    }

    private static ModularPanel requester(TileMERequester tile, StockGuiFactory.Data data, PanelSyncManager sync) {
        int rows = tile.stockConfig()
            .size();
        var title = new StringSyncValue(
            () -> tile.name()
                .isEmpty() ? StockText.Requester.text() : tile.name());
        sync.syncValue("title", title);
        var panel = panel(
            "me_requester",
            IKey.dynamic(title::getValue),
            195,
            120 + rows * 19,
            StockGuiAssets.requester(false, () -> rows, index -> true),
            true);
        panel.child(
            StockGuiAssets.texture("big_scroller_disabled", 12, 15, 0, 0, 12, 15)
                .asWidget()
                .pos(175, 18)
                .size(12, 15));
        for (int i = 0; i < rows; i++) {
            int row = i;
            requestRow(panel, data, sync, i, () -> new StockRequesterView.Line(tile, row, ""), () -> true);
        }
        return panel;
    }

    static ModularPanel terminal(StockGuiFactory.Data data, PanelSyncManager sync) {
        var view = new StockRequesterView(data);
        var rowCount = new ClientRows(() -> view.visibleRows, value -> {
            if (data.getWorld().isRemote || authorized(data, sync)) {
                view.visibleRows = Math.max(3, Math.min(maximumTerminalRows, value));
                view.setScroll(view.scroll);
            }
        }).allowC2S();
        sync.syncValue("rows", rowCount);
        var kinds = new IntSyncValue[maximumTerminalRows];
        for (int i = 0; i < maximumTerminalRows; i++) {
            int row = i;
            kinds[i] = new IntSyncValue(() -> {
                var line = view.line(row);
                return line == null ? 0 : line.tile == null ? 1 : 2;
            });
            sync.syncValue("kind", i, kinds[i]);
        }
        var panel = new TerminalPanel(rowCount);
        panel.size(195, 215)
            .background(StockGuiAssets.requester(true, rowCount::getIntValue, index -> kinds[index].getIntValue() == 2))
            .disableHoverBackground();
        panel.child(
            IKey.lang("item.requester_terminal.name")
                .color(0xff404040)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(8, 6)
                .size(94, 9));
        playerInventory(panel);
        var query = new StringSyncValue(() -> view.query, value -> {
            if (authorized(data, sync)) {
                view.query = value.length() > 64 ? value.substring(0, 64) : value;
                view.scroll = 0;
            }
        }).allowC2S();
        sync.syncValue("search", query);
        var search = new ConfirmField();
        search.clearWithRightClick = true;
        panel.child(
            search.value(query)
                .autoUpdateOnChange(true)
                .setMaxLength(64)
                .background(
                    (context, x, y, w, h, theme) -> StockGuiAssets.textField(search.isFocused())
                        .draw(context, x, y, w, h, theme))
                .padding(2)
                .setTextAlignment(Alignment.CenterLeft)
                .setTextColor(0xffffffff)
                .hintText(StockText.Search.text())
                .setFocusOnGuiOpen(true)
                .pos(104, 4)
                .size(65, 12));
        var maximum = new IntSyncValue(view::maximumScroll);
        var scroll = new IntSyncValue(
            () -> view.scroll,
            value -> { if (authorized(data, sync)) view.setScroll(value); }).allowC2S();
        sync.syncValue("maximumScroll", maximum);
        sync.syncValue("scroll", scroll);
        var wheel = new ScrollCapture(scroll, maximum).pos(7, 20)
            .size(180, 95)
            .background(IDrawable.EMPTY);
        panel.child(wheel);
        panel.wheel = wheel;
        var slider = new SliderWidget().value(scroll)
            .bounds(0, 1)
            .setAxis(GuiAxis.Y)
            .stopper(1)
            .sliderSize(12, 15)
            .sliderTexture(
                (context, x, y, w, h, theme) -> StockGuiAssets
                    .texture(maximum.getIntValue() > 0 ? "big_scroller" : "big_scroller_disabled", 12, 15, 0, 0, 12, 15)
                    .draw(context, x, y, w, h, theme))
            .background(IDrawable.EMPTY)
            .pos(175, 18)
            .size(12, 96)
            .onUpdateListener(widget -> widget.bounds(0, Math.max(1, maximum.getIntValue())));
        panel.child(slider);
        panel.slider = slider;
        panel.child(clientButton(() -> {
            terminalStyle = terminalStyle % 4 + 1;
            var screen = panel.getScreen();
            screen.onResize(
                screen.getScreenArea()
                    .w(),
                screen.getScreenArea()
                    .h());
        }).pos(-18, 1)
            .size(18, 20)
            .background(StockGuiAssets.toolbar)
            .hoverBackground(StockGuiAssets.toolbarHover)
            .overlay(
                (context, x, y, w, h, theme) -> StockGuiAssets.icon((terminalStyle - 1) * 16, 208)
                    .draw(context, x + 1, y + 1, 16, 16, theme))
            .excludeAreaInRecipeViewer()
            .tooltip(
                t -> t.addLine(
                    IKey.dynamic(
                        () -> new StockText[] { StockText.StyleSmall, StockText.StyleMedium, StockText.StyleTall,
                            StockText.StyleFull }[terminalStyle - 1].text()))));
        for (int i = 0; i < maximumTerminalRows; i++) {
            int row = i;
            var header = new StringSyncValue(() -> {
                var line = view.line(row);
                return line == null ? "" : line.header;
            });
            sync.syncValue("header", i, header);
            panel.child(
                IKey.dynamic(header::getValue)
                    .color(0xff404040)
                    .asWidget()
                    .alignment(Alignment.TopLeft)
                    .shadow(false)
                    .pos(10, 26 + i * 19)
                    .size(156, 9)
                    .setEnabledIf(w -> row < rowCount.getIntValue() && kinds[row].getIntValue() == 1));
            requestRow(
                panel,
                data,
                sync,
                i,
                () -> view.line(row),
                () -> row < rowCount.getIntValue() && kinds[row].getIntValue() == 2);
        }
        panel.child(
            StockText.NoRequesters.label()
                .color(0xff404040)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(10, 26)
                .size(156, 9)
                .setEnabledIf(w -> kinds[0].getIntValue() == 0));
        return panel;
    }

    private static void requestRow(ModularPanel panel, StockGuiFactory.Data data, PanelSyncManager sync, int index,
        Supplier<StockRequesterView.Line> reference, BooleanSupplier visible) {
        int y = 20 + index * 19;
        var token = new DeviceToken(() -> {
            var line = reference.get();
            return line == null ? "" : line.token();
        });
        var enabled = new BooleanSyncValue(
            () -> valid(reference.get()) && reference.get().tile.stockConfig()
                .enabled(reference.get().row));
        var fluid = new BooleanSyncValue(
            () -> valid(reference.get()) && reference.get().tile.stockConfig()
                .key(reference.get().row) != null
                && reference.get().tile.stockConfig()
                    .key(reference.get().row)
                    .isFluid());
        var state = new IntSyncValue(
            () -> valid(reference.get()) ? reference.get().tile.status(reference.get().row) : TileMERequester.empty);
        var stored = new LongSyncValue(
            () -> valid(reference.get()) ? reference.get().tile.stored(reference.get().row) : 0);
        var pending = new LongSyncValue(
            () -> valid(reference.get()) ? reference.get().tile.pending(reference.get().row) : 0);
        sync.syncValue("identity", index, token);
        sync.syncValue("enabled", index, enabled);
        sync.syncValue("fluid", index, fluid);
        sync.syncValue("status", index, state);
        sync.syncValue("stored", index, stored);
        sync.syncValue("pending", index, pending);
        BooleanSupplier permitted = () -> valid(reference.get())
            && StockGuiFactory.instance.canEdit(sync.getPlayer(), data, reference.get().tile);
        var sample = sample(
            sync,
            index,
            token,
            data,
            () -> valid(reference.get()) ? reference.get().tile.stockConfig()
                .key(reference.get().row) : null,
            permitted,
            (mouse, cursor) -> {
                var line = reference.get();
                if (mouse.shift || cursor == null && mouse.mouseButton == 1) line.tile.stockConfig()
                    .setKey(line.row, null);
                else if (cursor != null) line.tile.stockConfig()
                    .setKey(line.row, StockResources.sample(cursor));
            });
        panel.child(
            new SampleSlot(sample).background(IDrawable.EMPTY)
                .pos(26, y)
                .setEnabledIf(w -> visible.getAsBoolean()));
        var target = new StringSyncValue(
            () -> StockNumbers.format(
                valid(reference.get()) ? reference.get().tile.stockConfig()
                    .amount(reference.get().row) : 0)).allowC2S();
        var batch = new StringSyncValue(
            () -> StockNumbers.format(
                valid(reference.get()) ? reference.get().tile.stockConfig()
                    .batch(reference.get().row) : 0)).allowC2S();
        sync.syncValue("target" + index, target);
        sync.syncValue("batch" + index, batch);
        var targetField = field(target);
        var batchField = field(batch);
        ((ConfirmField) targetField).fluid = fluid::getBoolValue;
        ((ConfirmField) batchField).fluid = fluid::getBoolValue;
        sync.registerSyncedAction("submit" + index + "Action", false, true, buffer -> {
            String identity = NetworkUtils.readStringSafe(buffer), amountText = NetworkUtils.readStringSafe(buffer),
                batchText = NetworkUtils.readStringSafe(buffer);
            if (!token.matches(identity) || !authorized(data, sync) || !permitted.getAsBoolean()) return;
            try {
                long amount = StockNumbers.parse(amountText), batchAmount = StockNumbers.parse(batchText);
                var line = reference.get();
                line.tile.stockConfig()
                    .setAmounts(line.row, amount, batchAmount);
            } catch (ArithmeticException | NumberFormatException ignored) {}
        });
        Runnable submit = () -> sync.callSyncedAction("submit" + index + "Action", buffer -> {
            NetworkUtils.writeStringSafe(buffer, token.getValue());
            NetworkUtils.writeStringSafe(buffer, targetField.getText());
            NetworkUtils.writeStringSafe(buffer, batchField.getText());
        });
        ((ConfirmField) targetField).confirm = submit;
        ((ConfirmField) batchField).confirm = submit;
        token.changeListener(() -> {
            if (!data.getWorld().isRemote) return;
            if (targetField.isFocused() || batchField.isFocused()) targetField.getContext()
                .removeFocus();
            targetField.setText(target.getValue());
            batchField.setText(batch.getValue());
        });
        panel.child(
            targetField.pos(46, y)
                .size(52, 12)
                .setEnabledIf(w -> visible.getAsBoolean())
                .tooltip(t -> t.addLine(StockText.Target.label())));
        panel.child(
            batchField.pos(100, y)
                .size(52, 12)
                .setEnabledIf(w -> visible.getAsBoolean())
                .tooltip(t -> t.addLine(StockText.Batch.label())));
        panel.child(
            IKey.str("L")
                .color(0xff545454)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(90, y + 2)
                .size(8, 9)
                .setEnabledIf(w -> visible.getAsBoolean() && fluid.getBoolValue()));
        panel.child(
            IKey.str("L")
                .color(0xff545454)
                .asWidget()
                .alignment(Alignment.TopLeft)
                .shadow(false)
                .pos(144, y + 2)
                .size(8, 9)
                .setEnabledIf(w -> visible.getAsBoolean() && fluid.getBoolValue()));
        panel.child(tokenButton(sync, "toggle" + index, token, data, () -> {
            if (permitted.getAsBoolean()) {
                var line = reference.get();
                line.tile.stockConfig()
                    .setEnabled(
                        line.row,
                        !line.tile.stockConfig()
                            .enabled(line.row));
            }
        }).pos(10, y + 2)
            .size(14, 14)
            .background(
                (context, x, py, w, h, theme) -> StockGuiAssets.checkbox(enabled.getBoolValue(), false)
                    .draw(context, x, py, w, h, theme))
            .hoverBackground(
                (context, x, py, w, h, theme) -> StockGuiAssets.checkbox(enabled.getBoolValue(), true)
                    .draw(context, x, py, w, h, theme))
            .setEnabledIf(w -> visible.getAsBoolean())
            .tooltip(t -> t.addLine(StockText.RequesterHint.label())));
        panel.child(
            clientButton(submit).pos(154, y)
                .size(12, 12)
                .background(StockGuiAssets.submit(false))
                .hoverBackground(StockGuiAssets.submit(true))
                .setEnabledIf(w -> visible.getAsBoolean())
                .tooltip(t -> t.addLine(StockText.Submit.label())));
        panel.child(
            ((IDrawable) (context, x, py, w, h, theme) -> new Rectangle()
                .color(statusColor(state.getIntValue(), enabled.getBoolValue()))
                .draw(context, x, py, w, h, theme)).asWidget()
                    .pos(47, y + 15)
                    .size(118, 2)
                    .setEnabledIf(w -> visible.getAsBoolean())
                    .tooltip(
                        t -> t.addLine(IKey.dynamic(() -> status(state.getIntValue())))
                            .addLine(IKey.dynamic(() -> StockText.Stored.text() + ": " + stored.getLongValue()))
                            .addLine(IKey.dynamic(() -> StockText.Pending.text() + ": " + pending.getLongValue()))));
    }

    private static boolean valid(StockRequesterView.Line line) {
        return line != null && line.tile != null;
    }

    private static boolean authorized(StockGuiFactory.Data data, PanelSyncManager sync) {
        return !data.getWorld().isRemote && StockGuiFactory.instance.canInteractWith(sync.getPlayer(), data);
    }

    private static TextFieldWidget field(StringSyncValue value) {
        var field = new ConfirmField();
        return field.value(value)
            .autoUpdateOnChange(false)
            .setMaxLength(32)
            .background(
                (context, x, y, w, h, theme) -> StockGuiAssets.textField(field.isFocused())
                    .draw(context, x, y, w, h, theme))
            .padding(2)
            .setTextAlignment(Alignment.CenterLeft)
            .setTextColor(0xffffffff);
    }

    private static TextFieldWidget quantity(PanelSyncManager sync, String id, DeviceToken token, BooleanSupplier fluid,
        LongSupplier getter, LongConsumer setter, StockGuiFactory.Data data, BooleanSupplier permitted) {
        var value = new TokenString(() -> StockNumbers.format(getter.getAsLong()), text -> {
            if (!authorized(data, sync) || !permitted.getAsBoolean()) return;
            try {
                setter.accept(StockNumbers.parse(text));
            } catch (ArithmeticException | NumberFormatException ignored) {}
        }, token, data);
        sync.syncValue(id, value);
        var field = field(value).autoUpdateOnChange(true);
        ((ConfirmField) field).fluid = fluid;
        ((ConfirmField) field).confirm = () -> field.getScreen()
            .close();
        return field;
    }

    private static ButtonWidget<?> button(PanelSyncManager sync, String id, StockGuiFactory.Data data,
        Supplier<String> label, Consumer<MouseData> action) {
        var handler = new InteractionSyncHandler().setOnMousePressed(
            mouse -> {
                if (!mouse.isClient() && mouse.mouseButton == 0 && authorized(data, sync)) action.accept(mouse);
            });
        sync.syncValue(id, handler);
        return new ButtonWidget<>().syncHandler(handler)
            .overlay(IKey.dynamic(label))
            .background(StockGuiAssets.button(false))
            .hoverBackground(StockGuiAssets.button(true));
    }

    private static ButtonWidget<?> clientButton(Runnable action) {
        return new ButtonWidget<>().onMousePressed(mouse -> {
            if (mouse == 0) action.run();
            return true;
        })
            .background(StockGuiAssets.button(false))
            .hoverBackground(StockGuiAssets.button(true));
    }

    private static ButtonWidget<?> toolbar(PanelSyncManager sync, String id, StockGuiFactory.Data data, int x, int y,
        Supplier<IDrawable> icon, Supplier<String> hint, Consumer<MouseData> action) {
        return button(sync, id, data, () -> "", action).pos(x, y)
            .size(18, 20)
            .background(StockGuiAssets.toolbar)
            .hoverBackground(StockGuiAssets.toolbarHover)
            .overlay(
                (context, px, py, w, h, theme) -> icon.get()
                    .draw(context, px + 1, py + 1, 16, 16, theme))
            .excludeAreaInRecipeViewer()
            .tooltip(t -> t.addLine(IKey.dynamic(hint)));
    }

    /** Upstream's left toolbar closes gaps when the redstone-card button is hidden. */
    private static ButtonWidget<?> toolbarPosition(ButtonWidget<?> button, BooleanSyncValue redstoneCard, int withCard,
        int withoutCard) {
        int[] previous = { Integer.MIN_VALUE };
        return button.onUpdateListener(widget -> {
            int top = redstoneCard.getBoolValue() ? withCard : withoutCard;
            if (top != previous[0]) {
                previous[0] = top;
                widget.top(top);
                WidgetTree.resizeInternal(widget.resizer(), false);
            }
        });
    }

    private static ButtonWidget<?> tokenButton(PanelSyncManager sync, String id, DeviceToken token,
        StockGuiFactory.Data data, Runnable action) {
        sync.registerSyncedAction(id + "Action", false, true, buffer -> {
            String identity = NetworkUtils.readStringSafe(buffer);
            if (token.matches(identity) && authorized(data, sync)) action.run();
        });
        var handler = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.isClient() && mouse.mouseButton == 0)
                sync.callSyncedAction(id + "Action", buffer -> NetworkUtils.writeStringSafe(buffer, token.getValue()));
        });
        sync.syncValue(id, handler);
        return new ButtonWidget<>().syncHandler(handler)
            .background(IDrawable.EMPTY);
    }

    private static String redstoneText(int value) {
        return (value == RedstoneMode.HIGH_SIGNAL.ordinal() ? StockText.RedstoneHigh
            : value == RedstoneMode.LOW_SIGNAL.ordinal() ? StockText.RedstoneLow : StockText.RedstoneIgnore).text();
    }

    private static int statusColor(int state, boolean enabled) {
        if (!enabled || state == TileMERequester.empty
            || state == TileMERequester.disabled
            || state == TileMERequester.offline) return 0xff555555;
        return switch (state) {
            case TileMERequester.missingMaterials, TileMERequester.noPattern, TileMERequester.failed -> 0xffff5555;
            case TileMERequester.waitingCPU -> 0xffffaa00;
            case TileMERequester.crafting, TileMERequester.calculating, TileMERequester.waiting -> 0xffffff55;
            default -> 0xff00aa00;
        };
    }

    private static String status(int value) {
        return switch (value) {
            case TileMERequester.ready -> StockText.Ready.text();
            case TileMERequester.waiting -> StockText.Waiting.text();
            case TileMERequester.calculating -> StockText.Calculating.text();
            case TileMERequester.crafting -> StockText.Crafting.text();
            case TileMERequester.missingMaterials -> StockText.Materials.text();
            case TileMERequester.waitingCPU -> StockText.CPU.text();
            case TileMERequester.offline -> StockText.Offline.text();
            case TileMERequester.disabled -> StockText.Disabled.text();
            case TileMERequester.noPattern -> StockText.Pattern.text();
            case TileMERequester.failed -> StockText.Failed.text();
            default -> "—";
        };
    }

    /** Quantity packets include the displayed device identity, rejecting edits after scrolling/searching. */
    private static final class TokenString extends StringSyncValue {

        private final DeviceToken token;
        private final StockGuiFactory.Data data;
        private boolean matching = true;

        TokenString(Supplier<String> getter, Consumer<String> setter, DeviceToken token, StockGuiFactory.Data data) {
            super(getter, setter);
            this.token = token;
            this.data = data;
            allowC2S();
        }

        @Override
        protected void serialize(PacketBuffer buffer, String value) throws IOException {
            super.serialize(buffer, value);
            NetworkUtils.writeStringSafe(buffer, token.getValue());
        }

        @Override
        protected String deserialize(PacketBuffer buffer) throws IOException {
            String value = super.deserialize(buffer), identity = NetworkUtils.readStringSafe(buffer);
            matching = data.getWorld().isRemote || token.matches(identity);
            return value;
        }

        @Override
        public void setValue(String value, boolean setSource, boolean sync) {
            super.setValue(value, setSource && matching, sync);
        }
    }

    private static SampleSync sample(PanelSyncManager sync, int index, DeviceToken token, StockGuiFactory.Data data,
        Supplier<IAEStack<?>> key, BooleanSupplier permitted, BiConsumer<MouseData, ItemStack> action) {
        var handler = new SampleSync(
            new ModularSlot(new SampleInventory(key, data), 0).singletonSlotGroup(),
            token,
            data,
            permitted,
            action);
        sync.syncValue("sample", index, handler);
        return handler;
    }

    private static final class SampleSlot extends PhantomItemSlot {

        private final SampleSync handler;
        private LongSupplier amount;

        SampleSlot(SampleSync handler) {
            this.handler = handler;
            syncHandler(handler);
        }

        SampleSlot displayAmount(LongSupplier amount) {
            this.amount = amount;
            return this;
        }

        @Override
        protected void drawSlotAmountText(int stackSize, String format) {
            if (amount == null) {
                super.drawSlotAmountText(stackSize, format);
                return;
            }
            long value = amount.getAsLong();
            var key = StockResources.sample(getSlot().getStack());
            boolean fluid = key != null && key.isFluid();
            String text = ReadableNumberConverter.INSTANCE.toSlimReadableForm(value);
            if (fluid) text += "L";
            var renderer = TextRenderer.SHARED;
            renderer.setColor(0xffffffff);
            renderer.setShadow(true);
            renderer.setScale(0.5f);
            renderer.setPos(1, 9);
            renderer.setAlignment(Alignment.BottomRight, 16, 8);
            renderer.draw(text);
        }

        @Override
        public Result onMousePressed(int button) {
            handler.click(MouseData.create(button));
            return Result.SUCCESS;
        }

        @Override
        public boolean onMouseScroll(UpOrDown direction, int amount) {
            return false;
        }
    }

    private static final class SampleSync extends PhantomItemSlotSH {

        private static final int clickPacket = 1200, dragPacket = 1201;
        private final DeviceToken token;
        private final StockGuiFactory.Data data;
        private final BooleanSupplier permitted;
        private final BiConsumer<MouseData, ItemStack> action;

        SampleSync(ModularSlot slot, DeviceToken token, StockGuiFactory.Data data, BooleanSupplier permitted,
            BiConsumer<MouseData, ItemStack> action) {
            super(slot);
            this.token = token;
            this.data = data;
            this.permitted = permitted;
            this.action = action;
        }

        void click(MouseData mouse) {
            syncToServer(clickPacket, buffer -> {
                NetworkUtils.writeStringSafe(buffer, token.getValue());
                mouse.writeToPacket(buffer);
            });
        }

        @Override
        public void updateFromClient(ItemStack stack, int button) {
            syncToServer(dragPacket, buffer -> {
                NetworkUtils.writeStringSafe(buffer, token.getValue());
                NetworkUtils.writeItemStack(buffer, stack);
                buffer.writeInt(button);
            });
        }

        @Override
        public void readOnServer(int id, PacketBuffer buffer) throws IOException {
            if (id != clickPacket && id != dragPacket) return;
            String identity = NetworkUtils.readStringSafe(buffer);
            if (!authorized(data, getSyncManager()) || !permitted.getAsBoolean() || !token.matches(identity)) return;
            if (id == clickPacket) action.accept(MouseData.readPacket(buffer), getSyncManager().getCursorItem());
            else {
                ItemStack stack = NetworkUtils.readItemStack(buffer);
                action.accept(new MouseData(Side.SERVER, buffer.readInt(), false, false, false), stack);
            }
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && stack.stackSize > 0;
        }
    }

    /** Server validation reads the live row, including changes earlier in the same tick. */
    private static final class DeviceToken extends StringSyncValue {

        private final Supplier<String> identity;

        DeviceToken(Supplier<String> identity) {
            super(identity);
            this.identity = identity;
        }

        boolean matches(String incoming) {
            return incoming != null && incoming.equals(identity.get());
        }
    }

    /** Viewport size is client-owned; an initial server value must not overwrite the resized GUI. */
    private static final class ClientRows extends IntSyncValue {

        ClientRows(IntSupplier getter, IntConsumer setter) {
            super(getter, setter);
        }

        @Override
        public void detectAndSendChanges(boolean init) {}

        @Override
        public void readOnClient(int id, PacketBuffer buffer) {}
    }

    private static final class SampleInventory implements IItemHandlerModifiable {

        private final Supplier<IAEStack<?>> key;
        private final StockGuiFactory.Data data;
        private ItemStack client;

        SampleInventory(Supplier<IAEStack<?>> key, StockGuiFactory.Data data) {
            this.key = key;
            this.data = data;
        }

        public int getSlots() {
            return 1;
        }

        public int getSlotLimit(int slot) {
            return 1;
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return null;
        }

        public void setStackInSlot(int slot, ItemStack stack) {
            if (data.getWorld().isRemote) client = stack;
        }

        public ItemStack getStackInSlot(int slot) {
            return data.getWorld().isRemote ? client : StockResources.display(key.get());
        }
    }

    private static final class ScrollCapture extends Widget<ScrollCapture> implements Interactable {

        private final IntSyncValue scroll, maximum;

        ScrollCapture(IntSyncValue scroll, IntSyncValue maximum) {
            this.scroll = scroll;
            this.maximum = maximum;
        }

        @Override
        public Result onMousePressed(int button) {
            return Result.IGNORE;
        }

        @Override
        public boolean onMouseScroll(UpOrDown direction, int amount) {
            scroll.setIntValue(
                Math.max(0, Math.min(maximum.getIntValue(), scroll.getIntValue() - direction.modifier * 2)));
            return true;
        }
    }

    /** Enter submits the current visible values together, rather than committing one field on focus loss. */
    private static final class ConfirmField extends TextFieldWidget {

        Runnable confirm;
        boolean clearWithRightClick;
        BooleanSupplier fluid;
        private int normalWidth;
        private String lastInput;

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (fluid == null || !areAncestorsEnabled()) return;
            if (normalWidth == 0) normalWidth = getArea().w();
            int width = normalWidth - (fluid.getAsBoolean() ? 10 : 0);
            if (getArea().w() != width) {
                width(width);
                WidgetTree.resizeInternal(resizer(), false);
            }
            String input = getText();
            if (input.equals(lastInput)) return;
            lastInput = input;
            try {
                StockNumbers.parse(input);
                setTextColor(0xffffffff);
            } catch (ArithmeticException | NumberFormatException ignored) {
                setTextColor(0xffff0000);
            }
        }

        @Override
        public Result onMousePressed(int button) {
            if (button == 1 && clearWithRightClick) {
                setText("");
                getStringValue().setStringValue("");
                return Result.SUCCESS;
            }
            return super.onMousePressed(button);
        }

        @Override
        public Result onKeyPressed(char character, int keyCode) {
            if (isFocused() && confirm != null
                && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)) {
                getContext().removeFocus();
                confirm.run();
                return Result.SUCCESS;
            }
            return super.onKeyPressed(character, keyCode);
        }
    }

    /** Mirrors the upstream terminal's margin and four height modes while bounding synchronized rows. */
    static final class TerminalPanel extends ModularPanel {

        final IntSyncValue rows;
        ScrollCapture wheel;
        SliderWidget slider;

        TerminalPanel(IntSyncValue rows) {
            super("requester_terminal");
            this.rows = rows;
        }

        void resizeRows(int screenHeight) {
            int possible = Math.max(3, (screenHeight - 50 - 119) / 19);
            int count = Math.max(3, Math.min(maximumTerminalRows, possible * terminalStyle / 4));
            rows.setIntValue(count);
            height(120 + count * 19);
            wheel.height(count * 19);
            slider.height(count * 19 + 1);
        }
    }
}
