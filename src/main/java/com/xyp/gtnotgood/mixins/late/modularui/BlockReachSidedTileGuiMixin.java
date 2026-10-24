package com.xyp.gtnotgood.mixins.late.modularui;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.cleanroommc.modularui.factory.SidedPosGuiData;
import com.cleanroommc.modularui.factory.SidedTileEntityGuiFactory;
import com.xyp.gtnotgood.config.InteractionConfig;

@Mixin(value = SidedTileEntityGuiFactory.class, remap = false)
public abstract class BlockReachSidedTileGuiMixin {

    @ModifyConstant(
        method = "canInteractWith(Lnet/minecraft/entity/player/EntityPlayer;"
            + "Lcom/cleanroommc/modularui/factory/SidedPosGuiData;)Z",
        constant = @Constant(doubleValue = 64.0D),
        require = 1,
        expect = 1,
        allow = 1)
    private double gtng$containerDistance(double original, EntityPlayer player, SidedPosGuiData guiData) {
        return InteractionConfig.getContainerDistanceSquared(player);
    }
}
