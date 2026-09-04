package com.xyp.gtnotgood.mixins.minecraft;

import net.minecraft.tileentity.TileEntityFurnace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtnotgood.config.Config;

/**
 * 原版熔炉(TileEntityFurnace)冶炼提速——仿 GTNH-CutCorners。
 * <p>
 * 原版熔炉每次冶炼固定 200 tick(10 秒)，该常量在 {@code updateEntity} 里的 {@code furnaceCookTime == 200} 判断上。
 * 这里 {@link ModifyConstant} 把 200 按 {@link Config#getModifiedRecipeDuration(int)} 改写
 * (mode 0=不改 / 1=固定 / 2=倍率)，与 GT 机器共用同一套配方提速配置。
 * <p>
 * <b>必须走标准阶段(mixins.sciencenotcool.json)而非 late 阶段</b>：{@code TileEntityFurnace} 是原版类，
 * 游戏早期即被加载；late mixin 在原版类加载<b>之后</b>才应用，会抛
 * {@code MixinTargetAlreadyLoadedException: target ... was loaded too early}。标准阶段先于原版类加载，故安全。
 * <p>
 * 用 {@code require = 0}：若该常量因映射差异未命中，本注入静默跳过、不崩游戏，其余提速不受影响。
 */
@Mixin(TileEntityFurnace.class)
public abstract class VanillaFurnaceMixin {

    @ModifyConstant(method = "updateEntity", constant = @Constant(intValue = 200), require = 0)
    private int gtnc$modifyFurnaceCookTime(int constant) {
        if (Config.recipeSpeedMode == 0) {
            return constant;
        }
        return Config.getModifiedRecipeDuration(constant);
    }
}
