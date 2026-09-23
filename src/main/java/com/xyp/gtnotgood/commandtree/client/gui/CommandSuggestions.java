// spotless:off
package com.xyp.gtnotgood.commandtree.client.gui;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.xyp.gtnotgood.commandtree.accessor.GuiTextFieldExtras;
import com.xyp.gtnotgood.commandtree.accessor.NetHandlerPlayClientExtras;
import com.xyp.gtnotgood.commandtree.client.ISuggestionProvider;
import com.xyp.gtnotgood.commandtree.client.renderer.Rect2i;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.CommandDispatcher;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.Message;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.ParseResults;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.StringReader;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.CommandContextBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.ParsedArgument;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.context.SuggestionContext;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.exceptions.CommandSyntaxException;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestion;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.Suggestions;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.suggestion.SuggestionsBuilder;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.CommandNode;
import com.xyp.gtnotgood.commandtree.shadow.brigadier.tree.LiteralCommandNode;
import com.xyp.gtnotgood.commandtree.util.ComponentUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Mouse;

/** Renders the interactive command dropdown and highlights parsed arguments. */
public class CommandSuggestions {
   private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
   private static final EnumChatFormatting[] ARGUMENT_COLORS = new EnumChatFormatting[]{
      EnumChatFormatting.AQUA, EnumChatFormatting.YELLOW, EnumChatFormatting.GREEN, EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.GOLD
   };
   private final Minecraft minecraft;
   private final GuiScreen screen;
   private final GuiTextField input;
   private final FontRenderer fontRenderer;
   private final CommandSuggestionsConfig config;
   private final List<String> commandUsage = Lists.newArrayList();
   private int commandUsagePosition;
   private int commandUsageWidth;
   private boolean allowSuggestions;
   private boolean keepSuggestions;
   @Nullable
   private ParseResults<ISuggestionProvider> currentParse;
   @Nullable
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Nullable
   private SuggestionsList suggestions;

   public CommandSuggestions(
      Minecraft minecraft, GuiScreen screen, GuiTextField input, FontRenderer fontRenderer, CommandSuggestionsConfig config
   ) {
      this.minecraft = minecraft;
      this.screen = screen;
      this.input = input;
      this.fontRenderer = fontRenderer;
      this.config = config;
      ((GuiTextFieldExtras)input).brigo$textFormatter(this::formatCommandText);
   }

   public void allowSuggestions(boolean allow) {
      this.allowSuggestions = allow;
      if (!allow) {
         this.suggestions = null;
      }
   }

   public boolean handleKeyPress(int keyCode) {
      if (this.suggestions != null && this.suggestions.handleKeyPress(keyCode)) {
         return true;
      } else if (keyCode == 15) {
         if (this.pendingSuggestions == null) return false;
         this.showSuggestions();
         return true;
      } else {
         return false;
      }
   }

   public boolean handleMouseScroll(double delta) {
      return this.suggestions != null && this.suggestions.handleMouseScroll(MathHelper.clamp_double(delta, -1.0, 1.0));
   }

   public boolean handleMouseClick(double mouseX, double mouseY, int button) {
      return this.suggestions != null && this.suggestions.handleMouseClick((int)mouseX, (int)mouseY, button);
   }

   public void showSuggestions() {
      if (this.pendingSuggestions != null && this.pendingSuggestions.isDone()) {
         Suggestions suggestions = this.pendingSuggestions.join();
         if (suggestions.isEmpty()) {
            return;
         }

         List<Suggestion> filteredSuggestions = this.sortSuggestions(suggestions);
         if (!filteredSuggestions.isEmpty()) {
            int maxWidth = 0;
            for (Suggestion suggestion : suggestions.getList()) {
               int w = this.fontRenderer.getStringWidth(suggestion.getText());
               if (w > maxWidth) maxWidth = w;
            }
            int x = MathHelper.clamp_int(
               ((GuiTextFieldExtras)this.input).brigo$screenX(suggestions.getRange().getStart()), 0, this.screen.width - maxWidth
            );
            int y = this.config.anchorToBottom ? this.screen.height - 12 : 72;
            this.suggestions = new SuggestionsList(x, y, maxWidth, filteredSuggestions);
         }
      }
   }

