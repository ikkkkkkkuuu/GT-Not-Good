package com.xyp.ldlib.gui.fancy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.xyp.ldlib.gui.ui.UIElement;

/** GTCEu-style page collection with validated local selection and a reusable navigation callback. */
public class TabsWidget extends UIElement {

    protected final List<IFancyUIProvider> tabs = new ArrayList<>();
    protected IFancyUIProvider selectedTab;
    private final Consumer<IFancyUIProvider> onTabClick;

    public TabsWidget(int x, int y, int width, int height, Consumer<IFancyUIProvider> onTabClick) {
        super(x, y, width, height);
        this.onTabClick = Objects.requireNonNull(onTabClick);
    }

    public void setMainTab(IFancyUIProvider main) {
        if (!tabs.isEmpty()) throw new IllegalStateException("Main tab already configured");
        attachSubTab(main);
    }

    public void attachSubTab(IFancyUIProvider provider) {
        Objects.requireNonNull(provider);
        if (tabs.contains(provider)) throw new IllegalArgumentException("Duplicate page");
        tabs.add(provider);
        rebuildTabs();
    }

    public List<IFancyUIProvider> getTabs() {
        return Collections.unmodifiableList(tabs);
    }

    public IFancyUIProvider getSelectedTab() {
        return selectedTab;
    }

    public void select(IFancyUIProvider page) {
        if (!tabs.contains(page)) throw new IllegalArgumentException("Unknown page");
        if (selectedTab == page) return;
        onTabClick.accept(page);
        selectedTab = page;
    }

    protected void rebuildTabs() {}
}
