package com.xyp.gtnotgood.common.blocks.stockio;

import static gregtech.api.modularui2.GTGuis.createPopUpPanel;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.FluidDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.fluid.FluidInteractions;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import gregtech.api.modularui2.GTGuiTextures;

/** Item and fluid stocking pages share the super ME input GUI's paired, synchronized scroll layout. */
public final class StockIOGui {

    private static final int columns = 9;
    private static final int rows = StockIOLogic.SLOT_COUNT / columns;
    private static final int gridWidth = columns * 18 + 4;
    private static final int gridHeight = 4 * 18;
    private final StockIOLogic logic;
    private final PanelSyncManager sync;
    private final BooleanSupplier authorized;
    private final ItemSamples itemSamples;
    private final FluidSample[] fluidSamples = new FluidSample[StockIOLogic.SLOT_COUNT];
    private final IntSyncValue selectedItem;
    private final IntSyncValue selectedFluid;
    private int itemSelection;
    private int fluidSelection;

    private StockIOGui(StockIOLogic logic, PanelSyncManager sync, BooleanSupplier authorized) {
        this.logic = logic;
        this.sync = sync;
        this.authorized = authorized;
        itemSamples = new ItemSamples();
        selectedItem = new IntSyncValue(() -> itemSelection, value -> itemSelection = clampSlot(value)).allowC2S();
        selectedFluid = new IntSyncValue(() -> fluidSelection, value -> fluidSelection = clampSlot(value)).allowC2S();
        for (int i = 0; i < fluidSamples.length; i++) fluidSamples[i] = new FluidSample(i);
    }

    /** Builds the same inventory view for a full block or an AE cable part. */
    public static ModularPanel build(StockIOLogic logic, PanelSyncManager sync) {
        return build(logic, sync, logic::isServerSide);
    }

    /**
     * Checks the factory's live permission predicate only inside server-side interaction handlers.
     *
     * @param authorized verifies the original host, distance and ME BUILD permission after the container is bound
     * @return the shared stocking panel
     */
    public static ModularPanel build(StockIOLogic logic, PanelSyncManager sync, BooleanSupplier authorized) {
        return new StockIOGui(logic, sync, authorized).build();
    }

