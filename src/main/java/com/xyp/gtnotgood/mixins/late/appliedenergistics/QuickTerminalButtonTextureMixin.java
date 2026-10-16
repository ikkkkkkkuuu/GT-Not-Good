package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.QuickTerminalButtonTextures;

import appeng.api.config.ActionItems;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.core.AEConfig;

@Mixin(value = GuiImgButton.class, remap = false)
public abstract class QuickTerminalButtonTextureMixin {

    @Inject(method = "getMessage", at = @At("HEAD"), cancellable = true)
    private void fullRowTooltip(CallbackInfoReturnable<String> cir) {
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiQuickEncodingTerminal)) return;
        Enum<?> action = ((GuiImgButton) (Object) this).getCurrentValue();
        if (
            action != ActionItems.TOGGLE_SHOW_FULL_INTERFACES_OFF
                && action != ActionItems.TOGGLE_SHOW_FULL_INTERFACES_ON
        ) return;
        // #tr gtnotgood.terminal.quick.hide_full_rows.on
        // # Hide filled pattern rows
        // # zh_CN 隐藏已满样板行
        String enabled = StatCollector.translateToLocal("gtnotgood.terminal.quick.hide_full_rows.on");
        // #tr gtnotgood.terminal.quick.hide_full_rows.off
        // # Show all pattern rows
        // # zh_CN 显示全部样板行
        String disabled = StatCollector.translateToLocal("gtnotgood.terminal.quick.hide_full_rows.off");
        // #tr gtnotgood.terminal.quick.hide_full_rows.detail
        // # Keep the last filled row visible for editing
        // # zh_CN 保留最后一行已满样板，便于修改
        String detail = StatCollector.translateToLocal("gtnotgood.terminal.quick.hide_full_rows.detail");
        cir.setReturnValue(
            (AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal ? enabled : disabled) + "\n"
                + detail);
    }

    @ModifyArg(
        method = "drawButton",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
            remap = true),
        index = 0,
        remap = true,
        require = 1)
    private ResourceLocation gtnotgood$useModernTerminalButtons(ResourceLocation original) {
        return Minecraft.getMinecraft().currentScreen instanceof GuiQuickEncodingTerminal
            ? QuickTerminalButtonTextures.states()
            : original;
    }
}
