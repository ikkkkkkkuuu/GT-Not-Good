// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier;

import lombok.Getter;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContextBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;
import java.util.Collections;
import java.util.Map;

/** Relocated Brigadier ParseResults used by the command-tree parser. */
public class ParseResults<S> {
   @Getter
   private final CommandContextBuilder<S> context;
   @Getter
   private final Map<CommandNode<S>, CommandSyntaxException> exceptions;
   @Getter
   private final ImmutableStringReader reader;

   public ParseResults(CommandContextBuilder<S> context, ImmutableStringReader reader, Map<CommandNode<S>, CommandSyntaxException> exceptions) {
      this.context = context;
      this.reader = reader;
      this.exceptions = exceptions;
   }

   public ParseResults(CommandContextBuilder<S> context) {
      this(context, new StringReader(""), Collections.emptyMap());
   }

}
// spotless:on
