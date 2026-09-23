// spotless:off
package com.xyp.gtnotgood.commandtree.util;

import com.google.common.collect.Maps;
import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionProvider;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.util.ResourceLocation;

/** Ported command-tree SuggestionProviders used by the integrated chat suggestions. */
public class SuggestionProviders {
   private static final Map<ResourceLocation, SuggestionProvider<ISuggestionProvider>> REGISTRY = Maps.newHashMap();
   private static final ResourceLocation ASK_SERVER_ID = new ResourceLocation("minecraft:ask_server");
   public static final SuggestionProvider<ISuggestionProvider> ASK_SERVER = register(
      ASK_SERVER_ID, new SuggestionProvider<ISuggestionProvider>() {
         @Override
         public CompletableFuture<Suggestions> getSuggestions(CommandContext<ISuggestionProvider> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
            return commandContext.getSource().getSuggestionsFromServer(commandContext, suggestionsBuilder);
         }
      }
   );

   @SuppressWarnings("unchecked")
   public static <S extends ISuggestionProvider> SuggestionProvider<S> register(
      ResourceLocation resourceLocation, SuggestionProvider<ISuggestionProvider> suggestionProvider
   ) {
      if (REGISTRY.containsKey(resourceLocation)) {
         throw new IllegalArgumentException("A command suggestion provider is already registered with the name " + resourceLocation);
      } else {
         REGISTRY.put(resourceLocation, suggestionProvider);
         return (SuggestionProvider<S>) new SuggestionProviders.Wrapper(resourceLocation, suggestionProvider);
      }
   }

   public static SuggestionProvider<ISuggestionProvider> get(ResourceLocation resourceLocation) {
      SuggestionProvider<ISuggestionProvider> provider = REGISTRY.get(resourceLocation);
      return provider != null ? provider : ASK_SERVER;
   }

   public static ResourceLocation getId(SuggestionProvider<ISuggestionProvider> suggestionProvider) {
      return suggestionProvider instanceof SuggestionProviders.Wrapper ? ((SuggestionProviders.Wrapper)suggestionProvider).id : ASK_SERVER_ID;
   }

   public static SuggestionProvider<ISuggestionProvider> safelySwap(SuggestionProvider<ISuggestionProvider> suggestionProvider) {
      return suggestionProvider instanceof SuggestionProviders.Wrapper ? suggestionProvider : ASK_SERVER;
   }

   public static class Wrapper implements SuggestionProvider<ISuggestionProvider> {
      private final SuggestionProvider<ISuggestionProvider> provider;
      private final ResourceLocation id;

      public Wrapper(ResourceLocation id, SuggestionProvider<ISuggestionProvider> provider) {
         this.provider = provider;
         this.id = id;
      }

      @Override
      public CompletableFuture<Suggestions> getSuggestions(CommandContext<ISuggestionProvider> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
         return this.provider.getSuggestions(commandContext, suggestionsBuilder);
      }
   }
}
// spotless:on
