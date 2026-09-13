package com.xyp.ldlib.gui.ui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;

import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/** Client-only LDLib2-style textured button with hover feedback and left-click consumption. */
public final class Button extends UIElement {

    private final Runnable callback;
    private IGuiTexture hoverTexture;
    private IGuiTexture focusTexture;
    private IGuiTexture disabledTexture;
    private boolean focused;

    public Button(int x, int y, int width, int height, IGuiTexture texture, Runnable callback) {
        super(x, y, width, height);
        setBackground(texture);
        this.callback = callback;
        setFocusable(true);
        addEventListener(UIEvents.FOCUS, e -> focused = true);
        addEventListener(UIEvents.BLUR, e -> focused = false);
        addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0 && !event.isDefaultPrevented()) {
                activate();
                event.preventDefault();
                event.stopPropagation();
            }
        });
        addEventListener(UIEvents.KEY_DOWN, event -> {
            if (event.keyCode == Keyboard.KEY_SPACE || event.keyCode == Keyboard.KEY_RETURN) {
                activate();
                event.preventDefault();
                event.stopPropagation();
            }
        });
    }

    public Button setHoverTexture(IGuiTexture texture) {
        hoverTexture = texture;
        return this;
    }

    public Button setFocusTexture(IGuiTexture texture) {
        focusTexture = texture;
        return this;
    }

    public Button setDisabledTexture(IGuiTexture texture) {
        disabledTexture = texture;
        return this;
    }

    @Override
    protected void drawForeground(int mx, int my, int px, int py) {
        if (focused && focusTexture != null) focusTexture.draw(mx, my, px + x, py + y, width, height);
    }

    @Override
    protected void drawBackground(int mouseX, int mouseY, int parentX, int parentY) {
        if (!isInteractive() && disabledTexture != null) {
            disabledTexture.draw(mouseX, mouseY, parentX + x, parentY + y, width, height);
        } else if (isInteractive() && hoverTexture != null && (focused || contains(mouseX, mouseY, parentX, parentY))) {
            hoverTexture.draw(mouseX, mouseY, parentX + x, parentY + y, width, height);
        } else {
            super.drawBackground(mouseX, mouseY, parentX, parentY);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button, int parentX, int parentY) {
        if (!isInteractive() || button != 0 || !contains(mouseX, mouseY, parentX, parentY)) return false;
        if (super.mouseClicked(mouseX, mouseY, button, parentX, parentY)) return true;
        activate();
        return true;
    }

    private void activate() {
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1f));
        callback.run();
    }
}
