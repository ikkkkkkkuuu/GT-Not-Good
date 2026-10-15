package com.xyp.gtnotgood.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtnotgood.common.items.toolbelt.ToolBeltData;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class ResizeToolBelt implements IMessage {

    private int requestedCount;

    public ResizeToolBelt() {}

    public ResizeToolBelt(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        requestedCount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(requestedCount);
    }

    public static class Handler implements IMessageHandler<ResizeToolBelt, IMessage> {

        @Override
        public IMessage onMessage(ResizeToolBelt message, MessageContext ctx) {
            ServerThreadUtil
                .addScheduledTask(() -> resize(message.requestedCount, ctx.getServerHandler().playerEntity));
            return null;
        }
    }

    private static void resize(int requestedCount, EntityPlayerMP player) {
        ToolBeltData data = ToolBeltData.get(player);
        if (data == null) {
            ToolBeltData.register(player);
            data = ToolBeltData.get(player);
        }
        if (data == null) return;
        int previousCount = data.getSlotCount();
        boolean accepted = data.resize(requestedCount);
        if (!accepted) {
            if (requestedCount < ToolBeltData.MIN_SLOT_COUNT || requestedCount > ToolBeltData.MAX_SLOT_COUNT) {
                // #tr chat.gtnotgood.toolbelt.resize_range
                // # Tool belt size must be between %s and %s slots.
                // # zh_CN 工具腰带槽位数必须在 %s 到 %s 之间。
                player.addChatMessage(new ChatComponentTranslation("chat.gtnotgood.toolbelt.resize_range",
                    ToolBeltData.MIN_SLOT_COUNT, ToolBeltData.MAX_SLOT_COUNT));
            } else {
                // #tr chat.gtnotgood.toolbelt.resize_occupied
                // # Remove items from the slots being removed before shrinking the tool belt.
                // # zh_CN 请先取出被缩减槽位中的物品，再缩小工具腰带。
                player.addChatMessage(new ChatComponentTranslation("chat.gtnotgood.toolbelt.resize_occupied"));
            }
        }
        if (!accepted || previousCount == requestedCount) data.syncToSelf();
    }
}
