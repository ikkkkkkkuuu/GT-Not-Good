package com.xyp.gtnotgood.common.utils;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;

import bartworks.system.material.Werkstoff;
import bartworks.system.material.WerkstoffLoader;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;

/**
 * 模具数据管理类
 * 统一管理所有可选择的模具列表（Shape_Mold_*、Shape_Extruder_*、所有透镜物品 以及 所有纳米蜂群）
 * 将模具列表从SuperMTEHatchCraftingInputME中提取出来，便于维护和扩展
 */
public class MoldDataManager {

    // 所有可选择的模具列表（Shape_Mold_* 和 Shape_Extruder_* 系列物品）
    public static final ItemStack[] CRIB_MOLDS = buildMoldArray();

    private static ItemStack[] buildMoldArray() {
        ArrayList<ItemStack> items = new ArrayList<>();

        // 基础模具 (Shape_Mold_*)
        items.add(ItemList.Shape_Mold_Bottle.get(1));
        items.add(ItemList.Shape_Mold_Plate.get(1));
        items.add(ItemList.Shape_Mold_Ingot.get(1));
        items.add(ItemList.Shape_Mold_Casing.get(1));
        items.add(ItemList.Shape_Mold_Gear.get(1));
        items.add(ItemList.Shape_Mold_Gear_Small.get(1));
        items.add(ItemList.Shape_Mold_Credit.get(1));
        items.add(ItemList.Shape_Mold_Nugget.get(1));
        items.add(ItemList.Shape_Mold_Block.get(1));
        items.add(ItemList.Shape_Mold_Ball.get(1));
        items.add(ItemList.Shape_Mold_Cylinder.get(1));
        items.add(ItemList.Shape_Mold_Anvil.get(1));
        items.add(ItemList.Shape_Mold_Arrow.get(1));
        items.add(ItemList.Shape_Mold_Rod.get(1));
        items.add(ItemList.Shape_Mold_Bolt.get(1));
        items.add(ItemList.Shape_Mold_Round.get(1));
        items.add(ItemList.Shape_Mold_Screw.get(1));
        items.add(ItemList.Shape_Mold_Ring.get(1));
        items.add(ItemList.Shape_Mold_Rod_Long.get(1));
        items.add(ItemList.Shape_Mold_Rotor.get(1));
        items.add(ItemList.Shape_Mold_Turbine_Blade.get(1));
        items.add(ItemList.Shape_Mold_Pipe_Tiny.get(1));
        items.add(ItemList.Shape_Mold_Pipe_Small.get(1));
        items.add(ItemList.Shape_Mold_Pipe_Medium.get(1));
        items.add(ItemList.Shape_Mold_Pipe_Large.get(1));
        items.add(ItemList.Shape_Mold_Pipe_Huge.get(1));
        items.add(ItemList.Shape_Mold_ToolHeadDrill.get(1));

        // 挤出机模头 (Shape_Extruder_*)
        items.add(ItemList.Shape_Extruder_Axe.get(1));
        items.add(ItemList.Shape_Extruder_File.get(1));
        items.add(ItemList.Shape_Extruder_Hammer.get(1));
        items.add(ItemList.Shape_Extruder_Hoe.get(1));
        items.add(ItemList.Shape_Extruder_Pickaxe.get(1));
        items.add(ItemList.Shape_Extruder_Saw.get(1));
        items.add(ItemList.Shape_Extruder_Shovel.get(1));
        items.add(ItemList.Shape_Extruder_Sword.get(1));
        items.add(ItemList.Shape_Extruder_ToolHeadDrill.get(1));
        items.add(ItemList.Shape_Extruder_Gear.get(1));
        items.add(ItemList.Shape_Extruder_Small_Gear.get(1));
        items.add(ItemList.Shape_Extruder_Rotor.get(1));
        items.add(ItemList.Shape_Extruder_Turbine_Blade.get(1));
        items.add(ItemList.Shape_Extruder_Pipe_Tiny.get(1));
        items.add(ItemList.Shape_Extruder_Pipe_Small.get(1));
        items.add(ItemList.Shape_Extruder_Pipe_Medium.get(1));
        items.add(ItemList.Shape_Extruder_Pipe_Large.get(1));
        items.add(ItemList.Shape_Extruder_Pipe_Huge.get(1));
        items.add(ItemList.Shape_Extruder_Block.get(1));
        items.add(ItemList.Shape_Extruder_Bolt.get(1));
        items.add(ItemList.Shape_Extruder_Ingot.get(1));
        items.add(ItemList.Shape_Extruder_Plate.get(1));
        items.add(ItemList.Shape_Extruder_Ring.get(1));
        items.add(ItemList.Shape_Extruder_Rod.get(1));
        items.add(ItemList.Shape_Extruder_Bottle.get(1));
        items.add(ItemList.Shape_Extruder_Casing.get(1));
        items.add(ItemList.Shape_Extruder_Cell.get(1));

        // 激光雕刻机专用透镜 (Laser_Lens_*)
        items.add(GregtechItemList.Laser_Lens_WoodsGlass.get(1));
        items.add(GregtechItemList.Laser_Lens_Special.get(1));

        // 所有材料透镜 (OrePrefixes.lens) - GT 原生材料
        for (Materials m : Materials.values()) {
            ItemStack lens = GTOreDictUnificator.get(OrePrefixes.lens, m, 1);
            if (lens != null) {
                items.add(lens);
            }
        }

        // 所有材料透镜 (OrePrefixes.lens) - BartWorks Werkstoff 材料
        if (WerkstoffLoader.items.containsKey(OrePrefixes.lens)) {
            for (Werkstoff werkstoff : Werkstoff.werkstoffHashSet) {
                if (!werkstoff.hasItemType(OrePrefixes.lens)) continue;
                items.add(new ItemStack(WerkstoffLoader.items.get(OrePrefixes.lens), 1, werkstoff.getmID()));
            }
        }

        // GTNH (NewHorizonsCoreMod) 特殊透镜
        // 这些透镜用于高阶激光雕刻/纳米锻造等 GTNH 特有配方
        {
            ItemStack reinforcedGlassLense = GTModHandler.getModItem("dreamcraft", "ReinforcedGlassLense", 1);
            ItemStack mysteriousCrystalLens = GTModHandler.getModItem("dreamcraft", "MysteriousCrystalLens", 1);
            ItemStack radoxPolymerLens = GTModHandler.getModItem("dreamcraft", "RadoxPolymerLens", 1);
            ItemStack chromaticLens = GTModHandler.getModItem("dreamcraft", "ChromaticLens", 1);
            if (reinforcedGlassLense != null) items.add(reinforcedGlassLense);
            if (mysteriousCrystalLens != null) items.add(mysteriousCrystalLens);
            if (radoxPolymerLens != null) items.add(radoxPolymerLens);
            if (chromaticLens != null) items.add(chromaticLens);
        }

        // AE2 压印模板 (Inscriber Presses)
        items.add(GTModHandler.getModItem("appliedenergistics2", "item.ItemMultiMaterial", 1, 13)); // 运算压印模板
        items.add(GTModHandler.getModItem("appliedenergistics2", "item.ItemMultiMaterial", 1, 14)); // 工程压印模板
        items.add(GTModHandler.getModItem("appliedenergistics2", "item.ItemMultiMaterial", 1, 15)); // 逻辑压印模板
        items.add(GTModHandler.getModItem("appliedenergistics2", "item.ItemMultiMaterial", 1, 19)); // 硅压印模板

        // 所有纳米蜂群 (OrePrefixes.nanite) - GT 原生材料
        // 纳米蜂群由纳米锻造机 (Nano Forge) 生产，用于 PCB 工厂、光学电路线、星门等
        for (Materials m : Materials.values()) {
            ItemStack nanite = GTOreDictUnificator.get(OrePrefixes.nanite, m, 1);
            if (nanite != null) {
                items.add(nanite);
            }
        }

        // 所有纳米蜂群 (OrePrefixes.nanite) - BartWorks Werkstoff 材料
        if (WerkstoffLoader.items.containsKey(OrePrefixes.nanite)) {
            for (Werkstoff werkstoff : Werkstoff.werkstoffHashSet) {
                if (!werkstoff.hasItemType(OrePrefixes.nanite)) continue;
                items.add(new ItemStack(WerkstoffLoader.items.get(OrePrefixes.nanite), 1, werkstoff.getmID()));
            }
        }

        // 去重: 多个来源的透镜/纳米蜂群可能存在重复（如 GT++ 专用透镜与材料透镜重叠、GT 材料与 Werkstoff 材料重叠）
        for (int i = items.size() - 1; i >= 0; i--) {
            ItemStack current = items.get(i);
            if (current == null) {
                items.remove(i);
                continue;
            }
            for (int j = 0; j < i; j++) {
                ItemStack prev = items.get(j);
                if (prev != null && ItemStack.areItemStacksEqual(prev, current)) {
                    items.remove(i);
                    break;
                }
            }
        }

        return items.toArray(new ItemStack[0]);
    }

    /**
     * 获取模具列表
     *
     * @return 模具数组
     */
    public static ItemStack[] getMolds() {
        return CRIB_MOLDS;
    }

    /**
     * 获取模具数量
     *
     * @return 模具数量
     */
    public static int getMoldCount() {
        return CRIB_MOLDS.length;
    }
}
