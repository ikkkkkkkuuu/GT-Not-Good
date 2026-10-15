package com.xyp.gtnotgood.common.network;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.client.radialmenu.client.toolbelt.ToolBeltMenu;
import com.xyp.gtnotgood.common.items.toolbelt.ToolBeltData;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SyncToolBeltData implements IMessage {

    private int entityId;
    private ItemStack[] items;

    public SyncToolBeltData() {}

    public SyncToolBeltData(Entity entity, ToolBeltData data) {
        this.entityId = entity.getEntityId();
        this.items = new ItemStack[data.getSlotCount()];
        for (int i = 0; i < items.length; i++) {
            ItemStack stack = data.getStackInSlot(i);
            this.items[i] = stack == null ? null : stack.copy();
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        int count = buf.readInt();
        if (count < 0 || count > ToolBeltData.MAX_SLOT_COUNT) {
            throw new IllegalArgumentException("Invalid tool belt item count: " + count);
        }
        ItemStack[] restored = new ItemStack[ToolBeltData.MAX_SLOT_COUNT];
        int highestSlot = -1;
        for (int i = 0; i < count; i++) {
            int slot = buf.readInt();
            if (slot < 0 || slot >= restored.length) {
                throw new IllegalArgumentException("Invalid tool belt slot: " + slot);
            }
            restored[slot] = ByteBufUtils.readItemStack(buf);
            highestSlot = Math.max(highestSlot, slot);
        }
        int capacity = buf.isReadable() ? buf.readInt() : Math.max(ToolBeltData.DEFAULT_SLOT_COUNT, highestSlot + 1);
        if (
            capacity < ToolBeltData.MIN_SLOT_COUNT || capacity > ToolBeltData.MAX_SLOT_COUNT || highestSlot >= capacity
        ) {
            throw new IllegalArgumentException("Invalid tool belt capacity: " + capacity);
        }
        items = Arrays.copyOf(restored, capacity);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        int count = 0;
        for (ItemStack item : items) {
            if (item != null) count++;
        }
        buf.writeInt(count);
        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] == null) continue;
            buf.writeInt(slot);
            ByteBufUtils.writeItemStack(buf, items[slot]);
        }
        buf.writeInt(items.length);
    }

    public static class Handler implements IMessageHandler<SyncToolBeltData, IMessage> {

        @Override
        public IMessage onMessage(SyncToolBeltData message, MessageContext ctx) {
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.func_152344_a(() -> {
                if (minecraft.theWorld == null) return;
                Entity entity = minecraft.theWorld.getEntityByID(message.entityId);
                if (!(entity instanceof EntityLivingBase)) return;
                EntityLivingBase living = (EntityLivingBase) entity;
                ToolBeltData data = ToolBeltData.get(living);
                if (data == null) {
                    ToolBeltData.register(living);
                    data = ToolBeltData.get(living);
                }
                if (data == null) return;
                data.applySnapshot(message.items);
                if (entity == minecraft.thePlayer) ToolBeltMenu.onInventorySynced();
            });
            return null;
        }
    }
}
