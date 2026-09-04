package com.xyp.gtnotgood.ae2thing.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.util.Util;
import com.xyp.gtnotgood.common.teleport.TeleportManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class CPacketTeleportToInterface implements IMessage {

    private long entryId;

    public CPacketTeleportToInterface() {}

    public CPacketTeleportToInterface(long entryId) {
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

    public static final class Handler implements IMessageHandler<CPacketTeleportToInterface, IMessage> {

        @Override
        public IMessage onMessage(CPacketTeleportToInterface message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            if (!(player.openContainer instanceof ContainerQuickEncodingTerminal container)) return null;

            Util.DimensionalCoordSide target = container.getTrackedInterfaceLocation(message.entryId);
            if (target != null) {
                TeleportManager.teleportNearInterface(player, target);
            }
            return null;
        }
    }
}
