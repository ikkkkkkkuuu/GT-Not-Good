package com.xyp.gtnotgood.common.network;

import java.math.BigInteger;
import java.util.UUID;

import com.xyp.gtnotgood.GTNotGood;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Exact balance and server tick, with a request token to discard replies from an earlier HUD session. */
public final class WirelessMonitorSnapshot implements IMessage {

    private static final int MAX_ENERGY_BYTES = 4096;
    public long requestId;
    public long tick;
    public UUID owner;
    public BigInteger energy;

    public WirelessMonitorSnapshot() {}

    public WirelessMonitorSnapshot(long requestId, long tick, UUID owner, BigInteger energy) {
        this.requestId = requestId;
        this.tick = tick;
        this.owner = owner;
        this.energy = energy;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        requestId = buffer.readLong();
        tick = buffer.readLong();
        owner = new UUID(buffer.readLong(), buffer.readLong());
        int size = buffer.readUnsignedShort();
        if (size == 0 || size > MAX_ENERGY_BYTES || size != buffer.readableBytes()) {
            throw new IllegalArgumentException("Invalid wireless energy size");
        }
        byte[] bytes = new byte[size];
        buffer.readBytes(bytes);
        energy = new BigInteger(bytes);
        if (energy.signum() < 0) throw new IllegalArgumentException("Negative wireless energy");
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        byte[] bytes = energy.toByteArray();
        if (bytes.length > MAX_ENERGY_BYTES || energy.signum() < 0) {
            throw new IllegalArgumentException("Invalid wireless energy");
        }
        buffer.writeLong(requestId);
        buffer.writeLong(tick);
        buffer.writeLong(owner.getMostSignificantBits());
        buffer.writeLong(owner.getLeastSignificantBits());
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    public static final class Handler implements IMessageHandler<WirelessMonitorSnapshot, IMessage> {

        @Override
        public IMessage onMessage(WirelessMonitorSnapshot message, MessageContext context) {
            GTNotGood.proxy.receiveWirelessMonitor(message);
            return null;
        }
    }
}
