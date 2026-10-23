// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.network.quantum;

import net.minecraft.client.Minecraft;

import com.xyp.gtnotgood.common.crafting.quantum.ae2.IQuantumCraftAmountGui;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class QuantumCraftingAvailabilityPacket implements IMessage {

    private boolean available;

    public QuantumCraftingAvailabilityPacket() {}

    public QuantumCraftingAvailabilityPacket(boolean available) {
        this.available = available;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.available = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.available);
    }

    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        minecraft.func_152344_a(() -> {
            if (minecraft.currentScreen instanceof IQuantumCraftAmountGui gui) {
                gui.gtng$setQuantumAvailable(this.available);
            }
        });
    }

    public static final class Handler implements IMessageHandler<QuantumCraftingAvailabilityPacket, IMessage> {

        @Override
        public IMessage onMessage(QuantumCraftingAvailabilityPacket message, MessageContext context) {
            message.handleClient(Minecraft.getMinecraft());
            return null;
        }
    }
}
