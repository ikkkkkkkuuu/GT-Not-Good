package com.xyp.gtnotgood.common.blocks.largeinterface;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.drawable.UITexture;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.modularui2.GTGuiTextures;

/**
 * Official AE2 surfaces reused from the packaged-provider and ME-stock asset sets. Their sources and CC BY-NC-SA
 * 3.0 license are recorded in the corresponding packaged manifests; these drawables contain no GUI state.
 */
public final class LargeInterfaceGuiTextures {

    /** The priority page has no painted slots, so its two-pixel border can safely fit the main panel. */
    public static final UITexture BACKGROUND = texture("priority", 256, 256, 0, 0, 176, 125).adaptable(2)
        .build()
        .withColorOverride(0xffffffff);
    public static final UITexture SLOT = crop("states", 256, 256, 192, 192, 18, 18);
    public static final IDrawable PATTERN_SLOT = new DrawableStack(SLOT, GTGuiTextures.OVERLAY_SLOT_PATTERN_ME);
    public static final UITexture PRIORITY_BACKGROUND = crop("priority", 256, 256, 0, 0, 176, 125);
    public static final UITexture SIDE_BUTTON = crop("states", 256, 256, 176, 128, 18, 20);
    public static final UITexture SIDE_BUTTON_HOVER = crop("states", 256, 256, 212, 128, 18, 20);
    public static final UITexture TAB = crop("states", 256, 256, 160, 192, 20, 20);
    public static final UITexture TAB_HOVER = crop("states", 256, 256, 160, 224, 22, 22);
    public static final UITexture BUTTON = texture("button", 200, 20, 0, 0, 200, 20).adaptable(3)
        .build()
        .withColorOverride(0xffffffff);
    public static final UITexture BUTTON_HOVER = texture("button_highlighted", 200, 20, 0, 0, 200, 20).adaptable(3)
        .build()
        .withColorOverride(0xffffffff);
    public static final UITexture SCROLL_HANDLE = texture("big_scroller", 12, 15, 0, 0, 12, 15).adaptable(1, 2, 1, 2)
        .build()
        .withColorOverride(0xffffffff);
    public static final UITexture TEXT_FIELD = textField(0);
    public static final UITexture TEXT_FIELD_FOCUSED = textField(24);

    private static final UITexture upgradeBody = crop("extra_panels", 128, 128, 18, 5, 1, 1);
    private static final UITexture upgradeOutline = crop("extra_panels", 128, 128, 1, 0, 1, 1);
    private static final UITexture upgradeHighlight = crop("extra_panels", 128, 128, 1, 1, 1, 1);
    private static final UITexture upgradeShadow = crop("extra_panels", 128, 128, 1, 28, 1, 1);

    /**
     * Keeps the native open left edge and four upgrade backgrounds. MUI2 draws sibling slot backgrounds before
     * this drawable widget, so the frame must repaint them before the real slots draw their items and hover.
     */
    public static final IDrawable UPGRADES_FRAME = (context, x, y, width, height, theme) -> {
        if (width < 3 || height < 4) return;
        upgradeBody.draw(context, x, y + 2, width - 2, height - 4, theme);
        upgradeOutline.draw(context, x + 1, y, width - 1, 1, theme);
        upgradeHighlight.draw(context, x + 1, y + 1, width - 2, 1, theme);
        upgradeOutline.draw(context, x + width - 1, y + 1, 1, height - 2, theme);
        upgradeHighlight.draw(context, x + width - 2, y + 2, 1, height - 4, theme);
        upgradeShadow.draw(context, x + 1, y + height - 2, width - 2, 1, theme);
        upgradeOutline.draw(context, x + 1, y + height - 1, width - 1, 1, theme);
        for (int slot = 0; slot < 4; slot++) SLOT.draw(context, x + 4, y + 5 + slot * 18, 18, 18, theme);
    };

    private LargeInterfaceGuiTextures() {}

    private static UITexture crop(String name, int imageWidth, int imageHeight, int x, int y, int width, int height) {
        return texture(name, imageWidth, imageHeight, x, y, width, height).build()
            .withColorOverride(0xffffffff);
    }

    private static UITexture textField(int y) {
        return UITexture.builder()
            .location(ModList.ModIds.GT_NOT_GOOD, "gui/me_stock/text_field")
            .imageSize(128, 128)
            .subAreaXYWH(0, y, 128, 12)
            .adaptable(2)
            .build()
            .withColorOverride(0xffffffff);
    }

    private static UITexture.Builder texture(String name, int imageWidth, int imageHeight, int x, int y, int width,
        int height) {
        return UITexture.builder()
            .location(ModList.ModIds.GT_NOT_GOOD, "gui/packaged/" + name)
            .imageSize(imageWidth, imageHeight)
            .subAreaXYWH(x, y, width, height);
    }
}
