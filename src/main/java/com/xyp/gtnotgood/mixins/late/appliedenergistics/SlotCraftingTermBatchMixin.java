package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerNull;
import appeng.container.slot.SlotCraftingTerm;
import appeng.util.Platform;

/** Keeps native bulk crafting running after the last extracted backup item has refilled the grid. */
@Mixin(value = SlotCraftingTerm.class, remap = false)
public abstract class SlotCraftingTermBatchMixin {

    @Shadow
    @Final
    private IInventory craftInv;

    /**
     * AE2 tests the exhausted backup stack after moving its final item into the grid. Continue only when that real
     * grid still contains every original ingredient; replaced containers, missing inputs and broken tools stop as
     * usual.
     * Verify that the next recipe still exists so the native invalid-recipe exit cannot discard already crafted output.
     *
     * @param canContinue original continuation decision
     * @param player      crafting player
     * @param inventory   native network inventory
     * @param extracted   backup stacks after native refill and refunds
     * @param result      native crafting result
     * @return whether the native batch can continue using the refilled grid
     */
    @ModifyReturnValue(method = "postCraft", at = @At("RETURN"), require = 1)
    private boolean gtng$continueRefilledBatch(boolean canContinue, EntityPlayer player,
        IMEMonitor<IAEItemStack> inventory, ItemStack[] extracted, ItemStack result) {
        if (canContinue || !Platform.isServer() || this.craftInv.getSizeInventory() != 9) return canContinue;
        for (int slot = 0; slot < this.craftInv.getSizeInventory(); slot++) {
            ItemStack actual = this.craftInv.getStackInSlot(slot);
            ItemStack expected = extracted[slot];
            if (expected == null) {
                if (actual != null) return false;
            } else if (actual == null || actual.stackSize <= 0 || !Platform.isSameItem(actual, expected)) return false;
        }
        InventoryCrafting nextGrid = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int slot = 0; slot < this.craftInv.getSizeInventory(); slot++) {
            nextGrid.setInventorySlotContents(slot, this.craftInv.getStackInSlot(slot));
        }
        return Platform.findMatchingRecipe(nextGrid, player.worldObj) != null;
    }
}
