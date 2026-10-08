package com.xyp.gtnotgood.utils.machine.factory;

import static bartworks.system.material.WerkstoffLoader.*;
import static gregtech.api.enums.OrePrefixes.dust;
import static gregtech.api.enums.OrePrefixes.milled;
import static gregtech.api.recipe.RecipeMaps.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.fluids.GTPPFluids;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;

/** Curated process templates resolved against registered recipes; never creates recipes or bypasses catalog checks. */
public final class FactoryPresets {

    public enum Preset {

        PlatinumGroup(FactoryText.PresetPlatinumGroup, FactoryText.PresetPlatinumGroupHelp),
        Platinum(FactoryText.PresetPlatinum, FactoryText.PresetPlatinumHelp),
        Palladium(FactoryText.PresetPalladium, FactoryText.PresetPalladiumHelp),
        Residue(FactoryText.PresetResidue, FactoryText.PresetResidueHelp),
        Ruthenium(FactoryText.PresetRuthenium, FactoryText.PresetRutheniumHelp),
        Osmium(FactoryText.PresetOsmium, FactoryText.PresetOsmiumHelp),
        Iridium(FactoryText.PresetIridium, FactoryText.PresetIridiumHelp),
        Rhodium(FactoryText.PresetRhodium, FactoryText.PresetRhodiumHelp),
        Polybenzimidazole(FactoryText.PresetPbi, FactoryText.PresetPbiHelp),
        RocketRp1(FactoryText.PresetRp1, FactoryText.PresetRp1Help),
        RocketHydrazine(FactoryText.PresetHydrazine, FactoryText.PresetHydrazineHelp),
        RocketCn3h7o3(FactoryText.PresetCnFuel, FactoryText.PresetCnFuelHelp),
        RocketH8n4c2o4(FactoryText.PresetH8Fuel, FactoryText.PresetH8FuelHelp),
        CetaneDiesel(FactoryText.PresetCetane, FactoryText.PresetCetaneHelp),
        EpoxyPropene(FactoryText.PresetEpoxy, FactoryText.PresetEpoxyHelp),
        Netherite(FactoryText.PresetNetherite, FactoryText.PresetNetheriteHelp);

        public final FactoryText title;
        public final FactoryText help;

        Preset(FactoryText title, FactoryText help) {
            this.title = title;
            this.help = help;
        }
    }

    public static final class Result {

        private final FactoryGraph graph;
        public final String problem;

        private Result(FactoryGraph graph, String problem) {
            this.graph = graph;
            this.problem = problem;
        }

        public boolean available() {
            return graph != null;
        }

        public int size() {
            return graph == null ? 0 : graph.nodes.size();
        }

        public FactoryGraph copy() {
            return graph == null ? null : graph.copy();
        }
    }

    private static final Map<Preset, Result> cache = new HashMap<>();

    private FactoryPresets() {}

    /** Called after recipe loading by the editor/server; only successful templates are cached. */
    public static synchronized Result resolve(Preset preset) {
        Result cached = cache.get(preset);
        if (cached != null) return cached;
        try {
            FactoryGraph graph = build(preset);
            Result result = new Result(graph, "");
            cache.put(preset, result);
            return result;
        } catch (IllegalArgumentException | ArithmeticException invalid) {
            return new Result(null, invalid.getMessage());
        }
    }

