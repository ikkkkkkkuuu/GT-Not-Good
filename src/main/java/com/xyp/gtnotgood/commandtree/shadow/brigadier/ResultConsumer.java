// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;

/** Relocated Brigadier ResultConsumer used by the command-tree parser. */
@FunctionalInterface
public interface ResultConsumer<S> {
   void onCommandComplete(CommandContext<S> var1, boolean var2, int var3);
}
// spotless:on
