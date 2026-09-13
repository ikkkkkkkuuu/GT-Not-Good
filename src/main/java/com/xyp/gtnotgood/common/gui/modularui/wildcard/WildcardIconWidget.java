package com.xyp.gtnotgood.common.gui.modularui.wildcard;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;

/**
 * 只读物品图标格：每帧从 supplier 读取要显示的物品并绘制（带槽位背景）。用于预览页的输入/输出格。
 */
public class WildcardIconWidget extends Widget<WildcardIconWidget> {

    private final Supplier<ItemStack> getter;

    public WildcardIconWidget(Supplier<ItemStack> getter) {
        this.getter = getter;
        size(18);
        background(WildcardPatternGui.THEME.slot);
        tooltip().setAutoUpdate(true)
            .tooltipBuilder(tooltip -> {
                ItemStack stack = displayStack();
                if (stack != null && stack.getItem() != null) tooltip.addFromItem(stack);
            });
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        ItemStack stack = displayStack();
        if (stack != null && stack.getItem() != null) {
            new ItemDrawable(stack).draw(context, 1, 1, 16, 16, widgetTheme.getTheme());
        }
    }

    /** Uses the same fluid display conversion for the icon and its item tooltip. */
    private ItemStack displayStack() {
        return com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials
            .toDisplayStack(getter == null ? null : getter.get());
    }

    /** Supplies the current item to MUI tooltip events and rebuilds text as the preview cycles. */
    @Override
    public void drawForeground(ModularGuiContext context) {
        ItemStack stack = displayStack();
        RichTooltip tooltip = getTooltip();
        if (stack != null && stack.getItem() != null
            && tooltip != null
            && isHoveringFor(tooltip.getShowUpTimer())
            && !context.hasDraggable()) {
            tooltip.draw(context, stack);
        }
    }
}
