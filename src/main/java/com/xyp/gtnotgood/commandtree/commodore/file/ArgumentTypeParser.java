// spotless:off
package com.xyp.gtnotgood.commandtree.commodore.file;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.ArgumentType;

/** Relocated Commodore ArgumentTypeParser used to parse bundled command definitions. */
public interface ArgumentTypeParser {
   boolean canParse(String var1, String var2);

   ArgumentType<?> parse(String var1, String var2, TokenStream var3) throws ParseException;
}
// spotless:on
