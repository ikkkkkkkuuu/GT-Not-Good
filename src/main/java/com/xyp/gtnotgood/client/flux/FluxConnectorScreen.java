/* Keyboard flow ported from Flux Networks GuiFocusable; copyright SonarSonic/BloCamLimb, MIT. */
package com.xyp.gtnotgood.client.flux;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.PagedWidget;

/**
 * Retains Flux's keyboard interaction: Enter/Escape first leave an editor, Escape from a tab
 * returns home, and only Escape from home closes the screen. Focused typing consumes game shortcuts.
 */
public final class FluxConnectorScreen extends ModularScreen {

    public FluxConnectorScreen(String owner, ModularPanel panel) {
        super(owner, panel);
    }

    @Override
    public boolean onKeyPressed(char character, int key) {
        boolean close = key == Keyboard.KEY_ESCAPE
            || key == Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode();
        if (getContext().isFocused()) {
            if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                getContext().removeFocus();
                return true;
            }
            super.onKeyPressed(character, key);
            return true;
        }
        if (close) {
            for (com.cleanroommc.modularui.api.widget.IWidget widget : getMainPanel().getChildren()) {
                if (widget instanceof PagedWidget<?>pages && pages.getCurrentPageIndex() != 0) {
                    pages.setPage(0);
                    return true;
                }
            }
        }
        return super.onKeyPressed(character, key);
    }
}
