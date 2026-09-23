// spotless:off
package com.xyp.gtnotgood.commandtree.handler;

import com.google.common.collect.Maps;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.client.network.CommandsPacket;
import com.xyp.gtnotgood.commandtree.command.CommandSource;
import com.xyp.gtnotgood.commandtree.command.CommandTreeConverter;
import com.xyp.gtnotgood.commandtree.compat.CompatRegistry;
import com.xyp.gtnotgood.commandtree.network.CommandTreePacket;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.CommandDispatcher;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.StringArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.LiteralCommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.RootCommandNode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/** Sends each player the commands they are permitted to use. */
public class PlayerJoinHandler {
   @SubscribeEvent
   public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.player instanceof EntityPlayerMP) {
         EntityPlayerMP player = (EntityPlayerMP) event.player;
         sendCommands(player);
      }
   }

   @SuppressWarnings("unchecked")
   private void sendCommands(EntityPlayerMP player) {
      CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<CommandSource>();
      Map<String, ICommand> commandMap = (Map<String, ICommand>) MinecraftServer.getServer().getCommandManager().getCommands();

      // Deduplicate: commandMap contains both primary names and aliases pointing to the same ICommand
      Set<ICommand> seen = new HashSet<ICommand>();
      for (ICommand command : commandMap.values()) {
         if (seen.add(command) && command.canCommandSenderUseCommand(player)) {
            registerBrigoCommand(dispatcher, command);
         }
      }

      // Convert to client-side tree and send
      CommandSource source = CommandSource.adapt(player);
      Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> nodeMapping = Maps.newHashMap();
      RootCommandNode<ISuggestionProvider> clientRoot = new RootCommandNode<ISuggestionProvider>();
      nodeMapping.put(dispatcher.getRoot(), clientRoot);
      new CommandTreeConverter(nodeMapping, source).convertChildren(dispatcher.getRoot(), clientRoot);

      byte[] data = CommandsPacket.create(clientRoot);
      GTNotGood.channel.sendTo(new CommandTreePacket(data), player);
   }

   @SuppressWarnings("unchecked")
   private void registerBrigoCommand(CommandDispatcher<CommandSource> dispatcher, ICommand command) {
      LiteralCommandNode<CommandSource> node;
      if (CompatRegistry.hasCompatFor(command.getClass())) {
         node = CompatRegistry.getCompatCommand(command.getClass());
      } else {
         node = LiteralArgumentBuilder.<CommandSource>literal(command.getCommandName())
            .then(
               RequiredArgumentBuilder.<CommandSource, String>argument("params", StringArgumentType.greedyString())
                  .suggests((context, suggestions) -> suggestions.buildFuture())
            )
            .build();
      }

      dispatcher.getRoot().addChild(node);
      java.util.List<?> aliases = command.getCommandAliases();
      if (aliases != null) {
         for (Object aliasObj : aliases) {
            String alias = (String) aliasObj;
            if (!alias.equals(command.getCommandName())) {
               dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal(alias).redirect(node));
            }
         }
      }
   }
}
// spotless:on
