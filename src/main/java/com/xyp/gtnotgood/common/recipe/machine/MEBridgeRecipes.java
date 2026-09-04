package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.enums.TierEU.RECIPE_LV;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.loader.BlockLoader;
import com.xyp.gtnotgood.loader.ItemsLoader;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

/**
 * 跨维度 ME 网桥两方块(发起端 / 接收端)的合成配方。
 * <p>
 * 刻意用低阶(LV)材料:低阶电路 + 常见金属板/线 + 一个基础 ME 接口(体现"接入 ME 网络"主题),
 * 装配机产出,早期就能造。
 */
public class MEBridgeRecipes {

    public static void loadRecipes() {

        // 基础 ME 接口 —— 体现"接入 ME 网络"主题的核心组件
        final ItemStack ae2Interface = GTModHandler.getModItem("appliedenergistics2", "tile.BlockInterface", 1);

        // 发起端网桥 —— LV 阶装配机配方
        GTValues.RA.stdBuilder()
            .itemInputs(
                ae2Interface,
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.ULV, 2),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 4),
                GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 4))
            .fluidInputs(Materials.Redstone.getMolten(288))
            .itemOutputs(new ItemStack(BlockLoader.blockMEBridgeSender, 4))
            .eut(RECIPE_LV)
            .duration(15 * SECONDS)
            .addTo(RecipeMaps.assemblerRecipes);

        // 接收端网桥 —— LV 阶装配机配方
        GTValues.RA.stdBuilder()
            .itemInputs(
                ae2Interface,
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.ULV, 2),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 4),
                GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 4))
            .fluidInputs(Materials.Glowstone.getMolten(288))
            .itemOutputs(new ItemStack(BlockLoader.blockMEBridgeReceiver, 4))
            .eut(RECIPE_LV)
            .duration(15 * SECONDS)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(BlockLoader.blockMEBridgeSender),
                new ItemStack(BlockLoader.blockMEBridgeReceiver),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 1))
            .itemOutputs(new ItemStack(ItemsLoader.meWirelessTransceiver))
            .eut(RECIPE_LV)
            .duration(10 * SECONDS)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