    private ModularPanel build() {
        ModularPanel panel = ModularPanel.defaultPanel("stock_io", 366, 282)
            .background(GTGuiTextures.BACKGROUND_STANDARD);
        // #tr gui.stock_io.title
        // # ME Inventory IO Interface
        // # zh_CN 库存 IO 接口 (ME)
        panel.child(
            IKey.lang("gui.stock_io.title")
                .asWidget()
                .pos(8, 7)
                .size(350, 12));
        BooleanSyncValue online = new BooleanSyncValue(logic::isOnline);
        BooleanSyncValue connected = new BooleanSyncValue(logic::isTargetConnected);
        BooleanSyncValue enabled = toggleValue("enabled", logic::isEnabled, logic::setEnabled);
        sync.syncValue("online", online);
        sync.syncValue("connected", connected);
        panel.child(IKey.dynamic(() -> {
            // #tr gui.stock_io.offline
            // # Offline: check power and channel
            // # zh_CN 离线：检查供电与频道
            if (!online.getBoolValue()) return IKey.lang("gui.stock_io.offline")
                .get();
            // #tr gui.stock_io.no_target
            // # No adjacent inventory
            // # zh_CN 未找到相邻容器
            if (!connected.getBoolValue()) return IKey.lang("gui.stock_io.no_target")
                .get();
            // #tr gui.stock_io.running
            // # Running
            // # zh_CN 运行中
            if (enabled.getBoolValue()) return IKey.lang("gui.stock_io.running")
                .get();
            // #tr gui.stock_io.paused
            // # Paused
            // # zh_CN 已暂停
            return IKey.lang("gui.stock_io.paused")
                .get();
        })
            .color(() -> online.getBoolValue() && connected.getBoolValue() ? 0xFF287034 : 0xFF902E2E)
            .asWidget()
            .pos(194, 29)
            .size(164, 10));

        sync.syncValue("selectedItem", selectedItem);
        sync.syncValue("selectedFluid", selectedFluid);
        IPanelHandler settings = sync.syncedPanel("stockSettings", true, (manager, handler) -> settingsPanel(panel));
        IPanelHandler itemPolicy = sync
            .syncedPanel("itemPolicy", true, (manager, handler) -> policyPanel(panel, false));
        IPanelHandler fluidPolicy = sync
            .syncedPanel("fluidPolicy", true, (manager, handler) -> policyPanel(panel, true));
        PagedWidget.Controller controller = new PagedWidget.Controller();
        panel.child(
            new PagedWidget<>().controller(controller)
                .pos(8, 49)
                .size(350, 86)
                .addPage(page(false, itemPolicy, settings))
                .addPage(page(true, fluidPolicy, settings)));
        // #tr gui.stock_io.items
        // # Items
        // # zh_CN 物品
        panel.child(tab(controller, 0, IKey.lang("gui.stock_io.items")).pos(8, 25));
        // #tr gui.stock_io.fluids
        // # Fluids
        // # zh_CN 流体
        panel.child(tab(controller, 1, IKey.lang("gui.stock_io.fluids")).pos(86, 25));
        // #tr gui.stock_io.samples_hint
        // # Samples are not consumed. Right-click a sample to configure its quantities.
        // # zh_CN 样本不消耗，右键样本设置保留量与固定量。
        panel.child(
            IKey.lang("gui.stock_io.samples_hint")
                .asWidget()
                .pos(8, 141)
                .size(350, 10));

        // #tr gui.stock_io.enabled
        // # Enabled
        // # zh_CN 运行
        IKey enabledLabel = IKey.lang("gui.stock_io.enabled");
        // #tr gui.stock_io.enabled_help
        // # Allow the target machine to read ME directly and recycle its outputs.
        // # zh_CN 允许目标机器直接读取 ME，并执行已启用的回收。
        IKey enabledHelp = IKey.lang("gui.stock_io.enabled_help");
        panel.child(toggle(enabled, enabledLabel, enabledHelp).pos(8, 157));
        // #tr gui.stock_io.limited
        // # Reserve
        // # zh_CN 限制
        IKey limitedLabel = IKey.lang("gui.stock_io.limited");
        // #tr gui.stock_io.limited_help
        // # Keep each resource's configured reserve in ME.
        // # zh_CN 每种资源在 ME 中保留设定数量。
        IKey limitedHelp = IKey.lang("gui.stock_io.limited_help");
        panel.child(
            toggle(toggleValue("limited", logic::isLimitedMode, logic::setLimitedMode), limitedLabel, limitedHelp)
                .pos(97, 157));
        // #tr gui.stock_io.fixed
        // # Fixed
        // # zh_CN 固定
        IKey fixedLabel = IKey.lang("gui.stock_io.fixed");
        // #tr gui.stock_io.fixed_help
        // # Offer the configured amount per recipe or fuel check; wait if ME has less.
        // # zh_CN 每次配方或燃料检查提供设定量，ME 不足则等待。
        IKey fixedHelp = IKey.lang("gui.stock_io.fixed_help");
        panel.child(
            toggle(toggleValue("fixed", logic::isFixedMode, logic::setFixedMode), fixedLabel, fixedHelp).pos(186, 157));
        // #tr gui.stock_io.recycle
        // # Recycle
        // # zh_CN 回收
        IKey recycleLabel = IKey.lang("gui.stock_io.recycle");
        // #tr gui.stock_io.recycle_help
        // # Return GT item and fluid outputs to ME without extracting its inputs.
        // # zh_CN 回收 GT 机器的物品与流体输出，不抽取输入。
        IKey recycleHelp = IKey.lang("gui.stock_io.recycle_help");
        panel.child(
            toggle(toggleValue("recycle", logic::isRecycle, logic::setRecycle), recycleLabel, recycleHelp)
                .pos(275, 157));
        addTarget(panel);
        panel.child(
            SlotGroupWidget.playerInventory((index, slot) -> slot.background(GTGuiTextures.SLOT_ITEM_STANDARD))
                .pos(102, 200));
        return panel;
    }

