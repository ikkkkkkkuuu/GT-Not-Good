package com.xyp.gtnotgood.common.items.compass;

import java.lang.reflect.Method;

import net.minecraft.world.World;

import eu.usrv.legacylootgames.worldgen.LootGamesWorldGen;
import ru.timeconqueror.lootgames.common.config.LGConfigs;

/** Calls the installed LootGames seed rule instead of duplicating its generation algorithm. */
final class LootGamesCandidates {

    private final LootGamesWorldGen generator = new LootGamesWorldGen();
    private final Method candidate;

    LootGamesCandidates() throws ReflectiveOperationException {
        candidate = LootGamesWorldGen.class.getDeclaredMethod("canSpawnInChunk_v3", int.class, int.class, World.class);
        candidate.setAccessible(true);
    }

    static boolean enabled(World world) {
        return !LGConfigs.GENERAL.worldGen.disableDungeonGen
            && LGConfigs.GENERAL.worldGen.isDimensionEnabledForWG(world.provider.dimensionId);
    }

    boolean test(World world, int x, int z) throws ReflectiveOperationException {
        return (Boolean) candidate.invoke(generator, x, z, world);
    }
}
