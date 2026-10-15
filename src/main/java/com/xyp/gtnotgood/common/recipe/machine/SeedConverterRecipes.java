package com.xyp.gtnotgood.common.recipe.machine;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.cropsnh.api.CropsNHItemList;
import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;
import com.gtnewhorizon.cropsnh.items.ItemGenericSeed;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.GTItemStack;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipeBuilder;

public final class SeedConverterRecipes {

    private static boolean loaded;

    private SeedConverterRecipes() {}

    public static void loadRecipes() {
        GTModHandler.addCraftingRecipe(GTNGItemList.SeedConverter.get(1),
            new Object[] { "CSC", "RHR", "WMW", 'C', OrePrefixes.circuit.get(Materials.LV), 'S',
                CropsNHItemList.cropSticks.get(1), 'R', ItemList.Conveyor_Module_LV.get(1), 'H',
                ItemList.Hull_LV.get(1), 'W', OrePrefixes.cableGt01.get(Materials.Tin), 'M',
                ItemList.Electric_Motor_LV.get(1) });
    }

    /** CropsNH populates its alternative seed registry during postInit, after this mod. */
    public static void loadConversionRecipes() {
        if (loaded) return;
        loaded = true;
        Set<GTItemStack> registered = new HashSet<>();
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            for (ItemStack alternate : crop.getAlternateSeeds()) {
                if (alternate == null || alternate.getItem() == null || alternate.getItem() instanceof ItemGenericSeed)
                    continue;
                ICropCard target = CropRegistry.instance.fromAlternateSeed(alternate);
                if (target == null || !registered.add(new GTItemStack(alternate))) continue;
                ItemStack input = alternate.copy();
                input.stackSize = 1;
                ItemStack output = target.getSeedItem(SeedStats.DEFAULT_ANALYZED);
                output.stackSize = 1;
                GTRecipeBuilder.builder().itemInputsUnified(input).itemOutputs(output).duration(20).eut(16)
                    .addTo(GTNGRecipeMaps.SeedConversionRecipes);
            }
        }
        GTNotGood.LOG.info("Registered {} seed conversion recipes",
            GTNGRecipeMaps.SeedConversionRecipes.getAllRecipes().size());
    }
}
