package com.xyp.gtnotgood.mixins.late.AppliedEnergistics;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;

import com.xyp.gtnotgood.common.compat.AutomaticMachineCircuit;

import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/** Exposes AE's all-or-nothing pattern receiver only for supported GT single-block machines. */
@Mixin(value = BaseMetaTileEntity.class, remap = false)
public abstract class AutomaticMachineCircuitMixin implements ICraftingMachine {

    @Override
    public boolean acceptsPlans() {
        return AutomaticMachineCircuit.supports((BaseMetaTileEntity) (Object) this);
    }

    /**
     * Both item and fluid interfaces call this hook before their ordinary inventory insertion path.
     * Returning false keeps the complete batch in AE and prevents falling back to unconfigured insertion.
     *
     * @param patternDetails    processing pattern including its expected outputs
     * @param table             actual resources supplied by the crafting CPU
     * @param ejectionDirection receiving face of this machine
     * @return true only after the entire batch has been accepted
     */
    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table,
        ForgeDirection ejectionDirection) {
        return AutomaticMachineCircuit
            .push((BaseMetaTileEntity) (Object) this, patternDetails, table, ejectionDirection);
    }
}
