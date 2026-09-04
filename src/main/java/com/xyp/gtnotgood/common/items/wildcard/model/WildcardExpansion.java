package com.xyp.gtnotgood.common.items.wildcard.model;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.Materials;

/**
 * 材料轴展开引擎（对齐 Wildcard-Pattern 语义）。
 * 遍历全部材料，对每个通过全部过滤组件（AND）的材料，把所有输入/输出组件套上该材料生成一个具体样板。
 * 任一组件对该材料产不出 stack → 该材料整体跳过。
 */
public final class WildcardExpansion {

    private WildcardExpansion() {}

    /** 单个展开结果：一个材料 → 输入列表 + 输出列表。 */
    public static final class Expanded {

        public final Materials material;
        public final List<ItemStack> inputs;
        public final List<ItemStack> outputs;

        Expanded(Materials material, List<ItemStack> inputs, List<ItemStack> outputs) {
            this.material = material;
            this.inputs = inputs;
            this.outputs = outputs;
        }
    }

    /**
     * 展开全部样板。
     *
     * @param inputs  输入组件
     * @param outputs 输出组件
     * @param filters 过滤组件（AND）
     * @return 每个通过过滤且输入输出都能生成的材料对应一个 Expanded
     */
    public static List<Expanded> expand(List<IWildcardIOComponent> inputs, List<IWildcardIOComponent> outputs,
        List<IWildcardFilterComponent> filters) {
        List<Expanded> result = new ArrayList<>();
        if (inputs == null || outputs == null) return result;

        boolean hasInput = hasNonEmpty(inputs);
        boolean hasOutput = hasNonEmpty(outputs);
        if (!hasInput && !hasOutput) return result;

        for (Materials material : Materials.getAll()) {
            if (!WildcardMaterials.isExpandableMaterial(material)) continue;
            if (!passesFilters(material, filters)) continue;

            List<ItemStack> inStacks = applyAll(inputs, material);
            if (inStacks == null) continue; // 某个输入组件产不出 → 跳过材料
            List<ItemStack> outStacks = applyAll(outputs, material);
            if (outStacks == null) continue;
            if (inStacks.isEmpty() && outStacks.isEmpty()) continue;

            result.add(new Expanded(material, inStacks, outStacks));
        }
        return result;
    }

    /** 只统计展开数量（用于 tooltip），避免构建全部 stack 的开销。 */
    public static int countExpanded(List<IWildcardIOComponent> inputs, List<IWildcardIOComponent> outputs,
        List<IWildcardFilterComponent> filters) {
        return expand(inputs, outputs, filters).size();
    }

    private static boolean passesFilters(Materials material, List<IWildcardFilterComponent> filters) {
        if (filters == null || filters.isEmpty()) return true;
        for (IWildcardFilterComponent filter : filters) {
            if (filter != null && !filter.test(material)) return false;
        }
        return true;
    }

    /**
     * 把所有非空组件套上材料生成 stack；任一非空组件产出 null 则返回 null（跳过材料）。
     * 空组件被忽略。
     */
    private static List<ItemStack> applyAll(List<IWildcardIOComponent> components, Materials material) {
        List<ItemStack> stacks = new ArrayList<>();
        for (IWildcardIOComponent component : components) {
            if (component == null || component.isEmpty()) continue;
            ItemStack stack = component.apply(material);
            if (stack == null || stack.getItem() == null) return null;
            stacks.add(stack);
        }
        return stacks;
    }

    private static boolean hasNonEmpty(List<IWildcardIOComponent> components) {
        for (IWildcardIOComponent component : components) {
            if (component != null && !component.isEmpty()) return true;
        }
        return false;
    }
}
