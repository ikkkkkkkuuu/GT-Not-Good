package com.xyp.gtnotgood.ae2thing.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;

import com.xyp.gtnotgood.ae2thing.api.AE2ThingAPI;
import com.xyp.gtnotgood.ae2thing.api.Constants;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Packet dedicated to item inventory update.
 */
public class SPacketMEItemInvUpdate extends SPacketMEBaseInvUpdate implements IMessage {

    public SPacketMEItemInvUpdate() {
        super();
    }

    /**
     * Used for the GUI to confirm crafting. 0 = available 1 = pending 2 = missing
     */
    public SPacketMEItemInvUpdate(byte b) {
        super(b);
    }

    public SPacketMEItemInvUpdate(Constants.MessageType type) {
        this(type.type);
    }

    public void appendItem(final IAEItemStack is) {
        list.add(is);
    }

    public List<IAEItemStack> getItemStacks() {
        List<IAEItemStack> items = new ArrayList<>();
        for (IAEStack<?> stack : this.list) {
            if (stack instanceof IAEItemStack item) {
                items.add(item);
            }
        }
        return items;
    }

    public static class Handler implements IMessageHandler<SPacketMEItemInvUpdate, IMessage> {

        @Override
        public IMessage onMessage(SPacketMEItemInvUpdate message, MessageContext ctx) {
            final GuiScreen gs = Minecraft.getMinecraft().currentScreen;
            if (message.ref == Constants.MessageType.UPDATE_PLAYER_CURRENT_ITEM.type) {
                if (gs == null) {
                    Minecraft mc = Minecraft.getMinecraft();
                    EntityClientPlayerMP player = mc.thePlayer;
                    if (message.isEmpty() || !(message.list.get(0) instanceof IAEItemStack item)) return null;
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, item.getItemStack());
                }
            } else if (message.ref == Constants.MessageType.UPDATE_PINNED_ITEMS.type) {
                AE2ThingAPI.instance()
                    .getPinned()
                    .updatePinnedItems(message.getItemStacks());
            } else if (message.ref == Constants.MessageType.ADD_PINNED_ITEM.type) {
                if (!message.isEmpty() && message.list.get(0) instanceof IAEItemStack item) {
                    AE2ThingAPI.instance()
                        .getPinned()
                        .add(item);
                }
            }
            return null;
        }
    }
}
