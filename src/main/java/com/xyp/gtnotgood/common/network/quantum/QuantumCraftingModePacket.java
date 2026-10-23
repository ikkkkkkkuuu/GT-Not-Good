// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.network.quantum;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.crafting.quantum.ae2.QuantumCraftingMode;

import appeng.api.networking.IGrid;
import appeng.container.implementations.ContainerCraftAmount;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class QuantumCraftingModePacket implements IMessage {

    private boolean enabled;

    public QuantumCraftingModePacket() {}

    public QuantumCraftingModePacket(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.enabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.enabled);
    }

    public void handleServer(EntityPlayerMP player) {
        QuantumCraftingMode.setEnabled(player, this.enabled);
        if (!(player.openContainer instanceof ContainerCraftAmount container)) return;

        IGrid grid;
        try {
            grid = container.getGrid();
        } catch (RuntimeException e) {
            grid = null;
        }
        GTNotGood.channel.sendTo(new QuantumCraftingAvailabilityPacket(QuantumCraftingMode.isAvailable(grid)), player);
    }

    public static final class Handler implements IMessageHandler<QuantumCraftingModePacket, IMessage> {

        @Override
        public IMessage onMessage(QuantumCraftingModePacket message, MessageContext context) {
            var player = context.getServerHandler().playerEntity;
            ServerThreadUtil.addScheduledTask(() -> message.handleServer(player));
            return null;
        }
    }
}
