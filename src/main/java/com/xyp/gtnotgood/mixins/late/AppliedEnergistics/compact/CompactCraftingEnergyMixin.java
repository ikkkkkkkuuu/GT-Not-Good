package com.xyp.gtnotgood.mixins.late.AppliedEnergistics.compact;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xyp.gtnotgood.common.machines.multiblock.AssemblerMatrix;
import com.xyp.gtnotgood.utils.ECraftingCPUCluster;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.me.cluster.implementations.CraftingCPUCluster;

/** Waives dispatch energy only for this mod's compact CPU or assembler; other AE crafting keeps its usual cost. */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class CompactCraftingEnergyMixin {

    /**
     * Covers both simulation and actual extraction so energy cannot gate an otherwise free dispatch.
     *
     * @return the requested amount for a compact machine, or the ordinary extracted amount
     */
    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/networking/energy/IEnergyGrid;extractAEPower(DLappeng/api/config/Actionable;Lappeng/api/config/PowerMultiplier;)D"),
        require = 2)
    private double gtng$freeDispatch(IEnergyGrid grid, double amount, Actionable action, PowerMultiplier multiplier,
        Operation<Double> original, @Local(name = "medium") ICraftingMedium medium) {
        return (Object) this instanceof ECraftingCPUCluster || medium instanceof AssemblerMatrix ? amount
            : original.call(grid, amount, action, multiplier);
    }
}
