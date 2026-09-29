package com.xyp.gtnotgood.mixins.late.AppliedEnergistics;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.tile.inventory.AppEngInternalInventory;

/** Avoids decoding and rendering patterns below the scissor rectangle while preserving the entry's full height. */
@Mixin(value = GuiInterfaceTerminal.class, remap = false)
public abstract class InterfaceTerminalViewportMixin {

    @Shadow
    private int viewHeight;

    @WrapOperation(
        method = "drawEntry",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/tile/inventory/AppEngInternalInventory;getStackInSlot(I)Lnet/minecraft/item/ItemStack;",
            remap = true),
        require = 1)
    private ItemStack skipOffscreenPattern(AppEngInternalInventory inventory, int slot, Operation<ItemStack> original,
        @Coerce Object entry, int viewY, int titleBottom, int mouseX, int mouseY) {
        int rowSize = ((InterfaceEntryViewportAccessor) entry).getRowSize();
        if (rowSize > 0 && viewY + slot / rowSize * 18 >= viewHeight) return null;
        return original.call(inventory, slot);
    }
}
