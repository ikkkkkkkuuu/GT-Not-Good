package com.xyp.gtnotgood.common.machines.hatch.me;

import java.lang.reflect.Field;

import com.xyp.gtnotgood.GTNotGood;

import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;

/**
 * Forces GregTech ME output providers to use the largest possible built-in cache capacity.
 * <p>
 * The original ME output bus and hatch keep their cache size in a private provider instance whose capacity fields are
 * protected inside GT5. Subclasses cannot configure those values through the public constructors, so this helper
 * applies
 * the project-specific maximum by reflection while leaving the original AE2 network, filter, cache-mode, and GUI logic
 * untouched.
 */
final class MaxCapacityMEOutputCapacity {

    static final long MAX_CACHE_CAPACITY = Long.MAX_VALUE;

    private static final Field BASE_CAPACITY_FIELD = getCapacityField("baseCapacity");
    private static final Field CACHE_CAPACITY_FIELD = getCapacityField("cacheCapacity");

    private MaxCapacityMEOutputCapacity() {}

    /**
     * Writes the maximum capacity to both provider capacity fields.
     * <p>
     * GT5 recalculates cache capacity when cells are inserted, NBT is loaded, cache mode changes, or description
     * packets
     * are received. Calling this after those events restores the intended behavior: storage cells may still be used for
     * filtering, but they no longer limit this mod's ME output cache.
     *
     * @param provider original GT5 ME output provider owned by the hatch or bus
     */
    static void forceMaxCapacity(MTEHatchOutputMEBase<?> provider) {
        if (provider == null) return;
        try {
            BASE_CAPACITY_FIELD.setLong(provider, MAX_CACHE_CAPACITY);
            CACHE_CAPACITY_FIELD.setLong(provider, MAX_CACHE_CAPACITY);
        } catch (IllegalAccessException e) {
            GTNotGood.LOG.error("Failed to force max capacity on ME output provider", e);
        }
    }

    /**
     * Resolves one protected capacity field from GT5's provider implementation.
     *
     * @param name field name inside {@link MTEHatchOutputMEBase}
     * @return accessible field handle used by {@link #forceMaxCapacity(MTEHatchOutputMEBase)}
     */
    private static Field getCapacityField(String name) {
        try {
            Field field = MTEHatchOutputMEBase.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Missing GT5 ME output capacity field: " + name, e);
        }
    }
}
