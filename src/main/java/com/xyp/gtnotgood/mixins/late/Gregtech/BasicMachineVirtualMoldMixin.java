package com.xyp.gtnotgood.mixins.late.Gregtech;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.compat.VirtualMachineMolds;
import com.xyp.gtnotgood.common.compat.VirtualMoldMachine;

import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/** Persists virtual mold configuration outside the inventory and supplies it to GT lookup and consumption. */
@Mixin(value = MTEBasicMachine.class, remap = false)
public abstract class BasicMachineVirtualMoldMixin implements VirtualMoldMachine {

    @Unique
    private ItemStack gtng$virtualMold;

    @Override
    public ItemStack gtng$getVirtualMold() {
        return gtng$virtualMold == null ? null : gtng$virtualMold.copy();
    }

    @Override
    public void gtng$setVirtualMold(ItemStack mold) {
        if (mold == null && gtng$virtualMold == null) return;
        if (mold != null && gtng$virtualMold != null
            && mold.isItemEqual(gtng$virtualMold)
            && ItemStack.areItemStackTagsEqual(mold, gtng$virtualMold)) return;
        gtng$virtualMold = VirtualMachineMolds.at(VirtualMachineMolds.indexOf(mold));
    }

    @Inject(method = "saveNBTData", at = @At("TAIL"))
    private void gtng$saveMold(NBTTagCompound tag, CallbackInfo ci) {
        if (gtng$virtualMold != null) tag.setTag("GTNGVirtualMold", gtng$virtualMold.writeToNBT(new NBTTagCompound()));
        else tag.removeTag("GTNGVirtualMold");
    }

    @Inject(method = "loadNBTData", at = @At("TAIL"))
    private void gtng$loadMold(NBTTagCompound tag, CallbackInfo ci) {
        gtng$setVirtualMold(ItemStack.loadItemStackFromNBT(tag.getCompoundTag("GTNGVirtualMold")));
    }

    /** Both native processing and insertion filters must see the same non-consumed catalyst. */
    @Inject(method = { "getAllInputs", "appendSelectedCircuit" }, at = @At("RETURN"), cancellable = true)
    private void gtng$appendMold(CallbackInfoReturnable<ItemStack[]> cir) {
        MTEBasicMachine machine = (MTEBasicMachine) (Object) this;
        if (VirtualMachineMolds.supports(machine))
            cir.setReturnValue(VirtualMachineMolds.append(cir.getReturnValue(), gtng$virtualMold));
    }
}
