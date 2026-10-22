package com.xyp.gtnotgood.common.recipe.machine;

public final class DimensionalFocusRecipeRules {

    private static final int UHV_RECIPE_EUT = 7_864_320;

    private DimensionalFocusRecipeRules() {}

    public static int duration(int originalDuration) {
        return Math.max(1, originalDuration / 5);
    }

    public static int energy(int originalEut) {
        return originalEut <= 0 || originalEut > Integer.MAX_VALUE / 4 ? 0 : originalEut * 4;
    }

    public static boolean usesEuv(int originalEut) {
        return originalEut > 1_048_576;
    }

    public static int photoresistAmount(int originalEut) {
        int value = (31 - Integer.numberOfLeadingZeros(Math.max(1, originalEut))) / 2;
        return Math.max(1, usesEuv(originalEut) ? value / 2 : value);
    }

    public static long computation(int originalEut) {
        if (originalEut < UHV_RECIPE_EUT) return 0;
        int tier = 1;
        long threshold = 8_388_608;
        while (tier < 8 && originalEut > threshold) {
            threshold *= 4;
            tier++;
        }
        return tier;
    }
}
