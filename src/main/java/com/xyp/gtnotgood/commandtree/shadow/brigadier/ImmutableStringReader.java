// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier;

/** Relocated Brigadier ImmutableStringReader used by the command-tree parser. */
public interface ImmutableStringReader {
   String getString();

   int getRemainingLength();

   int getTotalLength();

   int getCursor();

   String getRead();

   String getRemaining();

   boolean canRead(int var1);

   boolean canRead();

   char peek();

   char peek(int var1);
}
// spotless:on
