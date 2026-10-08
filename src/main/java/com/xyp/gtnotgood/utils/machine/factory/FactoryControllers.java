package com.xyp.gtnotgood.utils.machine.factory;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.common.blocks.ItemMachines;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;

/** One controller contract for server deposits and client labels/icons; never accepts single-block machines. */
public final class FactoryControllers {

    private static final Map<String, ItemStack> icons = new HashMap<>();

    private FactoryControllers() {}

    public static String displayName(String map) {
        ItemStack controller = representative(map);
        return controller == null ? FactoryText.Invalid.text() : controller.getDisplayName();
    }

    public static boolean supports(String map, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemMachines)) return false;
        IMetaTileEntity meta = ItemMachines.getMetaTileEntity(stack);
        if (!(meta instanceof MTEMultiBlockBase) || !(meta instanceof RecipeMapWorkable workable)) return false;
        String required = FactoryRecipeCatalog.controllerKey(map);
        if (matches(workable.getRecipeMap(), required)) return true;
        if (workable.getAvailableRecipeMaps() != null) for (RecipeMap<?> available : workable.getAvailableRecipeMaps())
            if (matches(available, required)) return true;
        return false;
    }

    private static boolean matches(RecipeMap<?> map, String required) {
        return map != null && FactoryRecipeCatalog.controllerKey(map.unlocalizedName).equals(required);
    }

    /** Prefer the named industrial machine; other maps resolve to real registered compatible controllers. */
    public static ItemStack representative(String map) {
        String key = FactoryRecipeCatalog.controllerKey(map);
        ItemStack cached = icons.get(key);
        if (cached != null) return cached.copy();
        ItemStack preferred = null;
        if (key.equals(RecipeMaps.electrolyzerNonCellRecipes.unlocalizedName))
            preferred = ItemList.IndustrialElectrolyzer.get(1);
        else if (key.equals(RecipeMaps.centrifugeNonCellRecipes.unlocalizedName))
            preferred = ItemList.IndustrialCentrifuge.get(1);
        else if (key.equals(RecipeMaps.mixerNonCellRecipes.unlocalizedName))
            preferred = ItemList.IndustrialMixer.get(1);
        else if (key.equals(RecipeMaps.sifterRecipes.unlocalizedName))
            preferred = GregtechItemList.Industrial_Sifter.get(1);
        if (!supports(key, preferred)) {
            preferred = null;
            for (IMetaTileEntity meta : GregTechAPI.METATILEENTITIES) {
                if (!(meta instanceof MTEMultiBlockBase) || !(meta instanceof RecipeMapWorkable workable)) continue;
                ItemStack stack = meta.getStackForm(1);
                if (!supports(key, stack)) continue;
                preferred = stack;
                if (matches(workable.getRecipeMap(), key)) break;
            }
        }
        if (preferred != null) icons.put(key, preferred.copy());
        return preferred;
    }
}
