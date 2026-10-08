// Native setting icons adapted from GTNH AE2 rv3-beta-1073-GTNH, LGPL-3.0-or-later; see META-INF/large-interface-port.
package com.xyp.gtnotgood.common.blocks.largeinterface;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.utils.item.IItemHandler;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.glodblock.github.inventory.IDualHost;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
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
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.parts.IPart;
import appeng.client.gui.AEBaseGui;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.helpers.DualityInterface;
import appeng.items.materials.MaterialType;
import appeng.me.cache.CraftingGridCache;
import appeng.util.PatternMultiplierHelper;
import appeng.util.Platform;
import appeng.util.inv.IUpgradeInventory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.gui.modularui.util.PatternSlot;

/** Real pattern slots share the native interface inventories and settings; only the central grid is enlarged. */
public final class LargeInterfaceGui {

    private static final int COLUMNS = 9;
    private static final int VISIBLE_ROWS = 4;
    private static final int SLOT_SIZE = 18;
    private static final UITexture[] ICONS = new UITexture[256];
    private static final Map<Upgrades, ItemStack> UPGRADE_CARDS = upgradeCards();
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
        ModularPanel panel = ModularPanel.defaultPanel("large_interface", 190, 207)
            .background(LargeInterfaceGuiTextures.BACKGROUND).disableHoverBackground();
        // #tr gui.large_interface.title
        // # Large ME Dual Interface
        // # zh_CN 大容量 ME 二合一接口
        panel.child(
            IKey.lang("gui.large_interface.title").color(LargeInterfaceGuiTextures.TEXT_COLOR).asWidget().pos(8, 7));
        // #tr gui.large_interface.patterns
        // # Patterns (900)
        // # zh_CN 样板（900）
        panel.child(IKey.lang("gui.large_interface.patterns").color(LargeInterfaceGuiTextures.TEXT_COLOR).asWidget()
            .pos(8, 21));
        panel.child(patternGrid().pos(8, 32));
        addUpgrades(panel);
        addSettings(panel);
        var priorityPanel = sync.syncedPanel("large_interface_priority", true,
            (manager, handler) -> priorityPanel(manager));
        panel
            .child(new ButtonWidget<>().name("priority").pos(166, -5).size(22).background(LargeInterfaceGuiTextures.TAB)
                .hoverBackground(LargeInterfaceGuiTextures.TAB_HOVER).overlay(icon(66).asIcon().size(16))
                .excludeAreaInRecipeViewer().addTooltipLine(GuiText.Priority.getLocal()).onMousePressed(mouse -> {
                    if (mouse != 0 && mouse != 1) return false;
                    priorityPanel.openPanel();
                    panel.setEnabled(false);
                    return true;
                }));
        panel
            .child(IKey.lang("container.inventory").color(LargeInterfaceGuiTextures.TEXT_COLOR).asWidget().pos(8, 109));
        panel.child(SlotGroupWidget.playerInventory((index, slot) -> slot.background(LargeInterfaceGuiTextures.SLOT))
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
        return new Grid().name("pattern_grid").minColWidth(SLOT_SIZE)
            .gridOfWidthHeight(COLUMNS, rows, (x, y, index) -> new PatternSlot() {

                @Override
                public IDrawable getCurrentBackground(WidgetThemeEntry<?> widgetTheme) {
                    return isSynced() && getSlot().getStack() != null ? LargeInterfaceGuiTextures.SLOT
                        : LargeInterfaceGuiTextures.PATTERN_SLOT;
                }

                @Override
                @SideOnly(Side.CLIENT)
                public boolean onMouseScroll(UpOrDown direction, int amount) {
                    if (!AEBaseGui.isCtrlKeyDown() || getSlot().getStack() == null) return false;
                    actions.change(index, direction.modifier > 0 ? 1 : -1);
                    return true;
                }
            }.disableThemeBackground(true).disableHoverThemeBackground(true)
                .slot(new AuthorizedSlot(inventory, index, 1).slotGroup("patterns")
                    .filter(stack -> stack.getItem() instanceof ICraftingPatternItem)))
            .size(COLUMNS * SLOT_SIZE + 12, VISIBLE_ROWS * SLOT_SIZE)
            .scrollable(new VerticalScrollData(false, 12).texture(LargeInterfaceGuiTextures.SCROLL_HANDLE))
            .showScrollShadows(false);
    }

