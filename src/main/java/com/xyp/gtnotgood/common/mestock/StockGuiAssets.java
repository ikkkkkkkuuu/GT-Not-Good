// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.mestock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.xyp.gtnotgood.utils.enums.ModList;

/** Original upstream GUI atlas crops; source, license and byte hashes are recorded in the packaged asset manifest. */
final class StockGuiAssets {

    private static final Map<String, UITexture> atlasCrops = new ConcurrentHashMap<>();
    private static final UITexture normalButton = buttonTexture("button");
    private static final UITexture highlightedButton = buttonTexture("button_highlighted");
    private static final UITexture normalTextField = fieldTexture(0);
    private static final UITexture focusedTextField = fieldTexture(24);
    private static final Rectangle upgradeBorder = new Rectangle().color(0xfff2f2f2);

    static final UITexture bus = texture("storagebus", 256, 256, 0, 0, 176, 253);
    static final UITexture emitter = texture("threshold_level_emitter", 256, 256, 0, 0, 176, 186);
    static final UITexture amount = texture("craft_amt", 256, 256, 0, 0, 176, 107);
    static final UITexture toolbar = texture("states", 256, 256, 176, 128, 18, 20);
    static final UITexture toolbarHover = texture("states", 256, 256, 212, 128, 18, 20);
    static final UITexture slot = texture("storagebus", 256, 256, 7, 28, 18, 18);

    private StockGuiAssets() {}

    static UITexture texture(String name, int imageWidth, int imageHeight, int x, int y, int width, int height) {
        String key = name + ':' + x + ':' + y + ':' + width + ':' + height;
        return atlasCrops.computeIfAbsent(
            key,
            ignored -> UITexture.builder()
                .location(ModList.ModIds.GT_NOT_GOOD, "gui/me_stock/" + name)
                .imageSize(imageWidth, imageHeight)
                .subAreaXYWH(x, y, width, height)
                .build());
    }

    static UITexture icon(int x, int y) {
        return texture("states", 256, 256, x, y, 16, 16);
    }

    static UITexture thresholdIcon(boolean above) {
        return texture("nicons", 64, 64, above ? 0 : 16, 32, 16, 16);
    }

    static UITexture checkbox(boolean checked, boolean hovered) {
        return texture("state_box", 28, 28, hovered ? 14 : 0, checked ? 14 : 0, 14, 14);
    }

    static UITexture submit(boolean hovered) {
        return texture("submit_button", 24, 12, hovered ? 12 : 0, 0, 12, 12);
    }

    static UITexture button(boolean hovered) {
        return hovered ? highlightedButton : normalButton;
    }

    private static UITexture buttonTexture(String name) {
        return UITexture.builder()
            .location(ModList.ModIds.GT_NOT_GOOD, "gui/me_stock/" + name)
            .imageSize(200, 20)
            .adaptable(3)
            .build();
    }

    static UITexture textField(boolean focused) {
        return focused ? focusedTextField : normalTextField;
    }

    private static UITexture fieldTexture(int y) {
        return UITexture.builder()
            .location(ModList.ModIds.GT_NOT_GOOD, "gui/me_stock/text_field")
            .imageSize(128, 128)
            .subAreaXYWH(0, y, 128, 12)
            .adaptable(1, 0, 1, 0)
            .build();
    }

    static IDrawable requester(boolean terminal, IntSupplier rowCount, IntPredicate requestLine) {
        String name = terminal ? "requester_terminal" : "requester";
        return (context, x, y, width, height, theme) -> {
            int rows = rowCount.getAsInt();
            texture(name, 256, 256, 0, 0, 195, 20).draw(context, x, y, 195, 20, theme);
            for (int row = 0; row < rows; row++) texture(name, 256, 256, 0, requestLine.test(row) ? 38 : 60, 195, 19)
                .draw(context, x, y + 20 + 19 * row, 195, 19, theme);
            texture(name, 256, 256, 0, terminal ? 133 : 114, 195, 101)
                .draw(context, x, y + 20 + 19 * rows, 195, 101, theme);
        };
    }

    static IDrawable upgrades(int slots) {
        return (context, x, y, width, height, theme) -> {
            for (int i = 0; i < slots; i++) {
                int top = i == 0 ? 5 : 0, bottom = i == slots - 1 ? 7 : 0;
                texture("extra_panels", 128, 128, 0, i == 0 ? 0 : 5, 28, 18 + top + bottom)
                    .draw(context, x, y + i * 18 + 5 - top, 28, 18 + top + bottom, theme);
            }
            upgradeBorder.draw(context, x + 1, y + 5, 16, 1, theme);
            upgradeBorder.draw(context, x + 1, y + 5 + slots * 18 - 1, 16, 1, theme);
            upgradeBorder.draw(context, x, y + 4, 1, slots * 18 + 2, theme);
            upgradeBorder.draw(context, x + 17, y + 4, 1, slots * 18 + 2, theme);
        };
    }
}
