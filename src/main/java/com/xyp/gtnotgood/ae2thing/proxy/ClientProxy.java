package com.xyp.gtnotgood.ae2thing.proxy;

import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.ae2thing.api.AE2ThingAPI;
import com.xyp.gtnotgood.ae2thing.api.MouseWheelHandler;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.DualInterfaceTerminal;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.FCBaseItemTerminal;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.FCUltraTerminal;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.parts.AETerminal;
import com.xyp.gtnotgood.ae2thing.client.event.CraftTracking;
import com.xyp.gtnotgood.ae2thing.client.event.GuiOverlayButtonEvent;
import com.xyp.gtnotgood.ae2thing.client.event.UpdateAmountTextEvent;
import com.xyp.gtnotgood.ae2thing.integration.Mods;
import com.xyp.gtnotgood.ae2thing.loader.KeybindLoader;
import com.xyp.gtnotgood.ae2thing.nei.recipes.DefaultExtractorLoader;
import com.xyp.gtnotgood.ae2thing.network.CPacketCraftRequest;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;

import appeng.api.events.GuiScrollEvent;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.implementations.GuiPatternTermEx;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class ClientProxy extends CommonProxy {

    private static GuiOverlayButton overlayButton = null;
    public static List<MouseWheelHandler> mouseHandlers = new ArrayList<>();

    @Override
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        super.onLoadComplete(event);
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded()) {
            new DefaultExtractorLoader().run();
        }
    }

    public static GuiOverlayButton getOverlayButton() {
        return overlayButton;
    }

    @SubscribeEvent
    public void trackingMissingItems(CraftTracking c) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        IItemList<IAEItemStack> list = c.getItems();
        if (!list.isEmpty() && AE2ThingAPI.instance()
            .terminal()
            .isCraftingTerminal(screen)) {
            for (IAEItemStack is : list) {
                AE2Thing.proxy.netHandler.sendToServer(new CPacketCraftRequest(is, isShiftKeyDown()));
                is.reset();
                break;
            }
        }
    }

    @SubscribeEvent
    public void updateCraftAmount(UpdateAmountTextEvent amount) {
        amount.updateAmount();
    }

    @SubscribeEvent
    public boolean handleMouseWheelInput(GuiScrollEvent event) {
        if (mouseHandlers.isEmpty()) return false;
        for (MouseWheelHandler handler : mouseHandlers) {
            if (handler.handleMouseWheel(event, overlayButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        (new KeybindLoader()).run();
        AE2ThingAPI.instance()
            .terminal()
            .registerTerminal(GuiMEMonitorable.class);
        AE2ThingAPI.instance()
            .terminal()
            .registerTerminal(GuiCraftingTerm.class);
        AE2ThingAPI.instance()
            .terminal()
            .registerTerminal(GuiPatternTerm.class);
        AE2ThingAPI.instance()
            .terminal()
            .registerTerminal(GuiPatternTermEx.class);

        AE2ThingAPI.instance()
            .terminal()
            .registerTerminalBlackList(GuiQuickEncodingTerminal.class);
        AE2ThingAPI.instance()
            .terminal()
            .registerTerminalSet(DualInterfaceTerminal.instance);
        AE2ThingAPI.instance()
            .terminal()
            .registerTerminalSet(FCBaseItemTerminal.instance);
        AE2ThingAPI.instance()
            .terminal()
            .registerTerminalSet(FCUltraTerminal.instance);
        AE2ThingAPI.instance()
            .terminal()
            .registerTerminalSet(new AETerminal());
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event) {
        AE2ThingAPI.instance()
            .getPinned()
            .updateCraftingItems();
    }

    @SubscribeEvent
    public void onActionPerformedEventPost(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
        if (!(event.gui instanceof GuiRecipe<?>)) return;
        overlayButton = null;
        for (GuiRecipeButton btn : event.buttonList) {
            if (btn instanceof GuiOverlayButton gob) {
                gob.setRequireShiftForOverlayRecipe(false);
            }
        }
    }

    @SubscribeEvent
    public void onActionOverlayButton(GuiOverlayButtonEvent event) {
        overlayButton = event.getButton();
    }

    @SubscribeEvent
    public void initGuiEvent(GuiScreenEvent.InitGuiEvent.Post event) {
        if (AE2ThingAPI.instance()
            .terminal()
            .isCraftingTerminal(event.gui)) {
            MinecraftForge.EVENT_BUS.post(new CraftTracking());
        }
        if (UpdateAmountTextEvent.needUpdateAmountText()) {
            MinecraftForge.EVENT_BUS.post(new UpdateAmountTextEvent());
        }
    }

    @SubscribeEvent
    public void initGuiEvent(GuiScreenEvent.InitGuiEvent.Pre event) {
        if (AE2ThingAPI.instance()
            .terminal()
            .isPinTerminal(event.gui)) {
            AE2ThingAPI.instance()
                .getPinned()
                .prune();
        }
    }

    @SubscribeEvent
    public void ClientDisconnectionFromServerEvent(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        AE2ThingAPI.instance()
            .getPinned()
            .clear();
    }
}
