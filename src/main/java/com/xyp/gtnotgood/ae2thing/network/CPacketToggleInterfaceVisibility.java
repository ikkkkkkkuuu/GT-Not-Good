package com.xyp.gtnotgood.ae2thing.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class CPacketToggleInterfaceVisibility implements IMessage {

    private long entryId;

    public CPacketToggleInterfaceVisibility() {}

    public CPacketToggleInterfaceVisibility(long entryId) {
        this.entryId = entryId;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entryId = buffer.readLong();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(entryId);
    }

    public static final class Handler implements IMessageHandler<CPacketToggleInterfaceVisibility, IMessage> {

        @Override
        public IMessage onMessage(CPacketToggleInterfaceVisibility message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            if (!(player.openContainer instanceof ContainerQuickEncodingTerminal container)) return null;
            if (!container.toggleTrackedInterfaceVisibility(message.entryId)) {
                // #tr gtnotgood.message.interface_visibility.unsupported
                // # This interface does not support terminal visibility switching.
                // # zh_CN 该接口不支持切换终端可见性。
                player
                    .addChatMessage(new ChatComponentTranslation("gtnotgood.message.interface_visibility.unsupported"));
            }
            return null;
        }
    }
}
