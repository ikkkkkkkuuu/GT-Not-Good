// SPDX-License-Identifier: GPL-3.0-only
// Layout sources and asset licenses: META-INF/advancedio-port/NOTICE.md.
package com.xyp.gtnotgood.common.advancedio;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

/**
 * AdvancedAE's stock-bus screen and AE2AddonLib's amount screen on the 1.7.10 sync backend.
 * Atlas crops, slot origins, external upgrade panel and toolbar positions retain the pinned upstream layout.
 * Amount changes are buffered until confirmation; opening either screen preserves server permission checks.
 */
final class AdvancedIOGui {

    private AdvancedIOGui() {}

    private static UITexture texture(String name, int w, int h, int x, int y, int width, int height) {
        return UITexture.builder()
            .location(ModList.ModIds.GT_NOT_GOOD, "gui/advancedio/" + name)
            .imageSize(w, h)
            .subAreaXYWH(x, y, width, height)
            .nonOpaque()
            .build();
    }

    private static UITexture sprite(String name, int width, int height, int left, int top, int right, int bottom) {
        return UITexture.builder()
            .location(ModList.ModIds.GT_NOT_GOOD, "gui/advancedio/" + name)
            .imageSize(width, height)
            .adaptable(left, top, right, bottom)
            .build();
    }

