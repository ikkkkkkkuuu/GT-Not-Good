// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** The request contains only the hit position; selection, ownership and sneak state come from the server. */
public final class MessagePackagedConnector implements IMessage {

    private int x, y, z, face;

    public MessagePackagedConnector() {}

    public MessagePackagedConnector(int x, int y, int z, int face) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        x = buffer.readInt();
        y = buffer.readInt();
        z = buffer.readInt();
        face = buffer.readUnsignedByte();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(z);
        buffer.writeByte(face);
    }

    /** FML 1.7 messages are queued onto the server tick before inspecting the world. */
    public static final class Handler implements IMessageHandler<MessagePackagedConnector, IMessage> {

        @Override
        public IMessage onMessage(MessagePackagedConnector message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            var requestWorld = player.getServerForPlayer();
            PackagedServerActions.enqueue(() -> {
                ItemStack held = player.getHeldItem();
                var world = player.getServerForPlayer();
                if (world != requestWorld || player.isDead
                    || held == null
                    || !(held.getItem() instanceof ItemWirelessConnector)
                    || message.face > 5
                    || message.y < 0
                    || message.y >= world.getHeight()
                    || !world.getChunkProvider()
                        .chunkExists(message.x >> 4, message.z >> 4))
                    return;
                double reach = player.theItemInWorldManager.getBlockReachDistance() + 1;
                if (player.getDistanceSq(message.x + .5, message.y + .5, message.z + .5) > reach * reach
                    || MinecraftServer.getServer()
                        .isBlockProtected(world, message.x, message.y, message.z, player)
                    || !player.canPlayerEdit(message.x, message.y, message.z, message.face, held)) return;
                PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(
                    player,
                    PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
                    message.x,
                    message.y,
                    message.z,
                    message.face,
                    world);
                if (!event.isCanceled() && event.useItem != Event.Result.DENY) {
                    ItemWirelessConnector.use(player, message.x, message.y, message.z, message.face);
                    player.inventory.markDirty();
                    player.inventoryContainer.detectAndSendChanges();
                }
            });
            return null;
        }
    }
}
