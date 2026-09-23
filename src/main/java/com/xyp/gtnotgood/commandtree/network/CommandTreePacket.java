// spotless:off
package com.xyp.gtnotgood.commandtree.network;

import com.xyp.gtnotgood.commandtree.accessor.NetHandlerPlayClientExtras;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.client.network.CommandsPacket;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.CommandDispatcher;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.RootCommandNode;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/** Transfers a player's visible command tree to the client. */
public class CommandTreePacket implements IMessage {
   private byte[] treeData;

   public CommandTreePacket() {
   }

   public CommandTreePacket(byte[] treeData) {
      this.treeData = treeData;
   }

   @Override
   public void fromBytes(ByteBuf buf) {
      int length = buf.readInt();
      if (length < 0 || length > 1024 * 1024 || length > buf.readableBytes()) {
         throw new IllegalArgumentException("Invalid command tree size: " + length);
      }
      this.treeData = new byte[length];
      buf.readBytes(this.treeData);
   }

   @Override
   public void toBytes(ByteBuf buf) {
      buf.writeInt(this.treeData.length);
      buf.writeBytes(this.treeData);
   }

   public static class Handler implements IMessageHandler<CommandTreePacket, IMessage> {
      @Override
      public IMessage onMessage(CommandTreePacket message, MessageContext ctx) {
         final byte[] data = message.treeData;
         Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
               try {
                  RootCommandNode<ISuggestionProvider> root = CommandsPacket.read(data);
                  NetHandlerPlayClientExtras extras = (NetHandlerPlayClientExtras) Minecraft.getMinecraft().getNetHandler();
                  if (extras != null) {
                     extras.brigo$setCommands(new CommandDispatcher<ISuggestionProvider>(root));
                  }
               } catch (Exception e) {
                  GTNotGood.LOG.warn("Could not read command tree from server", e);
               }
            }
         });
         return null;
      }
   }
}
// spotless:on
