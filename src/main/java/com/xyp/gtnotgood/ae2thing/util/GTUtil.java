package com.xyp.gtnotgood.ae2thing.util;

import static com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config.getConfigValue;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.ae2thing.integration.Mods;
import com.xyp.gtnotgood.ae2thing.nei.ButtonConstants;
import com.xyp.gtnotgood.ae2thing.nei.object.OrderStack;

import codechicken.nei.recipe.IRecipeHandler;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.common.blocks.ItemMachines;
import gregtech.nei.GTNEIDefaultHandler;

public final class GTUtil {

    private GTUtil() {}

    public static String getRecipeName(IRecipeHandler recipe, List<OrderStack<?>> inputs) {
        if (!(recipe instanceof GTNEIDefaultHandler)) return recipe.getRecipeName();
        if (Mods.PROGRAMMABLE_HATCHES.isModLoaded()
            && getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_FILL_CIRCUIT)) {
            return recipe.getRecipeName();
        }
        if (!getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE)) {
            return recipe.getRecipeName();
        }
        StringBuilder name = new StringBuilder(recipe.getRecipeName());
        for (OrderStack<?> stack : inputs) {
            if (stack.getStack() instanceof ItemStack item && item.stackSize == 0) {
                name.append(' ')
                    .append(item.getItemDamage());
            }
        }
        return name.toString();
    }

    public static boolean isHatchItem(ItemStack item) {
        return item != null && item.getItem() instanceof ItemMachines
            && ItemMachines.getMetaTileEntity(item) instanceof MTEHatch;
    }
}
