package com.xyp.gtnotgood.common.recipe.gregtech;


import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class BenderRecipes {

    public static void loadRecipes() {
        RecipeMap<?> Br = RecipeMaps.benderRecipes;

        // 末影珍珠 → 末影珍珠板
        GTRecipeBuilder.builder()
            .itemInputs(new ItemStack(Items.ender_pearl, 1))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.EnderPearl, 1))
            .circuit(1)
            .duration(100)
            .eut(30)
            .addTo(Br);

        // 末影之眼 → 末影之眼板
        GTRecipeBuilder.builder()
            .itemInputs(new ItemStack(Items.ender_eye, 1))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.EnderEye, 1))
            .circuit(1)
            .duration(100)
            .eut(30)
            .addTo(Br);

        // 绿宝石 → 绿宝石板
        GTRecipeBuilder.builder()
            .itemInputs(new ItemStack(Items.emerald, 1))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.Emerald, 1))
            .circuit(1)
            .duration(100)
            .eut(30)
            .addTo(Br);

        // 钻石 → 钻石板
        GTRecipeBuilder.builder()
            .itemInputs(new ItemStack(Items.diamond, 1))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.Diamond, 1))
            .circuit(1)
            .duration(100)
            .eut(30)
            .addTo(Br);

        // 赛特斯石英 → 赛特斯石英板
        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.gem, Materials.CertusQuartz, 1))
            .itemOutputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.CertusQuartz, 1))
            .circuit(1)
            .duration(100)
            .eut(30)
            .addTo(Br);

    }

}
