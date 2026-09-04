package com.xyp.gtnotgood.common.items.wildcard.model;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

/**
 * 新材料轴模型的物品 NBT 读写：三个平行组件列表（输入/输出/过滤）+ 模型版本 + 展开数缓存。
 */
public final class WildcardModelState {

    public static final int MODEL_VERSION = 2;

    public static final String KEY_MODEL_VERSION = "WPModelVersion";
    public static final String KEY_INPUT = "WPInputComponents";
    public static final String KEY_OUTPUT = "WPOutputComponents";
    public static final String KEY_FILTER = "WPFilterComponents";
    public static final String KEY_EXPANDED_COUNT = "WPExpandedCount";

    private WildcardModelState() {}

    public static boolean isNewModel(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        return tag != null && tag.getInteger(KEY_MODEL_VERSION) >= MODEL_VERSION;
    }

    public static void markModelVersion(ItemStack stack) {
        getOrCreateTag(stack).setInteger(KEY_MODEL_VERSION, MODEL_VERSION);
    }

    public static List<IWildcardIOComponent> getInputs(ItemStack stack) {
        return WildcardComponentCodec.readIO(tag(stack), KEY_INPUT);
    }

    public static List<IWildcardIOComponent> getOutputs(ItemStack stack) {
        return WildcardComponentCodec.readIO(tag(stack), KEY_OUTPUT);
    }

    public static List<IWildcardFilterComponent> getFilters(ItemStack stack) {
        return WildcardComponentCodec.readFilters(tag(stack), KEY_FILTER);
    }

    public static void setInputs(ItemStack stack, List<IWildcardIOComponent> components) {
        getOrCreateTag(stack).setTag(KEY_INPUT, WildcardComponentCodec.writeIO(components));
    }

    public static void setOutputs(ItemStack stack, List<IWildcardIOComponent> components) {
        getOrCreateTag(stack).setTag(KEY_OUTPUT, WildcardComponentCodec.writeIO(components));
    }

    public static void setFilters(ItemStack stack, List<IWildcardFilterComponent> components) {
        getOrCreateTag(stack).setTag(KEY_FILTER, WildcardComponentCodec.writeFilters(components));
    }

    public static int getExpandedCount(ItemStack stack) {
        NBTTagCompound tag = tag(stack);
        return tag == null ? 0 : Math.max(0, tag.getInteger(KEY_EXPANDED_COUNT));
    }

    public static void setExpandedCount(ItemStack stack, int count) {
        getOrCreateTag(stack).setInteger(KEY_EXPANDED_COUNT, Math.max(0, count));
    }

    /** 确保三个列表键存在（空列表）并标注模型版本。 */
    public static void ensureInitialized(ItemStack stack) {
        if (stack == null) return;
        NBTTagCompound tag = getOrCreateTag(stack);
        if (!tag.hasKey(KEY_INPUT, Constants.NBT.TAG_LIST)) tag.setTag(KEY_INPUT, new NBTTagList());
        if (!tag.hasKey(KEY_OUTPUT, Constants.NBT.TAG_LIST)) tag.setTag(KEY_OUTPUT, new NBTTagList());
        if (!tag.hasKey(KEY_FILTER, Constants.NBT.TAG_LIST)) tag.setTag(KEY_FILTER, new NBTTagList());
        markModelVersion(stack);
    }

    /** 导出配置（三个列表 + 展开数），用于网络同步。 */
    public static NBTTagCompound exportConfig(ItemStack stack) {
        NBTTagCompound exported = new NBTTagCompound();
        NBTTagCompound source = getOrCreateTag(stack);
        copyList(source, exported, KEY_INPUT);
        copyList(source, exported, KEY_OUTPUT);
        copyList(source, exported, KEY_FILTER);
        exported.setInteger(KEY_EXPANDED_COUNT, getExpandedCount(stack));
        exported.setInteger(KEY_MODEL_VERSION, MODEL_VERSION);
        return exported;
    }

    /** 应用配置（服务端收到客户端更新时）。 */
    public static void applyConfig(ItemStack stack, NBTTagCompound config) {
        if (stack == null || config == null) return;
        NBTTagCompound tag = getOrCreateTag(stack);
        copyList(config, tag, KEY_INPUT);
        copyList(config, tag, KEY_OUTPUT);
        copyList(config, tag, KEY_FILTER);
        if (config.hasKey(KEY_EXPANDED_COUNT)) {
            tag.setInteger(KEY_EXPANDED_COUNT, Math.max(0, config.getInteger(KEY_EXPANDED_COUNT)));
        }
        markModelVersion(stack);
    }

    // ============================================================

    private static NBTTagCompound tag(ItemStack stack) {
        return stack == null ? null : stack.getTagCompound();
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (stack.getTagCompound() == null) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    private static void copyList(NBTTagCompound source, NBTTagCompound target, String key) {
        if (source.hasKey(key, Constants.NBT.TAG_LIST)) {
            target.setTag(
                key,
                source.getTagList(key, Constants.NBT.TAG_COMPOUND)
                    .copy());
        } else {
            target.setTag(key, new NBTTagList());
        }
    }
}
