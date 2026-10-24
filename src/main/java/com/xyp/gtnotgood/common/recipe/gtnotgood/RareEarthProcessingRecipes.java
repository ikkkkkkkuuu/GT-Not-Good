// SPDX-License-Identifier: MIT
// Copyright (c) 2026 QionSi
// Production Line Simplification recipe port; see META-INF/production-simplification-port/NOTICE.md.
package com.xyp.gtnotgood.common.recipe.gtnotgood;

import static bartworks.system.material.WerkstoffLoader.CrudeRhMetall;
import static bartworks.system.material.WerkstoffLoader.IrLeachResidue;
import static bartworks.system.material.WerkstoffLoader.IrOsLeachResidue;
import static bartworks.system.material.WerkstoffLoader.IridiumDioxide;
import static bartworks.system.material.WerkstoffLoader.LeachResidue;
import static bartworks.system.material.WerkstoffLoader.PDAmmonia;
import static bartworks.system.material.WerkstoffLoader.PDMetallicPowder;
import static bartworks.system.material.WerkstoffLoader.PTMetallicPowder;
import static bartworks.system.material.WerkstoffLoader.PTResidue;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static java.util.Objects.requireNonNull;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import bartworks.system.material.Werkstoff;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.material.Material;
import gtPlusPlus.core.material.MaterialsElements;

public final class RareEarthProcessingRecipes {

    private static final int ORE_WATER = 64000;
    private static final MaterialsElements ELEMENTS = MaterialsElements.getInstance();

    private RareEarthProcessingRecipes() {}

    public static void loadRecipes() {
        registerPlatinumMetallicPowder();
        registerIridiumDioxide();
        registerPalladiumMetallicPowder();
        registerPalladiumEnrichedAmmonia();
        registerLeachResidue();
        registerPlatinumResidue();
        registerCrudeRhodiumMetal();
        registerRarestMetalResidue();
        registerIridiumMetalResidue();
        registerBastnasite();
        registerRareEarth();
        registerPyropeOre();
        registerAlmandineOre();
        registerGrossularOre();
        registerSpessartineOre();
        registerRedstoneOre();
        registerNickelOre();
        registerChalcopyriteOre();
        registerPentlanditeOre();
        registerSphaleriteOre();
        registerPlatinumOre();
        registerMonaziteOre();
        registerNetherrack();
    }

