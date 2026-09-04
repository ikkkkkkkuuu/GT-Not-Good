package com.xyp.gtnotgood.common.items.wildcard.model.io;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMaterials;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;

/**
 * 前缀组件：用 GT 前缀（ingot/plate/dust...）+ 数量，把当前材料变成对应统一物品。
 * 这是"通配"的核心组件——apply(材料) 用材料生成对应形态。
 */
public final class PrefixIOComponent implements IWildcardIOComponent {

    public static final String TYPE = "prefix";

    private static final String KEY_PREFIX = "Prefix";
    private static final String KEY_AMOUNT = "Amount";

    private OrePrefixes prefix;
    private int amount;
    /** 用户正在输入的前缀名原始文本；解析成 OrePrefixes 后仍保留原文，避免输入过程被 getter 冲掉。 */
    private String rawText;

    public PrefixIOComponent(OrePrefixes prefix, int amount) {
        this.prefix = prefix;
        this.amount = Math.max(1, amount);
        this.rawText = prefix == null ? "" : prefix.name();
    }

    public static PrefixIOComponent empty() {
        return new PrefixIOComponent(null, 1);
    }

    public static PrefixIOComponent readData(NBTTagCompound data) {
        OrePrefixes prefix = WildcardMaterials.findPrefix(data.getString(KEY_PREFIX));
        int amount = Math.max(1, data.getInteger(KEY_AMOUNT));
        return new PrefixIOComponent(prefix, amount);
    }

    public OrePrefixes getPrefix() {
        return prefix;
    }

    public void setPrefix(OrePrefixes prefix) {
        this.prefix = prefix;
        if (prefix != null) this.rawText = prefix.name();
    }

    /** GUI 文本框读取的原始文本。 */
    public String getRawText() {
        return rawText == null ? "" : rawText;
    }

    /** GUI 文本框写入：记住原文并尝试解析成前缀（失败时保留原文、prefix 置空）。 */
    public void setRawText(String text) {
        this.rawText = text == null ? "" : text;
        this.prefix = WildcardMaterials.findPrefix(this.rawText);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(1, amount);
    }

    @Override
    public ItemStack apply(Materials material) {
        return WildcardMaterials.makePrefixStack(prefix, material, amount);
    }

    @Override
    public ItemStack getDisplayStack() {
        // 用一个代表性材料（铁）展示前缀形态
        ItemStack display = WildcardMaterials.makePrefixStack(prefix, Materials.Iron, Math.max(1, amount));
        return display;
    }

    @Override
    public boolean isEmpty() {
        return prefix == null;
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = new NBTTagCompound();
        data.setString(KEY_PREFIX, prefix == null ? "" : prefix.name());
        data.setInteger(KEY_AMOUNT, amount);
        return data;
    }
}
