package com.xyp.gtnotgood.common.items.wildcard.model;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternEntry;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.FluidIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.SimpleIOComponent;

import gregtech.api.enums.FluidState;
import gregtech.api.enums.OrePrefixes;

/**
 * 旧模型（9 行规则配对 + 字符串过滤）→ 新材料轴模型的一次性迁移。
 * 只保留旧配置的第 1 条规则（模型所限），把它的输入/输出条目映射成新的 IO 组件，
 * 旧的全局排除字符串映射成 string 黑名单过滤。迁移不可逆。
 */
public final class WildcardMigration {

    // 旧 NBT 键
    private static final String OLD_KEY_INPUT = "WildcardInputComponents";
    private static final String OLD_KEY_OUTPUT = "WildcardOutputComponents";
    private static final String OLD_KEY_GLOBAL_EXCLUDE = "WildcardGlobalExcludeMaterials";

    private WildcardMigration() {}

    /** 若物品是旧模型则迁移到新模型；已是新模型则不动。 */
    public static void migrateIfNeeded(ItemStack stack) {
        if (stack == null) return;
        if (WildcardModelState.isNewModel(stack)) return;

        NBTTagCompound tag = stack.getTagCompound();
        boolean hadOldData = tag != null && (tag.hasKey(OLD_KEY_INPUT) || tag.hasKey(OLD_KEY_OUTPUT));

        List<IWildcardIOComponent> inputs = new ArrayList<>();
        List<IWildcardIOComponent> outputs = new ArrayList<>();
        List<IWildcardFilterComponent> filters = new ArrayList<>();

        if (hadOldData) {
            List<WildcardPatternEntry> oldInputs = readOldEntries(tag, OLD_KEY_INPUT);
            List<WildcardPatternEntry> oldOutputs = readOldEntries(tag, OLD_KEY_OUTPUT);
            // 只迁移第 1 条非空规则
            IWildcardIOComponent in = convertEntry(firstNonEmpty(oldInputs));
            IWildcardIOComponent out = convertEntry(firstNonEmpty(oldOutputs));
            if (in != null) inputs.add(in);
            if (out != null) outputs.add(out);

            String globalExclude = tag.hasKey(OLD_KEY_GLOBAL_EXCLUDE) ? tag.getString(OLD_KEY_GLOBAL_EXCLUDE) : "";
            for (String token : splitExclude(globalExclude)) {
                boolean whitelist = token.startsWith("!");
                String value = whitelist ? token.substring(1) : token;
                if (!value.isEmpty()) {
                    filters.add(new StringFilterComponent(value, whitelist));
                }
            }
        }

        // 清掉旧键，写入新模型
        clearOldKeys(stack);
        WildcardModelState.ensureInitialized(stack);
        WildcardModelState.setInputs(stack, inputs);
        WildcardModelState.setOutputs(stack, outputs);
        WildcardModelState.setFilters(stack, filters);
    }

    private static List<WildcardPatternEntry> readOldEntries(NBTTagCompound tag, String key) {
        List<WildcardPatternEntry> result = new ArrayList<>();
        if (tag == null || !tag.hasKey(key)) return result;
        net.minecraft.nbt.NBTTagList list = tag
            .getTagList(key, net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            result.add(WildcardPatternEntry.fromNbt(list.getCompoundTagAt(i)));
        }
        return result;
    }

    private static WildcardPatternEntry firstNonEmpty(List<WildcardPatternEntry> entries) {
        for (WildcardPatternEntry entry : entries) {
            if (entry != null && !entry.isEmpty()) return entry;
        }
        return null;
    }

    /** 旧条目 → 新 IO 组件。流体条目→FluidIOComponent，矿辞条目→PrefixIOComponent，否则→固定物品。 */
    private static IWildcardIOComponent convertEntry(WildcardPatternEntry entry) {
        if (entry == null || entry.isEmpty()) return null;

        int amount = entry.getAmount();
        if (entry.isFluid()) {
            FluidState state = entry.getFluidType();
            return new FluidIOComponent(state == null ? FluidState.MOLTEN : state, Math.max(1L, entry.getAmountLong()));
        }
        if (entry.isOreDict()) {
            OrePrefixes prefix = extractPrefix(entry.getMatcher());
            if (prefix != null) {
                return new PrefixIOComponent(prefix, amount);
            }
        }
        // 回退：把显示物品当固定物品
        ItemStack display = entry.getDisplayStack();
        if (display != null) {
            return new SimpleIOComponent(display, Math.max(1, entry.getAmount()));
        }
        return null;
    }

    /** 从旧 matcher（如 "plate.*"、"ingot*"）里提取 OrePrefix。 */
    private static OrePrefixes extractPrefix(String matcher) {
        if (matcher == null || matcher.isEmpty()) return null;
        String token = matcher.trim();
        int dot = token.indexOf('.');
        if (dot > 0) token = token.substring(0, dot);
        int star = token.indexOf('*');
        if (star >= 0) token = token.substring(0, star);
        token = token.trim();
        return token.isEmpty() ? null : WildcardMaterials.findPrefix(token);
    }

    private static List<String> splitExclude(String value) {
        List<String> result = new ArrayList<>();
        if (value == null) return result;
        for (String token : value.split("[,;，；\\s]+")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private static void clearOldKeys(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) return;
        tag.removeTag(OLD_KEY_INPUT);
        tag.removeTag(OLD_KEY_OUTPUT);
        tag.removeTag(OLD_KEY_GLOBAL_EXCLUDE);
        tag.removeTag("WildcardRuleIncludeMaterials");
        tag.removeTag("WildcardRuleExcludeMaterials");
        tag.removeTag("WildcardOreDictPreferences");
        tag.removeTag("WildcardExpandedPatternCount");
    }
}
