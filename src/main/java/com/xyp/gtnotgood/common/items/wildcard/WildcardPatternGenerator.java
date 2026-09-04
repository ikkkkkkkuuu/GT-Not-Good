package com.xyp.gtnotgood.common.items.wildcard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardExpansion;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMigration;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardModelState;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import gregtech.api.enums.Materials;

/**
 * 通配样板符配方生成器（材料轴模型）。
 * 遍历全部材料，对每个通过过滤的材料把输入/输出组件套上该材料，生成一个具体 AE2 加工样板。
 */
public final class WildcardPatternGenerator {

    private static final String KEY_WILDCARD = "WildcardPattern";
    private static final String KEY_SELECTED_MATERIAL = "WildcardSelectedMaterial";
    public static final String KEY_GENERATED_PATTERN_ID = "WildcardGeneratedPatternId";

    private WildcardPatternGenerator() {}

    // ============================================================
    // 标记 / 判定
    // ============================================================

    public static boolean isWildcardPattern(ItemStack stack) {
        return stack != null && (stack.getItem() == com.xyp.gtnotgood.loader.ItemsLoader.wildcardPattern
            || stack.hasTagCompound() && stack.getTagCompound()
                .getBoolean(KEY_WILDCARD));
    }

    /** 标记为通配样板并确保新模型 NBT 已初始化（含旧存档迁移）。 */
    public static void markAsWildcard(ItemStack stack) {
        if (stack == null) return;
        NBTTagCompound tag = getOrCreateTag(stack);
        tag.setBoolean(KEY_WILDCARD, true);
        WildcardMigration.migrateIfNeeded(stack);
        WildcardModelState.ensureInitialized(stack);
    }

    // ============================================================
    // AE2 对接：单样板详情（供 ItemEncodedPattern.getPatternForItem）
    // ============================================================

    public static ICraftingPatternDetails getDetailsForItem(ItemStack stack, World world) {
        if (!isWildcardPattern(stack)) {
            return null;
        }
        markAsWildcard(stack);
        if (isGeneratedPattern(stack)) {
            return createDetailForCurrentStack(stack, world);
        }
        return getDisplayDetails(stack, world);
    }

    /**
     * 展开全部具体样板 - AE2 mixin 调用此方法拿到多个配方。
     */
    public static List<ICraftingPatternDetails> generateAllDetails(ItemStack stack, World world) {
        if (!isWildcardPattern(stack)) {
            return Collections.emptyList();
        }
        markAsWildcard(stack);

        List<IWildcardIOComponent> inputs = WildcardModelState.getInputs(stack);
        List<IWildcardIOComponent> outputs = WildcardModelState.getOutputs(stack);
        List<IWildcardFilterComponent> filters = WildcardModelState.getFilters(stack);

        List<ICraftingPatternDetails> result = new ArrayList<>();
        for (WildcardExpansion.Expanded expanded : WildcardExpansion.expand(inputs, outputs, filters)) {
            ItemStack generated = createPatternStack(stack, expanded.material, expanded.inputs, expanded.outputs);
            if (generated == null) {
                continue;
            }
            ICraftingPatternDetails detail = createDetailForCurrentStack(generated, world);
            if (detail != null) {
                result.add(detail);
            }
        }
        result.sort(Comparator.comparing(details -> getPatternIdentity(details == null ? null : details.getPattern())));
        WildcardModelState.setExpandedCount(stack, result.size());
        return result;
    }

    /** 统计展开数量（tooltip 用）。 */
    public static int countActualPatternsAfterExclude(ItemStack stack) {
        if (!isWildcardPattern(stack)) {
            return 0;
        }
        markAsWildcard(stack);
        return WildcardExpansion.countExpanded(
            WildcardModelState.getInputs(stack),
            WildcardModelState.getOutputs(stack),
            WildcardModelState.getFilters(stack));
    }

    // ============================================================
    // 显示 / 输出
    // ============================================================

    private static ICraftingPatternDetails getDisplayDetails(ItemStack stack, World world) {
        return new WildcardPreviewPatternDetails(stack, getRepresentativeInput(stack), getRepresentativeOutput(stack));
    }

