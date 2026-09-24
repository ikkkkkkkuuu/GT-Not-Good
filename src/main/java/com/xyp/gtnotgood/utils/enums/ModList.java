package com.xyp.gtnotgood.utils.enums;

import java.util.Locale;

import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.Loader;

/**
 * Central enum for mod IDs, display names, resource domains, and loaded checks.
 */
public enum ModList {

    GTNotGood(ModIds.GT_NOT_GOOD, Names.GT_NOT_GOOD),
    Angelica(ModIds.ANGELICA, Names.ANGELICA),
    NotEnoughItems(ModIds.NOT_ENOUGH_ITEMS, Names.NOT_ENOUGH_ITEMS),
    AE2(ModIds.APPLIED_ENERGISTICS_2, Names.APPLIED_ENERGISTICS_2),
    CropsNH(ModIds.CROPSNH, Names.CROPSNH),
    SpiceOfLife(ModIds.SPICE_OF_LIFE, Names.SPICE_OF_LIFE),
    Forestry(ModIds.FORESTRY, Names.FORESTRY),
    GregTech(ModIds.GREGTECH, Names.GREGTECH),
    Thaumcraft(ModIds.THAUMCRAFT, Names.THAUMCRAFT),
    Baubles(ModIds.BAUBLES, Names.BAUBLES),
    BloodMagic(ModIds.BLOOD_MAGIC, Names.BLOOD_MAGIC),
    ThaumicEnergistics(ModIds.THAUMIC_ENERGISTICS, Names.THAUMIC_ENERGISTICS),
    ENDER_IO(ModIds.ENDER_IO, Names.ENDER_IO),
    BetterQuestingAPI(ModIds.BETTER_QUESTING_API, Names.BETTER_QUESTING_API),
    WarpTheory(ModIds.WARP_THEORY, Names.WARP_THEORY);

    /**
     * String constants for mod IDs used by annotations, resources, and dependency checks.
     */
    public static class ModIds {

        public static final String GT_NOT_GOOD = "gtnotgood";
        /** Preserved resource namespace of the embedded official RTS Building module. */
        public static final String RTS_BUILDING = "rtsbuilding";
        public static final String ANGELICA = "angelica";
        public static final String NOT_ENOUGH_ITEMS = "NotEnoughItems";
        public static final String BETTER_QUESTING_API = "bqapi";
        public static final String APPLIED_ENERGISTICS_2 = "appliedenergistics2";
        public static final String CROPSNH = "cropsnh";
        public static final String SPICE_OF_LIFE = "SpiceOfLife";
        public static final String FORESTRY = "Forestry";
        public static final String GREGTECH = "gregtech";
        public static final String THAUMCRAFT = "Thaumcraft";
        public static final String BAUBLES = "Baubles";
        public static final String BLOOD_MAGIC = "AWWayofTime";
        public static final String THAUMIC_ENERGISTICS = "thaumicenergistics";
        public static final String WARP_THEORY = "WarpTheory";
        public static final String ENDER_IO = "EnderIO";

        private ModIds() {}
    }

    /**
     * Human-readable mod names used by UI text and logs.
     */
    public static class Names {

        public static final String GT_NOT_GOOD = "GT-Not-Good";
        public static final String ANGELICA = "Angelica";
        public static final String NOT_ENOUGH_ITEMS = "Not Enough Items";
        public static final String BETTER_QUESTING_API = "Better Questing API";
        public static final String APPLIED_ENERGISTICS_2 = "Applied Energistics 2";
        public static final String CROPSNH = "CropsNH";
        public static final String SPICE_OF_LIFE = "Spice of Life";
        public static final String FORESTRY = "Forestry";
        public static final String GREGTECH = "GregTech";
        public static final String THAUMCRAFT = "Thaumcraft";
        public static final String BAUBLES = "Baubles";
        public static final String BLOOD_MAGIC = "Blood Magic";
        public static final String THAUMIC_ENERGISTICS = "Thaumic Energistics";
        public static final String WARP_THEORY = "WarpTheory";
        public static final String ENDER_IO = "Ender IO";

        private Names() {}
    }

    private final String ID;
    private final String resourceDomain;
    private final String displayName;
    private Boolean modLoaded;

    ModList(String ID, String displayName) {
        this.ID = ID;
        this.resourceDomain = ID.toLowerCase(Locale.ENGLISH);
        this.displayName = displayName;
    }

    public boolean isModLoaded() {
        if (this.modLoaded == null) {
            this.modLoaded = Loader.isModLoaded(ID);
        }
        return this.modLoaded;
    }

    public String getID() {
        return ID;
    }

    public String getResourceLocation() {
        return resourceDomain;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getResourcePath(String path) {
        return this.getResourceLocation(path)
            .toString();
    }

    public String getResourcePath(String... path) {
        return this.getResourceLocation(path)
            .toString();
    }

    public ResourceLocation getResourceLocation(String path) {
        return new ResourceLocation(this.resourceDomain, path);
    }

    public ResourceLocation getResourceLocation(String... path) {
        return new ResourceLocation(this.resourceDomain, String.join("/", path));
    }
}
