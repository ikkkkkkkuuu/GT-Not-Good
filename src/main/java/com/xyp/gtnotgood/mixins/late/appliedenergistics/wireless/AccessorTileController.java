// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.late.appliedenergistics.wireless;

import org.spongepowered.asm.mixin.Mixin;

import appeng.tile.networking.TileController;

@Mixin(value = TileController.class, remap = false)
public interface AccessorTileController {
}
