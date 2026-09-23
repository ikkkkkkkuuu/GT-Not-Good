// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;

@FunctionalInterface
/** Relocated Brigadier ResultConsumer used by the command-tree parser. */
public interface ResultConsumer<S> {
   void onCommandComplete(CommandContext<S> var1, boolean var2, int var3);
}
// spotless:on
