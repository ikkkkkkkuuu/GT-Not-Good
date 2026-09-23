// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.context;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;

/** Relocated Brigadier SuggestionContext used by the command-tree parser. */
public class SuggestionContext<S> {
   public final CommandNode<S> parent;
   public final int startPos;

   public SuggestionContext(CommandNode<S> parent, int startPos) {
      this.parent = parent;
      this.startPos = startPos;
   }
}
// spotless:on
