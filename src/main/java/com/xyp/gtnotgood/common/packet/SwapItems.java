package com.xyp.gtnotgood.common.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.items.toolbelt.ConfigData;
import com.xyp.gtnotgood.common.items.toolbelt.ToolBeltData;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Requests a server-authoritative swap between the held item and a tool belt slot.
 */
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

    /**
     * Handles tool belt swap packets on the logical server.
     */
    public static class Handler implements IMessageHandler<SwapItems, IMessage> {

        @Override
        public IMessage onMessage(SwapItems message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            swapItem(message.swapWith, player);
            return null;
        }
    }

    public static void swapItem(int swapWith, EntityPlayer player) {
        ToolBeltData data = ToolBeltData.get(player);
        if (data == null) {
            ToolBeltData.register(player);
            data = ToolBeltData.get(player);
        }
        if (data == null) return;

        ItemStack inHand = player.getHeldItem();
        int size = ToolBeltData.SLOT_COUNT;

        if (swapWith < 0) {
            if (inHand == null) return;
            if (!ConfigData.isItemStackAllowed(inHand)) return;

            for (int i = 0; i < size; i++) {
                ItemStack inSlot = data.getStackInSlot(i);
                if (inSlot != null && inSlot.isItemEqual(inHand) && ItemStack.areItemStackTagsEqual(inSlot, inHand)) {
                    int max = inSlot.getMaxStackSize();
                    int acc = inSlot.stackSize + inHand.stackSize;
                    if (acc <= max) {
                        inSlot.stackSize = acc;
                        data.setStackInSlot(i, inSlot);
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                        break;
                    } else {
                        inSlot.stackSize = max;
                        data.setStackInSlot(i, inSlot);
                        inHand.stackSize = acc - max;
                        player.inventory.setInventorySlotContents(
                            player.inventory.currentItem,
                            inHand.stackSize > 0 ? inHand : null);
                    }
                } else if (inSlot == null) {
                    data.setStackInSlot(i, inHand.copy());
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                    break;
                }
            }
        } else {
            if (swapWith >= size) return;

            ItemStack inSlot = data.getStackInSlot(swapWith);

            if (inHand != null) {
                if (!ConfigData.isItemStackAllowed(inHand)) return;
                data.setStackInSlot(swapWith, inHand.copy());
            } else {
                if (inSlot == null) return;
                data.setStackInSlot(swapWith, null);
            }
            player.inventory.setInventorySlotContents(player.inventory.currentItem, inSlot);
        }

        data.syncToTracking();
    }
}
