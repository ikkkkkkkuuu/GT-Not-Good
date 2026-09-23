// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier;

/** Relocated Brigadier LiteralMessage used by the command-tree parser. */
public class LiteralMessage implements Message {
   private final String string;

   public LiteralMessage(String string) {
      this.string = string;
   }

   @Override
   public String getString() {
      return this.string;
   }

   @Override
   public String toString() {
      return this.string;
   }
}
// spotless:on
