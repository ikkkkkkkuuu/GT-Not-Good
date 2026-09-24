package com.xyp.gtnotgood.mixins.late.SpiceOfLife;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;

/**
 * Gives Spice of Life a full food modifier when its diminishing returns are disabled.
 * <p>
 * Both the AppleCore food-value handler and the eating-duration handler use this modifier. Food history and unrelated
 * Spice of Life features continue to run normally.
 *
 * @see <a href=
 *      "https://github.com/GTNewHorizons/SpiceOfLife/blob/master/src/main/java/squeek/spiceoflife/foodtracker/FoodModifier.java">Spice
 *      of Life FoodModifier</a>
 */
@Pseudo
@Mixin(targets = "squeek.spiceoflife.foodtracker.FoodModifier", remap = false)
public abstract class MixinFoodModifier {

    /**
     * Replaces the final nutritional multiplier without changing food history or item values.
     *
     * @param cir return value for the food modifier
     */
    @Inject(
        method = "getFoodModifier(Lsqueek/spiceoflife/foodtracker/FoodHistory;Lnet/minecraft/item/ItemStack;)F",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private static void gtnotgood$fullFoodValue(CallbackInfoReturnable<Float> cir) {
        Config.ensureLoaded();
        if (Config.disableSpiceOfLifeDiminishingReturns) {
            cir.setReturnValue(1.0F);
        }
    }
}
