package com.xyp.gtnotgood.mixins.late.structurelib;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalHintProbe;

@Mixin(value = StructureLibAPI.class, remap = false)
public abstract class TerminalHintProbeMixin {

    @Inject(
        method = "hintParticle(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;I)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1,
        allow = 1)
    private static void gtnotgood$captureTerminalHint(World world, int x, int y, int z, Block block, int meta,
        CallbackInfo ci) {
        if (TerminalHintProbe.capture(world, x, y, z, block, meta)) ci.cancel();
    }
}
