package com.xyp.gtnotgood.mixins.early.interaction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtnotgood.config.InteractionConfig;

@Mixin({ TileEntityChest.class, TileEntityFurnace.class, TileEntityHopper.class, TileEntityDispenser.class,
    TileEntityBrewingStand.class, TileEntityBeacon.class })
public abstract class BlockReachInventoryMixin {

    @ModifyConstant(
        method = "isUseableByPlayer(Lnet/minecraft/entity/player/EntityPlayer;)Z",
        constant = @Constant(doubleValue = 64.0),
        require = 1,
        expect = 1,
        allow = 1)
    private double gtnotgood$containerDistance(double original, EntityPlayer player) {
        return Math.max(original, InteractionConfig.getContainerDistanceSquared(player));
    }
}
