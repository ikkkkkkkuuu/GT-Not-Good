// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.network.wirelessfrequency;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessAutoConnect;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class ToggleWirelessCardPacket implements IMessage {

    private boolean heldOnly;
    private int slot;
    private boolean visualisation;
    private boolean backwards;

    public ToggleWirelessCardPacket() {}

    public ToggleWirelessCardPacket(boolean heldOnly, int slot, boolean visualisation, boolean backwards) {
        this.heldOnly = heldOnly;
        this.slot = slot;
        this.visualisation = visualisation;
        this.backwards = backwards;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        if (buffer.readableBytes() != 4) throw new IllegalArgumentException("Invalid wireless card payload length");
        heldOnly = buffer.readBoolean();
        slot = buffer.readByte();
        visualisation = buffer.readBoolean();
        backwards = buffer.readBoolean();
        if (
            heldOnly && (!visualisation || slot < 0 || slot > 8) || !heldOnly && slot != 0
                || !visualisation && backwards
        ) throw new IllegalArgumentException("Invalid wireless card action");
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(heldOnly);
        buffer.writeByte(slot);
        buffer.writeBoolean(visualisation);
        buffer.writeBoolean(backwards);
    }

    public static final class Handler implements IMessageHandler<ToggleWirelessCardPacket, IMessage> {

        @Override
        public IMessage onMessage(ToggleWirelessCardPacket message, MessageContext context) {
            var player = context.getServerHandler().playerEntity;
            ServerThreadUtil.addScheduledTask(() -> {
                if (message.visualisation)
                    WirelessAutoConnect.cycleVisualisation(player, message.heldOnly, message.slot, message.backwards);
                else WirelessAutoConnect.toggle(player);
            });
            return null;
        }
    }
}
