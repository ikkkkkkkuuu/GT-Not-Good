/* Adapted from GT-Not-Leisure, LGPL-3.0; see META-INF/shimmer-port/NOTICE.md. */
package com.xyp.gtnotgood.mixins.late.Gregtech;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.recipe.gtnotgood.ShimmerCraftingRegistry;

import gregtech.api.util.GTShapelessRecipe;

/** Captures GT machine crafting with the same scope as upstream Shimmer. */
@Mixin(value = GTShapelessRecipe.class, remap = false)
public class TransmutationShapelessRecipeMixin {

    @Inject(
        method = "<init>(Lnet/minecraft/item/ItemStack;ZZZ[Lnet/minecraft/enchantment/Enchantment;[I[Ljava/lang/Object;)V",
        at = @At("RETURN"))
    private void init(ItemStack aResult, boolean aRemovableByGT, boolean aKeepingNBT, boolean overwriteNBT,
        Enchantment[] aEnchantmentsAdded, int[] aEnchantmentLevelsAdded, Object[] aRecipe, CallbackInfo ci) {
        ShimmerCraftingRegistry.capture(aResult, aRecipe, false);
    }

}