    private ParentWidget<?> page(boolean fluid, IPanelHandler policy, IPanelHandler settings) {
        ParentWidget<?> page = new ParentWidget<>().size(350, 86);
        // #tr gui.stock_io.marked
        // # Marked samples (900)
        // # zh_CN 标记样本（900）
        page.child(
            IKey.lang("gui.stock_io.marked")
                .asWidget()
                .pos(0, 0)
                .size(gridWidth, 10));
        // #tr gui.stock_io.available
        // # Available from ME
        // # zh_CN ME 可用量
        page.child(
            IKey.lang("gui.stock_io.available")
                .asWidget()
                .pos(184, 0)
                .size(gridWidth, 10));
        BooleanSyncValue autoPull = toggleValue(
            fluid ? "autoFluids" : "autoItems",
            fluid ? logic::isAutoPullFluids : logic::isAutoPullItems,
            fluid ? logic::setAutoPullFluids : logic::setAutoPullItems);
        GenericSyncValue<long[][], ?> amounts = amounts(fluid);
        VerticalScrollData scroll = new VerticalScrollData();
        Grid samples = new Grid()
            .gridOfWidthHeight(
                columns,
                rows,
                (x, y, index) -> fluid ? fluidSlot(index, autoPull, policy) : itemSlot(index, autoPull, policy))
            .size(gridWidth, gridHeight)
            .scrollable(scroll)
            .pos(0, 14);
        Grid stocks = new Grid()
            .gridOfWidthHeight(
                columns,
                rows,
                (x, y, index) -> new StockDisplay(
                    fluid ? null : () -> itemSamples.getStackInSlot(index),
                    fluid ? fluidSamples[index]::getFluid : null,
                    () -> amounts.getValue()[0][index],
                    () -> amounts.getValue()[1][index]))
            .size(gridWidth, gridHeight)
            .scrollable(scroll)
            .pos(184, 14);
        page.child(samples)
            .child(stocks);
        // #tr gui.stock_io.autopull
        // # Auto-mark resources from ME. Right-click: refresh and minimum quantities.
        // # zh_CN 自动标记 ME 资源。右键：设置刷新间隔与最小数量。
        IKey autoPullHelp = IKey.lang("gui.stock_io.autopull");
        page.child(new ToggleButton() {

            @Override
            public Result onMousePressed(int button) {
                if (button == 1) {
                    settings.openPanel();
                    return Result.SUCCESS;
                }
                return super.onMousePressed(button);
            }
        }.value(autoPull)
            .size(16)
            .pos(168, 14)
            .background(false, GTGuiTextures.BUTTON_STANDARD)
            .background(true, GTGuiTextures.BUTTON_STANDARD_PRESSED)
            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_AUTOPULL_ME)
            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_AUTOPULL_ME_DISABLED)
            .addTooltipLine(autoPullHelp));
        page.child(
            GTGuiTextures.PICTURE_ARROW_DOUBLE.asWidget()
                .size(12)
                .pos(170, 40));
        // #tr gui.stock_io.settings
        // # Auto-mark settings
        // # zh_CN 自动标记设置
        IKey settingsHelp = IKey.lang("gui.stock_io.settings");
        page.child(
            new ButtonWidget<>().size(16)
                .pos(168, 64)
                .background(GTGuiTextures.BUTTON_STANDARD)
                .overlay(IKey.str("..."))
                .addTooltipLine(settingsHelp)
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    settings.openPanel();
                    return true;
                }));
        return page;
    }

    private PhantomItemSlot itemSlot(int index, BooleanSyncValue locked, IPanelHandler policy) {
        ModularSlot slot = new ModularSlot(itemSamples, index) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack != null && stack.getItem() != null && stack.stackSize > 0;
            }

            @Override
            public int getItemStackLimit(ItemStack stack) {
                return 1;
            }
        }.singletonSlotGroup();
        PhantomItemSlotSH handler = new PhantomItemSlotSH(slot) {

            @Override
            protected void phantomClick(MouseData mouse, ItemStack cursor) {
                if (!canEdit() || mouse.mouseButton != 0 || logic.isAutoPullItems()) return;
                if (cursor != null && !slot.isItemValid(cursor)) return;
                ItemStack copy = cursor == null ? null : cursor.copy();
                if (copy != null) copy.stackSize = 1;
                slot.putStack(copy);
            }

            @Override
            protected void phantomScroll(MouseData mouse) {}
        };
        // #tr gui.stock_io.configure
        // # Right-click: reserve and fixed quantity
        // # zh_CN 右键：设置保留量与固定量
        IKey configure = IKey.lang("gui.stock_io.configure");
        PhantomItemSlot widget = new PhantomItemSlot() {

            @Override
            public Result onMousePressed(int button) {
                if (button == 1 && getSlot().getStack() != null) {
                    selectedItem.setIntValue(index);
                    policy.openPanel();
                    return Result.SUCCESS;
                }
                return super.onMousePressed(button);
            }

            @Override
            public boolean onMouseScroll(UpOrDown direction, int amount) {
                return false;
            }
        };
        widget.syncHandler(handler);
        widget.background(GTGuiTextures.SLOT_ITEM_STANDARD, GTGuiTextures.OVERLAY_SLOT_ARROW_ME);
        widget.tooltip(t -> {
            t.addLine(configure);
            if (locked.getBoolValue()) t.addLine(IKey.lang("GT5U.machines.stocking_bus.cannot_set_slot"));
        });
        return widget;
    }

    private FluidSlot fluidSlot(int index, BooleanSyncValue locked, IPanelHandler policy) {
        FluidSlotSyncHandler handler = new FluidSlotSyncHandler(fluidSamples[index]) {

            @Override
            protected void tryClickPhantom(MouseData mouse, ItemStack cursor) {
                if (!canEdit() || logic.isAutoPullFluids() || mouse.mouseButton != 0) return;
                fluidSamples[index].sample(FluidInteractions.getFluidForItem(cursor));
            }

            @Override
            public void tryScrollPhantom(MouseData mouse) {}

            @Override
            public void readOnServer(int id, PacketBuffer buffer) {
                if (!canEdit() || id == SYNC_VALUE || logic.isAutoPullFluids()) return;
                super.readOnServer(id, buffer);
            }
        }.phantom(true)
            .controlsAmount(false);
        return new FluidSlot() {

            @Override
            public Result onMousePressed(int button) {
                if (button == 1 && getFluidStack() != null) {
                    selectedFluid.setIntValue(index);
                    policy.openPanel();
                    return Result.SUCCESS;
                }
                return super.onMousePressed(button);
            }

            @Override
            public boolean onMouseScroll(UpOrDown direction, int amount) {
                return false;
            }

            @Override
            protected void addToolTip(RichTooltip tooltip) {
                FluidStack stack = getFluidStack();
                if (stack != null) tooltip.addFromFluid(stack);
                tooltip.addLine(IKey.lang("gui.stock_io.configure"));
                if (locked.getBoolValue()) tooltip.addLine(IKey.lang("GT5U.machines.stocking_bus.cannot_set_slot"));
                else tooltip.addLine(IKey.lang("modularui2.fluid.phantom.clear"));
            }
        }.syncHandler(handler)
            .background(GTGuiTextures.SLOT_FLUID_STANDARD, GTGuiTextures.OVERLAY_SLOT_ARROW_ME);
    }

    private ModularPanel policyPanel(ModularPanel parent, boolean fluid) {
        IntSyncValue selection = fluid ? selectedFluid : selectedItem;
        LongSyncValue reserve = new LongSyncValue(
            () -> logic.getPolicy(fluid, selection.getIntValue()).reserve,
            value -> {
                if (!canEdit()) return;
                logic.getPolicy(fluid, selection.getIntValue()).reserve = Math.max(0, value);
                logic.policyChanged();
            }).allowC2S();
        IntSyncValue batch = new IntSyncValue(() -> logic.getPolicy(fluid, selection.getIntValue()).batch, value -> {
            if (!canEdit()) return;
            logic.getPolicy(fluid, selection.getIntValue()).batch = Math.max(1, value);
            logic.policyChanged();
        }).allowC2S();
        Flow content = Flow.col()
            .coverChildren()
            .childPadding(4)
            .marginTop(16);
        content.child(
            IKey.dynamic(() -> sampleName(fluid, selection.getIntValue()))
                .asWidget()
                .maxWidth(204));
        // #tr gui.stock_io.reserve_quantity
        // # Keep in ME (items / mB)
        // # zh_CN ME 保留量（个 / mB）
        content.child(
            IKey.lang("gui.stock_io.reserve_quantity")
                .asWidget());
        content.child(
            new TextFieldWidget().value(reserve)
                .numbersLong(0, Long.MAX_VALUE)
                .formatAsInteger(true)
                .setMaxLength(19)
                .width(192));
        // #tr gui.stock_io.fixed_quantity
        // # Per check (items / mB)
        // # zh_CN 单次检查可用量（个 / mB）
        content.child(
            IKey.lang("gui.stock_io.fixed_quantity")
                .asWidget());
        content.child(
            new TextFieldWidget().value(batch)
                .numbersInt(1, Integer.MAX_VALUE)
                .formatAsInteger(true)
                .setMaxLength(10)
                .width(192));
        // #tr gui.stock_io.fixed_hint
        // # Waits for the full amount; recipes and fuels debit only actual use.
        // # zh_CN 不足设定量则等待，只扣除配方或燃料的实际消耗。
        content.child(
            IKey.lang("gui.stock_io.fixed_hint")
                .asWidget()
                .maxWidth(204));
        return createPopUpPanel(fluid ? "fluidPolicy" : "itemPolicy").size(224, 164)
            .relative(parent)
            .center()
            .padding(8)
            .child(content);
    }

    private ModularPanel settingsPanel(ModularPanel parent) {
        IntSyncValue minimumItems = new IntSyncValue(
            logic::getMinItemAutoPull,
            value -> { if (canEdit()) logic.setMinItemAutoPull(value); }).allowC2S();
        IntSyncValue minimumFluids = new IntSyncValue(
            logic::getMinFluidAutoPull,
            value -> { if (canEdit()) logic.setMinFluidAutoPull(value); }).allowC2S();
        IntSyncValue refresh = new IntSyncValue(
            logic::getRefreshTime,
            value -> { if (canEdit()) logic.setRefreshTime(value); }).allowC2S();
        Flow content = Flow.col()
            .coverChildren()
            .childPadding(3)
            .marginTop(16);
        content.child(
            IKey.lang("GT5U.machines.stocking_bus.min_stack_size")
                .asWidget());
        content.child(numberField(minimumItems));
        content.child(
            IKey.lang("GT5U.machines.stocking_hatch.min_amount")
                .asWidget());
        content.child(numberField(minimumFluids));
        content.child(
            IKey.lang("GT5U.machines.stocking_bus.refresh_time")
                .asWidget());
        content.child(numberField(refresh));
        return createPopUpPanel("stockSettings").size(224, 174)
            .relative(parent)
            .center()
            .padding(8)
            .child(content);
    }

    private void addTarget(ModularPanel panel) {
        IntSyncValue side = new IntSyncValue(
            () -> logic.getTargetSide()
                .ordinal(),
            value -> {
                if (canEdit() && logic.canSelectTargetSide()) {
                    logic.setTargetSide(ForgeDirection.getOrientation(Math.floorMod(value, 6)));
                }
            }).allowC2S();
        BooleanSyncValue selectable = new BooleanSyncValue(logic::canSelectTargetSide);
        StringSyncValue target = new StringSyncValue(logic::targetNameKey);
        sync.syncValue("targetSide", side);
        sync.syncValue("targetSelectable", selectable);
        sync.syncValue("targetName", target);
        // #tr gui.stock_io.target_side
        // # Inventory-facing side. Click to cycle.
        // # zh_CN 面向容器的方向，点击切换。
        IKey directionHelp = IKey.lang("gui.stock_io.target_side");
        panel.child(
            new ButtonWidget<>().size(84, 18)
                .pos(8, 179)
                .background(GTGuiTextures.BUTTON_STANDARD)
                .overlay(IKey.dynamic(() -> directionName(side.getIntValue())))
                .setEnabledIf(widget -> selectable.getBoolValue())
                .addTooltipLine(directionHelp)
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    side.setIntValue((side.getIntValue() + 1) % 6);
                    return true;
                }));
        // #tr gui.stock_io.target
        // # Target:
        // # zh_CN 目标：
        panel.child(
            IKey.dynamic(
                () -> IKey.lang("gui.stock_io.target")
                    .get() + " "
                    + (target.getValue()
                        .isEmpty() ? ""
                            : IKey.lang(target.getValue())
                                .get()))
                .asWidget()
                .pos(98, 183)
                .size(260, 10));
    }

    private BooleanSyncValue toggleValue(String name, BooleanSupplier getter, Consumer<Boolean> setter) {
        BooleanSyncValue value = new BooleanSyncValue(getter, enabled -> { if (canEdit()) setter.accept(enabled); })
            .allowC2S();
        sync.syncValue(name, value);
        return value;
    }

    private static ToggleButton toggle(BooleanSyncValue value, IKey caption, IKey help) {
        return new ToggleButton().value(value)
            .size(84, 18)
            .background(false, GTGuiTextures.BUTTON_STANDARD)
            .background(true, GTGuiTextures.BUTTON_STANDARD_PRESSED)
            .overlay(caption)
            .addTooltipLine(help);
    }

    private static PageButton tab(PagedWidget.Controller controller, int index, IKey caption) {
        return new PageButton(index, controller).size(74, 18)
            .background(false, GTGuiTextures.BUTTON_STANDARD)
            .background(true, GTGuiTextures.BUTTON_STANDARD_PRESSED)
            .overlay(caption);
    }

    private static TextFieldWidget numberField(IntSyncValue value) {
        return new TextFieldWidget().value(value)
            .numbersInt(1, Integer.MAX_VALUE)
            .formatAsInteger(true)
            .setMaxLength(10)
            .setTextAlignment(Alignment.CENTER)
            .width(192);
    }

    private String sampleName(boolean fluid, int index) {
        if (fluid) {
            FluidStack stack = fluidSamples[index].getFluid();
            return stack == null ? "" : stack.getLocalizedName();
        }
        ItemStack stack = itemSamples.getStackInSlot(index);
        return stack == null ? "" : stack.getDisplayName();
    }

    private static int clampSlot(int index) {
        return Math.max(0, Math.min(StockIOLogic.SLOT_COUNT - 1, index));
    }

    private boolean canEdit() {
        return logic.isServerSide() && authorized.getAsBoolean();
    }

    private static String directionName(int side) {
        switch (ForgeDirection.getOrientation(side)) {
            case DOWN:
                // #tr gui.stock_io.down
                // # Down
                // # zh_CN 下
                return IKey.lang("gui.stock_io.down")
                    .get();
            case UP:
                // #tr gui.stock_io.up
                // # Up
                // # zh_CN 上
                return IKey.lang("gui.stock_io.up")
                    .get();
            case NORTH:
                // #tr gui.stock_io.north
                // # North
                // # zh_CN 北
                return IKey.lang("gui.stock_io.north")
                    .get();
            case SOUTH:
                // #tr gui.stock_io.south
                // # South
                // # zh_CN 南
                return IKey.lang("gui.stock_io.south")
                    .get();
            case WEST:
                // #tr gui.stock_io.west
                // # West
                // # zh_CN 西
                return IKey.lang("gui.stock_io.west")
                    .get();
            case EAST:
                // #tr gui.stock_io.east
                // # East
                // # zh_CN 东
                return IKey.lang("gui.stock_io.east")
                    .get();
            default:
                return "";
        }
    }

    /** One read-only, sparse packet per resource type carries exact 64-bit available and network quantities. */
    private GenericSyncValue<long[][], ?> amounts(boolean fluid) {
        GenericSyncValue<long[][], ?> value = GenericSyncValue.builder(long[][].class)
            .getter(
                () -> new long[][] { fluid ? logic.offeredFluids : logic.offeredItems,
                    fluid ? logic.networkFluids : logic.networkItems })
            .copy(amounts -> new long[][] { amounts[0].clone(), amounts[1].clone() })
            .equals((first, second) -> Arrays.equals(first[0], second[0]) && Arrays.equals(first[1], second[1]))
            .serializer((buffer, amounts) -> {
                int nonzero = 0;
                for (int i = 0; i < StockIOLogic.SLOT_COUNT; i++) {
                    if (amounts[0][i] != 0 || amounts[1][i] != 0) nonzero++;
                }
                buffer.writeVarIntToBuffer(nonzero);
                for (int i = 0; i < StockIOLogic.SLOT_COUNT; i++) {
                    if (amounts[0][i] == 0 && amounts[1][i] == 0) continue;
                    buffer.writeVarIntToBuffer(i);
                    buffer.writeLong(amounts[0][i]);
                    buffer.writeLong(amounts[1][i]);
                }
            })
            .deserializer(buffer -> {
                long[][] amounts = new long[2][StockIOLogic.SLOT_COUNT];
                int count = buffer.readVarIntFromBuffer();
                if (count < 0 || count > StockIOLogic.SLOT_COUNT) throw new IOException("Invalid stock IO count");
                for (int i = 0; i < count; i++) {
                    int index = buffer.readVarIntFromBuffer();
                    if (index < 0 || index >= StockIOLogic.SLOT_COUNT) throw new IOException("Invalid stock IO slot");
                    amounts[0][index] = Math.max(0, buffer.readLong());
                    amounts[1][index] = Math.max(0, buffer.readLong());
                }
                return amounts;
            })
            .build();
        sync.syncValue(fluid ? "fluidAmounts" : "itemAmounts", value);
        return value;
    }

    /** Stock cells are draw-only; clicking them cannot extract ME resources or alter a sample. */
    private static final class StockDisplay extends Widget<StockDisplay> {

        private final Supplier<ItemStack> item;
        private final Supplier<FluidStack> fluid;
        private final LongSupplier available;
        private final LongSupplier network;

        StockDisplay(Supplier<ItemStack> item, Supplier<FluidStack> fluid, LongSupplier available,
            LongSupplier network) {
            this.item = item;
            this.fluid = fluid;
            this.available = available;
            this.network = network;
            size(18).background(GTGuiTextures.SLOT_ITEM_DARK)
                .disableHoverBackground();
            tooltip().setAutoUpdate(true)
                .tooltipBuilder(tooltip -> {
                    if (item != null) {
                        ItemStack stack = item.get();
                        if (stack == null) return;
                        tooltip.addFromItem(stack);
                    } else {
                        FluidStack stack = fluid.get();
                        if (stack == null) return;
                        tooltip.addFromFluid(stack);
                    }
                    String unit = fluid == null ? "" : " mB";
                    tooltip.addLine(
                        IKey.str(
                            IKey.lang("gui.stock_io.available")
                                .get() + ": "
                                + available.getAsLong()
                                + unit));
                    // #tr gui.stock_io.network_stock
                    // # Total in ME
                    // # zh_CN ME 总库存
                    tooltip.addLine(
                        IKey.str(
                            IKey.lang("gui.stock_io.network_stock")
                                .get() + ": "
                                + network.getAsLong()
                                + unit));
                });
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            if (item != null) {
                ItemStack stack = item.get();
                if (stack == null) return;
                new ItemDrawable(stack).draw(context, 1, 1, 16, 16, theme.getTheme());
            } else {
                FluidStack stack = fluid.get();
                if (stack == null) return;
                new FluidDrawable(stack).draw(context, 1, 1, 16, 16, theme.getTheme());
            }
        }

        @Override
        public void drawOverlay(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            super.drawOverlay(context, theme);
            long quantity = available.getAsLong();
            if (item != null ? item.get() == null : fluid.get() == null) return;
            IKey.str(compact(quantity))
                .color(0xFFFFFFFF)
                .shadow(true)
                .scale(0.5f)
                .alignment(Alignment.BottomRight)
                .draw(context, 1, 9, 16, 8, theme.getTheme());
        }
    }

    private static String compact(long amount) {
        if (amount < 1000) return Long.toString(amount);
        String[] units = { "k", "M", "G", "T", "P", "E" };
        double scaled = amount;
        int unit = -1;
        do {
            scaled /= 1000;
            unit++;
        } while (scaled >= 1000 && unit < units.length - 1);
        return (long) scaled + units[unit];
    }

    /** Client copies belong to this GUI; phantom sample synchronization never changes gameplay on the client. */
    private final class ItemSamples implements IItemHandlerModifiable {

        private final ItemStack[] client = new ItemStack[StockIOLogic.SLOT_COUNT];

        public int getSlots() {
            return StockIOLogic.SLOT_COUNT;
        }

        public int getSlotLimit(int slot) {
            return 1;
        }

        public ItemStack getStackInSlot(int slot) {
            return logic.isServerSide() ? logic.itemFilters[slot] : client[slot];
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
            if (logic.isServerSide()) {
                if (canEdit() && !logic.isAutoPullItems()) logic.setItemFilter(slot, copy);
            } else client[slot] = copy;
        }
    }

    /** Phantom tank stores one fluid identity; cursor containers and actual ME stock are untouched. */
    private final class FluidSample implements IFluidTank {

        private final int index;
        private FluidStack client;

        FluidSample(int index) {
            this.index = index;
        }

        public FluidStack getFluid() {
            return logic.isServerSide() ? logic.fluidFilters[index] : client;
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

        void sample(FluidStack stack) {
            FluidStack copy = stack == null ? null : stack.copy();
            if (copy != null) copy.amount = 1;
            if (logic.isServerSide()) {
                if (canEdit() && !logic.isAutoPullFluids()) logic.setFluidFilter(index, copy);
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
