package com.xyp.gtnotgood.client.radialmenu.client.gui;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.IConfigElement;

public final class RadialSettingsEntry extends GuiConfigEntries.CategoryEntry {

    @SuppressWarnings("rawtypes")
    public RadialSettingsEntry(GuiConfig screen, GuiConfigEntries entries, IConfigElement element) {
        super(screen, entries, element);
    }

    @Override
    protected GuiScreen buildChildScreen() {
        try {
            return new SimpleGuiConfig(owningScreen, ModList.GTNotGood.getID(), ModList.GTNotGood.getDisplayName());
        } catch (ConfigException e) {
            RadialMenuMod.LOG.error("Cannot open radial menu settings", e);
            return owningScreen;
        }
    }
}
