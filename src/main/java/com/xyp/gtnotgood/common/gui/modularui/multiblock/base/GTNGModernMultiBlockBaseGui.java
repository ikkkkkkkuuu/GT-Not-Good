package com.xyp.gtnotgood.common.gui.modularui.multiblock.base;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.sync.DynamicLinkedSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.FluidDisplayWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;
import com.xyp.gtnotgood.common.gui.modularui.MachineModeSelector;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGModularMachineBase;
import com.xyp.gtnotgood.common.machines.processing.ModuleWorkDisplay;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeDisplay;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

public class GTNGModernMultiBlockBaseGui<T extends MTEMultiBlockBase> extends MTEMultiBlockBaseGui<T> {

    protected static final int CONTROL_BUTTON_SIZE = 16;

    public GTNGModernMultiBlockBaseGui(T multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        if (!(multiblock instanceof GTNGModularMachineBase<?>modular)) return;
        GenericSyncValue<NBTTagCompound, ?> progress = GenericSyncValue.builder(NBTTagCompound.class)
            .getter(modular::getDisplayedModuleProgress).deserializer(PacketBuffer::readNBTTagCompoundFromBuffer)
            .serializer(PacketBuffer::writeNBTTagCompoundToBuffer).equals(Objects::equals)
            .copy(value -> (NBTTagCompound) value.copy()).build();
        syncManager.syncValue("gtngModuleProgress", progress);
        GenericListSyncHandler<NBTTagCompound> tasks = new GenericListSyncHandler<>(
            () -> modular.getDisplayedModuleTasks(128), ignored -> {}, PacketBuffer::readNBTTagCompoundFromBuffer,
            PacketBuffer::writeNBTTagCompoundToBuffer, Objects::equals, value -> (NBTTagCompound) value.copy());
        syncManager.syncValue("gtngModuleTasks", tasks);
        syncManager.syncValue("gtngModuleTaskWidget",
            new DynamicLinkedSyncHandler<>(tasks).widgetProvider((sync, value) -> {
                Flow column = Flow.column().coverChildrenHeight(0).crossAxisAlignment(Alignment.CrossAxis.START);
                for (NBTTagCompound task : value.getValue()) column.child(createModuleTaskGroup(task, progress));
                return column;
            }));
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        ListWidget<IWidget, ?> terminal = super.createTerminalTextWidget(syncManager, parent);
        if (multiblock instanceof GTNGModularMachineBase<?>) {
            terminal.name("gtngModuleTerminal");
            DynamicLinkedSyncHandler<?> tasks = syncManager.findSyncHandler("gtngModuleTaskWidget",
                DynamicLinkedSyncHandler.class);
            terminal.child(new DynamicSyncedWidget<>().syncHandler(tasks).fullWidth().coverChildrenHeight(0));
        }
        return terminal;
    }

