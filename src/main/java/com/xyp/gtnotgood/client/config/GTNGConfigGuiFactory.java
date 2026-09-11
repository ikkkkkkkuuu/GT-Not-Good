package com.xyp.gtnotgood.client.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.config.MainConfig;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.client.GuiIngameModOptions;
import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.DummyConfigElement.DummyCategoryElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Provides the client-only Mods menu configuration entry and persists accepted edits.
 * Runtime fields remain unchanged until restart because some options control mixins and registration.
 */
public class GTNGConfigGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ConfigTooltipHandler());
    }

    /** Adds a working entry to Forge 1.7.10's otherwise unfinished in-world Mod Options screen. */
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onModOptionsInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiIngameModOptions) {
            event.buttonList.add(new SettingsButton(event.gui.width / 2 - 100, event.gui.height / 6 + 140));
        }
    }

    /** Opens the same configuration root from the pause menu without disconnecting from the server. */
    @SubscribeEvent
    public void onModOptionsClick(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.gui instanceof GuiIngameModOptions && event.button instanceof SettingsButton) {
            event.setCanceled(true);
            Minecraft.getMinecraft()
                .displayGuiScreen(new SettingsScreen(event.gui));
        }
    }

    /** Identifies our menu button without relying on another mod's button IDs. */
    private static final class SettingsButton extends GuiButton {

        private SettingsButton(int x, int y) {
            super(27140, x, y, ModList.GTNotGood.getDisplayName());
        }
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return SettingsScreen.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    /**
     * Saves the existing Configuration objects after Forge applies the Done button's edits.
     * Does not reload files or mutate live gameplay fields, including an integrated server's fields.
     *
     * @param event configuration change accepted by the Forge GUI
     */
    @SubscribeEvent
    public void onConfigChanged(OnConfigChangedEvent event) {
        if (ModList.GTNotGood.getID()
            .equals(event.modID)) {
            Config.getConfiguration()
                .save();
            MainConfig.getConfiguration()
                .save();
        }
    }

    /** Displays both Forge configuration files using their existing categories, comments and value bounds. */
    public static class SettingsScreen extends GuiConfig {

        public SettingsScreen(GuiScreen parentScreen) {
            super(
                parentScreen,
                createElements(),
                ModList.GTNotGood.getID(),
                false,
                true,
                ModList.GTNotGood.getDisplayName());
        }

        @SuppressWarnings("rawtypes")
        private static List<IConfigElement> createElements() {
            List<IConfigElement> elements = new ArrayList<>();
            if (Minecraft.getMinecraft().theWorld != null) {
                // #tr gui.gtnotgood.server_settings
                // # Server settings (administrator)
                // # zh_CN 服务器设置（管理员）
                elements.add(
                    new DummyCategoryElement(
                        "server",
                        "gui.gtnotgood.server_settings",
                        ServerSettingsScreen.ServerEntry.class));
            }
            addCategories(elements, Config.getConfiguration());
            addCategories(elements, MainConfig.getConfiguration());
            return elements;
        }

        /**
         * Adds root categories only; Forge builds nested category screens itself.
         *
         * @param elements      destination for GUI categories
         * @param configuration source configuration whose properties are edited in place
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static void addCategories(List<IConfigElement> elements, Configuration configuration) {
            for (String name : configuration.getCategoryNames()) {
                ConfigCategory category = configuration.getCategory(name);
                if (!category.isChild()) {
                    elements.add(new ConfigElement(category));
                }
            }
        }
    }
}
