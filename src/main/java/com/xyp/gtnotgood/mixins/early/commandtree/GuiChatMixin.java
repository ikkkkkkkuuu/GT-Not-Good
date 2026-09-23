// spotless:off
package com.xyp.gtnotgood.mixins.early.commandtree;

import com.xyp.gtnotgood.commandtree.client.gui.CommandSuggestions;
import com.xyp.gtnotgood.commandtree.accessor.GuiTextFieldExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Connects Brigadier suggestions to the vanilla chat screen. */
@Mixin(GuiChat.class)
/** Ported command-tree GuiChatMixin used by the integrated chat suggestions. */
public class GuiChatMixin extends GuiScreen {
   @Shadow
   protected GuiTextField inputField; // inputField
   @Shadow
   private String defaultInputFieldText; // defaultInputFieldText
   @Unique
   private CommandSuggestions brigo$commandSuggestions;
   @Unique
   private String brigo$lastText = "";

   @Inject(method = "initGui", at = @At("TAIL"))
   private void onInitGui(CallbackInfo ci) {
      this.brigo$initializeCommandSuggestions();
   }

   @Unique
   private void brigo$initializeCommandSuggestions() {
      ((GuiTextFieldExtras) this.inputField).brigo$chatInput(true);
      CommandSuggestions.CommandSuggestionsConfig config = new CommandSuggestions.CommandSuggestionsConfig(false, false, 1, 10, true, -805306368);
      this.brigo$commandSuggestions = new CommandSuggestions(this.mc, this, this.inputField, this.fontRendererObj, config);
      this.brigo$commandSuggestions.updateCommandInfo();
      this.brigo$lastText = this.inputField.getText();
   }

   @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
   private void onHandleMouseInput(CallbackInfo ci) {
      if (this.brigo$commandSuggestions != null && this.brigo$commandSuggestions.handleMouseScroll(Mouse.getEventDWheel())) {
         ci.cancel();
      }
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
      if (this.brigo$commandSuggestions != null && this.brigo$commandSuggestions.handleMouseClick(mouseX, mouseY, mouseButton)) {
         ci.cancel();
      }
   }

   @Inject(method = "drawScreen", at = @At("TAIL"))
   private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      if (this.brigo$commandSuggestions != null) {
         // Poll for text changes since 1.7.10 has no GuiResponder
         String currentText = this.inputField.getText();
         if (!currentText.equals(this.brigo$lastText)) {
            this.brigo$lastText = currentText;
            this.brigo$commandSuggestions.allowSuggestions(!currentText.equals(this.defaultInputFieldText));
            this.brigo$commandSuggestions.updateCommandInfo();
         }
         this.brigo$commandSuggestions.render(mouseX, mouseY);
      }
   }

   @Override
   public void handleKeyboardInput() {
      if (Keyboard.getEventKeyState() && this.brigo$commandSuggestions != null && this.brigo$commandSuggestions.handleKeyPress(Keyboard.getEventKey())) {
         // Brigo handled the key press
      } else {
         super.handleKeyboardInput();
      }
   }
}
// spotless:on
