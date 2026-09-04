package com.xyp.gtnotgood.ae2thing.network;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.ae2thing.api.InventoryActionExtend;
import com.xyp.gtnotgood.ae2thing.api.WirelessObject;
import com.xyp.gtnotgood.ae2thing.common.item.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtnotgood.ae2thing.util.InvUtil;

import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketInventoryActionExtend implements IMessage {

    private InventoryActionExtend action;
    private int slot;
    private long id;
    private IAEItemStack stack;
    private boolean isEmpty;

    public CPacketInventoryActionExtend() {}

    public CPacketInventoryActionExtend(final InventoryActionExtend action, final int slot, final int id) {
        this(action, slot, id, null);
    }

    public CPacketInventoryActionExtend(final InventoryActionExtend action) {
        this(action, 0, 0, null);
    }

    public CPacketInventoryActionExtend(final InventoryActionExtend action, final int slot, final int id,
        IAEItemStack stack) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = stack;
        this.isEmpty = stack == null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action.ordinal());
        buf.writeInt(slot);
        buf.writeLong(id);
        buf.writeBoolean(isEmpty);
        if (!isEmpty) {
            try {
                stack.writeToPacket(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = InventoryActionExtend.values()[buf.readInt()];
        slot = buf.readInt();
        id = buf.readLong();
        isEmpty = buf.readBoolean();
        if (!isEmpty) {
            try {
                stack = AEItemStack.loadItemStackFromPacket(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Handler implements IMessageHandler<CPacketInventoryActionExtend, IMessage> {

        /**
         * Extracts the requested item and returns the exact AE2 stack that was removed.
         *
         * <p>
         * The old implementation only reduced {@code requestItem}, then put the client-side pick-block template
         * into the player's hand. That loses the actual stored stack's NBT/damage identity for machine blocks and
         * other special items.
         * </p>
         */
        private IAEItemStack extractItemFromME(EntityPlayer player, IAEItemStack requestItem, int slot) {
            if (requestItem == null || requestItem.getStackSize() <= 0) {
                return null;
            }

            IAEItemStack extractedTotal = null;

            List<ItemStack> items = InvUtil.matcher(
                player,
                stack -> stack != null && stack.getItem() instanceof ItemWirelessDualInterfaceTerminal);

            if (items.isEmpty()) {
                items = InvUtil
                    .matcher(player, stack -> stack != null && stack.getItem() instanceof IWirelessTermHandler);
            }

            for (ItemStack item : items) {
                if (requestItem.getStackSize() <= 0) {
                    break;
                }

                try {
                    WirelessObject object = new WirelessObject(item, player.worldObj, slot, 0, 0, player);
                    if (!object.rangeCheck()) {
                        continue;
                    }

                    /*
                     * Pass a copy to the storage monitor. A custom storage handler is allowed to mutate the stack
                     * object it receives; the authoritative remaining request must stay under our control.
                     */
                    IAEItemStack attempt = requestItem.copy();
                    IAEItemStack result = object.getItemInventory()
                        .extractItems(attempt, Actionable.MODULATE, object.getSource());

                    if (result == null || result.getStackSize() <= 0) {
                        continue;
                    }

                    long extracted = Math.min(requestItem.getStackSize(), result.getStackSize());
                    requestItem.decStackSize(extracted);

                    if (extractedTotal == null) {
                        extractedTotal = result.copy();
                        extractedTotal.setStackSize(extracted);
                    } else if (extractedTotal.isSameType(result)) {
                        extractedTotal.setStackSize(extractedTotal.getStackSize() + extracted);
                    }

                    // One key press belongs to one wireless terminal/network. Do not continue into another network.
                    break;
                } catch (Exception ignored) {}
            }

            return extractedTotal;
        }

        @Nullable
        @Override
        public IMessage onMessage(CPacketInventoryActionExtend message, MessageContext ctx) {
            final EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (message.action == InventoryActionExtend.REQUEST_ITEM && message.stack != null
                && message.slot >= 0
                && message.slot < sender.inventory.mainInventory.length
                && sender.inventory.mainInventory[message.slot] == null) {

                // id == 1 -> extract a single item (Shift+middle click); otherwise a full legal stack.
                ItemStack requestTemplate = message.stack.getItemStack();
                if (requestTemplate == null || requestTemplate.getItem() == null) {
                    return null;
                }

                long requestCount = message.id == 1 ? 1 : requestTemplate.getMaxStackSize();
                IAEItemStack requestItem = message.stack.copy();
                requestItem.setStackSize(requestCount);

                IAEItemStack extracted = extractItemFromME(sender, requestItem, message.slot);
                if (extracted == null || extracted.getStackSize() <= 0) {
                    return null;
                }

                /*
                 * Put the exact stack returned by AE2 into the player's hand. Do not reconstruct it from the
                 * client-side pick-block template, because that can drop or replace NBT on special machine blocks.
                 */
                ItemStack actualStack = extracted.getItemStack();
                actualStack.stackSize = (int) Math.min(Integer.MAX_VALUE, extracted.getStackSize());

                sender.inventory.setInventorySlotContents(message.slot, actualStack);
                sender.inventory.markDirty();
                sender.inventoryContainer.detectAndSendChanges();
                return null;
            }
            return null;
        }
    }

}
