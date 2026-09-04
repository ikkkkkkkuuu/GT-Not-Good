package com.xyp.gtnotgood.ae2thing.proxy;

import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtnotgood.ae2thing.api.AE2ThingAPI;
import com.xyp.gtnotgood.ae2thing.api.adapter.crafting.AECraftingTerminal;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.DualInterfaceTerminalHandler;
import com.xyp.gtnotgood.ae2thing.common.item.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtnotgood.ae2thing.loader.InvLoader;
import com.xyp.gtnotgood.ae2thing.loader.PatternTerminalMouseWheelLoader;
import com.xyp.gtnotgood.ae2thing.network.wrapper.AE2ThingNetworkWrapper;
import com.xyp.gtnotgood.ae2thing.util.ModAndClassUtil;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public AE2ThingNetworkWrapper netHandler = new AE2ThingNetworkWrapper("ae2thing_dit");

    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        ModAndClassUtil.init();
    }

    public void init(FMLInitializationEvent event) {
        AE2ThingAPI.instance()
            .terminal()
            .registerCraftingTerminal(new AECraftingTerminal());
        new PatternTerminalMouseWheelLoader().run();
        new InvLoader().run();
    }

    public void postInit(FMLPostInitializationEvent event) {
        AE2ThingAPI.instance()
            .terminal()
            .registerTerminalItem(ItemWirelessDualInterfaceTerminal.class, new DualInterfaceTerminalHandler());
    }

    public void onLoadComplete(FMLLoadCompleteEvent event) {

    }

}
