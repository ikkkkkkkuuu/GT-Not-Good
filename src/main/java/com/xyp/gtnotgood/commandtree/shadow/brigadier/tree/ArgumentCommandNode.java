// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.tree;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.Command;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.RedirectModifier;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.StringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.ArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContextBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.ParsedArgument;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionProvider;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/** Relocated Brigadier ArgumentCommandNode used by the command-tree parser. */
public class ArgumentCommandNode<S, T> extends CommandNode<S> {
   private static final String USAGE_ARGUMENT_OPEN = "<";
   private static final String USAGE_ARGUMENT_CLOSE = ">";
   private final String name;
   private final ArgumentType<T> type;
   private final SuggestionProvider<S> customSuggestions;

   public ArgumentCommandNode(
      String name,
      ArgumentType<T> type,
      Command<S> command,
      Predicate<S> requirement,
      CommandNode<S> redirect,
      RedirectModifier<S> modifier,
      boolean forks,
      SuggestionProvider<S> customSuggestions
   ) {
      super(command, requirement, redirect, modifier, forks);
      this.name = name;
      this.type = type;
      this.customSuggestions = customSuggestions;
   }

   public ArgumentType<T> getType() {
      return this.type;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public String getUsageText() {
      return "<" + this.name + ">";
   }

   public SuggestionProvider<S> getCustomSuggestions() {
      return this.customSuggestions;
   }

   @Override
   public void parse(StringReader reader, CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
      int start = reader.getCursor();
      T result = this.type.parse(reader);
      ParsedArgument<S, T> parsed = new ParsedArgument<>(start, reader.getCursor(), result);
      contextBuilder.withArgument(this.name, parsed);
      contextBuilder.withNode(this, parsed.getRange());
   }

   @Override
   public CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) throws CommandSyntaxException {
      return this.customSuggestions == null ? this.type.listSuggestions(context, builder) : this.customSuggestions.getSuggestions(context, builder);
   }

   public RequiredArgumentBuilder<S, T> createBuilder() {
      RequiredArgumentBuilder<S, T> builder = RequiredArgumentBuilder.argument(this.name, this.type);
      builder.requires(this.getRequirement());
      builder.forward(this.getRedirect(), this.getRedirectModifier(), this.isFork());
      builder.suggests(this.customSuggestions);
      if (this.getCommand() != null) {
         builder.executes(this.getCommand());
      }

      return builder;
   }

   @Override
   public boolean isValidInput(String input) {
      try {
         StringReader reader = new StringReader(input);
         this.type.parse(reader);
         return !reader.canRead() || reader.peek() == ' ';
      } catch (CommandSyntaxException var3) {
         return false;
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof ArgumentCommandNode)) {
         return false;
      } else {
         ArgumentCommandNode that = (ArgumentCommandNode)o;
         if (!this.name.equals(that.name)) {
            return false;
         } else {
            return !this.type.equals(that.type) ? false : super.equals(o);
         }
      }
   }

   @Override
   public int hashCode() {
      int result = this.name.hashCode();
      return 31 * result + this.type.hashCode();
   }

   @Override
   protected String getSortedKey() {
      return this.name;
   }

   @Override
   public Collection<String> getExamples() {
      return this.type.getExamples();
   }

   @Override
   public String toString() {
      return "<argument " + this.name + ":" + this.type + ">";
   }
}
// spotless:on
