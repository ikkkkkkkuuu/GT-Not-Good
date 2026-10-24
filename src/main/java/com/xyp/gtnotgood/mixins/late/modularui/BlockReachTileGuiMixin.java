package com.xyp.gtnotgood.mixins.late.modularui;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.factory.TileEntityGuiFactory;
import com.xyp.gtnotgood.config.InteractionConfig;

@Mixin(value = TileEntityGuiFactory.class, remap = false)
public abstract class BlockReachTileGuiMixin {

    @ModifyConstant(
        method = "canInteractWith(Lnet/minecraft/entity/player/EntityPlayer;"
            + "Lcom/cleanroommc/modularui/factory/PosGuiData;)Z",
        constant = @Constant(doubleValue = 64.0D),
        require = 1,
        expect = 1,
        allow = 1)
    private double gtng$containerDistance(double original, EntityPlayer player, PosGuiData guiData) {
        return InteractionConfig.getContainerDistanceSquared(player);
    }
}
