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
 * <p>
 * Note on the CropsNH ordering: the dependency string deliberately says {@code required-before:cropsnh} rather than
 * {@code required-after}. CropsNH and Et Futurum Requiem both bundle MCLib 0.3.7.7, and MCLib's shared-state election
 * keeps whichever copy registers first when the versions tie. CropsNH's relocated copy ships a minimized cglib that is
 * missing {@code net.sf.cglib.proxy.NoOp}, so when it wins the tie every MCLib consumer dies during preInit. Combined
 * with {@code after:etfuturum} this forces the transitive order etfuturum -> gtnotgood -> cropsnh, which makes Et
 * Futurum's intact copy register first. Loading before CropsNH is safe because nothing in our preInit touches it, and
 * the CropsNH items we need at init are registered during CropsNH's own preInit, which the FML phase barrier already
 * guarantees. Remove this workaround once CropsNH ships a complete shaded cglib.
 */
@Mod(
    modid = GTNotGood.MODID,
    version = Tags.VERSION,
    name = GTNotGood.NAME,
    guiFactory = "com.xyp.gtnotgood.client.config.GTNGConfigGuiFactory",
    dependencies = "after:AWWayofTime;" + "required-after:Avaritia;"
        + "after:BloodArsenal;"
        + "required-after:Botania;"
        + "required-before:cropsnh;"
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
