package com.xyp.gtnotgood.common.items.compass;

import java.util.List;
import java.util.Random;

import net.minecraft.world.World;

import greymerk.roguelike.config.RogueConfig;
import greymerk.roguelike.dungeon.Dungeon;
import greymerk.roguelike.worldgen.WorldEditor;

/**
 * Optional Roguelike integration: invokes its installed spawn rule without generating terrain or altering world RNG.
 */
final class RoguelikeCandidates extends WorldEditor {

    private final long seed;

    RoguelikeCandidates(World world) {
        super(world);
        seed = world.getSeed();
    }

    static boolean enabled(World world) {
        List<Integer> allowed = RogueConfig.getIntList(RogueConfig.DIMENSIONWL);
        return RogueConfig.getBoolean(RogueConfig.DONATURALSPAWN)
            && !RogueConfig.getIntList(RogueConfig.DIMENSIONBL).contains(world.provider.dimensionId)
            && (allowed.isEmpty() || allowed.contains(world.provider.dimensionId));
    }

    boolean test(int x, int z) {
        return Dungeon.canSpawnInChunk(x, z, this);
    }

    /** Uses Minecraft's region seed convention with a private RNG, leaving the live world's RNG untouched. */
    @Override
    public Random getSeededRandom(int x, int z, int salt) {
        return new Random((long) x * 341873128712L + (long) z * 132897987541L + seed + salt);
    }
}
