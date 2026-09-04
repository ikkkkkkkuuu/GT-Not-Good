package com.xyp.gtnotgood.common.gui.modularui.multiblock.base;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.function.BooleanSupplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * Applies GT Not Good's modern GUI textures to the standard GregTech multiblock panel.
 * <p>
 * The class keeps GregTech's existing multiblock GUI behavior and only wraps the visual surfaces: background panels,
 * button textures, overlays, slot backgrounds, and the logo. State changes still flow through the upstream
 * {@link MTEMultiBlockBaseGui} implementation, preserving its server/client synchronization rules.
 *
 * @param <T> concrete GregTech multiblock type displayed by this GUI
 */
public class GTNGModernMultiBlockBaseGui<T extends MTEMultiBlockBase> extends MTEMultiBlockBaseGui<T> {

    public GTNGModernMultiBlockBaseGui(T multiblock) {
        super(multiblock);
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
        button.background(
            new DynamicDrawable(
                () -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        button.hoverBackground(
            new DynamicDrawable(
                () -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_HOVER
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
        button.hoverBackground(
            new DynamicDrawable(
                () -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_HOVER
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
        button.background(
            false,
            new DynamicDrawable(
                () -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        button.background(
            true,
            new DynamicDrawable(
                () -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_PRESSED
                    : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        button.hoverBackground(
            false,
            new DynamicDrawable(
                () -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_HOVER
                    : GTNGGuiTextures.MODERN_BUTTON_DISABLED));
        button.hoverBackground(
            true,
            new DynamicDrawable(
                () -> enabled.getAsBoolean() ? GTNGGuiTextures.MODERN_BUTTON_HOVER
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
        return new ParentWidget<>().size(getTerminalWidgetWidth(), getTerminalWidgetHeight())
            .paddingTop(4)
            .paddingBottom(4)
            .paddingLeft(4)
            .paddingRight(0)
            .background(GTNGGuiTextures.MODERN_VAULT_PANEL_BORDER)
            .child(
                createTerminalTextWidget(syncManager, panel)
                    .size(getTerminalWidgetWidth() - 4, getTerminalWidgetHeight() - 8)
                    .collapseDisabledChild())
            .childIf(
                multiblock.supportsTerminalRightCornerColumn(),
                () -> createTerminalRightCornerColumn(panel, syncManager))
            .childIf(
                multiblock.supportsTerminalLeftCornerColumn(),
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
        return Flow.row()
            .fullWidth()
            .height(76)
            .childIf(
                multiblock.doesBindPlayerInventory(),
                () -> SlotGroupWidget
                    .playerInventory((index, slot) -> slot.background(GTNGGuiTextures.MODERN_VAULT_ITEM_SLOT))
                    .marginLeft(4))
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
        ToggleButton button = ((ToggleButton) super.createStructureUpdateButton(syncManager)).size(16)
            .overlay(new DynamicDrawable(() -> {
                if (multiblock.getStructureUpdateTime() > -20) {
                    return GTNGGuiTextures.OVERLAY_BUTTON_STRUCTURE_CHECK;
                }
                return GTNGGuiTextures.OVERLAY_BUTTON_STRUCTURE_CHECK_OFF;
            }))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernToggleButton(button, () -> true);
    }

    /**
     * Skins the power switch and chooses an overlay that reflects whether work is currently allowed.
     *
     * @return skinned power switch toggle
     */
    @Override
    protected ToggleButton createPowerSwitchButton() {
        ToggleButton button = super.createPowerSwitchButton().size(16)
            .overlay(new DynamicDrawable(() -> {
                if (multiblock.isAllowedToWork()) {
                    return GTNGGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_ON;
                }
                return GTNGGuiTextures.OVERLAY_BUTTON_POWER_SWITCH_DISABLED;
            }))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
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
        ButtonWidget<?> button = ((ButtonWidget<?>) super.createVoidExcessButton(syncManager)).size(16)
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
        ToggleButton button = ((ToggleButton) super.createInputSeparationButton(syncManager)).size(16)
            .overlay(new DynamicDrawable(() -> {
                if (multiblock.isInputSeparationEnabled()) {
                    return GTNGGuiTextures.OVERLAY_BUTTON_INPUT_SEPARATION;
                }
                return GTNGGuiTextures.OVERLAY_BUTTON_INPUT_SEPARATION_OFF;
            }))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
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
        ToggleButton button = ((ToggleButton) super.createBatchModeButton(syncManager)).size(16)
            .overlay(new DynamicDrawable(() -> {
                if (multiblock.isBatchModeEnabled()) {
                    return GTNGGuiTextures.OVERLAY_BUTTON_BATCH_MODE;
                }
                return GTNGGuiTextures.OVERLAY_BUTTON_BATCH_MODE_OFF;
            }))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
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
        ToggleButton button = ((ToggleButton) super.createLockToSingleRecipeButton(syncManager)).size(16)
            .overlay(new DynamicDrawable(() -> {
                if (multiblock.isRecipeLockingEnabled()) {
                    return GTNGGuiTextures.OVERLAY_BUTTON_RECIPE_LOCKED;
                }
                return GTNGGuiTextures.OVERLAY_BUTTON_RECIPE_UNLOCKED;
            }))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernToggleButton(button, multiblock::supportsSingleRecipeLocking);
    }

    /**
     * Applies modern backgrounds to every state of GregTech's mode switch cycle button.
     * <p>
     * Some multiblocks expose the mode switch as a {@link CycleButtonWidget}; the guard keeps this override compatible
     * if the base GUI returns another widget type.
     *
     * @param syncManager panel sync manager supplied by ModularUI
     * @return mode switch widget with modern state backgrounds when applicable
     */
    @Override
    protected IWidget createModeSwitchButton(PanelSyncManager syncManager) {
        IWidget button = super.createModeSwitchButton(syncManager);
        if (button instanceof CycleButtonWidget cycleButton) {
            for (int i = 0; i < 8; i++) {
                cycleButton.stateBackground(i, GTNGGuiTextures.MODERN_BUTTON);
                cycleButton.stateHoverBackground(i, GTNGGuiTextures.MODERN_BUTTON_HOVER);
            }
        }
        return button;
    }

    @Override
    protected ButtonWidget<?> createPowerPanelButton(PanelSyncManager syncManager, ModularPanel parent) {
        return applyModernButton(super.createPowerPanelButton(syncManager, parent), () -> true);
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
        return Flow.column()
            .coverChildren()
            .rightRel(0, 6, 0)
            .bottomRel(0, 6, 0)
            .childIf(
                multiblock.supportsShutdownReasonHoverable(),
                () -> createShutdownReasonHoverableTerminal(syncManager))
            .childIf(
                multiblock.supportsMaintenanceIssueHoverable(),
                () -> createMaintIssueHoverableTerminal(syncManager))
            .child(makeLogoWidget(syncManager, panel));
    }

    @Override
    protected Widget<? extends Widget<?>> makeLogoWidget(PanelSyncManager syncManager, ModularPanel parent) {
        return new IDrawable.DrawableWidget(GTNGGuiTextures.PICTURE_GODFORGE_LOGO).size(18)
            .marginTop(4);
    }
}
