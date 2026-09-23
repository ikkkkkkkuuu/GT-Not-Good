// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.ImmutableStringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.Message;

/** Relocated Brigadier SimpleCommandExceptionType used by the command-tree parser. */
public class SimpleCommandExceptionType implements CommandExceptionType {
   private final Message message;

   public SimpleCommandExceptionType(Message message) {
      this.message = message;
   }

   public CommandSyntaxException create() {
      return new CommandSyntaxException(this, this.message);
   }

   public CommandSyntaxException createWithContext(ImmutableStringReader reader) {
      return new CommandSyntaxException(this, this.message, reader.getString(), reader.getCursor());
   }

   @Override
   public String toString() {
      return this.message.getString();
   }
}
// spotless:on
