// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.early.wirelessfrequency;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.wirelessfrequency.IWirelessTileData;

@Mixin(TileEntity.class)
public class MixinWirelessTileData implements IWirelessTileData {

    @Unique
    private NBTTagCompound gtng$wirelessData;

    @Override
    public NBTTagCompound gtng$wirelessData() {
        if (gtng$wirelessData == null) gtng$wirelessData = new NBTTagCompound();
        return gtng$wirelessData;
    }

    @Override
    public boolean gtng$hasWirelessData() {
        return gtng$wirelessData != null && gtng$wirelessData.hasKey(KEY, 10);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void gtng$readWireless(NBTTagCompound data, CallbackInfo ci) {
        gtng$wirelessData = null;
        if (data.hasKey(KEY, 10)) gtng$wirelessData().setTag(KEY, data.getCompoundTag(KEY).copy());
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void gtng$writeWireless(NBTTagCompound data, CallbackInfo ci) {
        if (gtng$wirelessData != null && gtng$wirelessData.hasKey(KEY, 10)) {
            data.setTag(KEY, gtng$wirelessData.getCompoundTag(KEY).copy());
        } else {
            data.removeTag(KEY);
        }
    }
}
