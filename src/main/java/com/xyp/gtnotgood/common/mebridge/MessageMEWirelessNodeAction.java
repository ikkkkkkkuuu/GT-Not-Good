package com.xyp.gtnotgood.common.mebridge;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Client request for a server-authoritative wireless node inspection or bind action. */
public final class MessageMEWirelessNodeAction implements IMessage {

    private int x;
    private int y;
    private int z;
    private int side;
    private boolean bind;

    public MessageMEWirelessNodeAction() {}

    public MessageMEWirelessNodeAction(int x, int y, int z, int side, boolean bind) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.side = side;
        this.bind = bind;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        x = buffer.readInt();
        y = buffer.readInt();
        z = buffer.readInt();
        side = buffer.readUnsignedByte();
        bind = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(z);
        buffer.writeByte(side);
        buffer.writeBoolean(bind);
    }

    public static final class Handler implements IMessageHandler<MessageMEWirelessNodeAction, IMessage> {

        @Override
        public IMessage onMessage(MessageMEWirelessNodeAction message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            WorldServer world = player.getServerForPlayer();
            if (!isValidRequest(player, world, message)) return null;

            PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(
                player,
                PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
                message.x,
                message.y,
                message.z,
                message.side,
                world);
            if (event.isCanceled() || event.useItem == cpw.mods.fml.common.eventhandler.Event.Result.DENY) return null;

            if (message.bind) {
                ItemMEWirelessTransceiver
                    .handleBindRequest(player, world, message.x, message.y, message.z, message.side);
            } else {
                ItemMEWirelessTransceiver
                    .handleInspectRequest(player, world, message.x, message.y, message.z, message.side);
            }
            return null;
        }

        private static boolean isValidRequest(EntityPlayerMP player, WorldServer world,
            MessageMEWirelessNodeAction message) {
            if (player == null || world == null || player.isDead || player.isSneaking() != message.bind) return false;

            ItemStack held = player.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemMEWirelessTransceiver)) return false;

            MinecraftServer server = MinecraftServer.getServer();
            if (server == null || message.side < 0
                || message.side > 5
                || message.y < 0
                || message.y >= server.getBuildLimit()) {
                return false;
            }
            if (!world.getChunkProvider()
                .chunkExists(message.x >> 4, message.z >> 4)) {
                return false;
            }

            double reach = player.theItemInWorldManager.getBlockReachDistance() + 1.0D;
            if (player.getDistanceSq(message.x + 0.5D, message.y + 0.5D, message.z + 0.5D) >= reach * reach) {
                return false;
            }
            return !server.isBlockProtected(world, message.x, message.y, message.z, player);
        }
    }
}
