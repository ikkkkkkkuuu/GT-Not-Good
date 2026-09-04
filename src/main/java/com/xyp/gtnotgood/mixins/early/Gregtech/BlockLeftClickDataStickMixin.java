package com.xyp.gtnotgood.mixins.early.Gregtech;

import net.minecraft.block.Block;
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
import com.xyp.gtnotgood.common.torcherino.tile.TileWirelessTorcherinoBase;
import com.xyp.gtnotgood.config.Config;

import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/**
 * Lets a GT Data Stick remember non-GregTech tile coordinates for wireless Torcherino binding.
 */
@Pseudo
@SuppressWarnings("UnusedMixin")
@Mixin(Block.class)
public abstract class BlockLeftClickDataStickMixin {

    @Inject(method = "onBlockClicked", at = @At("HEAD"))
    private void gtnotgood$onBlockClicked(World world, int x, int y, int z, EntityPlayer player, CallbackInfo ci) {
        if (world == null || world.isRemote || player == null || !Config.enableWirelessTorcherino) return;

        ItemStack held = player.getHeldItem();
        if (held == null || !ItemList.Tool_DataStick.isStackEqual(held, false, true)) return;

        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile == null || tile.isInvalid()) return;
        if (tile instanceof BaseMetaTileEntity || tile instanceof TileWirelessTorcherinoBase) return;

        BlockWirelessTorcherino.writeBindingToDataStick(world, x, y, z, player, held, tile);
    }
}
