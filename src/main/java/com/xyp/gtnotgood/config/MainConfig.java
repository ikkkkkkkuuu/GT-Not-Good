package com.xyp.gtnotgood.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class MainConfig {

    private static boolean configLoaded = false;
    private static Configuration configuration;

    private static final File DEFAULT_CONFIG_FILE = new File(Config.getConfigDirectory(), "main.cfg");

    public static boolean someOption = true;

    public static synchronized void ensureLoaded() {
        if (!configLoaded) {
            loadConfiguration();
        }
    }

    private static void loadConfiguration() {

        configuration = new Configuration(DEFAULT_CONFIG_FILE);

        someOption = configuration.getBoolean("someOption", Configuration.CATEGORY_GENERAL, someOption, "示例配置");

        if (configuration.hasChanged()) {
            configuration.save();
        }

        configLoaded = true;
    }

    public static synchronized Configuration getConfiguration() {
        ensureLoaded();
        return configuration;
    }

    public static boolean isSomeOption() {
        ensureLoaded();
        return someOption;
    }
}
