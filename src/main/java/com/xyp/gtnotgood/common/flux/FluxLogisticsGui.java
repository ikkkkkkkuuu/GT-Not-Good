package com.xyp.gtnotgood.common.flux;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.value.sync.DynamicLinkedSyncHandler;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.mebridge.MEBridgeChannelManager;
import com.xyp.gtnotgood.common.mebridge.MEBridgeChannelName;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.ModernThemeAdapter;

/** Channel picker and item/fluid ghost filters. Blank target fields mean fill, and never consume cursor samples. */
public final class FluxLogisticsGui {

    private static final ModernThemeAdapter THEME = new ModernThemeAdapter(
        path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));

    private FluxLogisticsGui() {}

    public static ModularPanel build(TileFluxLogistics tile, PanelSyncManager sync) {
        ModularPanel panel = ModularPanel.defaultPanel("flux_logistics", 400, 346)
            .background(THEME.panel);
        panel.child(
            IKey.lang("tile.flux_logistics_plug.name")
                .color(0xFF202830)
                .asWidget()
                .pos(8, 7)
                .size(160, 12));
        StringSyncValue direction = new StringSyncValue(() -> tile.importing() ? "1" : "0");
        sync.syncValue("direction", direction);
        panel.child(
            new ButtonWidget<>().pos(175, 5)
                .size(213, 17)
                .background(THEME.button)
                .overlay(IKey.dynamic(() -> {
                    // #tr flux.logistics.import
                    // # Container -> ME (automatic)
                    // # zh_CN 容器 → 网络（自动回收）
                    if ("1".equals(direction.getValue())) return IKey.lang("flux.logistics.import")
                        .get();
                    // #tr flux.logistics.export
                    // # ME -> Container (filtered)
                    // # zh_CN 网络 → 容器（按过滤槽）
                    return IKey.lang("flux.logistics.export")
                        .get();
                })
                    .color(0xFF202830))
                .syncHandler(
                    new InteractionSyncHandler().setOnMousePressed(
                        mouse -> {
                            if (!mouse.isClient() && tile.canEdit(sync.getPlayer())) tile.toggleDirection();
                        })));
        StringSyncValue channel = new StringSyncValue(
            tile::getChannelName,
            value -> { if (tile.isServerSide() && tile.canEdit(sync.getPlayer())) tile.setChannelName(value); })
                .allowC2S();
        sync.syncValue("channel", channel);
        panel.child(
            new TextFieldWidget().value(channel)
                .setMaxLength(MEBridgeChannelName.MAX_LENGTH)
                .autoUpdateOnChange(false)
                .pos(8, 25)
                .size(250, 16));
        StringSyncValue state = new StringSyncValue(() -> tile.online ? "1" : "0");
        sync.syncValue("state", state);
        panel.child(IKey.dynamic(() -> {
            // #tr flux.logistics.online
            // # Online
            // # zh_CN 在线
            if ("1".equals(state.getValue())) return IKey.lang("flux.logistics.online")
                .get();
            // #tr flux.logistics.offline
            // # Offline
            // # zh_CN 离线
            return IKey.lang("flux.logistics.offline")
                .get();
        })
            .color(0xFF202830)
            .asWidget()
            .pos(270, 25)
            .size(120, 16));
        // #tr flux.logistics.channels
        // # ME bridge channels
        // # zh_CN ME 网桥频道
        panel.child(
            IKey.lang("flux.logistics.channels")
                .color(0xFF202830)
                .asWidget()
                .pos(8, 48)
                .size(145, 12));
        StringSyncValue directory = new StringSyncValue(
            () -> MEBridgeChannelManager.browserSnapshot(tile.getWorldTime()));
        sync.syncValue("directoryData", directory);
        DynamicLinkedSyncHandler<StringSyncValue> dynamic = new DynamicLinkedSyncHandler<>(directory)
            .widgetProvider((manager, value) -> directory(tile, manager, value.getValue()));
        sync.syncValue("directory", dynamic);
        panel.child(
            new DynamicSyncedWidget<>().syncHandler(dynamic)
                .initialChild(directory(tile, sync, ""))
                .pos(8, 65)
                .size(145, 170));
        // #tr flux.logistics.items
        // # Item / target count
        // # zh_CN 物品 / 目标个数
        panel.child(
            IKey.lang("flux.logistics.items")
                .color(0xFF202830)
                .asWidget()
                .pos(163, 48)
                .size(110, 12));
        // #tr flux.logistics.fluids
        // # Fluid / target mB
        // # zh_CN 流体 / 目标 mB
        panel.child(
            IKey.lang("flux.logistics.fluids")
                .color(0xFF202830)
                .asWidget()
                .pos(280, 48)
                .size(110, 12));
        ItemSample samples = new ItemSample(tile, () -> tile.canEdit(sync.getPlayer()));
        for (int i = 0; i < TileFluxLogistics.SLOTS; i++) {
            int slot = i, y = 65 + i * 19;
            panel.child(
                new PhantomItemSlot().slot(new SampleSlot(samples, i).singletonSlotGroup())
                    .background(THEME.slot)
                    .pos(163, y)
                    .setEnabledIf(w -> !"1".equals(direction.getValue())));
            FluidSlotSyncHandler fluid = new FluidSlotSyncHandler(
                new FluidSample(tile, i, () -> tile.canEdit(sync.getPlayer()))).phantom(true)
                    .controlsAmount(false);
            sync.syncValue("fluid" + i, fluid);
            panel.child(
                new FluidSlot().syncHandler(fluid)
                    .background(THEME.slot)
                    .pos(280, y)
                    .setEnabledIf(w -> !"1".equals(direction.getValue())));
            panel.child(
                amount(tile, sync, tile.itemTargets, slot, "items").pos(184, y + 1)
                    .size(87, 16)
                    .setEnabledIf(w -> !"1".equals(direction.getValue())));
            panel.child(
                amount(tile, sync, tile.fluidTargets, slot, "fluids").pos(301, y + 1)
                    .size(87, 16)
                    .setEnabledIf(w -> !"1".equals(direction.getValue())));
        }
        panel.child(IKey.dynamic(() -> {
            // #tr flux.logistics.import_hint
            // # Auto-collect output slots/tanks on this face. Filters and counts are unused.
            // # zh_CN 自动回收该侧输出槽与流体；无需过滤槽或数量。
            if ("1".equals(direction.getValue())) return IKey.lang("flux.logistics.import_hint")
                .get();
            // #tr flux.logistics.target_hint
            // # Blank/0: fill container. Positive: restock to target. Fluids use mB.
            // # zh_CN 留空或 0：装满；填写数量：补到目标。流体单位为 mB。
            return IKey.lang("flux.logistics.target_hint")
                .get();
        })
            .color(0xFF202830)
            .asWidget()
            .pos(8, 239)
            .size(384, 14));
        panel.bindPlayerInventory();
        return panel;
    }

    private static TextFieldWidget amount(TileFluxLogistics tile, PanelSyncManager sync, long[] targets, int slot,
        String key) {
        StringSyncValue value = new StringSyncValue(
            () -> targets[slot] == 0 ? "" : Long.toString(targets[slot]),
            incoming -> {
                if (!tile.isServerSide() || !tile.canEdit(sync.getPlayer())) return;
                try {
                    targets[slot] = incoming.isEmpty() ? 0 : Math.max(0, Long.parseLong(incoming));
                    tile.markDirty();
                } catch (NumberFormatException ignored) {}
            }).allowC2S();
        sync.syncValue(key + slot, value);
        return new TextFieldWidget().value(value)
            .autoUpdateOnChange(false)
            .setMaxLength(18)
            .setPattern(Pattern.compile("[0-9]*"));
    }

    /** Dynamic rows register by channel identity so refreshes reuse handlers without retargeting old buttons. */
    static ListWidget<?, ?> directory(TileFluxLogistics tile, PanelSyncManager sync, String data) {
        ListWidget<com.cleanroommc.modularui.api.widget.IWidget, ?> list = new ListWidget<>().size(145, 170);
        for (String entry : data.split("\n")) {
            if (entry.isEmpty()) continue;
            String name = new String(
                java.util.Base64.getUrlDecoder()
                    .decode(entry.split("\u0001", -1)[0]),
                java.nio.charset.StandardCharsets.UTF_8);
            list.child(
                new ButtonWidget<>().size(135, 20)
                    .background(THEME.button)
                    .overlay(
                        IKey.str(name)
                            .color(0xFF202830))
                    .syncHandler(
                        sync.getOrCreateSyncHandler(
                            "logistics_channel_" + name,
                            0,
                            InteractionSyncHandler.class,
                            () -> new InteractionSyncHandler().setOnMousePressed(mouse -> {
                                if (!mouse.isClient() && tile.canEdit(sync.getPlayer())
                                    && MEBridgeChannelManager.exists(name)) tile.setChannelName(name);
                            }))));
        }
        return list;
    }

    /** Accepts any real item as a single phantom identity, without admitting items into the export buffer. */
    private static final class SampleSlot extends ModularSlot {

        SampleSlot(IItemHandlerModifiable handler, int slot) {
            super(handler, slot);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && stack.stackSize > 0;
        }

        @Override
        public int getItemStackLimit(ItemStack stack) {
            return 1;
        }
    }

    /** GUI-only filter handler; client synchronization never writes the tile's authoritative filter. */
    private static final class ItemSample implements IItemHandlerModifiable {

        private final TileFluxLogistics tile;
        private final Supplier<Boolean> editable;
        private final ItemStack[] client = new ItemStack[TileFluxLogistics.SLOTS];

        ItemSample(TileFluxLogistics tile, Supplier<Boolean> editable) {
            this.tile = tile;
            this.editable = editable;
        }

        public int getSlots() {
            return TileFluxLogistics.SLOTS;
        }

        public int getSlotLimit(int slot) {
            return 1;
        }

        public ItemStack getStackInSlot(int slot) {
            return tile.isServerSide() ? tile.itemFilters[slot] : client[slot];
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return null;
        }

        public void setStackInSlot(int slot, ItemStack stack) {
            ItemStack copy = stack == null ? null : stack.copy();
            if (copy != null) copy.stackSize = 1;
            if (tile.isServerSide()) {
                if (editable.get()) tile.setItemFilter(slot, copy);
            } else client[slot] = copy;
        }
    }

    /** GUI-only fluid sample. Phantom slot clicks use this tank without draining the cursor container. */
    private static final class FluidSample implements IFluidTank {

        private final TileFluxLogistics tile;
        private final Supplier<Boolean> editable;
        private FluidStack client;
        private final int slot;

        FluidSample(TileFluxLogistics tile, int slot, Supplier<Boolean> editable) {
            this.tile = tile;
            this.editable = editable;
            this.slot = slot;
        }

        public FluidStack getFluid() {
            return tile.isServerSide() ? tile.fluidFilters[slot] : client;
        }

        public int getFluidAmount() {
            return getFluid() == null ? 0 : 1;
        }

        public int getCapacity() {
            return 1;
        }

        public FluidTankInfo getInfo() {
            return new FluidTankInfo(getFluid(), 1);
        }

        private void sample(FluidStack stack) {
            FluidStack copy = stack == null ? null : stack.copy();
            if (copy != null) copy.amount = 1;
            if (tile.isServerSide()) {
                if (editable.get()) tile.setFluidFilter(slot, copy);
            } else client = copy;
        }

        public int fill(FluidStack resource, boolean doFill) {
            if (resource == null || resource.amount <= 0) return 0;
            if (doFill) sample(resource);
            return 1;
        }

        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (maxDrain <= 0 || getFluid() == null) return null;
            FluidStack result = getFluid().copy();
            if (doDrain) sample(null);
            return result;
        }
    }
}
