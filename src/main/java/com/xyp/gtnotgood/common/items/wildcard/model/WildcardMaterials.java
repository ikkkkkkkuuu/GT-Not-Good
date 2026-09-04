package com.xyp.gtnotgood.common.items.wildcard.model;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import gregtech.api.enums.FluidState;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;

/**
 * 材料轴模型的共享工具。封装 GT5U 材料的枚举、材料→前缀物品/流体的转换，以及结构化属性/标志查询。
 * 逻辑复用自旧的 {@link com.xyp.gtnotgood.common.items.wildcard.WildcardPatternEntry}，抽出来供新组件模型使用。
 */
public final class WildcardMaterials {

    private static final Map<String, Materials> MATERIAL_NAME_CACHE = new ConcurrentHashMap<>();

    private WildcardMaterials() {}

    /** 判断材料是否为可用的真实材料（排除 _NULL / Empty）。 */
    public static boolean isRealMaterial(Materials material) {
        return material != null && material != Materials._NULL
            && material != Materials.Empty
            && material.mName != null
            && !material.mName.isEmpty();
    }

    /**
     * 判断材料是否能作为通配符展开轴。
     * <p>
     * GT5U 里 AnyCopper / AnyIron 等 ore-dict 族材料是非统一材料，只用于重注册匹配；它们能查到一些
     * OreDictionary 栈，但不是实际 GT5 材料。通配符展开时跳过这些占位材料，避免生成 AnyCopper 锭 ->
     * Copper 导线这类输入输出材料不一致的样板。
     */
    public static boolean isExpandableMaterial(Materials material) {
        return isRealMaterial(material) && material.mUnifiable;
    }

    /** 按名（大小写不敏感）查找材料，找不到返回 {@link Materials#_NULL}。 */
    public static Materials findByName(String name) {
        if (name == null || name.isEmpty()) return Materials._NULL;
        String normalized = name.toLowerCase(Locale.ROOT);
        Materials cached = MATERIAL_NAME_CACHE.get(normalized);
        if (cached != null) return cached;
        for (Materials material : Materials.getAll()) {
            if (isRealMaterial(material)) {
                MATERIAL_NAME_CACHE.putIfAbsent(material.mName.toLowerCase(Locale.ROOT), material);
            }
        }
        Materials material = MATERIAL_NAME_CACHE.get(normalized);
        return material == null ? Materials._NULL : material;
    }

    /**
     * 材料 + 前缀 → 具体统一物品。找不到返回 null（触发展开时跳过该材料）。
     * <p>
     * 用 {@link GTOreDictUnificator#get} 的返回值判断——它内部先查统一表、再查 OreDictionary，
     * 变体真实注册时返回物品、否则返回 null，这才是正确判据。不用 {@code doGenerateItem} 作门槛：
     * 它只覆盖有 materialGenerationBits 的前缀（dust/metal/gem/ore），像 wireGt01/rod/gear 这类
     * 组件前缀 bits 为 0，doGenerateItem 恒为 false，会错误地跳过所有材料。
     */
    public static ItemStack makePrefixStack(OrePrefixes prefix, Materials material, int amount) {
        if (prefix == null || !isRealMaterial(material) || amount <= 0) return null;
        ItemStack stack = GTOreDictUnificator.get(prefix, material, amount);
        if (stack == null || stack.getItem() == null) return null;
        stack.stackSize = amount;
        return stack;
    }

    /**
     * 材料 + 流体状态 → AE2FC 的 ItemFluidDrop（CPU 通过 instanceof 识别流体请求）。找不到返回 null。
     */
    public static ItemStack makeFluidStack(FluidState state, Materials material, long amount) {
        if (state == null || !isRealMaterial(material) || amount <= 0) return null;
        FluidStack fluid = getMaterialFluid(material, state, amount);
        if (fluid == null || fluid.getFluid() == null) return null;
        // [液滴分类] 必须留液滴：产出 ItemFluidDrop 作为样板流体请求，CPU 靠 instanceof 识别下单
        return FluidDropCompat.newStack(fluid);
    }

    /**
     * 显示用：若是 AE2FC 的 ItemFluidDrop，转成 GTNH 原生流体显示物品（正确的流体图标）；否则原样返回。
     * 仅用于 GUI 显示，不能用于展开样板（样板需要 ItemFluidDrop）。
     */
    public static ItemStack toDisplayStack(ItemStack stack) {
        if (stack == null) return null;
        // [液滴分类] 可迁原生：仅把流体转成 GT 显示物品用于 GUI 图标，不参与合成
        FluidStack fluid = FluidDropCompat.getFluidStack(stack);
        if (fluid != null && fluid.getFluid() != null) {
            ItemStack display = gregtech.api.util.GTUtility.getFluidDisplayStack(fluid, true);
            if (display != null) {
                display.stackSize = 1;
                return display;
            }
        }
        return stack;
    }

