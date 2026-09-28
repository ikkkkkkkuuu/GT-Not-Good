package com.xyp.gtnotgood.common.items.wildcard;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardModelState;
import com.xyp.gtnotgood.common.items.wildcard.model.io.FluidIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.SimpleIOComponent;

/**
 * 通配样板符状态管理
 * 负责保存和加载物品的配置数据
 */
public final class WildcardPatternState {

    private static final String KEY_INPUT_COMPONENTS = "WildcardInputComponents";
    private static final String KEY_OUTPUT_COMPONENTS = "WildcardOutputComponents";
    private static final String KEY_EXPANDED_PATTERN_COUNT = "WildcardExpandedPatternCount";

    private WildcardPatternState() {}

    /**
     * 确保物品NBT已初始化
     */
    public static void ensureInitialized(ItemStack stack) {
        if (stack == null) return;

        NBTTagCompound tag = getOrCreateTag(stack);
        if (!tag.hasKey(KEY_INPUT_COMPONENTS, Constants.NBT.TAG_LIST)) {
            tag.setTag(KEY_INPUT_COMPONENTS, new NBTTagList());
        }
        if (!tag.hasKey(KEY_OUTPUT_COMPONENTS, Constants.NBT.TAG_LIST)) {
            tag.setTag(KEY_OUTPUT_COMPONENTS, new NBTTagList());
        }
    }

    /**
     * 从普通AE2样板初始化
     */
    public static void initializeFromPattern(ItemStack stack) {
        ensureInitialized(stack);
    }

    /**
     * 获取输入条目列表
     */
    public static List<WildcardPatternEntry> getInputEntries(ItemStack stack) {
        return getEntries(stack, KEY_INPUT_COMPONENTS);
    }

    /**
     * 获取输出条目列表
     */
    public static List<WildcardPatternEntry> getOutputEntries(ItemStack stack) {
        return getEntries(stack, KEY_OUTPUT_COMPONENTS);
    }

    /**
     * 设置输入条目列表
     */
    public static void setInputEntries(ItemStack stack, List<WildcardPatternEntry> entries) {
        getOrCreateTag(stack).setTag(KEY_INPUT_COMPONENTS, writeEntries(entries));
    }

    /**
     * 设置输出条目列表
     */
    public static void setOutputEntries(ItemStack stack, List<WildcardPatternEntry> entries) {
        getOrCreateTag(stack).setTag(KEY_OUTPUT_COMPONENTS, writeEntries(entries));
    }

    /**
     * 获取展开的配方数量
     */
    public static int getExpandedPatternCount(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        return tag == null || !tag.hasKey(KEY_EXPANDED_PATTERN_COUNT) ? 0
            : Math.max(0, tag.getInteger(KEY_EXPANDED_PATTERN_COUNT));
    }

    /**
     * 设置展开的配方数量
     */
    public static void setExpandedPatternCount(ItemStack stack, int count) {
        if (stack == null) return;
        getOrCreateTag(stack).setInteger(KEY_EXPANDED_PATTERN_COUNT, Math.max(0, count));
    }

    public static void applyBitModification(ItemStack stack, int bitMultiplier) {
        if (stack == null || bitMultiplier == 0) {
            return;
        }

        if (WildcardModelState.isNewModel(stack)) {
            applyModelBitModification(stack, bitMultiplier);
            return;
        }

        int factor = 1 << Math.min(30, Math.abs(bitMultiplier));
        List<WildcardPatternEntry> inputs = getInputEntries(stack);
        List<WildcardPatternEntry> outputs = getOutputEntries(stack);
        for (WildcardPatternEntry entry : inputs) {
            applyFactor(entry, factor, bitMultiplier < 0);
        }
        for (WildcardPatternEntry entry : outputs) {
            applyFactor(entry, factor, bitMultiplier < 0);
        }
        setInputEntries(stack, inputs);
        setOutputEntries(stack, outputs);
    }

    public static int getMaxBitMultiplier(ItemStack stack) {
        return getMaxBitModification(stack, false);
    }

    public static int getMaxBitDivider(ItemStack stack) {
        return getMaxBitModification(stack, true);
    }

    /**
     * 导出配置
     */
    public static NBTTagCompound exportConfig(ItemStack stack) {
        initializeFromPattern(stack);
        NBTTagCompound exported = new NBTTagCompound();
        NBTTagCompound source = getOrCreateTag(stack);
        exported.setTag(
            KEY_INPUT_COMPONENTS,
            source.getTagList(KEY_INPUT_COMPONENTS, Constants.NBT.TAG_COMPOUND)
                .copy());
        exported.setTag(
            KEY_OUTPUT_COMPONENTS,
            source.getTagList(KEY_OUTPUT_COMPONENTS, Constants.NBT.TAG_COMPOUND)
                .copy());
        copyIfPresent(source, exported, "WildcardGlobalExcludeMaterials");
        copyIfPresent(source, exported, "WildcardRuleIncludeMaterials");
        copyIfPresent(source, exported, "WildcardRuleExcludeMaterials");
        copyIfPresent(source, exported, "WildcardOreDictPreferences");
        copyIfPresent(source, exported, KEY_EXPANDED_PATTERN_COUNT);
        return exported;
    }

