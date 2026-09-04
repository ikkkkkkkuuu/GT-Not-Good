package com.xyp.gtnotgood.common.items.wildcard.model.filter;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials;

import gregtech.api.enums.Materials;

/** 按单个具体材料过滤。 */
public final class SimpleFilterComponent extends AbstractFilterComponent {

    public static final String TYPE = "simple";

    private static final String KEY_MATERIAL = "Material";

    private Materials material;

    public SimpleFilterComponent(Materials material, boolean whitelist) {
        super(whitelist);
        this.material = material;
    }

    public static SimpleFilterComponent empty() {
        return new SimpleFilterComponent(null, true);
    }

    public static SimpleFilterComponent readData(NBTTagCompound data) {
        Materials material = WildcardMaterials.findByName(data.getString(KEY_MATERIAL));
        return new SimpleFilterComponent(
            WildcardMaterials.isRealMaterial(material) ? material : null,
            readWhitelist(data));
    }

    public Materials getMaterial() {
        return material;
    }

    public void setMaterial(Materials material) {
        this.material = material;
    }

    @Override
    protected boolean matches(Materials candidate) {
        return material != null && material == candidate;
    }

    @Override
    public String describe() {
        return (isWhitelist() ? "+" : "-") + (material == null ? "?" : material.mName);
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = baseData();
        data.setString(KEY_MATERIAL, material == null ? "" : material.mName);
        return data;
    }
}
