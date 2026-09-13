package com.xyp.gtnotgood.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;

import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.demo.LibraryDemoScreen;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client-only addon adapter exposing the independent library showcase without a held item. */
public final class LibraryDemoCommand extends CommandBase {

    private boolean pending;

    public static void register() {
        LibraryDemoCommand command = new LibraryDemoCommand();
        ClientCommandHandler.instance.registerCommand(command);
        FMLCommonHandler.instance()
            .bus()
            .register(command);
    }

    @Override
    public String getCommandName() {
        return "ldlib-demo";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ldlib-demo";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        pending = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pending) return;
        pending = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.theWorld != null) {
            mc.displayGuiScreen(
                new LibraryDemoScreen(path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path)));
        }
    }
}
