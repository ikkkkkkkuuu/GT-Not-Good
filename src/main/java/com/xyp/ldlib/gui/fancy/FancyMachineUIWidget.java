package com.xyp.ldlib.gui.fancy;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.elements.Label;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/**
 * Reduced GTCEu Fancy shell: title, vertical tabs, content panel and cached provider pages.
 * Despite the upstream name this class has no GregTech or machine dependency.
 * Pages retain local edits when revisited; the screen host clears focus on detached pages.
 */
public final class FancyMachineUIWidget extends UIElement {

    private final UIElement pageContainer;
    private final VerticalTabsWidget sideTabsWidget;
    private final Map<IFancyUIProvider, UIElement> pages = new IdentityHashMap<>();
    private IFancyUIProvider currentPage;

    public FancyMachineUIWidget(IFancyUIProvider mainPage, int width, int height, ModernTheme theme) {
        super(0, 0, width, height);
        if (width < 160 || height < 100) throw new IllegalArgumentException("Fancy window too small");
        addChild(new UIElement(71, 27, width - 71, height - 27).setBackground(theme.panel));
        addChild(new UIElement(71, 0, width - 71, 28).setBackground(theme.title));
        addChild(
            new Label(79, 5, width - 87, 18, () -> currentPage == null ? mainPage.getTitle() : currentPage.getTitle()));
        pageContainer = new UIElement(77, 35, width - 85, height - 65);
        addChild(pageContainer);
        sideTabsWidget = new VerticalTabsWidget(0, 34, 75, height - 40, theme, this::openPage);
        addChild(sideTabsWidget);
        sideTabsWidget.setMainTab(mainPage);
        mainPage.attachSideTabs(sideTabsWidget);
        sideTabsWidget.select(mainPage);
    }

    public int getContentWidth() {
        return pageContainer.getWidth();
    }

    public int getContentHeight() {
        return pageContainer.getHeight();
    }

    public TabsWidget getSideTabsWidget() {
        return sideTabsWidget;
    }

    public IFancyUIProvider getCurrentPage() {
        return currentPage;
    }

    public void navigate(IFancyUIProvider page) {
        sideTabsWidget.select(page);
    }

    private void openPage(IFancyUIProvider page) {
        UIElement content = pages.get(page);
        if (content == null) {
            content = Objects.requireNonNull(page.createMainPage(this), "Page content");
            if (content.getParent() != null || content == this
                || content == pageContainer
                || content.getWidth() > getContentWidth()
                || content.getHeight() > getContentHeight()) {
                throw new IllegalArgumentException("Page must be detached and fit inside the content area");
            }
            content.setPosition(0, 0);
            pages.put(page, content);
        }
        pageContainer.clearAllChildren();
        pageContainer.addChild(content);
        currentPage = page;
    }
}
