// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.context;

import lombok.Getter;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;
import java.util.Objects;

/** Relocated Brigadier ParsedCommandNode used by the command-tree parser. */
public class ParsedCommandNode<S> {
   @Getter
   private final CommandNode<S> node;
   @Getter
   private final StringRange range;

   public ParsedCommandNode(CommandNode<S> node, StringRange range) {
      this.node = node;
      this.range = range;
   }

   @Override
   public String toString() {
      return this.node + "@" + this.range;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ParsedCommandNode<?> that = (ParsedCommandNode<?>)o;
         return Objects.equals(this.node, that.node) && Objects.equals(this.range, that.range);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.node, this.range);
   }
}
// spotless:on
