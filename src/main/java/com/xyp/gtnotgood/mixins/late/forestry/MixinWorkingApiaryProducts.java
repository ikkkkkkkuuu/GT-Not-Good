package com.xyp.gtnotgood.mixins.late.forestry;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xyp.gtnotgood.common.blocks.beekeeping.TileWorkingApiary;
import com.xyp.gtnotgood.common.blocks.beekeeping.WorkingApiaryProducts;
import com.xyp.gtnotgood.config.Config;

import forestry.api.apiculture.IBeeHousing;
import forestry.apiculture.genetics.Bee;

/** Separates ordinary and specialty quantities at Forestry's successful-roll insertion points. */
@Mixin(Bee.class)
public abstract class MixinWorkingApiaryProducts {

    @Redirect(
        method = "produceStacks",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0),
        remap = false,
        require = 1)
    private boolean gtnotgood$primary(List<ItemStack> products, Object product, IBeeHousing housing) {
        return gtnotgood$add(products, (ItemStack) product, housing, Config.workingApiaryProducts);
    }

    @Redirect(
        method = "produceStacks",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1),
        remap = false,
        require = 1)
    private boolean gtnotgood$secondary(List<ItemStack> products, Object product, IBeeHousing housing) {
        return gtnotgood$add(products, (ItemStack) product, housing, Config.workingApiaryProducts);
    }

    @Redirect(
        method = "produceStacks",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 2),
        remap = false,
        require = 1)
    private boolean gtnotgood$specialty(List<ItemStack> products, Object product, IBeeHousing housing) {
        return gtnotgood$add(products, (ItemStack) product, housing, Config.workingApiarySpecialties);
    }

    private static boolean gtnotgood$add(List<ItemStack> products, ItemStack product, IBeeHousing housing,
        float multiplier) {
        if (!(housing instanceof TileWorkingApiary)) return products.add(product);
        return WorkingApiaryProducts.add(products, product, multiplier, housing.getWorld().rand);
    }
}
