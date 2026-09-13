package com.xyp.ldlib.integration.modularui;

import java.util.function.Function;

import net.minecraft.util.ResourceLocation;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/**
 * Optional MUI2 bridge for the LDLib theme. Retains MUI2 input, ghost slots and network synchronization.
 * Resource resolution is supplied by the host; texture rendering occurs only on the client.
 */
public final class ModernThemeAdapter {

    public final ModernTheme theme;
    public final IDrawable panel, button, hover, disabled, slot, tab, selectedTab, input, inputFocused;

    public ModernThemeAdapter(Function<String, ResourceLocation> resources) {
        theme = new ModernTheme(resources);
        panel = drawable(theme.panel);
        button = drawable(theme.button);
        hover = drawable(theme.hover);
        disabled = drawable(theme.disabled);
        slot = drawable(theme.slot);
        tab = drawable(theme.tab);
        selectedTab = drawable(theme.selectedTab);
        input = drawable(theme.input);
        inputFocused = drawable(theme.inputFocused);
    }

    /** Ignores MUI color tint so upstream artwork keeps the same colors as the LDLib screen. */
    public static IDrawable drawable(IGuiTexture texture) {
        return (context, x, y, width, height, widgetTheme) -> texture.draw(0, 0, x, y, width, height);
    }

    public ButtonWidget<?> button() {
        ButtonWidget<?> widget = new ButtonWidget<>();
        return widget.background(new DynamicDrawable(() -> widget.isEnabled() ? button : disabled))
            .hoverBackground(hover);
    }

    public TextFieldWidget textField() {
        TextFieldWidget field = new TextFieldWidget();
        return field.background(new DynamicDrawable(() -> field.isFocused() ? inputFocused : input))
            .padding(3, 0)
            .setTextAlignment(com.cleanroommc.modularui.utils.Alignment.CenterLeft)
            .setTextColor(0xFFFFFFFF);
    }
}
