package com.xyp.ldlib.gui.ui.style;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.util.ResourceLocation;

import com.xyp.ldlib.gui.texture.ColorBorderTexture;
import com.xyp.ldlib.gui.texture.GuiTextureGroup;
import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.texture.SpriteTexture;
import com.xyp.ldlib.gui.texture.TextTexture;
import com.xyp.ldlib.gui.ui.elements.Button;
import com.xyp.ldlib.gui.ui.elements.ScrollerView;
import com.xyp.ldlib.gui.ui.elements.TextField;

/**
 * AE2 1.21.1 theme with Modernity-GTNH-UI slot and side-tab adaptations.
 * The host supplies resource resolution; the GUI library has no dependency on addon registries.
 * See META-INF/ldlib-port/NOTICE.md for asset provenance.
 */
public final class ModernTheme {

    public final SpriteTexture panel, title, button, slot, tab, selectedTab, accent;
    public final IGuiTexture hover, focus;
    public final SpriteTexture disabled, input, inputFocused, inputDisabled, scrollThumb;

    public ModernTheme(Function<String, ResourceLocation> resources) {
        panel = new SpriteTexture(resources.apply("modern/background.png"), 256, 256, 2);
        title = panel.copy();
        button = new SpriteTexture(resources.apply("modern/button.png"), 200, 20, 3);
        disabled = new SpriteTexture(resources.apply("modern/button_disabled.png"), 200, 20, 3);
        input = new SpriteTexture(resources.apply("modern/text_field.png"), 128, 128, 0).setSprite(0, 0, 128, 12)
            .setBorder(1, 1, 1, 1);
        inputFocused = input.copy()
            .setSprite(0, 24, 128, 12);
        inputDisabled = input.copy()
            .setSprite(0, 12, 128, 12);
        scrollThumb = new SpriteTexture(resources.apply("modern/small_scroller.png"), 7, 15, 2);
        slot = new SpriteTexture(resources.apply("modern/slot.png"), 18, 18, 1);
        tab = new SpriteTexture(resources.apply("modern/tabs_left.png"), 64, 84, 0).setSprite(0, 28, 32, 28)
            .setBorder(3, 3, 3, 3);
        selectedTab = tab.copy()
            .setSprite(32, 28, 32, 28);
        accent = new SpriteTexture(resources.apply("modern/button_highlighted.png"), 200, 20, 3);
        hover = accent;
        focus = new ColorBorderTexture(1, 0xFFA5D8FF);
    }

    public IGuiTexture text(IGuiTexture background, Supplier<String> text) {
        return new GuiTextureGroup(background, new TextTexture(text, 0xFF202830));
    }

    public Button button(int x, int y, int width, int height, Supplier<String> text, Runnable callback) {
        return new Button(x, y, width, height, text(button, text), callback).setHoverTexture(text(hover, text))
            .setDisabledTexture(new GuiTextureGroup(disabled, new TextTexture(text, 0xFFA0A0AB)))
            .setFocusTexture(focus);
    }

    public TextField textField(int x, int y, int width, int height) {
        return new TextField(x, y, width, height).setTextures(input, inputFocused, inputDisabled);
    }

    public ScrollerView scroller(int x, int y, int width, int height) {
        return new ScrollerView(x, y, width, height).setThumbTexture(scrollThumb);
    }
}
