package com.xyp.gtnotgood.mixins.late.EnderIO;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import crazypants.enderio.config.Config;
import crazypants.enderio.item.ItemSoulVessel;

/** Allows every non-player living entity to be captured by an Ender IO Soul Vial. */
@Mixin(value = ItemSoulVessel.class, remap = false)
public abstract class MixinItemSoulVessel {

    // remap = true overrides the class-level remap = false: unlike isBlackListed (Ender IO's own method), this is an
    // override of vanilla Item#itemInteractionForEntity, so it is func_111207_a at runtime and needs a refmap entry.
    @Inject(method = "itemInteractionForEntity", at = @At("HEAD"), remap = true)
    private void gtnc$enableBossCapture(ItemStack item, EntityPlayer player, EntityLivingBase entity,
        CallbackInfoReturnable<Boolean> cir) {
        Config.soulVesselCapturesBosses = true;
    }

    @Inject(method = "isBlackListed", at = @At("HEAD"), cancellable = true)
    private void gtnc$removeBlacklist(String entityId, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
