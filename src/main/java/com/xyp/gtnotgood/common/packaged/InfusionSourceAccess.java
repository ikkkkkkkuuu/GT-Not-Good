// SPDX-License-Identifier: LGPL-3.0-only
// New GTNG TC4 integration (c) 2026 GTNG contributors.
package com.xyp.gtnotgood.common.packaged;

import net.minecraft.nbt.NBTTagCompound;

/** Implemented by the TC4 matrix mixin; stores a server-only, persistent direct-supply job reference. */
public interface InfusionSourceAccess {

    NBTTagCompound gtnotgood$getEssentiaSource();

    void gtnotgood$setEssentiaSource(NBTTagCompound source);
}
