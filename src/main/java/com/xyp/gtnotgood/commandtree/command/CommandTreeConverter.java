// spotless:off
package com.xyp.gtnotgood.commandtree.command;

import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.ArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;
import com.xyp.gtnotgood.commandtree.util.SuggestionProviders;
import java.util.Map;

/** Ported command-tree CommandTreeConverter used by the integrated chat suggestions. */
public class CommandTreeConverter {
   private final Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> nodeMapping;
   private final CommandSource source;

   public CommandTreeConverter(Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> nodeMapping, CommandSource source) {
      this.nodeMapping = nodeMapping;
      this.source = source;
   }

   public void convertChildren(CommandNode<CommandSource> serverNode, CommandNode<ISuggestionProvider> clientNode) {
      for (CommandNode<CommandSource> child : serverNode.getChildren()) {
         if (child.canUse(this.source)) {
            CommandNode<ISuggestionProvider> convertedChild = this.convertNode(child);
            this.nodeMapping.put(child, convertedChild);
            clientNode.addChild(convertedChild);
            this.convertChildren(child, convertedChild);
         }
      }
   }

   private CommandNode<ISuggestionProvider> convertNode(CommandNode<CommandSource> serverNode) {
      @SuppressWarnings("unchecked")
      ArgumentBuilder<ISuggestionProvider, ?> builder = (ArgumentBuilder<ISuggestionProvider, ?>) (ArgumentBuilder) serverNode.createBuilder();
      return this.configureBuilder(builder, serverNode).build();
   }

   private ArgumentBuilder<ISuggestionProvider, ?> configureBuilder(ArgumentBuilder<ISuggestionProvider, ?> builder, CommandNode<CommandSource> serverNode) {
      builder.requires(client -> true);
      if (builder.getCommand() != null) {
         builder.executes(context -> 0);
      }

      if (builder instanceof RequiredArgumentBuilder) {
         this.configureArgumentBuilder((RequiredArgumentBuilder<ISuggestionProvider, ?>)builder);
      }

      if (builder.getRedirect() != null) {
         builder.redirect(this.nodeMapping.get(builder.getRedirect()));
      }

      return builder;
   }

   private void configureArgumentBuilder(RequiredArgumentBuilder<ISuggestionProvider, ?> builder) {
      if (builder.getSuggestionsProvider() != null) {
         builder.suggests(SuggestionProviders.safelySwap(builder.getSuggestionsProvider()));
      } else {
         builder.suggests(SuggestionProviders.ASK_SERVER);
      }
   }
}
// spotless:on
