// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;
import java.util.Collection;

/** Relocated Brigadier AmbiguityConsumer used by the command-tree parser. */
@FunctionalInterface
public interface AmbiguityConsumer<S> {
   void ambiguous(CommandNode<S> var1, CommandNode<S> var2, CommandNode<S> var3, Collection<String> var4);
}
// spotless:on
