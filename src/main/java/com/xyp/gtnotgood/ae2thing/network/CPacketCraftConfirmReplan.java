package com.xyp.gtnotgood.ae2thing.network;

import com.xyp.gtnotgood.ae2thing.util.Util;

import appeng.container.implementations.ContainerCraftConfirm;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class CPacketCraftConfirmReplan implements IMessage {

    @Override
    public void fromBytes(ByteBuf buffer) {}

    @Override
    public void toBytes(ByteBuf buffer) {}

    public static final class Handler implements IMessageHandler<CPacketCraftConfirmReplan, IMessage> {

        @Override
        public IMessage onMessage(CPacketCraftConfirmReplan message, MessageContext context) {
            if (context.getServerHandler().playerEntity.openContainer instanceof ContainerCraftConfirm container) {
                Util.replan(context.getServerHandler().playerEntity, container);
            }
            return null;
        }
    }
}
