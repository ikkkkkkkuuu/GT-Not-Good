// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;

/** Relocated Brigadier RedirectModifier used by the command-tree parser. */
@FunctionalInterface
public interface RedirectModifier<S> {
   Collection<S> apply(CommandContext<S> var1) throws CommandSyntaxException;
}
// spotless:on
