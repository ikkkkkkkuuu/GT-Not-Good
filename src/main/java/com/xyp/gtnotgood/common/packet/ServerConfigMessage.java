package com.xyp.gtnotgood.common.packet;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.config.ServerConfigService;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Bounded scalar configuration request/reply; no file paths or reflective field names are accepted. */
public final class ServerConfigMessage implements IMessage {

    public long requestId;
    public int status;
    public Map<String, String> values = new LinkedHashMap<>();
    public Map<String, String> expected = new LinkedHashMap<>();

    public ServerConfigMessage() {}

    public ServerConfigMessage(long requestId, int status, Map<String, String> values) {
        this.requestId = requestId;
        this.status = status;
        this.values.putAll(values);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        requestId = buffer.readLong();
        status = buffer.readUnsignedByte();
        values = readMap(buffer);
        expected = readMap(buffer);
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing configuration data");
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(requestId);
        buffer.writeByte(status);
        writeMap(buffer, values);
        writeMap(buffer, expected);
    }

    private static Map<String, String> readMap(ByteBuf buffer) {
        int size = buffer.readUnsignedByte();
        if (size > 64) throw new IllegalArgumentException("Too many configuration entries");
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String key = readString(buffer);
            if (values.put(key, readString(buffer)) != null) throw new IllegalArgumentException("Duplicate entry");
        }
        return values;
    }

    private static String readString(ByteBuf buffer) {
        int size = buffer.readUnsignedShort();
        if (size > 512 || size > buffer.readableBytes()) throw new IllegalArgumentException("Invalid string size");
        byte[] bytes = new byte[size];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeMap(ByteBuf buffer, Map<String, String> values) {
        if (values.size() > 64) throw new IllegalArgumentException("Too many configuration entries");
        buffer.writeByte(values.size());
        values.forEach((key, value) -> {
            writeString(buffer, key);
            writeString(buffer, value);
        });
    }

    private static void writeString(ByteBuf buffer, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 512) throw new IllegalArgumentException("String too long");
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }

    /** Hands work to the server tick without touching configuration from the Netty thread. */
    public static final class ServerHandler implements IMessageHandler<ServerConfigMessage, IMessage> {

        @Override
        public IMessage onMessage(ServerConfigMessage message, MessageContext context) {
            ServerConfigService.enqueue(context.getServerHandler().playerEntity, message);
            return null;
        }
    }

    /** Uses the sided proxy so dedicated servers never resolve a Minecraft client class. */
    public static final class ClientHandler implements IMessageHandler<ServerConfigMessage, IMessage> {

        @Override
        public IMessage onMessage(ServerConfigMessage message, MessageContext context) {
            GTNotGood.proxy.receiveServerConfig(message);
            return null;
        }
    }
}
