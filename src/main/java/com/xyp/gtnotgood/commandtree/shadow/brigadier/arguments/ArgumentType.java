// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.StringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/** Relocated Brigadier ArgumentType used by the command-tree parser. */
public interface ArgumentType<T> {
   T parse(StringReader var1) throws CommandSyntaxException;

   default <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return Suggestions.empty();
   }

   default Collection<String> getExamples() {
      return Collections.emptyList();
   }
}
// spotless:on
