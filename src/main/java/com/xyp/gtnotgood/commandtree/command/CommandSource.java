// spotless:off
package com.xyp.gtnotgood.commandtree.command;

import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

/** Ported command-tree CommandSource used by the integrated chat suggestions. */
public class CommandSource implements ISuggestionProvider {
   private final ICommandSender sender;

   public CommandSource(ICommandSender sender) {
      this.sender = sender;
   }

   public static CommandSource adapt(ICommandSender sender) {
      return new CommandSource(sender);
   }

   @Override
   public Collection<String> getPlayerNames() {
      return Arrays.asList(MinecraftServer.getServer().getAllUsernames());
   }

   @Override
   public CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> context, SuggestionsBuilder suggestionsBuilder) {
      return null;
   }

   @Override
   public CompletableFuture<Suggestions> getSuggestionsFromServer() {
      return null;
   }
}
// spotless:on
