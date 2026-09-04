package com.xyp.gtnotgood.mixins.late.Forestry;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xyp.gtnotgood.common.machines.bee.BeeMutationConditionFilter;

import forestry.api.core.IClimateProvider;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IMutationCondition;
import forestry.core.genetics.mutations.Mutation;

/**
 * Treats selected Forestry bee mutation environment requirements as satisfied.
 */
@Mixin(Mutation.class)
public abstract class MixinMutationConditions {

    @Redirect(
        method = "getChance",
        at = @At(
            value = "INVOKE",
            target = "Lforestry/api/genetics/IMutationCondition;getChance(Lnet/minecraft/world/World;IIILforestry/api/genetics/IAllele;Lforestry/api/genetics/IAllele;Lforestry/api/genetics/IGenome;Lforestry/api/genetics/IGenome;Lforestry/api/core/IClimateProvider;)F"),
        remap = false)
    private float gtnotgood$skipBeeEnvConditions(IMutationCondition condition, World world, int x, int y, int z,
        IAllele allele0, IAllele allele1, IGenome genome0, IGenome genome1, IClimateProvider climate) {
        if (BeeMutationConditionFilter.shouldSkip(condition, genome0)) {
            return 1f;
        }
        return condition.getChance(world, x, y, z, allele0, allele1, genome0, genome1, climate);
    }
}
