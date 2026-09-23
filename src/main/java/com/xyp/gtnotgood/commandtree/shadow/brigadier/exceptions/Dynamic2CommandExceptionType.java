// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.ImmutableStringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.Message;

/** Relocated Brigadier Dynamic2CommandExceptionType used by the command-tree parser. */
public class Dynamic2CommandExceptionType implements CommandExceptionType {
   private final Dynamic2CommandExceptionType.Function function;

   public Dynamic2CommandExceptionType(Dynamic2CommandExceptionType.Function function) {
      this.function = function;
   }

   public CommandSyntaxException create(Object a, Object b) {
      return new CommandSyntaxException(this, this.function.apply(a, b));
   }

   public CommandSyntaxException createWithContext(ImmutableStringReader reader, Object a, Object b) {
      return new CommandSyntaxException(this, this.function.apply(a, b), reader.getString(), reader.getCursor());
   }

   public interface Function {
      Message apply(Object var1, Object var2);
   }
}
// spotless:on
