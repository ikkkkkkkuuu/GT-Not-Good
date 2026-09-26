package com.xyp.gtnotgood.mixins.late.Thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.xyp.gtnotgood.common.packaged.CrucibleCooldownAccess;

import thaumcraft.common.tiles.TileCrucible;

/** Exposes only the native successful-craft cooldown; ordinary thrown-item crafting remains unchanged. */
@Mixin(value = TileCrucible.class, remap = false)
public abstract class MixinPackagedCrucible implements CrucibleCooldownAccess {

    @Shadow
    private long counter;

    @Override
    public void gtnotgood$resetCraftingCooldown() {
        counter = -250L;
    }
}
