package com.xyp.gtnotgood.qa;

import java.nio.file.Files;
import java.nio.file.Paths;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client bootstrap only; all gameplay assertions execute on the real integrated server thread. */
public final class TerminalCraftingClientChecks {

    private boolean started;

    public TerminalCraftingClientChecks() {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getMinecraft();
        client.gameSettings.pauseOnLostFocus = false;
        if (!started && client.currentScreen instanceof GuiMainMenu) {
            started = true;
            client.launchIntegratedServer(
                "terminal-crafting-qa-" + System.currentTimeMillis(),
                "Terminal Crafting QA",
                new WorldSettings(81973L, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT));
        }
        if (started && Files.exists(
            Paths.get(System.getProperty("gtng.terminalCrafting.report"))
                .resolveSibling("result.txt")))
            client.shutdown();
    }
}
