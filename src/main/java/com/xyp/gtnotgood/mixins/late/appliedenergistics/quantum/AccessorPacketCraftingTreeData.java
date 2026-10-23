// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.mixins.late.appliedenergistics.quantum;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.core.sync.packets.PacketCraftingTreeData;
import io.netty.buffer.ByteBuf;

@Mixin(value = PacketCraftingTreeData.class, remap = false)
public interface AccessorPacketCraftingTreeData {

    @Invoker("<init>")
    static PacketCraftingTreeData gtng$create(ByteBuf chunkData, int chunkId, int totalChunks) {
        throw new AssertionError();
    }
}
