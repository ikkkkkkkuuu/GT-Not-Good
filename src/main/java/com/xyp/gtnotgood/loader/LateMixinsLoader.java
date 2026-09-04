package com.xyp.gtnotgood.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.xyp.gtnotgood.utils.enums.ModList;

/**
 * Provides the late mixin config and conditionally lists ordinary late mixins.
 * <p>
 * Normal GT Not Good mixins should be added from {@link #getMixins(Set)} with loaded-mod checks. Only genuinely early
 * mixins should be listed directly in JSON, matching the project rule copied from GT-Not-Cool.
 */
@LateMixin
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
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> list = new ArrayList<>();

        if (loadedMods.contains(ModList.AE2.getID())) {
            addAll(
                list,
                "AppliedEnergistics.DualityInterfaceMixin",
                "AppliedEnergistics.ItemEncodedPatternMixin",
                "AppliedEnergistics.MTEHatchCraftingInputMEMixin",
                "AppliedEnergistics.MTEHatchCraftingInputMENameMixin",
                "AppliedEnergistics.MTEHatchCraftingInputMEMultiBlockNameMixin",
                "AppliedEnergistics.PatternMultiplierHelperMixin",
                "AppliedEnergistics.SuperMTEHatchCraftingInputMEMixin");
        }

        if (loadedMods.contains(ModList.GregTech.getID())) {
            addAll(
                list,
                "Accessor.Grade4WaterPurificationAccessor",
                "TreatedWater.Grade1WaterPurificationMixin",
                "TreatedWater.Grade2WaterPurificationMixin",
                "TreatedWater.Grade3WaterPurificationMixin",
                "TreatedWater.Grade4WaterPurificationMixin",
                "TreatedWater.Grade5WaterPurificationMixin",
                "TreatedWater.Grade6WaterPurificationMixin",
                "TreatedWater.Grade7WaterPurificationMixin",
                "TreatedWater.Grade8WaterPurificationMixin",
                "Gregtech.BlackHoleCompressorMixin",
                "FOG.FOGShardsAvailable",
                "EOH.EyeOfHarmonySuccessRateControl",
                "EOH.EyeOfHarmonyGas",
                "Accessor.EyeOfHarmonyAccessor",
                "Gregtech.ModifySomeConfigs",
                "Gregtech.MixinMTEBasicMachineFacing",
                "Gregtech.GTMachineLeftClickDataStickMixin",
                "CutCorners.RecipeSpeedMixin",
                "CutCorners.FurnaceBackendMixin",
                "CutCorners.BasicMachineOutputMixin");
        }
        if (loadedMods.contains(ModList.ENDER_IO.getID())) {
            addAll(
                list,
                "EnderIO.MixinNetworkedInventory",
                "EnderIO.MixinNetworkedInventory",
                "EnderIO.MixinItemSoulVessel");
        }

        if (loadedMods.contains(ModList.Forestry.getID())) {
            addAll(list, "Forestry.MixinBee", "Forestry.MixinMutationConditions", "Forestry.MixinBeeHomozygous");
        }

        if (loadedMods.contains(ModList.GregTech.getID()) && loadedMods.contains(ModList.Forestry.getID())) {
            addAll(list, "Gregtech.MixinGTBeeMutation");
        }

        if (loadedMods.contains(ModList.CropsNH.getID())) {
            addAll(list, "CropsNH.MixinTileEntityCropSticks", "CropsNH.MixinSeedStats");
        }

        if (loadedMods.contains(ModList.Thaumcraft.getID())) {
            addAll(
                list,
                "Thaumcraft.MixinWarpEvents",
                "Thaumcraft.MixinResearchManager",
                "Thaumcraft.MixinPlayerKnowledge",
                "Thaumcraft.MixinTileInfusionMatrix",
                "Thaumcraft.MixinVisNetHandler");
        }

        if (loadedMods.contains(ModList.WarpTheory.getID())) {
            addAll(list, "WarpTheory.MixinWarpEventHandler");
        }

        return list;
    }
}
