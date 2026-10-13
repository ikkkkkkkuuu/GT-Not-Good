// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.network.wirelessfrequency;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessAutoConnect;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class OpenWirelessCardPacket implements IMessage {

    public OpenWirelessCardPacket() {}

    @Override
    public void fromBytes(ByteBuf buffer) {
        if (buffer.isReadable()) throw new IllegalArgumentException("Unexpected wireless card payload");
    }

    @Override
    public void toBytes(ByteBuf buffer) {}

    public static final class Handler implements IMessageHandler<OpenWirelessCardPacket, IMessage> {

        @Override
        public IMessage onMessage(OpenWirelessCardPacket message, MessageContext context) {
            var player = context.getServerHandler().playerEntity;
            ServerThreadUtil.addScheduledTask(() -> WirelessAutoConnect.openGui(player));
            return null;
        }
    }
}
