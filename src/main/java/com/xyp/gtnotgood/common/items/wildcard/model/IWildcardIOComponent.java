package com.xyp.gtnotgood.common.items.wildcard.model;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.Materials;

/**
 * 输入/输出组件：把"当前展开的材料"变成一个具体的物品/流体 stack。
 * 返回 null 表示该材料无法生成此组件（展开时整条样板被跳过）。
 */
public interface IWildcardIOComponent extends IWildcardComponent {

    /**
     * 用给定材料生成具体 stack。
     *
     * @param material 当前正在展开的材料（simple 组件可忽略它，返回固定 stack）
     * @return 具体物品/流体 stack，或 null（无法生成）
     */
    ItemStack apply(Materials material);

    /** 用于 GUI 显示的代表性 stack（可为 null）。 */
    ItemStack getDisplayStack();

    /** 是否为空（未配置有效内容）。 */
    boolean isEmpty();
}
