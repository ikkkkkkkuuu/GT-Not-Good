// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.ImmutableStringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.Message;

/** Relocated Brigadier Dynamic3CommandExceptionType used by the command-tree parser. */
public class Dynamic3CommandExceptionType implements CommandExceptionType {
   private final Dynamic3CommandExceptionType.Function function;

   public Dynamic3CommandExceptionType(Dynamic3CommandExceptionType.Function function) {
      this.function = function;
   }

   public CommandSyntaxException create(Object a, Object b, Object c) {
      return new CommandSyntaxException(this, this.function.apply(a, b, c));
   }

   public CommandSyntaxException createWithContext(ImmutableStringReader reader, Object a, Object b, Object c) {
      return new CommandSyntaxException(this, this.function.apply(a, b, c), reader.getString(), reader.getCursor());
   }

   public interface Function {
      Message apply(Object var1, Object var2, Object var3);
   }
}
// spotless:on
