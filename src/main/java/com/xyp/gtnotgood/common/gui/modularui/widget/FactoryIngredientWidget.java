package com.xyp.gtnotgood.common.gui.modularui.widget;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.ObjectValue;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPreview;
import com.xyp.gtnotgood.utils.machine.factory.FactoryText;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Recipe-viewer-compatible material icon with a fractional throughput label at its bottom-right corner. */
public final class FactoryIngredientWidget extends ItemDisplayWidget {

    private final Supplier<FactoryPreview.Ingredient> ingredient;

    public FactoryIngredientWidget(Supplier<FactoryPreview.Ingredient> ingredient) {
        this.ingredient = ingredient;
        item(new ObjectValue.Dynamic<>(ItemStack.class, () -> {
            FactoryPreview.Ingredient entry = ingredient.get();
            return entry == null ? null : entry.display;
        }, value -> {}));
        displayAmount(false);
        background(GTNGGuiTextures.MODERN_VAULT_ITEM_SLOT);
        // A reused slot can point to a new snapshot or page without its dynamic value emitting a change event.
        // Rebuild only while the tooltip is drawn, using the same current ingredient as the icon and amount.
        tooltip().setAutoUpdate(true);
        tooltipBuilder(tooltip -> {
            FactoryPreview.Ingredient entry = ingredient.get();
            if (entry == null) return;
            tooltip.addLine(entry.name());
            tooltip.addLine(
                java.math.BigDecimal.valueOf(entry.rate)
                    .stripTrailingZeros()
                    .toPlainString() + (entry.fluid == null ? " " : " L") + FactoryText.PER_TICK.text());
            if (entry.internal) tooltip.addLine(FactoryText.INTERNAL_SURPLUS.text());
        });
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.draw(context, theme);
        FactoryPreview.Ingredient entry = ingredient.get();
        if (entry != null && entry.internal) GuiDraw.drawScaledAlignedTextInBox(
            "§e!",
            1,
            1,
            getArea().width - 2,
            getArea().height - 2,
            Alignment.TopLeft,
            0.8f);
        if (entry != null) GuiDraw.drawScaledAlignedTextInBox(
            entry.amount(),
            1,
            1,
            getArea().width - 2,
            getArea().height - 2,
            Alignment.BottomRight,
            0.8f);
    }
}
