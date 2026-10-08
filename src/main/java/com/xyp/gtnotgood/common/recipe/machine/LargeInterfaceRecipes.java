package com.xyp.gtnotgood.common.recipe.machine;

import java.util.Collections;
import java.util.Map;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import appeng.api.config.Upgrades;
import appeng.util.Platform;
import cpw.mods.fml.common.registry.GameRegistry;

public final class LargeInterfaceRecipes {

    private LargeInterfaceRecipes() {}

    public static void register() {
        GameRegistry.addShapelessRecipe(GTNGItemList.LargeInterface.get(1), ModsItemlist.FluidInterfaceBlock.get(1),
            GTNGItemList.SuperMTEHatchCraftingInputME.get(1));
        GameRegistry.addRecipe(new Conversion(GTNGItemList.LargeInterface, GTNGItemList.LargeInterfacePart));
        GameRegistry.addRecipe(new Conversion(GTNGItemList.LargeInterfacePart, GTNGItemList.LargeInterface));
    }

    /** Copies the original interface's supported cards after all mods finish their upgrade registration. */
    public static void registerUpgrades() {
        ItemStack original = ModsItemlist.FluidInterfaceBlock.get(1);
        for (Upgrades upgrade : Upgrades.values()) {
            int maximum = 0;
            for (Map.Entry<ItemStack, Integer> supported : upgrade.getSupported().entrySet()) {
                if (Platform.isSameItemPrecise(original, supported.getKey()))
                    maximum = Math.max(maximum, supported.getValue());
            }
            if (maximum > 0) {
                upgrade.registerItem(GTNGItemList.LargeInterface.get(1), maximum);
                upgrade.registerItem(GTNGItemList.LargeInterfacePart.get(1), maximum);
            }
        }
    }

    /** Preserves the native wrench's saved settings when converting between the two interface shapes. */
    private static final class Conversion extends ShapelessRecipes {

        private Conversion(GTNGItemList input, GTNGItemList output) {
            super(output.get(1), Collections.singletonList(input.get(1)));
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            if (!matches(inventory, null)) return null;
            ItemStack result = super.getCraftingResult(inventory);
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack source = inventory.getStackInSlot(slot);
                if (source != null && source.hasTagCompound()) {
                    result.setTagCompound((NBTTagCompound) source.getTagCompound().copy());
                    break;
                }
            }
            return result;
        }
    }
}
