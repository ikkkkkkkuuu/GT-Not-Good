// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.tree;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.StringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.ArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContextBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/** Relocated Brigadier RootCommandNode used by the command-tree parser. */
public class RootCommandNode<S> extends CommandNode<S> {
   public RootCommandNode() {
      super(null, c -> true, null, s -> Collections.singleton(s.getSource()), false);
   }

   @Override
   public String getName() {
      return "";
   }

   @Override
   public String getUsageText() {
      return "";
   }

   @Override
   public void parse(StringReader reader, CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
   }

   @Override
   public CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return Suggestions.empty();
   }

   @Override
   public boolean isValidInput(String input) {
      return false;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof RootCommandNode) ? false : super.equals(o);
      }
   }

   @Override
   public ArgumentBuilder<S, ?> createBuilder() {
      throw new IllegalStateException("Cannot convert root into a builder");
   }

   @Override
   protected String getSortedKey() {
      return "";
   }

   @Override
   public Collection<String> getExamples() {
      return Collections.emptyList();
   }

   @Override
   public String toString() {
      return "<root>";
   }
}
// spotless:on
