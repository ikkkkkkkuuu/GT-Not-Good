package com.xyp.gtnotgood.mixins.late.Forestry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xyp.gtnotgood.config.Config;

import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeHousing;
import forestry.apiculture.genetics.Bee;

/**
 * Allows bee specialty products to be produced without matching the species' jubilance climate.
 */
@Mixin(Bee.class)
public abstract class MixinBee {

    @Redirect(
        method = "produceStacks",
        at = @At(
            value = "INVOKE",
            target = "Lforestry/api/apiculture/IAlleleBeeSpecies;isJubilant(Lforestry/api/apiculture/IBeeGenome;Lforestry/api/apiculture/IBeeHousing;)Z"),
        remap = false)
    private boolean gtnotgood$alwaysJubilant(IAlleleBeeSpecies species, IBeeGenome genome, IBeeHousing housing) {
        Config.ensureLoaded();
        if (Config.enableBeeAlwaysJubilant) return true;
        return species.isJubilant(genome, housing);
    }
}
