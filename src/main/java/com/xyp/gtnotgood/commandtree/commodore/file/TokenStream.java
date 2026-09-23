// spotless:off
package com.xyp.gtnotgood.commandtree.commodore.file;

/** Relocated Commodore TokenStream used to parse bundled command definitions. */
public interface TokenStream {
   boolean hasNext();

   Token next();

   Token peek();

   ParseException createException(String var1);

   ParseException createException(Throwable var1);

   ParseException createException(String var1, Throwable var2);
}
// spotless:on
