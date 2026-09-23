package com.xyp.gtnotgood.commandtree;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.commandtree.command.serialization.ArgumentTypes;
import com.xyp.gtnotgood.commandtree.compat.CompatRegistry;
import com.xyp.gtnotgood.commandtree.handler.PlayerJoinHandler;

import cpw.mods.fml.common.FMLCommonHandler;

/** Initializes the integrated command tree and its server-side join listener. */
public final class CommandTreeBootstrap {

    private CommandTreeBootstrap() {}

    /** Registers argument serializers and bundled command definitions before players can join. */
    public static void preInit() {
        ArgumentTypes.init();
        CompatRegistry.init();
        GTNotGood.LOG.info("Command tree initialized");
    }

    /** Registers the login handler after the regular mod initialization phase. */
    public static void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(new PlayerJoinHandler());
    }
}
