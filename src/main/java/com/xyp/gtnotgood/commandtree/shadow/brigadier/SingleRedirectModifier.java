// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;

@FunctionalInterface
/** Relocated Brigadier SingleRedirectModifier used by the command-tree parser. */
public interface SingleRedirectModifier<S> {
   S apply(CommandContext<S> var1) throws CommandSyntaxException;
}
// spotless:on
