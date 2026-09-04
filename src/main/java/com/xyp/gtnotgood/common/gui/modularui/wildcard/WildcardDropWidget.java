package com.xyp.gtnotgood.common.gui.modularui.wildcard;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.MCHelper;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;

/**
 * 一个类槽位的输入控件：渲染一个物品图标，支持
 * <ul>
 * <li>鼠标左键：把光标上拿着的物品放入（复制一份，数量置 1），空手点击则清空；</li>
 * <li>从 NEI 拖入物品（实现 {@link RecipeViewerGhostIngredientSlot}）。</li>
 * </ul>
 * 放入/拖入的物品通过 {@code onDrop} 回调交给上层解析（矿辞前缀 / 真实名 / 固定物品）。
 * 纯客户端交互，不做同步；结果由外层"保存"统一推送到服务端。
 */
public class WildcardDropWidget extends Widget<WildcardDropWidget>
    implements Interactable, RecipeViewerGhostIngredientSlot<ItemStack> {

    private final Supplier<ItemStack> getter;
    private final Consumer<ItemStack> onDrop;

    public WildcardDropWidget(Supplier<ItemStack> getter, Consumer<ItemStack> onDrop) {
        this.getter = getter;
        this.onDrop = onDrop;
        size(18);
        background(GuiTextures.SLOT_ITEM);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        ItemStack stack = getter == null ? null : getter.get();
        stack = com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials.toDisplayStack(stack);
        if (stack != null && stack.getItem() != null) {
            new ItemDrawable(stack).draw(context, 1, 1, 16, 16, widgetTheme.getTheme());
        }
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        EntityPlayer player = MCHelper.getPlayer();
        ItemStack held = player == null ? null : player.inventory.getItemStack();
        if (held != null && held.getItem() != null) {
            ItemStack copy = held.copy();
            copy.stackSize = 1;
            fire(copy);
        } else {
            fire(null);
        }
        Interactable.playButtonClickSound();
        return Result.SUCCESS;
    }

    @Override
    public boolean handleDragAndDrop(@NotNull ItemStack draggedStack, int button) {
        if (draggedStack.getItem() == null) return false;
        ItemStack copy = draggedStack.copy();
        copy.stackSize = 1;
        fire(copy);
        return true;
    }

    private void fire(ItemStack stack) {
        if (onDrop != null) onDrop.accept(stack);
    }
}
