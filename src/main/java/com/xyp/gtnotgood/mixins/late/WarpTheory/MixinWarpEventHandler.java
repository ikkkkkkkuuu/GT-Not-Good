package com.xyp.gtnotgood.mixins.late.WarpTheory;

import net.minecraftforge.event.entity.living.LivingEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.config.Config;

/**
 * Suppresses WarpTheory's independent warp-event handler without changing the player's warp values.
 */
@Pseudo
@Mixin(targets = "shukaro.warptheory.handlers.WarpEventHandler", remap = false)
public class MixinWarpEventHandler {

    /**
     * Stops WarpTheory from queuing or executing warp events when the shared option is enabled.
     *
     * @param event current living-update event
     * @param ci    cancellable Mixin callback
     */
    @Inject(
        method = "livingUpdate(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void gtnotgood$disableWarpTheoryEvents(LivingEvent.LivingUpdateEvent event, CallbackInfo ci) {
        if (Config.disableWarpEvents) {
            ci.cancel();
        }
    }
}
