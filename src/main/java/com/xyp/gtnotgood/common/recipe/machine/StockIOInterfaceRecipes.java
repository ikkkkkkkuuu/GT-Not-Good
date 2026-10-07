package com.xyp.gtnotgood.common.recipe.machine;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;

public final class StockIOInterfaceRecipes {

    private StockIOInterfaceRecipes() {}

    public static void register() {
        GameRegistry.addShapelessRecipe(
            GTNGItemList.StockIOInterface.get(1),
            AEApi.instance()
                .definitions()
                .blocks()
                .iface()
                .maybeStack(1)
                .get(),
            GTNGItemList.AdvancedIOBus.get(1));
        GameRegistry.addRecipe(new Conversion(GTNGItemList.StockIOInterface, GTNGItemList.StockIOInterfacePart));
        GameRegistry.addRecipe(new Conversion(GTNGItemList.StockIOInterfacePart, GTNGItemList.StockIOInterface));
    }

    /** Form conversion preserves ghost settings and real unsettled refunds; ordinary crafting would discard them. */
    private static final class Conversion implements IRecipe {

        private final GTNGItemList input;
        private final GTNGItemList output;

        private Conversion(GTNGItemList input, GTNGItemList output) {
            this.input = input;
            this.output = output;
        }

        private ItemStack source(InventoryCrafting inventory) {
            ItemStack found = null;
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack == null) continue;
                if (found != null || stack.getItem() != input.getItem()) return null;
                found = stack;
            }
            return found;
        }

        @Override
        public boolean matches(InventoryCrafting inventory, World world) {
            return source(inventory) != null;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventory) {
            ItemStack source = source(inventory);
            if (source == null) return null;
            ItemStack result = output.get(1);
            if (source.hasTagCompound()) result.setTagCompound(
                (NBTTagCompound) source.getTagCompound()
                    .copy());
            return result;
        }

        @Override
        public int getRecipeSize() {
            return 1;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return output.get(1);
        }
    }
}
