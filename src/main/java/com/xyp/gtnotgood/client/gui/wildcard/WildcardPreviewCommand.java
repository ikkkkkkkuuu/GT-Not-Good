package com.xyp.gtnotgood.client.gui.wildcard;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.client.ClientCommandHandler;

import com.xyp.gtnotgood.common.items.wildcard.WildcardPatternItem;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Opens the read-only GUI prototype for a held wildcard pattern.
 * Opening is deferred until the next client tick so GuiChat cannot immediately close it.
 */
public final class WildcardPreviewCommand extends CommandBase {

    private ItemStack pendingStack;

    public static void register() {
        WildcardPreviewCommand command = new WildcardPreviewCommand();
        ClientCommandHandler.instance.registerCommand(command);
        FMLCommonHandler.instance()
            .bus()
            .register(command);
    }

    @Override
    public String getCommandName() {
        return "gtng-wildcard-preview";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack held = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof WildcardPatternItem)) {
            // #tr gui.wildcardprototype.hold
            // # Hold a wildcard pattern first.
            // # zh_CN 请先手持通配样板符。
            sender.addChatMessage(new ChatComponentTranslation("gui.wildcardprototype.hold"));
            return;
        }
        pendingStack = held.copy();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingStack == null) return;
        ItemStack snapshot = pendingStack;
        pendingStack = null;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.theWorld != null) {
            mc.displayGuiScreen(new WildcardPreviewScreen(snapshot));
        }
    }
}
