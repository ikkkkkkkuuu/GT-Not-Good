// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
/** Relocated Brigadier Command used by the command-tree parser. */
public interface Command<S> {
   int SINGLE_SUCCESS = 1;

   int run(CommandContext<S> var1) throws CommandSyntaxException;
}
// spotless:on
