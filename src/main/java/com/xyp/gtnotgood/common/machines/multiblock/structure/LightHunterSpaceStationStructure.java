// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.xyp.gtnotgood.common.blocks.StructureCasing;
import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.blocks.fieldextruder.FieldExtruderCasing;
import com.xyp.gtnotgood.common.blocks.heart.HeartCasing;
import com.xyp.gtnotgood.common.blocks.kerrnewman.KerrNewmanCasing;
import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

public final class LightHunterSpaceStationStructure {

    public static final int WIDTH = 73;
    public static final int HEIGHT = 73;
    public static final int LENGTH = 189;
    public static final int OFFSET_X = 36;
    public static final int OFFSET_Y = 36;
    public static final int OFFSET_Z = 0;
    public static final String SYMBOLS = "=ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcd";
    public static final String RESOURCE = "/assets/" + ModList.ModIds.GT_NOT_GOOD
        + "/structures/light_hunter_space_station.mbs";
    private static final String[][] SHAPE = loadShape();

    private LightHunterSpaceStationStructure() {}

    /** Horizontal layers, top to bottom; pass to StructureUtility.transpose. Spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) shape[row] = SHAPE[row].clone();
        return shape;
    }

    private static StructureCasing casing(char symbol) {
        return switch (symbol) {
            case '=' -> LightHunterCasing.NeutroniumPipeCasing;
            case 'A' -> LightHunterCasing.UhvMachineCasing;
            case 'B' -> FieldExtruderCasing.HyperMechanical;
            case 'C' -> LightHunterCasing.UxvMachineCasing;
            case 'D' -> DimensionalFocusCasing.FusionGlass;
            case 'E' -> DimensionalFocusCasing.DimensionInjection;
            case 'F' -> LightHunterCasing.DimensionalBridgeCasing;
            case 'G' -> FieldExtruderCasing.ContainmentFieldGenerator;
            case 'H' -> KerrNewmanCasing.AtomicCasing;
            case 'I' -> KerrNewmanCasing.ExtremeDensityCasing;
            case 'K' -> LightHunterCasing.EchoCasing;
            case 'L' -> LightHunterCasing.DragonStrengthTritaniumCasing;
            case 'M' -> LightHunterCasing.HyperCore;
            case 'N' -> DimensionalFocusCasing.DimensionallyTranscendent;
            case 'O' -> LightHunterCasing.UxvHermeticCasing;
            case 'P' -> LightHunterCasing.RestraintDevice;
            case 'Q' -> AssemblyPlantCasing.SpacetimeAssemblyLineUnit;
            case 'R' -> LightHunterCasing.HighPowerCasing;
            case 'S' -> FieldExtruderCasing.Molecular;
            case 'U' -> LightHunterCasing.FusionCasingMkIV;
            case 'V' -> HeartCasing.TemporalAnchorFieldCasing;
            case 'W' -> LightHunterCasing.FusionCasingMkV;
            case 'X' -> LightHunterCasing.UltimateStellarContainmentCasing;
            case 'Y' -> KerrNewmanCasing.EnhancedHyperCasing;
            case 'Z' -> DimensionalFocusCasing.ImprovedSuperconductor;
            case '[' -> LightHunterCasing.ForceFieldGlass;
            case ']' -> KerrNewmanCasing.GravityStabilizationCasing;
            case '^' -> LightHunterCasing.ModuleConnector;
            case '_' -> LightHunterCasing.AdvancedComputerCasing;
            case '`' -> LightHunterCasing.ComputerHeatVent;
            case 'a' -> LightHunterCasing.SuperComputationComponent;
            case 'b' -> LightHunterCasing.UivMachineCasing;
            case 'c' -> LightHunterCasing.UhvUltimateBattery;
            case 'd' -> LightHunterCasing.HighPowerCasing;
            default -> null;
        };
    }

    public static Block block(char symbol) {
        return switch (symbol) {
            case '=' -> LightHunterCasing.NeutroniumPipeCasing.getStructureBlock();
            case 'A' -> LightHunterCasing.UhvMachineCasing.getStructureBlock();
            case 'B' -> FieldExtruderCasing.HyperMechanical.getStructureBlock();
            case 'C' -> LightHunterCasing.UxvMachineCasing.getStructureBlock();
            case 'D' -> DimensionalFocusCasing.FusionGlass.getStructureBlock();
            case 'E' -> DimensionalFocusCasing.DimensionInjection.getStructureBlock();
            case 'F' -> LightHunterCasing.DimensionalBridgeCasing.getStructureBlock();
            case 'G' -> FieldExtruderCasing.ContainmentFieldGenerator.getStructureBlock();
            case 'H' -> KerrNewmanCasing.AtomicCasing.getStructureBlock();
            case 'I' -> KerrNewmanCasing.ExtremeDensityCasing.getStructureBlock();
            case 'K' -> LightHunterCasing.EchoCasing.getStructureBlock();
            case 'L' -> LightHunterCasing.DragonStrengthTritaniumCasing.getStructureBlock();
            case 'M' -> LightHunterCasing.HyperCore.getStructureBlock();
            case 'N' -> DimensionalFocusCasing.DimensionallyTranscendent.getStructureBlock();
            case 'O' -> LightHunterCasing.UxvHermeticCasing.getStructureBlock();
            case 'P' -> LightHunterCasing.RestraintDevice.getStructureBlock();
            case 'Q' -> AssemblyPlantCasing.SpacetimeAssemblyLineUnit.getStructureBlock();
            case 'R' -> LightHunterCasing.HighPowerCasing.getStructureBlock();
            case 'S' -> FieldExtruderCasing.Molecular.getStructureBlock();
            case 'U' -> LightHunterCasing.FusionCasingMkIV.getStructureBlock();
            case 'V' -> HeartCasing.TemporalAnchorFieldCasing.getStructureBlock();
            case 'W' -> LightHunterCasing.FusionCasingMkV.getStructureBlock();
            case 'X' -> LightHunterCasing.UltimateStellarContainmentCasing.getStructureBlock();
            case 'Y' -> KerrNewmanCasing.EnhancedHyperCasing.getStructureBlock();
            case 'Z' -> DimensionalFocusCasing.ImprovedSuperconductor.getStructureBlock();
            case '[' -> LightHunterCasing.ForceFieldGlass.getStructureBlock();
            case ']' -> KerrNewmanCasing.GravityStabilizationCasing.getStructureBlock();
            case '^' -> LightHunterCasing.ModuleConnector.getStructureBlock();
            case '_' -> LightHunterCasing.AdvancedComputerCasing.getStructureBlock();
            case '`' -> LightHunterCasing.ComputerHeatVent.getStructureBlock();
            case 'a' -> LightHunterCasing.SuperComputationComponent.getStructureBlock();
            case 'b' -> LightHunterCasing.UivMachineCasing.getStructureBlock();
            case 'c' -> LightHunterCasing.UhvUltimateBattery.getStructureBlock();
            case 'd' -> LightHunterCasing.HighPowerCasing.getStructureBlock();
            case 'J' -> Block.getBlockFromItem(neutroniumFrame().getItem());
            case 'T' -> AEApi.instance().definitions().blocks().quartzVibrantGlass().maybeBlock().get();
            default -> throw new IllegalArgumentException("Unknown Light Hunter structure symbol: " + symbol);
        };
    }

    public static int metadata(char symbol) {
        return switch (symbol) {
            case '=' -> LightHunterCasing.NeutroniumPipeCasing.getStructureMetadata();
            case 'A' -> LightHunterCasing.UhvMachineCasing.getStructureMetadata();
            case 'B' -> FieldExtruderCasing.HyperMechanical.getStructureMetadata();
            case 'C' -> LightHunterCasing.UxvMachineCasing.getStructureMetadata();
            case 'D' -> DimensionalFocusCasing.FusionGlass.getStructureMetadata();
            case 'E' -> DimensionalFocusCasing.DimensionInjection.getStructureMetadata();
            case 'F' -> LightHunterCasing.DimensionalBridgeCasing.getStructureMetadata();
            case 'G' -> FieldExtruderCasing.ContainmentFieldGenerator.getStructureMetadata();
            case 'H' -> KerrNewmanCasing.AtomicCasing.getStructureMetadata();
            case 'I' -> KerrNewmanCasing.ExtremeDensityCasing.getStructureMetadata();
            case 'K' -> LightHunterCasing.EchoCasing.getStructureMetadata();
            case 'L' -> LightHunterCasing.DragonStrengthTritaniumCasing.getStructureMetadata();
            case 'M' -> LightHunterCasing.HyperCore.getStructureMetadata();
            case 'N' -> DimensionalFocusCasing.DimensionallyTranscendent.getStructureMetadata();
            case 'O' -> LightHunterCasing.UxvHermeticCasing.getStructureMetadata();
            case 'P' -> LightHunterCasing.RestraintDevice.getStructureMetadata();
            case 'Q' -> AssemblyPlantCasing.SpacetimeAssemblyLineUnit.getStructureMetadata();
            case 'R' -> LightHunterCasing.HighPowerCasing.getStructureMetadata();
            case 'S' -> FieldExtruderCasing.Molecular.getStructureMetadata();
            case 'U' -> LightHunterCasing.FusionCasingMkIV.getStructureMetadata();
            case 'V' -> HeartCasing.TemporalAnchorFieldCasing.getStructureMetadata();
            case 'W' -> LightHunterCasing.FusionCasingMkV.getStructureMetadata();
            case 'X' -> LightHunterCasing.UltimateStellarContainmentCasing.getStructureMetadata();
            case 'Y' -> KerrNewmanCasing.EnhancedHyperCasing.getStructureMetadata();
            case 'Z' -> DimensionalFocusCasing.ImprovedSuperconductor.getStructureMetadata();
            case '[' -> LightHunterCasing.ForceFieldGlass.getStructureMetadata();
            case ']' -> KerrNewmanCasing.GravityStabilizationCasing.getStructureMetadata();
            case '^' -> LightHunterCasing.ModuleConnector.getStructureMetadata();
            case '_' -> LightHunterCasing.AdvancedComputerCasing.getStructureMetadata();
            case '`' -> LightHunterCasing.ComputerHeatVent.getStructureMetadata();
            case 'a' -> LightHunterCasing.SuperComputationComponent.getStructureMetadata();
            case 'b' -> LightHunterCasing.UivMachineCasing.getStructureMetadata();
            case 'c' -> LightHunterCasing.UhvUltimateBattery.getStructureMetadata();
            case 'd' -> LightHunterCasing.HighPowerCasing.getStructureMetadata();
            case 'J' -> neutroniumFrame().getItemDamage();
            case 'T' -> 0;
            default -> throw new IllegalArgumentException("Unknown Light Hunter structure symbol: " + symbol);
        };
    }

    public static <T> IStructureElement<T> element(char symbol) {
        StructureCasing casing = casing(symbol);
        return casing == null ? StructureUtility.ofBlock(block(symbol), metadata(symbol)) : casing.asElement();
    }

    private static ItemStack neutroniumFrame() {
        ItemStack frame = GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Neutronium, 1);
        if (frame == null || Block.getBlockFromItem(frame.getItem()) == Blocks.air)
            throw new IllegalStateException("Native neutronium frame is unavailable");
        return frame;
    }

    private static String[][] loadShape() {
        String[][] result = new String[HEIGHT][LENGTH];
        int controllers = 0;
        try (InputStream stream = LightHunterSpaceStationStructure.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing Light Hunter structure: " + RESOURCE);
            try (DataInputStream data = new DataInputStream(new GZIPInputStream(stream))) {
                if (readVarInt(data) != -1 || readVarInt(data) != 1)
                    throw new IOException("Unsupported Light Hunter structure format");
                if (readVarInt(data) != 2 || readVarInt(data) != 0 || readVarInt(data) != 4)
                    throw new IOException("Unexpected Light Hunter structure axes");
                if (readVarInt(data) != LENGTH) throw new IOException("Invalid Light Hunter structure depth");
                for (int aisle = 0; aisle < LENGTH; aisle++) {
                    if (readVarInt(data) != HEIGHT) throw new IOException("Invalid Light Hunter structure height");
                    for (int row = 0; row < HEIGHT; row++) {
                        String source = data.readUTF();
                        if (source.length() != WIDTH) throw new IOException("Invalid Light Hunter structure width");
                        for (int column = 0; column < WIDTH; column++) {
                            char symbol = source.charAt(column);
                            if (symbol == '~') controllers++;
                            else if (symbol != ' ' && SYMBOLS.indexOf(symbol) < 0)
                                throw new IOException("Invalid Light Hunter structure symbol: " + symbol);
                        }
                        result[HEIGHT - row - 1][LENGTH - aisle - 1] = source;
                    }
                }
                if (data.read() != -1) throw new IOException("Unexpected Light Hunter structure data");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Light Hunter structure", exception);
        }
        if (controllers != 1 || result[OFFSET_Y][OFFSET_Z].charAt(OFFSET_X) != '~')
            throw new IllegalStateException("Invalid Light Hunter controller offset");
        return result;
    }

    private static int readVarInt(DataInputStream data) throws IOException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int next = data.readUnsignedByte();
            value |= (next & 127) << shift;
            if ((next & 128) == 0) return value;
        }
        throw new IOException("Invalid Light Hunter structure integer");
    }
}
