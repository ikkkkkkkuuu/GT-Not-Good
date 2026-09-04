package com.xyp.gtnotgood.ae2thing.network;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

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

/**
 * Sent from the client when the player presses the "send held item to network" keybind. The server locates any wireless
 * terminal in the player's main inventory or baubles slots, resolves its ME network and injects the currently held
 * stack into network storage. Mirrors the extraction path of {@link CPacketInventoryActionExtend}'s REQUEST_ITEM.
 */
public class CPacketSendHeldItemToNetwork implements IMessage {

    private int slot;

    public CPacketSendHeldItemToNetwork() {}

    public CPacketSendHeldItemToNetwork(final int slot) {
        this.slot = slot;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(slot);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slot = buf.readInt();
    }

    public static class Handler implements IMessageHandler<CPacketSendHeldItemToNetwork, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketSendHeldItemToNetwork message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (message.slot < 0 || message.slot >= player.inventory.mainInventory.length) {
                return null;
            }

            final ItemStack held = player.inventory.mainInventory[message.slot];
            if (held == null || held.getItem() == null || held.stackSize <= 0) {
                return null;
            }

            /*
             * Never use the live inventory ItemStack for accounting after calling an external storage handler.
             * Some special items/storage handlers can mutate stack/NBT objects during insertion.
             */
            final ItemStack original = held.copy();
            final int originalCount = original.stackSize;

            IAEItemStack remaining = AEItemStack.create(original.copy());
            if (remaining == null || remaining.getStackSize() <= 0) {
                return null;
            }

            java.util.List<ItemStack> terminals = InvUtil.matcher(
                player,
                stack -> stack != null && stack.getItem() instanceof ItemWirelessDualInterfaceTerminal);

            if (terminals.isEmpty()) {
                terminals = InvUtil
                    .matcher(player, stack -> stack != null && stack.getItem() instanceof IWirelessTermHandler);
            }

            for (ItemStack terminal : terminals) {
                if (remaining.getStackSize() <= 0) {
                    break;
                }

                try {
                    WirelessObject object = new WirelessObject(terminal, player.worldObj, message.slot, 0, 0, player);

                    if (!object.rangeCheck()) {
                        continue;
                    }

                    /*
                     * First ask how much this inventory can accept. Then MODULATE only that amount and calculate
                     * the actual inserted count from the returned remainder. The player slot is reduced only by
                     * the amount AE2 confirms was inserted.
                     */
                    IAEItemStack simulatedOffer = remaining.copy();
                    long offered = simulatedOffer.getStackSize();

                    IAEItemStack simulatedRemainder = object.getItemInventory()
                        .injectItems(simulatedOffer, Actionable.SIMULATE, object.getSource());

                    long simulatedLeft = simulatedRemainder == null ? 0
                        : Math.max(0, Math.min(offered, simulatedRemainder.getStackSize()));
                    long canInsert = offered - simulatedLeft;

                    if (canInsert <= 0) {
                        continue;
                    }

                    IAEItemStack actualOffer = remaining.copy();
                    actualOffer.setStackSize(canInsert);

                    IAEItemStack actualRemainder = object.getItemInventory()
                        .injectItems(actualOffer, Actionable.MODULATE, object.getSource());

                    long actualLeft = actualRemainder == null ? 0
                        : Math.max(0, Math.min(canInsert, actualRemainder.getStackSize()));
                    long inserted = canInsert - actualLeft;

                    if (inserted > 0) {
                        remaining.decStackSize(inserted);
                    }

                    // One key press must never distribute the stack across unrelated wireless networks.
                    break;
                } catch (Exception ignored) {}
            }

            final long remainingLong = Math.max(0, Math.min(originalCount, remaining.getStackSize()));
            final int remainingCount = (int) remainingLong;
            final int storedCount = originalCount - remainingCount;

            if (storedCount <= 0) {
                return null;
            }

            if (remainingCount <= 0) {
                player.inventory.setInventorySlotContents(message.slot, null);
            } else {
                ItemStack updated = original.copy();
                updated.stackSize = remainingCount;
                player.inventory.setInventorySlotContents(message.slot, updated);
            }

            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
            return null;
        }
    }
}
