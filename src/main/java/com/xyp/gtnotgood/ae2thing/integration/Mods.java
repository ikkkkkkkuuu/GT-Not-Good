package com.xyp.gtnotgood.ae2thing.integration;

import java.util.Locale;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.data.IMod;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.core.localization.ButtonToolTips;
import cpw.mods.fml.common.Loader;

public enum Mods implements IMod, ITargetMod {

    AE2("appliedenergistics2"),
    AE2_FLUID_CRAFT("ae2fc"),
    AE2_STUFF("ae2stuff"),
    ANGELICA("angelica"),
    ADVENTURE_BACKPACK("adventurebackpack"),
    ASPECT_RECIPE_INDEX("aspectrecipeindex"),
    BACKPACK("Backpack"),
    BAUBLES("Baubles", () -> Loader.isModLoaded("Baubles") || Loader.isModLoaded("Baubles|Expanded"), null),
    BETTER_P2P("betterp2p"),
    BLOCK_RENDERER("blockrenderer6343"),
    BOTANIA("Botania"),
    CORE_MOD("dreamcraft"),
    FIND_IT("findit"),
    FORESTRY("Forestry"),
    GREGTECH("gregtech"),
    HBM_AE_ADDON("hbmaeaddon"),
    HODGEPODGE("hodgepodge"),
    IC2("IC2"),
    NECHAR("nechar"),
    NECH("nech"),
    NOT_ENOUGH_ENERGISTICS("neenergistics"),
    NOT_ENOUGH_ITEMS("NotEnoughItems"),
    OK_BACKPACK("okbackpack"),
    PROGRAMMABLE_HATCHES("programmablehatches"),
    THAUMCRAFT("Thaumcraft"),
    THAUMIC_ENERGISTICS("thaumicenergistics"),
    TINKERS_CONSTRUCT("TConstruct"),
    WAILA("Waila"),
    WIRELESS_CRAFTING_TERMINAL("ae2wct");

    private final String modid;
    private final String resourceDomain;
    private final Supplier<Boolean> supplier;
    private final TargetModBuilder targetBuilder;
    private Boolean loaded;

    Mods(String modid) {
        this(modid, null, null);
    }

    Mods(String modid, Supplier<Boolean> supplier, String coreModClass) {
        this.modid = modid;
        this.resourceDomain = modid.toLowerCase(Locale.ENGLISH);
        this.supplier = supplier;
        this.targetBuilder = new TargetModBuilder().setModId(modid)
            .setCoreModClass(coreModClass);
    }

    @NotNull
    @Override
    public TargetModBuilder getBuilder() {
        return targetBuilder;
    }

    @Override
    public boolean isModLoaded() {
        if (loaded == null) {
            loaded = supplier != null ? supplier.get() : Loader.isModLoaded(modid);
        }
        return loaded;
    }

    @Override
    public String getID() {
        return modid;
    }

    @Override
    public String getResourceLocation() {
        return resourceDomain;
    }

    public static boolean isGt5Loaded() {
        return GREGTECH.isModLoaded() && !Loader.isModLoaded("gregapi");
    }

    public static boolean isGt5UnofficialLoaded() {
        if (!isGt5Loaded()) {
            return false;
        }

        try {
            Class.forName("gregtech.api.recipe.RecipeMap");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static boolean isLegacyGt5Loaded() {
        return isGt5Loaded() && !isGt5UnofficialLoaded();
    }

    // Capability checks below probe for classes/fields that don't change at runtime; memoize so callers on GUI
    // draw/update paths don't repeat Class.forName / getDeclaredField every invocation.
    private static Boolean hasAe2TypeFilter;
    private static Boolean hasCraftingStatusSetting;
    private static Boolean hasDoubleButton;
    private static Boolean hasBeSubstitutionsButton;

    public static boolean hasAe2TypeFilter() {
        if (hasAe2TypeFilter == null) {
            try {
                Class.forName("appeng.core.features.registries.ItemDisplayRegistry");
                hasAe2TypeFilter = true;
            } catch (ClassNotFoundException ignored) {
                hasAe2TypeFilter = false;
            }
        }
        return hasAe2TypeFilter;
    }

    public static boolean hasCraftingStatusSetting() {
        if (hasCraftingStatusSetting == null) {
            try {
                Settings.class.getDeclaredField("CRAFTING_STATUS");
                hasCraftingStatusSetting = true;
            } catch (NoSuchFieldException ignored) {
                hasCraftingStatusSetting = false;
            }
        }
        return hasCraftingStatusSetting;
    }

    public static boolean hasDoubleButton() {
        if (hasDoubleButton == null) {
            try {
                ActionItems.class.getDeclaredField("DOUBLE");
                hasDoubleButton = true;
            } catch (NoSuchFieldException ignored) {
                hasDoubleButton = false;
            }
        }
        return hasDoubleButton;
    }

    public static boolean hasBeSubstitutionsButton() {
        if (hasBeSubstitutionsButton == null) {
            try {
                ButtonToolTips.class.getDeclaredField("BeSubstitutionsDescEnabled");
                hasBeSubstitutionsButton = true;
            } catch (NoSuchFieldException ignored) {
                hasBeSubstitutionsButton = false;
            }
        }
        return hasBeSubstitutionsButton;
    }
}