    /**
     * 应用配置
     */
    public static void applyConfig(ItemStack stack, NBTTagCompound config) {
        if (stack == null || config == null) return;
        ensureInitialized(stack);
        NBTTagCompound tag = getOrCreateTag(stack);
        copyIfPresent(config, tag, KEY_INPUT_COMPONENTS);
        copyIfPresent(config, tag, KEY_OUTPUT_COMPONENTS);
        copyIfPresent(config, tag, "WildcardGlobalExcludeMaterials");
        copyIfPresent(config, tag, "WildcardRuleIncludeMaterials");
        copyIfPresent(config, tag, "WildcardRuleExcludeMaterials");
        copyIfPresent(config, tag, "WildcardOreDictPreferences");
        copyIfPresent(config, tag, KEY_EXPANDED_PATTERN_COUNT);
    }

    // ========== 私有方法 ==========

    private static List<WildcardPatternEntry> getEntries(ItemStack stack, String key) {
        initializeFromPattern(stack);
        NBTTagList list = getOrCreateTag(stack).getTagList(key, Constants.NBT.TAG_COMPOUND);
        List<WildcardPatternEntry> result = new ArrayList<>();
        for (int index = 0; index < list.tagCount(); index++) {
            result.add(WildcardPatternEntry.fromNbt(list.getCompoundTagAt(index)));
        }
        return result;
    }

    private static NBTTagList writeEntries(List<WildcardPatternEntry> entries) {
        NBTTagList list = new NBTTagList();
        for (WildcardPatternEntry entry : entries) {
            list.appendTag(entry.toNbt());
        }
        return list;
    }

    private static void applyFactor(WildcardPatternEntry entry, int factor, boolean dividing) {
        if (entry == null || entry.isEmpty()) {
            return;
        }
        if (dividing) {
            entry.divideAmount(factor);
        } else {
            entry.multiplyAmount(factor);
        }
    }

    private static int getMaxBitModification(ItemStack stack, boolean dividing) {
        if (WildcardModelState.isNewModel(stack)) {
            return getModelMaxBitModification(stack, dividing);
        }
        int result = 30;
        boolean found = false;
        for (WildcardPatternEntry entry : getInputEntries(stack)) {
            if (entry != null && !entry.isEmpty()) {
                result = Math.min(result, getMaxBits(entry.getAmountLong(), dividing));
                found = true;
            }
        }
        for (WildcardPatternEntry entry : getOutputEntries(stack)) {
            if (entry != null && !entry.isEmpty()) {
                result = Math.min(result, getMaxBits(entry.getAmountLong(), dividing));
                found = true;
            }
        }
        return found ? result : 0;
    }

    /** Scales the shared component amounts, so every material expansion receives the same batch size. */
    private static void applyModelBitModification(ItemStack stack, int bitMultiplier) {
        boolean dividing = bitMultiplier < 0;
        int bits = Math.min(30, Math.abs(bitMultiplier));
        if (bits > getModelMaxBitModification(stack, dividing)) return;
        long factor = 1L << bits;
        List<IWildcardIOComponent> inputs = WildcardModelState.getInputs(stack);
        List<IWildcardIOComponent> outputs = WildcardModelState.getOutputs(stack);
        for (IWildcardIOComponent component : inputs) scaleComponent(component, factor, dividing);
        for (IWildcardIOComponent component : outputs) scaleComponent(component, factor, dividing);
        WildcardModelState.setInputs(stack, inputs);
        WildcardModelState.setOutputs(stack, outputs);
    }

    private static void scaleComponent(IWildcardIOComponent component, long factor, boolean dividing) {
        if (component == null || component.isEmpty()) return;
        if (component instanceof PrefixIOComponent prefix) {
            prefix.setAmount((int) (dividing ? prefix.getAmount() / factor : prefix.getAmount() * factor));
        } else if (component instanceof FluidIOComponent fluid) {
            fluid.setAmount(dividing ? fluid.getAmount() / factor : fluid.getAmount() * factor);
        } else if (component instanceof SimpleIOComponent simple) {
            simple.setAmount(dividing ? simple.getAmount() / factor : simple.getAmount() * factor);
        }
    }

    private static int getModelMaxBitModification(ItemStack stack, boolean dividing) {
        int result = 30;
        boolean found = false;
        for (IWildcardIOComponent component : WildcardModelState.getInputs(stack)) {
            if (component != null && !component.isEmpty()) {
                result = Math.min(result, getMaxBits(componentAmount(component), dividing));
                found = true;
            }
        }
        for (IWildcardIOComponent component : WildcardModelState.getOutputs(stack)) {
            if (component != null && !component.isEmpty()) {
                result = Math.min(result, getMaxBits(componentAmount(component), dividing));
                found = true;
            }
        }
        return found ? result : 0;
    }

    private static long componentAmount(IWildcardIOComponent component) {
        if (component instanceof PrefixIOComponent prefix) return prefix.getAmount();
        if (component instanceof FluidIOComponent fluid) return fluid.getAmount();
        if (component instanceof SimpleIOComponent simple) return simple.getAmount();
        return WildcardPatternEntry.MAX_AMOUNT;
    }

    private static int getMaxBits(long amount, boolean dividing) {
        long value = Math.max(1L, amount);
        int bits = 0;
        if (dividing) {
            while ((value & 1) == 0) {
                value >>= 1;
                bits++;
            }
        } else {
            while (value > 0 && value <= WildcardPatternEntry.MAX_AMOUNT / 2L) {
                value <<= 1;
                bits++;
            }
        }
        return bits;
    }

    private static void copyIfPresent(NBTTagCompound source, NBTTagCompound target, String key) {
        if (source.hasKey(key)) {
            target.setTag(
                key,
                source.getTag(key)
                    .copy());
        }
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (stack.getTagCompound() == null) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
