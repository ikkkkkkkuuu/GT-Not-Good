package com.xyp.gtnotgood.common.utils;

import java.lang.reflect.Field;
import java.util.Map;

import com.xyp.gtnotgood.GTNotGood;

import bwcrossmod.galacticgreg.VoidMinerUtility;

/**
 * Safe accessor for GalacticGreg void-miner drop tables.
 * <p>
 * GalacticGreg stores its generated drop maps in static fields. This shim reflects those fields once, caches failures,
 * and returns empty drop maps when the upstream field layout is unavailable so the multiblock fails softly instead of
 * crashing during machine checks or GUI sync.
 */
public final class VoidMinerUtilityShim {

    private static Map<String, VoidMinerUtility.DropMap> dropMapsByName;
    private static Map<String, VoidMinerUtility.DropMap> extraDropsByName;
    private static boolean initialized;
    private static boolean initFailureLogged;

    private VoidMinerUtilityShim() {}

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        dropMapsByName = readDropMapField("dropMapsByDimName");
        extraDropsByName = readDropMapField("extraDropsByDimName");
        if (dropMapsByName == null || extraDropsByName == null) {
            warnInitFailureOnce();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, VoidMinerUtility.DropMap> readDropMapField(String fieldName) {
        try {
            Field field = VoidMinerUtility.class.getDeclaredField(fieldName);
            return (Map<String, VoidMinerUtility.DropMap>) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private static synchronized void warnInitFailureOnce() {
        if (initFailureLogged) return;
        initFailureLogged = true;
        GTNotGood.LOG.warn(
            "Failed to read GalacticGreg VoidMinerUtility drop tables; void miner dimensions may produce no ores.");
    }

    /**
     * Converts stable GTNH dimension IDs to GalacticGreg drop-map names.
     *
     * @param dimId world provider dimension id
     * @return GalacticGreg dimension name, or null when this dimension must be selected with a dimension-display item
     */
    public static String dimIdToName(int dimId) {
        switch (dimId) {
            case 0:
                return "Overworld";
            case -1:
                return "Nether";
            case 1:
                return "The End";
            case 7:
                return "Twilight Forest";
            case -7:
                return "Underdark";
            default:
                return null;
        }
    }

    public static VoidMinerUtility.DropMap getDropMap(String dimName) {
        init();
        if (dropMapsByName != null && dimName != null) {
            return dropMapsByName.getOrDefault(dimName, new VoidMinerUtility.DropMap());
        }
        return new VoidMinerUtility.DropMap();
    }

    public static VoidMinerUtility.DropMap getExtraDropMap(String dimName) {
        init();
        if (extraDropsByName != null && dimName != null) {
            return extraDropsByName.getOrDefault(dimName, new VoidMinerUtility.DropMap());
        }
        return new VoidMinerUtility.DropMap();
    }
}