   private List<Suggestion> sortSuggestions(Suggestions suggestions) {
      String inputText = this.input.getText().substring(0, this.input.getCursorPosition());
      int lastWordIndex = findLastWordIndex(inputText);
      String currentWord = inputText.substring(lastWordIndex).toLowerCase(Locale.ROOT);
      List<Suggestion> prioritySuggestions = Lists.newArrayList();
      List<Suggestion> otherSuggestions = Lists.newArrayList();

      for (Suggestion suggestion : suggestions.getList()) {
         String text = suggestion.getText();
         if (!text.equals(currentWord)) {
            if (!text.startsWith(currentWord) && !text.startsWith("minecraft:" + currentWord)) {
               otherSuggestions.add(suggestion);
            } else {
               prioritySuggestions.add(suggestion);
            }
         }
      }

      prioritySuggestions.addAll(otherSuggestions);
      return prioritySuggestions;
   }

   public void updateCommandInfo() {
      String text = this.input.getText();
      if (this.currentParse != null && !this.currentParse.getReader().getString().equals(text)) {
         this.currentParse = null;
      }

      if (!this.keepSuggestions) {
         ((GuiTextFieldExtras)this.input).brigo$suggestion(null);
         this.suggestions = null;
      }

      this.commandUsage.clear();
      StringReader reader = new StringReader(text);
      boolean hasCommandPrefix = reader.canRead() && reader.peek() == '/';
      if (hasCommandPrefix) {
         reader.skip();
      }

      boolean isCommand = this.config.commandsOnly || hasCommandPrefix;
      int cursorPos = this.input.getCursorPosition();
      if (isCommand) {
         this.processCommandSuggestions(reader, cursorPos);
      } else {
         this.processPlayerNameSuggestions(text, cursorPos);
      }
   }

   private void processCommandSuggestions(StringReader reader, int cursorPos) {
      if (this.minecraft.thePlayer == null || this.minecraft.thePlayer.sendQueue == null) return;
      CommandDispatcher<ISuggestionProvider> dispatcher = ((NetHandlerPlayClientExtras)this.minecraft.thePlayer.sendQueue).brigo$commands();
      if (this.currentParse == null) {
         ISuggestionProvider provider = ((NetHandlerPlayClientExtras)this.minecraft.thePlayer.sendQueue).brigo$suggestionsProvider();
         this.currentParse = dispatcher.parse(reader, provider);
      }

      int minCursor = this.config.onlyShowIfCursorPastError ? reader.getCursor() : 1;
      if (cursorPos >= minCursor && (this.suggestions == null || !this.keepSuggestions)) {
         CompletableFuture<Suggestions> requested = dispatcher.getCompletionSuggestions(this.currentParse, cursorPos);
         this.pendingSuggestions = requested;
         requested.thenRun(() -> {
            if (this.pendingSuggestions == requested && requested.isDone()) {
               this.updateUsageInfo();
            }
         });
      }
   }

   private void processPlayerNameSuggestions(String text, int cursorPos) {
      if (this.minecraft.thePlayer == null || this.minecraft.thePlayer.sendQueue == null) return;
      String textToCursor = text.substring(0, cursorPos);
      int lastWordIndex = findLastWordIndex(textToCursor);
      Collection<String> playerNames = ((NetHandlerPlayClientExtras)this.minecraft.thePlayer.sendQueue).brigo$suggestionsProvider().getPlayerNames();
      this.pendingSuggestions = ISuggestionProvider.suggest(playerNames, new SuggestionsBuilder(textToCursor, lastWordIndex));
   }