    private void addUpgrades(ModularPanel panel) {
        InvWrapper upgrades = new InvWrapper(duality.getUpgrades());
        sync.registerSlotGroup("upgrades", 1);
        panel.child(
            LargeInterfaceGuiTextures.UPGRADES_FRAME.asWidget().pos(186, 30).size(28, 82).excludeAreaInRecipeViewer());
        for (int i = 0; i < 4; i++) {
            panel.child(new ItemSlot() {

                @Override
                protected void drawOverlay() {
                    if (isSynced() && getSlot().getStack() == null) {
                        LargeInterfaceGuiTextures.UPGRADE_SLOT_HINT.draw(getContext(), 0, 0, SLOT_SIZE, SLOT_SIZE,
                            getWidgetThemeInternal(getPanel().getTheme()).getTheme());
                    }
                    super.drawOverlay();
                }
            }.name("upgrade_" + i).slot(new AuthorizedSlot(upgrades, i, 1).slotGroup("upgrades").canDragInto(false))
                .background(LargeInterfaceGuiTextures.SLOT).tooltip(t -> t.addLine(IKey.dynamic(this::upgradeTooltip)))
                .pos(190, 35 + i * SLOT_SIZE));
        }
    }

    /** Native card lookup is cached once; displayed limits include cards already installed in the interface. */
    private static Map<Upgrades, ItemStack> upgradeCards() {
        Map<Upgrades, ItemStack> cards = new EnumMap<>(Upgrades.class);
        for (MaterialType material : MaterialType.VALUES) {
            if (!material.isRegistered() || material.getItemInstance() == null) continue;
            ItemStack stack = material.stack(1);
            if (stack.getItem() instanceof IUpgradeModule module) {
                Upgrades upgrade = module.getType(stack);
                if (upgrade != null) cards.putIfAbsent(upgrade, stack);
            }
        }
        return cards;
    }

    private String upgradeTooltip() {
        StringBuilder text = new StringBuilder(GuiText.Accepts.getLocal());
        IUpgradeInventory upgrades = (IUpgradeInventory) duality.getUpgrades();
        for (Upgrades upgrade : Upgrades.values()) {
            int maximum = upgrades.getMaxInstalled(upgrade);
            ItemStack card = UPGRADE_CARDS.get(upgrade);
            if (maximum <= 0 || card == null) continue;
            text.append("\n- ").append(card.getDisplayName());
            if (maximum > 1) text.append(" (").append(maximum).append(')');
        }
        return text.toString();
    }

