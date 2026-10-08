package com.xyp.gtnotgood.common.recipe.gregtech;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import gregtech.api.enums.Mods;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;

public final class MixerRecipes {

    private MixerRecipes() {}

    public static void loadRecipes() {
        if (!Mods.NEIOrePlugin.isModLoaded()) return;
        if (Mods.GalacticraftCore.isModLoaded()) {
            addDimensionRecipes(ModsItemlist.Tier1Rocket, ModsItemlist.DimensionOverworld, ModsItemlist.DimensionNether,
                ModsItemlist.DimensionTwilight, ModsItemlist.DimensionEnd, ModsItemlist.DimensionEndAsteroids,
                ModsItemlist.DimensionEverglades, ModsItemlist.DimensionMoon);
        }
        if (Mods.GalacticraftMars.isModLoaded()) {
            addDimensionRecipes(ModsItemlist.Tier2Rocket, ModsItemlist.DimensionDeimos, ModsItemlist.DimensionMars,
                ModsItemlist.DimensionPhobos);
            addDimensionRecipes(ModsItemlist.Tier3Rocket, ModsItemlist.DimensionAsteroids,
                ModsItemlist.DimensionCallisto, ModsItemlist.DimensionCeres, ModsItemlist.DimensionEuropa,
                ModsItemlist.DimensionGanymede, ModsItemlist.DimensionRoss128b);
        }
        if (Mods.GalaxySpace.isModLoaded()) {
            addDimensionRecipes(ModsItemlist.Tier4Rocket, ModsItemlist.DimensionIo, ModsItemlist.DimensionMercury,
                ModsItemlist.DimensionVenus);
            addDimensionRecipes(ModsItemlist.Tier5Rocket, ModsItemlist.DimensionEnceladus,
                ModsItemlist.DimensionMiranda, ModsItemlist.DimensionOberon, ModsItemlist.DimensionTitan,
                ModsItemlist.DimensionRoss128ba);
            addDimensionRecipes(ModsItemlist.Tier6Rocket, ModsItemlist.DimensionProteus, ModsItemlist.DimensionTriton);
            addDimensionRecipes(ModsItemlist.Tier7Rocket, ModsItemlist.DimensionHaumea,
                ModsItemlist.DimensionKuiperBelt, ModsItemlist.DimensionMakeMake, ModsItemlist.DimensionPluto);
            addDimensionRecipes(ModsItemlist.Tier8Rocket, ModsItemlist.DimensionBarnardC,
                ModsItemlist.DimensionBarnardE, ModsItemlist.DimensionBarnardF, ModsItemlist.DimensionCentauriBb,
                ModsItemlist.DimensionTauCetiE, ModsItemlist.DimensionVegaB);
        }
    }

    /**
     * Uses a zero-size rocket input as a reusable tool. Circuit numbers follow the dimension display order and restart
     * at one for each rocket tier; tier one includes both T0 and T1.
     */
    private static void addDimensionRecipes(ModsItemlist rocket, ModsItemlist... dimensions) {
        ItemStack rocketStack = rocket.get(0);
        if (rocketStack == null) return;
        for (int index = 0; index < dimensions.length; index++) {
            ItemStack output = dimensions[index].get(1);
            if (output == null) continue;
            GTRecipeBuilder.builder().itemInputs(rocketStack.copy(), new ItemStack(Blocks.cobblestone))
                .circuit(index + 1).itemOutputs(output).duration(5 * GTRecipeBuilder.SECONDS).eut(TierEU.RECIPE_LV)
                .addTo(RecipeMaps.mixerNonCellRecipes);
        }
    }
}
