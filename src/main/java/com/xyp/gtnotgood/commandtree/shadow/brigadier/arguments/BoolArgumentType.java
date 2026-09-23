// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.StringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** Relocated Brigadier BoolArgumentType used by the command-tree parser. */
public class BoolArgumentType implements ArgumentType<Boolean> {
   private static final Collection<String> EXAMPLES = Arrays.asList("true", "false");

   private BoolArgumentType() {
   }

   public static BoolArgumentType bool() {
      return new BoolArgumentType();
   }

   public static boolean getBool(CommandContext<?> context, String name) {
      return context.getArgument(name, Boolean.class);
   }

   public Boolean parse(StringReader reader) throws CommandSyntaxException {
      return reader.readBoolean();
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      if ("true".startsWith(builder.getRemainingLowerCase())) {
         builder.suggest("true");
      }

      if ("false".startsWith(builder.getRemainingLowerCase())) {
         builder.suggest("false");
      }

      return builder.buildFuture();
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
// spotless:on
