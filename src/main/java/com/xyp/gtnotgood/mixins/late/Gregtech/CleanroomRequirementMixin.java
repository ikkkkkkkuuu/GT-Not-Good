package com.xyp.gtnotgood.mixins.late.Gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;

import gregtech.api.util.FakeCleanroom;

/**
 * Applies the configured cleanroom exemption through GregTech's shared bypass switch.
 * This supplies a valid, fully clean room to all standard cleanroom receivers, preventing both recipe rejection and
 * cleanness-related output loss without changing recipe data or low-gravity requirements.
 */
@Mixin(value = FakeCleanroom.class, remap = false)
public abstract class CleanroomRequirementMixin {

    /**
     * Forces the bypass only when cleanrooms are not required. Otherwise GregTech retains its original behavior,
     * including the administrator's own bypass command. Config loading is lazy because this hook may run before
     * preInit.
     *
     * @param cir callback holding whether the shared cleanroom reference should use the fully clean dummy room
     */
    @Inject(method = "isCleanroomBypassEnabled", at = @At("HEAD"), cancellable = true, require = 1)
    private static void gtnotgood$applyCleanroomRequirement(CallbackInfoReturnable<Boolean> cir) {
        Config.ensureLoaded();
        if (!Config.requireCleanroom) {
            cir.setReturnValue(true);
        }
    }
}
