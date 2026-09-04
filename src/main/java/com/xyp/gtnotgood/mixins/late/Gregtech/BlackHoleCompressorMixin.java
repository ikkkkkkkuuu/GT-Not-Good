package com.xyp.gtnotgood.mixins.late.Gregtech;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.xyp.gtnotgood.config.MainConfig;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.common.tileentities.machines.multi.compressor.MTEBlackHoleCompressor;
import gregtech.common.tileentities.render.RenderingTileEntityBlackhole;

@Mixin(value = MTEBlackHoleCompressor.class, remap = false)
public abstract class BlackHoleCompressorMixin extends MTEExtendedPowerMultiBlockBase<MTEBlackHoleCompressor>
    implements ISurvivalConstructable {

    protected BlackHoleCompressorMixin(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    @Shadow
    private int collapseTimer = -1;

    @Shadow
    protected abstract void destroyRenderBlock();

    @Shadow
    private byte blackHoleStatus = 1;

    @Shadow
    private float blackHoleStability = 100;

    @Shadow
    private int catalyzingCostModifier = 1;

    @Shadow
    private boolean shouldRender = true;

    @Shadow
    private RenderingTileEntityBlackhole rendererTileEntity = null;

    @Shadow
    protected abstract boolean createRenderBlock();

    private static final Method PLAY_SOUND_METHOD;

    static {
        Method method = null;
        if (FMLLaunchHandler.side()
            .isClient()) {
            try {
                method = MTEBlackHoleCompressor.class.getDeclaredMethod("playBlackHoleSounds");
                method.setAccessible(true);
            } catch (NoSuchMethodException ignored) {}
        }
        PLAY_SOUND_METHOD = method;
    }

    @Inject(method = "onPostTick", at = @At("HEAD"), cancellable = true)
    private void beforeOnPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick, CallbackInfo ci) {
        if (MainConfig.BlackHoleCompressorStabilityLock) {
            super.onPostTick(aBaseMetaTileEntity, aTick);

            if (collapseTimer != -1) {
                if (collapseTimer == 0) {
                    destroyRenderBlock();
                }
                collapseTimer = 1;
            }

            if (!aBaseMetaTileEntity.isServerSide()) {
                if (PLAY_SOUND_METHOD != null) {
                    try {
                        PLAY_SOUND_METHOD.invoke(this);
                    } catch (IllegalAccessException | InvocationTargetException ignored) {}
                }
                return;
            }

            if (blackHoleStatus == 1 || aTick % 20 != 0) return;

            float stabilityDecrease = 0F;
            boolean didDrain = false;

            if (blackHoleStability >= 0) {
                didDrain = false;
            } else blackHoleStatus = 3;

            if (shouldRender) {
                if (rendererTileEntity != null || createRenderBlock()) {
                    rendererTileEntity.toggleLaser(didDrain);
                    rendererTileEntity.setStability(Math.max(0, blackHoleStability / 100F));
                }
            }

            blackHoleStability -= stabilityDecrease;

            if (blackHoleStability <= -900) {
                blackHoleStatus = 1;
                blackHoleStability = 100;
                catalyzingCostModifier = 1;
                rendererTileEntity = null;
                destroyRenderBlock();
            }

            ci.cancel();
        }
    }

    private float parseFloatConfig(String value, float defaultValue) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            System.err.println("配置文件中数据异常：" + value + "，将使用默认值：" + defaultValue);
            return defaultValue;
        }
    }
}
