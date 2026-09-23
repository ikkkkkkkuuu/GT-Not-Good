// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.ImmutableStringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.Message;

/** Relocated Brigadier DynamicNCommandExceptionType used by the command-tree parser. */
public class DynamicNCommandExceptionType implements CommandExceptionType {
   private final DynamicNCommandExceptionType.Function function;

   public DynamicNCommandExceptionType(DynamicNCommandExceptionType.Function function) {
      this.function = function;
   }

   public CommandSyntaxException create(Object a, Object... args) {
      return new CommandSyntaxException(this, this.function.apply(args));
   }

   public CommandSyntaxException createWithContext(ImmutableStringReader reader, Object... args) {
      return new CommandSyntaxException(this, this.function.apply(args), reader.getString(), reader.getCursor());
   }

   public interface Function {
      Message apply(Object[] var1);
   }
}
// spotless:on
