package com.xyp.ldlib.demo;

import java.util.function.Function;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.xyp.ldlib.gui.holder.ModularUIScreen;
import com.xyp.ldlib.gui.texture.GuiTextureGroup;
import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.texture.ItemStackTexture;
import com.xyp.ldlib.gui.texture.TextTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.elements.Flow;
import com.xyp.ldlib.gui.ui.elements.Label;
import com.xyp.ldlib.gui.ui.elements.ScrollerView;
import com.xyp.ldlib.gui.ui.elements.Toggle;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/**
 * Standalone library showcase. Exercises focused text input, validation, tab replacement,
 * scroll clipping and original upstream texture regions without any machine/item data model.
 */
public final class LibraryDemoScreen extends ModularUIScreen {

    private final ModernTheme theme;
    private UIElement pages;
    private String text = "";
    private boolean toggled;
    private int clicks;

    public LibraryDemoScreen(Function<String, ResourceLocation> resources) {
        super(new UIElement(0, 0, 310, 226));
        theme = new ModernTheme(resources);
        rebuild();
    }

    private void rebuild() {
        root.clearAllChildren();
        // #tr gui.ldlibdemo.controls
        // # Controls
        // # zh_CN 基础控件
        String controls = StatCollector.translateToLocal("gui.ldlibdemo.controls");
        // #tr gui.ldlibdemo.scroll
        // # Scrolling
        // # zh_CN 滚动列表
        String scroll = StatCollector.translateToLocal("gui.ldlibdemo.scroll");
        // #tr gui.ldlibdemo.textures
        // # Textures
        // # zh_CN 主题材质
        String textures = StatCollector.translateToLocal("gui.ldlibdemo.textures");
        com.xyp.ldlib.gui.fancy.IFancyUIProvider main = new com.xyp.ldlib.gui.fancy.IFancyUIProvider() {

            @Override
            public String getTitle() {
                return controls;
            }

            @Override
            public UIElement createMainPage(com.xyp.ldlib.gui.fancy.FancyMachineUIWidget window) {
                return createPage(0);
            }

            @Override
            public void attachSideTabs(com.xyp.ldlib.gui.fancy.TabsWidget tabs) {
                tabs.attachSubTab(provider(scroll, 1));
                tabs.attachSubTab(provider(textures, 2));
            }
        };
        root.addChild(new com.xyp.ldlib.gui.fancy.FancyMachineUIWidget(main, 310, 226, theme));
        // #tr gui.ldlibdemo.help
        // # Tab: focus / Wheel: scroll / Esc: close
        // # zh_CN Tab 切换焦点 / 滚轮滚动 / Esc 关闭
        root.addChild(new Label(77, 201, 225, 18, StatCollector.translateToLocal("gui.ldlibdemo.help")));
    }

    private com.xyp.ldlib.gui.fancy.IFancyUIProvider provider(String title, int index) {
        return new com.xyp.ldlib.gui.fancy.IFancyUIProvider() {

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public UIElement createMainPage(com.xyp.ldlib.gui.fancy.FancyMachineUIWidget window) {
                return createPage(index);
            }
        };
    }

    private UIElement createPage(int index) {
        pages = new UIElement(0, 0, 225, 161);
        if (index == 0) buildControls();
        else if (index == 1) buildScroll();
        else buildTextures();
        return pages;
    }

    private void buildControls() {
        // #tr gui.ldlibdemo.edit
        // # Text / selection / clipboard
        // # zh_CN 文字输入 / 选中 / 复制粘贴
        pages.addChild(new Label(0, 0, 225, 18, StatCollector.translateToLocal("gui.ldlibdemo.edit")));
        pages.addChild(
            theme.textField(6, 23, 213, 20)
                .setText(text)
                .setOnChange(value -> text = value));
        pages.addChild(new Label(6, 47, 213, 18, () -> text));
        // #tr gui.ldlibdemo.switch
        // # Toggle
        // # zh_CN 开关
        String toggle = StatCollector.translateToLocal("gui.ldlibdemo.switch");
        pages.addChild(
            new Toggle(
                6,
                72,
                95,
                22,
                theme.text(theme.button, () -> toggle + " -"),
                theme.text(theme.accent, () -> toggle + " +")).setValue(toggled)
                    .setOnChange(value -> toggled = value));
        // #tr gui.ldlibdemo.clicks
        // # Clicks
        // # zh_CN 点击次数
        String count = StatCollector.translateToLocal("gui.ldlibdemo.clicks");
        pages.addChild(theme.button(109, 72, 110, 22, () -> count + ": " + clicks, () -> clicks++));
        // #tr gui.ldlibdemo.digits
        // # Digits only (up to 6)
        // # zh_CN 数字校验（最多6位）
        pages.addChild(new Label(6, 102, 213, 18, StatCollector.translateToLocal("gui.ldlibdemo.digits")));
        pages.addChild(
            theme.textField(6, 124, 96, 20)
                .setMaxLength(6)
                .setValidator(value -> value.matches("[0-9]*")));
        pages.addChild(new UIElement(112, 120, 28, 28).setBackground(theme.slot));
        pages.addChild(
            new UIElement(114, 122, 24, 24).setBackground(new ItemStackTexture(new ItemStack(Items.diamond))));
    }

    private void buildScroll() {
        ScrollerView scroll = theme.scroller(0, 0, 225, 161);
        Flow rows = new Flow(0, 0, 216, 40 * 27, true).setGap(3);
        // #tr gui.ldlibdemo.row
        // # Component
        // # zh_CN 控件
        String row = StatCollector.translateToLocal("gui.ldlibdemo.row");
        for (int i = 0; i < 40; i++) {
            final int number = i + 1;
            rows.addChild(theme.button(0, 0, 213, 24, () -> row + " " + number, () -> clicks++));
        }
        scroll.addChild(rows);
        pages.addChild(scroll);
    }

    private void buildTextures() {
        IGuiTexture[] samples = { theme.panel, theme.button, theme.accent, theme.disabled, theme.input,
            theme.inputFocused, theme.slot };
        for (int i = 0; i < samples.length; i++) {
            pages.addChild(new UIElement(6, 4 + i * 22, 92, 19).setBackground(samples[i]));
            pages.addChild(
                new UIElement(113, 4 + i * 22, 105, 19)
                    .setBackground(new GuiTextureGroup(samples[i], new TextTexture(() -> "Aa 123", 0xFF202830))));
        }
    }
}
