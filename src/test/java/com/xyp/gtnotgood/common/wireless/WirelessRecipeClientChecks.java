package com.xyp.gtnotgood.common.wireless;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import com.xyp.gtnotgood.loader.WirelessLaserLoader;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.AnimatedText;
import com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in startup check for voltage and amperage costs without creating a world or exercising unrelated GUIs. */
@Mod(
    modid = "wirelessrecipeqa",
    name = "Wireless Recipe QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class WirelessRecipeClientChecks {

    private boolean complete;

    @Mod.EventHandler
    public void loaded(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.wireless.recipe.qa")) return;
        System.out.println("WIRELESS_RECIPE_QA: registered startup check");
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (complete || event.phase != TickEvent.Phase.END || minecraft.currentScreen == null) return;
        complete = true;
        WirelessLaserClientChecks.checkCatalogAndRecipes();
        int credits = 0;
        for (int tier = 1; tier <= 14; tier++) {
            for (int slot = 0; slot < 8; slot++) {
                ItemStack stack = slot == 7 ? WirelessLaserLoader.dynamo(tier).get(1)
                    : WirelessLaserLoader.energy(tier, slot).get(1);
                List<Supplier<String>> lines = AnimatedTooltipHandler.tooltipMap.get(stack);
                if (lines == null || Collections.frequency(lines, AnimatedText.GT_NOT_GOOD) != 1) {
                    throw new AssertionError("Missing or duplicate wireless credit: " + tier + "/" + slot);
                }
                List<String> tooltip = new ArrayList<>();
                AnimatedTooltipHandler.renderTooltip(new ItemTooltipEvent(stack, null, tooltip, false));
                if (tooltip.size() != 1 || !tooltip.get(0).contains("GT-Not-Good")) {
                    throw new AssertionError("Wireless credit not rendered: " + tooltip);
                }
                credits++;
            }
        }
        System.out.println("WIRELESS_TOOLTIP_QA: " + credits + " credits rendered without duplicates");
        Files.write(new File("wireless-recipe-qa-result.txt").toPath(),
            "PASS: 112 catalog entries and 162 recipes".getBytes(StandardCharsets.UTF_8));
        System.out.println("WIRELESS_RECIPE_QA: PASS");
        minecraft.shutdown();
    }
}
