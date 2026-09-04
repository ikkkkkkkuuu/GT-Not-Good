package com.xyp.gtnotgood.common.items.wildcard;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.util.Platform;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;

/**
 * 通配样板符条目
 * 存储单个输入/输出规则的配置信息
 */
public class WildcardPatternEntry {

    private static final String KEY_MODE = "Mode";
    private static final String KEY_FLUID_TYPE = "FluidType";
    private static final String KEY_STACK = "Stack";
    private static final String KEY_DISPLAY = "Display";
    private static final String KEY_MATCHER = "Matcher";
    private static final String KEY_AMOUNT = "Amount";
    public static final long MAX_AMOUNT = 2_100_000_000L;

    static final String FLUID_PREFIX_MOLTEN = "molten.";
    static final String FLUID_PREFIX_PLASMA = "plasma.";
    static final String FLUID_PREFIX_GAS = "gas.";
    static final String FLUID_PREFIX_LIQUID = "liquid.";

    private static final Map<String, Pattern> NAME_PATTERN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Pattern> ORE_PATTERN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> ORE_CANDIDATE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> NAME_CANDIDATE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Materials> MATERIAL_NAME_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ItemStack> DEFAULT_ORE_STACK_CACHE = new ConcurrentHashMap<>();
    private static volatile Set<String> ALL_KNOWN_MATERIAL_NAMES;
    /** Cached OrePrefixes -> display name, resolved once per prefix to avoid per-call reflection in hot loops. */
    private static final Map<OrePrefixes, String> PREFIX_NAME_CACHE = new ConcurrentHashMap<>();

    private boolean oreDictMode;
    private FluidState fluidType;
    private ItemStack stack;
    private ItemStack displayStack;
    private String matcher;
    private long amount;

    /**
     * 从物品堆创建条目
     */
    public static WildcardPatternEntry fromStack(ItemStack stack) {
        WildcardPatternEntry entry = new WildcardPatternEntry();
        entry.stack = stack == null ? null : stack.copy();
        entry.displayStack = stack == null ? null : stack.copy();
        entry.amount = stack == null ? 1L : Math.max(1L, stack.stackSize);
        entry.oreDictMode = false;

        // 检测是否为GT5原生流体展示物品(ItemFluidDisplay) - 新版GTNH用itemDamage编码流体ID
        if (stack != null) {
            FluidStack gtFluidStack = GTUtility.getFluidFromDisplayStack(stack);
            if (gtFluidStack != null && gtFluidStack.getFluid() != null) {
                // GT5的ItemFluidDisplay把数量存在NBT中(可能为0),优先使用NBT数量
                if (gtFluidStack.amount > 0) {
                    entry.amount = gtFluidStack.amount;
                }
                FluidParseResult parsed = parseFluidName(gtFluidStack);
                entry.fluidType = parsed.state;
                if (parsed.materialName != null) {
                    entry.matcher = getFluidPrefixString(parsed.state) + parsed.materialName;
                } else {
                    entry.matcher = gtFluidStack.getLocalizedName();
                }
                entry.oreDictMode = true;
                return entry;
            }
        }

        // 检测是否为旧版AE2FC流体drop(ItemFluidDrop) - 兼容性保留
        // [液滴分类] 可迁原生：仅解析输入堆读出流体信息以填充条目(matcher/amount)，不构造合成请求
        if (stack != null && FluidDropCompat.isFluidDrop(stack.getItem())) {
            FluidStack fluidStack = FluidDropCompat.getFluidStack(stack);
            if (fluidStack != null && fluidStack.getFluid() != null) {
                entry.amount = fluidStack.amount;
                FluidParseResult parsed = parseFluidName(fluidStack);
                entry.fluidType = parsed.state;
                if (parsed.materialName != null) {
                    entry.matcher = getFluidPrefixString(parsed.state) + parsed.materialName;
                } else {
                    entry.matcher = fluidStack.getLocalizedName();
                }
                entry.oreDictMode = true;
                return entry;
            }
        }

        String displayName = safeGetDisplayName(stack);
        entry.matcher = displayName == null ? "" : displayName;
        return entry;
    }

    /**
     * 从AE2样板槽位NBT创建条目
     */
    public static WildcardPatternEntry fromPatternSlot(NBTTagCompound tag) {
        ItemStack stack = tag == null ? null : Platform.loadItemStackFromNBT(tag);
        if (stack != null && stack.stackSize == 0 && tag.hasKey("Cnt")) {
            stack.stackSize = (int) tag.getLong("Cnt");
        }
        return fromStack(stack);
    }

    /**
     * 从NBT标签创建条目
     */
    public static WildcardPatternEntry fromNbt(NBTTagCompound tag) {
        WildcardPatternEntry entry = new WildcardPatternEntry();
        entry.oreDictMode = tag.getBoolean(KEY_MODE);
        if (tag.hasKey(KEY_FLUID_TYPE)) {
            try {
                entry.fluidType = FluidState.valueOf(tag.getString(KEY_FLUID_TYPE));
            } catch (IllegalArgumentException ignored) {
                // 兼容旧格式(整数索引)
                entry.fluidType = FluidState.fromValue(tag.getInteger(KEY_FLUID_TYPE));
            }
        }
        entry.matcher = tag.hasKey(KEY_MATCHER) ? tag.getString(KEY_MATCHER) : tag.getString("Prefix");
        entry.amount = Math.max(1L, tag.getLong(KEY_AMOUNT));
        if (tag.hasKey(KEY_STACK)) {
            entry.stack = ItemStack.loadItemStackFromNBT(tag.getCompoundTag(KEY_STACK));
        }
        if (tag.hasKey(KEY_DISPLAY)) {
            entry.displayStack = ItemStack.loadItemStackFromNBT(tag.getCompoundTag(KEY_DISPLAY));
        }
        if (entry.displayStack == null && entry.stack != null) {
            entry.displayStack = entry.stack.copy();
        }
        if ((entry.matcher == null || entry.matcher.isEmpty()) && entry.stack != null) {
            String displayName = safeGetDisplayName(entry.stack);
            entry.matcher = displayName == null ? "" : displayName;
        }
        return entry;
    }

