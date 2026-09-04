package com.xyp.gtnotgood.common.machines.bee;

import com.xyp.gtnotgood.config.Config;

import forestry.api.apiculture.IBeeGenome;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IMutationCondition;

/**
 * Decides which hard environment requirements should be treated as satisfied for bee mutation checks.
 */
public final class BeeMutationConditionFilter {

    private BeeMutationConditionFilter() {}

    public static boolean shouldSkip(IMutationCondition condition, IGenome genome0) {
        if (condition == null) return false;
        if (!(genome0 instanceof IBeeGenome)) return false;

        Config.ensureLoaded();
        String name = condition.getClass()
            .getName();

        if (Config.enableBeeIgnoreDimensionMutation && name.endsWith("DimensionMutationCondition")) {
            return true;
        }

        if (Config.enableBeeIgnoreResourceMutation) {
            if (name.equals("forestry.core.genetics.mutations.MutationConditionRequiresResource")
                || name.equals("forestry.core.genetics.mutations.MutationConditionRequiresResourceOreDict")
                || name.endsWith("ActiveGTMachineMutationCondition")) {
                return true;
            }
        }

        return false;
    }
}
