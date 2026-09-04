package com.xyp.gtnotgood.common.recipe.gregtech;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

public class CraftingTableRecipes {

    public static void loadRecipes() {
        // spotless:off


        GTModHandler.addCraftingRecipe(
            GTNGItemList.VeinMiningPickaxe.get(1),
            new Object[] {
                "AAA",
                " B ",
                " A ",
                'A', new ItemStack(Blocks.planks, 1, 0),
                'B', new ItemStack(Items.wooden_pickaxe, 1, 0)
            });

        GTModHandler.addCraftingRecipe(
            GTNGItemList.SteamTurbineLV.get(1),
            new Object[] { "ABA", "CDC", "AEA",
                'A', ItemList.Electric_Pump_LV.get(1),
                'B', OrePrefixes.circuit.get(Materials.LV),
                'C', GTOreDictUnificator.get(OrePrefixes.rotor, Materials.Steel, 1L),
                'D', ItemList.Hull_LV.get(1),
                'E', GTOreDictUnificator.get(OrePrefixes.cableGt16, Materials.Tin, 1L)
            });

        GTModHandler.addCraftingRecipe(
            GTNGItemList.SteamTurbineMV.get(1),
            new Object[] { "ABA", "CDC", "AEA",
                'A', ItemList.Electric_Pump_MV.get(1),
                'B', OrePrefixes.circuit.get(Materials.MV),
                'C', GTOreDictUnificator.get(OrePrefixes.rotor, Materials.Aluminium, 1L),
                'D', ItemList.Hull_MV.get(1),
                'E', GTOreDictUnificator.get(OrePrefixes.cableGt16, Materials.AnnealedCopper, 1L)
            });

        GTModHandler.addCraftingRecipe(
            GTNGItemList.SteamTurbineHV.get(1),
            new Object[] { "ABA", "CDC", "AEA",
                'A', ItemList.Electric_Pump_HV.get(1),
                'B', OrePrefixes.circuit.get(Materials.HV),
                'C', GTOreDictUnificator.get(OrePrefixes.rotor, Materials.StainlessSteel, 1L),
                'D', ItemList.Hull_HV.get(1),
                'E', GTOreDictUnificator.get(OrePrefixes.cableGt16, Materials.Gold, 1L)
            });

        GTModHandler.addCraftingRecipe(
            GTNGItemList.SteamTurbineEV.get(1),
            new Object[] { "ABA", "CDC", "AEA",
                'A', ItemList.Electric_Pump_EV.get(1),
                'B', OrePrefixes.circuit.get(Materials.EV),
                'C', GTOreDictUnificator.get(OrePrefixes.rotor, Materials.StainlessSteel, 1L),
                'D', ItemList.Hull_EV.get(1),
                'E', GTOreDictUnificator.get(OrePrefixes.cableGt16, Materials.Gold, 1L)
            });

        GTModHandler.addCraftingRecipe(
            GTNGItemList.SteamTurbineIV.get(1),
            new Object[] { "ABA", "CDC", "AEA",
                'A', ItemList.Electric_Pump_IV.get(1),
                'B', OrePrefixes.circuit.get(Materials.IV),
                'C', GTOreDictUnificator.get(OrePrefixes.rotor, Materials.TungstenSteel, 1L),
                'D', ItemList.Hull_IV.get(1),
                'E', GTOreDictUnificator.get(OrePrefixes.cableGt16, Materials.Tungsten, 1L)
            });

        GTModHandler.addCraftingRecipe(
            GTNGItemList.SteamTurbineLuV.get(1),
            new Object[] { "ABA", "CDC", "AEA",
                'A', ItemList.Electric_Pump_LuV.get(1),
                'B', OrePrefixes.circuit.get(Materials.LuV),
                'C', GTOreDictUnificator.get(OrePrefixes.rotor, Materials.Chrome, 1L),
                'D', ItemList.Hull_LuV.get(1),
                'E', GTOreDictUnificator.get(OrePrefixes.cableGt16, Materials.NiobiumTitanium, 1L)
            });









        // spotless:on
    }

}
