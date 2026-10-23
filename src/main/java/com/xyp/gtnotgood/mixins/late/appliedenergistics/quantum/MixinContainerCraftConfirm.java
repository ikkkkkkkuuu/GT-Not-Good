// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.mixins.late.appliedenergistics.quantum;

import net.minecraft.entity.player.EntityPlayerMP;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.crafting.quantum.ae2.IQuantumCraftConfirm;
import com.xyp.gtnotgood.common.crafting.quantum.ae2.QuantumCraftingJob;
import com.xyp.gtnotgood.common.crafting.quantum.ae2.QuantumCraftingTree;
import com.xyp.gtnotgood.common.network.quantum.QuantumCraftConfirmPacket;
import com.xyp.gtnotgood.utils.QuantumComputerCpuStatus;

import appeng.api.networking.crafting.ICraftingJob;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.implementations.CraftingCPUStatus;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketCraftingTreeData;

@Mixin(value = ContainerCraftConfirm.class, remap = false)
public abstract class MixinContainerCraftConfirm implements IQuantumCraftConfirm {

    @Shadow
    protected ICraftingJob result;

    @Unique
    private boolean gtng$quantumJob;

    @Inject(
        method = "detectAndSendChanges",
        at = @At(
            value = "FIELD",
            target = "Lappeng/container/implementations/ContainerCraftConfirm;result:Lappeng/api/networking/crafting/ICraftingJob;",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER,
            remap = false),
        require = 1,
        remap = true)
    private void gtng$syncQuantumJob(CallbackInfo ci) {
        final boolean quantumJob = this.result instanceof QuantumCraftingJob;
        if (!(((AEBaseContainer) (Object) this).getPlayerInv().player instanceof EntityPlayerMP player)) return;

        if (quantumJob != this.gtng$quantumJob) {
            this.gtng$quantumJob = quantumJob;
            GTNotGood.channel.sendTo(new QuantumCraftConfirmPacket(quantumJob), player);
        }
        // AE2 only sends the crafting tree of its own V2 jobs
        if (this.result instanceof QuantumCraftingJob<?>job) {
            for (PacketCraftingTreeData packet : QuantumCraftingTree.createChunks(player.worldObj, job)) {
                NetworkHandler.instance.sendTo(packet, player);
            }
        }
    }

    @Inject(method = "cpuMatches", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtng$onlyQuantumCPUs(CraftingCPUStatus c, CallbackInfoReturnable<Boolean> cir) {
        if (
            this.gtng$quantumJob
                && ((QuantumComputerCpuStatus) c).gtng$getCPUType() == QuantumComputerCpuStatus.NORMAL_CPU
        ) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public boolean gtng$isQuantumJob() {
        return this.gtng$quantumJob;
    }

    @Override
    public void gtng$setQuantumJob(boolean quantumJob) {
        this.gtng$quantumJob = quantumJob;
    }
}
