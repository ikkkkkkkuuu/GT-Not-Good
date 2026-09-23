package com.xyp.gtnotgood.mixins.late.AppliedEnergistics.compact;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.utils.DireCraftingPatternDetails;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.me.cluster.implementations.CraftingCPUCluster;

/** Restores the matrix's crafting wrapper when AE reloads a serialized in-flight CPU task. */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MatrixPatternPersistenceMixin {

    @WrapOperation(
        method = "readFromNBT",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/implementations/ICraftingPatternItem;getPatternForItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;)Lappeng/api/networking/crafting/ICraftingPatternDetails;"),
        require = 1)
    private ICraftingPatternDetails gtng$restoreMatrixPattern(ICraftingPatternItem item, ItemStack stack, World world,
        Operation<ICraftingPatternDetails> original) {
        ICraftingPatternDetails details = original.call(item, stack, world);
        if (details != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(DireCraftingPatternDetails.SERIALIZED_MULTIPLIER)) {
            DireCraftingPatternDetails wrapped = new DireCraftingPatternDetails(details);
            wrapped.setMultiply(
                stack.getTagCompound()
                    .getInteger(DireCraftingPatternDetails.SERIALIZED_MULTIPLIER));
            return wrapped;
        }
        return details;
    }
}
