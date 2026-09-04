package com.xyp.gtnotgood.common.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 同步矿脉挖掘镐NBT数据到服务器
 * Sync Vein Mining Pickaxe NBT data to server
 */
public class SyncVeinPickaxeNBT implements IMessage {

    private int slot;
    private NBTTagCompound nbt;

    public SyncVeinPickaxeNBT() {}

    public SyncVeinPickaxeNBT(int slot, NBTTagCompound nbt) {
        this.slot = slot;
        this.nbt = nbt;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slot = buf.readInt();
        nbt = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(slot);
        ByteBufUtils.writeTag(buf, nbt);
    }

    public static class Handler implements IMessageHandler<SyncVeinPickaxeNBT, IMessage> {

        @Override
        public IMessage onMessage(SyncVeinPickaxeNBT message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            if (message.slot >= 0 && message.slot < player.inventory.mainInventory.length) {
                ItemStack stack = player.inventory.mainInventory[message.slot];
                if (stack != null) {
                    stack.setTagCompound(message.nbt);
                }
            }

            return null;
        }
    }
}
