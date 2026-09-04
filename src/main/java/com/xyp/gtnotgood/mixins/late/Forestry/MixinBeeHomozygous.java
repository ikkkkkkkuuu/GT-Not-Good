package com.xyp.gtnotgood.mixins.late.Forestry;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.machines.bee.BeeBreedingHelper;
import com.xyp.gtnotgood.config.Config;

import forestry.api.apiculture.IBee;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IChromosome;
import forestry.apiculture.genetics.Bee;
import forestry.core.genetics.Chromosome;

/**
 * Makes Forestry bee offspring homozygous and optionally rewrites bred offspring to full genes.
 */
@Mixin(Bee.class)
public abstract class MixinBeeHomozygous {

    @Redirect(
        method = "createOffspring",
        at = @At(
            value = "INVOKE",
            target = "Lforestry/core/genetics/Chromosome;inheritChromosome(Ljava/util/Random;Lforestry/api/genetics/IChromosome;Lforestry/api/genetics/IChromosome;)Lforestry/api/genetics/IChromosome;"),
        remap = false)
    private IChromosome gtnotgood$homozygousInherit(Random rand, IChromosome parent1, IChromosome parent2) {
        IChromosome inherited = Chromosome.inheritChromosome(rand, parent1, parent2);
        Config.ensureLoaded();
        if (!Config.enableBeeHomozygousOffspring || inherited == null) {
            return inherited;
        }
        IAllele active = inherited.getActiveAllele();
        if (active == null) {
            return inherited;
        }
        return new Chromosome(active);
    }

    @Inject(method = "createOffspring", at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private void gtnotgood$maxGenomeOffspring(CallbackInfoReturnable<IBee> cir) {
        Config.ensureLoaded();
        if (!Config.enableBeeMaxGenomeOnBreed) return;
        IBee offspring = cir.getReturnValue();
        if (offspring == null) return;
        IBee maxed = BeeBreedingHelper.maximizeBee(offspring);
        if (maxed != offspring) {
            cir.setReturnValue(maxed);
        }
    }
}
