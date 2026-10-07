package com.xyp.gtnotgood.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.TextEffectsCompat;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

/**
 * Provides the late mixin config and conditionally lists ordinary late mixins.
 * <p>
 * Normal GT Not Good mixins should be added from {@link #getMixins(Set)} with loaded-mod checks. Only genuinely early
 * mixins should be listed directly in JSON, matching the project rule copied from GT-Not-Cool.
 */
@LateMixin
@SuppressWarnings("unused")
public class LateMixinsLoader implements ILateMixinLoader {

    /**
     * Appends non-empty mixin names to the target list.
     * <p>
     * This helper exists so optional blocks can add several related mixins in one call while ignoring accidental null
     * or
     * empty entries.
     *
     * @param list       mutable mixin-name list returned from {@link #getMixins(Set)}
     * @param mixinNames simple mixin class names to append
     */
    private static void addAll(List<String> list, String... mixinNames) {
        for (String name : mixinNames) {
            if (name != null && !name.isEmpty()) {
                list.add(name);
            }
        }
    }

    /**
     * Returns the late mixin JSON config owned by this mod.
     *
     * @return mixin configuration resource name
     */
    @Override
    public String getMixinConfig() {
        return "mixins.gtnotgood.late.json";
    }

    /**
     * Builds the list of late mixin classes that should load for the current mod set.
     * <p>
     * Future optional integrations should check {@code loadedMods} here before adding their mixin names. This prevents
     * classloading crashes when a target mod is absent from the development or pack environment.
     *
     * @param loadedMods set of loaded mod IDs supplied by GTNHMixins
     * @return simple mixin class names from the late mixin config
     */
    @Override
    @Nonnull
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> list = new ArrayList<>();
        if (loadedMods.contains(ModList.Railcraft.getID())) {
            addAll(list, "railcraft.MultiblockBoilerMixin");
        }
        if (loadedMods.contains(ModList.Roguelike.getID())) {
            addAll(list, "compass.RoguelikeWorldAccessor", "compass.RoguelikeHouseMixin");
        }
        if (loadedMods.contains(ModList.LootGames.getID())) {
            addAll(
                list,
                "compass.LootGamesStructureMixin",
                "lootgames.EasyLightConfigMixin",
                "lootgames.EasyMinesweeperConfigMixin",
                "lootgames.EasyMinesweeperStageMixin",
                "lootgames.EasySudokuConfigMixin",
                "lootgames.MaximumRewardsMixin");
        }
        if (loadedMods.contains(ModList.BloodMagic.getID())) {
            addAll(list, "bloodmagic.MixinPackagedBloodAltar");
        }
        if (loadedMods.contains(ModList.GregTech.getID())) {
            addAll(
                list,
                "gregtech.wireless.WirelessProcessingAccess",
                "gregtech.wireless.CrossRecipeProcessingMixin",
                "gregtech.wireless.CrossRecipeControllerMixin",
                "gregtech.wireless.CrossRecipeVoltageMixin");
            addAll(list, "gregtech.wireless.CrossRecipeGuiMixin");
            if (loadedMods.contains(ModList.GTNotLeisure.getID())) {
                addAll(
                    list,
                    "gregtech.wireless.GtnlCrossRecipeProcessingMixin",
                    "gregtech.wireless.GtnlCrossRecipeControllerMixin");
            }
            addAll(
                list,
                "gregtech.AssemblyLineDataAccessMixin",
                "gregtech.TransmutationShapedRecipeMixin",
                "gregtech.TransmutationShapelessRecipeMixin");
        }
        if (FMLLaunchHandler.side()
            .isClient() && loadedMods.contains(ModList.NotEnoughItems.getID())
            && !TextEffectsCompat.hasUpstreamRenderer()) {
            addAll(list, "texteffect.MixinNEIFormattedTextField");
        }

        if (loadedMods.contains(ModList.AE2.getID()) && loadedMods.contains(ModList.GregTech.getID())) {
            addAll(
                list,
                "gregtech.BasicMachineStockIOMixin",
                "gregtech.BasicGeneratorStockIOMixin",
                "appliedenergistics.PatternMEOutputMultiblockMixin",
                "appliedenergistics.CraftingPatternAlternativesMixin",
                "appliedenergistics.compact.MixinCraftingGridCache",
                "appliedenergistics.compact.MixinCraftingCPUCluster",
                "appliedenergistics.compact.MixinInventoryCrafting",
                "appliedenergistics.compact.AccessorTaskProgress",
                "appliedenergistics.compact.AccessorSessionCraftCount",
                "appliedenergistics.compact.CompactCraftingEnergyMixin",
                "appliedenergistics.compact.MatrixInterfaceTerminalVisibilityMixin",
                "appliedenergistics.compact.MatrixPatternPersistenceMixin",
                "appliedenergistics.AutomaticMachineCircuitMixin");
        }

