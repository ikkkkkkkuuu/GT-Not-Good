// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

/** Common upstream core base; the client renderer adds the installed target mod's own item icon. */
public final class ItemPackagedCore extends Item {

    public final String adapterId;

    /**
     * Creates an adapter-specific core using the common upstream base. The caller declares the display-name
     * translation next to the registration site and registers the matching adapter before worlds are loaded.
     *
     * @param adapterId       stable registry key, or empty for a nonfunctional base core
     * @param unlocalizedName item translation name without the item. prefix or .name suffix
     */
    public ItemPackagedCore(String adapterId, String unlocalizedName) {
        this.adapterId = java.util.Objects.requireNonNull(adapterId);
        setUnlocalizedName(unlocalizedName);
        setMaxStackSize(1);
        setCreativeTab(GTNGCreativeTabs.GTNGItem);
        setTextureName(ModList.GTNotGood.getResourcePath("packaged/provider_core_base"));
    }

    public ItemPackagedCore(boolean infusion) {
        adapterId = infusion ? "thaumcraft_infusion" : "";
        if (infusion) {
            // #tr item.tc4_infusion_packaged_core.name
            // # Thaumcraft Infusion Packaged Core
            // # zh_CN 神秘时代注魔封包核心
            setUnlocalizedName("tc4_infusion_packaged_core");
        } else {
            // #tr item.basic_packaged_core.name
            // # Basic Packaged Core
            // # zh_CN 基础封包核心
            setUnlocalizedName("basic_packaged_core");
        }
        setMaxStackSize(1);
        setCreativeTab(GTNGCreativeTabs.GTNGItem);
        setTextureName(ModList.GTNotGood.getResourcePath("packaged/provider_core_base"));
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        if ("thaumcraft_infusion".equals(adapterId)) {
            // #tr tooltip.packaged.infusion
            // # Automates a real infusion altar; research and essentia are required.
            // # zh_CN 自动操作真实注魔祭坛；需要研究与源质。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.infusion"));
        } else if ("assembly_line".equals(adapterId) || "advanced_assembly_line".equals(adapterId)) {
            // #tr tooltip.packaged.assembly_line
            // # Bind the controller; requires its data recipe and power.
            // # zh_CN 绑定控制器；需要机器的数据配方和供电。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.assembly_line"));
            // #tr tooltip.packaged.assembly_hatches
            // # Use empty ordinary input buses, fluid hatches and one output bus.
            // # zh_CN 使用空的普通输入总线、流体输入舱和一个输出总线。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.assembly_hatches"));
        } else if ("blood_altar".equals(adapterId)) {
            // #tr tooltip.packaged.blood_altar
            // # Bind an empty Blood Altar; supply LP normally.
            // # zh_CN 绑定空血祭坛；通过原有方式供应生命源质。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.blood_altar"));
            // #tr tooltip.packaged.blood_altar_step
            // # One recipe step per pattern. Holds the result until collected.
            // # zh_CN 每张样板填写单步配方；产物保留到回收为止。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.blood_altar_step"));
        } else if (adapterId.isEmpty()) {
            // #tr tooltip.packaged.basic_core
            // # Base for machine-specific Packaged Cores.
            // # zh_CN 用于制作对应机器的封包核心。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.basic_core"));
        }
    }
}
