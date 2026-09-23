// spotless:off
package com.xyp.gtnotgood.commandtree.client;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Ported command-tree ISuggestionProvider used by the integrated chat suggestions. */
public interface ISuggestionProvider {
   Collection<String> getPlayerNames();

   CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> var1, SuggestionsBuilder var2);

   CompletableFuture<Suggestions> getSuggestionsFromServer();

   static CompletableFuture<Suggestions> suggest(Iterable<String> iterable, SuggestionsBuilder suggestionsBuilder) {
      String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

      for (String string2 : iterable) {
         if (string2.toLowerCase(Locale.ROOT).startsWith(string)) {
            suggestionsBuilder.suggest(string2);
         }
      }

      return suggestionsBuilder.buildFuture();
   }
}
// spotless:on
