package com.xyp.gtnotgood.mixins.late.gregtech;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.common.blocks.stockio.StockIORecipeBridge;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/** Keeps the native recipe checks while committing one adjacent ME interface's simulated inputs. */
@Mixin(value = MTEBasicMachine.class, remap = false)
public abstract class BasicMachineStockIOMixin {

    @Inject(method = "onPostTick", at = @At("HEAD"))
    private void gtng$stockIORetry(IGregTechTileEntity base, long tick, CallbackInfo ci) {
        StockIORecipeBridge.retryIdle((MTEBasicMachine) (Object) this, base, tick);
    }

    @WrapMethod(method = "checkRecipe(Z)I")
    private int gtng$stockIORecipe(boolean skipOC, Operation<Integer> original) {
        MTEBasicMachine machine = (MTEBasicMachine) (Object) this;
        return StockIORecipeBridge.checkRecipe(machine, () -> original.call(skipOC));
    }

    @WrapOperation(
        method = "checkRecipe(Z)I",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEBasicMachine;getAllInputs()[Lnet/minecraft/item/ItemStack;"))
    private ItemStack[] gtng$stockIOItems(MTEBasicMachine machine, Operation<ItemStack[]> original) {
        return StockIORecipeBridge.itemInputs(machine, original.call(machine));
    }

    @ModifyArg(
        method = "checkRecipe(Z)I",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/recipe/FindRecipeQuery;fluids([Lnet/minecraftforge/fluids/FluidStack;)Lgregtech/api/recipe/FindRecipeQuery;"),
        index = 0)
    private FluidStack[] gtng$stockIOFluidLookup(FluidStack[] local) {
        return StockIORecipeBridge.fluidInputs((MTEBasicMachine) (Object) this, local);
    }

    @ModifyArg(
        method = "checkRecipe(Z)I",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/util/GTRecipe;isRecipeInputEqual(Z[Lnet/minecraftforge/fluids/FluidStack;[Lnet/minecraft/item/ItemStack;)Z"),
        index = 1)
    private FluidStack[] gtng$stockIOFluidConsumption(FluidStack[] local) {
        return StockIORecipeBridge.fluidInputs((MTEBasicMachine) (Object) this, local);
    }
}
