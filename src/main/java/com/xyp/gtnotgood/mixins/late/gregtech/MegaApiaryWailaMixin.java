package com.xyp.gtnotgood.mixins.late.gregtech;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialApiary;
import com.xyp.gtnotgood.utils.machine.ApiaryWailaPreview;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import kubatech.tileentity.gregtech.multiblock.MTEMegaIndustrialApiary;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class MegaApiaryWailaMixin {

    @Unique
    private boolean gtng$isApiary() {
        Object machine = this;
        return machine instanceof MTEMegaIndustrialApiary || machine instanceof LargeIndustrialApiary;
    }

    @ModifyExpressionValue(
        method = "getWailaNBTData",
        at = @At(
            value = "FIELD",
            target = "Lgregtech/api/metatileentity/implementations/MTEMultiBlockBase;mOutputItems:[Lnet/minecraft/item/ItemStack;"),
        require = 2,
        expect = 2)
    private ItemStack[] gtng$limitItemPreview(ItemStack[] outputs,
        @Share("gtng$itemPreview") LocalRef<ApiaryWailaPreview<ItemStack>> previewRef) {
        if (!gtng$isApiary()) return outputs;
        ApiaryWailaPreview<ItemStack> preview = previewRef.get();
        if (preview == null) {
            preview = ApiaryWailaPreview.of(outputs);
            previewRef.set(preview);
        }
        return preview.outputs();
    }

    @Inject(method = "getWailaNBTData", at = @At("RETURN"), require = 1)
    private void gtng$preserveOutputTotals(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world,
        int x, int y, int z, CallbackInfo ci,
        @Share("gtng$itemPreview") LocalRef<ApiaryWailaPreview<ItemStack>> itemPreviewRef) {
        ApiaryWailaPreview<ItemStack> items = itemPreviewRef.get();
        if (items != null && items.totalCount() > 0) tag.setInteger("outputItemLength", items.totalCount());
    }
}