    /**
     * 转换为NBT标签
     */
    public NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(KEY_MODE, this.oreDictMode);
        if (this.fluidType != null) {
            tag.setString(KEY_FLUID_TYPE, this.fluidType.name());
        }
        tag.setString(KEY_MATCHER, getMatcher());
        tag.setLong(KEY_AMOUNT, getAmountLong());
        if (this.stack != null) {
            NBTTagCompound stackTag = new NBTTagCompound();
            this.stack.writeToNBT(stackTag);
            tag.setTag(KEY_STACK, stackTag);
        }
        if (this.displayStack != null) {
            NBTTagCompound displayTag = new NBTTagCompound();
            this.displayStack.writeToNBT(displayTag);
            tag.setTag(KEY_DISPLAY, displayTag);
        }
        return tag;
    }

    /**
     * 根据材料名创建物品堆
     */
    public ItemStack createStack(String materialName, ItemStack configStack) {
        if (isEmpty()) {
            return null;
        }
        // 流体模式: 创建流体drop
        if (this.fluidType != null) {
            return createFluidStack(materialName, configStack);
        }
        if (this.oreDictMode) {
            return createOreDictStack(materialName, configStack);
        }
        return createNameMatchedStack(materialName, configStack);
    }

    /**
     * 获取候选材料列表
     */
    public Set<String> getCandidateMaterials() {
        // 流体模式: 从Materials收集具有对应流体的材料
        if (isFluid()) {
            String fluidMatcher = normalizeOreMatcher(this.matcher);
            if (fluidMatcher.isEmpty() || isMatchAllPattern(fluidMatcher)) {
                return new LinkedHashSet<>();
            }
            return collectFluidCandidateMaterials(fluidMatcher);
        }
        if (this.oreDictMode) {
            String oreMatcher = normalizeOreMatcher(this.matcher);
            if (oreMatcher.isEmpty() || isMatchAllPattern(oreMatcher)) {
                return new LinkedHashSet<>();
            }
            return new LinkedHashSet<>(
                ORE_CANDIDATE_CACHE
                    .computeIfAbsent(oreMatcher, WildcardPatternEntry::collectOreDictCandidateMaterials));
        }

        String nameMatcher = getMatcher();
        if (nameMatcher.isEmpty() || isMatchAllPattern(nameMatcher)) {
            return new LinkedHashSet<>();
        }
        if (!containsWildcard(nameMatcher) && !containsRegexMeta(nameMatcher)) {
            return getDirectCandidateMaterials();
        }
        return new LinkedHashSet<>(
            NAME_CANDIDATE_CACHE.computeIfAbsent(nameMatcher, WildcardPatternEntry::collectNameCandidateMaterials));
    }

    /**
     * 判断是否可以转换为矿辞模式
     */
    public boolean canOreDict() {
        return isFluid() || getDisplayPrefix() != null;
    }

    /**
     * 转换为矿辞模式
     */
    public void convertToOreDict() {
        this.oreDictMode = true;
        if (isFluid()) {
            String prefix = getFluidPrefixString(this.fluidType);
            this.matcher = prefix + "*";
            return;
        }
        // 检查matcher文本是否为流体前缀模式
        FluidState detected = detectFluidFromPrefix(getMatcher());
        if (detected != null) {
            this.fluidType = detected;
            this.matcher = getFluidPrefixString(detected) + "*";
            return;
        }
        OrePrefixes prefix = getDisplayPrefix();
        if (prefix != null) {
            this.matcher = getPrefixName(prefix) + "*";
        } else if (this.matcher == null || this.matcher.trim()
            .isEmpty()) {
                this.matcher = "*";
            }
    }

    /**
     * 转换为物品模式
     */
    public void convertToItem() {
        this.oreDictMode = false;
        this.fluidType = null;
        if (this.displayStack != null) {
            String displayName = safeGetDisplayName(this.displayStack);
            this.matcher = displayName == null ? "" : displayName;
        }
    }

    /**
     * 获取显示物品堆
     */
    public ItemStack getDisplayStack() {
        // 流体模式: 返回GT5原生流体展示物品(新版流体图标)
        if (isFluid()) {
            ItemStack stored = this.displayStack != null ? this.displayStack : this.stack;
            if (stored != null) {
                // [液滴分类] 可迁原生：仅取出流体转成 GT 显示物品用于 GUI 图标渲染，无合成计算
                FluidStack fluid = FluidDropCompat.getFluidStack(stored);
                if (fluid != null) {
                    ItemStack display = GTUtility.getFluidDisplayStack(fluid, true);
                    if (display != null) {
                        display.stackSize = 1;
                        return display;
                    }
                }
                return stored.copy();
            }
            return null;
        }
        if (this.displayStack != null) {
            ItemStack copy = this.displayStack.copy();
            copy.stackSize = getClampedAmount();
            return copy;
        }
        if (this.stack != null) {
            ItemStack copy = this.stack.copy();
            copy.stackSize = getClampedAmount();
            return copy;
        }
        return null;
    }

    /**
     * 获取标签文本
     */
    public String getLabel() {
        return getMatcher();
    }

    /**
     * 是否为矿辞模式
     */
    public boolean isOreDict() {
        return this.oreDictMode;
    }

    /**
     * 是否为流体模式
     */
    public boolean isFluid() {
        return this.fluidType != null;
    }

    /**
     * 获取流体类型
     */
    public FluidState getFluidType() {
        return this.fluidType;
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return getMatcher().isEmpty() && this.stack == null && this.displayStack == null;
    }

    /**
     * 获取匹配器字符串
     */
    public String getMatcher() {
        return this.matcher == null ? "" : this.matcher.trim();
    }

    /**
     * 设置匹配器字符串
     */
    public void setMatcher(String matcher) {
        this.matcher = matcher == null ? "" : matcher.trim();
    }

    /**
     * 设置矿辞名称或前缀
     */
    public void setOreNameOrPrefix(String oreNameOrPrefix) {
        setMatcher(oreNameOrPrefix);
        // 检测流体前缀并设置fluidType
        FluidState detected = detectFluidFromPrefix(oreNameOrPrefix);
        if (detected != null) {
            this.fluidType = detected;
        }
    }

    /**
     * 获取数量
     */
    public int getAmount() {
        return getClampedAmount();
    }

    /**
     * 获取数量(long)
     */
    public long getAmountLong() {
        return Math.max(1L, Math.min(MAX_AMOUNT, this.amount));
    }

    /**
     * 设置数量
     */
    public void setAmount(int amount) {
        setAmount((long) amount);
    }

    /**
     * 设置数量(long)
     */
    public void setAmount(long amount) {
        this.amount = Math.max(1L, Math.min(MAX_AMOUNT, amount));
    }

    /**
     * 乘以倍数
     */
    public void multiplyAmount(int factor) {
        if (factor <= 1) {
            return;
        }
        long current = getAmountLong();
        if (current > MAX_AMOUNT / factor) {
            this.amount = MAX_AMOUNT;
            return;
        }
        setAmount(current * factor);
    }

    /**
     * 除以倍数
     */
    public void divideAmount(int divisor) {
        if (divisor <= 1) {
            return;
        }
        this.amount = Math.max(1L, getAmountLong() / divisor);
    }

    /**
     * 设置物品堆
     */
    public void setStack(ItemStack stack) {
        this.stack = stack == null ? null : stack.copy();
        this.displayStack = this.stack == null ? null : this.stack.copy();
        if (this.stack != null && !this.oreDictMode) {
            String displayName = safeGetDisplayName(this.stack);
            this.matcher = displayName == null ? "" : displayName;
        }
    }

    /**
     * 获取物品堆
     */
    public ItemStack getStack() {
        return this.stack == null ? null : this.stack.copy();
    }

    // ========== 私有方法 ==========

    private Set<String> getDirectCandidateMaterials() {
        Set<String> result = new LinkedHashSet<>();
        ItemStack source = this.displayStack != null ? this.displayStack : this.stack;
        if (source == null) {
            return result;
        }
        ItemData association = GTOreDictUnificator.getAssociation(source);
        if (association != null && association.hasValidPrefixMaterialData()) {
            Materials mat = association.mMaterial.mMaterial;
            if (isRealMaterial(mat) && mat.mName != null && !mat.mName.isEmpty()) {
                result.add(mat.mName);
                return result;
            }
        }
        int[] oreIds = OreDictionary.getOreIDs(source);
        if (oreIds != null) {
            for (int oreId : oreIds) {
                String materialName = extractMaterialNameFromOreName(OreDictionary.getOreName(oreId));
                if (!materialName.isEmpty()) {
                    result.add(materialName);
                }
            }
        }
        return result;
    }

    private ItemStack createOreDictStack(String materialName, ItemStack configStack) {
        String matcherValue = normalizeOreMatcher(this.matcher);
        if (matcherValue.isEmpty() || isMatchAllPattern(matcherValue)
            || materialName == null
            || materialName.trim()
                .isEmpty()) {
            return null;
        }

        Materials material = findMaterialByName(materialName);
        String token = normalizeOreToken(matcherValue);
        OrePrefixes exactPrefix = findPrefix(token);
        if (exactPrefix != null) {
            return createPreferredOreVariant(configStack, exactPrefix, materialName, material);
        }

        OreMatch bestMatch = null;
        for (OrePrefixes prefix : OrePrefixes.VALUES) {
            if (prefix.mPrefixedItems == null || prefix.mPrefixedItems.isEmpty()) continue;
            String oreName = getOreName(prefix, materialName);
            if (oreName.isEmpty() || !matchesOreName(oreName, matcherValue)) {
                continue;
            }
            OreMatch current = new OreMatch(prefix, oreName);
            if (bestMatch == null || current.compareTo(bestMatch) < 0) {
                bestMatch = current;
            }
        }
        if (bestMatch == null) {
            return null;
        }

        return createPreferredOreVariant(configStack, bestMatch.prefix, materialName, material);
    }

    private ItemStack createNameMatchedStack(String materialName, ItemStack configStack) {
        if (materialName == null || materialName.trim()
            .isEmpty()) {
            return null;
        }

        if (getDisplayPrefix() == null) {
            ItemStack source = this.displayStack != null ? this.displayStack : this.stack;
            if (source != null && matchesName(safeGetDisplayName(source), this.matcher)) {
                ItemStack copy = source.copy();
                copy.stackSize = getClampedAmount();
                return copy;
            }
            return null;
        }

        ItemStack preferred = tryCreatePreferredStack(materialName, configStack);
        if (preferred != null && matchesName(safeGetDisplayName(preferred), this.matcher)) {
            preferred.stackSize = getClampedAmount();
            return preferred;
        }

        ItemStack source = this.stack != null ? this.stack : this.displayStack;
        String sourceMaterial = getAssociatedMaterialName(source);
        if (source != null && !sourceMaterial.isEmpty()
            && sourceMaterial.equalsIgnoreCase(materialName)
            && matchesName(safeGetDisplayName(source), this.matcher)) {
            ItemStack copy = source.copy();
            copy.stackSize = getClampedAmount();
            return copy;
        }

        Materials material = findMaterialByName(materialName);
        for (OrePrefixes prefix : OrePrefixes.VALUES) {
            if (prefix.mPrefixedItems == null || prefix.mPrefixedItems.isEmpty()) continue;
            ItemStack candidate = getPreferredOreStack(configStack, prefix, materialName);
            if (candidate == null && isRealMaterial(material)) {
                candidate = GTOreDictUnificator.get(prefix, material, getClampedAmount());
            }
            if (candidate != null && matchesName(safeGetDisplayName(candidate), this.matcher)) {
                candidate.stackSize = getClampedAmount();
                return candidate;
            }
        }

        for (OrePrefixes prefix : OrePrefixes.VALUES) {
            if (prefix.mPrefixedItems == null || prefix.mPrefixedItems.isEmpty()) continue;
            String oreName = getOreName(prefix, materialName);
            if (oreName.isEmpty()) continue;
            ArrayList<ItemStack> options = OreDictionary.getOres(oreName);
            if (options == null || options.isEmpty()) continue;
            for (ItemStack option : options) {
                if (option == null || option.getItem() == null
                    || !matchesName(safeGetDisplayName(option), this.matcher)) {
                    continue;
                }
                ItemStack copy = option.copy();
                copy.stackSize = getClampedAmount();
                return copy;
            }
        }
        return null;
    }

    private OrePrefixes getDisplayPrefix() {
        ItemData association = getDisplayAssociation();
        if (association != null && association.hasValidPrefixMaterialData()) {
            return association.mPrefix;
        }
        return extractPrefixFromOreName(getDisplayOreName());
    }

    private ItemData getDisplayAssociation() {
        ItemStack target = getDisplayStack();
        return target == null ? null : GTOreDictUnificator.getAssociation(target);
    }

    private String getDisplayOreName() {
        ItemStack target = this.displayStack != null ? this.displayStack : this.stack;
        return getBestOreName(target);
    }

    private ItemStack tryCreatePreferredStack(String materialName, ItemStack configStack) {
        OrePrefixes prefix = getDisplayPrefix();
        return prefix == null ? null
            : createPreferredOreVariant(configStack, prefix, materialName, findMaterialByName(materialName));
    }

    private ItemStack createPreferredOreVariant(ItemStack configStack, OrePrefixes prefix, String materialName,
        Materials material) {
        ItemStack preferred = getPreferredOreStack(configStack, prefix, materialName);
        if (preferred != null) {
            preferred.stackSize = getClampedAmount();
            return preferred;
        }
        if (isRealMaterial(material)) {
            ItemStack unified = GTOreDictUnificator.get(prefix, material, getClampedAmount());
            if (unified != null) {
                unified.stackSize = getClampedAmount();
                return unified;
            }
        }
        String oreName = getOreName(prefix, materialName);
        ItemStack fallback = getDefaultPreferredOreStack(oreName);
        if (fallback != null) {
            fallback.stackSize = getClampedAmount();
        }
        return fallback;
    }

    private ItemStack getPreferredOreStack(ItemStack configStack, OrePrefixes prefix, String materialName) {
        String oreName = getOreName(prefix, materialName);
        if (oreName.isEmpty()) return null;

        ItemStack preferred = getDefaultPreferredOreStack(oreName);
        if (preferred == null) return null;

        ArrayList<ItemStack> options = OreDictionary.getOres(oreName);
        boolean matched = false;
        for (ItemStack option : options) {
            if (OreDictionary.itemMatches(option, preferred, false)) {
                matched = true;
                break;
            }
        }
        if (!matched) return null;

        ItemStack copy = preferred.copy();
        copy.stackSize = getClampedAmount();
        return copy;
    }

    private int getClampedAmount() {
        return (int) Math.max(1L, Math.min((long) Integer.MAX_VALUE, getAmountLong()));
    }

    private static ItemStack getDefaultPreferredOreStack(String oreName) {
        ItemStack cached = DEFAULT_ORE_STACK_CACHE.get(oreName);
        if (cached != null) {
            return cached == null ? null : cached.copy();
        }
        ItemStack result = computeDefaultPreferredOreStack(oreName);
        // ConcurrentHashMap不允许null值,只在result不为null时缓存
        if (result != null) {
            DEFAULT_ORE_STACK_CACHE.put(oreName, result);
        }
        return result;
    }

    private static ItemStack computeDefaultPreferredOreStack(String oreName) {
        ArrayList<ItemStack> options = OreDictionary.getOres(oreName);
        if (options == null || options.isEmpty()) {
            return null;
        }
        for (ItemStack option : options) {
            if (option == null || option.getItem() == null) continue;
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(option.getItem());
            if (id != null && "gregtech".equalsIgnoreCase(id.modId)) {
                return option.copy();
            }
        }
        return options.get(0) == null ? null
            : options.get(0)
                .copy();
    }

    private static Set<String> collectOreDictCandidateMaterials(String matcher) {
        String normalized = normalizeOreMatcher(matcher);
        if (normalized.isEmpty()) return new LinkedHashSet<>();

        if (!containsWildcard(normalized)) {
            return collectExactOreDictCandidateMaterials(normalized);
        }

        if (isSimplePrefixWildcard(normalized)) {
            return collectPrefixOreDictCandidateMaterials(normalizeOreToken(normalized));
        }

        Set<String> result = new LinkedHashSet<>();
        for (String oreName : OreDictionary.getOreNames()) {
            if (oreName == null || oreName.isEmpty() || !matchesOreName(oreName, normalized)) continue;
            collectMaterialNamesForOreName(result, oreName);
        }
        return result;
    }

    private static Set<String> collectExactOreDictCandidateMaterials(String oreName) {
        Set<String> result = new LinkedHashSet<>();
        if (oreName == null || oreName.isEmpty()) return result;
        for (String actualOreName : OreDictionary.getOreNames()) {
            if (actualOreName != null && actualOreName.equalsIgnoreCase(oreName)) {
                collectMaterialNamesForOreName(result, actualOreName);
            }
        }
        return result;
    }

    private static Set<String> collectPrefixOreDictCandidateMaterials(String prefixToken) {
        Set<String> result = new LinkedHashSet<>();
        OrePrefixes prefix = findPrefix(prefixToken);
        if (prefix == null) return result;

        String prefixName = getPrefixName(prefix);
        for (String oreName : OreDictionary.getOreNames()) {
            if (oreName == null || oreName.isEmpty()) continue;
            if (!oreName.regionMatches(true, 0, prefixName, 0, prefixName.length())) continue;
            String materialName = extractMaterialNameFromOreName(oreName);
            if (!materialName.isEmpty()) {
                result.add(materialName);
            }
        }
        return result;
    }

    private static void collectMaterialNamesForOreName(Set<String> result, String oreName) {
        if (result == null || oreName == null || oreName.isEmpty()) return;
        String parsed = extractMaterialNameFromOreName(oreName);
        if (!parsed.isEmpty()) {
            result.add(parsed);
        }

        ArrayList<ItemStack> options = OreDictionary.getOres(oreName);
        if (options == null) return;
        for (ItemStack option : options) {
            String associated = getAssociatedMaterialName(option);
            if (!associated.isEmpty()) {
                result.add(associated);
            }
        }
    }

    private static Set<String> collectNameCandidateMaterials(String matcher) {
        Set<String> result = new LinkedHashSet<>();
        for (String oreName : OreDictionary.getOreNames()) {
            ArrayList<ItemStack> options = OreDictionary.getOres(oreName);
            if (options == null) continue;
            String materialName = extractMaterialNameFromOreName(oreName);
            for (ItemStack option : options) {
                String displayName = safeGetDisplayName(option);
                if (displayName == null || !matchesName(displayName, matcher)) continue;
                if (!materialName.isEmpty()) {
                    result.add(materialName);
                }
                String associated = getAssociatedMaterialName(option);
                if (!associated.isEmpty()) {
                    result.add(associated);
                }
                break;
            }
        }
        return result;
    }

    private static boolean isRealMaterial(Materials material) {
        return material != null && material != Materials._NULL && material != Materials.Empty && material.mUnifiable;
    }

    private static String normalizeOreToken(String value) {
        String token = value == null ? "" : value.trim();
        if (token.endsWith("*")) {
            token = token.substring(0, token.length() - 1)
                .trim();
        }
        return token;
    }

    private static String normalizeOreMatcher(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean matchesName(String displayName, String wildcardPattern) {
        if (displayName == null) return false;
        String pattern = wildcardPattern == null ? "" : wildcardPattern.trim();
        if (pattern.isEmpty() || isMatchAllPattern(pattern)) return false;
        if (!containsWildcard(pattern) && !containsRegexMeta(pattern)) {
            return displayName.equalsIgnoreCase(pattern);
        }
        Pattern compiled = NAME_PATTERN_CACHE.computeIfAbsent(pattern, WildcardPatternEntry::compileNamePattern);
        return compiled != null && compiled.matcher(displayName)
            .find();
    }

    private static boolean matchesOreName(String oreName, String pattern) {
        if (oreName == null) return false;
        String normalizedOre = oreName.trim();
        String normalizedPattern = pattern == null ? "" : pattern.trim();
        if (normalizedPattern.isEmpty() || isMatchAllPattern(normalizedPattern)) return false;
        if (!containsWildcard(normalizedPattern)) {
            return normalizedOre.equalsIgnoreCase(normalizedPattern);
        }
        Pattern compiled = ORE_PATTERN_CACHE
            .computeIfAbsent(normalizedPattern, WildcardPatternEntry::compileOrePattern);
        return compiled != null && compiled.matcher(normalizedOre)
            .matches();
    }

    private static Pattern compileNamePattern(String pattern) {
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        try {
            if (containsWildcard(pattern)) {
                return Pattern.compile(wildcardToRegex(pattern), flags);
            }
            if (containsRegexMeta(pattern)) {
                return Pattern.compile(pattern, flags);
            }
            return Pattern.compile(Pattern.quote(pattern), flags);
        } catch (PatternSyntaxException ignored) {
            try {
                return Pattern.compile(wildcardToRegex(pattern), flags);
            } catch (PatternSyntaxException ignoredAgain) {
                return null;
            }
        }
    }

    private static Pattern compileOrePattern(String pattern) {
        try {
            return Pattern.compile(wildcardToRegex(pattern), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } catch (PatternSyntaxException ignored) {
            return null;
        }
    }

    private static boolean containsWildcard(String pattern) {
        return pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0;
    }

    private static boolean isSimplePrefixWildcard(String pattern) {
        if (pattern == null || !pattern.endsWith("*") || pattern.indexOf('?') >= 0) return false;
        return pattern.indexOf('*') == pattern.length() - 1;
    }

    private static boolean isMatchAllPattern(String pattern) {
        if (pattern == null) return false;
        String trimmed = pattern.trim();
        if (trimmed.isEmpty()) return false;
        for (int index = 0; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (current != '*' && current != '?') return false;
        }
        return true;
    }

    private static boolean containsRegexMeta(String pattern) {
        for (int index = 0; index < pattern.length(); index++) {
            if ("\\.^$|()[]{}+".indexOf(pattern.charAt(index)) >= 0) return true;
        }
        return false;
    }

    private static String wildcardToRegex(String pattern) {
        StringBuilder builder = new StringBuilder("^");
        for (int index = 0; index < pattern.length(); index++) {
            char value = pattern.charAt(index);
            if (value == '*') {
                builder.append(".*");
            } else if (value == '?') {
                builder.append('.');
            } else {
                builder.append(Pattern.quote(String.valueOf(value)));
            }
        }
        builder.append('$');
        return builder.toString();
    }

    private static String getPrefixName(OrePrefixes prefix) {
        if (prefix == null) return "";
        // Reflection resolved once per prefix and cached; called inside OrePrefixes.VALUES / oreName loops.
        return PREFIX_NAME_CACHE.computeIfAbsent(prefix, WildcardPatternEntry::resolvePrefixName);
    }

    private static String resolvePrefixName(OrePrefixes prefix) {
        try {
            return (String) prefix.getClass()
                .getMethod("getName")
                .invoke(prefix);
        } catch (Exception ignored) {}
        try {
            return (String) prefix.getClass()
                .getMethod("name")
                .invoke(prefix);
        } catch (Exception ignored) {}
        return prefix.toString();
    }

    private static OrePrefixes findPrefix(String token) {
        OrePrefixes exact = OrePrefixes.getPrefix(token);
        if (exact != null) return exact;
        if (token == null || token.isEmpty()) return null;
        for (OrePrefixes prefix : OrePrefixes.VALUES) {
            String prefixName = getPrefixName(prefix);
            if (!prefixName.isEmpty() && prefixName.equalsIgnoreCase(token)) {
                return prefix;
            }
        }
        return null;
    }

    private static String getOreName(OrePrefixes prefix, String materialName) {
        String prefixName = getPrefixName(prefix);
        String name = materialName == null ? "" : materialName.trim();
        return prefixName.isEmpty() || name.isEmpty() ? "" : prefixName + name;
    }

    private static String getAssociatedMaterialName(ItemStack stack) {
        Materials associated = getAssociatedMaterial(stack);
        if (isRealMaterial(associated) && associated.mName != null && !associated.mName.isEmpty()) {
            return associated.mName;
        }

        int[] oreIds = stack == null ? null : OreDictionary.getOreIDs(stack);
        if (oreIds == null || oreIds.length == 0) return "";
        for (int oreId : oreIds) {
            String candidate = extractMaterialNameFromOreName(OreDictionary.getOreName(oreId));
            if (!candidate.isEmpty()) return candidate;
        }
        return "";
    }

    private static Materials getAssociatedMaterial(ItemStack stack) {
        if (stack == null) return null;
        ItemData association = GTOreDictUnificator.getAssociation(stack);
        return association != null && association.hasValidPrefixMaterialData() ? association.mMaterial.mMaterial : null;
    }

    private static String safeGetDisplayName(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            return stack.getDisplayName();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String getBestOreName(ItemStack stack) {
        if (stack == null) return null;
        int[] oreIds = OreDictionary.getOreIDs(stack);
        if (oreIds == null || oreIds.length == 0) return null;

        String first = null;
        String best = null;
        int bestScore = Integer.MAX_VALUE;
        int bestLength = Integer.MAX_VALUE;
        for (int oreId : oreIds) {
            String oreName = OreDictionary.getOreName(oreId);
            if (oreName == null || oreName.isEmpty()) continue;
            if (first == null) first = oreName;
            OrePrefixes prefix = extractPrefixFromOreName(oreName);
            if (prefix == null) continue;
            int score = OreMatch.computeScore(prefix, oreName);
            if (best == null || score < bestScore || (score == bestScore && oreName.length() < bestLength)) {
                best = oreName;
                bestScore = score;
                bestLength = oreName.length();
            }
        }
        return best != null ? best : first;
    }

    private static OrePrefixes extractPrefixFromOreName(String oreName) {
        if (oreName == null || oreName.isEmpty()) return null;
        OrePrefixes bestMatch = null;
        int bestPrefixLength = -1;
        for (OrePrefixes prefix : OrePrefixes.VALUES) {
            String prefixName = getPrefixName(prefix);
            if (prefixName.isEmpty() || !oreName.regionMatches(true, 0, prefixName, 0, prefixName.length())) continue;
            if (prefixName.length() > bestPrefixLength) {
                bestMatch = prefix;
                bestPrefixLength = prefixName.length();
            }
        }
        return bestMatch;
    }

    private static String extractMaterialNameFromOreName(String oreName) {
        OrePrefixes prefix = extractPrefixFromOreName(oreName);
        if (prefix == null) return "";
        String prefixName = getPrefixName(prefix);
        if (prefixName.isEmpty() || oreName == null || oreName.length() <= prefixName.length()) {
            return "";
        }
        return oreName.substring(prefixName.length())
            .trim();
    }

    /**
     * 判断给定的文本是否看起来像矿辞模式或流体模式
     */
    public static boolean looksLikeOreDictPattern(String text) {
        if (text == null || text.isEmpty()) return false;
        // 检查流体前缀
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith(FLUID_PREFIX_MOLTEN) || lower.startsWith(FLUID_PREFIX_PLASMA)
            || lower.startsWith(FLUID_PREFIX_GAS)
            || lower.startsWith(FLUID_PREFIX_LIQUID)) {
            return true;
        }
        // 检查是否包含典型的矿辞前缀关键词
        return lower.contains("ore") || lower.contains("ingot")
            || lower.contains("plate")
            || lower.contains("dust")
            || lower.contains("gear")
            || lower.contains("block")
            || lower.startsWith("ore")
            || lower.startsWith("ingot")
            || lower.startsWith("plate");
    }

    private static Materials findMaterialByName(String name) {
        if (name == null || name.isEmpty()) return Materials._NULL;
        String normalized = name.toLowerCase(Locale.ROOT);
        Materials cached = MATERIAL_NAME_CACHE.get(normalized);
        if (cached != null) return cached;
        for (Materials material : Materials.getAll()) {
            if (material == null || material == Materials._NULL
                || material == Materials.Empty
                || material.mName == null) continue;
            MATERIAL_NAME_CACHE.putIfAbsent(material.mName.toLowerCase(Locale.ROOT), material);
        }
        Materials material = MATERIAL_NAME_CACHE.get(normalized);
        return material == null ? Materials._NULL : material;
    }

    /**
     * 获取所有已知材料名称
     */
    public static Set<String> getAllKnownMaterialNames() {
        Set<String> built = new LinkedHashSet<>();
        for (Materials material : Materials.getAll()) {
            if (isRealMaterial(material) && material.mName != null && !material.mName.isEmpty()) {
                built.add(material.mName);
            }
        }
        for (String oreName : OreDictionary.getOreNames()) {
            String materialName = extractMaterialNameFromOreName(oreName);
            if (!materialName.isEmpty()) {
                built.add(materialName);
            }
        }
        return new LinkedHashSet<>(built);
    }

    /**
     * 收集具有指定流体类型的材料名称(用于流体模式的候选材料)
     */
    private Set<String> collectFluidCandidateMaterials(String matcher) {
        Set<String> result = new LinkedHashSet<>();
        String token = normalizeOreToken(matcher);
        if (token.isEmpty()) return result;

        boolean isWildcard = containsWildcard(matcher) && isSimplePrefixWildcard(matcher);

        for (Materials material : Materials.getAll()) {
            if (!isRealMaterial(material) || material.mName == null || material.mName.isEmpty()) {
                continue;
            }
            if (!hasMatchingFluid(material, this.fluidType)) {
                continue;
            }
            if (isWildcard) {
                result.add(material.mName);
            } else {
                // 精确匹配：fluidPrefix + materialName
                String fullPattern = getFluidPrefixString(this.fluidType) + material.mName;
                if (fullPattern.equalsIgnoreCase(matcher)) {
                    result.add(material.mName);
                }
            }
        }
        return result;
    }

    /**
     * 检查材料是否具有指定类型的流体
     */
    private static boolean hasMatchingFluid(Materials material, FluidState state) {
        if (material == null || state == null) return false;
        switch (state) {
            case MOLTEN:
                return material.mStandardMoltenFluid != null;
            case PLASMA:
                return material.mPlasma != null;
            case GAS:
                return material.mGas != null;
            case LIQUID:
            default:
                return material.mFluid != null;
        }
    }

    // ========== 流体相关方法 ==========

    /**
     * 根据材料名和流体类型创建流体drop
     */
    private ItemStack createFluidStack(String materialName, ItemStack configStack) {
        if (materialName == null || materialName.trim()
            .isEmpty() || this.fluidType == null) {
            return null;
        }

        Materials material = findMaterialByName(materialName);
        if (!isRealMaterial(material)) {
            return null;
        }

        FluidStack fluidStack = getMaterialFluid(material, this.fluidType, getAmountLong());
        if (fluidStack == null) {
            return null;
        }

        // 使用AE2FC的ItemFluidDrop,CPU通过instanceof ItemFluidDrop识别流体并请求AE流体存储
        // [液滴分类] 必须留液滴：产出参与展开样板/构造合成请求，CPU 靠 ItemFluidDrop 识别流体下单
        return FluidDropCompat.newStack(fluidStack);
    }

    /**
     * 根据流体类型获取材料对应的FluidStack
     */
    private static FluidStack getMaterialFluid(Materials material, FluidState state, long amount) {
        if (material == null || state == null) return null;
        switch (state) {
            case MOLTEN:
                return material.getMolten(amount);
            case PLASMA:
                return material.getPlasma(amount);
            case GAS:
                return material.getGas(amount);
            case LIQUID:
            default:
                return material.getFluid(amount);
        }
    }

    /**
     * 解析流体名,提取材料和流体状态
     */
    private static FluidParseResult parseFluidName(FluidStack fluidStack) {
        if (fluidStack == null || fluidStack.getFluid() == null) {
            return new FluidParseResult(null, null);
        }
        String fluidName = fluidStack.getFluid()
            .getName();
        if (fluidName == null || fluidName.isEmpty()) {
            return new FluidParseResult(null, null);
        }

        String lower = fluidName.toLowerCase(Locale.ROOT);

        // 检查molten.前缀
        if (lower.startsWith(FLUID_PREFIX_MOLTEN)) {
            String materialPart = fluidName.substring(FLUID_PREFIX_MOLTEN.length());
            return new FluidParseResult(FluidState.MOLTEN, materialPart);
        }
        // 检查plasma.前缀
        if (lower.startsWith(FLUID_PREFIX_PLASMA)) {
            String materialPart = fluidName.substring(FLUID_PREFIX_PLASMA.length());
            return new FluidParseResult(FluidState.PLASMA, materialPart);
        }
        // 检查gas.前缀
        if (lower.startsWith(FLUID_PREFIX_GAS)) {
            String materialPart = fluidName.substring(FLUID_PREFIX_GAS.length());
            return new FluidParseResult(FluidState.GAS, materialPart);
        }
        // 检查liquid.前缀
        if (lower.startsWith(FLUID_PREFIX_LIQUID)) {
            String materialPart = fluidName.substring(FLUID_PREFIX_LIQUID.length());
            return new FluidParseResult(FluidState.LIQUID, materialPart);
        }

        // 没有前缀,尝试通过遍历Materials匹配流体
        for (Materials material : Materials.getAll()) {
            if (!isRealMaterial(material)) continue;
            // 检查各流体类型
            if (checkFluidMatch(material, fluidStack)) {
                String matName = material.mName;
                // 判断是哪种流体状态
                Fluid stateFluid = material.mFluid;
                Fluid gasFluid = material.mGas;
                Fluid plasmaFluid = material.mPlasma;
                Fluid moltenFluid = material.mStandardMoltenFluid;
                Fluid stackFluid = fluidStack.getFluid();

                if (moltenFluid != null && moltenFluid == stackFluid) {
                    return new FluidParseResult(FluidState.MOLTEN, matName);
                }
                if (plasmaFluid != null && plasmaFluid == stackFluid) {
                    return new FluidParseResult(FluidState.PLASMA, matName);
                }
                if (gasFluid != null && gasFluid == stackFluid) {
                    return new FluidParseResult(FluidState.GAS, matName);
                }
                if (stateFluid != null && stateFluid == stackFluid) {
                    return new FluidParseResult(FluidState.LIQUID, matName);
                }
                return new FluidParseResult(FluidState.LIQUID, matName);
            }
        }

        return new FluidParseResult(null, null);
    }

    /**
     * 检查材料的流体是否匹配给定的FluidStack
     */
    private static boolean checkFluidMatch(Materials material, FluidStack fluidStack) {
        Fluid target = fluidStack.getFluid();
        return (material.mFluid != null && material.mFluid == target)
            || (material.mGas != null && material.mGas == target)
            || (material.mPlasma != null && material.mPlasma == target)
            || (material.mStandardMoltenFluid != null && material.mStandardMoltenFluid == target);
    }

    /**
     * 获取流体类型对应的前缀字符串
     */
    private static String getFluidPrefixString(FluidState state) {
        if (state == null) return "";
        switch (state) {
            case MOLTEN:
                return FLUID_PREFIX_MOLTEN;
            case PLASMA:
                return FLUID_PREFIX_PLASMA;
            case GAS:
                return FLUID_PREFIX_GAS;
            case LIQUID:
                return FLUID_PREFIX_LIQUID;
            default:
                return "";
        }
    }

    /**
     * 从文本中检测流体前缀并返回对应的FluidState
     */
    private static FluidState detectFluidFromPrefix(String text) {
        if (text == null || text.isEmpty()) return null;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith(FLUID_PREFIX_MOLTEN)) return FluidState.MOLTEN;
        if (lower.startsWith(FLUID_PREFIX_PLASMA)) return FluidState.PLASMA;
        if (lower.startsWith(FLUID_PREFIX_GAS)) return FluidState.GAS;
        if (lower.startsWith(FLUID_PREFIX_LIQUID)) return FluidState.LIQUID;
        return null;
    }

    /**
     * 流体解析结果
     */
    private static final class FluidParseResult {

        final FluidState state;
        final String materialName;

        FluidParseResult(FluidState state, String materialName) {
            this.state = state;
            this.materialName = materialName;
        }
    }

    private static final class OreMatch {

        private final OrePrefixes prefix;
        private final String oreName;
        private final int score;

        private OreMatch(OrePrefixes prefix, String oreName) {
            this.prefix = prefix;
            this.oreName = oreName == null ? "" : oreName;
            this.score = computeScore(prefix, this.oreName);
        }

        private int compareTo(OreMatch other) {
            if (other == null) return -1;
            if (this.score != other.score) return Integer.compare(this.score, other.score);
            if (this.oreName.length() != other.oreName.length()) {
                return Integer.compare(this.oreName.length(), other.oreName.length());
            }
            return this.oreName.compareToIgnoreCase(other.oreName);
        }

        private static int computeScore(OrePrefixes prefix, String oreName) {
            String prefixName = getPrefixName(prefix).toLowerCase(Locale.ROOT);
            if ("plate".equals(prefixName)) return 0;
            if (prefixName.startsWith("plate")) return 100 + prefixName.length();
            return 1000 + oreName.length();
        }
    }
}
