package com.xyp.gtnotgood.client.gui;

import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.BROWSER_TITLE;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.CATEGORY_ALL;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.CATEGORY_ASC;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.CATEGORY_DESC;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.CATEGORY_FILTERED;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.CATEGORY_UNFILTERED;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.DIRECTIONAL_OFF;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.DIRECTIONAL_ON;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.ENERGY_COST;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.FORTUNE_LEVEL;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.MODE_HINT_DIRECTIONAL;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.MODE_HINT_FILTERED;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.ORE_MODE_CRUDE;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.ORE_MODE_CRUSHED;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.ORE_MODE_RAW;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.REFRESH;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.SLOTS_TITLE;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidMinerGuiText.UU_COST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.common.api.gui.OreEntryInfo;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeVoidMinerConfigGui;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidMiner;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.gui.texture.ColorRectTexture;
import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.texture.ItemStackTexture;
import com.xyp.ldlib.gui.texture.SpriteTexture;
import com.xyp.ldlib.gui.texture.TextTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.elements.Button;
import com.xyp.ldlib.gui.ui.elements.Dialog;
import com.xyp.ldlib.gui.ui.elements.Label;
import com.xyp.ldlib.gui.ui.elements.VirtualScrollerView;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Single-page LDLib configuration terminal retaining the original miner's information hierarchy. */
@SideOnly(Side.CLIENT)
public final class LDLibVoidMinerView extends UIElement {