    private static void registerPlatinumMetallicPowder() {
        GTRecipeBuilder.builder().circuit(1)
            .itemInputsUnsafe(dust(PTMetallicPowder, 192), dust(Materials.Sulfur, 13), dust(Materials.Saltpeter, 137),
                dust(Materials.Sodium, 131))
            .fluidInputs(Materials.Oxygen.getGas(743000), Materials.Nitrogen.getGas(190000),
                Materials.Hydrogen.getGas(167000))
            .itemOutputs(dust(Materials.Platinum, 138), dust(PDMetallicPowder, 176), dust(ELEMENTS.RHODIUM, 29),
                dust(ELEMENTS.RUTHENIUM, 99), dust(Materials.Iridium, 42), dust(Materials.Osmium, 5),
                dust(Materials.Gold, 17), dust(Materials.Copper, 21), dust(Materials.Nickel, 21),
                dust(Materials.Silicon, 9))
            .fluidOutputs(Materials.Chlorine.getGas(158000)).duration(300 * SECONDS).eut(7680)
            .addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerIridiumDioxide() {
        GTRecipeBuilder.builder().circuit(1).itemInputsUnsafe(dust(IridiumDioxide, 2))
            .itemOutputs(dust(Materials.Iridium, 2), dust(Materials.Nickel, 1), dust(Materials.Copper, 1))
            .fluidOutputs(Materials.Oxygen.getGas(4000)).duration(10 * SECONDS).eut(7680)
            .addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerPalladiumMetallicPowder() {
        GTRecipeBuilder.builder().circuit(1).itemInputsUnsafe(dust(PDMetallicPowder, 90))
            .fluidInputs(Materials.Oxygen.getGas(64000), Materials.Nitrogen.getGas(224000),
                Materials.Hydrogen.getGas(546000))
            .itemOutputs(dust(Materials.Palladium, 64), dust(Materials.Carbon, 64)).duration(120 * SECONDS).eut(16384)
            .addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerPalladiumEnrichedAmmonia() {
        GTRecipeBuilder.builder().circuit(1)
            .fluidInputs(PDAmmonia.getFluidOrGas(90000), Materials.Hydrogen.getGas(538000),
                Materials.Oxygen.getGas(64000), Materials.Nitrogen.getGas(134000))
            .itemOutputs(dust(Materials.Palladium, 64), dust(Materials.Carbon, 64)).duration(120 * SECONDS).eut(16384)
            .addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerLeachResidue() {
        GTRecipeBuilder.builder().circuit(1)
            .itemInputsUnsafe(dust(LeachResidue, 64), dust(Materials.Saltpeter, 64), dust(Materials.Sodium, 25))
            .fluidInputs(Materials.Hydrogen.getGas(175000))
            .itemOutputs(dust(ELEMENTS.RUTHENIUM, 46), dust(Materials.Iridium, 20), dust(Materials.Osmium, 2),
                dust(Materials.Gold, 8), dust(Materials.Copper, 10), dust(Materials.Silicon, 4),
                dust(Materials.Nickel, 10))
            .fluidOutputs(Materials.Oxygen.getGas(14000), Materials.Chlorine.getGas(107000)).duration(180 * SECONDS)
            .eut(16384).addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerPlatinumResidue() {
        GTRecipeBuilder.builder().circuit(1)
            .itemInputsUnsafe(dust(PTResidue, 96), dust(Materials.Sodium, 49), dust(Materials.Saltpeter, 100))
            .fluidInputs(Materials.Nitrogen.getGas(44000), Materials.Oxygen.getGas(184000),
                Materials.Hydrogen.getGas(220000))
            .itemOutputs(dust(ELEMENTS.RHODIUM, 20), dust(ELEMENTS.RUTHENIUM, 72), dust(Materials.Iridium, 30),
                dust(Materials.Osmium, 3), dust(Materials.Gold, 12), dust(Materials.Copper, 15),
                dust(Materials.Nickel, 15), dust(Materials.Silicon, 6))
            .fluidOutputs(Materials.Chlorine.getGas(198000)).duration(180 * SECONDS).eut(16384)
            .addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerCrudeRhodiumMetal() {
        GTRecipeBuilder.builder().circuit(1).itemInputsUnsafe(dust(CrudeRhMetall, 100), dust(Materials.Sodium, 50))
            .fluidInputs(Materials.Nitrogen.getGas(1000), Materials.Oxygen.getGas(180000),
                Materials.Chlorine.getGas(90000))
            .itemOutputs(dust(ELEMENTS.RHODIUM, 57)).fluidOutputs(Materials.Hydrogen.getGas(71000))
            .duration(180 * SECONDS).eut(16384).addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerRarestMetalResidue() {
        GTRecipeBuilder.builder().circuit(1).itemInputsUnsafe(dust(IrOsLeachResidue, 20))
            .fluidInputs(Materials.Hydrogen.getGas(50000))
            .itemOutputs(dust(Materials.Iridium, 10), dust(Materials.Osmium, 1), dust(Materials.Copper, 5),
                dust(Materials.Silicon, 2), dust(Materials.Nickel, 5), dust(Materials.Gold, 4))
            .fluidOutputs(Materials.Oxygen.getGas(4000), Materials.Chlorine.getGas(2000)).duration(150 * SECONDS)
            .eut(32768).addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerIridiumMetalResidue() {
        GTRecipeBuilder.builder().circuit(1).itemInputsUnsafe(dust(IrLeachResidue, 20))
            .fluidInputs(Materials.Hydrogen.getGas(8000), Materials.Chlorine.getGas(40000))
            .itemOutputs(dust(Materials.Iridium, 20), dust(Materials.Gold, 8), dust(Materials.Copper, 10),
                dust(Materials.Silicon, 10), dust(Materials.Nickel, 10))
            .fluidOutputs(Materials.Oxygen.getGas(8000)).duration(150 * SECONDS).eut(32768)
            .addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerBastnasite() {
        GTRecipeBuilder.builder().circuit(2)
            .itemInputsUnsafe(dust(Materials.Bastnasite, 64), dust(Materials.Carbon, 200), dust(Materials.Sodium, 16),
                dust(Materials.Saltpeter, 59), dust(Materials.Copper, 8))
            .fluidInputs(Materials.Hydrogen.getGas(808000), Materials.Nitrogen.getGas(210000),
                Materials.Chlorine.getGas(260000))
            .itemOutputs(dust(Materials.Cerium, 79), dust(Materials.Neodymium, 42), dust(Materials.Lanthanum, 26),
                dust(Materials.Holmium, 17), dust(Materials.Samarium, 11), dust(ELEMENTS.ZIRCONIUM, 11),
                dust(Materials.Gadolinium, 6), dust(Materials.Terbium, 3), dust(Materials.Silicon, 192),
                dust(Materials.Titanium, 163))
            .fluidOutputs(Materials.Fluorine.getGas(12000), Materials.Oxygen.getGas(30000)).duration(300 * SECONDS)
            .eut(32768).addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerRareEarth() {
        GTRecipeBuilder.builder().circuit(2).itemInputsUnsafe(dust(Materials.RareEarth, 10))
            .fluidInputs(Materials.Oxygen.getGas(12000), Materials.Nitrogen.getGas(1000),
                Materials.Hydrogen.getGas(19000))
            .itemOutputs(dust(Materials.Yttrium, 1), dust(MaterialsElements.STANDALONE.RUNITE, 1),
                dust(ELEMENTS.ZIRCONIUM, 1), dust(Materials.Caesium, 1), dust(Materials.Ytterbium, 1),
                dust(Materials.Erbium, 1), dust(Materials.Lanthanum, 2), dust(ELEMENTS.IODINE, 1),
                dust(Materials.Gadolinium, 1), dust(Materials.Dysprosium, 1), dust(ELEMENTS.GERMANIUM, 1),
                dust(Materials.Tellurium, 1), dust(Materials.Tantalum, 1), dust(Materials.Niobium, 1),
                dust(MaterialsElements.STANDALONE.BLACK_METAL, 1), dust(Materials.Samarium, 1),
                dust(Materials.Cerium, 1), dust(Materials.Holmium, 1))
            .fluidOutputs(ELEMENTS.BROMINE.getFluidStack(1000)).duration(16 * SECONDS).eut(32768)
            .addTo(GTNGRecipeMaps.RareEarthDustRecipes);
    }

    private static void registerPyropeOre() {
        GTRecipeBuilder.builder().circuit(1).itemInputs(crushedOre(Materials.Pyrope))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER)).itemOutputs(dust(Materials.Magnesium, 110),
                dust(Materials.Manganese, 70), dust(Materials.Borax, 60), dust(ELEMENTS.RHENIUM, 20))
            .duration(120 * SECONDS).eut(480).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerAlmandineOre() {
        GTRecipeBuilder.builder().circuit(1).itemInputs(crushedOre(Materials.Almandine))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER))
            .itemOutputs(dust(Materials.Aluminium, 150), dust(Materials.Magnesium, 75), dust(Materials.Yttrium, 25),
                dust(Materials.Ytterbium, 15))
            .duration(60 * SECONDS).eut(7680).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerGrossularOre() {
        GTRecipeBuilder.builder().circuit(1).itemInputs(crushedOre(Materials.Grossular))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER)).itemOutputs(dust(Materials.Calcium, 180),
                dust(Materials.Aluminium, 110), dust(Materials.Tungsten, 60), dust(ELEMENTS.THALLIUM, 15))
            .duration(60 * SECONDS).eut(7680).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerSpessartineOre() {
        GTRecipeBuilder.builder().circuit(1).itemInputs(crushedOre(Materials.Spessartine))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER))
            .itemOutputs(dust(Materials.Manganese, 150), dust(Materials.Aluminium, 90), dust(Materials.Osmium, 30),
                dust(Materials.Strontium, 20))
            .duration(60 * SECONDS).eut(7680).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerRedstoneOre() {
        GTRecipeBuilder.builder().circuit(2).itemInputs(crushedOre(Materials.Redstone))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER))
            .itemOutputs(dust(Materials.Redstone, 300), dust(Materials.Chrome, 60), dust(Materials.Flint, 45),
                dust(Materials.Dysprosium, 16))
            .duration(120 * SECONDS).eut(1960).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerNickelOre() {
        GTRecipeBuilder.builder().circuit(2).itemInputs(crushedOre(Materials.Nickel))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER))
            .itemOutputs(dust(Materials.Nickel, 150), dust(Materials.Cobalt, 120), dust(ELEMENTS.RHODIUM, 32),
                dust(ELEMENTS.RUTHENIUM, 16))
            .duration(120 * SECONDS).eut(1960).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerChalcopyriteOre() {
        GTRecipeBuilder.builder().circuit(2).itemInputs(crushedOre(Materials.Chalcopyrite))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER)).itemOutputs(dust(Materials.Copper, 180),
                dust(Materials.Iron, 120), dust(Materials.Cadmium, 50), dust(Materials.Indium, 10))
            .duration(120 * SECONDS).eut(1960).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerPentlanditeOre() {
        GTRecipeBuilder.builder().circuit(2).itemInputs(crushedOre(Materials.Pentlandite))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER)).itemOutputs(dust(Materials.Iron, 150),
                dust(Materials.Nickel, 100), dust(Materials.Promethium, 20), dust(ELEMENTS.HAFNIUM, 10))
            .duration(120 * SECONDS).eut(7680).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerSphaleriteOre() {
        GTRecipeBuilder.builder().circuit(2).itemInputs(crushedOre(Materials.Sphalerite))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER))
            .itemOutputs(dust(Materials.Zinc, 180), dust(Materials.Iron, 120), dust(Materials.Indium, 64),
                dust(ELEMENTS.GERMANIUM, 15))
            .duration(180 * SECONDS).eut(7680).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerPlatinumOre() {
        GTRecipeBuilder.builder().circuit(2).itemInputs(crushedOre(Materials.Platinum))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER))
            .itemOutputs(dust(PTMetallicPowder, 128), dust(ELEMENTS.RHODIUM, 60), dust(ELEMENTS.SELENIUM, 40),
                dust(Materials.Tellurium, 10))
            .duration(180 * SECONDS).eut(7680).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerMonaziteOre() {
        GTRecipeBuilder.builder().circuit(2).itemInputs(crushedOre(Materials.Monazite))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER))
            .itemOutputs(dust(Materials.Erbium, 64), dust(Materials.Lanthanum, 32), dust(Materials.Lutetium, 16),
                dust(Materials.Europium, 8))
            .duration(180 * SECONDS).eut(12288).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static void registerNetherrack() {
        GTRecipeBuilder.builder().circuit(3).itemInputs(new ItemStack(Blocks.netherrack, 64))
            .fluidInputs(Materials.Water.getFluid(ORE_WATER))
            .itemOutputs(dust(Materials.Sulfur, 256), dust(Materials.Antimony, 55), dust(Materials.CertusQuartz, 40),
                dust(Materials.NetherQuartz, 40), dust(Materials.Ardite, 32), dust(Materials.Cobalt, 32))
            .duration(180 * SECONDS).eut(12288).addTo(GTNGRecipeMaps.RareEarthOreRecipes);
    }

    private static ItemStack dust(Materials material, int amount) {
        return GTUtility.copyAmountUnsafe(amount,
            requireNonNull(GTOreDictUnificator.get(OrePrefixes.dust, material, 1), "Missing dust: " + material));
    }

    private static ItemStack dust(Werkstoff material, int amount) {
        return GTUtility.copyAmountUnsafe(amount,
            requireNonNull(material.get(OrePrefixes.dust, 1), "Missing dust: " + material.getVarName()));
    }

    private static ItemStack dust(Material material, int amount) {
        return GTUtility.copyAmountUnsafe(amount, requireNonNull(material.getDust(1), "Missing dust: " + material));
    }

    private static ItemStack crushedOre(Materials material) {
        return GTUtility.copyAmountUnsafe(64, requireNonNull(GTOreDictUnificator.get(OrePrefixes.crushed, material, 1),
            "Missing crushed ore: " + material));
    }
}