    private IWidget createModuleTaskGroup(NBTTagCompound task, GenericSyncValue<NBTTagCompound, ?> progress) {
        String core = task.getString("core");
        int duration = task.getInteger("duration");
        Flow group = Flow.column().name("gtngModuleTask:" + core).fullWidth().coverChildrenHeight(0)
            .crossAxisAlignment(Alignment.CrossAxis.START).marginBottom(4)
            .child(new TextWidget<>(IKey.str(EnumChatFormatting.GOLD + moduleTaskName(task))).height(9).scale(0.8f));
        String[] position = core.split(":");
        if (position.length == 4) group.tooltip(t -> {
            // #tr gtng.modular.gui.position
            // # Position: %s, %s, %s
            // # zh_CN 位置：%s, %s, %s
            t.addLine(StatCollector.translateToLocalFormatted("gtng.modular.gui.position", position[1], position[2],
                position[3]));
        });
        if (duration > 0) {
            group.child(new TextWidget<>(
                IKey.dynamic(() -> ModuleWorkDisplay.progress(progress.getValue().getInteger(core), duration)))
                    .name("gtngModuleProgress:" + core).fullWidth().marginBottom(2));
        } else {
            // #tr gtng.modular.gui.idle
            // # Idle
            // # zh_CN 空闲
            group.child(
                IKey.lang("gtng.modular.gui.idle").color(Color.WHITE.main).asWidget().fullWidth().marginBottom(2));
        }
        if (task.getBoolean("paused")) {
            group.child(IKey.lang("gtng.module.core.paused").asWidget().fullWidth().marginBottom(2));
        } else if (task.getBoolean("finished")) {
            // #tr gtng.modular.gui.waiting_output
            // # Waiting for output space
            // # zh_CN 等待输出空间
            group.child(IKey.lang("gtng.modular.gui.waiting_output").color(Color.YELLOW.main).asWidget().fullWidth()
                .marginBottom(2));
        }
        NBTTagList outputs = task.getTagList("outputs", 10);
        List<NBTTagCompound> rows = new ArrayList<>();
        for (int i = 0; i < outputs.tagCount(); i++) rows.add(outputs.getCompoundTagAt(i));
        rows.sort((first, second) -> {
            int kind = Boolean.compare(first.hasKey("fluid"), second.hasKey("fluid"));
            if (kind != 0) return kind;
            int amount = new BigInteger(second.getString("amount"))
                .compareTo(new BigInteger(first.getString("amount")));
            if (amount != 0) return amount;
            int name = WirelessRecipeDisplay.name(first).compareTo(WirelessRecipeDisplay.name(second));
            return first.hasKey("fluid") ? -name : name;
        });
        for (NBTTagCompound row : rows) group.child(createModuleOutputRow(row));
        return group;
    }

    private String moduleTaskName(NBTTagCompound task) {
        String core = task.getString("core");
        if (core.equals("controller")) {
            // #tr gtng.modular.gui.controller
            // # Controller
            // # zh_CN 主机
            return StatCollector.translateToLocal("gtng.modular.gui.controller");
        }
        if (core.startsWith("wireless:")) {
            // #tr gtng.modular.gui.wireless
            // # Controller wireless task %s
            // # zh_CN 主机无线任务 %s
            return StatCollector.translateToLocalFormatted("gtng.modular.gui.wireless", core.substring(9));
        }
        String type = switch (task.getString("type")) {
            // #tr gtng.modular.gui.advanced
            // # Advanced
            // # zh_CN 高级
            case "Advanced" -> StatCollector.translateToLocal("gtng.modular.gui.advanced");
            // #tr gtng.modular.gui.perfect
            // # Perfect
            // # zh_CN 完美
            case "Perfect" -> StatCollector.translateToLocal("gtng.modular.gui.perfect");
            // #tr gtng.modular.gui.normal
            // # Normal
            // # zh_CN 普通
            default -> StatCollector.translateToLocal("gtng.modular.gui.normal");
        };
        // #tr gtng.modular.gui.core
        // # Core %s (%s)
        // # zh_CN 附加核心 %s（%s）
        return StatCollector.translateToLocalFormatted("gtng.modular.gui.core", task.getInteger("index"), type);
    }

    protected IWidget createModuleOutputRow(NBTTagCompound row) {
        IWidget icon;
        if (row.hasKey("item")) {
            ItemStack item = ItemStack.loadItemStackFromNBT(row.getCompoundTag("item"));
            icon = new ItemDisplayWidget().item(item).displayAmount(false).disableThemeBackground(true)
                .disableHoverThemeBackground(true).widgetTheme(GTWidgetThemes.BACKGROUND_TERMINAL).size(14)
                .marginRight(2);
        } else {
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(row.getCompoundTag("fluid"));
            icon = new FluidDisplayWidget().value(fluid).displayAmount(false).disableThemeBackground(true)
                .disableHoverThemeBackground(true).widgetTheme(GTWidgetThemes.BACKGROUND_TERMINAL).size(14)
                .marginRight(2);
        }
        String name = WirelessRecipeDisplay.name(row);
        return Flow.row().widthRel(0.85f).height(15).child(icon)
            .child(Flow.column().name("gtngRecipeOutputText").coverChildren(0)
                .crossAxisAlignment(Alignment.CrossAxis.START)
                .child(new TextWidget<>(IKey.str(EnumChatFormatting.AQUA + name)).height(8).scale(0.75f)
                    .textAlign(Alignment.CenterLeft))
                .child(new TextWidget<>(IKey.str(RecipeOutputFormatting.amountLine(row))).height(6).scale(0.6f)
                    .textAlign(Alignment.CenterLeft))
                .tooltip(t -> t.addLine(RecipeOutputFormatting.tooltip(row))));
    }