    private final LargeVoidMinerConfigGui model;
    private final ModernTheme theme = new ModernTheme(
        path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));
    private VirtualScrollerView<OreEntryInfo> rows;
    private Button fortune;
    private List<OreEntryInfo> snapshot = Collections.emptyList();
    private String search = "", detail = "";
    private int category;
    private boolean lastDirectional;

    public LDLibVoidMinerView(LargeVoidMinerConfigGui model) {
        super(0, 0, 475, 280);
        this.model = model;
        addChild(new Label(8, 5, 459, 16, tr("NameLargeVoidMiner")));
        addChild(coloredButton(12, 26, 112, 20, this::oreMode, model.actions::sendCycleOreMode));
        fortune = coloredButton(
            130,
            26,
            105,
            20,
            this::fortuneLabel,
            model.actions::sendCycleFortune,
            () -> 0xFFFFFF33);
        addChild(fortune);
        addChild(
            new Label(
                241,
                26,
                224,
                20,
                () -> tr(ENERGY_COST) + " "
                    + number(model.energy.getDoubleValue())
                    + " EU/t  x"
                    + decimal(model.energyMult.getDoubleValue())));
        buildDimensions();
        buildBrowser();
        // #tr gui.gtnotgood.ldminer.hover
        // # Hover an ore for its full name and dimensions. Click its status to toggle it.
        // # zh_CN 悬停矿石查看完整名称与维度，点击状态切换过滤或定向。
        addChild(new Label(10, 264, 455, 16, () -> detail.isEmpty() ? tr("gui.gtnotgood.ldminer.hover") : detail));
    }

    private void buildDimensions() {
        addChild(new Label(12, 52, 90, 16, tr(SLOTS_TITLE)));
        addChild(coloredButton(109, 52, 57, 17, () -> tr(REFRESH), model.actions::sendRefreshPool));
        // The 5x5 real inventory at (12,72) is rendered by MUI2, beneath this transparent native root.
        // #tr gui.gtnotgood.ldminer.dimcost
        // # Dimension cost
        // # zh_CN 维度增幅
        addChild(new Label(106, 80, 62, 16, tr("gui.gtnotgood.ldminer.dimcost")));
        addChild(new Label(106, 98, 62, 16, () -> "+" + number(model.dimIncrease.getDoubleValue()) + "%"));
        // #tr gui.gtnotgood.ldminer.filtercost
        // # Filter cost
        // # zh_CN 过滤增幅
        addChild(new Label(106, 120, 62, 16, tr("gui.gtnotgood.ldminer.filtercost")));
        addChild(new Label(106, 138, 62, 16, () -> "+" + number(model.weightIncrease.getDoubleValue()) + "%"));
        addChild(
            new Label(
                10,
                164,
                158,
                16,
                () -> tr("gui.gtnotgood.largeVoidMiner.dimension_line") + model.dimension.getValue()));
        addChild(
            coloredButton(
                12,
                184,
                94,
                20,
                () -> tr((model.directional.getValue() ? DIRECTIONAL_ON : DIRECTIONAL_OFF)),
                model.actions::sendToggleDirectional,
                () -> model.directional.getValue() ? 0xFF9933FF : 0xFF337777));
        // #tr gui.gtnotgood.ldminer.power
        // # Power
        // # zh_CN 开关
        addChild(
            coloredButton(
                111,
                184,
                55,
                20,
                () -> tr("gui.gtnotgood.ldminer.power") + ": "
                    + tr(model.enabled.getValue() ? "options.on" : "options.off"),
                () -> model.enabled.setValue(!model.enabled.getValue()),
                () -> model.enabled.getValue() ? 0xFF337777 : 0xFF4852FF));
        addChild(
            new WrappedText(
                12,
                212,
                154,
                48,
                () -> tr((model.directional.getValue() ? MODE_HINT_DIRECTIONAL : MODE_HINT_FILTERED))
                    + (model.directional.getValue()
                        ? "\n" + tr(UU_COST) + " " + decimal(model.uu.getDoubleValue()) + " L/s"
                        : "")));
    }

    private void buildBrowser() {
        addChild(
            new Label(
                178,
                51,
                170,
                17,
                () -> tr(BROWSER_TITLE) + "  "
                    + rows.getItems()
                        .size()
                    + "/"
                    + snapshot.size()));
        // #tr gui.gtnotgood.ldminer.clear
        // # Clear selection
        // # zh_CN 清空当前选择
        addChild(
            coloredButton(
                367,
                51,
                97,
                17,
                () -> tr("gui.gtnotgood.ldminer.clear"),
                this::confirmClear,
                () -> 0xFFFF8800));
        addChild(
            theme.textField(178, 72, 169, 18)
                .setOnChange(value -> {
                    search = value.toLowerCase(Locale.ROOT);
                    refreshRows();
                }));
        addChild(coloredButton(352, 72, 112, 18, this::categoryLabel, () -> {
            category = (category + 1) % 5;
            refreshRows();
        }));
        // #tr gui.gtnotgood.ldminer.name
        // # Ore / search by name
        // # zh_CN 矿石名称 / 输入搜索
        addChild(new Label(178, 94, 140, 15, tr("gui.gtnotgood.ldminer.name")));
        // #tr gui.gtnotgood.ldminer.weight
        // # Weight
        // # zh_CN 权重
        addChild(new Label(318, 94, 42, 15, tr("gui.gtnotgood.ldminer.weight")));
        // #tr gui.gtnotgood.ldminer.dimension
        // # Dim.
        // # zh_CN 维度
        addChild(new Label(360, 94, 45, 15, tr("gui.gtnotgood.ldminer.dimension")));
        // #tr gui.gtnotgood.ldminer.action
        // # Action
        // # zh_CN 操作
        addChild(new Label(405, 94, 51, 15, tr("gui.gtnotgood.ldminer.action")));
        rows = new VirtualScrollerView<>(178, 111, 287, 150, 20, this::row);
        rows.setThumbTexture(theme.scrollThumb);
        // #tr gui.gtnotgood.ldminer.empty
        // # No matching ores
        // # zh_CN 没有符合条件的矿石
        addChild(
            new Label(
                180,
                162,
                278,
                20,
                () -> rows.getItems()
                    .isEmpty() ? tr("gui.gtnotgood.ldminer.empty") : ""));
        addChild(rows);
    }

    private UIElement row(OreEntryInfo info, int index) {
        UIElement row = new UIElement(0, 0, 278, 20) {

            @Override
            protected void drawForeground(int mx, int my, int px, int py) {
                if (mx >= getScreenX() && mx < getScreenX() + getWidth()
                    && my >= getScreenY()
                    && my < getScreenY() + getHeight()
                    && my >= rows.getScreenY()
                    && my < rows.getScreenY() + rows.getHeight()) {
                    detail = info.ore.getDisplayName() + " | "
                        + tr("gui.gtnotgood.ldminer.weight")
                        + " "
                        + decimal(info.weight)
                        + " | "
                        + dimensionNames(info);
                }
            }
        };
        row.setBackground(new ColorRectTexture(index % 2 == 0 ? 0xFFE6EAF0 : 0xFFD6DDE6));
        row.addChild(new UIElement(2, 2, 16, 16).setBackground(new ItemStackTexture(info.ore)));
        row.addChild(new Label(20, 0, 120, 20, info.ore.getDisplayName()));
        row.addChild(new Label(140, 0, 42, 20, decimal(info.weight)));
        // #tr gui.gtnotgood.ldminer.local
        // # Local
        // # zh_CN 本地
        row.addChild(
            new Label(
                182,
                0,
                45,
                20,
                String.join(",", info.dimAbbrs)
                    .replace("None", tr("gui.gtnotgood.ldminer.local"))));
        Button action = coloredButton(229, 1, 48, 18, () -> actionLabel(info), () -> {
            if (model.directional.getValue()) model.actions.sendToggleDirectionalOre(info);
            else model.actions.sendToggleFilter(info);
        });
        IntSupplier stateColor = () -> selected(info) ? (model.directional.getValue() ? 0xFF337777 : 0xFFFF8800)
            : 0xFF44AAFF;
        action.setBackground(tinted(theme.button.copy(), () -> actionLabel(info), stateColor));
        action.setHoverTexture(tinted(theme.accent.copy(), () -> actionLabel(info), stateColor));
        row.addChild(action);
        return row;
    }

    /** Same sprite tinting and color pairs used by the wildcard pattern editor, with readable text contrast. */
    private Button coloredButton(int x, int y, int width, int height, Supplier<String> text, Runnable action) {
        return coloredButton(x, y, width, height, text, action, () -> 0xFF44AAFF);
    }

    private Button coloredButton(int x, int y, int width, int height, Supplier<String> text, Runnable action,
        IntSupplier color) {
        return new Button(x, y, width, height, tinted(theme.button.copy(), text, color), action)
            .setHoverTexture(tinted(theme.accent.copy(), text, color))
            .setDisabledTexture(theme.text(theme.disabled, text))
            .setFocusTexture(theme.focus);
    }

    private IGuiTexture tinted(SpriteTexture texture, Supplier<String> text, IntSupplier color) {
        TextTexture light = new TextTexture(text, 0xFFFFFFFF);
        TextTexture dark = new TextTexture(text, 0xFF182433);
        return (mx, my, x, y, width, height) -> {
            int tint = color.getAsInt();
            texture.setColor(tint)
                .draw(mx, my, x, y, width, height);
            int brightness = ((tint >> 16 & 255) * 299 + (tint >> 8 & 255) * 587 + (tint & 255) * 114) / 1000;
            (brightness > 200 ? dark : light).draw(mx, my, x, y, width, height);
        };
    }

    private String dimensionNames(OreEntryInfo info) {
        List<String> names = new ArrayList<>();
        for (String dim : info.dimAbbrs) names.add("None".equals(dim) ? model.dimension.getValue() : dim);
        return String.join(", ", names);
    }

    private String oreMode() {
        int mode = Math.max(0, Math.min(2, model.oreMode.getIntValue()));
        return tr(new String[] { ORE_MODE_RAW, ORE_MODE_CRUDE, ORE_MODE_CRUSHED }[mode]) + " +"
            + Math.round(LargeVoidMiner.ORE_MODE_ENERGY_BONUS[mode] * 100)
            + "%";
    }

    private String fortuneLabel() {
        int level = model.fortune.getIntValue();
        int index = Math.max(0, Math.min(6, (level - 3) / 2));
        return tr(
            FORTUNE_LEVEL) + " " + level + " +" + Math.round(LargeVoidMiner.FORTUNE_ENERGY_BONUS[index] * 100) + "%";
    }

    private String categoryLabel() {
        if (category == 1 || category == 2) return category == 1 ? openLabel() : selectedLabel();
        return tr((category == 3 ? CATEGORY_ASC : category == 4 ? CATEGORY_DESC : CATEGORY_ALL));
    }

    private String selectedLabel() {
        // #tr gui.gtnotgood.ldminer.selected
        // # Targeted
        // # zh_CN 已定向
        return model.directional.getValue() ? tr("gui.gtnotgood.ldminer.selected") : tr(CATEGORY_FILTERED);
    }

    private String openLabel() {
        // #tr gui.gtnotgood.ldminer.select
        // # Not targeted
        // # zh_CN 未定向
        return model.directional.getValue() ? tr("gui.gtnotgood.ldminer.select") : tr(CATEGORY_UNFILTERED);
    }

    private String actionLabel(OreEntryInfo info) {
        return selected(info) ? selectedLabel() : openLabel();
    }

    private boolean selected(OreEntryInfo info) {
        return model.directional.getValue() ? info.aimed : info.filtered;
    }

    private void confirmClear() {
        // #tr gui.gtnotgood.ldminer.confirm
        // # Clear the current mode's ore selection?
        // # zh_CN 清空当前模式的矿石选择？
        Dialog.confirm(
            this,
            theme,
            tr("gui.gtnotgood.ldminer.confirm"),
            tr("gui.yes"),
            tr("gui.no"),
            accepted -> { if (accepted) model.actions.sendClearConfig(); });
    }

    private void refreshRows() {
        List<OreEntryInfo> filtered = new ArrayList<>();
        for (OreEntryInfo ore : snapshot) if (ore.ore != null && ore.ore.getDisplayName()
            .toLowerCase(Locale.ROOT)
            .contains(search) && (category != 1 || !selected(ore)) && (category != 2 || selected(ore)))
            filtered.add(ore);
        if (category >= 3) filtered.sort(
            category == 3 ? Comparator.comparingDouble(ore -> ore.weight)
                : Comparator.<OreEntryInfo>comparingDouble(ore -> ore.weight)
                    .reversed());
        rows.setItems(filtered);
    }

    /** Lets real dimension slots receive MUI input; modal dialogs still own the entire terminal. */
    @Override
    public UIElement hitTest(int mx, int my) {
        for (UIElement child : getChildren())
            if (child.isModal() && child.isInteractive()) return super.hitTest(mx, my);
        int localX = mx - getScreenX(), localY = my - getScreenY();
        if (localX >= 12 && localX < 102 && localY >= 72 && localY < 162) return null;
        return super.hitTest(mx, my);
    }

    @Override
    public void draw(int mx, int my, int px, int py) {
        detail = "";
        super.draw(mx, my, px, py);
    }

    @Override
    public void tick() {
        List<OreEntryInfo> latest = model.ores.getValue();
        boolean directional = model.directional.getValue();
        if (latest != null && (!snapshot.equals(latest) || directional != lastDirectional)) {
            snapshot = new ArrayList<>(latest);
            lastDirectional = directional;
            refreshRows();
        }
        fortune.setEnabled(model.oreMode.getIntValue() != 0);
        super.tick();
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%,.0f", value);
    }

    /** Wrapped mode instructions retain the original explicit filtering and UU-Matter explanations. */
    private static final class WrappedText extends UIElement {

        private final Supplier<String> text;

        private WrappedText(int x, int y, int width, int height, Supplier<String> text) {
            super(x, y, width, height);
            this.text = text;
        }

        @Override
        protected void drawForeground(int mx, int my, int px, int py) {
            Minecraft.getMinecraft().fontRenderer.drawSplitString(
                text.get()
                    .replace("\\n", "\n"),
                px + x,
                py + y,
                width,
                0xFF263544);
        }
    }
}
