package com.xyp.gtnotgood.common.recipe.gtnotgood;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalSettings;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;

public final class MultiblockTerminalRecipes {

    private MultiblockTerminalRecipes() {}

    public static void register() {
        GameRegistry.addRecipe(new ShapedOreRecipe(GTNGItemList.MultiblockTerminal.get(1), "ABA", "CDC", "CEC", 'A',
            OrePrefixes.screw.get(Materials.BlackBronze).toString(), 'B',
            OrePrefixes.plate.get(Materials.Glass).toString(), 'C', OrePrefixes.plate.get(Materials.Silicon).toString(),
            'D', ModsItemlist.StructureHologram.get(1), 'E', OrePrefixes.circuit.get(Materials.MV).toString()) {

            @Override
            public ItemStack getCraftingResult(InventoryCrafting inventory) {
                ItemStack output = super.getCraftingResult(inventory);
                for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                    ItemStack input = inventory.getStackInSlot(slot);
                    if (input != null && input.getItem() instanceof ItemConstructableTrigger) {
                        if (input.hasTagCompound())
                            output.setTagCompound((NBTTagCompound) input.getTagCompound().copy());
                        TerminalSettings.setCount(output, input.stackSize);
                        break;
                    }
                }
                return output;
            }
        });
    }
}
