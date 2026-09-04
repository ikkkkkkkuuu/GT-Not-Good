package com.xyp.gtnotgood.common.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.items.toolbelt.ToolBeltData;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Synchronizes a player's server-side tool belt inventory to tracking clients.
 */
public class SyncToolBeltData implements IMessage {

    private int entityId;
    private ItemStack[] items;

    public SyncToolBeltData() {}

    public SyncToolBeltData(Entity entity, ToolBeltData data) {
        this.entityId = entity.getEntityId();
        this.items = new ItemStack[ToolBeltData.SLOT_COUNT];
        for (int i = 0; i < ToolBeltData.SLOT_COUNT; i++) {
            this.items[i] = data.getStackInSlot(i);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        int count = buf.readInt();
        items = new ItemStack[ToolBeltData.SLOT_COUNT];
        for (int i = 0; i < count; i++) {
            int slot = buf.readInt();
            if (slot >= 0 && slot < ToolBeltData.SLOT_COUNT) {
                items[slot] = ByteBufUtils.readItemStack(buf);
            } else {
                ByteBufUtils.readItemStack(buf);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        int count = 0;
        for (ItemStack item : items) {
            if (item != null) {
                count++;
            }
        }
        buf.writeInt(count);
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                buf.writeInt(i);
                ByteBufUtils.writeItemStack(buf, items[i]);
            }
        }
    }

    /**
     * Applies tool belt sync packets on the client thread.
     */
    public static class Handler implements IMessageHandler<SyncToolBeltData, IMessage> {

        @Override
        public IMessage onMessage(SyncToolBeltData message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(new Runnable() {

                    @Override
                    public void run() {
                        Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(message.entityId);
                        if (entity instanceof EntityLivingBase) {
                            ToolBeltData data = ToolBeltData.get((EntityLivingBase) entity);
                            if (data != null) {
                                for (int i = 0; i < ToolBeltData.SLOT_COUNT; i++) {
                                    data.setStackInSlotSilent(i, message.items[i]);
                                }
                            }
                        }
                    }
                });
            return null;
        }
    }
}
