package com.xyp.ldlib.gui.ui;

import static org.junit.Assert.*;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;

import com.xyp.ldlib.gui.fancy.FancyMachineUIWidget;
import com.xyp.ldlib.gui.fancy.IFancyUIProvider;
import com.xyp.ldlib.gui.fancy.TabsWidget;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/** Regression coverage for cached Fancy pages and detached input ownership. */
public class FancyPageTest {

    @Test
    public void revisitingPagePreservesItsControlAndClearsDetachedFocus() {
        int[] builds = { 0 };
        UIElement field = new UIElement(0, 0, 40, 40).setFocusable(true);
        IFancyUIProvider second = page("second", new UIElement(0, 0, 20, 20));
        IFancyUIProvider main = new IFancyUIProvider() {

            @Override
            public String getTitle() {
                return "main";
            }

            @Override
            public UIElement createMainPage(FancyMachineUIWidget window) {
                builds[0]++;
                return field;
            }

            @Override
            public void attachSideTabs(TabsWidget tabs) {
                tabs.attachSubTab(second);
            }
        };
        FancyMachineUIWidget window = new FancyMachineUIWidget(
            main,
            310,
            226,
            new ModernTheme(path -> new ResourceLocation("test", path)));
        UIInput input = new UIInput(window);
        input.focus(field);
        assertSame(field, input.getFocused());
        field.setSize(35, 35);
        window.navigate(second);
        assertNull(input.getFocused());
        window.navigate(main);
        assertEquals(1, builds[0]);
        assertEquals(35, field.getWidth());
        assertSame(main, window.getCurrentPage());
        IFancyUIProvider bad = page("bad", new UIElement(0, 0, 500, 500));
        window.getSideTabsWidget()
            .attachSubTab(bad);
        try {
            window.navigate(bad);
            fail("Expected invalid page rejection");
        } catch (IllegalArgumentException expected) {
            assertSame(main, window.getCurrentPage());
        }
        assertSame(
            main,
            window.getSideTabsWidget()
                .getSelectedTab());
    }

    private static IFancyUIProvider page(String title, UIElement content) {
        return new IFancyUIProvider() {

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public UIElement createMainPage(FancyMachineUIWidget window) {
                return content;
            }
        };
    }
}