    @Override
    protected void initCustomIcons() {
        super.initCustomIcons();
        this.customIcons.put("power_switch_on", GTNGGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_ON);
        this.customIcons.put("power_switch_off", GTNGGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_OFF);
        this.customIcons.put("power_switch_disabled", GTNGGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_DISABLED);
    }

    /**
     * Applies the mod's standard background and hover textures to a normal button.
     * <p>
     * The enabled supplier is evaluated by {@link DynamicDrawable} when the widget is drawn, so buttons can visually
     * update when machine capabilities or modes change after the panel is built.
     *
     * @param button  button widget returned by the GregTech base GUI
     * @param enabled dynamic predicate controlling enabled versus disabled textures
     * @return the same button instance after applying backgrounds
     */
    protected ButtonWidget<?> applyModernButton(ButtonWidget<?> button, BooleanSupplier enabled) {
        button.background(new DynamicDrawable(
            () -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        button.hoverBackground(new DynamicDrawable(() -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_HOVER
            : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        return button;
    }

    /**
     * Applies the modern normal or pressed background to a stateful button.
     * <p>
     * This is used for ordinary {@link ButtonWidget} controls whose selected state is synchronized separately, such
     * as the GTNC-style wireless battery button. It keeps the selected state visibly pressed without changing the
     * button's interaction or synchronization behavior.
     *
     * @param button   stateful button to skin
     * @param selected dynamic predicate selecting the pressed background
     * @param enabled  dynamic predicate controlling enabled versus disabled textures
     * @return the same button instance after applying state-aware backgrounds
     */
    protected ButtonWidget<?> applyModernStateButton(ButtonWidget<?> button, BooleanSupplier selected,
        BooleanSupplier enabled) {
        button.background(new DynamicDrawable(() -> {
            if (!enabled.getAsBoolean()) return GTNGGuiTextures.MODERN_BUTTON_DISABLED;
            return selected.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_PRESSED : GTNGGuiTextures.MODERN_BUTTON;
        }));
        button.hoverBackground(new DynamicDrawable(() -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_HOVER
            : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        return button;
    }

    /**
     * Applies the mod's standard textures to a two-state toggle button.
     * <p>
     * False and true states are skinned separately so the pressed state remains visible while still falling back to the
     * disabled texture when the underlying feature is unsupported by the multiblock.
     *
     * @param button  toggle button widget returned by the GregTech base GUI
     * @param enabled dynamic predicate controlling enabled versus disabled textures
     * @return the same toggle button instance after applying backgrounds
     */
    protected ToggleButton applyModernToggleButton(ToggleButton button, BooleanSupplier enabled) {
        button.background(false, new DynamicDrawable(
            () -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        button.background(true, new DynamicDrawable(() -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_PRESSED
            : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        button.hoverBackground(false,
            new DynamicDrawable(() -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_HOVER
                : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        button.hoverBackground(true,
            new DynamicDrawable(() -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_HOVER
                : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        return button;
    }

    @Override
    protected ModularPanel getBasePanel(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return super.getBasePanel(guiData, syncManager, uiSettings).background(GTNGGuiTextures.MODERN_BACKGROUND);
    }

    /**
     * Builds the framed terminal panel used at the top of the multiblock GUI.
     * <p>
     * The default GregTech parent widget does not expose the GT-Not-Cool dark outer frame. This method recreates that
     * parent manually: the outer widget draws {@link GTNGGuiTextures#MODERN_VAULT_PANEL_BORDER}, then the actual
     * terminal text widget is inset so the border remains visible.
     *
     * @param panel       current ModularUI panel
     * @param syncManager panel sync manager supplied by ModularUI
     * @return terminal parent with modern dark frame and corner status columns
     */
    @Override
    protected ParentWidget<?> createTerminalParentWidget(ModularPanel panel, PanelSyncManager syncManager) {
        return new ParentWidget<>().size(getTerminalWidgetWidth(), getTerminalWidgetHeight()).paddingTop(4)
            .paddingBottom(4).paddingLeft(4).paddingRight(0).background(GTNGGuiTextures.MODERN_VAULT_PANEL_BORDER)
            .child(createTerminalTextWidget(syncManager, panel)
                .size(getTerminalWidgetWidth() - 4, getTerminalWidgetHeight() - 8).collapseDisabledChild())
            .childIf(multiblock.supportsTerminalRightCornerColumn(),
                () -> createTerminalRightCornerColumn(panel, syncManager))
            .childIf(multiblock.supportsTerminalLeftCornerColumn(),
                () -> createTerminalLeftCornerColumn(panel, syncManager));
    }

    /**
     * Rebuilds the lower inventory/button row with modern slot backgrounds.
     * <p>
     * The player inventory is only attached when the multiblock declares that it binds player inventory, matching the
     * upstream GUI behavior while letting this project control the slot texture.
     *
     * @param panel       current ModularUI panel
     * @param syncManager panel sync manager supplied by ModularUI
     * @return row widget containing the player inventory and multiblock button column
     */
    @Override
    protected IWidget createInventoryRow(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row().fullWidth().height(76).childIf(multiblock.doesBindPlayerInventory(), () -> SlotGroupWidget
            .playerInventory((index, slot) -> slot.background(GTNGGuiTextures.MODERN_VAULT_ITEM_SLOT)).marginLeft(4))
            .child(createButtonColumn(panel, syncManager));
    }

    /**
     * Skins the structure update toggle and swaps its overlay based on the structure refresh timer.
     *
     * @param syncManager panel sync manager supplied by ModularUI
     * @return skinned structure update toggle
     */
    @Override
    protected IWidget createStructureUpdateButton(PanelSyncManager syncManager) {
        ToggleButton button = ((ToggleButton) super.createStructureUpdateButton(syncManager)).size(CONTROL_BUTTON_SIZE)
            .overlay(new DynamicDrawable(() -> {
                if (multiblock.getStructureUpdateTime() > -20) {
                    return GTNGGuiTextures.OVERLAY_BUTTON_STRUCTURE_CHECK;
                }
                return GTNGGuiTextures.OVERLAY_BUTTON_STRUCTURE_CHECK_OFF;
            })).tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernToggleButton(button, () -> true);
    }

    /**
     * Skins the power switch and chooses an overlay that reflects whether work is currently allowed.
     *
     * @return skinned power switch toggle
     */
    @Override
    protected ToggleButton createPowerSwitchButton() {
        ToggleButton button = super.createPowerSwitchButton().size(CONTROL_BUTTON_SIZE)
            .overlay(new DynamicDrawable(() -> {
                if (multiblock.isAllowedToWork()) {
                    return GTNGGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_ON;
                }
                return GTNGGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_DISABLED;
            })).tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernToggleButton(button, () -> !isPowerSwitchDisabled());
    }

    @Override
    protected ToggleButton createMuffleButton() {
        return applyModernToggleButton(super.createMuffleButton(), () -> true);
    }

    /**
     * Skins the void-excess button while preserving GregTech's support check.
     * <p>
     * Machines that do not support void protection keep the disabled texture through
     * {@link MTEMultiBlockBase#supportsVoidProtection()}.
     *
     * @param syncManager panel sync manager supplied by ModularUI
     * @return skinned void-excess button
     */
    @Override
    protected ButtonWidget<?> createVoidExcessButton(PanelSyncManager syncManager) {
        ButtonWidget<?> button = ((ButtonWidget<?>) super.createVoidExcessButton(syncManager)).size(CONTROL_BUTTON_SIZE)
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernButton(button, multiblock::supportsVoidProtection);
    }

    /**
     * Skins the input separation toggle and uses distinct overlays for enabled and disabled machine mode.
     *
     * @param syncManager panel sync manager supplied by ModularUI
     * @return skinned input separation toggle
     */
    @Override
    protected ToggleButton createInputSeparationButton(PanelSyncManager syncManager) {
        ToggleButton button = super.createInputSeparationButton(syncManager).size(CONTROL_BUTTON_SIZE)
            .overlay(false, GTNGGuiTextures.OVERLAY_BUTTON_INPUT_SEPARATION_OFF)
            .overlay(true, GTNGGuiTextures.OVERLAY_BUTTON_INPUT_SEPARATION).tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernToggleButton(button, multiblock::supportsInputSeparation);
    }

    /**
     * Skins the batch mode toggle and keeps its enabled state tied to the multiblock capability.
     *
     * @param syncManager panel sync manager supplied by ModularUI
     * @return skinned batch mode toggle
     */
    @Override
    protected ToggleButton createBatchModeButton(PanelSyncManager syncManager) {
        ToggleButton button = super.createBatchModeButton(syncManager).size(CONTROL_BUTTON_SIZE)
            .overlay(false, GTNGGuiTextures.OVERLAY_BUTTON_BATCH_MODE_OFF)
            .overlay(true, GTNGGuiTextures.OVERLAY_BUTTON_BATCH_MODE).tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernToggleButton(button, multiblock::supportsBatchMode);
    }

    /**
     * Skins the recipe-lock toggle and swaps overlays for locked versus unlocked states.
     *
     * @param syncManager panel sync manager supplied by ModularUI
     * @return skinned single-recipe-lock toggle
     */
    @Override
    protected ToggleButton createLockToSingleRecipeButton(PanelSyncManager syncManager) {
        ToggleButton button = super.createLockToSingleRecipeButton(syncManager).size(CONTROL_BUTTON_SIZE)
            .overlay(false, GTNGGuiTextures.OVERLAY_BUTTON_RECIPE_UNLOCKED)
            .overlay(true, GTNGGuiTextures.OVERLAY_BUTTON_RECIPE_LOCKED).tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernToggleButton(button, multiblock::supportsSingleRecipeLocking);
    }

    @Override
    protected IWidget createModeSwitchButton(PanelSyncManager syncManager) {
        List<IKey> names = new ArrayList<>();
        for (var map : multiblock.getAvailableRecipeMaps()) names.add(IKey.lang(map.unlocalizedName));
        return MachineModeSelector
            .createButton(syncManager, getMachineModeSelectorPrefix(), names,
                syncManager.findSyncHandler("machineMode", IntSyncValue.class))
            .size(CONTROL_BUTTON_SIZE).tooltipBuilder(this::createModeSwitchTooltip);
    }

    protected String getMachineModeSelectorPrefix() {
        return "gtng";
    }

    @Override
    protected ButtonWidget<?> createPowerPanelButton(PanelSyncManager syncManager, ModularPanel parent) {
        return applyModernButton(super.createPowerPanelButton(syncManager, parent).size(CONTROL_BUTTON_SIZE),
            () -> true);
    }

    /**
     * Repositions the terminal corner status column inside the dark framed terminal panel.
     * <p>
     * This mirrors the GT-Not-Cool modern base GUI: shutdown and maintenance indicators stay in the terminal's lower
     * right corner, and the mod logo is drawn as the last child in that column.
     *
     * @param panel       current ModularUI panel
     * @param syncManager panel sync manager supplied by ModularUI
     * @return right-corner terminal column with status hoverables and logo
     */
    @Override
    protected Flow createTerminalRightCornerColumn(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.column().coverChildren().rightRel(0, 6, 0).bottomRel(0, 6, 0)
            .childIf(multiblock.supportsShutdownReasonHoverable(),
                () -> createShutdownReasonHoverableTerminal(syncManager))
            .childIf(multiblock.supportsMaintenanceIssueHoverable(),
                () -> createMaintIssueHoverableTerminal(syncManager))
            .child(makeLogoWidget(syncManager, panel));
    }

    @Override
    protected Widget<? extends Widget<?>> makeLogoWidget(PanelSyncManager syncManager, ModularPanel parent) {
        return new IDrawable.DrawableWidget(GTNGGuiTextures.PICTURE_GODFORGE_LOGO).size(18).marginTop(4);
    }
}
