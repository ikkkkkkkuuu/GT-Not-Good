// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import java.util.concurrent.CompletableFuture;

/** Relocated Brigadier SuggestionProvider used by the command-tree parser. */
@FunctionalInterface
public interface SuggestionProvider<S> {
   CompletableFuture<Suggestions> getSuggestions(CommandContext<S> var1, SuggestionsBuilder var2) throws CommandSyntaxException;
}
// spotless:on
