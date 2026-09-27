// spotless:off
package com.xyp.gtnotgood.commandtree.shadow.brigadier.builder;

import lombok.Getter;

import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.ArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionProvider;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.ArgumentCommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;

/** Relocated Brigadier RequiredArgumentBuilder used by the command-tree parser. */
public class RequiredArgumentBuilder<S, T> extends ArgumentBuilder<S, RequiredArgumentBuilder<S, T>> {
   @Getter
   private final String name;
   @Getter
   private final ArgumentType<T> type;
   @Getter
   private SuggestionProvider<S> suggestionsProvider = null;

   private RequiredArgumentBuilder(String name, ArgumentType<T> type) {
      this.name = name;
      this.type = type;
   }

   public static <S, T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type) {
      return new RequiredArgumentBuilder<>(name, type);
   }

   public RequiredArgumentBuilder<S, T> suggests(SuggestionProvider<S> provider) {
      this.suggestionsProvider = provider;
      return this.getThis();
   }

   protected RequiredArgumentBuilder<S, T> getThis() {
      return this;
   }

   public ArgumentCommandNode<S, T> build() {
      ArgumentCommandNode<S, T> result = new ArgumentCommandNode<>(
         this.getName(),
         this.getType(),
         this.getCommand(),
         this.getRequirement(),
         this.getRedirect(),
         this.getRedirectModifier(),
         this.isFork(),
         this.getSuggestionsProvider()
      );

      for (CommandNode<S> argument : this.getArguments()) {
         result.addChild(argument);
      }

      return result;
   }
}
// spotless:on
