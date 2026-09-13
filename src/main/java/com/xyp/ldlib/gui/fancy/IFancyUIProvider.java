package com.xyp.ldlib.gui.fancy;

import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.ui.UIElement;

/**
 * Client-only port of GTCEu's Fancy page contract. Pages supply content and navigation metadata.
 * Player inventory and machine/network bindings are deliberately owned by the application.
 */
public interface IFancyUIProvider {

    UIElement createMainPage(FancyMachineUIWidget window);

    String getTitle();

    default IGuiTexture getTabIcon() {
        return null;
    }

    default void attachSideTabs(TabsWidget tabs) {}
}
