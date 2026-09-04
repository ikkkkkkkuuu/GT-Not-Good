package com.xyp.gtnotgood.mixins.late.Thaumcraft;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xyp.gtnotgood.config.Config;

import thaumcraft.common.lib.WarpEvents;

/**
 * Suppresses Thaumcraft warp events while preserving all warp values and normal temporary-warp decay.
 * <p>
 * The redirected random roll gates the harmful event block in {@code WarpEvents.checkWarpEvent}. Returning a value
 * above every possible trigger threshold skips that block without cancelling the rest of the method.
 */
@Mixin(WarpEvents.class)
public class MixinWarpEvents {

    /**
     * Replaces the warp-event trigger roll when the no-warp-events option is enabled.
     *
     * @param rand  Thaumcraft's world random source
     * @param bound original random bound
     * @return an impossible trigger value when disabled, otherwise the original random result
     */
    @Redirect(
        method = "checkWarpEvent",
        at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I", ordinal = 0),
        remap = false)
    private static int gtnotgood$suppressWarpEventRoll(Random rand, int bound) {
        return Config.disableWarpEvents ? Integer.MAX_VALUE : rand.nextInt(bound);
    }
}
