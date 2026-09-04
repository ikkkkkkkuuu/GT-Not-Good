package com.xyp.gtnotgood.common.items.wildcard.model.filter;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardFilterComponent;

import gregtech.api.enums.Materials;

/**
 * 过滤组件基类：处理白/黑名单公共逻辑。子类只需实现 {@link #matches(Materials)}。
 */
public abstract class AbstractFilterComponent implements IWildcardFilterComponent {

    protected static final String KEY_WHITELIST = "Whitelist";

    private boolean whitelist = true;

    protected AbstractFilterComponent(boolean whitelist) {
        this.whitelist = whitelist;
    }

    /** 材料是否命中本过滤条件（不含白/黑名单反转）。 */
    protected abstract boolean matches(Materials material);

    @Override
    public final boolean test(Materials material) {
        if (material == null) return false;
        return whitelist == matches(material);
    }

    @Override
    public final boolean isWhitelist() {
        return whitelist;
    }

    @Override
    public final void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }

    protected final NBTTagCompound baseData() {
        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean(KEY_WHITELIST, whitelist);
        return data;
    }

    protected static boolean readWhitelist(NBTTagCompound data) {
        return !data.hasKey(KEY_WHITELIST) || data.getBoolean(KEY_WHITELIST);
    }
}
