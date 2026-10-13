// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.network.wirelessfrequency;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.client.wirelessfrequency.WirelessCardInput;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardBinding;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardVisualisation;

import appeng.core.sync.AppEngPacket;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class WirelessVisualisationPacket implements IMessage {

    private NBTTagCompound bindingTag;
    private int dimension;
    private byte[] payload;

    public WirelessVisualisationPacket() {}

    public WirelessVisualisationPacket(NBTTagCompound tag, int dimension, AppEngPacket packet) {
        bindingTag = new NBTTagCompound();
        WirelessCardBinding binding = WirelessCardBinding.read(tag);
        if (binding == null) throw new IllegalArgumentException("Unbound wireless frequency card");
        binding.write(bindingTag);
        this.dimension = dimension;
        ByteBuf nativePayload = packet.getProxy().payload();
        try {
            int size = nativePayload.readableBytes() - 4;
            if (size < 0 || size > WirelessCardVisualisation.MAX_PACKET_BYTES) {
                payload = new byte[0];
            } else {
                payload = new byte[size];
                nativePayload.getBytes(nativePayload.readerIndex() + 4, payload);
            }
        } finally {
            nativePayload.release();
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        bindingTag = ByteBufUtils.readTag(buffer);
        dimension = buffer.readInt();
        int size = buffer.readInt();
        if (
            WirelessCardBinding.read(bindingTag) == null || size < 0
                || size > WirelessCardVisualisation.MAX_PACKET_BYTES
                || size > buffer.readableBytes()
        ) throw new IllegalArgumentException("Invalid wireless visualisation snapshot");
        payload = new byte[size];
        buffer.readBytes(payload);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeTag(buffer, bindingTag);
        buffer.writeInt(dimension);
        buffer.writeInt(payload.length);
        buffer.writeBytes(payload);
    }

    public static final class Handler implements IMessageHandler<WirelessVisualisationPacket, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(WirelessVisualisationPacket message, MessageContext context) {
            Minecraft minecraft = Minecraft.getMinecraft();
            var connection = minecraft.getNetHandler();
            minecraft.func_152344_a(() -> {
                if (minecraft.getNetHandler() == connection) WirelessCardInput.receiveVisualisation(minecraft,
                    WirelessCardBinding.read(message.bindingTag), message.dimension, message.payload);
            });
            return null;
        }
    }
}
