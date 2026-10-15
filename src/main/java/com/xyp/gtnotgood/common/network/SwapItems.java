package com.xyp.gtnotgood.common.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.xyp.gtnotgood.common.items.toolbelt.ConfigData;
import com.xyp.gtnotgood.common.items.toolbelt.ToolBeltData;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SwapItems implements IMessage {

    private int swapWith;

    public SwapItems() {}

    public SwapItems(int swapWith) {
        this.swapWith = swapWith;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        swapWith = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(swapWith);
    }

    public static class Handler implements IMessageHandler<SwapItems, IMessage> {

        @Override
        public IMessage onMessage(SwapItems message, MessageContext ctx) {
            ServerThreadUtil.addScheduledTask(() -> swapItem(message.swapWith, ctx.getServerHandler().playerEntity));
            return null;
        }
    }

    public static void swapItem(int swapWith, EntityPlayer player) {
        if (player.worldObj.isRemote) return;
        ToolBeltData data = ToolBeltData.get(player);
        if (data == null) {
            ToolBeltData.register(player);
            data = ToolBeltData.get(player);
        }
        if (data == null) return;

        ItemStack inHand = player.getHeldItem();
        if (inHand != null && !ConfigData.isItemStackAllowed(inHand)) return;

        if (swapWith < 0) {
            if (swapWith != -1 || inHand == null) return;
            ItemStack remaining = data.tryInsert(inHand);
            if (remaining != null && remaining.stackSize == inHand.stackSize) return;
            player.inventory.setInventorySlotContents(player.inventory.currentItem, remaining);
        } else {
            if (swapWith >= data.getSlotCount()) return;
            ItemStack inSlot = data.getStackInSlot(swapWith);
            if (inHand == null) {
                if (inSlot == null) return;
                data.setStackInSlotSilent(swapWith, null);
                player.inventory.setInventorySlotContents(player.inventory.currentItem, inSlot);
            } else
                if (inSlot == null || inSlot.isItemEqual(inHand) && ItemStack.areItemStackTagsEqual(inSlot, inHand)) {
                    int existing = inSlot == null ? 0 : inSlot.stackSize;
                    int amount = Math.min(inHand.stackSize, Math.max(0, inHand.getMaxStackSize() - existing));
                    if (amount <= 0) return;
                    ItemStack inserted = inHand.copy();
                    inserted.stackSize = existing + amount;
                    data.setStackInSlotSilent(swapWith, inserted);
                    ItemStack remaining = inHand.copy();
                    remaining.stackSize -= amount;
                    player.inventory.setInventorySlotContents(player.inventory.currentItem,
                        remaining.stackSize > 0 ? remaining : null);
                } else {
                    if (inHand.stackSize > inHand.getMaxStackSize()) return;
                    data.setStackInSlotSilent(swapWith, inHand.copy());
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, inSlot);
                }
            data.onContentsChanged();
        }
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }
}
