package com.xyp.gtnotgood.mixins.late.draconicevolution;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = "com.brandon3055.draconicevolution.common.items.armor.DraconicArmor", remap = false)
public abstract class DraconicArmorFlightSpeedMixin {

    @ModifyConstant(
        method = "getFields(Lnet/minecraft/item/ItemStack;I)Ljava/util/List;",
        constant = @Constant(floatValue = 6F),
        require = 1,
        allow = 1)
    private float gtnotgood$extendFlightSpeedLimit(float original) {
        return 10F;
    }
}
