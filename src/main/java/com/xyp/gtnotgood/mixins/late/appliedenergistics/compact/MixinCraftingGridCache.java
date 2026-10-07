// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Modified for compact, fixed-maximum, energy-free GT Not Good machines.
package com.xyp.gtnotgood.mixins.late.appliedenergistics.compact;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.common.machines.multiblock.QuantumComputer;
import com.xyp.gtnotgood.utils.ECraftingCPUCluster;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.BaseActionSource;
import appeng.crafting.CraftingLink;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;

/** Adapted AE crafting component; see reference/UPSTREAM_PORT_NOTES.md for provenance. */
@Mixin(value = CraftingGridCache.class, remap = false)
public abstract class MixinCraftingGridCache {

    @Shadow
    @Final
    private IGrid grid;

    @Shadow
    @Final
    private Set<CraftingCPUCluster> craftingCPUClusters;

    @Shadow
    public abstract void addLink(final CraftingLink link);

    @Inject(method = "updateCPUClusters()V", at = @At("RETURN"), require = 1)
    private void injectUpdateCPUClusters(final CallbackInfo ci) {
        for (final IGridNode ecNode : grid.getMachines(QuantumComputer.class)) {
            final var ec = (QuantumComputer) ecNode.getMachine();
            ec.forEachCPU(cpu -> {
                this.craftingCPUClusters.add(cpu);

                final CraftingLink craftingLink = (CraftingLink) cpu.getLastCraftingLink();
                if (craftingLink != null) {
                    this.addLink(craftingLink);
                }
            });
        }
    }

    /** Make the replacement CPU available to another order submitted before AE's next grid tick. */
    @WrapOperation(
        method = "submitJob(Lappeng/api/networking/crafting/ICraftingJob;Lappeng/api/networking/crafting/ICraftingRequester;Lappeng/api/networking/crafting/ICraftingCPU;ZLappeng/api/networking/security/BaseActionSource;Z)Lappeng/api/networking/crafting/ICraftingLink;",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/me/cluster/implementations/CraftingCPUCluster;submitJob(Lappeng/api/networking/IGrid;Lappeng/api/networking/crafting/ICraftingJob;Lappeng/api/networking/security/BaseActionSource;Lappeng/api/networking/crafting/ICraftingRequester;)Lappeng/api/networking/crafting/ICraftingLink;"),
        require = 1)
    private ICraftingLink gtng$registerNextVirtualCPU(CraftingCPUCluster cpu, IGrid grid, ICraftingJob job,
        BaseActionSource source, ICraftingRequester requester, Operation<ICraftingLink> original) {
        ICraftingLink link = original.call(cpu, grid, job, source, requester);
        if (link != null && cpu instanceof ECraftingCPUCluster virtualCpu) {
            QuantumComputer owner = virtualCpu.getVirtualCPUOwner();
            if (owner != null && owner.virtualCPU != null) craftingCPUClusters.add(owner.virtualCPU);
        }
        return link;
    }

    /** AE normally favors merging into a busy CPU; independent orders need the idle virtual CPU first. */
    @WrapOperation(
        method = "submitJob(Lappeng/api/networking/crafting/ICraftingJob;Lappeng/api/networking/crafting/ICraftingRequester;Lappeng/api/networking/crafting/ICraftingCPU;ZLappeng/api/networking/security/BaseActionSource;Z)Lappeng/api/networking/crafting/ICraftingLink;",
        at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"),
        require = 1)
    private void gtng$preferIdleVirtualCPU(List<CraftingCPUCluster> candidates,
        Comparator<? super CraftingCPUCluster> comparator, Operation<Void> original) {
        original.call(candidates, comparator);
        for (int index = 0; index < candidates.size(); index++) {
            CraftingCPUCluster candidate = candidates.get(index);
            if (candidate instanceof ECraftingCPUCluster virtualCpu) {
                QuantumComputer owner = virtualCpu.getVirtualCPUOwner();
                if (owner != null && owner.isVirtualCPU(candidate)) {
                    candidates.remove(index);
                    candidates.add(0, candidate);
                    break;
                }
            }
        }
    }

}
