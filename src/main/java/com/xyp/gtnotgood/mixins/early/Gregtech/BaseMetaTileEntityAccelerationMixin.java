package com.xyp.gtnotgood.mixins.early.Gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import com.xyp.gtnotgood.common.torcherino.api.ITileEntityTickAcceleration;
import com.xyp.gtnotgood.config.Config;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.CommonBaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.basic.MTEWorldAccelerator;
import gregtech.common.tileentities.machines.multi.MTEBrickedBlastFurnace;

/**
 * Adds a safe progress-bar acceleration hook to GregTech base tile entities for GT Not Good Torcherinos.
 */
@Pseudo
@SuppressWarnings("UnusedMixin")
@Mixin(targets = "gregtech.api.metatileentity.BaseMetaTileEntity", remap = false)
public abstract class BaseMetaTileEntityAccelerationMixin extends CommonBaseMetaTileEntity
    implements ITileEntityTickAcceleration {

    @Shadow(remap = false)
    public abstract int getProgress();

    @Shadow(remap = false)
    public abstract int getMaxProgress();

    @Shadow(remap = false)
    public abstract IMetaTileEntity getMetaTileEntity();

    @Shadow(remap = false)
    public abstract boolean isActive();

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public boolean tickAcceleration(int acceleratedTicks) {
        if (!isActive() || acceleratedTicks <= 0) return true;

        IMetaTileEntity metaTileEntity = getMetaTileEntity();
        if (metaTileEntity == null || metaTileEntity instanceof MTEWorldAccelerator) return true;

        int maxProgress = getMaxProgress();
        int currentProgress = getProgress();
        if (maxProgress < 2 || currentProgress < 0) return true;

        int discountedTicks = applyDiscount(acceleratedTicks);
        if (discountedTicks <= 0) return true;

        int acceleratedProgress = Math.min(maxProgress, currentProgress + discountedTicks);
        if (metaTileEntity instanceof MTEBasicMachine) {
            ((MTEBasicMachine) metaTileEntity).mProgresstime = acceleratedProgress;
            return true;
        }
        if (metaTileEntity instanceof MTEMultiBlockBase) {
            ((MTEMultiBlockBase) metaTileEntity).mProgresstime = acceleratedProgress;
            return true;
        }
        if (metaTileEntity instanceof MTEBrickedBlastFurnace) {
            ((MTEBrickedBlastFurnace) metaTileEntity).mProgresstime = acceleratedProgress;
            return true;
        }
        return true;
    }

    private static int applyDiscount(int acceleratedTicks) {
        float discount = Math.max(0.0F, Math.min(1.0F, Config.torcherinoGregTechAccelerationDiscount));
        int result = (int) (acceleratedTicks * discount);
        return result == 0 && discount > 0.0F ? 1 : result;
    }
}
