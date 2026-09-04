package com.xyp.gtnotgood.common.items.wildcard.model.filter;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials;

import gregtech.api.enums.Materials;
import gregtech.api.enums.SubTag;

/**
 * 按 GT SubTag 过滤（METAL / CRYSTAL / NO_SMASHING ...）。
 * 可记住一个示例材料，供 GUI 只在它实际拥有的 SubTag 里循环选择（对齐属性组件的交互）。
 */
public final class SubTagFilterComponent extends AbstractFilterComponent {

    public static final String TYPE = "subtag";

    private static final String KEY_SUBTAG = "SubTag";
    private static final String KEY_EXAMPLE = "Example";

    private SubTag subTag;
    /** 示例材料（拖入的物品解析出来），仅用于 GUI 列出可选 SubTag，不参与匹配。 */
    private Materials example;

    public SubTagFilterComponent(SubTag subTag, Materials example, boolean whitelist) {
        super(whitelist);
        this.subTag = subTag;
        this.example = example;
    }

    public static SubTagFilterComponent empty() {
        return new SubTagFilterComponent(null, null, true);
    }

    public static SubTagFilterComponent readData(NBTTagCompound data) {
        SubTag subTag = WildcardMaterials.findSubTag(data.getString(KEY_SUBTAG));
        Materials example = data.hasKey(KEY_EXAMPLE) ? WildcardMaterials.findByName(data.getString(KEY_EXAMPLE)) : null;
        if (!WildcardMaterials.isRealMaterial(example)) example = null;
        return new SubTagFilterComponent(subTag, example, readWhitelist(data));
    }

    public SubTag getSubTag() {
        return subTag;
    }

    public void setSubTag(SubTag subTag) {
        this.subTag = subTag;
    }

    public Materials getExample() {
        return example;
    }

    /** 设置示例材料，并把当前 SubTag 对齐到该材料实际拥有的第一个 SubTag（若当前的它没有）。 */
    public void setExample(Materials example) {
        this.example = WildcardMaterials.isRealMaterial(example) ? example : null;
        if (this.example != null) {
            java.util.List<SubTag> avail = WildcardMaterials.subTagsOf(this.example);
            if (!avail.isEmpty() && (subTag == null || !avail.contains(subTag))) {
                subTag = avail.get(0);
            }
        }
    }

    @Override
    protected boolean matches(Materials material) {
        return WildcardMaterials.hasSubTag(material, subTag);
    }

    @Override
    public String describe() {
        return (isWhitelist() ? "+" : "-") + (subTag == null ? "?" : subTag.mName);
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = baseData();
        data.setString(KEY_SUBTAG, subTag == null ? "" : subTag.mName);
        if (example != null) data.setString(KEY_EXAMPLE, example.mName);
        return data;
    }
}
