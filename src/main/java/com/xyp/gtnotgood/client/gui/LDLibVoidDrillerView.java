package com.xyp.gtnotgood.client.gui;

import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.AMOUNT;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.BROWSER_TITLE;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.CATEGORY_ALL;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.CATEGORY_ASC;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.CATEGORY_DESC;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.CATEGORY_FILTERED;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.CATEGORY_UNFILTERED;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.DIMENSION_LINE;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.DIRECTIONAL_OFF;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.DIRECTIONAL_ON;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.ENERGY_COST;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.GAS_HINT;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.JUPITER;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.MODE_HINT_DIRECTIONAL;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.MODE_HINT_FILTERED;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.NEPTUNE;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.OVERCLOCK;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.REFRESH;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.SATURN;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.SLOTS_TITLE;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.URANUS;
import static com.xyp.gtnotgood.common.gui.modularui.multiblock.VoidDrillerGuiText.UU_COST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.common.api.gui.FluidEntryInfo;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeVoidDrillerConfigGui;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.gui.texture.ColorRectTexture;
import com.xyp.ldlib.gui.texture.IGuiTexture;
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

@SideOnly(Side.CLIENT)
public final class LDLibVoidDrillerView extends UIElement {

    private final LargeVoidDrillerConfigGui model;
    private final ModernTheme theme = new ModernTheme(
        path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));
    private VirtualScrollerView<FluidEntryInfo> rows;
    private List<FluidEntryInfo> snapshot = Collections.emptyList();
    private String search = "", detail = "";
    private int category;
    private boolean lastDirectional;

    public LDLibVoidDrillerView(LargeVoidDrillerConfigGui model) {
        super(0, 0, 475, 280);
        this.model = model;
        addChild(new Label(8, 5, 459, 16, tr("NameLargeVoidDriller")));
        String[] gasDimensions = { JUPITER, SATURN, URANUS, NEPTUNE };
        for (int i = 0; i < gasDimensions.length; i++) {
            final int index = i;
            addChild(coloredButton(12 + i * 56, 26, 54, 20, () -> tr(gasDimensions[index]),
                () -> model.actions.sendToggleGasDimension(index),
                () -> (model.gasDimensions.getIntValue() & (1 << index)) != 0 ? 0xFF337777 : 0xFF44AAFF));
        }
        addChild(coloredButton(241, 26, 100, 20,
            () -> tr(OVERCLOCK) + " " + model.overclock.getIntValue() + " (" + model.cycleTicks.getIntValue() + "t)",
            model.actions::sendCycleOverclock, () -> 0xFFFFAA44));
        addChild(new Label(347, 24, 118, 12, () -> number(model.energy.getLongValue()) + " EU/t"));
        addChild(
            new Label(347, 36, 118, 12, () -> tr(ENERGY_COST) + " x" + decimal(model.energyMult.getDoubleValue())));
        buildDimensions();
        buildBrowser();
        // #tr gui.gtnotgood.lddriller.hover
        // # Hover a fluid for output and dimensions. Click its status to toggle it.
        // # zh_CN 悬停流体查看产量与维度，点击状态切换过滤或定向。
        addChild(new Label(10, 264, 455, 16, () -> detail.isEmpty() ? tr("gui.gtnotgood.lddriller.hover") : detail));
    }

    private void buildDimensions() {
        addChild(new Label(12, 52, 90, 16, tr(SLOTS_TITLE)));
        addChild(coloredButton(109, 52, 57, 17, () -> tr(REFRESH), model.actions::sendRefreshPool));
        // #tr gui.gtnotgood.lddriller.dimcost
        // # Dim. cost
        // # zh_CN 维度增幅
        addChild(new Label(106, 80, 62, 16, tr("gui.gtnotgood.lddriller.dimcost")));
        addChild(new Label(106, 98, 62, 16, () -> "+" + number(model.dimIncrease.getDoubleValue()) + "%"));
        // #tr gui.gtnotgood.lddriller.filtercost
        // # Filter cost
        // # zh_CN 过滤增幅
        addChild(new Label(106, 120, 62, 16, tr("gui.gtnotgood.lddriller.filtercost")));
        addChild(new Label(106, 138, 62, 16, () -> "+" + number(model.weightIncrease.getDoubleValue()) + "%"));
        addChild(new Label(10, 164, 158, 16, () -> tr(DIMENSION_LINE) + model.dimension.getValue()));
        addChild(
            coloredButton(12, 184, 94, 20, () -> tr(model.directional.getValue() ? DIRECTIONAL_ON : DIRECTIONAL_OFF),
                model.actions::sendToggleDirectional, () -> model.directional.getValue() ? 0xFF9933FF : 0xFF337777));
        addChild(coloredButton(111, 184, 55, 20, () -> tr(model.enabled.getValue() ? "options.on" : "options.off"),
            () -> model.enabled.setValue(!model.enabled.getValue()),
            () -> model.enabled.getValue() ? 0xFF337777 : 0xFF4852FF));
        addChild(new WrappedText(12, 212, 154, 48,
            () -> tr(model.directional.getValue() ? MODE_HINT_DIRECTIONAL : MODE_HINT_FILTERED)
                + (model.directional.getValue() ? "\n" + tr(UU_COST) + " " + decimal(model.uu.getDoubleValue()) + " L/s"
                    : "")));
    }

    private void buildBrowser() {
        addChild(new Label(178, 51, 170, 17,
            () -> tr(BROWSER_TITLE) + "  " + rows.getItems().size() + "/" + snapshot.size()));
        // #tr gui.gtnotgood.lddriller.clear
        // # Clear selection
        // # zh_CN 清空当前选择
        addChild(coloredButton(367, 51, 97, 17, () -> tr("gui.gtnotgood.lddriller.clear"), this::confirmClear,
            () -> 0xFFFF8800));
        addChild(theme.textField(178, 72, 169, 18).setOnChange(value -> {
            search = value.toLowerCase(Locale.ROOT);
            refreshRows();
        }));
        addChild(coloredButton(352, 72, 112, 18, this::categoryLabel, () -> {
            category = (category + 1) % 5;
            refreshRows();
        }));
        // #tr gui.gtnotgood.lddriller.name
        // # Fluid / output per cycle
        // # zh_CN 流体名称 / 每周期产量
        addChild(new Label(178, 94, 140, 15, tr("gui.gtnotgood.lddriller.name")));
        // #tr gui.gtnotgood.lddriller.weight
        // # Weight
        // # zh_CN 权重
        addChild(new Label(318, 94, 42, 15, tr("gui.gtnotgood.lddriller.weight")));
        // #tr gui.gtnotgood.lddriller.dimension
        // # Dim.
        // # zh_CN 维度
        addChild(new Label(360, 94, 45, 15, tr("gui.gtnotgood.lddriller.dimension")));
        // #tr gui.gtnotgood.lddriller.action
        // # Action
        // # zh_CN 操作
        addChild(new Label(405, 94, 51, 15, tr("gui.gtnotgood.lddriller.action")));
        rows = new VirtualScrollerView<>(178, 111, 287, 150, 28, this::row);
        rows.setThumbTexture(theme.scrollThumb);
        // #tr gui.gtnotgood.lddriller.empty
        // # No matching fluids
        // # zh_CN 没有符合条件的流体
        addChild(
            new Label(180, 162, 278, 20, () -> rows.getItems().isEmpty() ? tr("gui.gtnotgood.lddriller.empty") : ""));
        addChild(rows);
    }

    private UIElement row(FluidEntryInfo info, int index) {
        UIElement row = new UIElement(0, 0, 278, 28) {

            @Override
            protected void drawForeground(int mx, int my, int px, int py) {
                if (
                    mx >= getScreenX() && mx < getScreenX() + getWidth()
                        && my >= getScreenY()
                        && my < getScreenY() + getHeight()
                        && my >= rows.getScreenY()
                        && my < rows.getScreenY() + rows.getHeight()
                ) {
                    detail = info.fluid.getLocalizedName() + " | "
                        + tr(AMOUNT)
                        + " "
                        + amountRange(info)
                        + " | "
                        + tr("gui.gtnotgood.lddriller.weight")
                        + " "
                        + decimal(info.weight)
                        + " | "
                        + dimensionNames(info);
                }
            }
        };
        row.setBackground(new ColorRectTexture(index % 2 == 0 ? 0xFFE6EAF0 : 0xFFD6DDE6));
        row.addChild(new UIElement(2, 6, 16, 16).setBackground(fluidTexture(info.fluid)));
        row.addChild(new Label(20, 0, 120, 14, info.fluid.getLocalizedName()));
        row.addChild(new Label(20, 14, 120, 12, amountRange(info)));
        row.addChild(new Label(140, 0, 42, 28, decimal(info.weight)));
        // #tr gui.gtnotgood.lddriller.local
        // # Local
        // # zh_CN 本地
        row.addChild(new Label(182, 0, 45, 28,
            String.join(",", info.dimAbbrs).replace("None", tr("gui.gtnotgood.lddriller.local"))));
        Button action = coloredButton(229, 5, 48, 18, () -> actionLabel(info),
            () -> model.actions.sendToggleFluid(info));
        IntSupplier stateColor = () -> selected(info) ? (model.directional.getValue() ? 0xFF337777 : 0xFFFF8800)
            : 0xFF44AAFF;
        action.setBackground(tinted(theme.button.copy(), () -> actionLabel(info), stateColor));
        action.setHoverTexture(tinted(theme.accent.copy(), () -> actionLabel(info), stateColor));
        row.addChild(action);
        return row;
    }

    private Button coloredButton(int x, int y, int width, int height, Supplier<String> text, Runnable action) {
        return coloredButton(x, y, width, height, text, action, () -> 0xFF44AAFF);
    }

    private Button coloredButton(int x, int y, int width, int height, Supplier<String> text, Runnable action,
        IntSupplier color) {
        return new Button(x, y, width, height, tinted(theme.button.copy(), text, color), action)
            .setHoverTexture(tinted(theme.accent.copy(), text, color))
            .setDisabledTexture(theme.text(theme.disabled, text)).setFocusTexture(theme.focus);
    }

    private IGuiTexture tinted(SpriteTexture texture, Supplier<String> text, IntSupplier color) {
        TextTexture light = new TextTexture(text, 0xFFFFFFFF);
        TextTexture dark = new TextTexture(text, 0xFF182433);
        return (mx, my, x, y, width, height) -> {
            int tint = color.getAsInt();
            texture.setColor(tint).draw(mx, my, x, y, width, height);
            int brightness = ((tint >> 16 & 255) * 299 + (tint >> 8 & 255) * 587 + (tint & 255) * 114) / 1000;
            (brightness > 200 ? dark : light).draw(mx, my, x, y, width, height);
        };
    }

    private static IGuiTexture fluidTexture(FluidStack fluid) {
        return (mx, my, x, y, width, height) -> {
            IIcon icon = fluid.getFluid().getIcon(fluid);
            if (icon == null || width <= 0 || height <= 0) return;
            int color = fluid.getFluid().getColor(fluid);
            GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
            try {
                Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glColor4f((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawingQuads();
                tessellator.addVertexWithUV(x, y + height, 0, icon.getMinU(), icon.getMaxV());
                tessellator.addVertexWithUV(x + width, y + height, 0, icon.getMaxU(), icon.getMaxV());
                tessellator.addVertexWithUV(x + width, y, 0, icon.getMaxU(), icon.getMinV());
                tessellator.addVertexWithUV(x, y, 0, icon.getMinU(), icon.getMinV());
                tessellator.draw();
            } finally {
                GL11.glPopAttrib();
            }
        };
    }

    private String dimensionNames(FluidEntryInfo info) {
        List<String> names = new ArrayList<>();
        for (String dim : info.dimAbbrs) names.add("None".equals(dim) ? model.dimension.getValue() : dim);
        return String.join(", ", names);
    }

    private String categoryLabel() {
        if (category == 1 || category == 2) return category == 1 ? openLabel() : selectedLabel();
        return tr(category == 3 ? CATEGORY_ASC : category == 4 ? CATEGORY_DESC : CATEGORY_ALL);
    }

    private String selectedLabel() {
        // #tr gui.gtnotgood.lddriller.selected
        // # Target
        // # zh_CN 已定向
        return model.directional.getValue() ? tr("gui.gtnotgood.lddriller.selected") : tr(CATEGORY_FILTERED);
    }

    private String openLabel() {
        // #tr gui.gtnotgood.lddriller.select
        // # Unset
        // # zh_CN 未定向
        return model.directional.getValue() ? tr("gui.gtnotgood.lddriller.select") : tr(CATEGORY_UNFILTERED);
    }

    private String actionLabel(FluidEntryInfo info) {
        return selected(info) ? selectedLabel() : openLabel();
    }

    private boolean selected(FluidEntryInfo info) {
        return model.directional.getValue() ? info.aimed : info.filtered;
    }

    private void confirmClear() {
        // #tr gui.gtnotgood.lddriller.confirm
        // # Clear this mode's selection?
        // # zh_CN 清空当前模式的选择？
        Dialog.confirm(this, theme, tr("gui.gtnotgood.lddriller.confirm"), tr("gui.yes"), tr("gui.no"),
            accepted -> { if (accepted) model.actions.sendClearConfig(); });
    }

    private void refreshRows() {
        List<FluidEntryInfo> filtered = new ArrayList<>();
        for (FluidEntryInfo fluid : snapshot) {
            if (fluid.fluid == null || fluid.fluid.getFluid() == null) continue;
            boolean matches = fluid.fluid.getLocalizedName().toLowerCase(Locale.ROOT).contains(search)
                || fluid.fluid.getFluid().getName().toLowerCase(Locale.ROOT).contains(search);
            if (matches && (category != 1 || !selected(fluid)) && (category != 2 || selected(fluid))) {
                filtered.add(fluid);
            }
        }
        if (category >= 3) filtered.sort(category == 3 ? Comparator.comparingDouble(fluid -> fluid.weight)
            : Comparator.<FluidEntryInfo>comparingDouble(fluid -> fluid.weight).reversed());
        rows.setItems(filtered);
    }

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
        int localX = mx - getScreenX(), localY = my - getScreenY();
        detail = localX >= 12 && localX < 234 && localY >= 26 && localY < 46 ? tr(GAS_HINT) : "";
        super.draw(mx, my, px, py);
    }

    @Override
    public void tick() {
        List<FluidEntryInfo> latest = model.fluids.getValue();
        boolean directional = model.directional.getValue();
        if (latest != null && (!snapshot.equals(latest) || directional != lastDirectional)) {
            snapshot = new ArrayList<>(latest);
            lastDirectional = directional;
            refreshRows();
        }
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

    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String amountRange(FluidEntryInfo info) {
        return number(info.minAmount) + "~" + number(info.maxAmount) + " L";
    }

    private static final class WrappedText extends UIElement {

        private final Supplier<String> text;

        private WrappedText(int x, int y, int width, int height, Supplier<String> text) {
            super(x, y, width, height);
            this.text = text;
        }

        @Override
        protected void drawForeground(int mx, int my, int px, int py) {
            Minecraft.getMinecraft().fontRenderer.drawSplitString(text.get().replace("\\n", "\n"), px + x, py + y,
                width, 0xFF263544);
        }
    }
}
