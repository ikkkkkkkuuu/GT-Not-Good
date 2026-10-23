// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GTLCore SkyTearsAndGregHeart; see META-INF/fragment-world-collector-port/NOTICE.md.
package com.xyp.gtnotgood.common.recipe.machine;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.gson.Gson;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.StoneType;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class FragmentWorldCollectorRecipes {

    private static final ModsItemlist[] DIMENSIONS = { ModsItemlist.DimensionOverworld, ModsItemlist.DimensionNether,
        ModsItemlist.DimensionEnd, ModsItemlist.DimensionTwilight, ModsItemlist.DimensionMoon,
        ModsItemlist.DimensionMars, ModsItemlist.DimensionVenus, ModsItemlist.DimensionMercury,
        ModsItemlist.DimensionCeres, ModsItemlist.DimensionIo, ModsItemlist.DimensionGanymede,
        ModsItemlist.DimensionPluto, ModsItemlist.DimensionEnceladus, ModsItemlist.DimensionTitan,
        ModsItemlist.DimensionTriton, ModsItemlist.DimensionBarnardC };
    private static final ModsItemlist[] PLANET_ROCKS = { ModsItemlist.MoonStone, ModsItemlist.MarsStone,
        ModsItemlist.VenusRock, ModsItemlist.MercuryRock, ModsItemlist.CeresRock, ModsItemlist.IoStone,
        ModsItemlist.GanymedeRock, ModsItemlist.PlutoStone, ModsItemlist.EnceladusRock, ModsItemlist.TitanStone,
        ModsItemlist.TritonStone };
    private static final Materials[] DRILLS = { Materials.Steel, Materials.Titanium, Materials.NaquadahAlloy,
        Materials.Neutronium, Materials.Infinity };
    private static final int[] FLUID_MULTIPLIERS = { 1, 16, 128, 1024 };
    private static final Set<String> unavailable = new LinkedHashSet<>();
    private static boolean loaded;

    private FragmentWorldCollectorRecipes() {}

    public static void scheduleRecipes() {
        if (GregTechAPI.sGTCompleteLoad == null) loadRecipes();
        else GregTechAPI.sGTCompleteLoad.add(FragmentWorldCollectorRecipes::loadRecipes);
    }

    public static void loadMachineRecipe() {
        GTModHandler.addCraftingRecipe(GTNGItemList.FragmentWorldCollector.get(1),
            new Object[] { "WNW", "PCQ", "XNX", 'W', OrePrefixes.cableGt01.get(Materials.Lead), 'N', "logRubber", 'P',
                ItemList.Sensor_LV.get(1), 'Q', ItemList.Emitter_LV.get(1), 'C', ItemList.Hull_LV.get(1), 'X',
                new ItemStack(Blocks.anvil) });
    }

    public static void loadRecipes() {
        if (loaded || !Mods.NEIOrePlugin.isModLoaded()) return;
        loaded = true;
        Data data = readData();
        for (int dimension = 0; dimension < DIMENSIONS.length; dimension++) {
            if (DIMENSIONS[dimension].get(1) == null) continue;
            int ores = data.dimensionCounts[dimension][0];
            for (int index = 0; index < ores; index++) {
                int vein = data.dimensionMaps[dimension][index];
                Outputs output = new Outputs();
                output.add(DIMENSIONS[dimension].get(1), 50);
                for (int slot = 0; slot < 4; slot++) {
                    output.add(ore(data.ores[vein][slot], data.oreCounts[vein][slot]), 10000);
                }
                addRock(output, dimension);
                output
                    .apply(base(dimension, dimension == 0 ? index + 2 : index + 1)
                        .duration(24000 / data.oreCounts[vein][4]))
                    .addTo(GTNGRecipeMaps.FragmentWorldCollectionRecipes);
            }
            for (int index = 0; index < data.dimensionCounts[dimension][1]; index++) {
                int fluid = data.dimensionMaps[dimension][ores + index];
                for (int drill = 0; drill < DRILLS.length; drill++) {
                    int amount = drill == 4 ? Integer.MAX_VALUE : data.fluidAmounts[fluid] * FLUID_MULTIPLIERS[drill];
                    FluidStack output = fluid(data.fluids[fluid], amount);
                    ItemStack head = GTOreDictUnificator.get(OrePrefixes.toolHeadDrill, DRILLS[drill], 1);
                    if (output == null || head == null) continue;
                    GTRecipeBuilder.builder()
                        .itemInputs(new ItemStack[] { DIMENSIONS[dimension].get(0), head, circuit(index + 1) },
                            new int[] { 10000, 100 - drill * 10, 10000 })
                        .itemOutputs(DIMENSIONS[dimension].get(1)).outputChances(50).fluidOutputs(output).duration(200)
                        .eut(8).addTo(GTNGRecipeMaps.FragmentWorldCollectionRecipes);
                }
            }
        }
        addSpecialRecipes();
        GTNotGood.LOG.info("registered {} fragment world collection recipes; unavailable source outputs: {}",
            GTNGRecipeMaps.FragmentWorldCollectionRecipes.getAllRecipes().size(), unavailable);
    }

    private static GTRecipeBuilder base(int dimension, int circuit) {
        return GTRecipeBuilder.builder().itemInputs(DIMENSIONS[dimension].get(0), circuit(circuit)).eut(8)
            .duration(200);
    }

    private static ItemStack circuit(int number) {
        ItemStack stack = GTUtility.getIntegratedCircuit(number);
        stack.stackSize = 0;
        return stack;
    }

    private static void addRock(Outputs output, int dimension) {
        if (dimension == 0) {
            output.add(new ItemStack(Blocks.stone, 32), 10000);
            output.add(Materials.Stone.getDust(32), 10000);
            output.add(ModsItemlist.Deepslate.get(32), 10000);
            output.add(StoneType.Deepslate.getDust(true, 32), 10000);
        } else if (dimension == 1) {
            output.add(new ItemStack(Blocks.netherrack, 32), 10000);
            output.add(Materials.Netherrack.getDust(32), 10000);
            output.add(ModsItemlist.Basalt.get(32), 10000);
            output.add(Materials.Basalt.getDust(32), 10000);
        } else if (dimension == 2) {
            output.add(new ItemStack(Blocks.end_stone, 64), 10000);
            output.add(Materials.Endstone.getDust(64), 10000);
        } else if (dimension == 3) {
            output.add(ModsItemlist.Diorite.get(64), 10000);
            output.add(dust("Diorite", 64), 10000);
        } else if (dimension < 15) {
            output.add(PLANET_ROCKS[dimension - 4].get(64), 10000);
        } else output.add(new ItemStack(Blocks.stone, 64), 10000);
    }

    private static ItemStack ore(String name, int amount) {
        if (name.equals("Goethite")) name = "BrownLimonite";
        if (name.equals("Hematite")) name = "BandedIron";
        if (name.equals("Plutonium239")) name = "Plutonium";
        ItemStack raw = oreDictionary("rawOre" + name, amount);
        if (raw != null) return raw;
        ItemStack crushed = oreDictionary("crushed" + name, amount);
        if (crushed != null) return crushed;
        unavailable.add("ore:" + name);
        return null;
    }

    private static ItemStack dust(String name, int amount) {
        ItemStack stack = oreDictionary("dust" + name, amount);
        if (stack == null) unavailable.add("dust:" + name);
        return stack;
    }

    private static ItemStack oreDictionary(String name, int amount) {
        for (ItemStack candidate : OreDictionary.getOres(name, false)) {
            if (candidate == null || candidate.getItem() == null) continue;
            return GTUtility.copyAmount(amount, candidate);
        }
        return null;
    }

    private static FluidStack fluid(String name, int amount) {
        String materialName = switch (name) {
            case "RawOil" -> "Oil";
            case "Oil" -> "OilMedium";
            default -> name;
        };
        Materials material = switch (name) {
            case "NaturalGas" -> Materials.NaturalGas;
            case "Helium3" -> Materials.Helium3;
            case "HydrochloricAcid" -> Materials.HydrochloricAcid;
            default -> Materials.get(materialName);
        };
        if (material != Materials._NULL) {
            FluidStack nativeFluid = material.getFluid(amount);
            if (nativeFluid == null) nativeFluid = material.getGas(amount);
            if (nativeFluid != null) return nativeFluid;
        }
        String nativeName = switch (name) {
            case "RawOil" -> "oil";
            case "Oil" -> "oilmedium";
            case "UnknowWater" -> "unknowwater";
            case "CoalGas" -> "coalgas";
            default -> name.toLowerCase(Locale.ROOT);
        };
        FluidStack stack = FluidRegistry.getFluidStack(nativeName, amount);
        if (stack == null) stack = FluidRegistry.getFluidStack("fluid." + nativeName, amount);
        if (stack == null) unavailable.add("fluid:" + name);
        return stack;
    }

    private static void addSpecialRecipes() {
        Outputs earth = new Outputs();
        earth.add(DIMENSIONS[0].get(1), 50);
        earth.add(new ItemStack(Blocks.dirt, 16), 6000);
        earth.add(new ItemStack(Blocks.gravel, 16), 4000);
        earth.add(new ItemStack(Blocks.sand, 16), 3000);
        earth.add(new ItemStack(Items.clay_ball, 64), 2000);
        for (int meta = 0; meta < 4; meta++) earth.add(new ItemStack(Blocks.sapling, 8, meta), 2000);
        earth.add(ModsItemlist.CherrySapling.get(8), 2000);
        earth.add(ModsItemlist.MangrovePropagule.get(8), 2000);
        earth.apply(base(0, 0)).fluidOutputs(new FluidStack(FluidRegistry.LAVA, 1000)).fluidOutputChances(500)
            .addTo(GTNGRecipeMaps.FragmentWorldCollectionRecipes);

        Outputs organic = new Outputs();
        organic.add(DIMENSIONS[0].get(1), 50);
        organic.add(new ItemStack(Items.reeds, 8), 2000);
        organic.add(GTModHandler.getIC2Item("rubberSapling", 4), 1000);
        organic.add(new ItemStack(Items.leather, 4), 500);
        organic.add(new ItemStack(Items.string, 8), 500);
        organic.add(ModsItemlist.Honeycomb.get(1), 2000);
        organic.add(new ItemStack(Blocks.soul_sand, 4), 5);
        organic.add(ModsItemlist.TotemOfUndying.get(1), 10);
        organic.apply(base(0, 1)).fluidOutputs(Materials.Oil.getFluid(1000)).fluidOutputChances(2000)
            .addTo(GTNGRecipeMaps.FragmentWorldCollectionRecipes);

        Outputs ancient = new Outputs();
        ancient.add(DIMENSIONS[3].get(1), 50);
        ancient.add(new ItemStack(Blocks.dirt, 16), 6000);
        ancient.add(ModsItemlist.Diorite.get(16), 6000);
        ancient.add(ModsItemlist.Andesite.get(16), 6000);
        ancient.add(ModsItemlist.Granite.get(16), 6000);
        ancient.add(ModsItemlist.RedGranite.get(16), 6000);
        ancient.add(ModsItemlist.Marble.get(16), 6000);
        ancient.add(new ItemStack(Blocks.sand, 16), 6000);
        ancient.add(new ItemStack(Blocks.gravel, 16), 6000);
        ancient.add(ModsItemlist.SkyStone.get(16), 500);
        ancient.apply(base(3, 0)).addTo(GTNGRecipeMaps.FragmentWorldCollectionRecipes);

        Outputs nether = new Outputs();
        nether.add(DIMENSIONS[1].get(1), 50);
        nether.add(new ItemStack(Blocks.soul_sand, 16), 6000);
        nether.add(ModsItemlist.SoulSoil.get(16), 3000);
        nether.add(ModsItemlist.AncientDebris.get(4), 500);
        nether.add(new ItemStack(Items.nether_wart, 12), 200);
        nether.add(ModsItemlist.CrimsonFungus.get(8), 2000);
        nether.add(ModsItemlist.WarpedFungus.get(8), 2000);
        nether.add(new ItemStack(Items.blaze_rod, 8), 500);
        nether.apply(base(1, 0)).fluidOutputs(new FluidStack(FluidRegistry.LAVA, 8000)).fluidOutputChances(5000)
            .addTo(GTNGRecipeMaps.FragmentWorldCollectionRecipes);

        Outputs end = new Outputs();
        end.add(DIMENSIONS[2].get(1), 50);
        end.add(new ItemStack(Blocks.dragon_egg), 5);
        end.add(ModsItemlist.DragonBreath.get(1), 500);
        end.add(ModsItemlist.ShulkerShell.get(8), 2000);
        end.add(ModsItemlist.ChorusFruit.get(16), 4000);
        end.add(ModsItemlist.ChorusFlower.get(1), 500);
        end.apply(base(2, 0)).addTo(GTNGRecipeMaps.FragmentWorldCollectionRecipes);

        Outputs frost = new Outputs();
        frost.add(DIMENSIONS[14].get(1), 50);
        // Ad Astra ice shards become native ice dust; the GTL-only spirit has no native processing chain.
        frost.add(Materials.Ice.getDust(1), 9500);
        frost.apply(base(14, 0)).addTo(GTNGRecipeMaps.FragmentWorldCollectionRecipes);

        Outputs barnard = new Outputs();
        barnard.add(DIMENSIONS[15].get(1), 50);
        barnard.add(ModsItemlist.BarnardaCLog.get(1), 500);
        barnard.add(ModsItemlist.BarnardaCLeaves.get(1), 9500);
        GTRecipeBuilder plants = barnard.apply(base(15, 0));
        FluidStack air = fluid("BarnardaAir", 16000);
        if (air != null) plants.fluidOutputs(air).fluidOutputChances(2000);
        plants.addTo(GTNGRecipeMaps.FragmentWorldCollectionRecipes);

        base(3, 9).itemInputs(DIMENSIONS[3].get(0), Materials.Steel.getDust(1), circuit(9))
            .fluidInputs(Materials.Lubricant.getFluid(100)).itemOutputs(Materials.DamascusSteel.getDust(1))
            .addTo(GTNGRecipeMaps.FragmentWorldCollectionRecipes);
    }

    private static Data readData() {
        String path = "/assets/" + ModList.GTNotGood.getID() + "/recipes/fragment_world_collection.json";
        try (InputStream input = FragmentWorldCollectorRecipes.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Missing collector recipe data: " + path);
            return new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Data.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load collector recipes", exception);
        }
    }

    private static final class Data {

        String[][] ores;
        int[][] oreCounts;
        String[] fluids;
        int[] fluidAmounts;
        int[][] dimensionCounts;
        int[][] dimensionMaps;
    }

    private static final class Outputs {

        private final List<ItemStack> items = new ArrayList<>();
        private final List<Integer> chances = new ArrayList<>();

        void add(ItemStack stack, int chance) {
            if (stack == null || stack.getItem() == null) return;
            items.add(stack);
            chances.add(chance);
        }

        GTRecipeBuilder apply(GTRecipeBuilder recipe) {
            return recipe.itemOutputs(items.toArray(new ItemStack[0]),
                chances.stream().mapToInt(Integer::intValue).toArray());
        }
    }
}