    public static ICraftingPatternDetails createDetailForCurrentStack(ItemStack stack, World world) {
        try {
            return new WildcardPatternDetails(stack, world);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static ItemStack getOutputForItem(ItemStack stack, World world) {
        if (!isWildcardPattern(stack)) {
            return null;
        }
        markAsWildcard(stack);
        if (isGeneratedPattern(stack)) {
            ItemStack generatedOutput = getFirstCondensedOutput(createDetailForCurrentStack(stack, world));
            if (generatedOutput != null) {
                return generatedOutput;
            }
        }
        return getRepresentativeOutput(stack);
    }

    public static ItemStack getRepresentativeInput(ItemStack stack) {
        return firstDisplayStack(WildcardModelState.getInputs(stack), stack);
    }

    public static ItemStack getRepresentativeOutput(ItemStack stack) {
        return firstDisplayStack(WildcardModelState.getOutputs(stack), stack);
    }

    private static ItemStack firstDisplayStack(List<IWildcardIOComponent> components, ItemStack fallbackStack) {
        if (components != null) {
            for (IWildcardIOComponent component : components) {
                if (component == null || component.isEmpty()) {
                    continue;
                }
                ItemStack display = component.getDisplayStack();
                if (display != null) {
                    return display;
                }
            }
        }
        if (fallbackStack == null) {
            return null;
        }
        ItemStack fallback = fallbackStack.copy();
        fallback.stackSize = 1;
        return fallback;
    }

    private static ItemStack getFirstCondensedOutput(ICraftingPatternDetails details) {
        IAEItemStack[] outputs = details == null ? null : details.getCondensedOutputs();
        if (outputs == null || outputs.length == 0 || outputs[0] == null) {
            return null;
        }
        ItemStack output = outputs[0].getItemStack();
        return output == null ? null : output.copy();
    }

    // ============================================================
    // 具体样板 stack 构建
    // ============================================================

    /**
     * 用输入/输出 stack 列表构建具体 AE2 加工样板物品（多槽）。
     */
    private static ItemStack createPatternStack(ItemStack template, Materials material, List<ItemStack> inputs,
        List<ItemStack> outputs) {
        if ((inputs == null || inputs.isEmpty()) && (outputs == null || outputs.isEmpty())) {
            return null;
        }

        NBTTagList inputList = buildPatternList(inputs);
        NBTTagList outputList = buildPatternList(outputs);

        ItemStack result = template.copy();
        NBTTagCompound resultTag = getOrCreateTag(result);
        resultTag.setTag("in", inputList);
        resultTag.setTag("out", outputList);
        String materialName = material == null ? "" : material.mName;
        resultTag.setString(KEY_SELECTED_MATERIAL, materialName);
        resultTag.setString(KEY_GENERATED_PATTERN_ID, buildGeneratedPatternId(materialName, inputs, outputs));
        resultTag.setBoolean("crafting", false);
        resultTag.removeTag("InvalidPattern");
        return result;
    }

    /** 构建 AE2 样板槽位 NBT 列表。空列表补一个占位空标签。 */
    private static NBTTagList buildPatternList(List<ItemStack> stacks) {
        NBTTagList list = new NBTTagList();
        if (stacks == null || stacks.isEmpty()) {
            list.appendTag(new NBTTagCompound());
            return list;
        }
        for (ItemStack stack : stacks) {
            if (stack == null) {
                list.appendTag(new NBTTagCompound());
                continue;
            }
            NBTTagCompound slotTag = new NBTTagCompound();
            stack.writeToNBT(slotTag);
            int count = Math.max(1, stack.stackSize);
            slotTag.setInteger("Count", count);
            slotTag.setLong("Cnt", count);
            list.appendTag(slotTag);
        }
        return list;
    }

    private static String buildGeneratedPatternId(String materialName, List<ItemStack> inputs,
        List<ItemStack> outputs) {
        StringBuilder builder = new StringBuilder(sanitizeIdentityPart(materialName)).append('|');
        for (ItemStack stack : inputs) {
            builder.append(getStackFingerprint(stack))
                .append('+');
        }
        builder.append("->");
        for (ItemStack stack : outputs) {
            builder.append(getStackFingerprint(stack))
                .append('+');
        }
        return builder.toString();
    }

    private static String sanitizeIdentityPart(String value) {
        return value == null ? ""
            : value.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("->", "-\\>");
    }

    // ============================================================
    // 唯一标识
    // ============================================================

    public static boolean isGeneratedPattern(ItemStack stack) {
        return !getGeneratedPatternId(stack).isEmpty();
    }

    public static String getGeneratedPatternId(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        return tag == null || !tag.hasKey(KEY_GENERATED_PATTERN_ID) ? "" : tag.getString(KEY_GENERATED_PATTERN_ID);
    }

    public static String getPatternIdentity(ItemStack stack) {
        String generatedId = getGeneratedPatternId(stack);
        if (!generatedId.isEmpty()) {
            return generatedId;
        }
        return getStackFingerprint(stack);
    }

    private static String getStackFingerprint(ItemStack stack) {
        if (stack == null) {
            return "empty";
        }
        String itemName = String.valueOf(net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()));
        if (itemName == null || itemName.isEmpty() || "null".equals(itemName)) {
            itemName = stack.getItem() == null ? "null"
                : stack.getItem()
                    .getClass()
                    .getName();
        }
        NBTTagCompound tag = stack.getTagCompound();
        return itemName + "@"
            + stack.getItemDamage()
            + "x"
            + Math.max(1, stack.stackSize)
            + "#"
            + (tag == null ? "" : Integer.toHexString(tag.hashCode()));
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (stack.getTagCompound() == null) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
