// spotless:off
package com.xyp.gtnotgood.mixins.early.commandtree;

import com.xyp.gtnotgood.commandtree.accessor.GuiTextFieldExtras;
import com.xyp.gtnotgood.utils.text.effect.EffectTextParser;
import java.util.function.BiFunction;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds command colors and inline completion to the chat input field. */
@Mixin(GuiTextField.class)
/** Ported command-tree GuiTextFieldMixin used by the integrated chat suggestions. */
public abstract class GuiTextFieldMixin implements GuiTextFieldExtras {
   @Shadow
   private String text; // text
   @Shadow
   private FontRenderer field_146211_a; // fontRendererInstance
   @Shadow
   public int xPosition; // xPosition
   @Shadow
   public int yPosition; // yPosition
   @Shadow
   private int width; // width
   @Shadow
   private int height; // height
   @Shadow
   private int cursorPosition; // cursorPosition
   @Shadow
   private int lineScrollOffset; // lineScrollOffset
   @Shadow
   private int selectionEnd; // selectionEnd
   @Shadow
   private boolean isFocused; // isFocused
   @Shadow
   private int cursorCounter; // cursorCounter
   @Shadow
   private boolean isEnabled; // isEnabled
   @Shadow
   private int enabledColor; // enabledColor
   @Shadow
   private int disabledColor; // disabledColor
   @Shadow
   private boolean enableBackgroundDrawing; // enableBackgroundDrawing

   @Unique
   private String brigo$currentSuggestion;
   @Unique
   private boolean brigo$chatInput;
   @Unique
   private BiFunction<String, Integer, String> brigo$textFormatter = new BiFunction<String, Integer, String>() {
      @Override
      public String apply(String text, Integer pos) {
         return text;
      }
   };

   @Shadow
   public abstract boolean getVisible(); // getVisible

   @Shadow
   public abstract boolean getEnableBackgroundDrawing(); // getEnableBackgroundDrawing

   @Shadow
   public abstract int getMaxStringLength(); // getMaxStringLength

   @Shadow
   protected abstract void drawCursorVertical(int startX, int startY, int endX, int endY); // drawCursorVertical

   @Inject(method = "drawTextBox", at = @At("HEAD"), cancellable = true)
   private void renderCustomTextBox(CallbackInfo ci) {
      if (!this.brigo$chatInput || EffectTextParser.containsMarkers(this.text)) return;
      if (!this.getVisible()) {
         ci.cancel();
         return;
      }

      this.brigo$renderBackground();

      int textColor = this.isEnabled ? this.enabledColor : this.disabledColor;
      int cursorPos = this.cursorPosition - this.lineScrollOffset;
      int selEnd = this.selectionEnd - this.lineScrollOffset;
      int innerWidth = this.enableBackgroundDrawing ? this.width - 8 : this.width;
      String visibleText = this.field_146211_a.trimStringToWidth(this.text.substring(this.lineScrollOffset), innerWidth);
      boolean shouldShowCursor = cursorPos >= 0 && cursorPos <= visibleText.length();
      boolean showBlinkingCursor = this.isFocused && this.cursorCounter / 6 % 2 == 0 && shouldShowCursor;
      int textX = this.enableBackgroundDrawing ? this.xPosition + 4 : this.xPosition;
      int textY = this.enableBackgroundDrawing ? this.yPosition + (this.height - 8) / 2 : this.yPosition;
      int renderX = textX;
      selEnd = Math.min(selEnd, visibleText.length());

      if (!visibleText.isEmpty()) {
         String textBeforeCursor = shouldShowCursor ? visibleText.substring(0, cursorPos) : visibleText;
         renderX = this.field_146211_a.drawStringWithShadow(this.brigo$textFormatter.apply(textBeforeCursor, this.lineScrollOffset), textX, textY, textColor);
      }

      boolean isAtEnd = this.cursorPosition < this.text.length() || this.text.length() >= this.getMaxStringLength();
      int cursorX = shouldShowCursor ? (isAtEnd ? renderX - 1 : renderX) : (cursorPos > 0 ? textX + this.width : textX);

      if (!visibleText.isEmpty() && shouldShowCursor && cursorPos < visibleText.length()) {
         this.field_146211_a.drawStringWithShadow(this.brigo$textFormatter.apply(visibleText.substring(cursorPos), this.lineScrollOffset), renderX, textY, textColor);
      }

      if (!isAtEnd && this.brigo$currentSuggestion != null) {
         this.field_146211_a.drawStringWithShadow(this.brigo$currentSuggestion, cursorX - 1, textY, -8355712);
      }

      if (showBlinkingCursor) {
         if (isAtEnd) {
            Gui.drawRect(cursorX, textY - 1, cursorX + 1, textY + 1 + this.field_146211_a.FONT_HEIGHT, -3092272);
         } else {
            this.field_146211_a.drawStringWithShadow("_", cursorX, textY, textColor);
         }
      }

      if (selEnd != cursorPos) {
         int selectionX = textX + this.field_146211_a.getStringWidth(visibleText.substring(0, selEnd));
         this.drawCursorVertical(cursorX, textY - 1, selectionX - 1, textY + 1 + this.field_146211_a.FONT_HEIGHT);
      }

      ci.cancel();
   }

   @Unique
   private void brigo$renderBackground() {
      if (this.getEnableBackgroundDrawing()) {
         Gui.drawRect(
            this.xPosition - 1,
            this.yPosition - 1,
            this.xPosition + this.width + 1,
            this.yPosition + this.height + 1,
            -6250336
         );
         Gui.drawRect(
            this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, -16777216
         );
      }
   }

   @Unique
   @Override
   public void brigo$suggestion(@Nullable String suggestion) {
      this.brigo$currentSuggestion = suggestion;
   }

   @Unique
   @Override
   public void brigo$chatInput(boolean enabled) {
      this.brigo$chatInput = enabled;
   }

   @Unique
   @Override
   public void brigo$textFormatter(BiFunction<String, Integer, String> formatter) {
      this.brigo$textFormatter = formatter;
   }

   @Unique
   @Override
   public int brigo$screenX(int position) {
      return position > this.text.length()
         ? this.xPosition
         : this.xPosition + this.field_146211_a.getStringWidth(this.text.substring(0, position));
   }
}
// spotless:on
