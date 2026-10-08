package com.xyp.gtnotgood.mixins.late.compass;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.items.compass.StructureLocations;

import eu.usrv.legacylootgames.StructureGenerator;

/** Records only successful puzzle-room generation, not rejected seed candidates. */
@Mixin(value = StructureGenerator.class, remap = false)
public abstract class LootGamesStructureMixin {

    @Shadow
    private int _mDungeonBottom;

    @Inject(method = "generatePuzzleMicroDungeon", at = @At("RETURN"), remap = false)
    private void gtng$recordGame(World world, int x, int z, CallbackInfoReturnable<Boolean> cir) {
        if (!world.isRemote && cir.getReturnValue()) {
            StructureLocations.get(world).add(1, x, _mDungeonBottom + StructureGenerator.PUZZLEROOM_MASTER_TE_OFFSET,
                z);
        }
    }
}
