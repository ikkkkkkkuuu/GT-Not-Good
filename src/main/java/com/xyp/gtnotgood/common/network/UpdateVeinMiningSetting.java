package com.xyp.gtnotgood.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.network.play.server.S2FPacketSetSlot;

import com.xyp.gtnotgood.common.items.veinmining.VeinMiningSettings;
import com.xyp.gtnotgood.common.items.veinmining.VeinMiningSettings.Setting;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Requests one bounded pickaxe setting; arbitrary item NBT is never accepted. */
public class UpdateVeinMiningSetting implements IMessage {

    private int slot;
    private Setting setting;
    private int value;

    public UpdateVeinMiningSetting() {}

    public UpdateVeinMiningSetting(int slot, Setting setting, int value) {
        this.slot = slot;
        this.setting = setting;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readableBytes() != 6) throw new IllegalArgumentException("Invalid pickaxe setting payload");
        slot = buf.readUnsignedByte();
        int settingId = buf.readUnsignedByte();
        if (settingId >= Setting.values().length) throw new IllegalArgumentException("Invalid pickaxe setting");
        setting = Setting.values()[settingId];
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(slot);
        buf.writeByte(setting.ordinal());
        buf.writeInt(value);
    }

    boolean apply(InventoryPlayer inventory) {
        return VeinMiningSettings.apply(inventory, slot, setting, value);
    }

    public static class Handler implements IMessageHandler<UpdateVeinMiningSetting, IMessage> {

        @Override
        public IMessage onMessage(UpdateVeinMiningSetting message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            if (!player.isDead && message.apply(player.inventory)) {
                player.inventoryContainer.detectAndSendChanges();
                if (message.setting.read(player.getHeldItem()) != message.value) {
                    // A server limit can differ from the client's predicted value even when the server slot did not
                    // change.
                    Slot slot = player.inventoryContainer.getSlotFromInventory(player.inventory, message.slot);
                    player.playerNetServerHandler.sendPacket(
                        new S2FPacketSetSlot(player.inventoryContainer.windowId, slot.slotNumber, slot.getStack()));
                }
            }

            return null;
        }
    }
}
