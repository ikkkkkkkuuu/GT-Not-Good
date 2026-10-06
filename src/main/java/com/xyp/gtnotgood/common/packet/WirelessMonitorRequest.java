package com.xyp.gtnotgood.common.packet;

import com.xyp.gtnotgood.common.wireless.monitor.WirelessMonitorService;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Requests the sender's own network; clients cannot supply another player's UUID. */
public final class WirelessMonitorRequest implements IMessage {

    public long requestId;

    public WirelessMonitorRequest() {}

    public WirelessMonitorRequest(long requestId) {
        this.requestId = requestId;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        requestId = buffer.readLong();
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing wireless monitor request data");
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(requestId);
    }

    public static final class Handler implements IMessageHandler<WirelessMonitorRequest, IMessage> {

        @Override
        public IMessage onMessage(WirelessMonitorRequest message, MessageContext context) {
            WirelessMonitorService.enqueue(context.getServerHandler().playerEntity, message.requestId);
            return null;
        }
    }
}