    static ModularPanel build(PartAdvancedIOBus part, PanelSyncManager sync) {
        var enabled = new IntSyncValue(part::availableSlots);
        var regulate = new BooleanSyncValue(part::regulate);
        var hasRedstone = new BooleanSyncValue(() -> part.getInstalledUpgrades(Upgrades.REDSTONE) > 0);
        var redstone = new IntSyncValue(
            () -> part.getRSMode()
                .ordinal());
        var scheduling = new IntSyncValue(
            () -> ((SchedulingMode) part.getConfigManager()
                .getSetting(Settings.SCHEDULING_MODE)).ordinal());
        sync.syncValue("enabled", enabled);
        sync.syncValue("regulate", regulate);
        sync.syncValue("hasRedstone", hasRedstone);
        sync.syncValue("redstone", redstone);
        sync.syncValue("scheduling", scheduling);
        var samples = new Samples(part);
        var panel = ModularPanel.defaultPanel("advanced_io", 176, 253)
            .background(texture("storagebus", 256, 256, 0, 0, 176, 253))
            .disableHoverBackground();
        hasRedstone.setChangeListener(panel::scheduleResize);
        panel.child(
            IKey.lang("item.advanced_io_bus.name")
                .color(0xff404040)
                .asWidget()
                .pos(8, 6));
        // #tr gui.advancedio.amount_hint
        // # Middle-click to set amount
        // # zh_CN 鼠标中键设置数量
        panel.child(
            IKey.lang("gui.advancedio.amount_hint")
                .color(0xff404040)
                .scale(0.6f)
                .asWidget()
                .pos(10, 17));
        for (int i = 0; i < PartAdvancedIOBus.CONFIG_SLOTS; i++) {
            final int index = i;
            var amount = new IntSyncValue(
                () -> part.filter(index) == null ? 0
                    : (int) part.filter(index)
                        .getStackSize());
            sync.syncValue("amount_" + i, amount);
            var fluid = new BooleanSyncValue(
                () -> part.filter(index) != null && part.filter(index)
                    .isFluid());
            sync.syncValue("fluid_" + i, fluid);
            var handler = new SampleSync(new ModularSlot(samples, i).singletonSlotGroup(), part, index);
            sync.syncValue("sample", i, handler);
            if (i >= 18) {
                IDrawable slotBackground = (context, x, y, w, h, theme) -> {
                    org.lwjgl.opengl.GL11.glColor4f(1, 1, 1, index < enabled.getIntValue() ? 1 : 0.2f);
                    texture("states", 256, 256, 192, 192, 18, 18).draw((float) x, y, w, h);
                    org.lwjgl.opengl.GL11.glColor4f(1, 1, 1, 1);
                };
                panel.child(
                    slotBackground.asWidget()
                        .pos(7 + i % 9 * 18, 28 + i / 9 * 18)
                        .size(18));
            }
            panel.child(
                new PhantomItemSlot().syncHandler(handler)
                    .background(IDrawable.EMPTY)
                    .overlay((context, x, y, w, h, theme) -> {
                        if (amount.getIntValue() <= 0) return;
                        String text = shortAmount(amount.getIntValue(), fluid.getBoolValue());
                        var font = net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
                        org.lwjgl.opengl.GL11.glPushMatrix();
                        org.lwjgl.opengl.GL11.glScalef(0.666f, 0.666f, 1);
                        font.drawStringWithShadow(
                            text,
                            (int) ((x + 18) / 0.666f - font.getStringWidth(text)),
                            (int) ((y + 16) / 0.666f - 5),
                            0xffffff);
                        org.lwjgl.opengl.GL11.glPopMatrix();
                    })
                    .tooltip(
                        t -> t.addLine(IKey.dynamic(() -> Integer.toString(amount.getIntValue())))
                            .addLine(IKey.lang("gui.advancedio.amount_hint")))
                    .setEnabledIf(widget -> index < enabled.getIntValue())
                    .pos(7 + i % 9 * 18, 28 + i / 9 * 18));
        }
        // UpgradesPanel's atlas has a horizontal offset: slot x is panel x, y has five pixels of padding.
        var upgrades = new InvWrapper(part.getInventoryByName("upgrades"));
        for (int i = 0; i < 8; i++) {
            int srcY = i == 0 ? 0 : 5;
            int height = i == 0 ? 23 : i == 7 ? 25 : 18;
            panel.child(
                texture("extra_panels", 128, 128, 0, srcY, 28, height).asWidget()
                    .pos(172, i == 0 ? 0 : 5 + i * 18)
                    .size(28, height)
                    .excludeAreaInRecipeViewer());
            panel.child(
                new ItemSlot().slot(new ModularSlot(upgrades, i).singletonSlotGroup())
                    .background(IDrawable.EMPTY)
                    .pos(172, 5 + i * 18));
        }
        panel.child(
            IKey.lang("container.inventory")
                .color(0xff404040)
                .asWidget()
                .pos(8, 158));
        for (int i = 0; i < 36; i++) {
            int column = i < 9 ? i : (i - 9) % 9;
            int row = i < 9 ? 3 : (i - 9) / 9;
            panel.child(
                new ItemSlot().syncHandler("player", i)
                    .background(IDrawable.EMPTY)
                    .pos(7 + column * 18, 168 + row * 18 + (i < 9 ? 4 : 0)));
        }
        IDrawable toolbar = (context, x, y, w, h, theme) -> sprite("vertical_buttons_bg", 21, 26, 0, 2, 0, 4)
            .draw(context, x, y, 21, hasRedstone.getBoolValue() ? 72 : 50, theme);
        panel.child(
            new com.cleanroommc.modularui.widget.Widget<>().background(toolbar)
                .pos(-19, 0)
                .size(21, 73)
                .excludeAreaInRecipeViewer());
        panel.child(
            toolbarButton(
                sync,
                "redstoneButton",
                () -> texture(
                    "states",
                    256,
                    256,
                    redstone.getIntValue() == RedstoneMode.HIGH_SIGNAL.ordinal() ? 16
                        : redstone.getIntValue() == RedstoneMode.LOW_SIGNAL.ordinal() ? 0 : 48,
                    0,
                    16,
                    16),
                () -> redstoneLabel(redstone.getIntValue()),
                mouse -> {
                    if (part.getInstalledUpgrades(Upgrades.REDSTONE) == 0) return;
                    RedstoneMode[] modes = { RedstoneMode.IGNORE, RedstoneMode.HIGH_SIGNAL, RedstoneMode.LOW_SIGNAL };
                    int old = part.getRSMode() == RedstoneMode.IGNORE ? 0
                        : part.getRSMode() == RedstoneMode.HIGH_SIGNAL ? 1 : 2;
                    part.getConfigManager()
                        .putSetting(Settings.REDSTONE_CONTROLLED, modes[(old + (mouse.mouseButton == 1 ? 2 : 1)) % 3]);
                    part.changed();
                }).pos(-16, 3)
                    .setEnabledIf(w -> hasRedstone.getBoolValue()));
        panel.child(
            toolbarButton(
                sync,
                "schedulingButton",
                () -> texture("states", 256, 256, scheduling.getIntValue() * 16, 240, 16, 16),
                () -> schedulingLabel(scheduling.getIntValue()),
                mouse -> {
                    int old = ((SchedulingMode) part.getConfigManager()
                        .getSetting(Settings.SCHEDULING_MODE)).ordinal();
                    part.getConfigManager()
                        .putSetting(
                            Settings.SCHEDULING_MODE,
                            SchedulingMode.values()[(old + (mouse.mouseButton == 1 ? 2 : 1)) % 3]);
                    part.changed();
                }).left(-16)
                    .top(
                        () -> hasRedstone.getBoolValue() ? 25 : 3,
                        com.cleanroommc.modularui.widget.sizer.Unit.Measure.PIXEL));
        // #tr gui.advancedio.regulate
        // # Recover excess stock
        // # zh_CN 回收超量原料
        panel.child(
            toolbarButton(
                sync,
                "regulateButton",
                () -> texture("advanced_states", 256, 256, regulate.getBoolValue() ? 32 : 48, 32, 16, 16),
                () -> IKey.lang("gui.advancedio.regulate")
                    .get() + ": "
                    + yesNo(regulate.getBoolValue()),
                mouse -> part.setRegulate(!part.regulate())).left(-16)
                    .top(
                        () -> hasRedstone.getBoolValue() ? 47 : 25,
                        com.cleanroommc.modularui.widget.sizer.Unit.Measure.PIXEL));
        return panel;
    }