    /** Atomically fills only an unlocked empty draft; failure preserves the entire existing graph. */
    public static boolean apply(FactoryGraph draft, boolean locked, String id) {
        if (locked || !draft.nodes.isEmpty()) return false;
        Preset preset;
        try {
            preset = Preset.valueOf(id);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
        FactoryGraph graph = resolve(preset).copy();
        if (graph == null) return false;
        draft.read(graph.write());
        return true;
    }

    private static List<Step> stepsFor(Preset preset) {
        List<Step> steps = new ArrayList<>();
        switch (preset) {
            case PlatinumGroup:
                Map<String, Step> unique = new LinkedHashMap<>();
                for (Preset part : new Preset[] { Preset.Platinum, Preset.Palladium, Preset.Residue, Preset.Ruthenium,
                    Preset.Osmium, Preset.Iridium, Preset.Rhodium }) {
                    for (Step step : stepsFor(part)) unique.putIfAbsent(step.resolve().id, step);
                }
                steps.addAll(unique.values());
                break;
            case Netherite:
                ItemStack milledNetherrack = GTOreDictUnificator.get(milled, Materials.Netherrack, 1);
                ItemStack scrap = ModsItemlist.NetheriteScrap.get(1);
                steps.add(step(multiblockRockBreakerRecipes, Materials.Glowstone.getDust(1),
                    new ItemStack(Blocks.netherrack), GTUtility.getIntegratedCircuit(6)));
                steps.add(step(millingRecipes, new ItemStack(Blocks.netherrack), milledNetherrack,
                    GregtechItemList.Milling_Ball_Alumina.get(0), GTUtility.getIntegratedCircuit(10)));
                steps.add(
                    step(vacuumFreezerRecipes, Materials.NetherAir.getFluid(1), Materials.NetherSemiFluid.getFluid(1)));
                steps.add(step(distillationTowerRecipes, Materials.NetherSemiFluid.getFluid(1),
                    ItemList.Heavy_Hellish_Mud.get(1)));
                steps.add(step(crackingRecipes, Materials.NefariousGas.getFluid(1), Materials.NefariousOil.getFluid(1),
                    Materials.Grade2PurifiedWater.getFluid(1), GTUtility.getIntegratedCircuit(1)));
                steps.add(
                    step(flotationCellRecipes, milledNetherrack, new FluidStack(GTPPFluids.NetherrackFlotationFroth, 1),
                        milledNetherrack, milledNetherrack, milledNetherrack, Materials.NefariousOil.getFluid(1)));
                steps.add(step(vacuumFurnaceRecipes, new FluidStack(GTPPFluids.NetherrackFlotationFroth, 1),
                    Materials.PoorNetherWaste.getFluid(1)));
                steps.add(
                    step(chemicalBathRecipes, scrap, ItemList.Hot_Netherite_Scrap.get(1), Materials.Lava.getFluid(1)));
                steps.add(step(chemicalBathRecipes, ItemList.Hot_Netherite_Scrap.get(1),
                    ItemList.Brittle_Netherite_Scrap.get(1), ItemList.Heavy_Hellish_Mud.get(1),
                    Materials.PoorNetherWaste.getFluid(1)));
                steps.add(step(maceratorRecipes, ItemList.Brittle_Netherite_Scrap.get(1),
                    ItemList.Netherite_Nanoparticles.get(1)));
                steps.add(step(blastFurnaceRecipes, ItemList.Netherite_Nanoparticles.get(1),
                    ItemList.Intensely_Bonded_Netherite_Nanoparticles.get(1), Materials.HellishMetal.getMolten(1),
                    GTUtility.getIntegratedCircuit(1)));
                break;
            case EpoxyPropene:
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Propene.getGas(1),
                    Materials.Epichlorohydrin.getFluid(1), Materials.Chlorine.getGas(1), Materials.Water.getFluid(1),
                    Materials.SodiumHydroxide.getDust(1), GTUtility.getIntegratedCircuit(23)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Acetone.getFluid(1),
                    Materials.BisphenolA.getFluid(1), Materials.Phenol.getFluid(1),
                    Materials.HydrochloricAcid.getFluid(1), GTUtility.getIntegratedCircuit(1)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.BisphenolA.getFluid(1),
                    Materials.Epoxid.getMolten(1), Materials.Epichlorohydrin.getFluid(1),
                    Materials.SodiumHydroxide.getDust(1)));
                steps.add(step(electrolyzerNonCellRecipes, Materials.SaltWater.getFluid(1),
                    Materials.SodiumHydroxide.getDust(1), GTUtility.getIntegratedCircuit(1)));
                steps.add(step(electrolyzerNonCellRecipes, Materials.HydrochloricAcid.getFluid(1),
                    Materials.Chlorine.getGas(1), GTUtility.getIntegratedCircuit(1)));
                break;
            case RocketRp1:
                steps.add(step(distilleryRecipes, Materials.Diesel.getFluid(1), new FluidStack(GTPPFluids.Kerosene, 1),
                    GTUtility.getIntegratedCircuit(23)));
                steps.add(step(distilleryRecipes, new FluidStack(GTPPFluids.Kerosene, 1),
                    new FluidStack(GTPPFluids.RP1, 1), GTUtility.getIntegratedCircuit(23)));
                steps.add(step(chemicalPlantRecipes, new FluidStack(GTPPFluids.RP1, 1),
                    new FluidStack(GTPPFluids.RP1RocketFuel, 1), Materials.LiquidOxygen.getGas(1),
                    GTUtility.getIntegratedCircuit(1)));
                break;
            case RocketHydrazine:
            case RocketCn3h7o3:
            case RocketH8n4c2o4:
                addHydrazine(steps);
                if (preset == Preset.RocketHydrazine) {
                    steps.add(step(chemicalPlantRecipes, new FluidStack(GTPPFluids.Hydrazine, 1),
                        new FluidStack(GTPPFluids.DenseHydrazineFuelMixture, 1), Materials.Methanol.getFluid(1),
                        GTUtility.getIntegratedCircuit(2)));
                } else if (preset == Preset.RocketCn3h7o3) {
                    steps.add(step(chemicalPlantRecipes, new FluidStack(GTPPFluids.Hydrazine, 1),
                        new FluidStack(GTPPFluids.Monomethylhydrazine, 1), Materials.Hydrogen.getGas(1),
                        Materials.Carbon.getDust(1), GTUtility.getIntegratedCircuit(21)));
                    steps.add(step(chemicalPlantRecipes, new FluidStack(GTPPFluids.Monomethylhydrazine, 1),
                        new FluidStack(GTPPFluids.CN3H7O3RocketFuel, 1), Materials.NitricAcid.getFluid(1),
                        GTUtility.getIntegratedCircuit(3)));
                } else {
                    steps.add(step(chemicalPlantRecipes, Materials.Methanol.getFluid(1),
                        new FluidStack(GTPPFluids.Formaldehyde, 1), Materials.Oxygen.getGas(1),
                        GTUtility.getIntegratedCircuit(21)));
                    steps.add(step(chemicalPlantRecipes, new FluidStack(GTPPFluids.Hydrazine, 1),
                        Materials.Dimethylhydrazine.getFluid(1), new FluidStack(GTPPFluids.Formaldehyde, 1),
                        Materials.Hydrogen.getGas(1), GTUtility.getIntegratedCircuit(21)));
                    steps.add(step(chemicalPlantRecipes, Materials.NitricAcid.getFluid(1),
                        new FluidStack(GTPPFluids.NitrogenTetroxide, 1), Materials.Copper.getDust(1)));
                    steps.add(step(electrolyzerRecipes, Materials.CupricOxide.getDust(1), Materials.Copper.getDust(1)));
                    steps.add(step(chemicalPlantRecipes, Materials.Dimethylhydrazine.getFluid(1),
                        new FluidStack(GTPPFluids.H8N4C2O4RocketFuel, 1),
                        new FluidStack(GTPPFluids.NitrogenTetroxide, 1), GTUtility.getIntegratedCircuit(4)));
                }
                break;
            case CetaneDiesel:
                steps.add(step(multiblockChemicalReactorRecipes, Materials.LightFuel.getFluid(1),
                    Materials.Diesel.getFluid(1), Materials.HeavyFuel.getFluid(1), GTUtility.getIntegratedCircuit(24)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Ethenone.getGas(1),
                    Materials.Tetranitromethane.getFluid(1), Materials.NitricAcid.getFluid(1)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Diesel.getFluid(1),
                    Materials.NitroFuel.getFluid(1), Materials.Tetranitromethane.getFluid(1),
                    GTUtility.getIntegratedCircuit(24)));
                break;
            case Polybenzimidazole:
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Benzene.getFluid(1),
                    Materials.Chlorobenzene.getFluid(1), Materials.Chlorine.getGas(1),
                    GTUtility.getIntegratedCircuit(1)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Chlorobenzene.getFluid(1),
                    Materials.Nitrochlorobenzene.getFluid(1), Materials.NitrationMixture.getFluid(1),
                    GTUtility.getIntegratedCircuit(1)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Nitrochlorobenzene.getFluid(1),
                    Materials.Dichlorobenzidine.getFluid(1), Materials.Copper.getDust(1),
                    GTUtility.getIntegratedCircuit(9)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Dichlorobenzidine.getFluid(1),
                    Materials.Diaminobenzidin.getFluid(1), Materials.Ammonia.getGas(1), Materials.Zinc.getDust(1)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Methane.getGas(1),
                    Materials.Dimethylbenzene.getFluid(1), Materials.Benzene.getFluid(1),
                    GTUtility.getIntegratedCircuit(1)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Dimethylbenzene.getFluid(1),
                    Materials.PhthalicAcid.getFluid(1), Materials.Oxygen.getGas(1),
                    Materials.Potassiumdichromate.getDust(1)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.PhthalicAcid.getFluid(1),
                    Materials.Diphenylisophthalate.getFluid(1), Materials.Phenol.getFluid(1),
                    Materials.SulfuricAcid.getFluid(1)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Diphenylisophthalate.getFluid(1),
                    Materials.Polybenzimidazole.getMolten(1), Materials.Diaminobenzidin.getFluid(1)));
                steps.add(
                    step(mixerNonCellRecipes, Materials.NitricAcid.getFluid(1), Materials.NitrationMixture.getFluid(1),
                        Materials.SulfuricAcid.getFluid(1), GTUtility.getIntegratedCircuit(1)));
                steps.add(step(distillationTowerRecipes, Materials.DilutedSulfuricAcid.getFluid(1),
                    Materials.SulfuricAcid.getFluid(1)));
                steps.add(step(electrolyzerNonCellRecipes, Materials.HydrochloricAcid.getFluid(1),
                    Materials.Chlorine.getGas(1), GTUtility.getIntegratedCircuit(1)));
                break;
            case Platinum:
                steps.add(step(multiblockChemicalReactorRecipes, PTMetallicPowder.get(dust, 90),
                    PTConcentrate.getFluidOrGas(1), AquaRegia.getFluidOrGas(1), GTUtility.getIntegratedCircuit(9)));
                steps.add(step(multiblockChemicalReactorRecipes, PTConcentrate.getFluidOrGas(180000),
                    PTSaltCrude.get(dust), AmmoniumChloride.getFluidOrGas(1), GTUtility.getIntegratedCircuit(3)));
                steps.add(step(sifterRecipes, PTSaltCrude.get(dust, 80), PTSaltRefined.get(dust)));
                steps.add(step(blastFurnaceRecipes, PTSaltRefined.get(dust, 76), PTMetallicPowder.get(dust)));
                steps.add(step(multiblockChemicalReactorRecipes, PTRawPowder.get(dust, 20),
                    Materials.Platinum.getDust(1), Materials.Calcium.getDust(1)));
                steps.add(step(electrolyzerRecipes, CalciumChloride.get(dust, 15), Materials.Calcium.getDust(1)));
                break;
            case Palladium:
                steps.add(step(multiblockChemicalReactorRecipes, PDAmmonia.getFluidOrGas(18000), PDSalt.get(dust),
                    PDMetallicPowder.get(dust), GTUtility.getIntegratedCircuit(9)));
                steps.add(step(sifterRecipes, PDSalt.get(dust, 32), PDMetallicPowder.get(dust)));
                steps.add(step(multiblockChemicalReactorRecipes, PDRawPowder.get(dust, 4),
                    Materials.Palladium.getDust(1), FormicAcid.getFluidOrGas(1)));
                break;
            case Residue:
                steps.add(step(blastFurnaceRecipes, PTResidue.get(dust, 550), LeachResidue.get(dust),
                    PotassiumDisulfate.getMolten(1)));
                steps.add(step(multiblockChemicalReactorRecipes, RHSulfate.getFluidOrGas(198000),
                    RHSulfateSolution.getFluidOrGas(1), Materials.Water.getFluid(1),
                    GTUtility.getIntegratedCircuit(3)));
                steps.add(step(blastFurnaceRecipes, LeachResidue.get(dust, 570), SodiumRuthenate.get(dust),
                    Materials.Saltpeter.getDust(1), Materials.SaltWater.getFluid(1)));
                steps.add(step(blastFurnaceRecipes, IrOsLeachResidue.get(dust, 342),
                    AcidicOsmiumSolution.getFluidOrGas(1), Materials.HydrochloricAcid.getFluid(1)));
                steps.add(step(blastFurnaceRecipes, IrLeachResidue.get(dust, 171), IridiumDioxide.get(dust)));
                steps.add(step(centrifugeRecipes, PGSDResidue.get(dust, 171), Materials.Gold.getDust(1)));
                break;
            case Ruthenium:
                steps.add(step(multiblockChemicalReactorRecipes, SodiumRuthenate.get(dust, 30),
                    RutheniumTetroxideSollution.getFluidOrGas(1), Materials.Chlorine.getGas(1)));
                steps.add(step(fluidHeaterRecipes, RutheniumTetroxideSollution.getFluidOrGas(45000),
                    HotRutheniumTetroxideSollution.getFluidOrGas(1)));
                steps.add(step(distillationTowerRecipes, HotRutheniumTetroxideSollution.getFluidOrGas(90000),
                    RutheniumTetroxide.getFluidOrGas(1)));
                steps.add(step(fluidSolidifierRecipes, RutheniumTetroxide.getFluidOrGas(72000),
                    RutheniumTetroxide.get(dust)));
                steps.add(step(multiblockChemicalReactorRecipes, RutheniumTetroxide.get(dust, 72), Ruthenium.get(dust),
                    Materials.HydrochloricAcid.getFluid(1)));
                break;
            case Osmium:
                steps.add(step(distillationTowerRecipes, AcidicOsmiumSolution.getFluidOrGas(10000),
                    OsmiumSolution.getFluidOrGas(1)));
                steps.add(step(multiblockChemicalReactorRecipes, OsmiumSolution.getFluidOrGas(1000),
                    Materials.Osmium.getDust(1), Materials.HydrochloricAcid.getFluid(1)));
                break;
            case Iridium:
                steps.add(step(multiblockChemicalReactorRecipes, IridiumDioxide.get(dust),
                    AcidicIridiumSolution.getFluidOrGas(1), Materials.HydrochloricAcid.getFluid(1)));
                steps.add(step(multiblockChemicalReactorRecipes, AcidicIridiumSolution.getFluidOrGas(1000),
                    IridiumChloride.get(dust), AmmoniumChloride.getFluidOrGas(1)));
                steps.add(step(multiblockChemicalReactorRecipes, IridiumChloride.get(dust),
                    Materials.Iridium.getDust(1), Materials.Calcium.getDust(1)));
                steps.add(step(fluidSolidifierRecipes, CalciumChloride.getFluidOrGas(3000), CalciumChloride.get(dust)));
                steps.add(step(electrolyzerRecipes, CalciumChloride.get(dust, 3), Materials.Calcium.getDust(1)));
                steps.add(step(centrifugeRecipes, PGSDResidue2.get(dust), Materials.Copper.getDust(1)));
                break;
            case Rhodium:
                steps.add(step(multiblockChemicalReactorRecipes, RHSulfateSolution.getFluidOrGas(100000),
                    CrudeRhMetall.get(dust), Materials.Zinc.getDust(1)));
                steps.add(step(multiblockChemicalReactorRecipes, ZincSulfate.get(dust, 600), Materials.Zinc.getDust(1),
                    Materials.Hydrogen.getGas(1)));
                steps.add(step(blastFurnaceRecipes, CrudeRhMetall.get(dust, 100), RHSalt.get(dust),
                    Materials.Salt.getDust(1), Materials.Chlorine.getGas(1)));
                steps.add(step(mixerRecipes, RHSalt.get(dust, 300), RHSaltSolution.getFluidOrGas(1),
                    Materials.Water.getFluid(1)));
                steps.add(step(multiblockChemicalReactorRecipes, RHSaltSolution.getFluidOrGas(60000),
                    RHNitrate.get(dust), SodiumNitrate.get(dust)));
                steps.add(step(multiblockChemicalReactorRecipes, Materials.Sodium.getDust(60), SodiumNitrate.get(dust),
                    Materials.NitricAcid.getFluid(1)));
                steps.add(step(sifterRecipes, RHNitrate.get(dust, 60), RhFilterCake.get(dust)));
                steps.add(step(mixerRecipes, RhFilterCake.get(dust, 57), RHFilterCakeSolution.getFluidOrGas(1),
                    Materials.Water.getFluid(1)));
                steps.add(
                    step(multiblockChemicalReactorRecipes, RHFilterCakeSolution.getFluidOrGas(57000), ReRh.get(dust)));
                steps.add(step(multiblockChemicalReactorRecipes, ReRh.get(dust, 57), Rhodium.get(dust),
                    Materials.HydrochloricAcid.getFluid(1)));
                break;
        }
        if (preset == Preset.Ruthenium || preset == Preset.Rhodium)
            steps.add(step(electrolyzerRecipes, Materials.Salt.getDust(2), Materials.Sodium.getDust(1)));
        return steps;
    }

    private static FactoryGraph build(Preset preset) {
        List<Step> steps = stepsFor(preset);
        FactoryGraph graph = new FactoryGraph();
        for (Step step : steps) graph.add(step.resolve().id);
        if (preset == Preset.Netherite || preset == Preset.PlatinumGroup)
            for (FactoryGraph.Node node : graph.nodes) node.wholeLineBatch = true;
        FactoryRouting.connect(graph);
        balanceRegisteredFlows(graph, steps, preset);
        if (FactoryPreview.describe(graph).exportIssue() != null)
            throw new IllegalArgumentException("Recovery balance retains undeliverable outputs");
        return graph;
    }

    /** Anthraquinone is recycled by the existing net-cycle executor; anthracene remains a consumed feed. */
    private static void addHydrazine(List<Step> steps) {
        steps.add(step(chemicalPlantRecipes, new FluidStack(GTPPFluids.Ethylanthraquinone, 1),
            new FluidStack(GTPPFluids.Ethylanthrahydroquinone, 1), Materials.Hydrogen.getGas(1),
            GTUtility.getIntegratedCircuit(4)));
        steps.add(step(chemicalPlantRecipes, new FluidStack(GTPPFluids.Ethylanthrahydroquinone, 1),
            new FluidStack(GTPPFluids.HydrogenPeroxide, 1), Materials.Oxygen.getGas(1),
            new FluidStack(GTPPFluids.Anthracene, 1), GTUtility.getIntegratedCircuit(4)));
        steps.add(step(chemicalPlantRecipes, new FluidStack(GTPPFluids.HydrogenPeroxide, 1),
            new FluidStack(GTPPFluids.Hydrazine, 1), Materials.Ammonia.getGas(1), GTUtility.getIntegratedCircuit(21)));
    }

    /** Solve intermediate pools from actual registered quantities, including recycled outputs from later steps. */
    private static void balanceRegisteredFlows(FactoryGraph graph, List<Step> steps, Preset preset) {
        List<Object> intermediates = new ArrayList<>();
        for (int i = 1; i < steps.size(); i++) {
            Object input = steps.get(i).inputs[0];
            // Salt recovery only covers part of the sodium demand; the rest remains a real external input.
            if (
                (preset == Preset.Rhodium || preset == Preset.PlatinumGroup) && input instanceof ItemStack item
                    && same(item, Materials.Sodium.getDust(1))
            ) continue;
            // Nether air supplies the mud demand; surplus nefarious gas remains an exported byproduct.
            if (
                preset == Preset.Netherite && input instanceof FluidStack fluid
                    && fluid.isFluidEqual(Materials.NefariousGas.getFluid(1))
            ) continue;
            intermediates.add(input);
        }
        if (preset == Preset.Netherite) {
            intermediates.add(ItemList.Heavy_Hellish_Mud.get(1));
            intermediates.add(Materials.NefariousOil.getFluid(1));
            intermediates.add(Materials.PoorNetherWaste.getFluid(1));
        }
        if (preset == Preset.Rhodium || preset == Preset.PlatinumGroup) intermediates.add(SodiumNitrate.get(dust));
        if (preset == Preset.Polybenzimidazole) {
            intermediates.add(Materials.Diaminobenzidin.getFluid(1));
            intermediates.add(Materials.NitrationMixture.getFluid(1));
        }
        if (preset == Preset.RocketH8n4c2o4) {
            intermediates.add(new FluidStack(GTPPFluids.Formaldehyde, 1));
            intermediates.add(new FluidStack(GTPPFluids.NitrogenTetroxide, 1));
        }
        if (preset == Preset.EpoxyPropene) {
            intermediates.add(Materials.Epichlorohydrin.getFluid(1));
            intermediates.add(Materials.SodiumHydroxide.getDust(1));
        }
        if (preset == Preset.CetaneDiesel) intermediates.add(Materials.Tetranitromethane.getFluid(1));
        List<FactoryBalancer.Fraction[]> equations = new ArrayList<>();
        for (Object type : intermediates) {
            FactoryBalancer.Fraction[] row = new FactoryBalancer.Fraction[graph.nodes.size()];
            boolean produced = false;
            for (int i = 0; i < row.length; i++) {
                GTRecipe recipe = FactoryRecipeCatalog.get(graph.nodes.get(i).recipe).recipe;
                long output = 0;
                if (type instanceof ItemStack item) {
                    for (int j = 0; j < recipe.mOutputs.length; j++) if (same(item, recipe.mOutputs[j]))
                        output += (long) recipe.mOutputs[j].stackSize * recipe.getOutputChance(j);
                } else for (FluidStack fluid : recipe.mFluidOutputs)
                    if (fluid != null && ((FluidStack) type).isFluidEqual(fluid)) output += (long) fluid.amount * 10000;
                produced |= output > 0;
                long input = Math.max(0, inputAmount(recipe, type)) * 10000;
                row[i] = new FactoryBalancer.Fraction(output - input, (long) Math.max(1, recipe.mDuration) * 10000);
            }
            if (produced) equations.add(row);
        }
        int[] solution = FactoryBalancer.solve(equations.toArray(new FactoryBalancer.Fraction[0][]), graph.nodes.size(),
            FactoryGraph.MAX_PARALLEL);
        if (solution == null) throw new IllegalArgumentException("No integral recovery balance");
        for (int i = 0; i < solution.length; i++) graph.nodes.get(i).parallel = solution[i];
    }

    private static Step step(RecipeMap<?> map, Object input, Object output, Object... others) {
        Step step = new Step();
        step.map = map;
        step.inputs = new Object[others.length + 1];
        step.inputs[0] = input;
        System.arraycopy(others, 0, step.inputs, 1, others.length);
        step.output = output;
        return step;
    }

    private static final class Step {

        RecipeMap<?> map;
        Object[] inputs;
        Object output;

        FactoryRecipeCatalog.Entry resolve() {
            if (output == null || Arrays.stream(inputs).anyMatch(input -> input == null))
                throw new IllegalArgumentException(map.unlocalizedName + " : Missing registered ingredient");
            Map<String, FactoryRecipeCatalog.Entry> found = new LinkedHashMap<>();
            for (GTRecipe recipe : map.getAllRecipes()) {
                if (FactoryRecipeCatalog.unsupportedReason(map, recipe) != null || !outputMatches(recipe, output))
                    continue;
                long positive = Arrays.stream(recipe.mInputs).filter(i -> i != null && i.stackSize > 0).count()
                    + Arrays.stream(recipe.mFluidInputs).filter(f -> f != null && f.amount > 0).count();
                long requested = Arrays.stream(inputs).filter(i -> amount(i) > 0).count();
                if (
                    positive != requested
                        || Arrays.stream(inputs).anyMatch(i -> inputAmount(recipe, i) < (amount(i) == 0 ? 0 : 1))
                ) continue;
                FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.find(map, recipe);
                if (entry != null) found.put(entry.id, entry);
            }
            if (found.size() != 1) throw new IllegalArgumentException(
                map.unlocalizedName + " : " + name(inputs[0]) + " → " + name(output) + " (" + found.size() + ")");
            return found.values().iterator().next();
        }
    }

    private static long inputAmount(GTRecipe recipe, Object type) {
        long amount = -1;
        if (
            type instanceof ItemStack item
        ) for (ItemStack input : recipe.mInputs) if (same(item, input)) amount = Math.max(0, amount) + input.stackSize;
        if (type instanceof FluidStack fluid) for (FluidStack input : recipe.mFluidInputs)
            if (input != null && fluid.isFluidEqual(input)) amount = Math.max(0, amount) + input.amount;
        return amount;
    }

    private static boolean outputMatches(GTRecipe recipe, Object output) {
        if (output instanceof ItemStack item)
            return Arrays.stream(recipe.mOutputs).anyMatch(value -> same(item, value));
        return Arrays.stream(recipe.mFluidOutputs)
            .anyMatch(value -> value != null && ((FluidStack) output).isFluidEqual(value));
    }

    private static boolean same(ItemStack first, ItemStack second) {
        return first != null && second != null && GTUtility.areStacksEqual(first, second);
    }

    private static int amount(Object value) {
        return value instanceof ItemStack item ? item.stackSize : ((FluidStack) value).amount;
    }

    private static String name(Object value) {
        if (value == null) return "Missing registered ingredient";
        return value instanceof ItemStack item ? item.getDisplayName() : ((FluidStack) value).getLocalizedName();
    }
}