   private static int findLastWordIndex(String text) {
      if (Strings.isNullOrEmpty(text)) {
         return 0;
      } else {
         int lastIndex = 0;
         Matcher matcher = WHITESPACE_PATTERN.matcher(text);

         while (matcher.find()) {
            lastIndex = matcher.end();
         }

         return lastIndex;
      }
   }

   private void updateUsageInfo() {
      if (this.input.getCursorPosition() == this.input.getText().length()) {
         this.processParsingErrors();
      }

      this.commandUsagePosition = 0;
      this.commandUsageWidth = this.screen.width;
      if (this.commandUsage.isEmpty()) {
         this.fillNodeUsage(EnumChatFormatting.GRAY);
      }

      this.suggestions = null;
      if (this.allowSuggestions) {
         this.showSuggestions();
      }
   }

   private void processParsingErrors() {
      if (this.pendingSuggestions.join().isEmpty() && !this.currentParse.getExceptions().isEmpty()) {
         int literalErrors = 0;

         for (Entry<CommandNode<ISuggestionProvider>, CommandSyntaxException> entry : this.currentParse.getExceptions().entrySet()) {
            CommandSyntaxException exception = entry.getValue();
            if (exception.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect()) {
               literalErrors++;
            } else {
               this.commandUsage.add(formatException(exception));
            }
         }

         if (literalErrors > 0) {
            this.commandUsage.add(formatException(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create()));
         }
      } else if (this.currentParse.getReader().canRead()) {
         CommandSyntaxException parseException = getParseException(this.currentParse);
         if (parseException != null) {
            this.commandUsage.add(formatException(parseException));
         }
      }
   }

   @Nullable
   public static <S> CommandSyntaxException getParseException(ParseResults<S> result) {
      if (!result.getReader().canRead()) {
         return null;
      } else if (result.getExceptions().size() == 1) {
         return result.getExceptions().values().iterator().next();
      } else {
         return result.getContext().getRange().isEmpty()
            ? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(result.getReader())
            : CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(result.getReader());
      }
   }

   private void fillNodeUsage(EnumChatFormatting color) {
      if (this.minecraft.thePlayer == null || this.minecraft.thePlayer.sendQueue == null) return;
      CommandContextBuilder<ISuggestionProvider> context = this.currentParse.getContext();
      SuggestionContext<ISuggestionProvider> suggestionContext = context.findSuggestionContext(this.input.getCursorPosition());
      CommandDispatcher<ISuggestionProvider> dispatcher = ((NetHandlerPlayClientExtras)this.minecraft.thePlayer.sendQueue).brigo$commands();
      ISuggestionProvider provider = ((NetHandlerPlayClientExtras)this.minecraft.thePlayer.sendQueue).brigo$suggestionsProvider();
      Map<CommandNode<ISuggestionProvider>, String> usageMap = dispatcher.getSmartUsage(suggestionContext.parent, provider);
      List<String> usageList = Lists.newArrayList();
      int maxWidth = 0;

      for (Entry<CommandNode<ISuggestionProvider>, String> entry : usageMap.entrySet()) {
         if (!(entry.getKey() instanceof LiteralCommandNode)) {
            String usage = color + entry.getValue();
            usageList.add(usage);
            int w = this.fontRenderer.getStringWidth(entry.getValue());
            if (w > maxWidth) maxWidth = w;
         }
      }

      if (!usageList.isEmpty()) {
         this.commandUsage.addAll(usageList);
         this.commandUsagePosition = MathHelper.clamp_int(
            ((GuiTextFieldExtras)this.input).brigo$screenX(suggestionContext.startPos), 0, this.screen.width - maxWidth
         );
         this.commandUsageWidth = maxWidth;
      }
   }

   private String formatCommandText(String text, int offset) {
      return this.currentParse != null ? formatParsedCommand(this.currentParse, text, offset) : text;
   }

   @Nullable
   static String calculateSuggestionSuffix(String inputText, String suggestionText) {
      return suggestionText.startsWith(inputText) ? suggestionText.substring(inputText.length()) : null;
   }