    /** Retains SetAmountScreen's separate 176x107 page; changes only commit when Set/Enter is pressed. */
    static ModularPanel amountPanel(PartAdvancedIOBus part, PanelSyncManager sync, int index) {
        int[] draft = { part.filter(index) == null ? 1
            : (int) part.filter(index)
                .getStackSize() };
        var quantity = new IntSyncValue(() -> draft[0], value -> draft[0] = Math.max(0, value)).allowC2S();
        sync.syncValue("quantity", quantity);
        var unit = new IntSyncValue(
            () -> part.filter(index) != null && part.filter(index)
                .isFluid() ? 1000 : 1);
        sync.syncValue("amountUnit", unit);
        var displayQuantity = new com.cleanroommc.modularui.value.sync.DoubleSyncValue(
            () -> (double) draft[0] / unit.getIntValue(),
            value -> {
                if (Double.isFinite(value)) draft[0] = (int) Math
                    .max(0, Math.min(Integer.MAX_VALUE - 1L, Math.round(value * unit.getIntValue())));
            }) {

            @Override
            public String getStringValue() {
                return java.math.BigDecimal.valueOf(getDoubleValue())
                    .stripTrailingZeros()
                    .toPlainString();
            }
        }.allowC2S();
        sync.syncValue("displayQuantity", displayQuantity);
        var panel = ModularPanel.defaultPanel("advanced_io_amount", 176, 107)
            .background(texture("craft_amt", 256, 256, 0, 0, 176, 107))
            .disableHoverBackground();
        unit.setChangeListener(panel::scheduleResize);
        // #tr gui.advancedio.set_amount
        // # Set Amount
        // # zh_CN 设置数量
        panel.child(
            IKey.lang("gui.advancedio.set_amount")
                .color(0xff404040)
                .asWidget()
                .pos(8, 6));
        var samples = new Samples(part);
        panel.child(
            new ItemSlot().slot(
                new ModularSlot(samples, index).singletonSlotGroup()
                    .canPut(false)
                    .canTake(false))
                .background(IDrawable.EMPTY)
                .pos(22, 52));
        var confirm = new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.isClient()) return;
            if (index < part.availableSlots() && part.filter(index) != null) {
                part.setFilter(
                    index,
                    draft[0] == 0 ? null
                        : part.filter(index)
                            .copy()
                            .setStackSize(draft[0]));
            }
            AdvancedIOGuiFactory.INSTANCE.open(sync.getPlayer(), part);
        });
        sync.syncValue("confirm", confirm);
        panel.child(new TextFieldWidget() {

            @Override
            public void setText(String text) {
                // DoubleSyncValue's change notification uses Double.toString even for integer amounts.
                super.setText(text.endsWith(".0") ? text.substring(0, text.length() - 2) : text);
            }

            @Override
            public Result onKeyPressed(char character, int keyCode) {
                Result result = super.onKeyPressed(character, keyCode);
                if (result == Result.SUCCESS && (keyCode == 28 || keyCode == 156)) confirm.onMousePressed(0);
                return result;
            }
        }.value(displayQuantity)
            .numbersDouble(() -> 0, () -> (Integer.MAX_VALUE - 1.0) / unit.getIntValue())
            .autoUpdateOnChange(true)
            .setFocusOnGuiOpen(true)
            .background(IDrawable.EMPTY)
            .setTextColor(0xff404040)
            .pos(48, 55)
            .height(12)
            .width(
                () -> unit.getIntValue() == 1000 ? 57 : 66,
                com.cleanroommc.modularui.widget.sizer.Unit.Measure.PIXEL));
        panel.child(
            IKey.dynamic(() -> unit.getIntValue() == 1000 ? "B" : "")
                .color(0xff404040)
                .asWidget()
                .pos(108, 57));
        int[] xs = { 20, 48, 82, 120 };
        int[] widths = { 22, 28, 32, 38 };
        int[] decimal = { 1, 10, 100, 1000 };
        int[] stacks = { 1, 16, 32, 64 };
        for (int i = 0; i < 8; i++) {
            final int step = i % 4;
            final int sign = i < 4 ? 1 : -1;
            var action = new InteractionSyncHandler().setOnMousePressed(mouse -> {
                if (!mouse.isClient()) quantity.setIntValue(
                    (int) Math.max(
                        0,
                        Math.min(
                            Integer.MAX_VALUE - 1L,
                            (long) draft[0]
                                + (long) sign * unit.getIntValue()
                                    * (mouse.shift || mouse.ctrl ? stacks[step] : decimal[step])
                                - (sign > 0 && step > 0 && draft[0] == unit.getIntValue() ? unit.getIntValue() : 0))),
                    true,
                    true);
            });
            sync.syncValue("step", i, action);
            panel.child(
                textButton(() -> (sign > 0 ? "+" : "-") + (shiftOrControl() ? stacks[step] : decimal[step]))
                    .syncHandler(action)
                    .pos(xs[step], i < 4 ? 30 : 72)
                    .size(widths[step], 20));
        }
        // #tr gui.advancedio.set
        // # Set
        // # zh_CN 设置
        panel.child(
            textButton(
                () -> IKey.lang("gui.advancedio.set")
                    .get()).syncHandler(confirm)
                        .pos(120, 51)
                        .size(38, 20));
        var back = new InteractionSyncHandler().setOnMousePressed(
            mouse -> { if (!mouse.isClient()) AdvancedIOGuiFactory.INSTANCE.open(sync.getPlayer(), part); });
        sync.syncValue("back", back);
        IDrawable backTexture = (context, x, y, w, h, theme) -> {
            texture("states", 256, 256, 160, 192, 20, 20).draw(context, x, y, 20, 20, theme);
            texture("states", 256, 256, 96, 16, 16, 16).draw(context, x + 2, y + 1, 16, 16, theme);
        };
        panel.child(
            new ButtonWidget<>().background(backTexture)
                .hoverBackground(backTexture)
                .syncHandler(back)
                // #tr gui.advancedio.back
                // # Back
                // # zh_CN 返回
                .tooltip(t -> t.addLine(IKey.lang("gui.advancedio.back")))
                .pos(152, -5)
                .size(20));
        return panel;
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private static boolean shiftOrControl() {
        return net.minecraft.client.gui.GuiScreen.isShiftKeyDown()
            || net.minecraft.client.gui.GuiScreen.isCtrlKeyDown();
    }

    private static ButtonWidget<?> textButton(Supplier<String> label) {
        return new ButtonWidget<>().background(sprite("button", 200, 20, 3, 3, 3, 3))
            .hoverBackground(sprite("button_highlighted", 200, 20, 3, 3, 3, 3))
            .overlay(
                IKey.dynamic(label)
                    .color(0xfff2f2f2)
                    .shadow(false))
            .hoverOverlay(
                IKey.dynamic(label)
                    .color(0xff517497)
                    .shadow(false));
    }

    /** Slot labels use bucket quantities and compact magnitudes as in AE2's small stack labels. */
    private static String shortAmount(int amount, boolean fluid) {
        double value = amount / (fluid ? 1000.0 : 1.0);
        String suffix = "";
        if (value >= 1000000) {
            value /= 1000000;
            suffix = "M";
        } else if (value >= 1000) {
            value /= 1000;
            suffix = "k";
        }
        return java.math.BigDecimal.valueOf(value)
            .setScale(value < 1 ? 3 : value < 10 ? 2 : value < 100 ? 1 : 0, java.math.RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString() + suffix;
    }

    private static ButtonWidget<?> toolbarButton(PanelSyncManager sync, String name, Supplier<IDrawable> icon,
        Supplier<String> tooltip, Consumer<MouseData> action) {
        var handler = new InteractionSyncHandler().setOnMousePressed(
            mouse -> {
                if (!mouse.isClient() && (mouse.mouseButton == 0 || mouse.mouseButton == 1)) action.accept(mouse);
            });
        sync.syncValue(name, handler);
        IDrawable normal = (context, x, y, w, h, theme) -> {
            texture("states", 256, 256, 176, 128, 18, 20).draw(context, x, y, 18, 20, theme);
            icon.get()
                .draw(context, x + 1, y + 1, 16, 16, theme);
        };
        IDrawable hover = (context, x, y, w, h, theme) -> {
            texture("states", 256, 256, 212, 128, 18, 20).draw(context, x, y + 1, 18, 20, theme);
            icon.get()
                .draw(context, x + 1, y + 2, 16, 16, theme);
        };
        return new ButtonWidget<>().background(normal)
            .hoverBackground(hover)
            .syncHandler(handler)
            .tooltip(t -> t.addLine(IKey.dynamic(tooltip)))
            .size(18, 20)
            .excludeAreaInRecipeViewer();
    }

    private static String yesNo(boolean value) {
        // #tr gui.advancedio.yes
        // # On
        // # zh_CN 开
        if (value) return IKey.lang("gui.advancedio.yes")
            .get();
        // #tr gui.advancedio.no
        // # Off
        // # zh_CN 关
        return IKey.lang("gui.advancedio.no")
            .get();
    }

    private static String schedulingLabel(int value) {
        // #tr gui.advancedio.scheduling_default
        // # Scheduling: first to last
        // # zh_CN 调度模式：从前往后
        if (value == 0) return IKey.lang("gui.advancedio.scheduling_default")
            .get();
        // #tr gui.advancedio.scheduling_roundrobin
        // # Scheduling: round-robin
        // # zh_CN 调度模式：轮询
        if (value == 1) return IKey.lang("gui.advancedio.scheduling_roundrobin")
            .get();
        // #tr gui.advancedio.scheduling_random
        // # Scheduling: random
        // # zh_CN 调度模式：随机
        return IKey.lang("gui.advancedio.scheduling_random")
            .get();
    }

    private static String redstoneLabel(int value) {
        // #tr gui.advancedio.redstone_high
        // # Redstone: run with signal
        // # zh_CN 红石：有信号运行
        if (value == RedstoneMode.HIGH_SIGNAL.ordinal()) return IKey.lang("gui.advancedio.redstone_high")
            .get();
        // #tr gui.advancedio.redstone_low
        // # Redstone: run without signal
        // # zh_CN 红石：无信号运行
        if (value == RedstoneMode.LOW_SIGNAL.ordinal()) return IKey.lang("gui.advancedio.redstone_low")
            .get();
        // #tr gui.advancedio.redstone_ignore
        // # Redstone: ignore signal
        // # zh_CN 红石：忽略信号
        return IKey.lang("gui.advancedio.redstone_ignore")
            .get();
    }

    /** Ghost filters never consume cursor items; the middle button opens the native-sized amount page. */
    private static final class SampleSync extends PhantomItemSlotSH {

        private final PartAdvancedIOBus part;
        private final int index;

        SampleSync(ModularSlot slot, PartAdvancedIOBus part, int index) {
            super(slot);
            this.part = part;
            this.index = index;
        }

        @Override
        protected void phantomClick(MouseData mouse, ItemStack cursor) {
            if (index >= part.availableSlots()) return;
            if (mouse.mouseButton == 2) {
                if (part.filter(index) != null)
                    AdvancedIOGuiFactory.INSTANCE.open(getSyncManager().getPlayer(), part, index);
            } else if (mouse.shift || cursor == null) {
                part.setFilter(index, null);
            } else {
                FluidStack fluid = ItemFluidPacket.getFluidStack(cursor);
                if (fluid == null) fluid = FluidContainerRegistry.getFluidForFilledItem(cursor);
                if (fluid == null && cursor.getItem() instanceof IFluidContainerItem container)
                    fluid = container.getFluid(cursor);
                IAEStack<?> sample = fluid == null ? AEItemStack.create(cursor) : AEFluidStack.create(fluid);
                if (sample != null && part.filter(index) != null
                    && part.filter(index)
                        .isSameType(sample)) {
                    long amount = part.filter(index)
                        .getStackSize() + (mouse.mouseButton == 1 ? -1 : 1) * sample.getStackSize();
                    sample = amount <= 0 ? null : sample.setStackSize(amount);
                }
                part.setFilter(index, sample);
            }
        }

        @Override
        protected void phantomScroll(MouseData mouse) {
            if (index >= part.availableSlots() || part.filter(index) == null) return;
            long amount = part.filter(index)
                .getStackSize() + (mouse.ctrl ? 64L : mouse.shift ? 16L : 1L) * mouse.mouseButton;
            part.setFilter(
                index,
                amount <= 0 ? null
                    : part.filter(index)
                        .copy()
                        .setStackSize(amount));
        }
    }

    /** The custom server handler owns changes; client copies hold only the synchronized display item. */
    private static final class Samples implements IItemHandlerModifiable {

        private final PartAdvancedIOBus part;
        private final ItemStack[] client = new ItemStack[PartAdvancedIOBus.CONFIG_SLOTS];

        Samples(PartAdvancedIOBus part) {
            this.part = part;
        }

        public int getSlots() {
            return client.length;
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
            if (part.getTile()
                .getWorldObj().isRemote) client[slot] = stack;
        }

        public ItemStack getStackInSlot(int slot) {
            if (part.getTile()
                .getWorldObj().isRemote) return client[slot];
            var key = part.filter(slot);
            if (key instanceof IAEFluidStack fluid) return ItemFluidPacket.newStack(fluid.getFluidStack());
            if (key instanceof IAEItemStack item) {
                ItemStack icon = item.getItemStack();
                icon.stackSize = 1;
                return icon;
            }
            return null;
        }
    }
}
