package com.xyp.ldlib.gui.fancy;

import java.util.function.Consumer;

import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.texture.TextTexture;
import com.xyp.ldlib.gui.ui.elements.Button;
import com.xyp.ldlib.gui.ui.elements.Flow;
import com.xyp.ldlib.gui.ui.elements.ScrollerView;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/**
 * Vertical GTCEu Fancy tabs adapted to the LDLib2 element tree.
 * A clipped scroller keeps arbitrarily long page collections inside the screen.
 */
public final class VerticalTabsWidget extends TabsWidget {

    private final ModernTheme theme;

    public VerticalTabsWidget(int x, int y, int width, int height, ModernTheme theme,
        Consumer<IFancyUIProvider> onTabClick) {
        super(x, y, width, height, onTabClick);
        this.theme = theme;
    }

    @Override
    protected void rebuildTabs() {
        clearAllChildren();
        ScrollerView viewport = theme.scroller(0, 0, width, height);
        Flow rows = new Flow(0, 0, width, tabs.size() * 30, true).setGap(2);
        for (IFancyUIProvider page : tabs) {
            IGuiTexture icon = page.getTabIcon();
            IGuiTexture label = icon != null ? icon : new TextTexture(page::getTitle, 0xFF202830);
            IGuiTexture normal = (mx, my, x, y, w, h) -> {
                (selectedTab == page ? theme.selectedTab : theme.tab).draw(mx, my, x, y, w, h);
                label.draw(mx, my, x + 4, y + 4, w - 8, h - 8);
            };
            IGuiTexture hover = (mx, my, x, y, w, h) -> {
                theme.selectedTab.draw(mx, my, x, y, w, h);
                label.draw(mx, my, x + 4, y + 4, w - 8, h - 8);
            };
            rows.addChild(
                new Button(0, 0, width, 28, normal, () -> select(page)).setHoverTexture(hover)
                    .setFocusTexture(theme.focus));
        }
        viewport.addChild(rows);
        addChild(viewport);
    }
}
