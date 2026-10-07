// Native setting icons adapted from GTNH AE2 rv3-beta-1073-GTNH, LGPL-3.0-or-later; see META-INF/large-interface-port.
package com.xyp.gtnotgood.common.blocks.largeinterface;

import java.io.IOException;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.glodblock.github.inventory.IDualHost;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.config.AdvancedBlockingMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.InsertionMode;
import appeng.api.config.LockCraftingMode;
import appeng.api.config.Settings;
import appeng.api.config.SidelessMode;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.parts.IPart;
import appeng.client.gui.AEBaseGui;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.helpers.DualityInterface;
import appeng.me.cache.CraftingGridCache;
import appeng.util.PatternMultiplierHelper;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.util.PatternSlot;

/** Real pattern slots share the native interface inventories and settings; only the central grid is enlarged. */
public final class LargeInterfaceGui {

    private static final int COLUMNS = 9;
    private static final int VISIBLE_ROWS = 4;
    private static final int SLOT_SIZE = 18;
    private static final UITexture[] ICONS = new UITexture[256];
    private final LargeInterfaceHost host;
    private final DualityInterface duality;
    private final PanelSyncManager sync;
    private final BooleanSupplier authorized;

    private LargeInterfaceGui(LargeInterfaceHost host, PanelSyncManager sync, BooleanSupplier authorized) {
        this.host = host;
        this.duality = host.getInterfaceDuality();
        this.sync = sync;
        this.authorized = authorized;
    }

    public static ModularPanel build(LargeInterfaceHost host, PanelSyncManager sync, BooleanSupplier authorized) {
        return new LargeInterfaceGui(host, sync, authorized).build();
    }

    private ModularPanel build() {
        ModularPanel panel = ModularPanel.defaultPanel("large_interface", 212, 207)
            .background(GTGuiTextures.BACKGROUND_STANDARD);
        // #tr gui.large_interface.title
        // # Large ME Dual Interface
        // # zh_CN 大容量 ME 二合一接口
        panel.child(
            IKey.lang("gui.large_interface.title")
                .asWidget()
                .pos(8, 7)
                .size(172, 10));
        // #tr gui.large_interface.patterns
        // # Patterns (900)
        // # zh_CN 样板（900）
        panel.child(
            IKey.lang("gui.large_interface.patterns")
                .asWidget()
                .pos(8, 21)
                .size(166, 9));
        panel.child(patternGrid().pos(8, 32));
        addUpgrades(panel);
        addSettings(panel);
        var priorityPanel = sync
            .syncedPanel("large_interface_priority", true, (manager, handler) -> priorityPanel(manager));
        panel.child(
            new ButtonWidget<>().name("priority")
                .pos(184, 5)
                .size(18)
                .overlay(icon(66))
                .addTooltipLine(GuiText.Priority.getLocal())
                .onMousePressed(mouse -> {
                    if (mouse != 0 && mouse != 1) return false;
                    priorityPanel.openPanel();
                    return true;
                }));
        panel.child(
            IKey.lang("container.inventory")
                .asWidget()
                .pos(8, 109));
        panel.child(
            SlotGroupWidget.playerInventory((index, slot) -> slot.background(GTGuiTextures.SLOT_ITEM_STANDARD))
                .pos(8, 120));
        return panel;
    }

    private Grid patternGrid() {
        IInventory patterns = duality.getPatterns();
        InvWrapper inventory = new InvWrapper(patterns);
        PatternActions actions = new PatternActions();
        sync.syncValue("patternActions", actions);
        int rows = patterns.getSizeInventory() / COLUMNS;
        sync.registerSlotGroup("patterns", rows);
        return new Grid().name("pattern_grid")
            .minColWidth(SLOT_SIZE)
            .gridOfWidthHeight(COLUMNS, rows, (x, y, index) -> new PatternSlot() {

                @Override
                @SideOnly(Side.CLIENT)
                public boolean onMouseScroll(UpOrDown direction, int amount) {
                    if (!AEBaseGui.isCtrlKeyDown() || getSlot().getStack() == null) return false;
                    actions.change(index, direction.modifier > 0 ? 1 : -1);
                    return true;
                }
            }.slot(
                new AuthorizedSlot(inventory, index, 1).slotGroup("patterns")
                    .filter(stack -> stack.getItem() instanceof ICraftingPatternItem)))
            .size(COLUMNS * SLOT_SIZE + 4, VISIBLE_ROWS * SLOT_SIZE)
            .scrollable();
    }

