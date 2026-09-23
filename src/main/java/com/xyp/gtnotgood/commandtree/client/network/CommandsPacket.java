// spotless:off
package com.xyp.gtnotgood.commandtree.client.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.command.serialization.ArgumentTypes;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.ArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.ArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.ArgumentCommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.LiteralCommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.RootCommandNode;
import com.xyp.gtnotgood.commandtree.util.SuggestionProviders;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Ported command-tree CommandsPacket used by the integrated chat suggestions. */
public class CommandsPacket {
   private static final byte NODE_TYPE_ROOT = 0;
   private static final byte NODE_TYPE_LITERAL = 1;
   private static final byte NODE_TYPE_ARGUMENT = 2;
   private static final byte FLAG_HAS_REDIRECT = 8;
   private static final byte FLAG_HAS_COMMAND = 4;
   private static final byte FLAG_HAS_SUGGESTIONS = 16;

   public static byte[] create(RootCommandNode<ISuggestionProvider> root) {
      return new CommandsPacket.PacketBuilder().withRoot(root).build();
   }

   public static RootCommandNode<ISuggestionProvider> read(byte[] data) {
      PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(data));
      return new CommandsPacket.PacketReader(buffer).readCommands();
   }

   private static HashMap<CommandNode<ISuggestionProvider>, Integer> enumerateNodesRecursively(RootCommandNode<ISuggestionProvider> root) {
      HashMap<CommandNode<ISuggestionProvider>, Integer> nodeMap = new HashMap<CommandNode<ISuggestionProvider>, Integer>();
      Queue<CommandNode<ISuggestionProvider>> queue = Queues.newArrayDeque();
      queue.add(root);

      CommandNode<ISuggestionProvider> current;
      while ((current = queue.poll()) != null) {
         if (!nodeMap.containsKey(current)) {
            nodeMap.put(current, nodeMap.size());
            queue.addAll(current.getChildren());
            if (current.getRedirect() != null) {
               queue.add(current.getRedirect());
            }
         }
      }

      return nodeMap;
   }

   @SuppressWarnings("unchecked")
   private static List<CommandNode<ISuggestionProvider>> getNodesInIdOrder(HashMap<CommandNode<ISuggestionProvider>, Integer> nodeMap) {
      CommandNode<ISuggestionProvider>[] nodes = new CommandNode[nodeMap.size()];
      for (Map.Entry<CommandNode<ISuggestionProvider>, Integer> entry : nodeMap.entrySet()) {
         nodes[entry.getValue()] = entry.getKey();
      }
      return Arrays.asList(nodes);
   }

   private static <T> void writeCollection(PacketBuffer buffer, Collection<T> collection, BiConsumer<PacketBuffer, T> writer) {
      buffer.writeVarIntToBuffer(collection.size());
      for (T item : collection) {
         writer.accept(buffer, item);
      }
   }

   private static int[] readVarIntArray(PacketBuffer buffer) {
      int length = buffer.readVarIntFromBuffer();
      int[] result = new int[length];
      for (int i = 0; i < length; i++) {
         result[i] = buffer.readVarIntFromBuffer();
      }
      return result;
   }

   private static class NodeEntry {
      @Nullable
      private final ArgumentBuilder<ISuggestionProvider, ?> builder;
      private final byte flags;
      private final int redirectIndex;
      private final int[] childIndices;
      @Nullable
      private CommandNode<ISuggestionProvider> node;

      NodeEntry(@Nullable ArgumentBuilder<ISuggestionProvider, ?> builder, byte flags, int redirectIndex, int[] childIndices) {
         this.builder = builder;
         this.flags = flags;
         this.redirectIndex = redirectIndex;
         this.childIndices = childIndices;
      }

      boolean tryBuild(List<CommandsPacket.NodeEntry> allEntries) {
         if (this.node != null) {
            return true;
         } else if (this.redirectIndex >= 0 && allEntries.get(this.redirectIndex).node == null) {
            return false;
         } else {
            for (int childIndex : this.childIndices) {
               if (allEntries.get(childIndex).node == null) {
                  return false;
               }
            }

            if (this.redirectIndex >= 0 && this.builder != null) {
               this.builder.redirect(allEntries.get(this.redirectIndex).node);
            }

            if (this.builder == null) {
               this.node = new RootCommandNode<ISuggestionProvider>();
            } else {
               if ((this.flags & 4) != 0) {
                  this.builder.executes(context -> 0);
               }

               this.node = this.builder.build();
            }

            for (int childIndex : this.childIndices) {
               CommandNode<ISuggestionProvider> childNode = allEntries.get(childIndex).node;
               if (!(childNode instanceof RootCommandNode)) {
                  this.node.addChild(childNode);
               }
            }

            return true;
         }
      }

      CommandNode<ISuggestionProvider> getNode() {
         return this.node;
      }
   }

   private static class PacketBuilder {
      private RootCommandNode<ISuggestionProvider> root;
      private final Map<CommandNode<ISuggestionProvider>, Integer> nodeIdMap = Maps.newHashMap();
      private final List<CommandNode<ISuggestionProvider>> nodeList = Lists.newArrayList();

      private PacketBuilder() {
      }

      public CommandsPacket.PacketBuilder withRoot(RootCommandNode<ISuggestionProvider> root) {
         this.root = root;
         return this;
      }

      public byte[] build() {
         PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
         this.enumerateNodes().writeNodes(buffer).writeRootIndex(buffer);
         byte[] data = new byte[buffer.readableBytes()];
         buffer.readBytes(data);
         buffer.release();
         return data;
      }

      private CommandsPacket.PacketBuilder enumerateNodes() {
         HashMap<CommandNode<ISuggestionProvider>, Integer> enumeration = CommandsPacket.enumerateNodesRecursively(this.root);
         this.nodeIdMap.putAll(enumeration);
         this.nodeList.addAll(CommandsPacket.getNodesInIdOrder(enumeration));
         return this;
      }

      private CommandsPacket.PacketBuilder writeNodes(PacketBuffer buffer) {
         CommandsPacket.writeCollection(buffer, this.nodeList, this::writeNode);
         return this;
      }

      private CommandsPacket.PacketBuilder writeRootIndex(PacketBuffer buffer) {
         buffer.writeVarIntToBuffer(this.nodeIdMap.get(this.root));
         return this;
      }

      private void writeNode(PacketBuffer buffer, CommandNode<ISuggestionProvider> node) {
         try {
            byte flags = this.calculateNodeFlags(node);
            buffer.writeByte(flags);
            this.writeChildrenIds(buffer, node);
            this.writeRedirectId(buffer, node);
            this.writeNodeData(buffer, node);
         } catch (IOException e) {
            throw new RuntimeException("Failed to write command node", e);
         }
      }

      private byte calculateNodeFlags(CommandNode<ISuggestionProvider> node) {
         byte flags = this.getNodeType(node);
         if (node.getRedirect() != null) {
            flags = (byte)(flags | 8);
         }

         if (node.getCommand() != null) {
            flags = (byte)(flags | 4);
         }

         if (node instanceof ArgumentCommandNode && ((ArgumentCommandNode<?, ?>)node).getCustomSuggestions() != null) {
            flags = (byte)(flags | 16);
         }

         return flags;
      }

      private byte getNodeType(CommandNode<ISuggestionProvider> node) {
         if (node instanceof RootCommandNode) {
            return 0;
         } else if (node instanceof LiteralCommandNode) {
            return 1;
         } else if (node instanceof ArgumentCommandNode) {
            return 2;
         } else {
            throw new UnsupportedOperationException("Unknown node type: " + node.getClass());
         }
      }

      private void writeChildrenIds(PacketBuffer buffer, CommandNode<ISuggestionProvider> node) {
         buffer.writeVarIntToBuffer(node.getChildren().size());

         for (CommandNode<ISuggestionProvider> child : node.getChildren()) {
            buffer.writeVarIntToBuffer(this.nodeIdMap.get(child));
         }
      }

      private void writeRedirectId(PacketBuffer buffer, CommandNode<ISuggestionProvider> node) {
         if (node.getRedirect() != null) {
            buffer.writeVarIntToBuffer(this.nodeIdMap.get(node.getRedirect()));
         }
      }

      @SuppressWarnings("unchecked")
      private void writeNodeData(PacketBuffer buffer, CommandNode<ISuggestionProvider> node) throws IOException {
         if (node instanceof ArgumentCommandNode) {
            this.writeArgumentNode(buffer, (ArgumentCommandNode<ISuggestionProvider, ?>)node);
         } else if (node instanceof LiteralCommandNode) {
            this.writeLiteralNode(buffer, (LiteralCommandNode<ISuggestionProvider>)node);
         }
      }

      @SuppressWarnings("unchecked")
      private void writeArgumentNode(PacketBuffer buffer, ArgumentCommandNode<ISuggestionProvider, ?> node) throws IOException {
         buffer.writeStringToBuffer(node.getName());
         ArgumentTypes.serialize(buffer, node.getType());
         if (node.getCustomSuggestions() != null) {
            ArgumentTypes.writeResourceLocation(buffer, SuggestionProviders.getId(node.getCustomSuggestions()));
         }
      }

      private void writeLiteralNode(PacketBuffer buffer, LiteralCommandNode<ISuggestionProvider> node) throws IOException {
         buffer.writeStringToBuffer(node.getLiteral());
      }
   }

   private static class PacketReader {
      private final PacketBuffer buffer;
      private final List<CommandsPacket.NodeEntry> entries = Lists.newArrayList();
      private int rootIndex;

      public PacketReader(PacketBuffer buffer) {
         this.buffer = buffer;
      }

      public RootCommandNode<ISuggestionProvider> readCommands() {
         try {
            return this.readEntries().readRootIndex().resolveNodes().getRootNode();
         } catch (IOException e) {
            throw new RuntimeException("Failed to read command tree", e);
         }
      }

      private CommandsPacket.PacketReader readEntries() throws IOException {
         int entryCount = this.buffer.readVarIntFromBuffer();

         for (int i = 0; i < entryCount; i++) {
            this.entries.add(this.readNodeEntry());
         }

         return this;
      }

      private CommandsPacket.PacketReader readRootIndex() {
         this.rootIndex = this.buffer.readVarIntFromBuffer();
         return this;
      }

      private CommandsPacket.PacketReader resolveNodes() {
         List<CommandsPacket.NodeEntry> unresolved = Lists.newArrayList(this.entries);

         while (!unresolved.isEmpty()) {
            boolean progress = unresolved.removeIf(entry -> entry.tryBuild(this.entries));
            if (!progress) {
               throw new IllegalStateException("Server sent an impossible command tree");
            }
         }

         return this;
      }

      @SuppressWarnings("unchecked")
      private RootCommandNode<ISuggestionProvider> getRootNode() {
         return (RootCommandNode<ISuggestionProvider>)this.entries.get(this.rootIndex).getNode();
      }

      private CommandsPacket.NodeEntry readNodeEntry() throws IOException {
         byte flags = this.buffer.readByte();
         int[] childIndices = CommandsPacket.readVarIntArray(this.buffer);
         int redirectIndex = (flags & 8) != 0 ? this.buffer.readVarIntFromBuffer() : -1;
         ArgumentBuilder<ISuggestionProvider, ?> builder = this.createBuilder(flags);
         return new CommandsPacket.NodeEntry(builder, flags, redirectIndex, childIndices);
      }

      @Nullable
      private ArgumentBuilder<ISuggestionProvider, ?> createBuilder(byte flags) throws IOException {
         int nodeType = flags & 3;
         switch (nodeType) {
            case 1:
               return LiteralArgumentBuilder.literal(this.buffer.readStringFromBuffer(32767));
            case 2:
               return this.createArgumentBuilder(flags);
            default:
               return null;
         }
      }

      @Nullable
      private RequiredArgumentBuilder<ISuggestionProvider, ?> createArgumentBuilder(byte flags) throws IOException {
         String name = this.buffer.readStringFromBuffer(32767);
         ArgumentType<?> argumentType = ArgumentTypes.deserialize(this.buffer);
         if (argumentType == null) {
            return null;
         } else {
            RequiredArgumentBuilder<ISuggestionProvider, ?> builder = RequiredArgumentBuilder.argument(name, argumentType);
            if ((flags & 16) != 0) {
               builder.suggests(SuggestionProviders.get(ArgumentTypes.readResourceLocation(this.buffer)));
            }

            return builder;
         }
      }
   }
}
// spotless:on