        if (loadedMods.contains(ModList.AE2.getID())) {
            if (FMLLaunchHandler.side()
                .isClient()) {
                addAll(
                    list,
                    "appliedenergistics.InvTweaksOrderCacheMixin",
                    "appliedenergistics.ItemRepoSortNameCacheMixin",
                    "appliedenergistics.ItemSortersNameCacheMixin",
                    "appliedenergistics.InterfaceEntryViewportAccessor",
                    "appliedenergistics.InterfaceTerminalViewportMixin");
            }
            addAll(
                list,
                "appliedenergistics.DualityInterfaceMixin",
                "appliedenergistics.ItemEncodedPatternMixin",
                "appliedenergistics.MTEHatchCraftingInputMEMixin",
                "appliedenergistics.MTEHatchCraftingInputMENameMixin",
                "appliedenergistics.MTEHatchCraftingInputMEMultiBlockNameMixin",
                "appliedenergistics.PatternMultiplierHelperMixin",
                "appliedenergistics.MatrixWildcardPatternOptimizationMixin",
                "appliedenergistics.SuperMTEHatchCraftingInputMEMixin");
        }

        if (loadedMods.contains(ModList.GregTech.getID())) {
            addAll(
                list,
                "accessor.Grade4WaterPurificationAccessor",
                "treatedwater.Grade1WaterPurificationMixin",
                "treatedwater.Grade2WaterPurificationMixin",
                "treatedwater.Grade3WaterPurificationMixin",
                "treatedwater.Grade4WaterPurificationMixin",
                "treatedwater.Grade5WaterPurificationMixin",
                "treatedwater.Grade6WaterPurificationMixin",
                "treatedwater.Grade7WaterPurificationMixin",
                "treatedwater.Grade8WaterPurificationMixin",
                "gregtech.BlackHoleCompressorMixin",
                "fog.FOGShardsAvailable",
                "eoh.EyeOfHarmonySuccessRateControl",
                "eoh.EyeOfHarmonyGas",
                "accessor.EyeOfHarmonyAccessor",
                "gregtech.ModifySomeConfigs",
                "gregtech.CleanroomRequirementMixin",
                "gregtech.GTMetaTools",
                "gregtech.MixinMTEBasicMachineFacing",
                "gregtech.BasicMachineVirtualMoldMixin",
                "gregtech.BasicMachineMoldGuiMixin",
                "gregtech.MixinMTEBrickedBlastFurnace",
                "cutcorners.RecipeSpeedMixin",
                "cutcorners.ScannerSpeedMixin",
                "cutcorners.AssemblyLineSpeedMixin",
                "cutcorners.PurificationPlantSpeedMixin",
                "cutcorners.ExtremeEntityCrusherSpeedMixin",
                "cutcorners.ThermalBoilerSpeedMixin",
                "cutcorners.FurnaceBackendMixin",
                "cutcorners.BasicMachineOutputMixin");
        }
        if (loadedMods.contains(ModList.EnderIO.getID())) {
            addAll(
                list,
                "enderio.MixinNetworkedInventory",
                "enderio.MixinNetworkedInventory",
                "enderio.MixinItemSoulVessel",
                "enderio.MixinSoulVesselConfig");
        }

        if (loadedMods.contains(ModList.Forestry.getID())) {
            addAll(list, "forestry.MixinWorkingApiaryProducts");
            addAll(list, "forestry.MixinBee", "forestry.MixinMutationConditions", "forestry.MixinBeeHomozygous");
        }

        if (loadedMods.contains(ModList.GregTech.getID()) && loadedMods.contains(ModList.Forestry.getID())) {
            addAll(list, "gregtech.MixinGTBeeMutation");
        }

        if (loadedMods.contains(ModList.CropsNH.getID())) {
            addAll(list, "cropsnh.MixinTileEntityCropSticks", "cropsnh.MixinSeedStats");
        }

        if (loadedMods.contains(ModList.SpiceOfLife.getID())) {
            addAll(list, "spiceoflife.MixinFoodModifier");
        }

        if (loadedMods.contains(ModList.Thaumcraft.getID())) {
            if (loadedMods.contains(ModList.AE2.getID())) {
                addAll(
                    list,
                    "thaumcraft.MixinPackagedEssentiaHandler",
                    "thaumcraft.MixinPackagedInfusionSource",
                    "thaumcraft.MixinPackagedCrucible");
            }
            addAll(
                list,
                "thaumcraft.MixinWarpEvents",
                "thaumcraft.MixinResearchManager",
                "thaumcraft.MixinPlayerKnowledge",
                "thaumcraft.MixinTileInfusionMatrix",
                "thaumcraft.MixinVisNetHandler");
        }

        if (loadedMods.contains(ModList.WarpTheory.getID())) {
            addAll(list, "warptheory.MixinWarpEventHandler");
        }

        return list;
    }
}
