// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.late.appliedenergistics.wireless;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtnotgood.client.wirelessfrequency.WirelessCardInput;

import appeng.client.render.NetworkVisualiserRender;
import appeng.items.tools.ToolNetworkVisualiser.VLink;
import appeng.items.tools.ToolNetworkVisualiser.VNode;

@Mixin(value = NetworkVisualiserRender.class, remap = false)
public abstract class MixinWirelessVisualiserRender {

    @Inject(method = "networkVisualiser", at = @At("HEAD"))
    private static void gtng$invalidateSnapshot(ArrayList<VNode> nodes, ArrayList<VLink> links, CallbackInfo ci) {
        WirelessCardInput.clearVisualisation();
    }

    @ModifyExpressionValue(
        method = "renderNetwork",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;",
            remap = true))
    private ItemStack gtng$cardVisualiser(ItemStack held) {
        return WirelessCardInput.visualiserStack(held);
    }
}
