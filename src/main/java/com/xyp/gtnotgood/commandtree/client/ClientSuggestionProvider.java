// spotless:off
package com.xyp.gtnotgood.commandtree.client;

import com.google.common.collect.Lists;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C14PacketTabComplete;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.StringRange;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestion;
import net.minecraft.client.gui.GuiPlayerInfo;

/** Requests vanilla server tab completions for Brigadier's asynchronous suggestions. */
public class ClientSuggestionProvider implements ISuggestionProvider {
   private final NetHandlerPlayClient connection;
   private final Minecraft mc;
   private final Queue<PendingRequest> pendingRequests = new ArrayDeque<PendingRequest>();

   public ClientSuggestionProvider(NetHandlerPlayClient connection, Minecraft mc) {
      this.connection = connection;
      this.mc = mc;
   }

   @Override
   public Collection<String> getPlayerNames() {
      List<String> list = Lists.newArrayList();
      if (this.mc.thePlayer != null && this.mc.thePlayer.sendQueue != null) {
         @SuppressWarnings("unchecked")
         List<GuiPlayerInfo> playerInfoList =
             (List<GuiPlayerInfo>) this.mc.thePlayer.sendQueue.playerInfoList;
         for (GuiPlayerInfo info : playerInfoList) {
            list.add(info.name);
         }
      }
      return list;
   }

   @Override
   public CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> context, SuggestionsBuilder suggestionsBuilder) {
      return request(context.getInput());
   }

   @Override
   public CompletableFuture<Suggestions> getSuggestionsFromServer() {
      return request("/");
   }

   private CompletableFuture<Suggestions> request(String command) {
      CompletableFuture<Suggestions> result = new CompletableFuture<Suggestions>();
      pendingRequests.add(new PendingRequest(command, result));
      connection.addToSendQueue(new C14PacketTabComplete(command));
      return result;
   }

   /** Completes the oldest outstanding request with the matching server reply. */
   public void completeCustomSuggestions(String[] matches) {
      PendingRequest request = pendingRequests.poll();
      if (request == null) return;
      int lastSpace = request.command.lastIndexOf(' ');
      int start = lastSpace < 0 ? 0 : lastSpace + 1;
      StringRange range =
          StringRange.between(start, request.command.length());
      List<Suggestion> suggestions = Lists.newArrayList();
      for (String match : matches) {
         suggestions.add(new Suggestion(range, match));
      }
      request.future.complete(new Suggestions(range, suggestions));
   }

   /** Keeps a request's input paired with its reply when the player types rapidly. */
   private static final class PendingRequest {
      private final String command;
      private final CompletableFuture<Suggestions> future;

      private PendingRequest(String command, CompletableFuture<Suggestions> future) {
         this.command = command;
         this.future = future;
      }
   }
}
// spotless:on
