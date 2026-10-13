// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.late.appliedenergistics.wireless;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.network.wirelessfrequency.WirelessVisualisationPacket;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardVisualisation;

import appeng.api.util.DimensionalCoord;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.NetworkHandler;
import appeng.items.tools.ToolNetworkVisualiser;

@Mixin(value = ToolNetworkVisualiser.class, remap = false)
public abstract class MixinWirelessVisualiserTool {

    @WrapOperation(
        method = "onUpdate",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lappeng/items/tools/ToolNetworkVisualiser;needToUpdate("
                + "Lnet/minecraft/entity/player/EntityPlayerMP;Lappeng/api/util/DimensionalCoord;)Z",
            remap = false))
    private boolean gtng$refreshCadence(EntityPlayerMP player, DimensionalCoord position, Operation<Boolean> original,
        ItemStack stack, World world, Entity entity, int slot, boolean active) {
        return WirelessCardVisualisation.delegated(stack) ? WirelessCardVisualisation.needsUpdate(player, stack)
            : original.call(player, position);
    }

    @Redirect(
        method = "onUpdate",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/util/DimensionalCoord;isInWorld(Lnet/minecraft/world/World;)Z",
            remap = false))
    private boolean gtng$viewerDimension(DimensionalCoord position, World world, ItemStack stack, World currentWorld,
        Entity entity, int slot, boolean active) {
        return position.isInWorld(WirelessCardVisualisation.delegated(stack) ? entity.worldObj : world);
    }

    @Redirect(
        method = "onUpdate",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lappeng/core/sync/network/NetworkHandler;sendTo("
                + "Lappeng/core/sync/AppEngPacket;Lnet/minecraft/entity/player/EntityPlayerMP;)V",
            remap = false))
    private void gtng$cardSnapshot(NetworkHandler network, AppEngPacket message, EntityPlayerMP player, ItemStack stack,
        World world, Entity entity, int slot, boolean active) {
        if (WirelessCardVisualisation.delegated(stack)) {
            GTNotGood.channel.sendTo(new WirelessVisualisationPacket(stack.getTagCompound(), player.dimension, message),
                player);
        } else {
            network.sendTo(message, player);
        }
    }
}
