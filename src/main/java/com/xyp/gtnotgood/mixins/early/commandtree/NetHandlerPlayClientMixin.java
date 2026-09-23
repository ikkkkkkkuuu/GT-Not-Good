// spotless:off
package com.xyp.gtnotgood.mixins.early.commandtree;

import com.xyp.gtnotgood.commandtree.accessor.NetHandlerPlayClientExtras;
import com.xyp.gtnotgood.commandtree.client.ClientSuggestionProvider;
import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.CommandDispatcher;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.arguments.StringArgumentType;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.LiteralArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.builder.RequiredArgumentBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.StringRange;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestion;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.util.SuggestionProviders;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S3APacketTabComplete;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stores the synced command tree and receives server tab completions. */
@Mixin(NetHandlerPlayClient.class)
/** Ported command-tree NetHandlerPlayClientMixin used by the integrated chat suggestions. */
public class NetHandlerPlayClientMixin implements NetHandlerPlayClientExtras {
   @Shadow
   private Minecraft gameController; // gameController

   @Unique
   private ClientSuggestionProvider brigo$suggestionsProvider;
   @Unique
   private CommandDispatcher<ISuggestionProvider> brigo$commands = new CommandDispatcher<ISuggestionProvider>();
   @Unique
   private boolean brigo$entityStatusReceived = false;

   @Inject(method = "<init>", at = @At("RETURN"))
   private void onInit(CallbackInfo ci) {
      this.brigo$suggestionsProvider = new ClientSuggestionProvider((NetHandlerPlayClient)(Object)this, this.gameController);
   }

   @Inject(method = "handleTabComplete", at = @At("TAIL"))
   private void handleTabComplete(S3APacketTabComplete packetIn, CallbackInfo ci) {
      this.brigo$suggestionsProvider.completeCustomSuggestions(packetIn.func_149630_c());
   }

   @Inject(method = "handleEntityStatus", at = @At("TAIL"))
   private void handleEntityStatus(S19PacketEntityStatus packetIn, CallbackInfo ci) {
      if (this.brigo$commands.getRoot().getChildren().isEmpty() && !this.brigo$entityStatusReceived) {
         this.brigo$entityStatusReceived = true;
         this.brigo$suggestionsProvider()
            .getSuggestionsFromServer()
            .thenAccept(
               commands -> {
                  for (Suggestion suggestion : commands.getList()) {
                     this.brigo$commands
                        .register(
                           LiteralArgumentBuilder.<ISuggestionProvider>literal(suggestion.getText().substring(1))
                              .then(
                                 RequiredArgumentBuilder.<ISuggestionProvider, String>argument("params", StringArgumentType.greedyString())
                                    .suggests(SuggestionProviders.ASK_SERVER)
                              )
                        );
                  }
               }
            );
      }
   }

   @Unique
   @Override
   public ClientSuggestionProvider brigo$suggestionsProvider() {
      return this.brigo$suggestionsProvider;
   }

   @Unique
   @NotNull
   @Override
   public CommandDispatcher<ISuggestionProvider> brigo$commands() {
      return this.brigo$commands;
   }

   @Unique
   @Override
   public void brigo$setCommands(@NotNull CommandDispatcher<ISuggestionProvider> dispatcher) {
      this.brigo$commands = dispatcher;
   }
}
// spotless:on
