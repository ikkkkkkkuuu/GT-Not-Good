// spotless:off
package com.xyp.gtnotgood.commandtree.accessor;

import com.xyp.gtnotgood.commandtree.client.ClientSuggestionProvider;
import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.CommandDispatcher;
import org.jetbrains.annotations.NotNull;

/** Ported command-tree NetHandlerPlayClientExtras used by the integrated chat suggestions. */
public interface NetHandlerPlayClientExtras {
   ClientSuggestionProvider brigo$suggestionsProvider();

   @NotNull
   CommandDispatcher<ISuggestionProvider> brigo$commands();

   void brigo$setCommands(@NotNull CommandDispatcher<ISuggestionProvider> dispatcher);
}
// spotless:on