    /** 材料是否有指定状态的流体。 */
    public static boolean hasFluid(Materials material, FluidState state) {
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

    private static FluidStack getMaterialFluid(Materials material, FluidState state, long amount) {
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

    // ============================================================
    // 结构化属性 / SubTag 查询（对齐 Wildcard-Pattern 的 property / flag 过滤）
    // ============================================================

    /** 支持的材料属性（用于 property 过滤）。 */
    public enum Property {

        DUST,
        METAL,
        GEM,
        ORE,
        CELL,
        PLASMA,
        TOOL_HEAD,
        GEAR,
        FLUID,
        GAS;

        public boolean test(Materials material) {
            if (material == null) return false;
            switch (this) {
                case DUST:
                    return material.hasDustItems();
                case METAL:
                    return material.hasMetalItems();
                case GEM:
                    return material.hasGemItems();
                case ORE:
                    return material.hasOresItems();
                case CELL:
                    return material.hasCell();
                case PLASMA:
                    return material.hasPlasma();
                case TOOL_HEAD:
                    return material.hasToolHeadItems();
                case GEAR:
                    return material.hasGearItems();
                case FLUID:
                    return material.mFluid != null || material.mStandardMoltenFluid != null;
                case GAS:
                    return material.mGas != null;
                default:
                    return false;
            }
        }
    }

    /** 按名解析属性，找不到返回 null。 */
    public static Property findProperty(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return Property.valueOf(
                name.trim()
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /** 列出某材料实际拥有的属性（对齐原版：拖入示例物品后只在它真有的属性里选）。 */
    public static java.util.List<Property> propertiesOf(Materials material) {
        java.util.List<Property> result = new java.util.ArrayList<>();
        if (material == null) return result;
        for (Property property : Property.values()) {
            if (property.test(material)) result.add(property);
        }
        return result;
    }

    /** 列出某材料实际拥有的 SubTag。 */
    public static java.util.List<SubTag> subTagsOf(Materials material) {
        java.util.List<SubTag> result = new java.util.ArrayList<>();
        if (material == null) return result;
        for (SubTag tag : SubTag.sSubTags.values()) {
            if (material.contains(tag)) result.add(tag);
        }
        return result;
    }

    /** 材料是否带指定 SubTag。 */
    public static boolean hasSubTag(Materials material, SubTag tag) {
        return material != null && tag != null && material.contains(tag);
    }

    /** 按名解析 SubTag，找不到返回 null。 */
    public static SubTag findSubTag(String name) {
        if (name == null || name.isEmpty()) return null;
        for (SubTag tag : SubTag.sSubTags.values()) {
            if (tag.mName.equalsIgnoreCase(name.trim())) return tag;
        }
        return null;
    }

    /** 按名解析 OrePrefix，找不到返回 null。 */
    public static OrePrefixes findPrefix(String name) {
        if (name == null || name.isEmpty()) return null;
        OrePrefixes exact = OrePrefixes.getPrefix(name.trim());
        if (exact != null) return exact;
        for (OrePrefixes prefix : OrePrefixes.VALUES) {
            if (prefix.name()
                .equalsIgnoreCase(name.trim())) return prefix;
        }
        return null;
    }

    /** 从一个物品解析出它的 GT (前缀, 材料) 关联；无法识别时两者为 null。 */
    public static PrefixMaterial parseItem(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return PrefixMaterial.EMPTY;
        ItemData data = GTOreDictUnificator.getAssociation(stack);
        if (data != null && data.hasValidPrefixMaterialData()) {
            Materials material = data.mMaterial.mMaterial;
            return new PrefixMaterial(data.mPrefix, isRealMaterial(material) ? material : null);
        }
        return PrefixMaterial.EMPTY;
    }

    /** (前缀, 材料) 解析结果。 */
    public static final class PrefixMaterial {

        public static final PrefixMaterial EMPTY = new PrefixMaterial(null, null);

        public final OrePrefixes prefix;
        public final Materials material;

        public PrefixMaterial(OrePrefixes prefix, Materials material) {
            this.prefix = prefix;
            this.material = material;
        }
    }

    /** 常用属性名列表（供 GUI 下拉），保持插入顺序。 */
    public static Map<String, Property> propertyChoices() {
        Map<String, Property> map = new LinkedHashMap<>();
        for (Property property : Property.values()) {
            map.put(property.name(), property);
        }
        return map;
    }
}
