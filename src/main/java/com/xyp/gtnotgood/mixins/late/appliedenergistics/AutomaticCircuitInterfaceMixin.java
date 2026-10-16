package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.common.compat.AutomaticMachineCircuit;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import gregtech.api.recipe.RecipeMap;

@Mixin(value = DualityInterface.class, remap = false)
public abstract class AutomaticCircuitInterfaceMixin {

    @Shadow
    @Final
    private IInterfaceHost iHost;

    @Shadow
    public List<ICraftingPatternDetails> craftingList;

    @Shadow
    public abstract void updateCraftingList();

    @Unique
    private List<RecipeMap<?>> gtng$recipeMaps;

    @WrapOperation(
        method = "addToCraftingList",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/implementations/ICraftingPatternItem;getPatternForItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;)Lappeng/api/networking/crafting/ICraftingPatternDetails;"),
        require = 1)
    private ICraftingPatternDetails gtng$omitVirtualRequirements(ICraftingPatternItem item, ItemStack stack,
        World world, Operation<ICraftingPatternDetails> original) {
        return AutomaticMachineCircuit.interfacePattern(original.call(item, stack, world), iHost);
    }

    @Inject(method = "updateCraftingList", at = @At("HEAD"))
    private void gtng$invalidateChangedTargets(CallbackInfo ci) {
        List<RecipeMap<?>> recipeMaps = AutomaticMachineCircuit.interfaceRecipeMaps(iHost);
        if (Objects.equals(gtng$recipeMaps, recipeMaps)) return;
        gtng$recipeMaps = recipeMaps;
        craftingList = null;
    }

    @Inject(method = "updateRedstoneState", at = @At("RETURN"))
    private void gtng$refreshChangedTargets(CallbackInfo ci) {
        if (!Objects.equals(gtng$recipeMaps, AutomaticMachineCircuit.interfaceRecipeMaps(iHost))) {
            updateCraftingList();
        }
    }

    @WrapOperation(
        method = "pushPattern",
        at = @At(value = "INVOKE", target = "Lappeng/helpers/IInterfaceHost;getTargets()Ljava/util/EnumSet;"),
        require = 1)
    private EnumSet<ForgeDirection> gtng$restrictVirtualTargets(IInterfaceHost host,
        Operation<EnumSet<ForgeDirection>> original, ICraftingPatternDetails details, InventoryCrafting table) {
        return AutomaticMachineCircuit.interfaceTargets(host, details, original.call(host));
    }
}
