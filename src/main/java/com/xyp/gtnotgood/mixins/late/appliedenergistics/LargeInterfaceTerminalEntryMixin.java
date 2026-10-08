package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.tile.inventory.AppEngInternalInventory;

/** Keeps native terminal recipe caches aligned with changing interface and matrix rows. */
@Mixin(targets = "appeng.client.gui.implementations.GuiInterfaceTerminal$InterfaceTerminalEntry", remap = false)
public abstract class LargeInterfaceTerminalEntryMixin {

    @Shadow
    private ItemStack selfRep;

    @Shadow
    private AppEngInternalInventory inv;

    @Shadow
    private boolean[] filteredRecipes;

    @Shadow
    private Boolean[] useSubstitute;

    @Inject(method = "fullItemUpdate", at = @At("RETURN"), remap = false)
    private void largeinterface$resizeRecipeCaches(NBTTagList items, int newSize, CallbackInfo ci) {
        if (!largeinterface$isLargeInterface()) return;
        int size = inv.getSizeInventory();
        filteredRecipes = new boolean[size];
        useSubstitute = new Boolean[size];
    }

    @Inject(method = "setItemInSlot", at = @At("RETURN"), remap = false)
    private void largeinterface$invalidateSubstituteCache(ItemStack stack, int slot, CallbackInfo ci) {
        if (largeinterface$isLargeInterface() && slot >= 0 && slot < useSubstitute.length) {
            useSubstitute[slot] = null;
        }
    }

    @Unique
    private boolean largeinterface$isLargeInterface() {
        if (selfRep == null) return false;
        ItemStack block = GTNGItemList.LargeInterface.getInternalStack_unsafe();
        ItemStack part = GTNGItemList.LargeInterfacePart.getInternalStack_unsafe();
        ItemStack matrix = GTNGItemList.AssemblerMatrix.getInternalStack_unsafe();
        return (block != null && selfRep.isItemEqual(block)) || (part != null && selfRep.isItemEqual(part))
            || (matrix != null && selfRep.isItemEqual(matrix));
    }
}
