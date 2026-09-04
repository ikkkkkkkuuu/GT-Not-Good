package com.xyp.gtnotgood.common.items.wildcard.model.filter;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials;

import gregtech.api.enums.Materials;

/** 按材料属性过滤（金属/宝石/矿石/齿轮/电池/流体...）。可记住一个示例材料，供 GUI 只在它实际拥有的属性里循环选择。 */
public final class PropertyFilterComponent extends AbstractFilterComponent {

    public static final String TYPE = "property";

    private static final String KEY_PROPERTY = "Property";
    private static final String KEY_EXAMPLE = "Example";

    private WildcardMaterials.Property property;
    /** 示例材料（拖入的物品解析出来），仅用于 GUI 列出可选属性，不参与匹配。 */
    private Materials example;

    public PropertyFilterComponent(WildcardMaterials.Property property, Materials example, boolean whitelist) {
        super(whitelist);
        this.property = property;
        this.example = example;
    }

    public static PropertyFilterComponent empty() {
        return new PropertyFilterComponent(WildcardMaterials.Property.METAL, null, true);
    }

    public static PropertyFilterComponent readData(NBTTagCompound data) {
        WildcardMaterials.Property property = WildcardMaterials.findProperty(data.getString(KEY_PROPERTY));
        Materials example = data.hasKey(KEY_EXAMPLE) ? WildcardMaterials.findByName(data.getString(KEY_EXAMPLE)) : null;
        if (!WildcardMaterials.isRealMaterial(example)) example = null;
        return new PropertyFilterComponent(property, example, readWhitelist(data));
    }

    public WildcardMaterials.Property getProperty() {
        return property;
    }

    public void setProperty(WildcardMaterials.Property property) {
        this.property = property;
    }

    public Materials getExample() {
        return example;
    }

    /** 设置示例材料，并把当前属性对齐到该材料实际拥有的第一个属性（若当前属性它没有）。 */
    public void setExample(Materials example) {
        this.example = WildcardMaterials.isRealMaterial(example) ? example : null;
        if (this.example != null) {
            java.util.List<WildcardMaterials.Property> avail = WildcardMaterials.propertiesOf(this.example);
            if (!avail.isEmpty() && (property == null || !avail.contains(property))) {
                property = avail.get(0);
            }
        }
    }

    @Override
    protected boolean matches(Materials material) {
        return property != null && property.test(material);
    }

    @Override
    public String describe() {
        return (isWhitelist() ? "+" : "-") + (property == null ? "?" : property.name());
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = baseData();
        data.setString(KEY_PROPERTY, property == null ? "" : property.name());
        if (example != null) data.setString(KEY_EXAMPLE, example.mName);
        return data;
    }
}
