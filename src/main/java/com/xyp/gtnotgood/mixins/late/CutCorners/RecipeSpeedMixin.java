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
 * <b>原理</b>：普通 GregTech 配方表通过 {@link RecipeMapBackend#compileRecipe(GTRecipe)} 注册配方。
 * 在它的 HEAD 按 {@link Config#getModifiedRecipeDuration(int)} 改写传入配方的 {@code mDuration}。
 * 装配线使用独立配方类型，由 {@link AssemblyLineSpeedMixin} 在控制器读取时单独提速。
 * 净化水线使用主控制器统一周期，由 {@link PurificationPlantSpeedMixin} 在启动周期时提速。
 * 怪物屠宰场使用独立怪物配方，由 {@link ExtremeEntityCrusherSpeedMixin} 在处理检查成功后提速。
 * 地热锅炉的批量模式由 {@link ThermalBoilerSpeedMixin} 约束最终固定时长。
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
