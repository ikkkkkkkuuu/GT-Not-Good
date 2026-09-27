package com.xyp.gtnotgood.common.items.wildcard.model.filter;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import lombok.Getter;
import lombok.Setter;

/** 按"能否做成某 OrePrefix 物品"过滤（doGenerateItem）。 */
public final class PrefixFilterComponent extends AbstractFilterComponent {

    public static final String TYPE = "prefix";

    private static final String KEY_PREFIX = "Prefix";

    @Getter
    @Setter
    private OrePrefixes prefix;

    public PrefixFilterComponent(OrePrefixes prefix, boolean whitelist) {
        super(whitelist);
        this.prefix = prefix;
    }

    public static PrefixFilterComponent empty() {
        return new PrefixFilterComponent(WildcardMaterials.findPrefix("plate"), true);
    }

    public static PrefixFilterComponent readData(NBTTagCompound data) {
        OrePrefixes prefix = WildcardMaterials.findPrefix(data.getString(KEY_PREFIX));
        return new PrefixFilterComponent(prefix, readWhitelist(data));
    }

    @Override
    protected boolean matches(Materials material) {
        return prefix != null && prefix.doGenerateItem(material);
    }

    @Override
    public String describe() {
        return (isWhitelist() ? "+" : "-") + (prefix == null ? "?" : prefix.name());
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = baseData();
        data.setString(KEY_PREFIX, prefix == null ? "" : prefix.name());
        return data;
    }
}
