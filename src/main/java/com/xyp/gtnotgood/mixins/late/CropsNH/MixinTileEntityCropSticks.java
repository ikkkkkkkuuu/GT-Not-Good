package com.xyp.gtnotgood.mixins.late.CropsNH;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.cropsnh.api.ISeedData;
import com.gtnewhorizon.cropsnh.tileentity.TileEntityCropSticks;
import com.xyp.gtnotgood.config.Config;

/**
 * Applies the CropsNH crop-stick quality-of-life changes from the original project.
 */
@Mixin(TileEntityCropSticks.class)
public abstract class MixinTileEntityCropSticks {

    @Shadow(remap = false)
    private ISeedData seed;

    @Shadow(remap = false)
    private int growthProgress;

    @Shadow(remap = false)
    private boolean isDirty;

    @Shadow(remap = false)
    public abstract boolean hasCrop();

    @Shadow(remap = false)
    public abstract boolean hasWeed();

    @Shadow(remap = false)
    public abstract ItemStack getSeedStack();

    @ModifyConstant(method = "updateEntity", constant = @Constant(intValue = 256), remap = true)
    private int gtnotgood$modifyTickRateInUpdateEntity(int original) {
        return 1;
    }

    @Inject(method = "doGrowth", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtnotgood$instantGrowth(CallbackInfo ci) {
        Config.ensureLoaded();
        if (!Config.enableCropInstantGrowth) return;
        if (!this.hasCrop()) return;
        int duration = this.seed.getCrop()
            .getGrowthDuration();
        if (this.growthProgress < duration) {
            this.growthProgress = duration;
            this.isDirty = true;
        }
        ci.cancel();
    }

    @Inject(method = "spawnWeed", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtnotgood$disableSpawnWeed(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "spreadWeed", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtnotgood$disableSpreadWeed(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "getSeedDrop", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtnotgood$guaranteedSeedDrop(CallbackInfoReturnable<ItemStack> cir) {
        Config.ensureLoaded();
        if (!Config.enableCropGuaranteedSeedDrop) return;
        if (this.hasCrop() && !this.hasWeed()) {
            cir.setReturnValue(this.getSeedStack());
        }
    }
}