   private static String formatException(CommandSyntaxException exception) {
      IChatComponent component = ComponentUtils.fromMessage(exception.getRawMessage());
      String context = exception.getContext();
      return context == null
         ? component.getFormattedText()
         : new ChatComponentText(String.format("%s at position %s: %s", component.getFormattedText(), exception.getCursor(), context)).getFormattedText();
   }

   private static String formatParsedCommand(ParseResults<ISuggestionProvider> parseResults, String command, int maxLength) {
      String grayCode = EnumChatFormatting.GRAY.toString();
      StringBuilder result = new StringBuilder(grayCode);
      int currentPos = 0;
      int colorIndex = -1;
      CommandContextBuilder<ISuggestionProvider> context = parseResults.getContext().getLastChild();

      for (ParsedArgument<ISuggestionProvider, ?> argument : context.getArguments().values()) {
         colorIndex = (colorIndex + 1) % ARGUMENT_COLORS.length;
         int start = Math.max(argument.getRange().getStart() - maxLength, 0);
         if (start >= command.length()) {
            break;
         }

         int end = Math.min(argument.getRange().getEnd() - maxLength, command.length());
         if (end > 0) {
            result.append(command, currentPos, start);
            result.append(ARGUMENT_COLORS[colorIndex]);
            result.append(command, start, end);
            result.append(grayCode);
            currentPos = end;
         }
      }

      if (parseResults.getReader().canRead()) {
         int errorStart = Math.max(parseResults.getReader().getCursor() - maxLength, 0);
         if (errorStart < command.length()) {
            int errorEnd = Math.min(errorStart + parseResults.getReader().getRemainingLength(), command.length());
            result.append(command, currentPos, errorStart);
            result.append(EnumChatFormatting.RED);
            result.append(command, errorStart, errorEnd);
            currentPos = errorEnd;
         }
      }

      result.append(command, currentPos, command.length());
      return result.toString();
   }

   public void render(int mouseX, int mouseY) {
      if (this.suggestions != null) {
         this.suggestions.render(mouseX, mouseY);
      } else {
         this.renderUsageInfo();
      }
   }

   private void renderUsageInfo() {
      for (int i = 0; i < this.commandUsage.size(); i++) {
         String usage = this.commandUsage.get(i);
         int y = this.config.anchorToBottom ? this.screen.height - 14 - 13 - 12 * i : 72 + 12 * i;
         Gui.drawRect(this.commandUsagePosition - 1, y, this.commandUsagePosition + this.commandUsageWidth + 1, y + 12, this.config.fillColor);
         this.fontRenderer.drawStringWithShadow(usage, this.commandUsagePosition, y + 2, -1);
      }
   }

   public static class CommandSuggestionsConfig {
      public final boolean commandsOnly;
      public final boolean onlyShowIfCursorPastError;
      public final int lineStartOffset;
      public final int suggestionLineLimit;
      public final boolean anchorToBottom;
      public final int fillColor;

      public CommandSuggestionsConfig(
         boolean commandsOnly, boolean onlyShowIfCursorPastError, int lineStartOffset, int suggestionLineLimit, boolean anchorToBottom, int fillColor
      ) {
         this.commandsOnly = commandsOnly;
         this.onlyShowIfCursorPastError = onlyShowIfCursorPastError;
         this.lineStartOffset = lineStartOffset;
         this.suggestionLineLimit = suggestionLineLimit;
         this.anchorToBottom = anchorToBottom;
         this.fillColor = fillColor;
      }
   }

   public class SuggestionsList {
      private final Rect2i bounds;
      private final String originalText;
      private final List<Suggestion> suggestions;
      private int scrollOffset;
      private int selectedIndex;
      private float lastMouseX = 0;
      private float lastMouseY = 0;
      private boolean tabCycles;