    private void addSettings(ModularPanel panel) {
        panel.child(setting(Settings.BLOCK, -18, 1, null));
        panel.child(setting(Settings.SMART_BLOCK, -36, 1, null));
        panel.child(setting(Settings.INTERFACE_TERMINAL, -18, 23, null));
        panel.child(setting(Settings.INSERTION_MODE, -18, 45, null));
        panel.child(new ButtonWidget<>().name("double_patterns").pos(-18, 67).size(18, 20)
            .background(LargeInterfaceGuiTextures.SIDE_BUTTON)
            .hoverBackground(LargeInterfaceGuiTextures.SIDE_BUTTON_HOVER).overlay(sideIcon(icon(71)))
            .excludeAreaInRecipeViewer().addTooltipLine(ButtonToolTips.DoublePatterns.getLocal())
            .addTooltipLine(ButtonToolTips.DoublePatternsHint.getLocal())
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                if (
                    !mouse.isClient() && authorized.getAsBoolean() && (mouse.mouseButton == 0 || mouse.mouseButton == 1)
                ) {
                    multiplyPatterns(mouse);
                }
            })));
        panel.child(setting(Settings.PATTERN_OPTIMIZATION, -18, 89, null));
        panel.child(setting(Settings.ADVANCED_BLOCKING_MODE, -18, 111, Upgrades.ADVANCED_BLOCKING));
        panel.child(setting(Settings.LOCK_CRAFTING_MODE, -18, 133, Upgrades.LOCK_CRAFTING));
        panel.child(setting(Settings.FUZZY_MODE, -18, 155, Upgrades.FUZZY));
        if (host instanceof IDualHost && !(host instanceof IPart)) {
            panel.child(setting(Settings.SIDELESS_MODE, -18, 177, null));
        }
    }

    private ButtonWidget<?> setting(Settings setting, int x, int y, Upgrades required) {
        IntSyncValue value = new IntSyncValue(() -> duality.getConfigManager().getSetting(setting).ordinal());
        sync.syncValue("setting_" + setting.name(), value);
        IDrawable drawable = (context, dx, dy, w, h, theme) -> icon(settingIcon(setting, value.getIntValue()))
            .draw(context, dx, dy, w, h, theme);
        ButtonWidget<?> button = new ButtonWidget<>().name(setting.name().toLowerCase(Locale.ROOT)).pos(x, y)
            .size(18, 20).background(LargeInterfaceGuiTextures.SIDE_BUTTON)
            .hoverBackground(LargeInterfaceGuiTextures.SIDE_BUTTON_HOVER).overlay(sideIcon(drawable))
            .excludeAreaInRecipeViewer()
            .tooltip(t -> t.addLine(IKey.dynamic(() -> settingTooltip(setting, value.getIntValue()))))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                if (
                    mouse.isClient() || !authorized.getAsBoolean() || (mouse.mouseButton != 0 && mouse.mouseButton != 1)
                ) return;
                if (required != null && duality.getInstalledUpgrades(required) <= 0) return;
                var manager = duality.getConfigManager();
                manager.putSetting(setting, Platform.rotateEnum(manager.getSetting(setting), mouse.mouseButton == 1,
                    setting.getPossibleValues()));
                host.saveChanges();
            }));
        if (required != null) {
            BooleanSyncValue installed = new BooleanSyncValue(() -> duality.getInstalledUpgrades(required) > 0);
            sync.syncValue("upgrade_" + required.name(), installed);
            button.setEnabledIf(widget -> installed.getBoolValue());
        }
        return button;
    }

    private static IDrawable sideIcon(IDrawable icon) {
        return (context, x, y, width, height, theme) -> icon.draw(context, x + 1, y + 1, 16, 16, theme);
    }

    private ModularPanel priorityPanel(PanelSyncManager manager) {
        ModularPanel panel = ModularPanel.defaultPanel("large_interface_priority", 176, 125)
            .background(LargeInterfaceGuiTextures.PRIORITY_BACKGROUND).disableHoverBackground();
        panel.onCloseAction(() -> panel.getScreen().getMainPanel().setEnabled(true));
        IntSyncValue priority = new IntSyncValue(duality::getPriority, value -> {
            if (!host.getTileEntity().getWorldObj().isRemote && authorized.getAsBoolean()) duality.setPriority(value);
        }).allowC2S();
        manager.syncValue("priority", priority);
        panel.child(
            IKey.str(GuiText.Priority.getLocal()).color(LargeInterfaceGuiTextures.TEXT_COLOR).asWidget().pos(8, 6));
        panel.child(new TextFieldWidget() {

            @Override
            public IDrawable getCurrentBackground(WidgetThemeEntry<?> widgetTheme) {
                return isFocused() ? LargeInterfaceGuiTextures.TEXT_FIELD_FOCUSED
                    : LargeInterfaceGuiTextures.TEXT_FIELD;
            }
        }.name("priority_value").pos(60, 55).size(61, 12).background(LargeInterfaceGuiTextures.TEXT_FIELD)
            .disableHoverBackground().setTextColor(LargeInterfaceGuiTextures.TEXT_COLOR).value(priority)
            .numbersInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        int[] steps = { 1, 10, 100, 1000 };
        if (AEConfig.instance != null) {
            for (int i = 0; i < steps.length; i++) steps[i] = AEConfig.instance.priorityByStacksAmounts(i);
        }
        int[] positions = { 20, 48, 82, 120 };
        int[] widths = { 22, 28, 32, 38 };
        for (int row = 0; row < 2; row++) for (int col = 0; col < steps.length; col++) {
            int amount = (row == 0 ? 1 : -1) * steps[col];
            panel.child(new ButtonWidget<>().name("priority_" + row + "_" + col).pos(positions[col], row == 0 ? 30 : 72)
                .size(widths[col], 20).background(LargeInterfaceGuiTextures.BUTTON)
                .hoverBackground(LargeInterfaceGuiTextures.BUTTON_HOVER)
                .overlay(IKey.str((amount > 0 ? "+" : "") + amount).color(LargeInterfaceGuiTextures.TEXT_COLOR))
                .hoverOverlay(
                    IKey.str((amount > 0 ? "+" : "") + amount).color(LargeInterfaceGuiTextures.HOVER_TEXT_COLOR))
                .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                    if (mouse.isClient() || !authorized.getAsBoolean() || mouse.mouseButton != 0) return;
                    long result = (long) duality.getPriority() + amount;
                    duality.setPriority((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, result)));
                })));
        }
        panel.child(ButtonWidget.panelCloseButton().pos(152, -5).size(20).background(LargeInterfaceGuiTextures.TAB)
            .hoverBackground(LargeInterfaceGuiTextures.TAB_HOVER)
            .overlay(new ItemDrawable(
                (host instanceof IPart ? GTNGItemList.LargeInterfacePart : GTNGItemList.LargeInterface).get(1)).asIcon()
                    .size(16))
            // #tr gui.large_interface.back
            // # Return to the interface
            // # zh_CN 返回二合一接口
            .tooltip(t -> t.addLine(IKey.lang("gui.large_interface.back"))).excludeAreaInRecipeViewer());
        return panel;
    }

    private void multiplyPatterns(MouseData mouse) {
        CraftingGridCache.pauseRebuilds();
        try {
            for (int i = 0; i < duality.getPatterns().getSizeInventory(); i++) {
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
        ICraftingPatternDetails details = item.getPatternForItem(stack, host.getTileEntity().getWorldObj());
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
            ICONS[index] = UITexture.builder().location(ModList.AE2.getResourceLocation(), "guis/states")
                .imageSize(256, 256).subAreaXYWH(index % 16 * 16, index / 16 * 16, 16, 16).build();
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
            case BLOCK -> text(ButtonToolTips.InterfaceBlockingMode,
                ordinal == YesNo.YES.ordinal() ? ButtonToolTips.Blocking : ButtonToolTips.NonBlocking);
            case SMART_BLOCK -> text(ButtonToolTips.InterfaceSmartBlockingMode,
                ordinal == YesNo.YES.ordinal() ? ButtonToolTips.SmartBlocking : ButtonToolTips.NonSmartBlocking);
            case INTERFACE_TERMINAL -> GuiText.InterfaceTerminal.getLocal() + "\n"
                + GuiText.InterfaceTerminalHint.getLocal();
            case PATTERN_OPTIMIZATION -> GuiText.PatternOptimization.getLocal() + "\n"
                + GuiText.PatternOptimizationHint.getLocal();
            case INSERTION_MODE -> switch (InsertionMode.values()[clamp(ordinal, InsertionMode.values().length)]) {
                    case DEFAULT -> text(ButtonToolTips.InsertionModeDefault, ButtonToolTips.InsertionModeDefaultDesc);
                    case PREFER_EMPTY -> text(ButtonToolTips.InsertionModePreferEmpty,
                        ButtonToolTips.InsertionModePreferEmptyDesc);
                    case ONLY_EMPTY -> text(ButtonToolTips.InsertionModeOnlyEmpty,
                        ButtonToolTips.InsertionModeOnlyEmptyDesc);
                };
            case ADVANCED_BLOCKING_MODE -> ordinal == AdvancedBlockingMode.DEFAULT.ordinal()
                ? text(ButtonToolTips.AdvancedBlockingModeDefault, ButtonToolTips.AdvancedBlockingModeDefaultDesc)
                : text(ButtonToolTips.AdvancedBlockingModeAll, ButtonToolTips.AdvancedBlockingModeAllDesc);
            case LOCK_CRAFTING_MODE -> text(ButtonToolTips.LockCraftingMode,
                switch (LockCraftingMode.values()[clamp(ordinal, LockCraftingMode.values().length)]) {
                case NONE -> ButtonToolTips.LockCraftingModeNone;
                case LOCK_UNTIL_PULSE -> ButtonToolTips.LockCraftingUntilRedstonePulse;
                case LOCK_WHILE_HIGH -> ButtonToolTips.LockCraftingWhileRedstoneHigh;
                case LOCK_WHILE_LOW -> ButtonToolTips.LockCraftingWhileRedstoneLow;
                case LOCK_UNTIL_RESULT -> ButtonToolTips.LockCraftingUntilResultReturned;
                });
            case FUZZY_MODE -> text(ButtonToolTips.FuzzyMode,
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
            return (host.getTileEntity().getWorldObj().isRemote || authorized.getAsBoolean())
                && super.isItemValid(stack);
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return (host.getTileEntity().getWorldObj().isRemote || authorized.getAsBoolean())
                && super.canTakeStack(player);
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
