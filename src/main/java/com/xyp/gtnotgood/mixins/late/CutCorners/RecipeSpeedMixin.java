package com.xyp.gtnotgood.mixins.late.CutCorners;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;

import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.util.GTRecipe;

/**
 * 全局配方提速（仿 GTNH-CutCorners）。
 *
 * <p>
 * <b>原理</b>：所有 GregTech 配方在注册时都会经过 {@link RecipeMapBackend#compileRecipe(GTRecipe)}
 * 这一唯一中心入口。在它的 HEAD 拦一下、按 {@link Config#getModifiedRecipeDuration(int)} 改写传入配方的
 * {@code mDuration}（公开字段，此时配方尚未入库），即等效「一处改动，全部机器配方提速」。
 *
 * <p>
 * <b>维护性</b>：只 hook 这一个稳定的配方系统入口，而非给每台机器单独写 mixin。上游(GT5U 594 → 2.9)
 * 该方法签名一致；若上游变动，只需改本文件一处的注入目标。由 {@code LateMixinsLoader} 在 gregtech 加载时注册。
 *
 * <p>
 * <b>作用范围</b>：仅修改配方时长(mDuration)，不做全 LV 化/EOH/研究站等特化——保持简洁。
 * mode=0 时原样返回，本 mixin 实际不改任何值（零开销）。
 */
@Mixin(value = RecipeMapBackend.class, remap = false)
public class RecipeSpeedMixin {

    @Inject(method = "compileRecipe", at = @At("HEAD"))
    private void gtnotgood$modifyRecipeDuration(GTRecipe recipe, CallbackInfoReturnable<GTRecipe> cir) {
        Config.ensureLoaded();
        if (Config.recipeSpeedMode == 0 || recipe == null) return;
        recipe.mDuration = Config.getModifiedRecipeDuration(recipe.mDuration);
    }
}
