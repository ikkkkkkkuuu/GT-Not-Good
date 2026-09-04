package com.xyp.gtnotgood.mixins.late.Gregtech;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.torcherino.block.BlockWirelessTorcherino;
import com.xyp.gtnotgood.config.Config;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Lets a GT Data Stick remember GregTech machine coordinates for wireless Torcherino binding.
 */
@Pseudo
@SuppressWarnings("UnusedMixin")
@Mixin(targets = "gregtech.api.metatileentity.CommonMetaTileEntity", remap = false)
public abstract class GTMachineLeftClickDataStickMixin {

    @Inject(method = "onLeftclick", at = @At("HEAD"), remap = false)
    private void gtnotgood$onLeftclick(IGregTechTileEntity baseMetaTileEntity, EntityPlayer player, CallbackInfo ci) {
        if (baseMetaTileEntity == null || baseMetaTileEntity.isClientSide()) return;
        if (player == null || !Config.enableWirelessTorcherino) return;

        ItemStack held = player.getHeldItem();
        if (held == null || !ItemList.Tool_DataStick.isStackEqual(held, false, true)) return;

        World world = baseMetaTileEntity.getWorld();
        if (world == null) return;

        int x = baseMetaTileEntity.getXCoord();
        int y = baseMetaTileEntity.getYCoord();
        int z = baseMetaTileEntity.getZCoord();
        TileEntity target = world.getTileEntity(x, y, z);
        if (target == null || target.isInvalid()) return;

        BlockWirelessTorcherino.writeBindingToDataStick(world, x, y, z, player, held, target);
    }
}