      SuggestionsList(int x, int y, int width, List<Suggestion> suggestions) {
         int actualY = CommandSuggestions.this.config.anchorToBottom
            ? y - 3 - Math.min(suggestions.size(), CommandSuggestions.this.config.suggestionLineLimit) * 12
            : y;
         this.bounds = new Rect2i(x - 1, actualY, width + 1, Math.min(suggestions.size(), CommandSuggestions.this.config.suggestionLineLimit) * 12);
         this.originalText = CommandSuggestions.this.input.getText();
         this.suggestions = suggestions;
         this.select(0);
      }

      public void render(int mouseX, int mouseY) {
         int visibleCount = Math.min(this.suggestions.size(), CommandSuggestions.this.config.suggestionLineLimit);
         boolean hasScrollUp = this.scrollOffset > 0;
         boolean hasScrollDown = this.suggestions.size() > this.scrollOffset + visibleCount;
         boolean hasMouseMoved = this.lastMouseX != mouseX || this.lastMouseY != mouseY;
         if (hasMouseMoved) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
         }

         this.renderScrollIndicators(hasScrollUp, hasScrollDown);
         this.renderSuggestions(mouseX, mouseY, visibleCount, hasMouseMoved);
      }

      private void renderScrollIndicators(boolean hasScrollUp, boolean hasScrollDown) {
         if (hasScrollUp || hasScrollDown) {
            Gui.drawRect(this.bounds.x(), this.bounds.y() - 1, this.bounds.right(), this.bounds.y(), CommandSuggestions.this.config.fillColor);
            Gui.drawRect(this.bounds.x(), this.bounds.bottom(), this.bounds.right(), this.bounds.bottom() + 1, CommandSuggestions.this.config.fillColor);
            if (hasScrollUp) {
               for (int i = 0; i < this.bounds.width(); i += 2) {
                  Gui.drawRect(this.bounds.x() + i, this.bounds.y() - 1, this.bounds.x() + i + 1, this.bounds.y(), -1);
               }
            }

            if (hasScrollDown) {
               for (int i = 0; i < this.bounds.width(); i += 2) {
                  Gui.drawRect(this.bounds.x() + i, this.bounds.bottom(), this.bounds.x() + i + 1, this.bounds.bottom() + 1, -1);
               }
            }
         }
      }

      private void renderSuggestions(int mouseX, int mouseY, int visibleCount, boolean hasMouseMoved) {
         boolean showTooltip = false;

         for (int i = 0; i < visibleCount; i++) {
            Suggestion suggestion = this.suggestions.get(i + this.scrollOffset);
            int itemY = this.bounds.y() + 12 * i;
            Gui.drawRect(this.bounds.x(), itemY, this.bounds.right(), itemY + 12, CommandSuggestions.this.config.fillColor);
            boolean isHovered = mouseX > this.bounds.x() && mouseX < this.bounds.right() && mouseY > itemY && mouseY < itemY + 12;
            if (isHovered) {
               if (hasMouseMoved) {
                  this.select(i + this.scrollOffset);
               }

               showTooltip = true;
            }

            int textColor = i + this.scrollOffset == this.selectedIndex ? -256 : -5592406;
            CommandSuggestions.this.fontRenderer.drawStringWithShadow(suggestion.getText(), this.bounds.x() + 1, itemY + 2, textColor);
         }

         if (showTooltip) {
            Message tooltip = this.suggestions.get(this.selectedIndex).getTooltip();
            if (tooltip != null) {
               // Draw tooltip as simple text - 1.7.10 drawHoveringText takes a List
               List<String> tooltipLines = new ArrayList<String>();
               tooltipLines.add(ComponentUtils.fromMessage(tooltip).getFormattedText());
               try {
                  java.lang.reflect.Method m = net.minecraft.client.gui.GuiScreen.class.getDeclaredMethod("func_146283_a", List.class, int.class, int.class);
                  m.setAccessible(true);
                  m.invoke(CommandSuggestions.this.screen, tooltipLines, mouseX, mouseY);
               } catch (Exception e) {
                  // fallback: just ignore tooltip
               }
            }
         }
      }

