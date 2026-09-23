// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.ImmutableStringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.Message;
import java.util.function.Function;

/** Relocated Brigadier DynamicCommandExceptionType used by the command-tree parser. */
public class DynamicCommandExceptionType implements CommandExceptionType {
   private final Function<Object, Message> function;

   public DynamicCommandExceptionType(Function<Object, Message> function) {
      this.function = function;
   }

   public CommandSyntaxException create(Object arg) {
      return new CommandSyntaxException(this, this.function.apply(arg));
   }

   public CommandSyntaxException createWithContext(ImmutableStringReader reader, Object arg) {
      return new CommandSyntaxException(this, this.function.apply(arg), reader.getString(), reader.getCursor());
   }
}
// spotless:on
