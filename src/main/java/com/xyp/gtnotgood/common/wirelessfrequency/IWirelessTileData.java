// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.wirelessfrequency;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public interface IWirelessTileData {

    String KEY = "GTNGWirelessLinks";

    NBTTagCompound gtng$wirelessData();

    boolean gtng$hasWirelessData();

    static NBTTagCompound of(TileEntity tile) {
        return ((IWirelessTileData) tile).gtng$wirelessData();
    }
}
