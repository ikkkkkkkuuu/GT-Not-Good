package com.xyp.gtnotgood.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;

import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Adds functional descriptions over value controls while preserving Forge's label, undo and default tooltips. */
public final class ConfigTooltipHandler {

    private GuiConfig hoveredScreen;
    private IConfigElement<?> hoveredElement;
    private long hoverStarted;

    /**
     * Uses the scrolling list's hit test so clipped and scrolled-out rows cannot display a tooltip.
     * Only this mod's value controls are extended; Forge continues to handle the other hover regions.
     *
     * @param event completed screen draw with current pointer coordinates
     */
    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiConfig)) {
            reset();
            return;
        }
        GuiConfig screen = (GuiConfig) event.gui;
        GuiConfigEntries entries = screen.entryList;
        if (!ModList.GTNotGood.getID()
            .equals(screen.modID) || entries == null
            || event.mouseY <= entries.top
            || event.mouseY >= entries.bottom
            || event.mouseX < entries.controlX
            || event.mouseX >= entries.controlX + entries.controlWidth) {
            reset();
            return;
        }
        int index = entries.func_148124_c(event.mouseX, event.mouseY);
        if (index < 0 || index >= entries.getSize()) {
            reset();
            return;
        }
        IConfigElement<?> element = entries.getListEntry(index)
            .getConfigElement();
        if (!element.isProperty()) {
            reset();
            return;
        }
        if (screen != hoveredScreen || element != hoveredElement) {
            hoveredScreen = screen;
            hoveredElement = element;
            hoverStarted = Minecraft.getSystemTime();
        }
        if (Minecraft.getSystemTime() - hoverStarted < 500) return;

        String description = element.getComment();
        if (description == null || description.trim()
            .isEmpty()) return;
        String name = I18n.format(element.getLanguageKey());
        if (name.equals(element.getLanguageKey())) name = element.getName();
        String tooltip = EnumChatFormatting.GREEN + name + "\n" + EnumChatFormatting.YELLOW + description;
        screen.drawToolTip(
            Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(tooltip, Math.min(300, screen.width - 20)),
            event.mouseX,
            event.mouseY);
    }

    private void reset() {
        hoveredScreen = null;
        hoveredElement = null;
    }
}
