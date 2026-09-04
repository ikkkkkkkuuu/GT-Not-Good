package com.xyp.gtnotgood.common.mebridge;

import java.util.Arrays;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/** Server-to-client snapshot containing only nodes linked by an ME Wireless Transceiver. */
public final class MessageMEWirelessVisualization implements IMessage {

    public static final int VALUES_PER_NODE = 4;

    private int dimension;
    private int[] positions = new int[0];

    public MessageMEWirelessVisualization() {}

    MessageMEWirelessVisualization(int dimension, int[] positions) {
        this.dimension = dimension;
        int length = positions == null ? 0
            : Math.min(positions.length / VALUES_PER_NODE, MEWirelessVisualizationSync.MAX_VISIBLE_NODES)
                * VALUES_PER_NODE;
        this.positions = length == 0 ? new int[0] : Arrays.copyOf(positions, length);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        dimension = buffer.readInt();
        int declaredCount = buffer.readUnsignedShort();
        int acceptedCount = Math.min(declaredCount, MEWirelessVisualizationSync.MAX_VISIBLE_NODES);
        positions = new int[acceptedCount * VALUES_PER_NODE];
        for (int index = 0; index < declaredCount; index++) {
            int x = buffer.readInt();
            int y = buffer.readInt();
            int z = buffer.readInt();
            int color = buffer.readInt();
            if (index < acceptedCount) {
                int offset = index * VALUES_PER_NODE;
                positions[offset] = x;
                positions[offset + 1] = y;
                positions[offset + 2] = z;
                positions[offset + 3] = color & 0xFFFFFF;
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(dimension);
        int count = positions.length / VALUES_PER_NODE;
        buffer.writeShort(count);
        for (int index = 0; index < count * VALUES_PER_NODE; index++) {
            buffer.writeInt(positions[index]);
        }
    }

    public static final class Handler implements IMessageHandler<MessageMEWirelessVisualization, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(MessageMEWirelessVisualization message, MessageContext context) {
            net.minecraft.client.Minecraft.getMinecraft()
                .func_152344_a(
                    () -> com.xyp.gtnotgood.client.mebridge.MEWirelessNodeRenderer
                        .update(message.dimension, message.positions));
            return null;
        }
    }
}
