// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.items.packaged;

import java.util.List;
import java.util.Objects;

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
        this.adapterId = Objects.requireNonNull(adapterId);
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
        if (!adapterId.isEmpty()) {
            // #tr tooltip.packaged.install_core
            // # Install in a Wireless Packaged Pattern Provider and bind the matching machine.
            // # zh_CN 装入无线封包样板供应器，再用连接器绑定对应机器。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.install_core"));
        }
        if ("thaumcraft_crucible".equals(adapterId)) {
            // #tr tooltip.packaged.crucible
            // # Automates crucible alchemy. Requires research and heat, but no water.
            // # zh_CN 自动执行坩埚炼金术，需要研究与热源，无需供水。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.crucible"));
            // #tr tooltip.packaged.crucible_pattern
            // # Encode one catalyst and one recipe's output in a processing pattern.
            // # zh_CN 用处理样板填写一个催化剂与单次配方产物。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.crucible_pattern"));
            // #tr tooltip.packaged.crucible_essentia
            // # Uses the crucible's aspects first; Thaumic Energistics supplies any missing essentia.
            // # zh_CN 优先使用坩埚已有源质，不足部分通过神秘能源从 ME 补充。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.crucible_essentia"));
        }
        if ("arcane_workbench".equals(adapterId)) {
            // #tr tooltip.packaged.arcane
            // # Automates an Arcane Workbench using its wand's Vis first.
            // # zh_CN 自动执行奥术工作台配方，优先消耗法杖中的 Vis。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.arcane"));
            // #tr tooltip.packaged.arcane_record
            // # Left-click the bound table in the target list to record its grid. Needs a blank pattern.
            // # zh_CN 左键连接列表中的工作台，消耗空白样板录入九宫格。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.arcane_record"));
            // #tr tooltip.packaged.arcane_owner
            // # Empty the grid and close the workbench screen before dispatching a recipe.
            // # zh_CN 派发配方前须清空九宫格并关闭工作台界面。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.arcane_owner"));
            // #tr tooltip.packaged.arcane_vis
            // # Thaumic Energistics can supply missing Vis from matching AE primal essentia.
            // # zh_CN Vis 不足时，可通过神秘能源将 ME 中对应的基础源质转化为 Vis。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.arcane_vis"));
        }
        if ("thaumcraft_infusion".equals(adapterId)) {
            // #tr tooltip.packaged.infusion
            // # Automates an activated infusion altar using the owner's research.
            // # zh_CN 按所有者已完成的研究，自动操作已激活的注魔祭坛。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.infusion"));
            // #tr tooltip.packaged.infusion_inputs
            // # Empty the pedestals and supply essentia. Only recipes with ordinary item outputs are supported.
            // # zh_CN 须清空基座并供应源质，仅支持产物为普通物品的注魔配方。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.infusion_inputs"));
        } else if (
            "assembly_line".equals(adapterId) || "advanced_assembly_line".equals(adapterId)
                || "circuit_assembly_line".equals(adapterId)
        ) {
            if ("circuit_assembly_line".equals(adapterId)) {
                // #tr tooltip.packaged.circuit_assembly_line
                // # Supplies recipe inputs in order to a bound Circuit Assembly Line.
                // # zh_CN 按配方顺序向已绑定的电路装配线投料。
                lines.add(StatCollector.translateToLocal("tooltip.packaged.circuit_assembly_line"));
                // #tr tooltip.packaged.circuit_imprint
                // # Requires Circuit Assembly Line mode, a matching imprint and power.
                // # zh_CN 须处于电路装配线模式，完成对应铭刻并正常供电。
                lines.add(StatCollector.translateToLocal("tooltip.packaged.circuit_imprint"));
            } else {
                // #tr tooltip.packaged.assembly_line
                // # Dispatches recipes to a bound assembly line controller. Requires recipe data and power.
                // # zh_CN 向已绑定的装配线控制器派发配方，需要配方数据与供电。
                lines.add(StatCollector.translateToLocal("tooltip.packaged.assembly_line"));
            }
            // #tr tooltip.packaged.assembly_hatches
            // # Use empty ordinary input buses and fluid hatches, plus exactly one ordinary output bus.
            // # zh_CN 输入总线与流体输入仓须为空且为普通仓室，输出须为一个普通总线。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.assembly_hatches"));
            // #tr tooltip.packaged.assembly_fluids
            // # Recipes with fluid inputs require Ultimate Encoded Patterns.
            // # zh_CN 含流体输入的配方须使用终极编码样板。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.assembly_fluids"));
        } else if ("blood_altar".equals(adapterId)) {
            // #tr tooltip.packaged.blood_altar
            // # Automates an empty Blood Altar. Supply LP and the required altar tier normally.
            // # zh_CN 自动操作空血祭坛，仍需供应 LP 并满足祭坛等级。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.blood_altar"));
            // #tr tooltip.packaged.blood_altar_step
            // # Encode one recipe step per pattern. Results stay in the altar until collected.
            // # zh_CN 每张样板只填写单步配方，产物保留在祭坛中等待回收。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.blood_altar_step"));
            // #tr tooltip.packaged.blood_altar_limit
            // # Does not charge blood orbs or run several recipe steps from one pattern.
            // # zh_CN 不支持血球充能，也不会用一张样板连续执行多步配方。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.blood_altar_limit"));
        } else if (adapterId.isEmpty()) {
            // #tr tooltip.packaged.basic_core
            // # Crafting component for machine-specific Packaged Cores.
            // # zh_CN 用于制作各类机器专用的封包核心。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.basic_core"));
        }
        if (!adapterId.isEmpty()) {
            // #tr tooltip.packaged.owner_required
            // # The provider's owner must be online in the target's dimension with permission to use it.
            // # zh_CN 供应器所有者须在目标维度在线，并具有目标的操作权限。
            lines.add(StatCollector.translateToLocal("tooltip.packaged.owner_required"));
        }
    }
}
