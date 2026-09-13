package com.xyp.gtnotgood.mixins.late.AppliedEnergistics;

import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

/**
 * 让原版制作输入舱口在AE2中显示控制器的配方类别名称（例如“组装器”
 * 接口终端代替相邻机器块的图标名称（例如“大型蒸汽组装机”）。
 * <p>
 * 这与我们自己的{@code SuperMTEHatchCraftingInputME}所做的事情相呼应——而且——关键是——它成为了终端名称的来源
 * 遵循多方块<em>模式</em>：{@link MTEHatchCraftingInputMEMultiBlockNameMixin} 保留{@code mRecipeMap}
 * 与控制器当前的 {@code getRecipeMap（）} 在结构形式和所有模式开关上同步，以及 AE2 的
 * {@code ContainerInterfaceTerminal} 轮询{@code getRawName（）} 每次轮询，以便在重命名包发生变化后触发重命名包。
 * <p>
 * {@code mRecipeMap} 继承自 {@code MTEHatchInputBus}，除此之外未用于该舱口的输入
 * 过滤（它硬编码 {@code allowPutStack -> false} / {@code isValidSlot -> true}），所以填充它是安全的。
 * 当它为空（机器没有配方图或尚未形成的结构）时，我们会掉回原始
 * 图标-名称逻辑。
 */
@Mixin(value = MTEHatchCraftingInputME.class, remap = false)
public abstract class MTEHatchCraftingInputMENameMixin {

    // mRecipeMap is declared on the parent MTEHatchInputBus — access via cast to avoid shadow-of-inherited-field
    // warning.

    @Shadow
    public abstract boolean hasCustomName();

    @Shadow
    public abstract IChatComponent getNameSuffix();

    /**
     * Untranslated name sent to the client. Prefer the recipe map's category key so the terminal translates it to the
     * recipe-category name; AE2 handles the client-side translation of this raw key.
     */
    @Inject(method = "getRawName", at = @At("HEAD"), cancellable = true)
    private void gtnotgood$rawNameFromRecipeMap(CallbackInfoReturnable<String> cir) {
        RecipeMap<?> map = ((MTEHatchInputBus) (Object) this).mRecipeMap;
        if (hasCustomName() || map == null) return;
        String key = gtnotgood$recipeCategoryKey(map);
        if (key != null) cir.setReturnValue(key);
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void gtnotgood$nameFromRecipeMap(CallbackInfoReturnable<String> cir) {
        RecipeMap<?> map = ((MTEHatchInputBus) (Object) this).mRecipeMap;
        if (hasCustomName() || map == null) return;
        String key = gtnotgood$recipeCategoryKey(map);
        if (key == null) return;
        StringBuilder name = new StringBuilder(StatCollector.translateToLocal(key));
        IChatComponent suffix = getNameSuffix();
        if (suffix != null) name.append(suffix.getUnformattedText());
        cir.setReturnValue(name.toString());
    }

    private static String gtnotgood$recipeCategoryKey(RecipeMap<?> map) {
        if (map.getDefaultRecipeCategory() == null) return null;
        return map.getDefaultRecipeCategory().unlocalizedName;
    }
}