      public boolean handleMouseClick(int mouseX, int mouseY, int button) {
         if (!this.bounds.contains(mouseX, mouseY)) {
            return false;
         } else {
            int clickedIndex = (mouseY - this.bounds.y()) / 12 + this.scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < this.suggestions.size()) {
               this.select(clickedIndex);
               this.applySuggestion();
            }

            return true;
         }
      }

      public boolean handleMouseScroll(double delta) {
         ScaledResolution resolution = new ScaledResolution(CommandSuggestions.this.minecraft, CommandSuggestions.this.minecraft.displayWidth, CommandSuggestions.this.minecraft.displayHeight);
         int mouseX = Mouse.getX() * resolution.getScaledWidth() / CommandSuggestions.this.minecraft.displayWidth;
         int mouseY = resolution.getScaledHeight() - Mouse.getY() * resolution.getScaledHeight() / CommandSuggestions.this.minecraft.displayHeight - 1;
         if (this.bounds.contains(mouseX, mouseY)) {
            this.scrollOffset = MathHelper.clamp_int(
               (int)(this.scrollOffset - delta), 0, Math.max(this.suggestions.size() - CommandSuggestions.this.config.suggestionLineLimit, 0)
            );
            return true;
         } else {
            return false;
         }
      }

      public boolean handleKeyPress(int keyCode) {
         switch (keyCode) {
            case 1:
               this.hide();
               return true;
            case 15:
               if (this.tabCycles) {
                  this.cycle(GuiScreen.isShiftKeyDown() ? -1 : 1);
               }

               this.applySuggestion();
               return true;
            case 200:
               this.cycle(-1);
               this.tabCycles = false;
               return true;
            case 208:
               this.cycle(1);
               this.tabCycles = false;
               return true;
            default:
               return false;
         }
      }

      public void cycle(int direction) {
         this.select(this.selectedIndex + direction);
         this.updateScrollOffset();
      }

      private void updateScrollOffset() {
         int visibleStart = this.scrollOffset;
         int visibleEnd = this.scrollOffset + CommandSuggestions.this.config.suggestionLineLimit - 1;
         if (this.selectedIndex < visibleStart) {
            this.scrollOffset = MathHelper.clamp_int(
               this.selectedIndex, 0, Math.max(this.suggestions.size() - CommandSuggestions.this.config.suggestionLineLimit, 0)
            );
         } else if (this.selectedIndex > visibleEnd) {
            this.scrollOffset = MathHelper.clamp_int(
               this.selectedIndex + CommandSuggestions.this.config.lineStartOffset - CommandSuggestions.this.config.suggestionLineLimit,
               0,
               Math.max(this.suggestions.size() - CommandSuggestions.this.config.suggestionLineLimit, 0)
            );
         }
      }

      public void select(int index) {
         this.selectedIndex = (index % this.suggestions.size() + this.suggestions.size()) % this.suggestions.size();
         Suggestion suggestion = this.suggestions.get(this.selectedIndex);
         String suffix = CommandSuggestions.calculateSuggestionSuffix(CommandSuggestions.this.input.getText(), suggestion.apply(this.originalText));
         ((GuiTextFieldExtras)CommandSuggestions.this.input).brigo$suggestion(suffix);
      }

      public void applySuggestion() {
         Suggestion suggestion = this.suggestions.get(this.selectedIndex);
         CommandSuggestions.this.keepSuggestions = true;
         String newText = suggestion.apply(this.originalText);
         CommandSuggestions.this.input.setText(newText);
         int cursorPos = suggestion.getRange().getStart() + suggestion.getText().length();
         CommandSuggestions.this.input.setCursorPosition(cursorPos);
         CommandSuggestions.this.input.setSelectionPos(cursorPos);
         this.select(this.selectedIndex);
         CommandSuggestions.this.keepSuggestions = false;
         this.tabCycles = true;
      }

      public void hide() {
         this.suggestions.clear();
         CommandSuggestions.this.suggestions = null;
      }
   }
}
// spotless:on