    private void addUpgrades(ModularPanel panel) {
        InvWrapper upgrades = new InvWrapper(duality.getUpgrades());
        sync.registerSlotGroup("upgrades", 1);
        for (int i = 0; i < 4; i++) {
            panel.child(
                new ItemSlot().name("upgrade_" + i)
                    .slot(
                        new AuthorizedSlot(upgrades, i, 1).slotGroup("upgrades")
                            .canDragInto(false))
                    .background(GTGuiTextures.SLOT_ITEM_STANDARD)
                    .pos(184, 32 + i * SLOT_SIZE));
        }
    }

    private void addSettings(ModularPanel panel) {
        panel.child(setting(Settings.BLOCK, -20, 8, null));
        panel.child(setting(Settings.SMART_BLOCK, -38, 8, null));
        panel.child(setting(Settings.INTERFACE_TERMINAL, -20, 26, null));
        panel.child(setting(Settings.INSERTION_MODE, -20, 44, null));
        panel.child(
            new ButtonWidget<>().name("double_patterns")
                .pos(-20, 62)
                .size(18)
                .overlay(icon(71))
                .addTooltipLine(ButtonToolTips.DoublePatterns.getLocal())
                .addTooltipLine(ButtonToolTips.DoublePatternsHint.getLocal())
                .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                    if (!mouse.isClient() && authorized.getAsBoolean()
                        && (mouse.mouseButton == 0 || mouse.mouseButton == 1)) {
                        multiplyPatterns(mouse);
                    }
                })));
        panel.child(setting(Settings.PATTERN_OPTIMIZATION, -20, 80, null));
        panel.child(setting(Settings.ADVANCED_BLOCKING_MODE, -20, 98, Upgrades.ADVANCED_BLOCKING));
        panel.child(setting(Settings.LOCK_CRAFTING_MODE, -20, 116, Upgrades.LOCK_CRAFTING));
        panel.child(setting(Settings.FUZZY_MODE, -20, 134, Upgrades.FUZZY));
        if (host instanceof IDualHost && !(host instanceof IPart)) {
            panel.child(setting(Settings.SIDELESS_MODE, -20, 152, null));
        }
    }

    private ButtonWidget<?> setting(Settings setting, int x, int y, Upgrades required) {
        IntSyncValue value = new IntSyncValue(
            () -> duality.getConfigManager()
                .getSetting(setting)
                .ordinal());
        sync.syncValue("setting_" + setting.name(), value);
        IDrawable drawable = (context, dx, dy, w, h, theme) -> icon(settingIcon(setting, value.getIntValue()))
            .draw(context, dx, dy, w, h, theme);
        ButtonWidget<?> button = new ButtonWidget<>().name(
            setting.name()
                .toLowerCase(Locale.ROOT))
            .pos(x, y)
            .size(18)
            .overlay(drawable)
            .tooltip(t -> t.addLine(IKey.dynamic(() -> settingTooltip(setting, value.getIntValue()))))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                if (mouse.isClient() || !authorized.getAsBoolean()
                    || (mouse.mouseButton != 0 && mouse.mouseButton != 1)) return;
                if (required != null && duality.getInstalledUpgrades(required) <= 0) return;
                var manager = duality.getConfigManager();
                manager.putSetting(
                    setting,
                    Platform
                        .rotateEnum(manager.getSetting(setting), mouse.mouseButton == 1, setting.getPossibleValues()));
                host.saveChanges();
            }));
        if (required != null) {
            BooleanSyncValue installed = new BooleanSyncValue(() -> duality.getInstalledUpgrades(required) > 0);
            sync.syncValue("upgrade_" + required.name(), installed);
            button.setEnabledIf(widget -> installed.getBoolValue());
        }
        return button;
    }

    private ModularPanel priorityPanel(PanelSyncManager manager) {
        ModularPanel panel = ModularPanel.defaultPanel("large_interface_priority", 176, 125)
            .background(GTGuiTextures.BACKGROUND_STANDARD);
        IntSyncValue priority = new IntSyncValue(
            duality::getPriority,
            value -> {
                if (!host.getTileEntity()
                    .getWorldObj().isRemote && authorized.getAsBoolean()) duality.setPriority(value);
            }).allowC2S();
        manager.syncValue("priority", priority);
        panel.child(
            IKey.str(GuiText.Priority.getLocal())
                .asWidget()
                .pos(8, 7));
        panel.child(
            new TextFieldWidget().name("priority_value")
                .pos(40, 54)
                .size(96, 18)
                .value(priority)
                .numbersInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        int[] steps = { 1, 10, 100, 1000 };
        if (AEConfig.instance != null) {
            for (int i = 0; i < steps.length; i++) steps[i] = AEConfig.instance.priorityByStacksAmounts(i);
        }
        for (int row = 0; row < 2; row++) for (int col = 0; col < steps.length; col++) {
            int amount = (row == 0 ? 1 : -1) * steps[col];
            panel.child(
                new ButtonWidget<>().name("priority_" + row + "_" + col)
                    .pos(8 + col * 40, row == 0 ? 29 : 80)
                    .size(38, 20)
                    .overlay(IKey.str((amount > 0 ? "+" : "") + amount))
                    .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                        if (mouse.isClient() || !authorized.getAsBoolean() || mouse.mouseButton != 0) return;
                        long result = (long) duality.getPriority() + amount;
                        duality.setPriority((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, result)));
                    })));
        }
        panel.child(
            ButtonWidget.panelCloseButton()
                .pos(154, 5)
                .size(16));
        return panel;
    }

    private void multiplyPatterns(MouseData mouse) {
        CraftingGridCache.pauseRebuilds();
        try {
            for (int i = 0; i < duality.getPatterns()
                .getSizeInventory(); i++) {
                modifyPattern(i, mouse.mouseButton == 1 ? -1 : 1, mouse.shift);
            }
        } finally {
            CraftingGridCache.unpauseRebuilds();
        }
        host.saveChanges();
    }

    /** Native multipliers retain Ultimate Encoded Pattern item and fluid NBT and skip crafting patterns. */
    private void modifyPattern(int index, int direction, boolean fast) {
        IInventory patterns = duality.getPatterns();
        if (index < 0 || index >= patterns.getSizeInventory()) return;
        ItemStack stack = patterns.getStackInSlot(index);
        if (stack == null || !(stack.getItem() instanceof ICraftingPatternItem item)) return;
        ICraftingPatternDetails details = item.getPatternForItem(
            stack,
            host.getTileEntity()
                .getWorldObj());
        if (details == null || details.isCraftable()) return;
        int maximum = direction < 0 ? PatternMultiplierHelper.getMaxBitDivider(details)
            : PatternMultiplierHelper.getMaxBitMultiplier(details);
        if (maximum <= 0) return;
        ItemStack copy = stack.copy();
        PatternMultiplierHelper.applyModification(copy, direction * (fast ? Math.min(3, maximum) : 1));
        patterns.setInventorySlotContents(index, copy);
    }

    private static UITexture icon(int index) {
        if (ICONS[index] == null) {
            ICONS[index] = UITexture.builder()
                .location(ModList.AE2.getResourceLocation(), "guis/states")
                .imageSize(256, 256)
                .subAreaXYWH(index % 16 * 16, index / 16 * 16, 16, 16)
                .build();
        }
        return ICONS[index];
    }

    private static int settingIcon(Settings setting, int ordinal) {
        return switch (setting) {
            case BLOCK -> ordinal == YesNo.YES.ordinal() ? 21 : 20;
            case SMART_BLOCK -> ordinal == YesNo.YES.ordinal() ? 25 : 21;
            case INTERFACE_TERMINAL -> ordinal == YesNo.YES.ordinal() ? 90 : 91;
            case INSERTION_MODE -> 147 + ordinal;
            case PATTERN_OPTIMIZATION -> ordinal == YesNo.YES.ordinal() ? 178 : 194;
            case ADVANCED_BLOCKING_MODE -> 152 + ordinal;
            case LOCK_CRAFTING_MODE -> new int[] { 10, 2, 0, 1, 7 }[clamp(ordinal, 5)];
            case FUZZY_MODE -> switch (FuzzyMode.values()[clamp(ordinal, FuzzyMode.values().length)]) {
                    case PERCENT_1 -> 106;
                    case PERCENT_10 -> 105;
                    case PERCENT_25 -> 96;
                    case PERCENT_50 -> 97;
                    case PERCENT_75 -> 98;
                    case PERCENT_99 -> 99;
                    case IGNORE_ALL -> 100;
                };
            case SIDELESS_MODE -> 150 + ordinal;
            default -> 255;
        };
    }

    private static String settingTooltip(Settings setting, int ordinal) {
        return switch (setting) {
            case BLOCK -> text(
                ButtonToolTips.InterfaceBlockingMode,
                ordinal == YesNo.YES.ordinal() ? ButtonToolTips.Blocking : ButtonToolTips.NonBlocking);
            case SMART_BLOCK -> text(
                ButtonToolTips.InterfaceSmartBlockingMode,
                ordinal == YesNo.YES.ordinal() ? ButtonToolTips.SmartBlocking : ButtonToolTips.NonSmartBlocking);
            case INTERFACE_TERMINAL -> GuiText.InterfaceTerminal.getLocal() + "\n"
                + GuiText.InterfaceTerminalHint.getLocal();
            case PATTERN_OPTIMIZATION -> GuiText.PatternOptimization.getLocal() + "\n"
                + GuiText.PatternOptimizationHint.getLocal();
            case INSERTION_MODE -> switch (InsertionMode.values()[clamp(ordinal, InsertionMode.values().length)]) {
                    case DEFAULT -> text(ButtonToolTips.InsertionModeDefault, ButtonToolTips.InsertionModeDefaultDesc);
                    case PREFER_EMPTY -> text(
                        ButtonToolTips.InsertionModePreferEmpty,
                        ButtonToolTips.InsertionModePreferEmptyDesc);
                    case ONLY_EMPTY -> text(
                        ButtonToolTips.InsertionModeOnlyEmpty,
                        ButtonToolTips.InsertionModeOnlyEmptyDesc);
                };
            case ADVANCED_BLOCKING_MODE -> ordinal == AdvancedBlockingMode.DEFAULT.ordinal()
                ? text(ButtonToolTips.AdvancedBlockingModeDefault, ButtonToolTips.AdvancedBlockingModeDefaultDesc)
                : text(ButtonToolTips.AdvancedBlockingModeAll, ButtonToolTips.AdvancedBlockingModeAllDesc);
            case LOCK_CRAFTING_MODE -> text(
                ButtonToolTips.LockCraftingMode,
                switch (LockCraftingMode.values()[clamp(ordinal, LockCraftingMode.values().length)]) {
                case NONE -> ButtonToolTips.LockCraftingModeNone;
                case LOCK_UNTIL_PULSE -> ButtonToolTips.LockCraftingUntilRedstonePulse;
                case LOCK_WHILE_HIGH -> ButtonToolTips.LockCraftingWhileRedstoneHigh;
                case LOCK_WHILE_LOW -> ButtonToolTips.LockCraftingWhileRedstoneLow;
                case LOCK_UNTIL_RESULT -> ButtonToolTips.LockCraftingUntilResultReturned;
                });
            case FUZZY_MODE -> text(
                ButtonToolTips.FuzzyMode,
                switch (FuzzyMode.values()[clamp(ordinal, FuzzyMode.values().length)]) {
                case PERCENT_1 -> ButtonToolTips.FZPercent_1;
                case PERCENT_10 -> ButtonToolTips.FZPercent_10;
                case PERCENT_25 -> ButtonToolTips.FZPercent_25;
                case PERCENT_50 -> ButtonToolTips.FZPercent_50;
                case PERCENT_75 -> ButtonToolTips.FZPercent_75;
                case PERCENT_99 -> ButtonToolTips.FZPercent_99;
                case IGNORE_ALL -> ButtonToolTips.FZIgnoreAll;
                });
            case SIDELESS_MODE -> ordinal == SidelessMode.SIDED.ordinal()
                ? text(ButtonToolTips.SidelessModeSided, ButtonToolTips.SidelessModeSidedDesc)
                : text(ButtonToolTips.SidelessModeSideless, ButtonToolTips.SidelessModeSidelessDesc);
            default -> "";
        };
    }

    private static int clamp(int ordinal, int count) {
        return Math.max(0, Math.min(count - 1, ordinal));
    }

    private static String text(ButtonToolTips title, ButtonToolTips description) {
        return title.getLocal() + "\n" + description.getLocal();
    }

    /** Permissions are evaluated after the container is bound, at each real inventory interaction. */
    private final class AuthorizedSlot extends ModularSlot {

        private final int limit;

        AuthorizedSlot(IItemHandler inventory, int index, int limit) {
            super(inventory, index);
            this.limit = limit;
        }

        @Override
        public int getItemStackLimit(@Nonnull ItemStack stack) {
            return limit;
        }

        @Override
        public int getSlotStackLimit() {
            return limit;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return (host.getTileEntity()
                .getWorldObj().isRemote || authorized.getAsBoolean()) && super.isItemValid(stack);
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return (host.getTileEntity()
                .getWorldObj().isRemote || authorized.getAsBoolean()) && super.canTakeStack(player);
        }
    }

    /** Ctrl-wheel edits a single server-owned processing pattern without moving its physical item. */
    private final class PatternActions extends SyncHandler<PatternActions> {

        PatternActions() {
            allowC2S();
        }

        void change(int index, int direction) {
            syncToServer(0, buffer -> {
                buffer.writeVarIntToBuffer(index);
                buffer.writeByte(direction);
            });
        }

        @Override
        public void readOnClient(int id, PacketBuffer buffer) throws IOException {}

        @Override
        public void readOnServer(int id, PacketBuffer buffer) throws IOException {
            if (id != 0 || !authorized.getAsBoolean()) return;
            int index = buffer.readVarIntFromBuffer();
            int direction = buffer.readByte();
            if (direction != 1 && direction != -1) return;
            modifyPattern(index, direction, false);
            host.saveChanges();
        }
    }
}
