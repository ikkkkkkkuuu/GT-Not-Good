package com.xyp.gtnotgood.ae2thing.coremod.mixin.ae;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.me.cluster.implementations.CraftingCPUCluster;

/**
 * Restores the ported "notify on every crafting job" behaviour on top of GTNH AE2's native notification pipeline.
 * <p>
 * GTNH AE2 already ships a complete crafting-complete notification stack (right-corner popup with elapsed time, chat
 * message, {@code random.levelup} sound and offline resend on next login), but it only fires for players who manually
 * subscribe to a craft via the CPU GUI ({@code playersFollowingCurrentCraft}). This mixin auto-subscribes the player
 * who submits a job, so the submitter is notified on every AE craft without having to click "follow" — matching the
 * old ae2thing behaviour while reusing the richer native pipeline.
 */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCPUClusterFollow {

    @Shadow
    @Final
    private List<String> playersFollowingCurrentCraft;

    @Inject(method = "submitJob", at = @At("RETURN"))
    private void gtnotgood$autoFollowSubmitter(IGrid g, ICraftingJob job, BaseActionSource src,
        ICraftingRequester requestingMachine, CallbackInfoReturnable<ICraftingLink> cir) {
        if (cir.getReturnValue() == null) return;
        if (!(src instanceof PlayerSource ps) || ps.player == null) return;
        String name = ps.player.getCommandSenderName();
        if (name != null && !this.playersFollowingCurrentCraft.contains(name)) {
            this.playersFollowingCurrentCraft.add(name);
        }
    }
}
