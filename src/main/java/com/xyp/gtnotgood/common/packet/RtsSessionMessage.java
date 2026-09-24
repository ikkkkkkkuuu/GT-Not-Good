package com.xyp.gtnotgood.common.packet;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.rts.session.RtsSessionLease;
import com.xyp.gtnotgood.common.rts.session.RtsSessionManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Fixed-size session protocol on the existing GTNG channel. Requests never contain camera bounds,
 * inventory stacks or a world mutation. Open/close are explicit to avoid duplicate-toggle races.
 */
public final class RtsSessionMessage implements IMessage {

    public static final int OPEN = 0, CLOSE = 1, HEARTBEAT = 2, GRANTED = 3, CLOSED = 4, DENIED = 5;
    private static final int VERSION = 1;
    private static final int HEADER_BYTES = 22;
    public int action;
    public long requestId;
    public long token;
    public int dimension;
    public RtsSessionLease lease;

    public RtsSessionMessage() {}

    public static RtsSessionMessage control(int action, long requestId, long token, int dimension) {
        RtsSessionMessage message = new RtsSessionMessage();
        message.action = action;
        message.requestId = requestId;
        message.token = token;
        message.dimension = dimension;
        message.validate();
        return message;
    }

    public static RtsSessionMessage grant(RtsSessionLease lease) {
        RtsSessionMessage message = new RtsSessionMessage();
        message.action = GRANTED;
        message.requestId = lease.requestId;
        message.token = lease.token;
        message.dimension = lease.dimension;
        message.lease = lease;
        return message;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        if (buffer.readableBytes() < HEADER_BYTES || buffer.readUnsignedByte() != VERSION) {
            throw new IllegalArgumentException("Invalid RTS session header");
        }
        action = buffer.readUnsignedByte();
        requestId = buffer.readLong();
        token = buffer.readLong();
        dimension = buffer.readInt();
        int expected = action == GRANTED ? 28 : 0;
        if (buffer.readableBytes() != expected) throw new IllegalArgumentException("Invalid RTS session length");
        lease = action == GRANTED
            ? new RtsSessionLease(
                requestId,
                token,
                dimension,
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt())
            : null;
        validate();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        validate();
        buffer.writeByte(VERSION);
        buffer.writeByte(action);
        buffer.writeLong(requestId);
        buffer.writeLong(token);
        buffer.writeInt(dimension);
        if (lease != null) {
            buffer.writeDouble(lease.anchorX);
            buffer.writeDouble(lease.anchorY);
            buffer.writeDouble(lease.anchorZ);
            buffer.writeInt(lease.radius);
        }
    }

    private void validate() {
        if (action < OPEN || action > DENIED
            || requestId <= 0
            || (action == OPEN && token != 0)
            || (action == HEARTBEAT && token == 0)
            || (action == GRANTED) != (lease != null)) {
            throw new IllegalArgumentException("Invalid RTS session operation");
        }
    }

    /** The Netty thread only enqueues a bounded request; it never accesses the world or session map. */
    public static final class ServerHandler implements IMessageHandler<RtsSessionMessage, IMessage> {

        @Override
        public IMessage onMessage(RtsSessionMessage message, MessageContext context) {
            if (message.action <= HEARTBEAT) {
                RtsSessionManager.enqueue(context.getServerHandler().playerEntity, message);
            }
            return null;
        }
    }

    /** Common-side bridge deliberately contains no Minecraft client class references. */
    public static final class ClientHandler implements IMessageHandler<RtsSessionMessage, IMessage> {

        @Override
        public IMessage onMessage(RtsSessionMessage message, MessageContext context) {
            if (message.action >= GRANTED) GTNotGood.proxy.receiveRtsSession(message, context.netHandler);
            return null;
        }
    }
}
