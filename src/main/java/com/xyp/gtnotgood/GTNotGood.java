package com.xyp.gtnotgood;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/**
 * Main Forge mod entry point that delegates lifecycle events to sided proxies.
 */
@Mod(
    modid = GTNotGood.MODID,
    version = Tags.VERSION,
    name = GTNotGood.NAME,
    dependencies = "after:AWWayofTime;" + "required-after:Avaritia;"
        + "after:BloodArsenal;"
        + "required-after:Botania;"
        + "required-after:cropsnh;"
        + "required-after:bartworks;"
        + "after:eternalsingularity;"
        + "after:etfuturum;"
        + "after:GalacticraftCore;"
        + "after:GalacticraftMars;"
        + "after:GalacticraftPlanets;"
        + "required-after:gtnhintergalactic;"
        + "required-after:gregtech;"
        + "required-after:GoodGenerator;"
        + "required-after:galacticgreg;"
        + "required-after:IC2;"
        + "required-after:modularui;"
        + "required-after:miscutils;"
        + "before:neicustomdiagram;"
        + "after:dreamcraft;"
        + "required-after:structurelib;"
        + "after:ThaumcraftResearchTweaks;"
        + "required-after:Thaumcraft;",
    acceptedMinecraftVersions = "1.7.10")
public class GTNotGood {

    @Mod.Instance(ModList.ModIds.GT_NOT_GOOD)
    public static GTNotGood instance;
    public static final String MODID = ModList.ModIds.GT_NOT_GOOD;
    public static final String NAME = ModList.Names.GT_NOT_GOOD;
    public static final String RESOURCE_ROOT_ID = MODID;
    public static final Logger LOG = LogManager.getLogger(MODID);
    public static SimpleNetworkWrapper channel;

    @SidedProxy(clientSide = "com.xyp.gtnotgood.ClientProxy", serverSide = "com.xyp.gtnotgood.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.complete(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
// #tr gui.example.key
// # English text
// # zh_CN Chinese text
