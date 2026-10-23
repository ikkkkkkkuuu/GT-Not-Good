// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.mixins.late.appliedenergistics.quantum;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.machines.multiblock.QuantumComputer;
import com.xyp.gtnotgood.utils.ECraftingCPUCluster;
import com.xyp.gtnotgood.utils.QuantumComputerCpuStatus;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.container.implementations.CraftingCPUStatus;

@Mixin(value = CraftingCPUStatus.class, remap = false)
public class MixinCraftingCPUStatus implements QuantumComputerCpuStatus {

    @Unique
    private byte gtng$cpuType = QuantumComputerCpuStatus.NORMAL_CPU;

    @Inject(method = "<init>(Lappeng/api/networking/crafting/ICraftingCPU;I)V", at = @At("RETURN"), require = 1)
    private void injectInit(final ICraftingCPU cluster, final int serial, final CallbackInfo ci) {
        if (!(cluster instanceof ECraftingCPUCluster eCluster)) {
            return;
        }

        final QuantumComputer owner = eCluster.getVirtualCPUOwner();
        if (owner == null) {
            return;
        }

        this.gtng$cpuType = owner.isVirtualCPU(cluster) ? QuantumComputerCpuStatus.VIRTUAL_CPU
            : QuantumComputerCpuStatus.SPLIT_CPU;
    }

    @Inject(method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"), require = 1)
    private void injectInit(final NBTTagCompound i, final CallbackInfo ci) {
        final byte cpuType = i.getByte("gtngQuantumComputerCPU");
        this.gtng$cpuType = cpuType == QuantumComputerCpuStatus.VIRTUAL_CPU
            || cpuType == QuantumComputerCpuStatus.SPLIT_CPU ? cpuType : QuantumComputerCpuStatus.NORMAL_CPU;
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"), require = 1)
    private void injectWriteToNBT(final NBTTagCompound i, final CallbackInfo ci) {
        if (this.gtng$cpuType == QuantumComputerCpuStatus.NORMAL_CPU) {
            return;
        }

        i.setByte("gtngQuantumComputerCPU", this.gtng$cpuType);
    }

    @Override
    public byte gtng$getCPUType() {
        return this.gtng$cpuType;
    }
}
