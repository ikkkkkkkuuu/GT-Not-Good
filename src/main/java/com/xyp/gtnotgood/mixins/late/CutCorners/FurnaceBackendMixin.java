package com.xyp.gtnotgood.mixins.late.CutCorners;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;

import gregtech.api.recipe.maps.FurnaceBackend;
import gregtech.api.util.GTRecipe;

/**
 * 电力熔炉(Electric Furnace)冶炼提速——仿 GTNH-CutCorners。
 * <p>
 * 电炉配方不在注册期预置，而是运行期由 {@link FurnaceBackend#overwriteFindRecipe} 从原版熔炉配方动态生成
 * (固定 {@code .duration(128)})。这条路<b>不经过 compileRecipe</b>，故 {@code RecipeSpeedMixin} 覆盖不到，
 * 需在此单独 hook：在方法返回处按 {@link Config#getModifiedRecipeDuration(int)} 改写返回配方的 {@code mDuration}。
 * <p>
 * {@code require = 0}：方法签名不匹配时静默跳过、不崩。
 */
@Mixin(value = FurnaceBackend.class, remap = false)
public abstract class FurnaceBackendMixin {

    @Inject(method = "overwriteFindRecipe", at = @At("RETURN"), remap = false, require = 0)
    private void gtnc$modifyFurnaceDuration(ItemStack[] items, FluidStack[] fluids, @Nullable ItemStack specialSlot,
        @Nullable GTRecipe cachedRecipe, CallbackInfoReturnable<GTRecipe> cir) {
        if (Config.recipeSpeedMode == 0) {
            return;
        }
        GTRecipe recipe = cir.getReturnValue();
        if (recipe != null) {
            recipe.mDuration = Config.getModifiedRecipeDuration(recipe.mDuration);
        }
    }
}
