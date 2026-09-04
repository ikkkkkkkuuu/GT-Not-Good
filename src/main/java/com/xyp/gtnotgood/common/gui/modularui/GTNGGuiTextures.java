package com.xyp.gtnotgood.common.gui.modularui;

import static com.xyp.gtnotgood.GTNotGood.RESOURCE_ROOT_ID;

import com.cleanroommc.modularui.drawable.UITexture;
import com.xyp.gtnotgood.utils.enums.ModList;

/**
 * Central registry for ModularUI texture handles used by GT Not Good GUIs.
 * <p>
 * Keep texture handles here instead of constructing {@link UITexture} objects inside GUI classes. That makes the modern
 * theme reusable across multiblock bases, pop-up panels, hatches, and any future custom screens.
 */
public final class GTNGGuiTextures {

    private static final String MODID = ModList.ModIds.GT_NOT_GOOD;
    private static final String BASE = RESOURCE_ROOT_ID + ":iconsets/";

    public static final UITexture MODERN_BUTTON = UITexture.builder()
        .location(MODID, "gui/modernity/button")
        .imageSize(20, 20)
        .adaptable(3)
        .build();
    public static final UITexture MODERN_BUTTON_HOVER = UITexture.builder()
        .location(MODID, "gui/modernity/button_hover")
        .imageSize(20, 20)
        .adaptable(3)
        .build();
    public static final UITexture MODERN_BUTTON_PRESSED = UITexture.builder()
        .location(MODID, "gui/modernity/button_pressed")
        .imageSize(20, 20)
        .adaptable(3)
        .build();
    public static final UITexture MODERN_BUTTON_DISABLED = UITexture.builder()
        .location(MODID, "gui/modernity/button_disabled")
        .imageSize(20, 20)
        .adaptable(3)
        .build();
    public static final UITexture MODERN_BUTTON_COMPACT = UITexture.builder()
        .location(MODID, "gui/modernity/button_compact")
        .imageSize(18, 18)
        .adaptable(2)
        .build();
    public static final UITexture MODERN_BACKGROUND = UITexture.builder()
        .location(MODID, "gui/modernity/background")
        .imageSize(176, 166)
        .adaptable(2)
        .build();
    /**
     * Dark 8x8 adaptable border for compact panels.
     * <p>
     * This matches the GT-Not-Cool modern theme and should be used when a small framed area needs the same visual
     * language as the main machine GUI.
     */
    public static final UITexture MODERN_PANEL_BORDER = UITexture.builder()
        .location(MODID, "gui/modernity/panel_border")
        .imageSize(8, 8)
        .adaptable(1)
        .build();
    public static final UITexture MODERN_DISPLAY = UITexture.builder()
        .location(MODID, "gui/modernity/display")
        .imageSize(143, 75)
        .adaptable(1)
        .build();
    /**
     * Dark 16x16 adaptable frame used around terminal-style information panels.
     * <p>
     * This is the missing "deep dark outer frame" from the GT-Not-Cool multiblock GUI. The terminal text widget is
     * nested inside this texture with padding so the frame remains visible.
     */
    public static final UITexture MODERN_VAULT_PANEL_BORDER = UITexture.builder()
        .location(MODID, "gui/modernity/vault_panel_border")
        .imageSize(16, 16)
        .adaptable(6)
        .build();
    public static final UITexture MODERN_VAULT_ITEM_SLOT = UITexture.builder()
        .location(MODID, "gui/modernity/vault_item_slot")
        .imageSize(18, 18)
        .adaptable(1)
        .build();
    public static final UITexture MODERN_VAULT_BACKGROUND = UITexture.builder()
        .location(MODID, "gui/modernity/vault_background")
        .imageSize(16, 16)
        .adaptable(2, 2, 2, 4)
        .build();

    public static final UITexture OVERLAY_BUTTON_POWER_SWITCH_ON = UITexture
        .fullImage(MODID, "gui/overlay_button/power_switch_on");
    public static final UITexture OVERLAY_BUTTON_POWER_SWITCH_OFF = UITexture
        .fullImage(MODID, "gui/overlay_button/power_switch_off");
    public static final UITexture OVERLAY_BUTTON_POWER_SWITCH_DISABLED = UITexture
        .fullImage(MODID, "gui/overlay_button/power_switch_disabled");
    public static final UITexture OVERLAY_BUTTON_STRUCTURE_CHECK = UITexture
        .fullImage(MODID, "gui/overlay_button/structure_check_on");
    public static final UITexture OVERLAY_BUTTON_STRUCTURE_CHECK_OFF = UITexture
        .fullImage(MODID, "gui/overlay_button/structure_check_off");
    public static final UITexture OVERLAY_BUTTON_INPUT_SEPARATION = UITexture
        .fullImage(MODID, "gui/overlay_button/input_separation_on");
    public static final UITexture OVERLAY_BUTTON_INPUT_SEPARATION_OFF = UITexture
        .fullImage(MODID, "gui/overlay_button/input_separation_off");
    public static final UITexture OVERLAY_BUTTON_BATCH_MODE = UITexture
        .fullImage(MODID, "gui/overlay_button/batch_mode_on");
    public static final UITexture OVERLAY_BUTTON_BATCH_MODE_OFF = UITexture
        .fullImage(MODID, "gui/overlay_button/batch_mode_off");
    public static final UITexture OVERLAY_BUTTON_RECIPE_LOCKED = UITexture
        .fullImage(MODID, "gui/overlay_button/recipe_locked");
    public static final UITexture OVERLAY_BUTTON_RECIPE_UNLOCKED = UITexture
        .fullImage(MODID, "gui/overlay_button/recipe_unlocked");
    public static final UITexture OVERLAY_BUTTON_POWER_PANEL = UITexture
        .fullImage(MODID, "gui/overlay_button/power_panel");
    public static final UITexture OVERLAY_BUTTON_BATTERY_ON = UITexture
        .fullImage(MODID, "gui/overlay_button/battery_on");
    public static final UITexture OVERLAY_BUTTON_BATTERY_OFF = UITexture
        .fullImage(MODID, "gui/overlay_button/battery_off");

    public static final UITexture PICTURE_GODFORGE_LOGO = UITexture.fullImage(MODID, "gui/picture/gorge_logo");

    private GTNGGuiTextures() {}
}
